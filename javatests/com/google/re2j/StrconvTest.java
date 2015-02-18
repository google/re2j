// Copyright 2011 The Go Authors.  All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

// Derived from http://golang.org/src/pkg/strconv/quote_test.go.

package com.google.re2j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

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
  };

  private static final String[] MISQUOTED = {
    "",
    "\"",
    "\"a",
    "\"'",
    "b\"",
    "\"\\\"",
    "'\\'",
    "'ab'",
    "\"\\x1!\"",
    "\"\\U12345678\"",
    "\"\\z\"",
    "`",
    "`xxx",
    "`\"",
    "\"\\'\"",
    "'\\\"'",
    "\"\n\"",
    "\"\\n\n\"",
    "'\n'",
  };

  @Test
  public void testUnquote() {
    for (String[] test : UNQUOTE_TESTS) {
      // System.err.println(test[0]);
      assertEquals(String.format("unquote(%s)", test[0]),
                   test[1], Strconv.unquote(test[0]));
    }
    for (String s : MISQUOTED) {
      try {
        // System.err.println(s);
        Strconv.unquote(s);
        fail(String.format("unquote(%s) succeeded unexpectedly", s));
      } catch (IllegalArgumentException e) {
        /* ok */
      } catch (StringIndexOutOfBoundsException e) {
        /* ok */
      }
    }
    // TODO(adonovan): port and run the quote tests too, backward
  }
}