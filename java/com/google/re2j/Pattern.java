// Copyright 2010 Google Inc. All Rights Reserved.

package com.google.re2j;

import java.io.Serializable;

import io.airlift.slice.Slice;

import static com.google.re2j.Options.DEFAULT_OPTIONS;
import static com.google.re2j.RE2.MatchKind.FIRST_MATCH;

/**
 * A compiled representation of an RE2 regular expression, mimicking the
 * {@code java.util.regex.Pattern} API.
 *
 * <p>The matching functions take {@code String} arguments instead of
 * the more general Java {@code CharSequence} since the latter doesn't
 * provide UTF-16 decoding.
 *
 * <p>See the <a href='package-summary.html'>package-level
 * documentation</a> for an overview of how to use this API.</p>
 *
 * @author rsc@google.com (Russ Cox)
 */
public final class Pattern implements Serializable {
  /** Flag: case insensitive matching. */
  public static final int CASE_INSENSITIVE = 1;

  /** Flag: dot ({@code .}) matches all characters, including newline. */
  public static final int DOTALL = 2;

  /**
   * Flag: multiline matching: {@code ^} and {@code $} match at
   * beginning and end of line, not just beginning and end of input.
   */
  public static final int MULTILINE = 4;

  /**
   * Flag: Unicode groups (e.g. {@code \p\{Greek\}}) will be syntax errors.
   */
  public static final int DISABLE_UNICODE_GROUPS = 8;

  // The pattern string at construction time.
  private final String pattern;

  // The flags at construction time.
  private final int flags;

  // The options at construction time.
  private final Options options;

  // The compiled RE2 regexp.
  private transient final RE2 re2;

  // This is visible for testing.
  Pattern(String pattern, int flags, RE2 re2, Options options) {
    if (pattern == null) {
      throw new NullPointerException("pattern is null");
    }
    if (re2 == null) {
      throw new NullPointerException("re2 is null");
    }
    this.pattern = pattern;
    this.flags = flags;
    this.options = options;
    this.re2 = re2;
  }

  /**
   * Returns the flags used in the constructor.
   */
  public int flags() {
    return flags;
  }

  /**
   * Returns the options used in the constructor.
   */
  public Options options() {
    return options;
  }

  /**
   * Returns the pattern used in the constructor.
   */
  public String pattern() {
    return pattern;
  }

  RE2 re2() {
    return re2;
  }

  /**
   * Creates and returns a new {@code Pattern} corresponding to
   * compiling {@code regex} with the default flags (0).
   *
   * @param regex the regular expression
   * @throws PatternSyntaxException if the pattern is malformed
   */
  public static Pattern compile(String regex) {
    return compile(regex, DEFAULT_OPTIONS);
  }

  public static Pattern compile(String regex, Options options) {
    return compile(regex, regex, 0, options);
  }

  /**
   * Creates and returns a new {@code Pattern} corresponding to
   * compiling {@code regex} with the default flags (0).
   *
   * @param regex the regular expression
   * @param flags bitwise OR of the flag constants {@code CASE_INSENSITIVE},
   *    {@code DOTALL}, and {@code MULTILINE}
   * @throws PatternSyntaxException if the regular expression is malformed
   * @throws IllegalArgumentException if an unknown flag is given
   */
  public static Pattern compile(String regex, int flags) {
    return compile(regex, flags, DEFAULT_OPTIONS);
  }

  public static Pattern compile(String regex, int flags, Options options) {
    String flregex = regex;
    if ((flags & CASE_INSENSITIVE) != 0) {
      flregex = "(?i)" + flregex;
    }
    if ((flags & DOTALL) != 0) {
      flregex = "(?s)" + flregex;
    }
    if ((flags & MULTILINE) != 0) {
      flregex = "(?m)" + flregex;
    }
    if ((flags & ~(MULTILINE | DOTALL | CASE_INSENSITIVE | DISABLE_UNICODE_GROUPS)) != 0) {
      throw new IllegalArgumentException("Flags should only be a combination " +
          "of MULTILINE, DOTALL, CASE_INSENSITIVE, DISABLE_UNICODE_GROUPS");
    }
    return compile(flregex, regex, flags, options);
  }

  /**
   * Helper: create new Pattern with given regex and flags.
   * Flregex is the regex with flags applied.
   */
  private static Pattern compile(String flregex, String regex, int flags, Options options) {
    int re2Flags = RE2.PERL;
    if ((flags & DISABLE_UNICODE_GROUPS) != 0) {
      re2Flags &= ~RE2.UNICODE_GROUPS;
    }
    return new Pattern(regex, flags, RE2.compileImpl(flregex, re2Flags, FIRST_MATCH, options), options);
  }

  /**
   * Matches a {@link Slice} against a regular expression.
   *
   * @param regex the regular expression
   * @param input the input
   * @return true if the regular expression matches the entire input
   * @throws PatternSyntaxException if the regular expression is malformed
   */
  public static boolean matches(String regex, Slice input) {
    return matches(regex, input, DEFAULT_OPTIONS);
  }

  public static boolean matches(String regex, Slice input, Options options) {
    return compile(regex, options).matcher(input).matches();
  }

  public boolean matches(Slice input) {
    return this.matcher(input).matches();
  }

  /**
   * Creates a new {@code Matcher} matching the pattern against the input.
   *
   * @param input the input {@link Slice}
   */
  public Matcher matcher(Slice input) {
    return new Matcher(this, input);
  }

  /**
   * Splits input around instances of the regular expression.
   * It returns an array giving the {@link Slice}s that occur before, between, and after instances
   * of the regular expression.  Empty {@link Slice}s that would occur at the end
   * of the array are omitted.
   *
   * @param input the input {@link Slice} to be split
   * @return the split {@link Slice}s
   */
  public Slice[] split(Slice input) {
    return split(input, 0);
  }

  /**
   * Splits input around instances of the regular expression.
   * It returns an array giving the {@link Slice}s that occur before, between, and after instances
   * of the regular expression.
   *
   * <p>If {@code limit <= 0}, there is no limit on the size of the returned array.
   * If {@code limit == 0}, empty {@link Slice}s that would occur at the end of the array are omitted.
   * If {@code limit > 0}, at most limit {@link Slice}s are returned.  The final {@link Slice} contains
   * the remainder of the input, possibly including additional matches of the pattern.
   *
   * @param input the input {@link Slice} to be split
   * @param limit the limit
   * @return the split {@link Slice}s
   */
  public Slice[] split(Slice input, int limit) {
    return split(new Matcher(this, input), limit);
  }

  /** Helper: run split on m's input. */
  private Slice[] split(Matcher m, int limit) {
    int matchCount = 0;
    int arraySize = 0;
    int last = 0;
    while (m.find()) {
      matchCount++;
      if (limit != 0 || last < m.start()) {
        arraySize = matchCount;
      }
      last = m.end();
    }
    if (last < m.inputLength() || limit != 0) {
      matchCount++;
      arraySize = matchCount;
    }

    int trunc = 0;
    if (limit > 0 && arraySize > limit) {
      arraySize = limit;
      trunc = 1;
    }
    Slice[] array = new Slice[arraySize];
    int i = 0;
    last = 0;
    m.reset();
    while (m.find() && i < arraySize - trunc) {
      array[i++] = m.substring(last, m.start());
      last = m.end();
    }
    if (i < arraySize) {
      array[i] = m.substring(last, m.inputLength());
    }
    return array;
  }

  /**
   * Returns a literal pattern string for the specified
   * string.
   *
   * <p>This method produces a string that can be used to
   * create a <code>Pattern</code> that would match the string
   * <code>s</code> as if it were a literal pattern.</p> Metacharacters
   * or escape sequences in the input sequence will be given no special
   * meaning.
   *
   * @param s The string to be literalized
   * @return A literal string replacement
   */
  public static String quote(String s) {
    return RE2.quoteMeta(s);
  }

  @Override
  public String toString() {
    return pattern;
  }

  /**
   * Returns the number of capturing groups in this matcher's pattern.
   * Group zero denotes the enture pattern and is excluded from this count.
   *
   * @return the number of capturing groups in this pattern
   */
  public int groupCount() {
    return re2.numberOfCapturingGroups();
  }

  Object readResolve() {
    // The deserialized version will be missing the RE2 instance, so we need to create a new,
    // compiled version.
    return Pattern.compile(pattern, flags, options);
  }

  private static final long serialVersionUID = 0;
}
