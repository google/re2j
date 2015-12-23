// Copyright 2015 The RE2 Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

// Original RE2 source here:
// https://github.com/google/re2/blob/master/re2/testing/dfa_test.cc

package com.google.re2j;

import com.google.re2j.DFA.DFATooManyStatesException;

import org.junit.Test;

import io.airlift.slice.Slice;

import static com.google.re2j.Options.Algorithm.DFA;
import static com.google.re2j.Options.Algorithm.DFA_FALLBACK_TO_NFA;
import static io.airlift.slice.Slices.utf8Slice;
import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FallbackToNFATest {
  // Choice of n is mostly arbitrary, except that:
  //   * making n too big makes the test run for too long.
  //   * making n too small makes the DFA refuse to run,
  //     because it has so little memory compared to the program size.
  // Empirically, n = 18 is a good compromise between the two.
  private static final int N = 18;

  // The De Bruijn string for n ends with a 1 followed by n 0s in a row,
  // which is not a match for 0[01]{n}$.  Adding one more 0 is a match.
  private static final String NO_MATCH_STRING = deBruijnString(N);
  private static final Slice NO_MATCH = utf8Slice(NO_MATCH_STRING);
  private static final Slice MATCH = utf8Slice(NO_MATCH_STRING + "0");

  // A number of fallbackToNFA() function invocations
  private int fallbackToNFAInvocations = 0;

  /**
   * Test that the DFA_FALLBACK_TO_NFA algorithm gets the right result even if it runs out of states
   * during a search. The regular expression 0[01]{n}$ matches a binary string of 0s and 1s only if
   * the (n+1)th-to-last character is a 0. Matching this in a single forward pass (as done by the
   * DFA) requires keeping one bit for each of the last n+1 characters (whether each was a 0), or
   * 2^(n+1) possible states. If we run this regexp to search in a string that contains every
   * possible n-character binary string as a substring, then it will have to run through at least
   * 2^n states -- so if the DFA can search correctly while staying within a 2^(n - 1) states limit,
   * it must be handling out-of-memory conditions gracefully.
   */
  @Test
  public void testFallbackToNFA() {
    Options options = Options.builder()
        .setAlgorithm(DFA_FALLBACK_TO_NFA)
        .setMaximumNumberOfDFAStates(1 << (N - 1))
        .setNumberOfDFARetries(2)
        .setEventsListener(new TestEventsListener())
        .build();

    Pattern pattern = Pattern.compile(format("0[01]{%d}$", N), options);

    // it should retry DFA for the first two times
    for (int i = 0; i < 2; ++i) {
      Matcher matcher = pattern.matcher(MATCH);
      assertTrue(matcher.find());
      assertEquals(fallbackToNFAInvocations, 0);
    }

    // for the third time it should fallback to NFA
    Matcher matcher = pattern.matcher(MATCH);
    assertTrue(matcher.find());
    assertEquals(fallbackToNFAInvocations, 1);

    // it should keep using NFA
    fallbackToNFAInvocations = 0;
    matcher = pattern.matcher(NO_MATCH);
    assertFalse(matcher.find());
    assertEquals(fallbackToNFAInvocations, 0);
  }

  @Test(expected = DFATooManyStatesException.class)
  public void testFailsTooManyDFAStates() {
    Options options = Options.builder()
        .setAlgorithm(DFA)
        .setMaximumNumberOfDFAStates(1 << (N - 1))
        .setNumberOfDFARetries(2)
        .build();

    Pattern pattern = Pattern.compile(format("0[01]{%d}$", N), options);

    for (int i = 0; i < 3; ++i) {
      pattern.matcher(MATCH).find();
    }
  }

  private class TestEventsListener implements Options.EventsListener {
    @Override
    public void fallbackToNFA() {
      fallbackToNFAInvocations++;
    }
  }

  /**
   * Generates and returns a string over binary alphabet {0,1} that contains all possible binary
   * sequences of length n as subsequences.  The obvious brute force method would generate a string
   * of length n * 2^n, but this generates a string of length n + 2^n - 1 called a De Bruijn cycle.
   * See Knuth, The Art of Computer Programming, Vol 2, Exercise 3.2.2 #17. Such a string is useful
   * for testing a DFA.  If you have a DFA where distinct last n bytes implies distinct states, then
   * running on a DeBruijn string causes the DFA to need to create a new state at every position in
   * the input, never reusing any states until it gets to the end of the string.  This is the worst
   * possible case for DFA execution.
   */
  private static String deBruijnString(int n) {
    boolean[] did = new boolean[1 << n];

    StringBuilder s = new StringBuilder();
    for (int i = 0; i < n - 1; i++) {
      s.append("0");
    }
    int bits = 0;
    int mask = (1 << n) - 1;
    for (int i = 0; i < (1 << n); i++) {
      bits <<= 1;
      bits &= mask;
      if (!did[bits | 1]) {
        bits |= 1;
        s.append("1");
      } else {
        s.append("0");
      }
      did[bits] = true;
    }
    return s.toString();
  }
}
