// Copyright 2011 The Go Authors.  All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package com.google.re2j;

import static org.junit.Assert.fail;

import org.junit.Test;

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
