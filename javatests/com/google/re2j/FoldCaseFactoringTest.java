/*
 * Copyright (c) 2026 The Go Authors. All rights reserved.
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
 * Tests that alternation factoring keeps case-insensitive and case-sensitive literals apart.
 *
 * <p>
 * Regexp.equals ignored FoldCase for LITERAL/CHAR_CLASS, so Parser.factor merged a folded literal
 * with its case-sensitive twin and the fold leaked into the merged arm: {@code "(?i:Z)x|Z"} matched
 * lowercase {@code "z"}.
 */
@RunWith(JUnit4.class)
public class FoldCaseFactoringTest {

  @Test
  public void foldDoesNotLeakIntoCaseSensitiveArm() {
    assertFalse(Pattern.compile("(?i:Z)x|Z").matcher("z").find());
    assertFalse(Pattern.compile("(?i:[Z])x|[Z]").matcher("z").find());
  }

  @Test
  public void bothArmsStillMatch() {
    assertTrue(Pattern.compile("(?i:Z)x|Z").matcher("Zx").find()); // folded arm
    assertTrue(Pattern.compile("(?i:Z)x|Z").matcher("Z").find()); // case-sensitive arm
  }

  @Test
  public void foldedArmStillFolds() {
    assertTrue(Pattern.compile("(?i:Z)x|Z").matcher("zx").find());
  }
}
