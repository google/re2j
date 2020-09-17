/*
 * Copyright (c) 2020 The Go Authors. All rights reserved.
 *
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
package com.google.re2j;

import com.google.common.truth.Truth;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;

public class RegexpHashcodeEqualsTest {
  public static Iterable<Object[]> testCases() {
    return Arrays.asList(
        new Object[][] {
          {"abc", "abc", true, RE2.POSIX},
          {"abc", "def", false, RE2.POSIX},
          {"(abc)", "(a)(b)(c)", false, RE2.POSIX},
          {"a|$", "a|$", true, RE2.POSIX},
          {"abc|def", "def|abc", false, RE2.POSIX},
          {"a?", "b?", false, RE2.POSIX},
          {"a?", "a?", true, RE2.POSIX},
          {"a{1,3}", "a{1,3}", true, RE2.POSIX},
          {"a{2,3}", "a{1,3}", false, RE2.POSIX},
          {"^((?P<foo>what)a)$", "^((?P<foo>what)a)$", true, RE2.PERL},
          {"^((?P<foo>what)a)$", "^((?P<bar>what)a)$", false, RE2.PERL},
        });
  }

  @ParameterizedTest
  @MethodSource("testCases")
  public void testEquals(String a, String b, boolean areEqual, int mode) {
    Regexp ra = Parser.parse(a, mode);
    Regexp rb = Parser.parse(b, mode);
    if (areEqual) {
      Truth.assertThat(ra).isEqualTo(rb);
      Truth.assertThat(ra.hashCode()).isEqualTo(rb.hashCode());
    } else {
      Truth.assertThat(ra).isNotEqualTo(rb);
    }
  }
}
