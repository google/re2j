// Copyright 2010 The Go Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

// Original Go source here:
// http://code.google.com/p/go/source/browse/src/pkg/regexp/regexp.go

// Beware, submatch results may pin a large underlying String into
// memory.  Consider creating explicit string copies if submatches are
// long-lived and inputs are large.
//
// The JDK API supports incremental processing of the input without
// necessarily consuming it all; we do not attempt to do so.

// The Java API emphasises UTF-16 Strings, not UTF-8 byte[] as in Go, as
// the primary input datatype, and the method names have been changed to
// reflect this.

package com.google.re2j;

import com.google.re2j.DFA.DFATooManyStatesException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import io.airlift.slice.DynamicSliceOutput;
import io.airlift.slice.Slice;
import io.airlift.slice.SliceOutput;
import io.airlift.slice.Slices;

import static com.google.re2j.MachineInput.EOF;
import static com.google.re2j.Options.Algorithm.DFA;
import static com.google.re2j.Options.Algorithm.DFA_FALLBACK_TO_NFA;
import static com.google.re2j.RE2.Anchor.UNANCHORED;
import static com.google.re2j.RE2.MatchKind.FIRST_MATCH;
import static com.google.re2j.RE2.MatchKind.LONGEST_MATCH;

/**
 * An RE2 class instance is a compiled representation of an RE2 regular
 * expression, independent of the public Java-like Pattern/Matcher API.
 *
 * <p>This class also contains various implementation helpers for RE2
 * regular expressions.
 *
 * <p>Use the {@link #quoteMeta(String)} utility function to quote all
 * regular expression metacharacters in an arbitrary string.
 *
 * <p>See the {@code Matcher} and {@code Pattern} classes for the public
 * API, and the <a href='package-summary.html'>package-level
 * documentation</a> for an overview of how to use this API.
 */
class RE2 {

  // (In the Go implementation this structure is just called "Regexp".)

  //// Parser flags.

  // Fold case during matching (case-insensitive).
  static final int FOLD_CASE            = 0x01;

  // Treat pattern as a literal string instead of a regexp.
  static final int LITERAL              = 0x02;

  // Allow character classes like [^a-z] and [[:space:]] to match newline.
  static final int CLASS_NL             = 0x04;

  // Allow '.' to match newline.
  static final int DOT_NL               = 0x08;

  // Treat ^ and $ as only matching at beginning and end of text, not
  // around embedded newlines.  (Perl's default).
  static final int ONE_LINE             = 0x10;

  // Make repetition operators default to non-greedy.
  static final int NON_GREEDY           = 0x20;

  // allow Perl extensions:
  //   non-capturing parens - (?: )
  //   non-greedy operators - *? +? ?? {}?
  //   flag edits - (?i) (?-i) (?i: )
  //     i - FoldCase
  //     m - !OneLine
  //     s - DotNL
  //     U - NonGreedy
  //   line ends: \A \z
  //   \Q and \E to disable/enable metacharacters
  //   (?P<name>expr) for named captures
  // \C (any byte) is not supported.
  static final int PERL_X               = 0x40;

  // Allow \p{Han}, \P{Han} for Unicode group and negation.
  static final int UNICODE_GROUPS       = 0x80;

  // Regexp END_TEXT was $, not \z.  Internal use only.
  static final int WAS_DOLLAR           = 0x100;

  static final int MATCH_NL             = CLASS_NL | DOT_NL;

  // As close to Perl as possible.
  static final int PERL = CLASS_NL | ONE_LINE | PERL_X | UNICODE_GROUPS;

  // POSIX syntax.
  static final int POSIX = 0;

  //// Anchors
  enum Anchor {
    UNANCHORED,
    ANCHOR_START,
    ANCHOR_BOTH;

    boolean isUnanchored() {
      return this == UNANCHORED;
    }

    boolean isAnchorEnd() {
      return this == ANCHOR_BOTH;
    }

    boolean isAnchorStart() {
      return this == ANCHOR_START || this == ANCHOR_BOTH;
    }

    boolean isAnchorBoth() {
      return this == ANCHOR_BOTH;
    }
  }

  // Kind of match to look for (for anchor != ANCHOR_BOTH)
  //
  // LONGEST_MATCH mode finds the overall longest
  // match but still makes its submatch choices the way
  // Perl would, not in the way prescribed by POSIX.
  // The POSIX rules are much more expensive to implement,
  // and no one has needed them.
  enum MatchKind {
    FIRST_MATCH,    // like Perl, PCRE
    LONGEST_MATCH   // like egrep or POSIX
  }

  //// RE2 instance members.

  final String expr;            // as passed to Compile
  final Prog prog;              // compiled program
  final Prog reverseProg;       // program for matching reversed text
  final int cond;               // EMPTY_* bitmask: empty-width conditions
                                // required at start of match
  final int numSubexp;
  final Options options;

  MatchKind matchKind;
  Slice prefixUTF8;             // required UTF-8 prefix in unanchored matches
  boolean prefixComplete;       // true iff prefix is the entire regexp

  // Cache of machines for running regexp.
  final ThreadLocal<NFAMachine> nfaMachine = new ThreadLocal<NFAMachine>() {
    @Override
    protected NFAMachine initialValue() {
      return new NFAMachine(RE2.this);
    }
  };
  volatile DFAMachine dfaMachine;
  AtomicInteger numberOfDFARetriesLeft;

  // This is visible for testing.
  RE2(RE2 re2) {
    // Copy everything.
    this(re2.expr, re2.prog, re2.reverseProg, re2.numSubexp, re2.matchKind,re2.options,
        re2.prefixComplete, re2.prefixUTF8);
  }

  private RE2(String expr, Prog prog, Prog reverseProg, int numSubexp, MatchKind matchKind,
              Options options, boolean prefixComplete, Slice prefixUTF8) {
    this.expr = expr;
    this.prog = prog;
    this.reverseProg = reverseProg;
    this.numSubexp = numSubexp;
    this.options = options;
    this.cond = prog.startCond();
    this.matchKind = matchKind;
    this.prefixComplete = prefixComplete;
    this.prefixUTF8 = prefixUTF8;
    if (options.getAlgorithm() == DFA || options.getAlgorithm() == DFA_FALLBACK_TO_NFA) {
      this.dfaMachine = new DFAMachine(this, options.getMaximumNumberOfDFAStates());
      this.numberOfDFARetriesLeft = new AtomicInteger(options.getNumberOfDFARetries());
    }
  }

  /**
   * Parses a regular expression and returns, if successful, an
   * {@code RE2} instance that can be used to match against text.
   *
   * <p>When matching against text, the regexp returns a match that
   * begins as early as possible in the input (leftmost), and among those
   * it chooses the one that a backtracking search would have found first.
   * This so-called leftmost-first matching is the same semantics
   * that Perl, Python, and other implementations use, although this
   * package implements it without the expense of backtracking.
   * For POSIX leftmost-longest matching, see {@link #compilePOSIX}.
   */
  static RE2 compile(String expr, Options options) throws PatternSyntaxException {
    return compileImpl(expr, PERL, FIRST_MATCH, options);
  }

  /**
   * {@code compilePOSIX} is like {@link #compile} but restricts the
   * regular expression to POSIX ERE (egrep) syntax and changes the
   * match semantics to leftmost-longest.
   *
   * <p>That is, when matching against text, the regexp returns a match that
   * begins as early as possible in the input (leftmost), and among those
   * it chooses a match that is as long as possible.
   * This so-called leftmost-longest matching is the same semantics
   * that early regular expression implementations used and that POSIX
   * specifies.
   *
   * <p>However, there can be multiple leftmost-longest matches, with different
   * submatch choices, and here this package diverges from POSIX.
   * Among the possible leftmost-longest matches, this package chooses
   * the one that a backtracking search would have found first, while POSIX
   * specifies that the match be chosen to maximize the length of the first
   * subexpression, then the second, and so on from left to right.
   * The POSIX rule is computationally prohibitive and not even well-defined.
   * See http://swtch.com/~rsc/regexp/regexp2.html#posix
   */
  static RE2 compilePOSIX(String expr, Options options) throws PatternSyntaxException {
    return compileImpl(expr, POSIX, LONGEST_MATCH, options);
  }

  // Exposed to ExecTests.
  static RE2 compileImpl(String expr, int mode, MatchKind matchKind, Options options)
      throws PatternSyntaxException {
    Regexp re = Parser.parse(expr, mode);
    int maxCap = re.maxCap();  // (may shrink during simplify)
    re = Simplify.simplify(re);
    Prog prog = Compiler.compileRegexp(re, false);
    Prog reverseProg = Compiler.compileRegexp(re, true);
    SliceOutput prefixBuilder = new DynamicSliceOutput(prog.numInst());
    boolean prefixComplete = prog.prefix(prefixBuilder);
    Slice prefixUTF8 = prefixBuilder.slice();
    RE2 re2 = new RE2(expr, prog, reverseProg, maxCap, matchKind, options, prefixComplete, prefixUTF8);
    return re2;
  }

  /**
   * Returns the number of parenthesized subexpressions in this regular
   * expression.
   */
  int numberOfCapturingGroups() {
    return numSubexp;
  }

  @Override
  public String toString() {
    return expr;
  }

  // doExecute() finds the leftmost match in the input and returns
  // the position of its subexpressions.
  // Derived from exec.go.
  private int[] doExecute(MachineInput in, int pos, Anchor anchor, int ncap) {
    DFAMachine currentDFAMachine = dfaMachine;
    if (currentDFAMachine == null) {
      return doExecute(nfaMachine.get(), in, pos, anchor, ncap);
    } else {
      try {
        return doExecute(currentDFAMachine, in, pos, anchor, ncap);
      } catch (DFATooManyStatesException e) {
        handleTooManyDFAStatesException(e, currentDFAMachine);
        return doExecute(nfaMachine.get(), in, pos, anchor, ncap);
      }
    }
  }

  private int[] doExecute(Machine machine, MachineInput in, int pos, Anchor anchor, int ncap) {
    int[] submatches = new int[ncap];
    return machine.match(in, pos, anchor, submatches) ? submatches : null;
  }

  private synchronized void handleTooManyDFAStatesException(DFATooManyStatesException e, DFAMachine currentDFAMachine) {
    // make sure we don't penalize new DFAMachine instance
    if (currentDFAMachine == dfaMachine) {
      if (numberOfDFARetriesLeft.decrementAndGet() < 0) {
        if (options.getAlgorithm() == DFA_FALLBACK_TO_NFA) {
          dfaMachine = null;
          if (options.getEventsListener() != null) {
            options.getEventsListener().fallbackToNFA();
          }
        } else {
          // keep the old DFAMachine, so other threads can fail too
          throw e;
        }
      } else {
        dfaMachine = new DFAMachine(this, options.getMaximumNumberOfDFAStates());
      }
    }
  }

  /**
   * Returns true iff this regexp matches the string {@code s}.
   */
  boolean match(Slice s) {
    return doExecute(MachineInput.fromUTF8(s), 0, UNANCHORED, 0) != null;
  }

  /**
   * Matches the regular expression against input starting at position start
   * and ending at position end, with the given anchoring.
   * Records the submatch boundaries in group, which is [start, end) pairs
   * of byte offsets. The number of boundaries needed is inferred
   * from the size of the group array. It is most efficient not to ask for
   * submatch boundaries.
   *
   * @param input the input {@link Slice}
   * @param start the beginning position in the input
   * @param anchor the anchoring flag (UNANCHORED, ANCHOR_START, ANCHOR_BOTH)
   * @param group the array to fill with submatch positions
   * @param ngroup the number of array pairs to fill in
   * @return true if a match was found
   */
  boolean match(Slice input, int start, Anchor anchor, int[] group,
                int ngroup) {
    if (start > input.length()) {
      return false;
    }
    // TODO(afrozm): We suspect that the correct code should look something
    // like the following:
    // doExecute(MachineInput.fromUTF16(input), start, anchor, 2*ngroup);
    //
    // In Russ' own words:
    // That is, I believe doExecute needs to know the bounds of the whole input
    // as well as the bounds of the subpiece that is being searched.
    int[] groupMatch = doExecute(MachineInput.fromUTF8(input), start,
        anchor, 2 * ngroup);

    if (groupMatch == null) {
      return false;
    }

    if (group != null) {
      System.arraycopy(groupMatch, 0, group, 0, groupMatch.length);
    }
    return true;
  }

  /**
   * Returns true iff textual regular expression {@code pattern}
   * matches {@link Slice} {@code s}.
   *
   * <p>More complicated queries need to use {@link #compile} and the
   * full {@code RE2} interface.
   */
  // This is visible for testing.
  static boolean match(String pattern, Slice s, Options options) throws PatternSyntaxException {
    return compile(pattern, options).match(s);
  }

  // This is visible for testing.
  interface ReplaceFunc {
    Slice replace(Slice orig);
  }

  /**
   * Returns a copy of {@code src} in which all matches for this regexp
   * have been replaced by {@code repl}.  No support is provided for
   * expressions (e.g. {@code \1} or {@code $1}) in the replacement
   * {@link Slice}.
   */
  // This is visible for testing.
  Slice replaceAll(Slice src, final Slice repl) {
    return replaceAllFunc(src, new ReplaceFunc() {
        @Override public Slice replace(Slice orig) { return repl; }
      }, 2 * src.length() + 1);
    // TODO(afrozm): Is the reasoning correct, there can be at the most 2*len +1
    // replacements. Basically [a-z]*? abc x will be xaxbcx. So should it be
    // len + 1 or 2*len + 1.
  }

  /**
   * Returns a copy of {@code src} in which only the first match for this regexp
   * has been replaced by {@code repl}.  No support is provided for
   * expressions (e.g. {@code \1} or {@code $1}) in the replacement
   * {@link Slice}.
   */
  // This is visible for testing.
  Slice replaceFirst(Slice src, final Slice repl) {
    return replaceAllFunc(src, new ReplaceFunc() {
      @Override public Slice replace(Slice orig) { return repl; }
    }, 1);
  }

  /**
   * Returns a copy of {@code src} in which at most {@code maxReplaces} matches
   * for this regexp have been replaced by the return value of of function
   * {@code repl} (whose first argument is the matched string). No support is
   * provided for expressions (e.g. {@code \1} or {@code $1}) in the
   * replacement {@link Slice}.
   */
  // This is visible for testing.
  Slice replaceAllFunc(Slice src, ReplaceFunc repl, int maxReplaces) {
    int lastMatchEnd = 0; // end position of the most recent match
    int searchPos = 0;    // position where we next look for a match
    SliceOutput buf = new DynamicSliceOutput(src.length());
    MachineInput input = MachineInput.fromUTF8(src);
    int numReplaces = 0;
    while (searchPos <= src.length()) {
      int[] a = doExecute(input, searchPos, UNANCHORED, 2);
      if (a == null || a.length == 0) {
        break;  // no more matches
      }

      // Copy the unmatched characters before this match.
      buf.writeBytes(src, lastMatchEnd, a[0] - lastMatchEnd);

      // Now insert a copy of the replacement string, but not for a
      // match of the empty string immediately after another match.
      // (Otherwise, we get double replacement for patterns that
      // match both empty and nonempty strings.)
      // FIXME(adonovan), FIXME(afrozm) - JDK seems to be doing exactly this
      // put a replacement for a pattern that also matches empty and non-empty
      // strings. The fix would not just be a[1] >= lastMatchEnd, there are a
      // few corner cases in that as well, and there are tests which will fail
      // when that case is touched (happens only at the end of the input string
      // though).
      if (a[1] > lastMatchEnd || a[0] == 0) {
        buf.writeBytes(repl.replace(src.slice(a[0], a[1] - a[0])));
        // Increment the replace count.
        ++numReplaces;
      }
      lastMatchEnd = a[1];

      // Advance past this match
      if (searchPos + 1 > a[1]) {
        searchPos++;
      } else {
        searchPos = a[1];
      }
      if (numReplaces >= maxReplaces) {
        // Should never be greater though.
        break;
      }
    }

    // Copy the unmatched characters after the last match.
    buf.writeBytes(src, lastMatchEnd, src.length() - lastMatchEnd);

    return buf.slice();
  }

  /**
   * Returns a string that quotes all regular expression metacharacters
   * inside the argument text; the returned string is a regular
   * expression matching the literal text.  For example,
   * {@code quoteMeta("[foo]").equals("\\[foo\\]")}.
   */
  static String quoteMeta(String s) {
    StringBuilder b = new StringBuilder(2 * s.length());
    // A char loop is correct because all metacharacters fit in one UTF-16 code.
    for (int i = 0, len = s.length(); i < len; i++) {
      char c = s.charAt(i);
      if ("\\.+*?()|[]{}^$".indexOf(c) >= 0) {
        b.append('\\');
      }
      b.append(c);
    }
    return b.toString();
  }

  // The number of capture values in the program may correspond
  // to fewer capturing expressions than are in the regexp.
  // For example, "(a){0}" turns into an empty program, so the
  // maximum capture in the program is 0 but we need to return
  // an expression for \1.  Pad returns a with -1s appended as needed;
  // the result may alias a.
  private int[] pad(int[] a) {
    if (a == null) {
      return null;  // No match.
    }
    int n = (1 + numSubexp) * 2;
    if (a.length < n) {
      int[] a2 = new int[n];
      System.arraycopy(a, 0, a2, 0, a.length);
      Arrays.fill(a2, a.length, n, -1);
      a = a2;
    }
    return a;
  }

  private interface DeliverFunc {
    // Called iteratively with a list of submatch indices in the same
    // unit as the MachineInput cursor.
    void deliver(int[] x);
  }

  // Find matches in input.
  private void allMatches(MachineInput input, int n, DeliverFunc deliver) {
    int end = input.endPos();
    if (n < 0) {
      n = end + 1;
    }
    for (int pos = 0, i = 0, prevMatchEnd = -1; i < n && pos <= end; ) {
      int[] matches = doExecute(input, pos, UNANCHORED, prog.numCap);
      if (matches == null || matches.length == 0) {
        break;
      }

      boolean accept = true;
      if (matches[1] == pos) {
        // We've found an empty match.
        if (matches[0] == prevMatchEnd) {
          // We don't allow an empty match right
          // after a previous match, so ignore it.
          accept = false;
        }
        byte b = input.getByte(pos);
        if (b == EOF) {
          pos = end + 1;
        } else {
          pos++;
        }
      } else {
        pos = matches[1];
      }
      prevMatchEnd = matches[1];

      if (accept) {
        deliver.deliver(pad(matches));
        i++;
      }
    }
  }

  // Legacy Go-style interface; preserved (package-private) for better
  // test coverage.
  //
  // There are 16 methods of RE2 that match a regular expression and
  // identify the matched text.  Their names are matched by this regular
  // expression:
  //
  //    find(All)?(UTF8)?(Submatch)?(Index)?
  //
  // If 'All' is present, the routine matches successive non-overlapping
  // matches of the entire expression.  Empty matches abutting a
  // preceding match are ignored.  The return value is an array
  // containing the successive return values of the corresponding
  // non-All routine.  These routines take an extra integer argument, n;
  // if n >= 0, the function returns at most n matches/submatches.
  //
  // If 'UTF8' is present, the argument is a UTF-8 encoded byte[] array;
  // otherwise it is a UTF-16 encoded java.lang.String; return values
  // are adjusted as appropriate.
  //
  // If 'Submatch' is present, the return value is an list identifying
  // the successive submatches of the expression.  Submatches are
  // matches of parenthesized subexpressions within the regular
  // expression, numbered from left to right in order of opening
  // parenthesis.  Submatch 0 is the match of the entire expression,
  // submatch 1 the match of the first parenthesized subexpression, and
  // so on.
  //
  // If 'Index' is present, matches and submatches are identified by
  // byte index pairs within the input string: result[2*n:2*n+1]
  // identifies the indexes of the nth submatch.  The pair for n==0
  // identifies the match of the entire expression.  If 'Index' is not
  // present, the match is identified by the text of the match/submatch.
  // If an index is negative, it means that subexpression did not match
  // any string in the input.

  /**
   * Returns a {@link Slice} holding the text of the leftmost match in
   * {@code s} of this regular expression.
   *
   * <p>If there is no match, the return value is an empty {@link Slice}, but it
   * will also be empty if the regular expression successfully matches
   * an empty {@link Slice}.  Use {@link #findIndex} or
   * {@link #findSubmatch} if it is necessary to distinguish these
   * cases.
   */
  // This is visible for testing.
  Slice find(Slice s) {
    int[] a = doExecute(MachineInput.fromUTF8(s), 0, UNANCHORED, 2);
    if (a == null) {
      return Slices.EMPTY_SLICE;
    }
    return s.slice(a[0], a[1] - a[0]);
  }

  /**
   * Returns a two-element array of integers defining the location of
   * the leftmost match in {@code s} of this regular expression.  The
   * match itself is at {@code s.slice(loc[0], loc[1] - loc[0])}.
   *
   * <p>A return value of null indicates no match.
   */
  // This is visible for testing.
  int[] findIndex(Slice s) {
    int[] a = doExecute(MachineInput.fromUTF8(s), 0, UNANCHORED, 2);
    if (a == null) {
      return null;
    }
    return a;
  }

  /**
   * Returns an array of {@link Slice}s holding the text of the leftmost match
   * of the regular expression in {@code s} and the matches, if any, of
   * its subexpressions, as defined by the <a
   * href='#submatch'>Submatch</a> description above.
   *
   * <p>A return value of null indicates no match.
   */
  // This is visible for testing.
  Slice[] findSubmatch(Slice s) {
    int[] a = doExecute(MachineInput.fromUTF8(s), 0, UNANCHORED, prog.numCap);
    if (a == null) {
      return null;
    }
    Slice[] ret = new Slice[1 + numSubexp];
    for (int i = 0; i < ret.length; i++) {
      if (2 * i < a.length && a[2 * i] >= 0) {
        int begin = a[2 * i];
        int end = a[2 * i + 1];
        ret[i] = s.slice(begin, end - begin);
      }
    }
    return ret;
  }

  /**
   * Returns an array holding the index pairs identifying the leftmost
   * match of this regular expression in {@code s} and the matches, if
   * any, of its subexpressions, as defined by the <a
   * href='#submatch'>Submatch</a> description above.
   *
   * <p>A return value of null indicates no match.
   */
  // This is visible for testing.
  int[] findSubmatchIndex(Slice s) {
    return pad(doExecute(MachineInput.fromUTF8(s), 0, UNANCHORED, prog.numCap));
  }

  /**
   * {@code findAll} is the <a href='#all'>All</a> version of
   * {@link #find}; it returns a list of up to {@code n}
   * successive matches of the expression, as defined by the <a
   * href='#all'>All</a> description above.
   *
   * <p>A return value of null indicates no match.
   */
  // This is visible for testing.
  List<Slice> findAll(final Slice s, int n) {
    final List<Slice> result = new ArrayList<Slice>();
    allMatches(MachineInput.fromUTF8(s), n, new DeliverFunc() {
        @Override public void deliver(int[] match) {
          result.add(s.slice(match[0], match[1] - match[0]));
        }});
    if (result.isEmpty()) {
      return null;
    }
    return result;
  }

  /**
   * {@code findAllIndex} is the <a href='#all'>All</a> version of
   * {@link #findIndex}; it returns a list of up to {@code n}
   * successive matches of the expression, as defined by the <a
   * href='#all'>All</a> description above.
   *
   * <p>A return value of null indicates no match.
   */
  // This is visible for testing.
  List<int[]> findAllIndex(Slice s, int n) {
    final List<int[]> result = new ArrayList<int[]>();
    allMatches(MachineInput.fromUTF8(s), n, new DeliverFunc() {
      @Override public void deliver(int[] match) {
          result.add(Utils.subarray(match, 0, 2));
        }});
    if (result.isEmpty()) {
      return null;
    }
    return result;
  }

  /**
   * {@code findAllSubmatch} is the <a href='#all'>All</a> version
   * of {@link #findSubmatch}; it returns a list of up to
   * {@code n} successive matches of the expression, as defined by the
   * <a href='#all'>All</a> description above.
   *
   * <p>A return value of null indicates no match.
   */
  // This is visible for testing.
  List<Slice[]> findAllSubmatch(final Slice s, int n) {
    final List<Slice[]> result = new ArrayList<Slice[]>();
    allMatches(MachineInput.fromUTF8(s), n, new DeliverFunc() {
        @Override public void deliver(int[] match) {
          Slice[] slice = new Slice[match.length / 2];
          for (int j = 0; j < slice.length; ++j) {
            if (match[2 * j] >= 0) {
              int begin = match[2 * j];
              int end = match[2 * j + 1];
              slice[j] = s.slice(begin, end - begin);
            }
          }
          result.add(slice);
        }});
    if (result.isEmpty()) {
      return null;
    }
    return result;
  }

  /**
   * {@code findAllSubmatchIndex} is the <a href='#all'>All</a>
   * version of {@link #findSubmatchIndex}; it returns a list of
   * up to {@code n} successive matches of the expression, as defined by
   * the <a href='#all'>All</a> description above.
   *
   * <p>A return value of null indicates no match.
   */
  // This is visible for testing.
  List<int[]> findAllSubmatchIndex(Slice s, int n) {
    final List<int[]> result = new ArrayList<int[]>();
    allMatches(MachineInput.fromUTF8(s), n, new DeliverFunc() {
        @Override public void deliver(int[] match) {
          result.add(match);
        }});
    if (result.isEmpty()) {
      return null;
    }
    return result;
  }

}
