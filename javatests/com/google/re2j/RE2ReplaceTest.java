/*
 * Copyright (c) 2020 The Go Authors. All rights reserved.
 *
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
package com.google.re2j;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class RE2ReplaceTest {
  private static final Object[][] REPLACE_TESTS = {
    // Test empty input and/or replacement,
    // with pattern that matches the empty string.
    {"", "", "", "", "false"},
    {"", "x", "", "x", "false"},
    {"", "", "abc", "abc", "false"},
    {"", "x", "abc", "xaxbxcx", "false"},

    // Test empty input and/or replacement,
    // with pattern that does not match the empty string.
    {"b", "", "", "", "false"},
    {"b", "x", "", "", "false"},
    {"b", "", "abc", "ac", "false"},
    {"b", "x", "abc", "axc", "false"},
    {"y", "", "", "", "false"},
    {"y", "x", "", "", "false"},
    {"y", "", "abc", "abc", "false"},
    {"y", "x", "abc", "abc", "false"},

    // Multibyte characters -- verify that we don't try to match in the middle
    // of a character.
    {"[a-c]*", "x", "\u65e5", "x\u65e5x", "false"},
    {"[^\u65e5]", "x", "abc\u65e5def", "xxx\u65e5xxx", "false"},

    // Start and end of a string.
    {"^[a-c]*", "x", "abcdabc", "xdabc", "false"},
    {"[a-c]*$", "x", "abcdabc", "abcdx", "false"},
    {"^[a-c]*$", "x", "abcdabc", "abcdabc", "false"},
    {"^[a-c]*", "x", "abc", "x", "false"},
    {"[a-c]*$", "x", "abc", "x", "false"},
    {"^[a-c]*$", "x", "abc", "x", "false"},
    {"^[a-c]*", "x", "dabce", "xdabce", "false"},
    {"[a-c]*$", "x", "dabce", "dabcex", "false"},
    {"^[a-c]*$", "x", "dabce", "dabce", "false"},
    {"^[a-c]*", "x", "", "x", "false"},
    {"[a-c]*$", "x", "", "x", "false"},
    {"^[a-c]*$", "x", "", "x", "false"},
    {"^[a-c]+", "x", "abcdabc", "xdabc", "false"},
    {"[a-c]+$", "x", "abcdabc", "abcdx", "false"},
    {"^[a-c]+$", "x", "abcdabc", "abcdabc", "false"},
    {"^[a-c]+", "x", "abc", "x", "false"},
    {"[a-c]+$", "x", "abc", "x", "false"},
    {"^[a-c]+$", "x", "abc", "x", "false"},
    {"^[a-c]+", "x", "dabce", "dabce", "false"},
    {"[a-c]+$", "x", "dabce", "dabce", "false"},
    {"^[a-c]+$", "x", "dabce", "dabce", "false"},
    {"^[a-c]+", "x", "", "", "false"},
    {"[a-c]+$", "x", "", "", "false"},
    {"^[a-c]+$", "x", "", "", "false"},

    // Other cases.
    {"abc", "def", "abcdefg", "defdefg", "false"},
    {"bc", "BC", "abcbcdcdedef", "aBCBCdcdedef", "false"},
    {"abc", "", "abcdabc", "d", "false"},
    {"x", "xXx", "xxxXxxx", "xXxxXxxXxXxXxxXxxXx", "false"},
    {"abc", "d", "", "", "false"},
    {"abc", "d", "abc", "d", "false"},
    {".+", "x", "abc", "x", "false"},
    {"[a-c]*", "x", "def", "xdxexfx", "false"},
    {"[a-c]+", "x", "abcbcdcdedef", "xdxdedef", "false"},
    {"[a-c]*", "x", "abcbcdcdedef", "xdxdxexdxexfx", "false"},

    // Test empty input and/or replacement,
    // with pattern that matches the empty string.
    {"", "", "", "", "true"},
    {"", "x", "", "x", "true"},
    {"", "", "abc", "abc", "true"},
    {"", "x", "abc", "xabc", "true"},

    // Test empty input and/or replacement,
    // with pattern that does not match the empty string.
    {"b", "", "", "", "true"},
    {"b", "x", "", "", "true"},
    {"b", "", "abc", "ac", "true"},
    {"b", "x", "abc", "axc", "true"},
    {"y", "", "", "", "true"},
    {"y", "x", "", "", "true"},
    {"y", "", "abc", "abc", "true"},
    {"y", "x", "abc", "abc", "true"},

    // Multibyte characters -- verify that we don't try to match in the middle
    // of a character.
    {"[a-c]*", "x", "\u65e5", "x\u65e5", "true"},
    {"[^\u65e5]", "x", "abc\u65e5def", "xbc\u65e5def", "true"},

    // Start and end of a string.
    {"^[a-c]*", "x", "abcdabc", "xdabc", "true"},
    {"[a-c]*$", "x", "abcdabc", "abcdx", "true"},
    {"^[a-c]*$", "x", "abcdabc", "abcdabc", "true"},
    {"^[a-c]*", "x", "abc", "x", "true"},
    {"[a-c]*$", "x", "abc", "x", "true"},
    {"^[a-c]*$", "x", "abc", "x", "true"},
    {"^[a-c]*", "x", "dabce", "xdabce", "true"},
    {"[a-c]*$", "x", "dabce", "dabcex", "true"},
    {"^[a-c]*$", "x", "dabce", "dabce", "true"},
    {"^[a-c]*", "x", "", "x", "true"},
    {"[a-c]*$", "x", "", "x", "true"},
    {"^[a-c]*$", "x", "", "x", "true"},
    {"^[a-c]+", "x", "abcdabc", "xdabc", "true"},
    {"[a-c]+$", "x", "abcdabc", "abcdx", "true"},
    {"^[a-c]+$", "x", "abcdabc", "abcdabc", "true"},
    {"^[a-c]+", "x", "abc", "x", "true"},
    {"[a-c]+$", "x", "abc", "x", "true"},
    {"^[a-c]+$", "x", "abc", "x", "true"},
    {"^[a-c]+", "x", "dabce", "dabce", "true"},
    {"[a-c]+$", "x", "dabce", "dabce", "true"},
    {"^[a-c]+$", "x", "dabce", "dabce", "true"},
    {"^[a-c]+", "x", "", "", "true"},
    {"[a-c]+$", "x", "", "", "true"},
    {"^[a-c]+$", "x", "", "", "true"},

    // Other cases.
    {"abc", "def", "abcdefg", "defdefg", "true"},
    {"bc", "BC", "abcbcdcdedef", "aBCbcdcdedef", "true"},
    {"abc", "", "abcdabc", "dabc", "true"},
    {"x", "xXx", "xxxXxxx", "xXxxxXxxx", "true"},
    {"abc", "d", "", "", "true"},
    {"abc", "d", "abc", "d", "true"},
    {".+", "x", "abc", "x", "true"},
    {"[a-c]*", "x", "def", "xdef", "true"},
    {"[a-c]+", "x", "abcbcdcdedef", "xdcdedef", "true"},
    {"[a-c]*", "x", "abcbcdcdedef", "xdcdedef", "true"},
  };

  @Parameters
  public static Object[][] replaceTests() {
    return REPLACE_TESTS;
  }

  private final String pattern;
  private final String replacement;
  private final String source;
  private final String expected;
  private boolean replaceFirst;

  public RE2ReplaceTest(
      String pattern, String replacement, String source, String expected, String replaceFirst) {
    this.pattern = pattern;
    this.replacement = replacement;
    this.source = source;
    this.expected = expected;
    this.replaceFirst = Boolean.parseBoolean(replaceFirst);
  }

  @Test
  public void replaceTestHelper() {
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
}
