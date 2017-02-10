// Copyright 2010 The Go Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

// Original Go source here:
// http://code.google.com/p/go/source/browse/src/pkg/regexp/syntax/prog.go

package com.google.re2j;

import static com.google.re2j.Inst.Op.BYTE;

/**
 * A single instruction in the regular expression virtual machine.
 *
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
    BYTE,
    BYTE1
  }

  Op op;
  int out;  // all but MATCH, FAIL
  int arg;  // ALT, ALT_MATCH, CAPTURE, EMPTY_WIDTH
  byte[] byteRanges; // length==1 => exact match. Otherwise a list of [lo,hi] pairs.  hi is *inclusive*.

  Inst(Op op) {
    this.op = op;
  }

  // op() returns i.Op but merges all the byte special cases into BYTE
  // Beware "op" is a public field.
  Op op() {
    switch (op) {
      case BYTE1:
        return BYTE;
      default:
        return op;
    }
  }

  // MatchByte returns true if the instruction matches (and consumes) b.
  // It should only be called when op == InstByte.
  boolean matchByte(byte b) {
    // Special case: single-byte slice is from literal string, not byte range.
    if (byteRanges.length == 1) {
      int b0 = byteRanges[0];
      return b == b0;
    }

    // Search through all pairs.
    int byteInt = b & 0xff;
    for (int j = 0; j < byteRanges.length; j += 2) {
      if (byteInt < (byteRanges[j] & 0xff)) {
        return false;
      }
      if (byteInt <= (byteRanges[j + 1] & 0xff)) {
        return true;
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
      case BYTE:
        return "byte " + appendBytes() + " -> " + out;
      case BYTE1:
        return "byte1 " + appendBytes() + " -> " + out;
      default:
        throw new IllegalStateException("unhandled case in Inst.toString");
    }
  }

  private String appendBytes() {
    StringBuilder out = new StringBuilder();
    if (byteRanges.length == 1) {
      out.append(byteRanges[0] & 0xff);
    } else {
      for (int i = 0; i < byteRanges.length; i += 2) {
        out.append("[")
            .append(byteRanges[i] & 0xff)
            .append(",")
            .append(byteRanges[i + 1] & 0xff)
            .append("]");
        if (i < byteRanges.length - 2) {
          out.append(";");
        }
      }
    }
    return out.toString();
  }
}
