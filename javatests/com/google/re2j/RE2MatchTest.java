package com.google.re2j;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class RE2MatchTest {
  @Parameters
  public static FindTest.Test[] matchTests() {
    return FindTest.FIND_TESTS;
  }

  private final FindTest.Test test;

  public RE2MatchTest(FindTest.Test findTest) {
    this.test = findTest;
  }

  @Test
  public void testMatch() {
    RE2 re = RE2.compile(test.pat);
    boolean m = re.match(test.text);
    if (m != (test.matches.length > 0)) {
      fail(String.format("RE2.match failure on %s: %s should be %s", test, m,
          test.matches.length > 0));
    }
    // now try bytes
    m = re.matchUTF8(GoTestCase.utf8(test.text));
    if (m != (test.matches.length > 0)) {
      fail(String.format("RE2.matchUTF8 failure on %s: %s should be %s", test, m,
          test.matches.length > 0));
    }
  }

  @Test
  public void testMatchFunction() {
    boolean m = RE2.match(test.pat, test.text);
    if (m != (test.matches.length > 0)) {
      fail(String.format("RE2.match failure on %s: %s should be %s", test, m,
          test.matches.length > 0));
    }
  }
}
