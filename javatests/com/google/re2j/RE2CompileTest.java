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
public class RE2CompileTest {
  // A list of regexp and expected error when calling RE2.compile. null implies that compile should
  // succeed.
  @Parameters()
  public static String[][] testData() {
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
      {"..|.#|..", null},
      {"\\!\\\\", null},
      {"abc]", null}, // Matches the closing bracket literally.
      {"a??", null},
      {"*", "missing argument to repetition operator: `*`"},
      {"+", "missing argument to repetition operator: `+`"},
      {"?", "missing argument to repetition operator: `?`"},
      {"(abc", "missing closing ): `(abc`"},
      {"abc)", "unexpected ): `abc)`"},
      {")", "unexpected ): `)`"},
      {"())", "unexpected ): `())`"},
      {"x[a-z", "missing closing ]: `[a-z`"},
      {"[z-a]", "invalid character class range: `z-a`"},
      {"abc\\", "trailing backslash at end of expression: ``"},
      {"a**", "invalid nested repetition operator: `**`"},
      {"a*+", "invalid nested repetition operator: `*+`"},
      {"\\x", "invalid escape sequence: `\\x`"},
      {"\\p", "invalid character class range: `\\p`"},
      {"\\p{", "invalid character class range: `\\p{`"},
      // As in Go, nested repetitions may make at most 1000 copies of their innermost expression.
      {"(?:(?:(?:a){1000}){1000}){1000}", "invalid repeat count: `{1000}`"},
      {"(?:(?:a{1000}){1000})", "invalid repeat count: `{1000}`"},
      {"(?:a{1000}){100}", "invalid repeat count: `{100}`"},
      {"(a{2}){500}", null},
      {"(a{2}){501}", "invalid repeat count: `{501}`"},
      {"(a{500}){2}", null},
      // A { that starts no repeat count is a literal, which a repetition may follow.
      {"x{*", null},
      {"x{", null},
      // Only an ASCII punctuation character escapes to itself.
      {"\\\u00e9", "invalid escape sequence: `\\\u00e9`"},
      // (?< and (?P< are a named capture only with something after them.
      {"(?<", "invalid or unsupported Perl syntax: `(?<`"},
      {"(?P<", "invalid or unsupported Perl syntax: `(?P`"}
    };
  }

  private final String input;
  private final String expectedError;

  public RE2CompileTest(String input, String expectedError) {
    this.input = input;
    this.expectedError = expectedError;
  }

  @Test
  public void testCompile() {
    try {
      RE2 unused = RE2.compile(input);
      if (expectedError != null) {
        fail("RE2.compile(" + input + ") was successful, expected " + expectedError);
      }
    } catch (PatternSyntaxException e) {
      if (expectedError == null
          || !e.getMessage().equals("error parsing regexp: " + expectedError)) {
        fail("compiling " + input + "; unexpected error: " + e.getMessage());
      }
    }
  }
}
