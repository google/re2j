/*
 * Copyright (c) 2021 The Go Authors. All rights reserved.
 *
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
// Original Go source here:
// http://code.google.com/p/go/source/browse/src/pkg/regexp/syntax/exec_test.go

package com.google.re2j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.google.common.base.Splitter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPInputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

// TestRE2 tests this package's regexp API against test cases
// considered during (C++) RE2's exhaustive tests, which run all possible
// regexps over a given set of atoms and operators, up to a given
// complexity, over all possible strings over a given alphabet,
// up to a given size.  Rather than try to link with RE2, we read a
// log file containing the test cases and the expected matches.
// The log file, re2.txt, is generated by running 'make exhaustive-log'
// in the open source RE2 distribution.  http://code.google.com/p/re2/
//
// The test file format is a sequence of stanzas like:
//
//      strings
//      "abc"
//      "123x"
//      regexps
//      "[a-z]+"
//      0-3;0-3
//      -;-
//      "([0-9])([0-9])([0-9])"
//      -;-
//      -;0-3 0-1 1-2 2-3
//
// The stanza begins by defining a set of strings, quoted
// using Go double-quote syntax, one per line.  Then the
// regexps section gives a sequence of regexps to run on
// the strings.  In the block that follows a regexp, each line
// gives the semicolon-separated match results of running
// the regexp on the corresponding string.
// Each match result is either a single -, meaning no match, or a
// space-separated sequence of pairs giving the match and
// submatch indices.  An unmatched subexpression formats
// its pair as a single - (not illustrated above).  For now
// each regexp run produces two match results, one for a
// ``full match'' that restricts the regexp to matching the entire
// string or nothing, and one for a ``partial match'' that gives
// the leftmost first match found in the string.
//
// Lines beginning with # are comments.  Lines beginning with
// a capital letter are test names printed during RE2's test suite
// and are echoed into t but otherwise ignored.
//
// At time of writing, re2.txt is 32 MB but compresses to 760 kB,
// so we store re2.txt.gz in the repository and decompress it on the fly.
@RunWith(JUnit4.class)
public class ExecTest {

  @Test
  public void testExamplesInDocumentation() throws PatternSyntaxException {
    RE2 re = RE2.compile("(?i:co(.)a)");
    assertEquals(Arrays.asList("Copa", "coba"), re.findAll("Copacobana", 10));
    List<String[]> x = re.findAllSubmatch("Copacobana", 100);
    assertEquals(Arrays.asList("Copa", "p"), Arrays.asList(x.get(0)));
    assertEquals(Arrays.asList("coba", "b"), Arrays.asList(x.get(1)));
  }

  @Test
  public void testRE2Search() throws IOException {
    testRE2("re2-search.txt");
  }

  @Test
  public void testRE2Exhaustive() throws IOException {
    testRE2("re2-exhaustive.txt.gz"); // takes about 30s
  }

  public void testRE2(String file) throws IOException {
    InputStream in = ExecTest.class.getResourceAsStream("/" + file);
    // TODO(adonovan): call in.close() on all paths.
    if (file.endsWith(".gz")) {
      in = new GZIPInputStream(in);
      file = file.substring(0, file.length() - ".gz".length()); // for errors
    }
    int lineno = 0;
    UNIXBufferedReader r = new UNIXBufferedReader(new InputStreamReader(in, "UTF-8"));
    ArrayList<String> strings = new ArrayList<String>();
    int input = 0; // next index within strings to read
    boolean inStrings = false;
    RE2 re = null, refull = null;
    int nfail = 0, ncase = 0;
    String line;
    while ((line = r.readLine()) != null) {
      lineno++;
      if (line.isEmpty()) {
        fail(String.format("%s:%d: unexpected blank line", file, lineno));
      }

      char first = line.charAt(0);
      if (first == '#') {
        continue;
      }
      if ('A' <= first && first <= 'Z') {
        // Test name.
        System.err.println(line);
        continue;
      } else if (line.equals("strings")) {
        if (input < strings.size()) {
          fail(
              String.format(
                  "%s:%d: out of sync: have %d strings left",
                  file,
                  lineno,
                  strings.size() - input));
        }
        strings.clear();
        inStrings = true;
      } else if (line.equals("regexps")) {
        inStrings = false;
      } else if (first == '"') {
        String q;
        try {
          q = Strconv.unquote(line);
        } catch (Exception e) {
          // Fatal because we'll get out of sync.
          fail(String.format("%s:%d: unquote %s: %s", file, lineno, line, e.getMessage()));
          q = null; // unreachable
        }
        if (inStrings) {
          strings.add(q);
          continue;
        }
        // Is a regexp.
        re = refull = null;
        try {
          re = RE2.compile(q);
        } catch (Throwable e) { // (handle compiler panic too)
          if (e.getMessage().equals("error parsing regexp: invalid escape sequence: `\\C`")) {
            // We don't and likely never will support \C; keep going.
            continue;
          }
          System.err.format("%s:%d: compile %s: %s\n", file, lineno, q, e.getMessage());
          if (++nfail >= 100) {
            fail("stopping after " + nfail + " errors");
          }
          continue;
        }
        String full = "\\A(?:" + q + ")\\z";
        try {
          refull = RE2.compile(full);
        } catch (Throwable e) { // (handle compiler panic too)
          // Fatal because q worked, so this should always work.
          fail(String.format("%s:%d: compile full %s: %s", file, lineno, full, e.getMessage()));
        }
        input = 0;
      } else if (first == '-' || ('0' <= first && first <= '9')) {
        // A sequence of match results.
        ncase++;
        if (re == null) {
          // Failed to compile: skip results.
          continue;
        }
        if (input >= strings.size()) {
          fail(String.format("%s:%d: out of sync: no input remaining", file, lineno));
        }
        String text = strings.get(input++);
        boolean multibyte = !isSingleBytes(text);
        if (multibyte && re.toString().contains("\\B")) {
          // C++ RE2's \B considers every position in the input, which
          // is a stream of bytes, so it sees 'not word boundary' in the
          // middle of a rune.  But this package only considers whole
          // runes, so it disagrees.  Skip those cases.
          continue;
        }
        List<String> res = Splitter.on(';').splitToList(line);
        if (res.size() != 4) {
          fail(String.format("%s:%d: have %d test results, want %d", file, lineno, res.size(), 4));
        }
        for (int i = 0; i < 4; ++i) {
          boolean partial = (i & 1) != 0, longest = (i & 2) != 0;
          RE2 regexp = partial ? re : refull;

          regexp.longest = longest;
          int[] have = regexp.findSubmatchIndex(text); // UTF-16 indices
          if (multibyte && have != null) {
            // The testdata uses UTF-8 indices, but we're using the UTF-16 API.
            // Perhaps we should use the UTF-8 RE2 API?
            have = utf16IndicesToUtf8(have, text);
          }
          int[] want = parseResult(file, lineno, res.get(i)); // UTF-8 indices
          if (!Arrays.equals(want, have)) {
            System.err.format(
                "%s:%d: %s[partial=%b,longest=%b].findSubmatchIndex(%s) = " + "%s, want %s\n",
                file,
                lineno,
                re,
                partial,
                longest,
                text,
                Arrays.toString(have),
                Arrays.toString(want));
            if (++nfail >= 100) {
              fail("stopping after " + nfail + " errors");
            }
            continue;
          }

          regexp.longest = longest;
          boolean b = regexp.match(text);
          if (b != (want != null)) {
            System.err.format(
                "%s:%d: %s[partial=%b,longest=%b].match(%s) = " + "%b, want %b\n",
                file,
                lineno,
                re,
                partial,
                longest,
                text,
                b,
                !b);
            if (++nfail >= 100) {
              fail("stopping after " + nfail + " errors");
            }
            continue;
          }
        }
      } else {
        fail(String.format("%s:%d: out of sync: %s\n", file, lineno, line));
      }
    }
    if (input < strings.size()) {
      fail(
          String.format(
              "%s:%d: out of sync: have %d strings left at EOF",
              file,
              lineno,
              strings.size() - input));
    }

    if (nfail > 0) {
      fail(String.format("Of %d cases tested, %d failed", ncase, nfail));
    } else {
      System.err.format("%d cases tested\n", ncase);
    }
  }

  // Returns true iff there are no runes with multibyte UTF-8 encodings in s.
  private static boolean isSingleBytes(String s) {
    for (int i = 0, len = s.length(); i < len; ++i) {
      if (s.charAt(i) >= 0x80) {
        return false;
      }
    }
    return true;
  }

  // Convert |idx16|, which are Java (UTF-16) string indices, into the
  // corresponding indices in the UTF-8 encoding of |text|.
  private static int[] utf16IndicesToUtf8(int[] idx16, String text) {
    try {
      int[] idx8 = new int[idx16.length];
      for (int i = 0; i < idx16.length; ++i) {
        idx8[i] = text.substring(0, idx16[i]).getBytes("UTF-8").length;
      }
      return idx8;
    } catch (java.io.UnsupportedEncodingException e) {
      throw new IllegalStateException(e);
    }
  }

  private static int[] parseResult(String file, int lineno, String res) {
    // A single - indicates no match.
    if (res.equals("-")) {
      return null;
    }
    // Otherwise, a space-separated list of pairs.
    int n = 1;
    // TODO(adonovan): is this safe or must we decode UTF-16?
    int len = res.length();
    for (int j = 0; j < len; j++) {
      if (res.charAt(j) == ' ') {
        n++;
      }
    }
    int[] out = new int[2 * n];
    int i = 0;
    n = 0;
    for (int j = 0; j <= len; j++) {
      if (j == len || res.charAt(j) == ' ') {
        // Process a single pair.  - means no submatch.
        String pair = res.substring(i, j);
        if (pair.equals("-")) {
          out[n++] = -1;
          out[n++] = -1;
        } else {
          int k = pair.indexOf('-');
          if (k < 0) {
            fail(String.format("%s:%d: invalid pair %s", file, lineno, pair));
          }
          int lo = -1, hi = -2;
          try {
            lo = Integer.parseInt(pair.substring(0, k));
            hi = Integer.parseInt(pair.substring(k + 1));
          } catch (NumberFormatException e) {
            /* fall through */
          }
          if (lo > hi) {
            fail(String.format("%s:%d: invalid pair %s", file, lineno, pair));
          }
          out[n++] = lo;
          out[n++] = hi;
        }
        i = j + 1;
      }
    }
    return out;
  }

  // The testFowler* methods run this package's regexp API against the
  // POSIX regular expression tests collected by Glenn Fowler at
  // http://www2.research.att.com/~gsf/testregex/.

  @Test
  public void testFowlerBasic() throws Exception {
    testFowler("basic.dat");
  }

  @Test
  public void testFowlerNullSubexpr() throws Exception {
    testFowler("nullsubexpr.dat");
  }

  @Test
  public void testFowlerRepetition() throws Exception {
    testFowler("repetition.dat");
  }

  private static final RE2 NOTAB = RE2.compilePOSIX("[^\t]+");

  private void testFowler(String file) throws IOException {
    InputStream in = ExecTest.class.getResourceAsStream("/" + file);
    // TODO(adonovan): call in.close() on all paths.
    UNIXBufferedReader r = new UNIXBufferedReader(new InputStreamReader(in, "UTF-8"));
    int lineno = 0;
    int nerr = 0;
    String line;
    String lastRegexp = "";
    while ((line = r.readLine()) != null) {
      lineno++;
      // if (line.isEmpty()) {
      //   fail(String.format("%s:%d: unexpected blank line", file, lineno));
      // }

      // http://www2.research.att.com/~gsf/man/man1/testregex.html
      //
      // INPUT FORMAT
      //   Input lines may be blank, a comment beginning with #, or a test
      //   specification. A specification is five fields separated by one
      //   or more tabs. NULL denotes the empty string and NULL denotes the
      //   0 pointer.
      if (line.isEmpty() || line.charAt(0) == '#') {
        continue;
      }
      List<String> field = NOTAB.findAll(line, -1);
      for (int i = 0; i < field.size(); ++i) {
        if (field.get(i).equals("NULL")) {
          field.set(i, "");
        }
        if (field.get(i).equals("NIL")) {
          System.err.format("%s:%d: skip: %s\n", file, lineno, line);
          continue;
        }
      }
      if (field.isEmpty()) {
        continue;
      }

      //   Field 1: the regex(3) flags to apply, one character per
      //   REG_feature flag. The test is skipped if REG_feature is not
      //   supported by the implementation. If the first character is
      //   not [BEASKLP] then the specification is a global control
      //   line. One or more of [BEASKLP] may be specified; the test
      //   will be repeated for each mode.
      //
      //     B        basic                   BRE     (grep, ed, sed)
      //     E        REG_EXTENDED            ERE     (egrep)
      //     A        REG_AUGMENTED           ARE     (egrep with negation)
      //     S        REG_SHELL               SRE     (sh glob)
      //     K        REG_SHELL|REG_AUGMENTED KRE     (ksh glob)
      //     L        REG_LITERAL             LRE     (fgrep)
      //
      //     a        REG_LEFT|REG_RIGHT      implicit ^...$
      //     b        REG_NOTBOL              lhs does not match ^
      //     c        REG_COMMENT             ignore space and #...\n
      //     d        REG_SHELL_DOT           explicit leading . match
      //     e        REG_NOTEOL              rhs does not match $
      //     f        REG_MULTIPLE            multiple \n separated patterns
      //     g        FNM_LEADING_DIR         testfnmatch only -- match until /
      //     h        REG_MULTIREF            multiple digit backref
      //     i        REG_ICASE               ignore case
      //     j        REG_SPAN                . matches \n
      //     k        REG_ESCAPE              \ to ecape [...] delimiter
      //     l        REG_LEFT                implicit ^...
      //     m        REG_MINIMAL             minimal match
      //     n        REG_NEWLINE             explicit \n match
      //     o        REG_ENCLOSED            (|&) magic inside [@|&](...)
      //     p        REG_SHELL_PATH          explicit / match
      //     q        REG_DELIMITED           delimited pattern
      //     r        REG_RIGHT               implicit ...$
      //     s        REG_SHELL_ESCAPED       \ not special
      //     t        REG_MUSTDELIM           all delimiters must be specified
      //     u        standard unspecified behavior -- errors not counted
      //     v        REG_CLASS_ESCAPE        \ special inside [...]
      //     w        REG_NOSUB               no subexpression match array
      //     x        REG_LENIENT             let some errors slide
      //     y        REG_LEFT                regexec() implicit ^...
      //     z        REG_NULL                NULL subexpressions ok
      //     $                                expand C \c escapes in fields
      //                                      2 and 3
      //     /                                field 2 is a regsubcomp() expr
      //     =                                field 3 is a regdecomp() expr
      //
      //   Field 1 control lines:
      //
      //     C                set LC_COLLATE and LC_CTYPE to locale in field 2
      //
      //     ?test ...        output field 5 if passed and != EXPECTED,
      //                      silent otherwise
      //     &test ...        output field 5 if current and previous passed
      //     |test ...        output field 5 if current passed and
      //                      previous failed
      //     ; ...            output field 2 if previous failed
      //     {test ...        skip if failed until }
      //     }                end of skip
      //
      //     : comment        comment copied as output NOTE
      //     :comment:test    :comment: ignored
      //     N[OTE] comment   comment copied as output NOTE
      //     T[EST] comment   comment
      //
      //     number           use number for nmatch (20 by default)
      String flag = field.get(0);
      switch (flag.charAt(0)) {
        case '?':
        case '&':
        case '|':
        case ';':
        case '{':
        case '}':
          // Ignore all the control operators.
          // Just run everything.
          flag = flag.substring(1);
          if (flag.isEmpty()) {
            continue;
          }
          break;
        case ':':
          {
            int i = flag.indexOf(':', 1);
            if (i < 0) {
              System.err.format("skip: %s\n", line);
              continue;
            }
            flag = flag.substring(1 + i + 1);
            break;
          }
        case 'C':
        case 'N':
        case 'T':
        case '0':
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
        case '8':
        case '9':
          System.err.format("skip: %s\n", line);
          continue;
      }

      // Can check field count now that we've handled the myriad comment
      // formats.
      if (field.size() < 4) {
        System.err.format("%s:%d: too few fields: %s\n", file, lineno, line);
        nerr++;
        continue;
      }

      // Expand C escapes (a.k.a. Go escapes).
      if (flag.indexOf('$') >= 0) {
        String f = "\"" + field.get(1) + "\"";
        try {
          field.set(1, Strconv.unquote(f));
        } catch (Exception e) {
          System.err.format("%s:%d: cannot unquote %s\n", file, lineno, f);
          nerr++;
        }
        f = "\"" + field.get(2) + "\"";
        try {
          field.set(2, Strconv.unquote(f));
        } catch (Exception e) {
          System.err.format("%s:%d: cannot unquote %s\n", file, lineno, f);
          nerr++;
        }
      }

      //   Field 2: the regular expression pattern; SAME uses the pattern from
      //     the previous specification.
      //
      if (field.get(1).equals("SAME")) {
        field.set(1, lastRegexp);
      }
      lastRegexp = field.get(1);

      //   Field 3: the string to match.
      String text = field.get(2);

      //   Field 4: the test outcome...
      boolean[] shouldCompileMatch = {false, false}; // in/out param to parser
      List<Integer> pos;
      try {
        pos = parseFowlerResult(field.get(3), shouldCompileMatch);
      } catch (Exception e) {
        System.err.format("%s:%d: cannot parse result %s\n", file, lineno, field.get(3));
        nerr++;
        continue;
      }

      //   Field 5: optional comment appended to the report.

      // Run test once for each specified capital letter mode that we support.
      for (char c : flag.toCharArray()) {
        String pattern = field.get(1);
        int flags = RE2.POSIX | RE2.CLASS_NL;
        switch (c) {
          default:
            continue;
          case 'E':
            // extended regexp (what we support)
            break;
          case 'L':
            // literal
            pattern = RE2.quoteMeta(pattern);
        }

        if (flag.indexOf('i') >= 0) {
          flags |= RE2.FOLD_CASE;
        }

        RE2 re = null;
        try {
          re = RE2.compileImpl(pattern, flags, true);
        } catch (PatternSyntaxException e) {
          if (shouldCompileMatch[0]) {
            System.err.format("%s:%d: %s did not compile\n", file, lineno, pattern);
            nerr++;
          }
          continue;
        }
        if (!shouldCompileMatch[0]) {
          System.err.format("%s:%d: %s should not compile\n", file, lineno, pattern);
          nerr++;
          continue;
        }
        boolean match = re.match(text);
        if (match != shouldCompileMatch[1]) {
          System.err.format(
              "%s:%d: %s.match(%s) = %s, want %s\n", file, lineno, pattern, text, match, !match);
          nerr++;
          continue;
        }
        int[] haveArray = re.findSubmatchIndex(text);
        if (haveArray == null) {
          haveArray = Utils.EMPTY_INTS; // to make .length and printing safe
        }
        if ((haveArray.length > 0) != match) {
          System.err.format(
              "%s:%d: %s.match(%s) = %s, " + "but %s.findSubmatchIndex(%s) = %s\n",
              file,
              lineno,
              pattern,
              text,
              match,
              pattern,
              text,
              Arrays.toString(haveArray));
          nerr++;
          continue;
        }
        // Convert int[] to List<Integer> and truncate to pos.length.
        List<Integer> have = new ArrayList<Integer>();
        for (int i = 0; i < pos.size(); ++i) {
          have.add(haveArray[i]);
        }
        if (!have.equals(pos)) {
          System.err.format(
              "%s:%d: %s.findSubmatchIndex(%s) = %s, want %s\n",
              file,
              lineno,
              pattern,
              text,
              have,
              pos);
          nerr++;
          continue;
        }
      }
    }
    if (nerr > 0) {
      fail("There were " + nerr + " errors");
    }
  }

  private static List<Integer> parseFowlerResult(String s, boolean[] shouldCompileMatch)
      throws RuntimeException {
    String olds = s;
    //   Field 4: the test outcome. This is either one of the posix error
    //     codes (with REG_ omitted) or the match array, a list of (m,n)
    //     entries with m and n being first and last+1 positions in the
    //     field 3 string, or NULL if REG_NOSUB is in effect and success
    //     is expected. BADPAT is acceptable in place of any regcomp(3)
    //     error code. The match[] array is initialized to (-2,-2) before
    //     each test. All array elements from 0 to nmatch-1 must be specified
    //     in the outcome. Unspecified endpoints (offset -1) are denoted by ?.
    //     Unset endpoints (offset -2) are denoted by X. {x}(o:n) denotes a
    //     matched (?{...}) expression, where x is the text enclosed by {...},
    //     o is the expression ordinal counting from 1, and n is the length of
    //     the unmatched portion of the subject string. If x starts with a
    //     number then that is the return value of re_execf(), otherwise 0 is
    //     returned.
    if (s.isEmpty()) {
      // Match with no position information.
      shouldCompileMatch[0] = true;
      shouldCompileMatch[1] = true;
      return Collections.emptyList();
    } else if (s.equals("NOMATCH")) {
      // Match failure.
      shouldCompileMatch[0] = true;
      shouldCompileMatch[1] = false;
      return Collections.emptyList();
    } else if ('A' <= s.charAt(0) && s.charAt(0) <= 'Z') {
      // All the other error codes are compile errors.
      shouldCompileMatch[0] = false;
      return Collections.emptyList();
    }
    shouldCompileMatch[0] = true;
    shouldCompileMatch[1] = true;

    List<Integer> result = new ArrayList<Integer>();
    while (!s.isEmpty()) {
      char end = ')';
      if ((result.size() % 2) == 0) {
        if (s.charAt(0) != '(') {
          throw new RuntimeException("parse error: missing '('");
        }
        s = s.substring(1);
        end = ',';
      }
      int i = s.indexOf(end);
      if (i <= 0) { // [sic]
        throw new RuntimeException("parse error: missing '" + end + "'");
      }
      String num = s.substring(0, i);
      if (!num.equals("?")) {
        result.add(Integer.valueOf(num)); // (may throw)
      } else {
        result.add(-1);
      }
      s = s.substring(i + 1);
    }
    if ((result.size() % 2) != 0) {
      throw new RuntimeException("parse error: odd number of fields");
    }
    return result;
  }
}
