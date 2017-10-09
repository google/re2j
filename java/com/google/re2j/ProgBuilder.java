// Copyright 2010 The Go Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

// Original Go source here:
// http://code.google.com/p/go/source/browse/src/pkg/regexp/syntax/prog.go

package com.google.re2j;

import java.util.ArrayList;
import java.util.List;

/**
 * A ProgBuilder is a builder for a Prog.
 */
final class ProgBuilder {

  
  final List<Inst> inst = new ArrayList<Inst>();
  int start; // index of start instruction
  int numCap = 2; // number of CAPTURE insts in re
                  // 2 => implicit ( and ) for whole match $0

  // Constructs an empty program.
  ProgBuilder() {}

  // Returns the instruction at the specified pc.
  // Precondition: pc > 0 && pc < numInst().
  Inst getInst(int pc) {
    return inst.get(pc);
  }

  // Returns the number of instructions in this program.
  int numInst() {
    return inst.size();
  }

  // Adds a new instruction to this program, with operator |op| and |pc| equal
  // to |numInst()|.
  void addInst(int op) {
    inst.add(new Inst(op));
  }


  int next(int l) {
    Inst i = inst.get(l >> 1);
    if ((l & 1) == 0) {
      return i.out;
    }
    return i.arg;
  }

  void patch(int l, int val) {
    while (l != 0) {
      Inst i = inst.get(l >> 1);
      if ((l & 1) == 0) {
        l = i.out;
        i.out = val;
      } else {
        l = i.arg;
        i.arg = val;
      }
    }
  }

  int append(int l1, int l2) {
    if (l1 == 0) {
      return l2;
    }
    if (l2 == 0) {
      return l1;
    }
    int last = l1;
    for (;;) {
      int next = next(last);
      if (next == 0) {
        break;
      }
      last = next;
    }
    Inst i = inst.get(last>>1);
    if ((last & 1) == 0) {
      i.out = l2;
    } else {
      i.arg = l2;
    }
    return l1;
  }

  // ---

  @Override
  public String toString() {
    StringBuilder out = new StringBuilder();
    for (int pc = 0; pc < inst.size(); ++pc) {
      int len = out.length();
      out.append(pc);
      if (pc == start) {
        out.append('*');
      }
      // Use spaces not tabs since they're not always preserved in
      // Google Java source, such as our tests.
      out.append("        ".substring(out.length() - len)).
          append(inst.get(pc)).append('\n');
    }
    return out.toString();
  }

  public Prog build() {
    return new Prog(inst.toArray(new Inst[0]), start, numCap);
  }
}
