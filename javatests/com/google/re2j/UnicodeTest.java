/*
 * Copyright (c) 2020 The Go Authors. All rights reserved.
 *
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
package com.google.re2j;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class UnicodeTest {

  @Test
  public void testFoldConstants() {
    int last = -1;
    for (int i = 0; i <= Unicode.MAX_RUNE; i++) {
      if (Unicode.simpleFold(i) == i) {
        continue;
      }
      if (last == -1 && Unicode.MIN_FOLD != i) {
        fail(String.format("MIN_FOLD=#%04X should be #%04X", Unicode.MIN_FOLD, i));
      }
      last = i;
    }
    if (Unicode.MAX_FOLD != last) {
      fail(String.format("MAX_FOLD=#%04X should be #%04X", Unicode.MAX_FOLD, last));
    }
  }

  @Test
  public void testEqualsIgnoreCase() {
    List<EqualsIgnoreCaseTest> testCases = new ArrayList<EqualsIgnoreCaseTest>();

    for (int r = 'a'; r <= 'z'; r++) {
      int u = r - ('a' - 'A');
      testCases.add(new EqualsIgnoreCaseTest(r, r, true));
      testCases.add(new EqualsIgnoreCaseTest(u, u, true));
      testCases.add(new EqualsIgnoreCaseTest(r, u, true));
      testCases.add(new EqualsIgnoreCaseTest(u, r, true));
    }

    testCases.add(new EqualsIgnoreCaseTest('{', '{', true));
    testCases.add(new EqualsIgnoreCaseTest('é', 'É', true));
    testCases.add(new EqualsIgnoreCaseTest('Ú', 'ú', true));
    testCases.add(new EqualsIgnoreCaseTest('\u212A', 'K', true));
    testCases.add(new EqualsIgnoreCaseTest('\u212A', 'k', true));

    testCases.add(new EqualsIgnoreCaseTest('\u212A', 'a', false));
    testCases.add(new EqualsIgnoreCaseTest('ü', 'ű', false));
    testCases.add(new EqualsIgnoreCaseTest('b', 'k', false));
    testCases.add(new EqualsIgnoreCaseTest('C', 'x', false));
    testCases.add(new EqualsIgnoreCaseTest('/', '_', false));
    testCases.add(new EqualsIgnoreCaseTest('d', ')', false));
    testCases.add(new EqualsIgnoreCaseTest('@', '`', false));

    for (EqualsIgnoreCaseTest testCase : testCases) {
      boolean equals = Unicode.equalsIgnoreCase(testCase.r1, testCase.r2);

      if (testCase.shouldMatch) {
        assertTrue((char) testCase.r1 + " should be equal to " + (char) testCase.r2, equals);
      } else {
        assertFalse((char) testCase.r1 + " should not be equal to " + (char) testCase.r2, equals);
      }
    }
  }

  // EqualsIgnoreCaseTest wraps test case parameters for testEqualsIgnoreCase().
  private static class EqualsIgnoreCaseTest {
    private final int r1;
    private final int r2;
    private final boolean shouldMatch;

    public EqualsIgnoreCaseTest(int r1, int r2, boolean shouldMatch) {
      this.r1 = r1;
      this.r2 = r2;
      this.shouldMatch = shouldMatch;
    }
  }

  // TODO(adonovan): tests for:
  //
  // boolean isUpper(int r);
  // boolean isLower(int r);
  // boolean isTitle(int r);
  // boolean isPrint(int r);
  // int to(int _case, int r, int[][] caseRange);
  // int toUpper(int r);
  // int toLower(int r);
  // int simpleFold(int r);

}
