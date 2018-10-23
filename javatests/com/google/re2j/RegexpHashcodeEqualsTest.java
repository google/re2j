package com.google.re2j;

import com.google.common.truth.Truth;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

@RunWith(Parameterized.class)
public class RegexpHashcodeEqualsTest {
  @Parameterized.Parameters
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

  @Parameterized.Parameter public String a;

  @Parameterized.Parameter(1)
  public String b;

  @Parameterized.Parameter(2)
  public boolean areEqual;

  @Parameterized.Parameter(3)
  public int mode;

  @Test
  public void testEquals() {
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
