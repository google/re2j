// Copyright 2010 The Go Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

// Original Go source here:
// http://code.google.com/p/go/source/browse/src/pkg/regexp/regexp.go

package com.google.re2j;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

import io.airlift.slice.Slice;

/**
 * MachineInput represents the UTF-8 input text supplied to the Machine. It provides one-character
 * lookahead.
 */
final class MachineInput {

  private static final Unsafe unsafe;

  static {
    try {
      // fetch theUnsafe object
      Field field = Unsafe.class.getDeclaredField("theUnsafe");
      field.setAccessible(true);
      unsafe = (Unsafe) field.get(null);
      if (unsafe == null) {
        throw new RuntimeException("Unsafe access not available");
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  static final byte EOF = -1;

  static MachineInput fromUTF8(Slice slice) {
    return new MachineInput(slice);
  }

  final Slice slice;
  final Object base;
  final long address;
  final int length;

  MachineInput(Slice slice) {
    this.slice = slice;
    this.base = slice.getBase();
    this.address = slice.getAddress();
    this.length = slice.length();
  }

  // Returns the byte at the specified index.
  byte getByte(int i) {
    if (i >= length) {
      return EOF;
    }

    if (i < 0) {
      throw new IndexOutOfBoundsException("index less than zero (" + i + ")");
    }

    return getByteUnchecked(i);
  }

  byte getByteUnchecked(int i) {
    return unsafe.getByte(base, address + i);
  }

  // Returns the index relative to |pos| at which |re2.prefix| is found
  // in this input stream, or a negative value if not found.
  int index(RE2 re2, int pos) {
    int i = Utils.indexOf(slice, re2.prefixUTF8, pos);
    return i < 0 ? i : i - pos;
  }

  // Returns the end position in the same units as step().
  int endPos() {
    return length;
  }
}
