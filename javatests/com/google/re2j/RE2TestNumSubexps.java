package com.google.re2j;

import static com.google.re2j.Options.DEFAULT_OPTIONS;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class RE2TestNumSubexps {
  private static final String[][] NUM_SUBEXP_CASES = {
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

  @Parameters
  public static String[][] testCases() {
    return NUM_SUBEXP_CASES;
  }

  private final String input;
  private final int expected;

  public RE2TestNumSubexps(String input, String expected) {
    this.input = input;
    this.expected = Integer.parseInt(expected);
  }

  @Test
  public void testNumSubexp() throws PatternSyntaxException {
    assertEquals("numberOfCapturingGroups(" + input +")", expected, RE2.compile(input, DEFAULT_OPTIONS).numberOfCapturingGroups());
  }
}
