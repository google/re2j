/*
 * Copyright (c) 2020 The Go Authors. All rights reserved.
 *
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
package com.google.re2j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests that matches never start inside a well-formed surrogate pair during a scan, regardless of
 * pattern shape, while an explicitly given pair-interior search start is honored.
 *
 * <p>
 * The unanchored-match fast path jumps to raw {@link String#indexOf} hits, whose UTF-16 unit
 * indices can land on the low half of a pair. Other code paths decode codepoints and never enter a
 * pair interior, so before the fix the result depended on whether the pattern compiled to a
 * singleton literal prefix: {@code "\uDC21"} matched inside a pair while the equivalent
 * {@code "\uDC21|\uDC22"} and {@code "[\uD800-\uDFFF]"} did not.
 */
@RunWith(JUnit4.class)
public class SurrogatePairTest {

  // 'a', well-formed pair U+10421 (low half is \uDC21), 'b'.
  private static final String PAIR_DC21 = "a\uD801\uDC21b";

  private static Matcher matcher(String pattern, String input) {
    return Pattern.compile(pattern).matcher(input);
  }

  @Test
  public void loneLowSurrogateDoesNotMatchInsidePair() {
    assertFalse(matcher("\uDC21", PAIR_DC21).find());
  }

  @Test
  public void patternShapeDoesNotChangeResult() {
    // These describe (near-)identical languages and must agree; widening the
    // language must never remove matches.
    String[] equivalents = {
      "\uDC21", "[\uDC21]", "\uDC21|\uDC22", "\uDC21|\uDC23", "[\uDC21\uDC22]", "[\uDC21-\uDC22]",
      "[\uD800-\uDFFF]",
    };
    for (String pattern : equivalents) {
      assertFalse(pattern, matcher(pattern, PAIR_DC21).find());
    }
  }

  @Test
  public void loneHighSurrogateDoesNotMatchInsidePair() {
    assertFalse(matcher("\uD801", PAIR_DC21).find());
  }

  @Test
  public void searchResumesAfterSkippedInteriorHit() {
    // Units 0-1 are a pair (interior hit at 1 must be skipped), unit 2 is a
    // real lone \uDC21. The skip must keep searching, not give up.
    Matcher m = matcher("\uDC21", "\uD801\uDC21\uDC21");
    assertTrue(m.find());
    assertEquals(2, m.start());
    assertEquals(3, m.end());
  }

  @Test
  public void loneSurrogatesAtRealBoundariesStillMatch() {
    Matcher m = matcher("\uDC21|\uDC22", "a\uDC22\uDC21b");
    assertTrue(m.find());
    assertEquals(1, m.start());
    assertTrue(matcher("\uDC21", "a\uDC21b").find());
    assertTrue(matcher("[\uD800-\uDFFF]", "\uD801b").find());
  }

  @Test
  public void explicitInteriorStartIsHonored() {
    // JDK parity: find(int) handed a position that IS a pair's low half
    // matches there. The interior skip governs scanning, not the explicitly
    // given start — refusing it would also diverge from this library's own
    // non-prefix search paths, which honor the given start.
    Matcher m = matcher("\uDC21", PAIR_DC21);
    assertTrue(m.find(2));
    assertEquals(2, m.start());
    assertEquals(3, m.end());
  }

  @Test
  public void supplementaryMatchingIsUnchanged() {
    Matcher m = matcher("\uD801\uDC21", PAIR_DC21);
    assertTrue(m.find());
    assertEquals(1, m.start());
    assertEquals(3, m.end());
  }
}
