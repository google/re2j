/*
 * Copyright (c) 2020 The Go Authors. All rights reserved.
 *
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
// Original Go source here:
// http://code.google.com/p/go/source/browse/src/pkg/regexp/syntax/prog_test.go

package com.google.re2j;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ProgTest {
  private static final String[][] COMPILE_TESTS = {
    {"a", "0       fail\n" + "1*      rune1 \"a\" -> 2\n" + "2       match\n"},
    {
      "[A-M][n-z]",
      "0       fail\n"
          + "1*      rune \"AM\" -> 2\n"
          + "2       rune \"nz\" -> 3\n"
          + "3       match\n"
    },
    {"", "0       fail\n" + "1*      nop -> 2\n" + "2       match\n"},
    {
      "a?",
      "0       fail\n" + "1       rune1 \"a\" -> 3\n" + "2*      alt -> 1, 3\n" + "3       match\n"
    },
    {
      "a??",
      "0       fail\n" + "1       rune1 \"a\" -> 3\n" + "2*      alt -> 3, 1\n" + "3       match\n"
    },
    {
      "a+",
      "0       fail\n" + "1*      rune1 \"a\" -> 2\n" + "2       alt -> 1, 3\n" + "3       match\n"
    },
    {
      "a+?",
      "0       fail\n" + "1*      rune1 \"a\" -> 2\n" + "2       alt -> 3, 1\n" + "3       match\n"
    },
    {
      "a*",
      "0       fail\n" + "1       rune1 \"a\" -> 2\n" + "2*      alt -> 1, 3\n" + "3       match\n"
    },
    {
      "a*?",
      "0       fail\n" + "1       rune1 \"a\" -> 2\n" + "2*      alt -> 3, 1\n" + "3       match\n"
    },
    {
      "a+b+",
      "0       fail\n"
          + "1*      rune1 \"a\" -> 2\n"
          + "2       alt -> 1, 3\n"
          + "3       rune1 \"b\" -> 4\n"
          + "4       alt -> 3, 5\n"
          + "5       match\n"
    },
    {
      "(a+)(b+)",
      "0       fail\n"
          + "1*      cap 2 -> 2\n"
          + "2       rune1 \"a\" -> 3\n"
          + "3       alt -> 2, 4\n"
          + "4       cap 3 -> 5\n"
          + "5       cap 4 -> 6\n"
          + "6       rune1 \"b\" -> 7\n"
          + "7       alt -> 6, 8\n"
          + "8       cap 5 -> 9\n"
          + "9       match\n"
    },
    {
      "a+|b+",
      "0       fail\n"
          + "1       rune1 \"a\" -> 2\n"
          + "2       alt -> 1, 6\n"
          + "3       rune1 \"b\" -> 4\n"
          + "4       alt -> 3, 6\n"
          + "5*      alt -> 1, 3\n"
          + "6       match\n"
    },
    {
      "A[Aa]",
      "0       fail\n"
          + "1*      rune1 \"A\" -> 2\n"
          + "2       rune \"A\"/i -> 3\n"
          + "3       match\n"
    },
    {
      "(?:(?:^).)",
      "0       fail\n" + "1*      empty 4 -> 2\n" + "2       anynotnl -> 3\n" + "3       match\n"
    },
    {
      "(?:|a)+",
      "0       fail\n"
          + "1       nop -> 4\n"
          + "2       rune1 \"a\" -> 4\n"
          + "3*      alt -> 1, 2\n"
          + "4       alt -> 3, 5\n"
          + "5       match\n"
    },
    {
      "(?:|a)*",
      "0       fail\n"
          + "1       nop -> 4\n"
          + "2       rune1 \"a\" -> 4\n"
          + "3       alt -> 1, 2\n"
          + "4       alt -> 3, 6\n"
          + "5*      alt -> 3, 6\n"
          + "6       match\n"
    },
  };

  private final String input;
  private final String expected;

  @Parameterized.Parameters
  public static Object[] getParameters() {
    return COMPILE_TESTS;
  }

  public ProgTest(String input, String expected) {
    this.input = input;
    this.expected = expected;
  }

  @Test
  public void testCompile() throws Exception {
    Regexp re = Parser.parse(input, RE2.PERL);
    Prog p = Compiler.compileRegexp(re);
    String s = p.toString();
    assertEquals("compiled: " + input, expected, s);
  }
}
