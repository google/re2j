// Copyright 2011 The Go Authors.  All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

// Derived from http://golang.org/src/pkg/strconv/quote_test.go.

package com.google.re2j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class StrconvTest {

  private static String rune(int r) {
    return new StringBuffer().appendCodePoint(r).toString();
  }

  private static final String[][] UNQUOTE_TESTS = {
    {"\"\"", ""},
    {"\"a\"", "a"},
    {"\"abc\"", "abc"},
    {"\"☺\"", "☺"},
    {"\"hello world\"", "hello world"},
    {"\"\\xFF\"", "\u00FF"},
    {"\"\\377\"", "\377"},
    {"\"\\u1234\"", "\u1234"},
    {"\"\\U00010111\"", rune(0x10111)},
    {"\"\\U0001011111\"", rune(0x10111) + "11"},
    {"\"\\a\\b\\f\\n\\r\\t\\v\\\\\\\"\"", "\007\b\f\n\r\t\013\\\""},
    {"\"'\"", "'"},
    {"'a'", "a"},
    {"'☹'", "☹"},
    {"'\\a'", "\u0007"},
    {"'\\x10'", "\u0010"},
    {"'\\377'", "\377"},
    {"'\\u1234'", "\u1234"},
    {"'\\U00010111'", rune(0x10111)},
    {"'\\t'", "\t"},
    {"' '", " "},
    {"'\\''", "'"},
    {"'\"'", "\""},
    {"``", ""},
    {"`a`", "a"},
    {"`abc`", "abc"},
    {"`☺`", "☺"},
    {"`hello world`", "hello world"},
    {"`\\xFF`", "\\xFF"},
    {"`\\377`", "\\377"},
    {"`\\`", "\\"},
    {"`\n`", "\n"},
    {"`\t`", "\t"},
    {"` `", " "},

    // Misquoted strings, should produce an error.
    {"", null},
    {"\"", null},
    {"\"a", null},
    {"\"'", null},
    {"b\"", null},
    {"\"\\\"", null},
    {"'\\'", null},
    {"'ab'", null},
    {"\"\\x1!\"", null},
    {"\"\\U12345678\"", null},
    {"\"\\z\"", null},
    {"`", null},
    {"`xxx", null},
    {"`\"", null},
    {"\"\\'\"", null},
    {"'\\\"'", null},
    {"\"\n\"", null},
    {"\"\\n\n\"", null},
    {"'\n'", null},
  };

  @Parameterized.Parameters
  public static Object[] testData() {
    return UNQUOTE_TESTS;
  }

  private final String input;
  private final String expected;

  public StrconvTest(String input, String expected) {
    this.input = input;
    this.expected = expected;
  }

  @Test
  public void testUnquote() {
    if (expected != null) {
      assertEquals(String.format("unquote(%s)", input), expected, Strconv.unquote(input));
    } else {
      try {
        Strconv.unquote(input);
        fail(String.format("unquote(%s) succeeded unexpectedly", input));
      } catch (IllegalArgumentException e) {
        /* ok */
      } catch (StringIndexOutOfBoundsException e) {
        /* ok */
      }
    }
    // TODO(adonovan): port and run the quote tests too, backward
  }
}
