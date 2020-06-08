// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.re2j;

import com.google.common.truth.Truth;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

/**
 * Some custom asserts and parametric tests.
 *
 * @author afrozm@google.com (Afroz Mohiuddin)
 */
public class ApiTestUtils {

  /**
   * Tests that both RE2's and JDK's pattern class act as we expect them. The regular expression
   * {@code regexp} matches the string {@code match} and doesn't match {@code nonMatch}
   */
  public static void testMatches(String regexp, String match, String nonMatch) {
    String errorString = "Pattern with regexp: " + regexp;
    assertTrue(
        "JDK " + errorString + " doesn't match: " + match,
        java.util.regex.Pattern.matches(regexp, match));
    assertFalse(
        "JDK " + errorString + " matches: " + nonMatch,
        java.util.regex.Pattern.matches(regexp, nonMatch));
    assertTrue(errorString + " doesn't match: " + match, Pattern.matches(regexp, match));
    assertFalse(errorString + " matches: " + nonMatch, Pattern.matches(regexp, nonMatch));
  }

  // Test matches via a matcher.
  public static void testMatcherMatches(String regexp, String match, String nonMatch) {
    testMatcherMatches(regexp, match);
    testMatcherNotMatches(regexp, nonMatch);
  }

  public static void testMatcherMatches(String regexp, String match) {
    java.util.regex.Pattern p = java.util.regex.Pattern.compile(regexp);
    assertTrue(
        "JDK Pattern with regexp: " + regexp + " doesn't match: " + match,
        p.matcher(match).matches());
    Pattern pr = Pattern.compile(regexp);
    assertTrue(
        "Pattern with regexp: " + regexp + " doesn't match: " + match, pr.matcher(match).matches());
  }

  public static void testMatcherNotMatches(String regexp, String nonMatch) {
    java.util.regex.Pattern p = java.util.regex.Pattern.compile(regexp);
    assertFalse(
        "JDK Pattern with regexp: " + regexp + " matches: " + nonMatch,
        p.matcher(nonMatch).matches());
    Pattern pr = Pattern.compile(regexp);
    assertFalse(
        "Pattern with regexp: " + regexp + " matches: " + nonMatch, pr.matcher(nonMatch).matches());
  }

  /**
   * This takes a regex and it's compile time flags, a string that is expected to match the regex
   * and a string that is not expected to match the regex.
   *
   * We don't check for JDK compatibility here, since the flags are not in a 1-1 correspondence.
   *
   */
  public static void testMatchesRE2(String regexp, int flags, String match, String nonMatch) {
    Pattern p = Pattern.compile(regexp, flags);
    String errorString = "Pattern with regexp: " + regexp + " and flags: " + flags;
    assertTrue(errorString + " doesn't match: " + match, p.matches(match));
    assertFalse(errorString + " matches: " + nonMatch, p.matches(nonMatch));
  }

  /**
   * Tests that both RE2 and JDK split the string on the regex in the same way, and that that way
   * matches our expectations.
   */
  public static void testSplit(String regexp, String text, String[] expected) {
    testSplit(regexp, text, 0, expected);
  }

  public static void testSplit(String regexp, String text, int limit, String[] expected) {
    Truth.assertThat(java.util.regex.Pattern.compile(regexp).split(text, limit))
        .isEqualTo(expected);
    Truth.assertThat(Pattern.compile(regexp).split(text, limit)).isEqualTo(expected);
  }

  // Helper methods for RE2Matcher's test.

  // Tests that both RE2 and JDK's Matchers do the same replaceFist.
  public static void testReplaceAll(String orig, String regex, String repl, String actual) {
    Pattern p = Pattern.compile(regex);
    Matcher m = p.matcher(orig);
    String replaced = m.replaceAll(repl);
    assertEquals(actual, replaced);

    // JDK's
    java.util.regex.Pattern pj = java.util.regex.Pattern.compile(regex);
    java.util.regex.Matcher mj = pj.matcher(orig);
    replaced = mj.replaceAll(repl);
    assertEquals(actual, replaced);
  }

  // Tests that both RE2 and JDK's Matchers do the same replaceFist.
  public static void testReplaceFirst(String orig, String regex, String repl, String actual) {
    Pattern p = Pattern.compile(regex);
    Matcher m = p.matcher(orig);
    String replaced = m.replaceFirst(repl);
    assertEquals(actual, replaced);

    // JDK's
    java.util.regex.Pattern pj = java.util.regex.Pattern.compile(regex);
    java.util.regex.Matcher mj = pj.matcher(orig);
    replaced = mj.replaceFirst(repl);
    assertEquals(actual, replaced);
  }

  // Tests that both RE2 and JDK's Patterns/Matchers give the same groupCount.
  public static void testGroupCount(String pattern, int count) {
    // RE2
    Pattern p = Pattern.compile(pattern);
    Matcher m = p.matcher("x");
    assertEquals(count, p.groupCount());
    assertEquals(count, m.groupCount());

    // JDK
    java.util.regex.Pattern pj = java.util.regex.Pattern.compile(pattern);
    java.util.regex.Matcher mj = pj.matcher("x");
    // java.util.regex.Pattern doesn't have group count in JDK.
    assertEquals(count, mj.groupCount());
  }

  public static void testGroup(String text, String regexp, String[] output) {
    // RE2
    Pattern p = Pattern.compile(regexp);
    Matcher matchString = p.matcher(text);
    assertTrue(matchString.find());
    assertEquals(output[0], matchString.group());
    for (int i = 0; i < output.length; i++) {
      assertEquals(output[i], matchString.group(i));
    }
    assertEquals(output.length - 1, matchString.groupCount());

    // JDK
    java.util.regex.Pattern pj = java.util.regex.Pattern.compile(regexp);
    java.util.regex.Matcher matchStringj = pj.matcher(text);
    // java.util.regex.Matcher matchBytes =
    //   p.matcher(text.getBytes(Charsets.UTF_8));
    assertTrue(matchStringj.find());
    // assertEquals(true, matchBytes.find());
    assertEquals(output[0], matchStringj.group());
    // assertEquals(output[0], matchBytes.group());
    for (int i = 0; i < output.length; i++) {
      assertEquals(output[i], matchStringj.group(i));
      // assertEquals(output[i], matchBytes.group(i));
    }
  }

  public static void testFind(String text, String regexp, int start, String output) {
    // RE2
    Pattern p = Pattern.compile(regexp);
    Matcher matchString = p.matcher(text);
    // RE2Matcher matchBytes = p.matcher(text.getBytes(Charsets.UTF_8));
    assertTrue(matchString.find(start));
    // assertTrue(matchBytes.find(start));
    assertEquals(output, matchString.group());
    // assertEquals(output, matchBytes.group());

    // JDK
    java.util.regex.Pattern pj = java.util.regex.Pattern.compile(regexp);
    java.util.regex.Matcher matchStringj = pj.matcher(text);
    assertTrue(matchStringj.find(start));
    assertEquals(output, matchStringj.group());
  }

  public static void testFindNoMatch(String text, String regexp, int start) {
    // RE2
    Pattern p = Pattern.compile(regexp);
    Matcher matchString = p.matcher(text);
    // RE2Matcher matchBytes = p.matcher(text.getBytes(Charsets.UTF_8));
    assertFalse(matchString.find(start));
    // assertFalse(matchBytes.find(start));

    // JDK
    java.util.regex.Pattern pj = java.util.regex.Pattern.compile(regexp);
    java.util.regex.Matcher matchStringj = pj.matcher(text);
    assertFalse(matchStringj.find(start));
  }

  public static void testInvalidGroup(String text, String regexp, int group) {
    Pattern p = Pattern.compile(regexp);
    Matcher m = p.matcher(text);
    m.find();
    m.group(group);
    fail(); // supposed to have exception by now
  }

  public static void verifyLookingAt(String text, String regexp, boolean output) {
    assertEquals(output, Pattern.compile(regexp).matcher(text).lookingAt());
    assertEquals(output, java.util.regex.Pattern.compile(regexp).matcher(text).lookingAt());
  }
}
