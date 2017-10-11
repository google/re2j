// Copyright 2010 The Go Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

// Original Go source here:
// http://code.google.com/p/go/source/browse/src/pkg/regexp/syntax/prog.go

package com.google.re2j;

/**
 * A single instruction in the regular expression virtual machine.
 * @see http://swtch.com/~rsc/regexp/regexp2.html
 */
class Inst {

  enum Op {
    ALT,
    ALT_MATCH,
    CAPTURE,
    EMPTY_WIDTH,
    FAIL,
    MATCH,
    NOP,
    RUNE,
    RUNE1,
    RUNE_ANY,
    RUNE_ANY_NOT_NL,
  }

  Op op;
  int out;  // all but MATCH, FAIL
  int arg;  // ALT, ALT_MATCH, CAPTURE, EMPTY_WIDTH
  int[] runes;  // length==1 => exact match
                // otherwise a list of [lo,hi] pairs.  hi is *inclusive*.
                // REVIEWERS: why not half-open intervals?

  Inst(Op op) {
    this.op = op;
  }

  // op() returns i.Op but merges all the rune special cases into RUNE
  // Beware "op" is a public field.
  Op op() {
    switch (op) {
      case RUNE1:
      case RUNE_ANY:
      case RUNE_ANY_NOT_NL:
        return Op.RUNE;
      default:
        return op;
    }
  }

  // MatchRune returns true if the instruction matches (and consumes) r.
  // It should only be called when op == InstRune.
  boolean matchRune(int r) {
    // Special case: single-rune slice is from literal string, not char
    // class.
    if (runes.length == 1) {
      return singleMatchRune(r);
    }
    
    return multiMatchRune(r);
  }

  private boolean singleMatchRune(int r) {
    int r0 = runes[0];
    if (r == r0) {
      return true;
    }

    if ((arg & RE2.FOLD_CASE) != 0) {
      int[] folds = Unicode.optimizedFoldOrbit(r0);

      if (folds == null) {
        return Unicode.areEqualsCaseInsensitive(r, r0);
      } else {
        for(int i = 0; i < folds.length; i++) {
          if (folds[i] == r) return true;
        }
      }
    }
    return false;
  }


  private boolean multiMatchRune(int r) {
    // Peek at the first 5 pairs.
    // Should handle ASCII well.
    int length = Math.min(runes.length, 10);
    for (int j = 0; j < length; j += 2) {
      if (r < runes[j]) {
        return false;
      }
      if (r <= runes[j + 1]) {
        return true;
      }
    }

    // Otherwise binary search
    // Invariant: lo, hi, m are even.
    for (int lo = 0, hi = runes.length / 2; lo < hi; ) {
      int m = lo + (hi - lo) / 2;
      int c = runes[2 * m];
      if (c <= r) {
        if (r <= runes[2 * m + 1]) {
          return true;
        }
        lo = m + 1;
      } else {
        hi = m;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    switch (op) {
      case ALT:
        return "alt -> " + out + ", " + arg;
      case ALT_MATCH:
        return "altmatch -> " + out + ", " + arg;
      case CAPTURE:
        return "cap " + arg + " -> " + out;
      case EMPTY_WIDTH:
        return "empty " + arg + " -> " + out;
      case MATCH:
        return "match";
      case FAIL:
        return "fail";
      case NOP:
        return "nop -> " + out;
      case RUNE:
        if (runes == null) {
          return "rune <null>";  // can't happen
        }
        return "rune " + escapeRunes(runes) +
            (((arg & RE2.FOLD_CASE) != 0) ? "/i" : "") + " -> " + out;
      case RUNE1:
        return "rune1 " + escapeRunes(runes) + " -> " + out;
      case RUNE_ANY:
        return "any -> " + out;
      case RUNE_ANY_NOT_NL:
        return "anynotnl -> " + out;
      default:
        throw new IllegalStateException("unhandled case in Inst.toString");
    }
  }

  // Returns an RE2 expression matching exactly |runes|.
  private static String escapeRunes(int[] runes) {
    StringBuilder out = new StringBuilder();
    out.append('"');
    for (int rune : runes) {
      Utils.escapeRune(out, rune);
    }
    out.append('"');
    return out.toString();
  }

}
