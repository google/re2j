// Copyright 2010 The Go Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

// Original Go source here:
// http://code.google.com/p/go/source/browse/src/pkg/regexp/regexp.go

package com.google.re2j;

import io.airlift.slice.Slice;
import io.airlift.slice.Slices;

/**
 * MachineInput represents the UTF-8 input text supplied to the Machine. It provides one-character
 * lookahead.
 */
class MachineInput {

  static final int EOF = (-1 << 3) | 0;

  static MachineInput fromUTF8(byte[] b) {
    return fromUTF8(b, 0, b.length);
  }

  static MachineInput fromUTF8(byte[] b, int start, int end) {
    return fromUTF8(Slices.wrappedBuffer(b, start, end));
  }

  static MachineInput fromUTF8(Slice slice) {
    return new MachineInput(slice);
  }

  final Slice slice;

  MachineInput(Slice slice) {
    this.slice = slice;
  }

  // Returns the rune at the specified index; the units are
  // unspecified, but could be UTF-8 byte, UTF-16 char, or rune
  // indices.  Returns the width (in the same units) of the rune in
  // the lower 3 bits, and the rune (Unicode code point) in the high
  // bits.  Never negative, except for EOF which is represented as -1
  // << 3 | 0.
  int step(int i) {
    if (i >= slice.length()) {
      return EOF;
    }

    // UTF-8.  RFC 3629 in five lines:
    //
    // Unicode code points            UTF-8 encoding (binary)
    //         00-7F  (7 bits)   0tuvwxyz
    //     0080-07FF (11 bits)   110pqrst 10uvwxyz
    //     0800-FFFF (16 bits)   1110jklm 10npqrst 10uvwxyz
    // 010000-10FFFF (21 bits)   11110efg 10hijklm 10npqrst 10uvwxyz
    int x = slice.getByte(i++) & 0xff;  // zero extend
    if ((x & 0x80) == 0) {
      return x << 3 | 1;
    } else if ((x & 0xE0) == 0xC0) {  // 110xxxxx
      x = x & 0x1F;
      if (i >= slice.length()) {
        return EOF;
      }
      x = x << 6 | slice.getByte(i++) & 0x3F;
      return x << 3 | 2;
    } else if ((x & 0xF0) == 0xE0) {  // 1110xxxx
      x = x & 0x0F;
      if (i + 1 >= slice.length()) {
        return EOF;
      }
      x = x << 6 | slice.getByte(i++) & 0x3F;
      x = x << 6 | slice.getByte(i++) & 0x3F;
      return x << 3 | 3;
    } else {  // 11110xxx
      x = x & 0x07;
      if (i + 2 >= slice.length()) {
        return EOF;
      }
      x = x << 6 | slice.getByte(i++) & 0x3F;
      x = x << 6 | slice.getByte(i++) & 0x3F;
      x = x << 6 | slice.getByte(i++) & 0x3F;
      return x << 3 | 4;
    }
  }

  // can we look ahead without losing info?
  boolean canCheckPrefix() {
    return true;
  }

  // Returns the index relative to |pos| at which |re2.prefix| is found
  // in this input stream, or a negative value if not found.
  int index(RE2 re2, int pos) {
    int i = Utils.indexOf(slice, re2.prefixUTF8, pos);
    return i < 0 ? i : i - pos;
  }

  // Returns a bitmask of EMPTY_* flags.
  int context(int pos) {
    int r1 = -1;
    if (pos > 0 && pos <= slice.length()) {
      int start = pos - 1;
      r1 = slice.getByte(start--);
      if (r1 >= 0x80) {  // decode UTF-8
        // Find start, up to 4 bytes earlier.
        int lim = pos - 4;
        if (lim < 0) {
          lim = 0;
        }
        while (start >= lim && (slice.getByte(start) & 0xC0) == 0x80) {  // 10xxxxxx
          start--;
        }
        if (start < 0) {
          start = 0;
        }
        r1 = step(start) >> 3;
      }
    }
    int r2 = pos < slice.length()
        ? (step(pos) >> 3)
        : -1;
    return Utils.emptyOpContext(r1, r2);
  }

  // Returns the end position in the same units as step().
  int endPos() {
    return slice.length();
  }
}
