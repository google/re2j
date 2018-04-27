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
      {"\\!\\\\", null},
      {"abc]", null}, // Matches the closing bracket literally.
      {"a??", null},
      {"*", "missing argument to repetition operator: `*`"},
      {"+", "missing argument to repetition operator: `+`"},
      {"?", "missing argument to repetition operator: `?`"},
      {"(abc", "missing closing ): `(abc`"},
      {"abc)", "regexp/syntax: internal error: `stack underflow`"},
      {"x[a-z", "missing closing ]: `[a-z`"},
      {"[z-a]", "invalid character class range: `z-a`"},
      {"abc\\", "trailing backslash at end of expression"},
      {"a**", "invalid nested repetition operator: `**`"},
      {"a*+", "invalid nested repetition operator: `*+`"},
      {"\\x", "invalid escape sequence: `\\x`"},
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
      RE2.compile(input);
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
