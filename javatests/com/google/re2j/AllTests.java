// Copyright 2011 Google Inc. All Rights Reserved.
package com.google.re2j;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests extends TestSuite {
  public static Test suite() {
    TestSuite suite = new TestSuite("com.google.re2j tests");
    suite.addTestSuite(CharClassTest.class);
    suite.addTestSuite(FindTest.class);
    suite.addTestSuite(ParserTest.class);
    suite.addTestSuite(ProgTest.class);
    suite.addTestSuite(RE2Test.class);
    suite.addTestSuite(MatcherTest.class);
    suite.addTestSuite(PatternTest.class);
    suite.addTestSuite(SimplifyTest.class);
    suite.addTestSuite(StrconvTest.class);
    suite.addTestSuite(UnicodeTest.class);
    return suite;
  }
}
