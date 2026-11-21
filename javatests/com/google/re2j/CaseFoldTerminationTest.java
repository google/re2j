/*
 * Copyright (c) 2020 The Go Authors. All rights reserved.
 *
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
package com.google.re2j;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests that simple-fold orbit walks terminate and stay shape-consistent for runes whose case
 * mappings postdate the generated table's Unicode version.
 *
 * <p>
 * {@code Unicode.simpleFold}'s fallback follows {@code toLower}/{@code toUpper} on table misses,
 * assuming they form closed two-element orbits with the rune. That holds for symmetric pairs, but
 * runes whose asymmetric mappings postdate the table (U+1C80..U+1C88, Cyrillic historic letters,
 * Unicode 9.0 vs tables at 6.0) step into the partner's own orbit and never cycle back: compiling
 * or matching such patterns used to hang forever. The fallback now follows a mapping only when the
 * partner maps back (a verified symmetric pair), so those runes are fold-inert — consistently in
 * literal and class form, in both directions — and every walk terminates.
 */
@RunWith(JUnit4.class)
public class CaseFoldTerminationTest {

  private static Matcher matcher(String pattern, String input) {
    return Pattern.compile(pattern).matcher(input);
  }

  @Test
  public void foldWalkTerminatesOnHistoricCyrillicLiterals() {
    // Every one of U+1C80..U+1C88 used to hang the compile under (?i).
    for (char c = '\u1C80'; c <= '\u1C88'; c++) {
      Pattern.compile("(?i)" + c);
    }
    Pattern.compile("(?i)[\u1C80-\u1C88]");
  }

  @Test
  public void historicCyrillicLettersAreFoldInert() {
    // The fallback declines the asymmetric mappings, so the letters fold
    // nowhere — same answer from every pattern shape, in both directions.
    for (char c = '\u1C80'; c <= '\u1C88'; c++) {
      assertTrue(matcher("(?i)" + c, String.valueOf(c)).find());
      assertTrue(matcher("(?i)[" + c + "]", String.valueOf(c)).find());
      assertFalse(matcher("(?i)" + c, "x").find());
    }
    // U+1C80 (Ꚁ) case-maps onto В/в; none of the three folds onto the others.
    assertFalse(matcher("(?i)\u1C80", "\u0412").find());
    assertFalse(matcher("(?i)\u1C80", "\u0432").find());
    assertFalse(matcher("(?i)\u0432", "\u1C80").find());
    assertFalse(matcher("(?i)[\u1C80]", "\u0432").find());
    assertFalse(matcher("(?i)[\u0432]", "\u1C80").find());
    // The partner pair itself keeps folding (symmetric, table-era data).
    assertTrue(matcher("(?i)\u0432", "\u0412").find());
    assertTrue(matcher("(?i)\u0412", "\u0432").find());
  }

  @Test
  public void symmetricPairsStillFold() {
    // Plain two-element orbits via the fallback ...
    assertTrue(matcher("(?i)z", "Z").find());
    assertTrue(matcher("(?i)\u0442", "\u0422").find());
    // ... and a three-element orbit from the table (K, k, Kelvin sign).
    assertTrue(matcher("(?i)k", "K").find());
    assertTrue(matcher("(?i)k", "\u212A").find());
    assertTrue(matcher("(?i)\u212A", "k").find());
  }

  @Test
  public void matchTimeFoldComparisonTerminates() {
    // equalsIgnoreCase walks the same orbit graph at match time.
    assertTrue(matcher("(?i)\u0442", "\u0442").find());
    assertFalse(matcher("(?i)\u0442", "x").find());
  }
}
