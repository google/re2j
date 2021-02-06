/*
 * Copyright (c) 2021 The Go Authors. All rights reserved.
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
public class RE2QuoteMetaTest {
  // (pattern, output, literal, isLiteral)
  private static final String[][] META_TESTS = {
    {"", "", "", "true"},
    {"foo", "foo", "foo", "true"},
    // has meta but no operator:
    {"foo\\.\\$", "foo\\\\\\.\\\\\\$", "foo.$", "true"},
    // has escaped operators and real operators:
    {"foo.\\$", "foo\\.\\\\\\$", "foo", "false"},
    {
      "!@#$%^&*()_+-=[{]}\\|,<.>/?~",
      "!@#\\$%\\^&\\*\\(\\)_\\+-=\\[\\{\\]\\}\\\\\\|,<\\.>/\\?~",
      "!@#",
      "false"
    },
  };

  @Parameters
  public static String[][] testCases() {
    return META_TESTS;
  }

  private final String pattern;
  private final String output;
  private final String literal;
  private final boolean isLiteral;

  public RE2QuoteMetaTest(String pattern, String output, String literal, String isLiteral) {
    this.pattern = pattern;
    this.output = output;
    this.literal = literal;
    this.isLiteral = Boolean.parseBoolean(isLiteral);
  }

  @Test
  public void testQuoteMeta() {
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

  @Test
  public void testLiteralPrefix() throws PatternSyntaxException {
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
}
