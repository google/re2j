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
public class RE2ReplaceAllFunctionTest {
  private static final RE2.ReplaceFunc REPLACE_XSY =
      new RE2.ReplaceFunc() {
        @Override
        public String replace(String s) {
          return "x" + s + "y";
        }

        @Override
        public String toString() {
          return "REPLACE_XSY";
        }
      };

  // Each row is (String pattern, input, output, ReplaceFunc replacement).
  // Conceptually the replacement func is a table column---but for now
  // it's always REPLACE_XSY.
  private static final String[][] REPLACE_FUNC_TESTS = {
    {"[a-c]", "defabcdef", "defxayxbyxcydef"},
    {"[a-c]+", "defabcdef", "defxabcydef"},
    {"[a-c]*", "defabcdef", "xydxyexyfxabcydxyexyfxy"},
  };

  @Parameters
  public static String[][] testCases() {
    return REPLACE_FUNC_TESTS;
  }

  private final String pattern;
  private final String input;
  private final String expected;

  public RE2ReplaceAllFunctionTest(String pattern, String input, String expected) {
    this.pattern = pattern;
    this.input = input;
    this.expected = expected;
  }

  @Test
  public void testReplaceAllFunc() {
    RE2 re = null;
    try {
      re = RE2.compile(pattern);
    } catch (PatternSyntaxException e) {
      fail(String.format("Unexpected error compiling %s: %s", pattern, e.getMessage()));
    }
    String actual = re.replaceAllFunc(input, REPLACE_XSY, input.length());
    if (!actual.equals(expected)) {
      fail(
          String.format(
              "%s.replaceAllFunc(%s,%s) = %s; want %s",
              pattern,
              input,
              REPLACE_XSY,
              actual,
              expected));
    }
  }
}
