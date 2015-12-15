package com.google.re2j;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import io.airlift.slice.Slice;

import static io.airlift.slice.Slices.utf8Slice;
import static org.junit.Assert.fail;

@RunWith(Enclosed.class)
public class RE2ReplaceAllFunctionTest {

  public static class DFA extends RE2ReplaceAllFunctionTestBase {
    public DFA(String pattern, String input, String expected) {
      super(pattern, input, expected, RUN_WITH_DFA);
    }
  }

  public static class NFA extends RE2ReplaceAllFunctionTestBase {
    public NFA(String pattern, String input, String expected) {
      super(pattern, input, expected, RUN_WITH_NFA);
    }
  }

  private static final RE2.ReplaceFunc REPLACE_XSY = new RE2.ReplaceFunc() {
    public Slice replace(Slice s) {
      return utf8Slice("x" + s.toStringUtf8() + "y");
    }

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
      {"[a-c]*", "defabcdef", "xydxyexyfxabcydxyexyfxy"},};

  @RunWith(Parameterized.class)
  public static abstract class RE2ReplaceAllFunctionTestBase extends OptionsTest {

    @Parameters
    public static String[][] testCases() {
      return REPLACE_FUNC_TESTS;
    }

    private final String pattern;
    private final String input;
    private final String expected;

    public RE2ReplaceAllFunctionTestBase(String pattern, String input, String expected, Options options) {
      super(options);
      this.pattern = pattern;
      this.input = input;
      this.expected = expected;
    }

    @Test
    public void testReplaceAllFunc() {
      RE2 re = null;
      try {
        re = RE2.compile(pattern, options);
      } catch (PatternSyntaxException e) {
        fail(String.format("Unexpected error compiling %s: %s", pattern, e.getMessage()));
      }
      String actual = re.replaceAllFunc(utf8Slice(input), REPLACE_XSY, input.length()).toStringUtf8();
      if (!actual.equals(expected)) {
        fail(String.format("%s.replaceAllFunc(%s,%s) = %s; want %s", pattern, input, REPLACE_XSY, actual, expected));
      }
    }
  }
}
