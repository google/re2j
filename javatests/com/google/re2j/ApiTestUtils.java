// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.re2j;

import com.google.common.base.Function;

import static com.google.common.collect.Lists.transform;
import static io.airlift.slice.Slices.utf8Slice;
import static java.util.Arrays.asList;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import io.airlift.slice.Slice;

/**
 * Some custom asserts and parametric tests.
 *
 * @author afrozm@google.com (Afroz Mohiuddin)
 */
public class ApiTestUtils {

  /**
   * Asserts that IllegalArgumentException is thrown from compile with flags.
   */
  public static void assertCompileFails(String regex, int flag) {
    try {
      Pattern.compile(regex, flag);
      fail("Compiling Pattern with regex: " + regex + " and flag: " + flag +
           " passed, when it should have failed.");
    } catch (IllegalArgumentException e) {
      if (!"Flags UNIX_LINES and COMMENTS unsupported".equals(e.getMessage())) {
        throw e;
      }
    }
  }

  /**
   * Asserts all strings in array equal.
   */
  public static void assertArrayEquals(Object[] expected, Object[] actual) {
    assertEquals(
        "Arrays have unequal length, therefore can't be equal to " +
        "each other. Expected: " + Arrays.toString(expected) + " Actual: " +
        Arrays.toString(actual),
        expected.length, actual.length);
    for (int idx = 0; idx < expected.length; ++idx) {
      assertEquals("Index: " + idx + " is unequal in the arrays",
                   expected[idx], actual[idx]);
    }
  }

  public static Slice[] stringsToSlices(String[] strings) {
    Slice[] slices = new Slice[strings.length];
    for (int i = 0; i < strings.length; ++i) {
      slices[i] = utf8Slice(strings[i]);
    }
    return slices;
  }

  public static List<String> slicesToStrings(Slice[] slices) {
    return slices == null ? null : slicesToStrings(asList(slices));
  }

  public static List<String> slicesToStrings(List<Slice> slices) {
    return slices == null ? null : transform(slices, new Function<Slice, String>() {
      @Override
      public String apply(Slice input) {
        return input == null ? null : input.toStringUtf8();
      }
    });
  }

  /**
   * Tests that both RE2's and JDK's pattern class act as we expect them.
   * The regular expression {@code regexp} matches the string {@code match} and
   * doesn't match {@code nonMatch}
   *
   * @param regexp
   * @param match
   * @param nonMatch
   */
  public static void testMatches(String regexp, String match, String nonMatch) {
    String errorString = "Pattern with regexp: " + regexp;
    assertTrue("JDK " + errorString + " doesn't match: " + match,
        java.util.regex.Pattern.matches(regexp, match));
    assertFalse("JDK " + errorString + " matches: " + nonMatch,
        java.util.regex.Pattern.matches(regexp, nonMatch));
    assertTrue(errorString + " doesn't match: " + match,
               Pattern.matches(regexp, utf8Slice(match)));
    assertFalse(errorString + " matches: " + nonMatch,
                Pattern.matches(regexp, utf8Slice(nonMatch)));
  }

  // Test matches via a matcher.
  public static void testMatcherMatches(String regexp, String match,
                                        String nonMatch) {
    testMatcherMatches(regexp, match);
    testMatcherNotMatches(regexp, nonMatch);
  }

  public static void testMatcherMatches(String regexp, String match) {
    java.util.regex.Pattern p = java.util.regex.Pattern.compile(regexp);
    assertTrue("JDK Pattern with regexp: " + regexp + " doesn't match: " +
        match, p.matcher(match).matches());
    Pattern pr = Pattern.compile(regexp);
    assertTrue("Pattern with regexp: " + regexp + " doesn't match: " + match,
        pr.matcher(utf8Slice(match)).matches());
  }
  
  public static void testMatcherNotMatches(String regexp, String nonMatch) {
    java.util.regex.Pattern p = java.util.regex.Pattern.compile(regexp);
    assertFalse("JDK Pattern with regexp: " + regexp + " matches: " + nonMatch,
        p.matcher(nonMatch).matches());
    Pattern pr = Pattern.compile(regexp);
    assertFalse("Pattern with regexp: " + regexp + " matches: " + nonMatch,
        pr.matcher(utf8Slice(nonMatch)).matches());
  }
  
  /**
   * This takes a regex and it's compile time flags, a string that is expected
   * to match the regex and a string that is not expected to match the regex.
   *
   * We don't check for JDK compatibility here, since the flags are not in a 1-1
   * correspondence.
   *
   */
  public static void testMatchesRE2(String regexp, int flags, String match,
                                    String nonMatch) {
    Pattern p = Pattern.compile(regexp, flags);
    String errorString =
        "Pattern with regexp: " + regexp + " and flags: " + flags;
    assertTrue(errorString + " doesn't match: " + match, p.matches(utf8Slice(match)));
    assertFalse(errorString + " matches: " + nonMatch, p.matches(utf8Slice(nonMatch)));
  }

  public static void testMatchesRE2(String regexp, int flags, String[] matches,
      String[] nonMatches) {
    Pattern p = Pattern.compile(regexp, flags);
    for (String s : matches) {
      assertTrue(p.matches(utf8Slice(s)));
    }
    for (String s : nonMatches) {
      assertFalse(p.matches(utf8Slice(s)));
    }
  }

  /**
   * Tests that both RE2 and JDK split the string on the regex in the same way,
   * and that that way matches our expectations.
   */
  public static void testSplit(String regexp, String text, String[] expected) {
    testSplit(regexp, text, 0, expected);
  }

  public static void testSplit(String regexp, String text, int limit,
                               String[] expected) {
    assertArrayEquals(expected,
        java.util.regex.Pattern.compile(regexp).split(text, limit));
    assertArrayEquals(stringsToSlices(expected), Pattern.compile(regexp).split(utf8Slice(text), limit));
  }

  // Helper methods for RE2Matcher's test.

  // Tests that both RE2 and JDK's Matchers do the same replaceFist.
  public static void testReplaceAll(String orig, String regex, String repl,
                                    String actual) {
    Pattern p = Pattern.compile(regex);
    Matcher m = p.matcher(utf8Slice(orig));
    String replaced = m.replaceAll(utf8Slice(repl)).toStringUtf8();
    assertEquals(actual, replaced);

    // JDK's
    java.util.regex.Pattern pj = java.util.regex.Pattern.compile(regex);
    java.util.regex.Matcher mj = pj.matcher(orig);
    replaced = mj.replaceAll(repl);
    assertEquals(actual, replaced);
  }

  // Tests that both RE2 and JDK's Matchers do the same replaceFist.
  public static void testReplaceFirst(String orig, String regex, String repl,
                                      String actual) {
    Pattern p = Pattern.compile(regex);
    Matcher m = p.matcher(utf8Slice(orig));
    String replaced = m.replaceFirst(utf8Slice(repl)).toStringUtf8();
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
    Matcher m = p.matcher(utf8Slice("x"));
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
    Matcher matchString = p.matcher(utf8Slice(text));
    assertEquals(true, matchString.find());
    assertEquals(utf8Slice(output[0]), matchString.group());
    for (int i = 0; i < output.length; i++) {
      assertEquals(output[i] == null ? null : utf8Slice(output[i]), matchString.group(i));
    }
    assertEquals(output.length - 1, matchString.groupCount());

    // JDK
    java.util.regex.Pattern pj = java.util.regex.Pattern.compile(regexp);
    java.util.regex.Matcher matchStringj = pj.matcher(text);
    // java.util.regex.Matcher matchBytes =
    //   p.matcher(text.getBytes(Charsets.UTF_8));
    assertEquals(true, matchStringj.find());
    // assertEquals(true, matchBytes.find());
    assertEquals(output[0], matchStringj.group());
    // assertEquals(output[0], matchBytes.group());
    for (int i = 0; i < output.length; i++) {
      assertEquals(output[i], matchStringj.group(i));
      // assertEquals(output[i], matchBytes.group(i));
    }
  }


  public static void testFind(String text, String regexp, int start,
                              String output) {
    // RE2
    Pattern p = Pattern.compile(regexp);
    Matcher matchString = p.matcher(utf8Slice(text));
    // RE2Matcher matchBytes = p.matcher(text.getBytes(Charsets.UTF_8));
    assertTrue(matchString.find(start));
    // assertTrue(matchBytes.find(start));
    assertEquals(utf8Slice(output), matchString.group());
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
    Matcher matchString = p.matcher(utf8Slice(text));
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
    Matcher m = p.matcher(utf8Slice(text));
    m.find();
    m.group(group);
    fail();  // supposed to have exception by now
  }

  public static void verifyLookingAt(String text, String regexp, boolean output) {
    assertEquals(output, Pattern.compile(regexp).matcher(utf8Slice(text)).lookingAt());
    assertEquals(output, java.util.regex.Pattern.compile(regexp).matcher(text).lookingAt());
  }
}
