// Copyright 2011 The Go Authors.  All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

// Original Go source here:
// http://code.google.com/p/go/source/browse/src/pkg/regexp/syntax/all_test.go

package com.google.re2j;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static com.google.re2j.RE2.Anchor.ANCHOR_BOTH;
import static com.google.re2j.RE2.Anchor.UNANCHORED;
import static io.airlift.slice.Slices.utf8Slice;
import static org.junit.Assert.assertEquals;


/**
 * Tests of RE2 API.
 */
@RunWith(Enclosed.class)
public class RE2Test {

  public static class DFA extends RE2TestBase {
    public DFA() {
      super(RUN_WITH_DFA);
    }
  }

  public static class NFA extends RE2TestBase {
    public NFA() {
      super(RUN_WITH_NFA);
    }
  }

  public static abstract class RE2TestBase extends OptionsTest {

    protected RE2TestBase(Options options) {
      super(options);
    }

    @Test
    public void testFullMatch() {
      assertEquals(true, RE2.compile("ab+c", options).match(
          utf8Slice("abbbbbc"), 0, ANCHOR_BOTH, null, 0));
      assertEquals(false, RE2.compile("ab+c", options).match(
          utf8Slice("xabbbbbc"), 0, ANCHOR_BOTH, null, 0));
    }

    @Test
    public void testFindEnd() {
      RE2 r = RE2.compile("abc.*def", options);
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
}
