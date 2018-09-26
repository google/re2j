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
      {"i\u9b47$ \u55ed54@\u864e\ub117\ua575\uad27\uc80cv^\u9fe8\u15ab\u6e2de\u67a5X+\u2d09\u649fn%\ua91e[Y\u6d7da\u9586\ua2bb\u7170nZ\u1476\u2a9eA\u4720\u1edb.\ua0603\u1754\u4915n\u2b90\"\u0fe5pC4\u117c\uc94bt\u580d\uad90\u3930\u1b92u\ub00e\u7361/\u234b\u8e5f\uaac6\u0647y\u9ca5\u0092\u3396\u1775@[q:\u3abb\\p", "TODO"},
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
