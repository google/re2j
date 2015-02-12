package com.google.re2j;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class RE2ReplaceTest {
  private static class ReplaceTestCase {
    private final String pattern;
    private final String replacement;
    private final String source;
    private final String expected;
    private boolean replaceFirst;

    public ReplaceTestCase(String pattern, String replacement, String source, String expected, boolean replaceFirst) {
      this.pattern = pattern;
      this.replacement = replacement;
      this.source = source;
      this.expected = expected;
      this.replaceFirst = replaceFirst;
    }
  }

  private static final ReplaceTestCase[] REPLACE_TESTS = {
    // Test empty input and/or replacement,
    // with pattern that matches the empty string.
    new ReplaceTestCase("", "", "", "", false),
    new ReplaceTestCase("", "x", "", "x", false),
    new ReplaceTestCase("", "", "abc", "abc", false),
    new ReplaceTestCase("", "x", "abc", "xaxbxcx", false),

    // Test empty input and/or replacement,
    // with pattern that does not match the empty string.
    new ReplaceTestCase("b", "", "", "", false),
    new ReplaceTestCase("b", "x", "", "", false),
    new ReplaceTestCase("b", "", "abc", "ac", false),
    new ReplaceTestCase("b", "x", "abc", "axc", false),
    new ReplaceTestCase("y", "", "", "", false),
    new ReplaceTestCase("y", "x", "", "", false),
    new ReplaceTestCase("y", "", "abc", "abc", false),
    new ReplaceTestCase("y", "x", "abc", "abc", false),

    // Multibyte characters -- verify that we don't try to match in the middle
    // of a character.
    new ReplaceTestCase("[a-c]*", "x", "\u65e5", "x\u65e5x", false),
    new ReplaceTestCase("[^\u65e5]", "x", "abc\u65e5def", "xxx\u65e5xxx", false),

    // Start and end of a string.
    new ReplaceTestCase("^[a-c]*", "x", "abcdabc", "xdabc", false),
    new ReplaceTestCase("[a-c]*$", "x", "abcdabc", "abcdx", false),
    new ReplaceTestCase("^[a-c]*$", "x", "abcdabc", "abcdabc", false),
    new ReplaceTestCase("^[a-c]*", "x", "abc", "x", false),
    new ReplaceTestCase("[a-c]*$", "x", "abc", "x", false),
    new ReplaceTestCase("^[a-c]*$", "x", "abc", "x", false),
    new ReplaceTestCase("^[a-c]*", "x", "dabce", "xdabce", false),
    new ReplaceTestCase("[a-c]*$", "x", "dabce", "dabcex", false),
    new ReplaceTestCase("^[a-c]*$", "x", "dabce", "dabce", false),
    new ReplaceTestCase("^[a-c]*", "x", "", "x", false),
    new ReplaceTestCase("[a-c]*$", "x", "", "x", false),
    new ReplaceTestCase("^[a-c]*$", "x", "", "x", false),

    new ReplaceTestCase("^[a-c]+", "x", "abcdabc", "xdabc", false),
    new ReplaceTestCase("[a-c]+$", "x", "abcdabc", "abcdx", false),
    new ReplaceTestCase("^[a-c]+$", "x", "abcdabc", "abcdabc", false),
    new ReplaceTestCase("^[a-c]+", "x", "abc", "x", false),
    new ReplaceTestCase("[a-c]+$", "x", "abc", "x", false),
    new ReplaceTestCase("^[a-c]+$", "x", "abc", "x", false),
    new ReplaceTestCase("^[a-c]+", "x", "dabce", "dabce", false),
    new ReplaceTestCase("[a-c]+$", "x", "dabce", "dabce", false),
    new ReplaceTestCase("^[a-c]+$", "x", "dabce", "dabce", false),
    new ReplaceTestCase("^[a-c]+", "x", "", "", false),
    new ReplaceTestCase("[a-c]+$", "x", "", "", false),
    new ReplaceTestCase("^[a-c]+$", "x", "", "", false),

    // Other cases.
    new ReplaceTestCase("abc", "def", "abcdefg", "defdefg", false),
    new ReplaceTestCase("bc", "BC", "abcbcdcdedef", "aBCBCdcdedef", false),
    new ReplaceTestCase("abc", "", "abcdabc", "d", false),
    new ReplaceTestCase("x", "xXx", "xxxXxxx", "xXxxXxxXxXxXxxXxxXx", false),
    new ReplaceTestCase("abc", "d", "", "", false),
    new ReplaceTestCase("abc", "d", "abc", "d", false),
    new ReplaceTestCase(".+", "x", "abc", "x", false),
    new ReplaceTestCase("[a-c]*", "x", "def", "xdxexfx", false),
    new ReplaceTestCase("[a-c]+", "x", "abcbcdcdedef", "xdxdedef", false),
    new ReplaceTestCase("[a-c]*", "x", "abcbcdcdedef", "xdxdxexdxexfx", false),

    // Test empty input and/or replacement,
    // with pattern that matches the empty string.
    new ReplaceTestCase("", "", "", "", true),
    new ReplaceTestCase("", "x", "", "x", true),
    new ReplaceTestCase("", "", "abc", "abc", true),
    new ReplaceTestCase("", "x", "abc", "xabc", true),

    // Test empty input and/or replacement,
    // with pattern that does not match the empty string.
    new ReplaceTestCase("b", "", "", "", true),
    new ReplaceTestCase("b", "x", "", "", true),
    new ReplaceTestCase("b", "", "abc", "ac", true),
    new ReplaceTestCase("b", "x", "abc", "axc", true),
    new ReplaceTestCase("y", "", "", "", true),
    new ReplaceTestCase("y", "x", "", "", true),
    new ReplaceTestCase("y", "", "abc", "abc", true),
    new ReplaceTestCase("y", "x", "abc", "abc", true),

    // Multibyte characters -- verify that we don't try to match in the middle
    // of a character.
    new ReplaceTestCase("[a-c]*", "x", "\u65e5", "x\u65e5", true),
    new ReplaceTestCase("[^\u65e5]", "x", "abc\u65e5def", "xbc\u65e5def", true),

    // Start and end of a string.
    new ReplaceTestCase("^[a-c]*", "x", "abcdabc", "xdabc", true),
    new ReplaceTestCase("[a-c]*$", "x", "abcdabc", "abcdx", true),
    new ReplaceTestCase("^[a-c]*$", "x", "abcdabc", "abcdabc", true),
    new ReplaceTestCase("^[a-c]*", "x", "abc", "x", true),
    new ReplaceTestCase("[a-c]*$", "x", "abc", "x", true),
    new ReplaceTestCase("^[a-c]*$", "x", "abc", "x", true),
    new ReplaceTestCase("^[a-c]*", "x", "dabce", "xdabce", true),
    new ReplaceTestCase("[a-c]*$", "x", "dabce", "dabcex", true),
    new ReplaceTestCase("^[a-c]*$", "x", "dabce", "dabce", true),
    new ReplaceTestCase("^[a-c]*", "x", "", "x", true),
    new ReplaceTestCase("[a-c]*$", "x", "", "x", true),
    new ReplaceTestCase("^[a-c]*$", "x", "", "x", true),

    new ReplaceTestCase("^[a-c]+", "x", "abcdabc", "xdabc", true),
    new ReplaceTestCase("[a-c]+$", "x", "abcdabc", "abcdx", true),
    new ReplaceTestCase("^[a-c]+$", "x", "abcdabc", "abcdabc", true),
    new ReplaceTestCase("^[a-c]+", "x", "abc", "x", true),
    new ReplaceTestCase("[a-c]+$", "x", "abc", "x", true),
    new ReplaceTestCase("^[a-c]+$", "x", "abc", "x", true),
    new ReplaceTestCase("^[a-c]+", "x", "dabce", "dabce", true),
    new ReplaceTestCase("[a-c]+$", "x", "dabce", "dabce", true),
    new ReplaceTestCase("^[a-c]+$", "x", "dabce", "dabce", true),
    new ReplaceTestCase("^[a-c]+", "x", "", "", true),
    new ReplaceTestCase("[a-c]+$", "x", "", "", true),
    new ReplaceTestCase("^[a-c]+$", "x", "", "", true),

    // Other cases.
    new ReplaceTestCase("abc", "def", "abcdefg", "defdefg", true),
    new ReplaceTestCase("bc", "BC", "abcbcdcdedef", "aBCbcdcdedef", true),
    new ReplaceTestCase("abc", "", "abcdabc", "dabc", true),
    new ReplaceTestCase("x", "xXx", "xxxXxxx", "xXxxxXxxx", true),
    new ReplaceTestCase("abc", "d", "", "", true),
    new ReplaceTestCase("abc", "d", "abc", "d", true),
    new ReplaceTestCase(".+", "x", "abc", "x", true),
    new ReplaceTestCase("[a-c]*", "x", "def", "xdef", true),
    new ReplaceTestCase("[a-c]+", "x", "abcbcdcdedef", "xdcdedef", true),
    new ReplaceTestCase("[a-c]*", "x", "abcbcdcdedef", "xdcdedef", true),
  };

  @Parameters
  public static ReplaceTestCase[] replaceTests() {
    return REPLACE_TESTS;
  }

  private ReplaceTestCase testCase;

  public RE2ReplaceTest(ReplaceTestCase testCase) {
    this.testCase = testCase;
  }

  @Test
  public void replaceTestHelper() {
    RE2 re = null;
    try {
      re = RE2.compile(testCase.pattern);
    } catch (PatternSyntaxException e) {
      fail(String.format("Unexpected error compiling %s: %s", testCase.pattern, e.getMessage()));
    }
    String actual = testCase.replaceFirst ? re.replaceFirst(testCase.source, testCase.replacement)
        : re.replaceAll(testCase.source, testCase.replacement);
    if (!actual.equals(testCase.expected)) {
      fail(String.format("%s.replaceAll(%s,%s) = %s; want %s", testCase.pattern, testCase.source,
          testCase.replacement, actual, testCase.expected));
    }
  }
}
