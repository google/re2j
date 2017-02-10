// Copyright 2010 Google Inc. All Rights Reserved.

package com.google.re2j;

import com.google.re2j.RE2.Anchor;

import io.airlift.slice.DynamicSliceOutput;
import io.airlift.slice.Slice;
import io.airlift.slice.SliceOutput;

import static com.google.re2j.RE2.Anchor.ANCHOR_BOTH;
import static com.google.re2j.RE2.Anchor.ANCHOR_START;
import static com.google.re2j.RE2.Anchor.UNANCHORED;

/**
 * A stateful iterator that interprets a regex {@code Pattern} on a
 * specific input.  Its interface mimics the JDK 1.4.2
 * {@code java.util.regex.Matcher}.
 *
 * <p>Conceptually, a Matcher consists of four parts:
 * <ol>
 *   <li>A compiled regular expression {@code Pattern}, set at
 *   construction and fixed for the lifetime of the matcher.</li>
 *
 *   <li>The remainder of the input string, set at construction or
 *   {@link #reset()} and advanced by each match operation such as
 *   {@link #find}, {@link #matches} or {@link #lookingAt}.</li>
 *
 *   <li>The current match information, accessible via {@link #start},
 *   {@link #end}, and {@link #group}, and updated by each match
 *   operation.</li>
 *
 *   <li>The append position, used and advanced by
 *   {@link #appendReplacement} and {@link #appendTail} if performing a
 *   search and replace from the input to an external {@code StringBuffer}.
 *
 * </ol>
 *
 * <p>See the <a href="package-summary.html">package-level
 * documentation</a> for an overview of how to use this API.</p>
 *
 * @author rsc@google.com (Russ Cox)
 */
public final class Matcher {
  // The pattern being matched.
  private final Pattern pattern;

  // The group indexes, in [start, end) pairs.  Zeroth pair is overall match.
  private final int[] groups;

  // The number of submatches (groups) in the pattern.
  private final int groupCount;

  private Slice input;

  // The append position: where the next append should start.
  private int appendPos;

  // Is there a current match?
  private boolean hasMatch;

  // Have we found the submatches (groups) of the current match?
  // group[0], group[1] are set regardless.
  private boolean hasGroups;

  // The anchor flag to use when repeating the match to find subgroups.
  private Anchor anchorFlag;

  private Matcher(Pattern pattern) {
    if (pattern == null) {
      throw new NullPointerException("pattern is null");
    }
    this.pattern = pattern;
    RE2 re2 = pattern.re2();
    groupCount = re2.numberOfCapturingGroups();
    groups = new int[2 + 2 * groupCount];
  }

  /** Creates a new {@code Matcher} with the given pattern and input. */
  Matcher(Pattern pattern, Slice input) {
    this(pattern);
    reset(input);
  }

  /** Returns the {@code Pattern} associated with this {@code Matcher}. */
  public Pattern pattern() {
    return pattern;
  }

  /**
   * Resets the {@code Matcher}, rewinding input and
   * discarding any match information.
   *
   * @return the {@code Matcher} itself, for chained method calls
   */
  public Matcher reset() {
    appendPos = 0;
    hasMatch = false;
    hasGroups = false;
    return this;
  }

  /**
   * Resets the {@code Matcher} and changes the input.
   *
   * @param input the new input {@link Slice}
   * @return the {@code Matcher} itself, for chained method calls
   */
  public Matcher reset(Slice input) {
    if (input == null) {
      throw new NullPointerException("input is null");
    }
    reset();
    this.input = input;
    return this;
  }

  /**
   * Returns the start position of the most recent match.
   *
   * @throws IllegalStateException if there is no match
   */
  public int start() {
    return start(0);
  }

  /**
   * Returns the end position of the most recent match.
   *
   * @throws IllegalStateException if there is no match
   */
  public int end() {
    return end(0);
  }

  /**
   * Returns the start position of a subgroup of the most recent match.
   *
   * @param group the group index; 0 is the overall match
   * @throws IllegalStateException if there is no match
   * @throws IndexOutOfBoundsException
   *   if {@code group < 0} or {@code group > groupCount()}
   */
  public int start(int group) {
    loadGroup(group);
    return groups[2 * group];
  }

  /**
   * Returns the end position of a subgroup of the most recent match.
   *
   * @param group the group index; 0 is the overall match
   * @throws IllegalStateException if there is no match
   * @throws IndexOutOfBoundsException
   *   if {@code group < 0} or {@code group > groupCount()}
   */
  public int end(int group) {
    loadGroup(group);
    return groups[2 * group + 1];
  }

  /**
   * Returns the most recent match.
   *
   * @throws IllegalStateException if there is no match
   */
  public Slice group() {
    return group(0);
  }

  /**
   * Returns the subgroup of the most recent match.
   *
   * @throws IllegalStateException if there is no match
   * @throws IndexOutOfBoundsException if {@code group < 0}
   *   or {@code group > groupCount()}
   */
  public Slice group(int group) {
    int start = start(group);
    int end = end(group);
    if (start < 0 && end < 0) {
      // Means the subpattern didn't get matched at all.
      return null;
    }
    return input.slice(start, end - start);
  }

  /**
   * Returns the number of subgroups in this pattern.
   *
   * @return the number of subgroups; the overall match (group 0) does not count
   */
  public int groupCount() {
    return groupCount;
  }

  /** Helper: finds subgroup information if needed for group. */
  private void loadGroup(int group) {
    if (group < 0 || group > groupCount) {
      throw new IndexOutOfBoundsException(
          "Group index out of bounds: " + group);
    }
    if (!hasMatch) {
      throw new IllegalStateException("perhaps no match attempted");
    }
    if (group == 0 || hasGroups) {
      return;
    }

    // Include the character after the matched text (if there is one).
    // This is necessary in the case of inputSequence abc and pattern
    // (a)(b$)?(b)? . If we do pass in the trailing c,
    // the groups evaluate to new String[] {"ab", "a", null, "b" }
    // If we don't, they evaluate to new String[] {"ab", "a", "b", null}
    // We know it won't affect the total matched because the previous call
    // to match included the extra character, and it was not matched then.
    int end = groups[1] + 1;
    if (end > input.length()) {
      end = input.length();
    }

    boolean ok = pattern.re2().match(input.slice(0, end), groups[0],
      anchorFlag, groups, 1 + groupCount);
    // Must match - hasMatch says that the last call with these
    // parameters worked just fine.
    if (!ok) {
      throw new IllegalStateException("inconsistency in matching group data");
    }
    hasGroups = true;
  }

  /**
   * Matches the entire input against the pattern (anchored start and end).
   * If there is a match, {@code matches} sets the match state to describe it.
   *
   * @return true if the entire input matches the pattern
   */
  public boolean matches() {
    return genMatch(0, ANCHOR_BOTH);
  }

  /**
   * Matches the beginning of input against the pattern (anchored start).
   * If there is a match, {@code lookingAt} sets the match state to describe it.
   *
   * @return true if the beginning of the input matches the pattern
   */
  public boolean lookingAt() {
    return genMatch(0, ANCHOR_START);
  }

  /**
   * Matches the input against the pattern (unanchored).
   * The search begins at the end of the last match, or else the beginning
   * of the input.
   * If there is a match, {@code find} sets the match state to describe it.
   *
   * @return true if it finds a match
   */
  public boolean find() {
    int start = 0;
    if (hasMatch) {
      start = groups[1];
      if (groups[0] == groups[1]) {  // empty match - nudge forward
        start++;
      }
    }
    return genMatch(start, UNANCHORED);
  }

  /**
   * Matches the input against the pattern (unanchored),
   * starting at a specified position.
   * If there is a match, {@code find} sets the match state to describe it.
   *
   * @param start the input position where the search begins
   * @return true if it finds a match
   * @throws IndexOutOfBoundsException if start is not a valid input position
   */
  public boolean find(int start) {
    if (start < 0 || start > input.length()) {
      throw new IndexOutOfBoundsException(
          "start index out of bounds: " + start);
    }
    reset();
    return genMatch(start, UNANCHORED);
  }

  /** Helper: does match starting at start, with RE2 anchor flag. */
  private boolean genMatch(int startByte, Anchor anchor) {
    // TODO(rsc): Is matches/lookingAt supposed to reset the append or input positions?
    // From the JDK docs, looks like no.
    boolean ok = pattern.re2().match(input, startByte,
        anchor, groups, 1);
    if (!ok) {
      return false;
    }
    hasMatch = true;
    hasGroups = false;
    anchorFlag = anchor;

    return true;
  }

  /** Helper: return substring for [start, end). */
  Slice substring(int start, int end) {
    return input.slice(start, end - start);
  }

  /** Helper for Pattern: return input length. */
  int inputLength() {
    return input.length();
  }

  /**
   * Appends to {@code so} two slices: the text from the append position up
   * to the beginning of the most recent match, and then the replacement with
   * submatch groups substituted for references of the form {@code $n}, where
   * {@code n} is the group number in decimal.  It advances the append position
   * to the position where the most recent match ended.
   *
   * <p>To embed a literal {@code $}, use \$ (actually {@code "\\$"} with string
   * escapes).  The escape is only necessary when {@code $} is followed by a
   * digit, but it is always allowed.  Only {@code $} and {@code \} need
   * escaping, but any character can be escaped.
   *
   * <p>The group number {@code n} in {@code $n} is always at least one digit
   * and expands to use more digits as long as the resulting number is a
   * valid group number for this pattern.  To cut it off earlier, escape the
   * first digit that should not be used.
   *
   * @param so the {@link SliceOutput} to append to
   * @param replacement the replacement {@link Slice}
   * @return the {@code Matcher} itself, for chained method calls
   * @throws IllegalStateException if there was no most recent match
   * @throws IndexOutOfBoundsException if replacement refers to an invalid group
   * @throws IllegalArgumentException if replacement has incorrect format
   */
  public Matcher appendReplacement(SliceOutput so, Slice replacement) {
    int s = start();
    int e = end();
    if (appendPos < s) {
      so.writeBytes(input, appendPos, s - appendPos);
    }
    appendPos = e;
    SliceUtils.appendReplacement(so, replacement, this);
    return this;
  }

  /**
   * Appends to {@code so} the subslice of the input from the
   * append position to the end of the input.
   *
   * @param so the {@link SliceOutput} to append to
   * @return the argument {@code so}, for method chaining
   */
  public SliceOutput appendTail(SliceOutput so) {
    so.writeBytes(input, appendPos, input.length() - appendPos);
    return so;
  }

  /**
   * Returns the input with all matches replaced by {@code replacement},
   * interpreted as for {@code appendReplacement}.
   *
   * @param replacement the replacement {@link Slice}
   * @return the input {@link Slice} with the matches replaced
   * @throws IndexOutOfBoundsException if replacement refers to an invalid group
   */
  public Slice replaceAll(Slice replacement) {
    return replace(replacement, true);
  }

  /**
   * Returns the input with the first match replaced by {@code replacement},
   * interpreted as for {@code appendReplacement}.
   *
   * @param replacement the replacement {@link Slice}
   * @return the input {@link Slice} with the first match replaced
   * @throws IndexOutOfBoundsException if replacement refers to an invalid group
   */
  public Slice replaceFirst(Slice replacement) {
    return replace(replacement, false);
  }

  /** Helper: replaceAll/replaceFirst hybrid. */
  private Slice replace(Slice replacement, boolean all) {
    reset();
    SliceOutput so = new DynamicSliceOutput(input.length());
    while (find()) {
      appendReplacement(so, replacement);
      if (!all) {
        break;
      }
    }
    appendTail(so);
    return so.slice();
  }
}
