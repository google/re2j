package com.google.re2j;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static io.airlift.slice.Slices.utf8Slice;
import static org.junit.Assert.fail;

@RunWith(Enclosed.class)
public class RE2MatchTest {

  public static class DFA extends RE2MatchTestBase {
    public DFA(FindTest.Test findTest) {
      super(findTest, RUN_WITH_DFA);
    }
  }

  public static class NFA extends RE2MatchTestBase {
    public NFA(FindTest.Test findTest) {
      super(findTest, RUN_WITH_NFA);
    }
  }

  @RunWith(Parameterized.class)
  public static abstract class RE2MatchTestBase extends OptionsTest {

    @Parameters
    public static FindTest.Test[] matchTests() {
      return FindTest.FIND_TESTS;
    }

    private final FindTest.Test test;

    protected RE2MatchTestBase(FindTest.Test findTest, Options options) {
      super(options);
      this.test = findTest;
    }

    @Test
    public void testMatch() {
      RE2 re = RE2.compile(test.pat, options);
      boolean m = re.match(utf8Slice(test.text));
      if (m != (test.matches.length > 0)) {
        fail(String.format("RE2.match failure on %s: %s should be %s", test, m,
            test.matches.length > 0));
      }
    }

    @Test
    public void testMatchFunction() {
      boolean m = RE2.match(test.pat, utf8Slice(test.text), options);
      if (m != (test.matches.length > 0)) {
        fail(String.format("RE2.match failure on %s: %s should be %s", test, m,
            test.matches.length > 0));
      }
    }
  }
}
