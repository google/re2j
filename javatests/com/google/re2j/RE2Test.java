// Copyright 2011 The Go Authors.  All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

// Original Go source here:
// http://code.google.com/p/go/source/browse/src/pkg/regexp/syntax/all_test.go

package com.google.re2j;

import static com.google.re2j.RE2.Anchor.ANCHOR_BOTH;
import static com.google.re2j.RE2.Anchor.UNANCHORED;
import static io.airlift.slice.Slices.utf8Slice;
import static org.junit.Assert.assertEquals;

import org.junit.Test;


/** Tests of RE2 API. */
public class RE2Test {
  @Test
  public void testFullMatch() {
    assertEquals(true, new RE2("ab+c").match(
        utf8Slice("abbbbbc"), 0, ANCHOR_BOTH, null, 0));
    assertEquals(false, new RE2("ab+c").match(
      utf8Slice("xabbbbbc"), 0, ANCHOR_BOTH, null, 0));
  }

  @Test
  public void testFindEnd() {
    RE2 r = new RE2("abc.*def");
    assertEquals(true, r.match(utf8Slice("yyyabcxxxdefzzz"),
                               0, UNANCHORED, null, 0));
    assertEquals(true, r.match(utf8Slice("yyyabcxxxdefzzz"),
                               3, UNANCHORED, null, 0));
    assertEquals(false, r.match(utf8Slice("yyyabcxxxdefzzz"),
                                4, UNANCHORED, null, 0));
    assertEquals(false, r.match(utf8Slice("abcxxxde"),
                                3, UNANCHORED, null, 0));
  }
}
