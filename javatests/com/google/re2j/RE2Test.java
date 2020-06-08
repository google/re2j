// Copyright 2011 The Go Authors.  All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

// Original Go source here:
// http://code.google.com/p/go/source/browse/src/pkg/regexp/syntax/all_test.go

package com.google.re2j;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Tests of RE2 API. */
public class RE2Test {
  @Test
  public void testFullMatch() {
    assertTrue(new RE2("ab+c").match("abbbbbc", 0, 7, RE2.ANCHOR_BOTH, null, 0));
    assertFalse(new RE2("ab+c").match("xabbbbbc", 0, 8, RE2.ANCHOR_BOTH, null, 0));
  }

  @Test
  public void testFindEnd() {
    RE2 r = new RE2("abc.*def");
    assertTrue(r.match("yyyabcxxxdefzzz", 0, 15, RE2.UNANCHORED, null, 0));
    assertTrue(r.match("yyyabcxxxdefzzz", 0, 12, RE2.UNANCHORED, null, 0));
    assertTrue(r.match("yyyabcxxxdefzzz", 3, 15, RE2.UNANCHORED, null, 0));
    assertTrue(r.match("yyyabcxxxdefzzz", 3, 12, RE2.UNANCHORED, null, 0));
    assertFalse(r.match("yyyabcxxxdefzzz", 4, 12, RE2.UNANCHORED, null, 0));
    assertFalse(r.match("yyyabcxxxdefzzz", 3, 11, RE2.UNANCHORED, null, 0));
  }
}
