/*
 * Copyright (c) 2020 The Go Authors. All rights reserved.
 *
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
// Original Go source here:
// http://code.google.com/p/go/source/browse/src/pkg/regexp/syntax/all_test.go

package com.google.re2j;

import com.google.common.truth.Truth;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Tests of RE2 API. */
public class RE2Test {
  @Test
  public void testFullMatch() {
    assertTrue(new RE2("ab+c").match("abbbbbc", 0, 7, RE2.ANCHOR_BOTH, null, 0));
    assertFalse(new RE2("ab+c").match("xabbbbbc", 0, 8, RE2.ANCHOR_BOTH, null, 0));
  }

  @Test
  public void testFindEnd() {
    RE2 r = new RE2("abc.*def");
    assertTrue(r.match("yyyabcxxxdefzzz", 0, 15, RE2.UNANCHORED, null, 0));
    assertTrue(r.match("yyyabcxxxdefzzz", 0, 12, RE2.UNANCHORED, null, 0));
    assertTrue(r.match("yyyabcxxxdefzzz", 3, 15, RE2.UNANCHORED, null, 0));
    assertTrue(r.match("yyyabcxxxdefzzz", 3, 12, RE2.UNANCHORED, null, 0));
    assertFalse(r.match("yyyabcxxxdefzzz", 4, 12, RE2.UNANCHORED, null, 0));
    assertFalse(r.match("yyyabcxxxdefzzz", 3, 11, RE2.UNANCHORED, null, 0));
  }

  public static String[][] compileTests() {
    return new String[][] {
      {"", null},
      {".", null},
      {"^.$", null},
      {"a", null},
      {"a*", null},
      {"a+", null},
      {"a?", null},
      {"a|b", null},
      {"a*|b*", null},
      {"(a*|b)(c*|d)", null},
      {"[a-z]", null},
      {"[a-abc-c\\-\\]\\[]", null},
      {"[a-z]+", null},
      {"[abc]", null},
      {"[^1234]", null},
      {"[^\n]", null},
      {"\\!\\\\", null},
      {"abc]", null}, // Matches the closing bracket literally.
      {"a??", null},
      {"*", "missing argument to repetition operator: `*`"},
      {"+", "missing argument to repetition operator: `+`"},
      {"?", "missing argument to repetition operator: `?`"},
      {"(abc", "missing closing ): `(abc`"},
      {"abc)", "regexp/syntax: internal error: `stack underflow`"},
      {"x[a-z", "missing closing ]: `[a-z`"},
      {"[z-a]", "invalid character class range: `z-a`"},
      {"abc\\", "trailing backslash at end of expression"},
      {"a**", "invalid nested repetition operator: `**`"},
      {"a*+", "invalid nested repetition operator: `*+`"},
      {"\\x", "invalid escape sequence: `\\x`"},
      {"\\p", "invalid character class range: `\\p`"},
      {"\\p{", "invalid character class range: `\\p{`"}
    };
  }

  @ParameterizedTest
  @MethodSource("compileTests")
  public void testCompile(final String input, String expectedError) {
    if (expectedError == null) {
      RE2.compile(input);
    } else {
      PatternSyntaxException e =
          assertThrows(
              "RE2.compile(" + input + ") was successful, expected " + expectedError,
              PatternSyntaxException.class,
              () -> RE2.compile(input));
      Truth.assertThat(e).hasMessageThat().isEqualTo("error parsing regexp: " + expectedError);
    }
  }

  private static Object[][] numSubexpTests() {
    return new Object[][] {
      {"", "0"},
      {".*", "0"},
      {"abba", "0"},
      {"ab(b)a", "1"},
      {"ab(.*)a", "1"},
      {"(.*)ab(.*)a", "2"},
      {"(.*)(ab)(.*)a", "3"},
      {"(.*)((a)b)(.*)a", "4"},
      {"(.*)(\\(ab)(.*)a", "3"},
      {"(.*)(\\(a\\)b)(.*)a", "3"},
    };
  }

  @ParameterizedTest
  @MethodSource("numSubexpTests")
  public void testNumSubexps(String input, int expectedOutput) {
    Truth.assertThat(RE2.compile(input).numberOfCapturingGroups()).isEqualTo(expectedOutput);
  }

  @ParameterizedTest
  @MethodSource("com.google.re2j.FindTest#testCases")
  public void testMatch(FindTest.Test test) {
    RE2 re = RE2.compile(test.pat);
    boolean m = re.match(test.text);
    if (m != (test.matches.length > 0)) {
      fail(
          String.format(
              "RE2.match failure on %s: %s should be %s", test, m, test.matches.length > 0));
    }
    // now try bytes
    m = re.matchUTF8(test.textUTF8);
    if (m != (test.matches.length > 0)) {
      fail(
          String.format(
              "RE2.matchUTF8 failure on %s: %s should be %s", test, m, test.matches.length > 0));
    }
  }

  @ParameterizedTest
  @MethodSource("com.google.re2j.FindTest#testCases")
  public void testMatchFunction(FindTest.Test test) {
    boolean m = RE2.match(test.pat, test.text);
    if (m != (test.matches.length > 0)) {
      fail(
          String.format(
              "RE2.match failure on %s: %s should be %s", test, m, test.matches.length > 0));
    }
  }

  // (pattern, output, literal, isLiteral)
  private static Object[][] metaTests() {
    return new Object[][] {
      {"", "", "", true},
      {"foo", "foo", "foo", true},
      // has meta but no operator:
      {"foo\\.\\$", "foo\\\\\\.\\\\\\$", "foo.$", true},
      // has escaped operators and real operators:
      {"foo.\\$", "foo\\.\\\\\\$", "foo", false},
      {
        "!@#$%^&*()_+-=[{]}\\|,<.>/?~",
        "!@#\\$%\\^&\\*\\(\\)_\\+-=\\[\\{\\]\\}\\\\\\|,<\\.>/\\?~",
        "!@#",
        false
      },
    };
  }

  @ParameterizedTest
  @MethodSource("metaTests")
  public void testQuoteMeta(String pattern, String output, String literal, boolean isLiteral) {
    // Verify that quoteMeta returns the expected string.
    String quoted = RE2.quoteMeta(pattern);
    if (!quoted.equals(output)) {
      fail(String.format("RE2.quoteMeta(\"%s\") = \"%s\"; want \"%s\"", pattern, quoted, output));
    }

    // Verify that the quoted string is in fact treated as expected
    // by compile -- i.e. that it matches the original, unquoted string.
    if (!pattern.isEmpty()) {
      RE2 re = null;
      try {
        re = RE2.compile(quoted);
      } catch (PatternSyntaxException e) {
        fail(
            String.format(
                "Unexpected error compiling quoteMeta(\"%s\"): %s", pattern, e.getMessage()));
      }
      String src = "abc" + pattern + "def";
      String repl = "xyz";
      String replaced = re.replaceAll(src, repl);
      String expected = "abcxyzdef";
      if (!replaced.equals(expected)) {
        fail(
            String.format(
                "quoteMeta(`%s`).replace(`%s`,`%s`) = `%s`; want `%s`",
                pattern,
                src,
                repl,
                replaced,
                expected));
      }
    }
  }

  @ParameterizedTest
  @MethodSource("metaTests")
  public void testLiteralPrefix(String pattern, String output, String literal, boolean isLiteral)
      throws PatternSyntaxException {
    // Literal method needs to scan the pattern.
    RE2 re = RE2.compile(pattern);
    if (re.prefixComplete != isLiteral) {
      fail(
          String.format(
              "literalPrefix(\"%s\") = %s; want %s", pattern, re.prefixComplete, isLiteral));
    }
    if (!re.prefix.equals(literal)) {
      fail(
          String.format(
              "literalPrefix(\"%s\") = \"%s\"; want \"%s\"", pattern, re.prefix, literal));
    }
  }

  public static Object[][] replaceTests() {
    return new Object[][] {
      // Test empty input and/or replacement,
      // with pattern that matches the empty string.
      {"", "", "", "", false},
      {"", "x", "", "x", false},
      {"", "", "abc", "abc", false},
      {"", "x", "abc", "xaxbxcx", false},

      // Test empty input and/or replacement,
      // with pattern that does not match the empty string.
      {"b", "", "", "", false},
      {"b", "x", "", "", false},
      {"b", "", "abc", "ac", false},
      {"b", "x", "abc", "axc", false},
      {"y", "", "", "", false},
      {"y", "x", "", "", false},
      {"y", "", "abc", "abc", false},
      {"y", "x", "abc", "abc", false},

      // Multibyte characters -- verify that we don't try to match in the middle
      // of a character.
      {"[a-c]*", "x", "\u65e5", "x\u65e5x", false},
      {"[^\u65e5]", "x", "abc\u65e5def", "xxx\u65e5xxx", false},

      // Start and end of a string.
      {"^[a-c]*", "x", "abcdabc", "xdabc", false},
      {"[a-c]*$", "x", "abcdabc", "abcdx", false},
      {"^[a-c]*$", "x", "abcdabc", "abcdabc", false},
      {"^[a-c]*", "x", "abc", "x", false},
      {"[a-c]*$", "x", "abc", "x", false},
      {"^[a-c]*$", "x", "abc", "x", false},
      {"^[a-c]*", "x", "dabce", "xdabce", false},
      {"[a-c]*$", "x", "dabce", "dabcex", false},
      {"^[a-c]*$", "x", "dabce", "dabce", false},
      {"^[a-c]*", "x", "", "x", false},
      {"[a-c]*$", "x", "", "x", false},
      {"^[a-c]*$", "x", "", "x", false},
      {"^[a-c]+", "x", "abcdabc", "xdabc", false},
      {"[a-c]+$", "x", "abcdabc", "abcdx", false},
      {"^[a-c]+$", "x", "abcdabc", "abcdabc", false},
      {"^[a-c]+", "x", "abc", "x", false},
      {"[a-c]+$", "x", "abc", "x", false},
      {"^[a-c]+$", "x", "abc", "x", false},
      {"^[a-c]+", "x", "dabce", "dabce", false},
      {"[a-c]+$", "x", "dabce", "dabce", false},
      {"^[a-c]+$", "x", "dabce", "dabce", false},
      {"^[a-c]+", "x", "", "", false},
      {"[a-c]+$", "x", "", "", false},
      {"^[a-c]+$", "x", "", "", false},

      // Other cases.
      {"abc", "def", "abcdefg", "defdefg", false},
      {"bc", "BC", "abcbcdcdedef", "aBCBCdcdedef", false},
      {"abc", "", "abcdabc", "d", false},
      {"x", "xXx", "xxxXxxx", "xXxxXxxXxXxXxxXxxXx", false},
      {"abc", "d", "", "", false},
      {"abc", "d", "abc", "d", false},
      {".+", "x", "abc", "x", false},
      {"[a-c]*", "x", "def", "xdxexfx", false},
      {"[a-c]+", "x", "abcbcdcdedef", "xdxdedef", false},
      {"[a-c]*", "x", "abcbcdcdedef", "xdxdxexdxexfx", false},

      // Test empty input and/or replacement,
      // with pattern that matches the empty string.
      {"", "", "", "", true},
      {"", "x", "", "x", true},
      {"", "", "abc", "abc", true},
      {"", "x", "abc", "xabc", true},

      // Test empty input and/or replacement,
      // with pattern that does not match the empty string.
      {"b", "", "", "", true},
      {"b", "x", "", "", true},
      {"b", "", "abc", "ac", true},
      {"b", "x", "abc", "axc", true},
      {"y", "", "", "", true},
      {"y", "x", "", "", true},
      {"y", "", "abc", "abc", true},
      {"y", "x", "abc", "abc", true},

      // Multibyte characters -- verify that we don't try to match in the middle
      // of a character.
      {"[a-c]*", "x", "\u65e5", "x\u65e5", true},
      {"[^\u65e5]", "x", "abc\u65e5def", "xbc\u65e5def", true},

      // Start and end of a string.
      {"^[a-c]*", "x", "abcdabc", "xdabc", true},
      {"[a-c]*$", "x", "abcdabc", "abcdx", true},
      {"^[a-c]*$", "x", "abcdabc", "abcdabc", true},
      {"^[a-c]*", "x", "abc", "x", true},
      {"[a-c]*$", "x", "abc", "x", true},
      {"^[a-c]*$", "x", "abc", "x", true},
      {"^[a-c]*", "x", "dabce", "xdabce", true},
      {"[a-c]*$", "x", "dabce", "dabcex", true},
      {"^[a-c]*$", "x", "dabce", "dabce", true},
      {"^[a-c]*", "x", "", "x", true},
      {"[a-c]*$", "x", "", "x", true},
      {"^[a-c]*$", "x", "", "x", true},
      {"^[a-c]+", "x", "abcdabc", "xdabc", true},
      {"[a-c]+$", "x", "abcdabc", "abcdx", true},
      {"^[a-c]+$", "x", "abcdabc", "abcdabc", true},
      {"^[a-c]+", "x", "abc", "x", true},
      {"[a-c]+$", "x", "abc", "x", true},
      {"^[a-c]+$", "x", "abc", "x", true},
      {"^[a-c]+", "x", "dabce", "dabce", true},
      {"[a-c]+$", "x", "dabce", "dabce", true},
      {"^[a-c]+$", "x", "dabce", "dabce", true},
      {"^[a-c]+", "x", "", "", true},
      {"[a-c]+$", "x", "", "", true},
      {"^[a-c]+$", "x", "", "", true},

      // Other cases.
      {"abc", "def", "abcdefg", "defdefg", true},
      {"bc", "BC", "abcbcdcdedef", "aBCbcdcdedef", true},
      {"abc", "", "abcdabc", "dabc", true},
      {"x", "xXx", "xxxXxxx", "xXxxxXxxx", true},
      {"abc", "d", "", "", true},
      {"abc", "d", "abc", "d", true},
      {".+", "x", "abc", "x", true},
      {"[a-c]*", "x", "def", "xdef", true},
      {"[a-c]+", "x", "abcbcdcdedef", "xdcdedef", true},
      {"[a-c]*", "x", "abcbcdcdedef", "xdcdedef", true},
    };
  }

  @ParameterizedTest
  @MethodSource("replaceTests")
  public void replaceTestHelper(
      String pattern, String replacement, String source, String expected, boolean replaceFirst) {
    RE2 re = null;
    try {
      re = RE2.compile(pattern);
    } catch (PatternSyntaxException e) {
      fail(String.format("Unexpected error compiling %s: %s", pattern, e.getMessage()));
    }
    String actual =
        replaceFirst ? re.replaceFirst(source, replacement) : re.replaceAll(source, replacement);
    if (!actual.equals(expected)) {
      fail(
          String.format(
              "%s.replaceAll(%s,%s) = %s; want %s",
              pattern,
              source,
              replacement,
              actual,
              expected));
    }
  }

  private static String[][] replaceAllFuncTests() {
    return new String[][] {
      {"[a-c]", "defabcdef", "defxayxbyxcydef"},
      {"[a-c]+", "defabcdef", "defxabcydef"},
      {"[a-c]*", "defabcdef", "xydxyexyfxabcydxyexyfxy"},
    };
  }

  @ParameterizedTest
  @MethodSource("replaceAllFuncTests")
  public void testReplaceAllFunc(String pattern, String input, String expected) {
    RE2 re = null;
    try {
      re = RE2.compile(pattern);
    } catch (PatternSyntaxException e) {
      fail(String.format("Unexpected error compiling %s: %s", pattern, e.getMessage()));
    }
    String actual = re.replaceAllFunc(input, s -> ("x" + s + "y"), input.length());
    if (!actual.equals(expected)) {
      fail(String.format("replaceAllFunc(%s,%s) = %s; want %s", pattern, input, actual, expected));
    }
  }
}
