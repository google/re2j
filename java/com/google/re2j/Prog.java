// Copyright 2010 The Go Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

// Original Go source here:
// http://code.google.com/p/go/source/browse/src/pkg/regexp/syntax/prog.go

package com.google.re2j;

/**
 * A Prog is a compiled regular expression program.
 */
final class Prog {

  
  final Inst[] inst;
  final int start; // index of start instruction
  final int numCap; // number of CAPTURE insts in re
                  // 2 => implicit ( and ) for whole match $0

  // Constructs an empty program.
  Prog(Inst[] inst, int start, int numCap) {
    this.inst = inst;
    this.start = start;
    this.numCap = numCap;
  }

  // Returns the number of instructions in this program.
  int numInst() {
    return inst.length;
  }

  
  // skipNop() follows any no-op or capturing instructions and returns the
  // resulting instruction.
  Inst skipNop(int pc) {
    Inst i = inst[pc];
    while (i.op == Inst.NOP || i.op == Inst.CAPTURE) {
      i = inst[pc];
      pc = i.out;
    }
    return i;
  }

  // prefix() returns a pair of a literal string that all matches for the
  // regexp must start with, and a boolean which is true if the prefix is the
  // entire match.  The string is returned by appending to |prefix|.
  boolean prefix(StringBuilder prefix) {
    Inst i = skipNop(start);

    // Avoid allocation of buffer if prefix is empty.
    if (i.op() != Inst.RUNE || i.runes.length != 1) {
      return i.op == Inst.MATCH;  // (append "" to prefix)
    }

    // Have prefix; gather characters.
    while (i.op() == Inst.RUNE &&
           i.runes.length == 1 &&
           (i.arg & RE2.FOLD_CASE) == 0) {
      prefix.appendCodePoint(i.runes[0]);  // an int, not a byte.
      i = skipNop(i.out);
    }
    return i.op == Inst.MATCH;
  }

  // startCond() returns the leading empty-width conditions that must be true
  // in any match.  It returns -1 (all bits set) if no matches are possible.
  int startCond()  {
    int flag = 0;  // bitmask of EMPTY_* flags
    int pc = start;
 loop:
    for (;;) {
      Inst i = inst[pc];
      switch (i.op) {
        case Inst.EMPTY_WIDTH:
          flag |= i.arg;
          break;
        case Inst.FAIL:
          return -1;
        case Inst.CAPTURE:
        case Inst.NOP:
          break;  // skip
        default:
          break loop;
      }
      pc = i.out;
    }
    return flag;
  }


  @Override
  public String toString() {
    StringBuilder out = new StringBuilder();
    for (int pc = 0; pc < inst.length; ++pc) {
      int len = out.length();
      out.append(pc);
      if (pc == start) {
        out.append('*');
      }
      // Use spaces not tabs since they're not always preserved in
      // Google Java source, such as our tests.
      out.append("        ".substring(out.length() - len)).
          append(inst[pc]).append('\n');
    }
    return out.toString();
  }
}
