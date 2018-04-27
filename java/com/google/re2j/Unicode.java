// Copyright 2010 The Go Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

// Many of these were derived from the corresponding Go functions in
// http://code.google.com/p/go/source/browse/src/pkg/unicode/letter.go

package com.google.re2j;

/**
 * Utilities for dealing with Unicode better than Java does.
 *
 * @author adonovan@google.com (Alan Donovan)
 */
class Unicode {

  // The highest legal rune value.
  static final int MAX_RUNE = 0x10FFFF;

  // The highest legal ASCII value.
  static final int MAX_ASCII = 0x7f;

  // The highest legal Latin-1 value.
  static final int MAX_LATIN1 = 0xFF;

  private static final int MAX_CASE = 3;

  // Represents invalid code points.
  private static final int REPLACEMENT_CHAR = 0xFFFD;

  // Minimum and maximum runes involved in folding.
  // Checked during test.
  static final int MIN_FOLD = 0x0041;
  static final int MAX_FOLD = 0x1044f;

  // is32 uses binary search to test whether rune is in the specified
  // slice of 32-bit ranges.
  // TODO(adonovan): opt: consider using int[n*3] instead of int[n][3].
  private static boolean is32(int[][] ranges, int r) {
    // binary search over ranges
    for (int lo = 0, hi = ranges.length; lo < hi; ) {
      int m = lo + (hi - lo) / 2;
      int[] range = ranges[m]; // [lo, hi, stride]
      if (range[0] <= r && r <= range[1]) {
        return ((r - range[0]) % range[2]) == 0;
      }
      if (r < range[0]) {
        hi = m;
      } else {
        lo = m + 1;
      }
    }
    return false;
  }

  // is tests whether rune is in the specified table of ranges.
  private static boolean is(int[][] ranges, int r) {
    // common case: rune is ASCII or Latin-1, so use linear search.
    if (r <= MAX_LATIN1) {
      for (int[] range : ranges) { // range = [lo, hi, stride]
        if (r > range[1]) {
          continue;
        }
        if (r < range[0]) {
          return false;
        }
        return ((r - range[0]) % range[2]) == 0;
      }
      return false;
    }
    return ranges.length > 0 && r >= ranges[0][0] && is32(ranges, r);
  }

  // isUpper reports whether the rune is an upper case letter.
  static boolean isUpper(int r) {
    // See comment in isGraphic.
    if (r <= MAX_LATIN1) {
      return Character.isUpperCase((char) r);
    }
    return is(UnicodeTables.Upper, r);
  }

  // isLower reports whether the rune is a lower case letter.
  static boolean isLower(int r) {
    // See comment in isGraphic.
    if (r <= MAX_LATIN1) {
      return Character.isLowerCase((char) r);
    }
    return is(UnicodeTables.Lower, r);
  }

  // isTitle reports whether the rune is a title case letter.
  static boolean isTitle(int r) {
    if (r <= MAX_LATIN1) {
      return false;
    }
    return is(UnicodeTables.Title, r);
  }

  // isPrint reports whether the rune is printable (Unicode L/M/N/P/S or ' ').
  static boolean isPrint(int r) {
    if (r <= MAX_LATIN1) {
      return r >= 0x20 && r < 0x7F || r >= 0xA1 && r != 0xAD;
    }
    return is(UnicodeTables.L, r)
        || is(UnicodeTables.M, r)
        || is(UnicodeTables.N, r)
        || is(UnicodeTables.P, r)
        || is(UnicodeTables.S, r);
  }

  // A case range is conceptually a record:
  // class CaseRange {
  //   int lo, hi;
  //   int upper, lower, title;
  // }
  // but flattened as an int[5].

  // to maps the rune using the specified case mapping.
  private static int to(int kase, int r, int[][] caseRange) {
    if (kase < 0 || MAX_CASE <= kase) {
      return REPLACEMENT_CHAR; // as reasonable an error as any
    }
    // binary search over ranges
    for (int lo = 0, hi = caseRange.length; lo < hi; ) {
      int m = lo + (hi - lo) / 2;
      int[] cr = caseRange[m]; // cr = [lo, hi, upper, lower, title]
      int crlo = cr[0];
      int crhi = cr[1];
      if (crlo <= r && r <= crhi) {
        int delta = cr[2 + kase];
        if (delta > MAX_RUNE) {
          // In an Upper-Lower sequence, which always starts with
          // an UpperCase letter, the real deltas always look like:
          //      {0, 1, 0}    UpperCase (Lower is next)
          //      {-1, 0, -1}  LowerCase (Upper, Title are previous)
          // The characters at even offsets from the beginning of the
          // sequence are upper case; the ones at odd offsets are lower.
          // The correct mapping can be done by clearing or setting the low
          // bit in the sequence offset.
          // The constants UpperCase and TitleCase are even while LowerCase
          // is odd so we take the low bit from kase.
          return crlo + (((r - crlo) & ~1) | (kase & 1));
        }
        return r + delta;
      }
      if (r < crlo) {
        hi = m;
      } else {
        lo = m + 1;
      }
    }
    return r;
  }

  // to maps the rune to the specified case: UpperCase, LowerCase, or TitleCase.
  private static int to(int kase, int r) {
    return to(kase, r, UnicodeTables.CASE_RANGES);
  }

  // toUpper maps the rune to upper case.
  static int toUpper(int r) {
    if (r <= MAX_ASCII) {
      if ('a' <= r && r <= 'z') {
        r -= 'a' - 'A';
      }
      return r;
    }
    return to(UnicodeTables.UpperCase, r);
  }

  // toLower maps the rune to lower case.
  static int toLower(int r) {
    if (r <= MAX_ASCII) {
      if ('A' <= r && r <= 'Z') {
        r += 'a' - 'A';
      }
      return r;
    }
    return to(UnicodeTables.LowerCase, r);
  }

  // simpleFold iterates over Unicode code points equivalent under
  // the Unicode-defined simple case folding.  Among the code points
  // equivalent to rune (including rune itself), SimpleFold returns the
  // smallest r >= rune if one exists, or else the smallest r >= 0.
  //
  // For example:
  //      SimpleFold('A') = 'a'
  //      SimpleFold('a') = 'A'
  //
  //      SimpleFold('K') = 'k'
  //      SimpleFold('k') = '\u212A' (Kelvin symbol, K)
  //      SimpleFold('\u212A') = 'K'
  //
  //      SimpleFold('1') = '1'
  //
  // Derived from Go's unicode.SimpleFold.
  //
  static int simpleFold(int r) {
    // Consult caseOrbit table for special cases.
    int lo = 0;
    int hi = UnicodeTables.CASE_ORBIT.length;
    while (lo < hi) {
      int m = lo + (hi - lo) / 2;
      if (UnicodeTables.CASE_ORBIT[m][0] < r) {
        lo = m + 1;
      } else {
        hi = m;
      }
    }
    if (lo < UnicodeTables.CASE_ORBIT.length && UnicodeTables.CASE_ORBIT[lo][0] == r) {
      return UnicodeTables.CASE_ORBIT[lo][1];
    }

    // No folding specified.  This is a one- or two-element
    // equivalence class containing rune and toLower(rune)
    // and toUpper(rune) if they are different from rune.
    int l = toLower(r);
    if (l != r) {
      return l;
    }
    return toUpper(r);
  }

  private Unicode() {} // uninstantiable
}
