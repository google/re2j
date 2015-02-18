// Copyright 2010 The Go Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

// Original Go source here:
// http://code.google.com/p/go/source/browse/src/pkg/regexp/exec.go

package com.google.re2j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// A Machine matches an input string of Unicode characters against an
// RE2 instance using a simple NFA.
//
// Called by RE2.doExecute.
class Machine {

  // A logical thread in the NFA.
  private static class Thread {
    Thread(int n) {
      this.cap = new int[n];
    }
    int[] cap;
    Inst inst;
  }

  // A queue is a 'sparse array' holding pending threads of execution.  See:
  // research.swtch.com/2008/03/using-uninitialized-memory-for-fun-and.html
  private static class Queue {

    static class Entry {
      int pc;
      Thread thread;
    }

    final Entry[] dense; // may contain stale Entries in slots >= size
    final int[] sparse;  // may contain stale but in-bounds values.
    int size;  // of prefix of |dense| that is logically populated

    Queue(int n) {
      this.sparse = new int[n];
      this.dense = new Entry[n];
    }

    boolean contains(int pc) {
      int j = sparse[pc];  // (non-negative)
      if (j >= size) {
        return false;
      }
      Entry d = dense[j];
      return d != null && d.pc == pc;
    }

    boolean isEmpty() { return size == 0; }

    Entry add(int pc) {
      int j = size++;
      sparse[pc] = j;
      Entry e = dense[j];
      if (e == null) {  // recycle previous Entry if any
        e = dense[j] = new Entry();
      }
      e.thread = null;
      e.pc = pc;
      return e;
    }

    // Frees all threads on the thread queue, returning them to the free pool.
    void clear(List<Thread> freePool) {
      for(int i = 0; i < size; ++i) {
        Entry entry = dense[i];
        if (entry != null && entry.thread != null) {
          // free(entry.thread)
          freePool.add(entry.thread);
        }
        // (don't release dense[i] to GC; recycle it.)
      }
      size = 0;
    }

    @Override public String toString() {
      StringBuilder out = new StringBuilder();
      out.append('{');
      for (int i = 0; i < size; ++i) {
        if (i != 0) {
          out.append(", ");
        }
        out.append(dense[i].pc);
      }
      out.append('}');
      return out.toString();
    }
  }

  // Corresponding compiled regexp.
  private RE2 re2;

  // Compiled program.
  private final Prog prog;

  // Two queues for runq, nextq.
  private final Queue q0, q1;

  // pool of available threads
  // Really a stack:
  private List<Thread> pool = new ArrayList<Thread>();

  // Whether a match was found.
  private boolean matched;

  // Capture information for the match.
  private int[] matchcap;

  /**
   * Constructs a matching Machine for the specified {@code RE2}.
   */
  Machine(RE2 re2) {
    this.prog = re2.prog;
    this.re2 = re2;
    this.q0 = new Queue(prog.numInst());
    this.q1 = new Queue(prog.numInst());
    this.matchcap = new int[prog.numCap < 2 ? 2 : prog.numCap];
  }

  // init() reinitializes an existing Machine for re-use on a new input.
  void init(int ncap) {
    for (Thread t : pool) {
      t.cap = new int[ncap];
    }
    this.matchcap = new int[ncap];
  }

  int[] submatches() {
    if (matchcap.length == 0) {
      return Utils.EMPTY_INTS;
    }
    int[] cap = new int[matchcap.length];
    System.arraycopy(matchcap, 0, cap, 0, matchcap.length);
    return cap;
  }

  // alloc() allocates a new thread with the given instruction.
  // It uses the free pool if possible.
  private Thread alloc(Inst inst) {
    int n = pool.size();
    Thread t = n > 0
        ? pool.remove(n - 1)
        : new Thread(matchcap.length);
    t.inst = inst;
    return t;
  }

  // free() returns t to the free pool.
  private void free(Thread t) {
    pool.add(t);
  }

  // match() runs the machine over the input |in| starting at |pos| with the
  // RE2 Anchor |anchor|.
  // It reports whether a match was found.
  // If so, matchcap holds the submatch information.
  boolean match(MachineInput in, int pos, int anchor) {
    int startCond = re2.cond;
    if (startCond == Utils.EMPTY_ALL) {  // impossible
      return false;
    }
    if ((anchor == RE2.ANCHOR_START || anchor == RE2.ANCHOR_BOTH) &&
        pos != 0){
      return false;
    }
    matched = false;
    Arrays.fill(matchcap, -1);
    Queue runq = q0, nextq = q1;
    int r = in.step(pos);
    int rune = r >> 3;
    int width = r & 7;
    int rune1 = -1;
    int width1 = 0;
    if (r != MachineInput.EOF) {
      r = in.step(pos + width);
      rune1 = r >> 3;
      width1 = r & 7;
    }
    int flag;  // bitmask of EMPTY_* flags
    if (pos == 0) {
      flag = Utils.emptyOpContext(-1, rune);
    } else {
      flag = in.context(pos);
    }
    for (;;) {

      if (runq.isEmpty()) {
        if ((startCond & Utils.EMPTY_BEGIN_TEXT) != 0 && pos != 0) {
          // Anchored match, past beginning of text.
          break;
        }
        if (matched) {
          // Have match; finished exploring alternatives.
          break;
        }
        if (!re2.prefix.isEmpty() &&
            rune1 != re2.prefixRune &&
            in.canCheckPrefix()) {
          // Match requires literal prefix; fast search for it.
          int advance = in.index(re2, pos);
          if (advance < 0) {
            break;
          }
          pos += advance;
          r = in.step(pos);
          rune = r >> 3;
          width = r & 7;
          r = in.step(pos + width);
          rune1 = r >> 3;
          width1 = r & 7;
        }
      }
      if (!matched && (pos == 0 || anchor == RE2.UNANCHORED)) {
        // If we are anchoring at begin then only add threads that begin
        // at |pos| = 0.
        if (matchcap.length > 0) {
          matchcap[0] = pos;
        }
        add(runq, prog.start, pos, matchcap, flag, null);
      }
      flag = Utils.emptyOpContext(rune, rune1);
      step(runq, nextq, pos, pos + width, rune, flag, anchor, pos == in.endPos());
      if (width == 0) {  // EOF
        break;
      }
      if (matchcap.length == 0 && matched) {
        // Found a match and not paying attention
        // to where it is, so any match will do.
        break;
      }
      pos += width;
      rune = rune1;
      width = width1;
      if (rune != -1) {
        r = in.step(pos + width);
        rune1 = r >> 3;
        width1 = r & 7;
      }
      Queue tmpq = runq;
      runq = nextq;
      nextq = tmpq;
    }
    nextq.clear(pool);
    return matched;
  }

  // step() executes one step of the machine, running each of the threads
  // on |runq| and appending new threads to |nextq|.
  // The step processes the rune |c| (which may be -1 for EOF),
  // which starts at position |pos| and ends at |nextPos|.
  // |nextCond| gives the setting for the EMPTY_* flags after |c|.
  // |anchor| is the anchoring flag and |atEnd| signals if we are at the end of
  // the input string.
  private void step(Queue runq, Queue nextq, int pos, int nextPos, int c,
            int nextCond, int anchor, boolean atEnd) {
    boolean longest = re2.longest;
    for (int j = 0; j < runq.size; ++j) {
      Queue.Entry entry = runq.dense[j];
      if (entry == null) {
        continue;
      }
      Thread t = entry.thread;
      if (t == null) {
        continue;
      }
      if (longest && matched && t.cap.length > 0 && matchcap[0] < t.cap[0]) {
        // free(t)
        pool.add(t);
        continue;
      }
      Inst i = t.inst;
      boolean add = false;
      switch (i.op) {
        case MATCH:
          if (anchor == RE2.ANCHOR_BOTH && !atEnd) {
            // Don't match if we anchor at both start and end and those
            // expectations aren't met.
            break;
          }
          if (t.cap.length > 0 && (!longest || !matched || matchcap[1] < pos)) {
            t.cap[1] = pos;
            System.arraycopy(t.cap, 0, matchcap, 0, t.cap.length);
          }
          if (!longest) {
            // First-match mode: cut off all lower-priority threads.
            for (int k = j + 1; k < runq.size; ++k) {
              Queue.Entry d = runq.dense[k];
              if (d.thread != null) {
                // free(d.thread)
                pool.add(d.thread);
              }
            }
            runq.size = 0;
          }
          matched = true;
          break;

        case RUNE:
          add = i.matchRune(c);
          break;

        case RUNE1:
          add = c == i.runes[0];
          break;

        case RUNE_ANY:
          add = true;
          break;

        case RUNE_ANY_NOT_NL:
          add = c != '\n';
          break;

        default:
          throw new IllegalStateException("bad inst");
      }
      if (add) {
        t = add(nextq, i.out, nextPos, t.cap, nextCond, t);
      }
      if (t != null) {
        // free(t)
        pool.add(t);
      }
    }
    runq.size = 0;
  }

  // add() adds an entry to |q| for |pc|, unless the |q| already has such an
  // entry.  It also recursively adds an entry for all instructions reachable
  // from |pc| by following empty-width conditions satisfied by |cond|.  |pos|
  // gives the current position in the input.  |cond| is a bitmask of EMPTY_*
  // flags.
  private Thread add(Queue q, int pc, int pos, int[] cap, int cond, Thread t) {
    if (pc == 0) {
      return t;
    }
    if (q.contains(pc)) {
      return t;
    }
    Queue.Entry d = q.add(pc);
    Inst inst = prog.getInst(pc);
    switch (inst.op()) {
      default:
        throw new IllegalStateException("unhandled");

      case FAIL:
        break;  // nothing

      case ALT:
      case ALT_MATCH:
        t = add(q, inst.out, pos, cap, cond, t);
        t = add(q, inst.arg, pos, cap, cond, t);
        break;

      case EMPTY_WIDTH:
        if ((inst.arg & ~cond) == 0) {
          t = add(q, inst.out, pos, cap, cond, t);
        }
        break;

      case NOP:
        t = add(q, inst.out, pos, cap, cond, t);
        break;

      case CAPTURE:
        if (inst.arg < cap.length) {
          int opos = cap[inst.arg];
          cap[inst.arg] = pos;
          add(q, inst.out, pos, cap, cond, null);
          cap[inst.arg] = opos;
        } else {
          t = add(q, inst.out, pos, cap, cond, t);
        }
        break;

      case MATCH:
      case RUNE:
      case RUNE1:
      case RUNE_ANY:
      case RUNE_ANY_NOT_NL:
        if (t == null) {
          t = alloc(inst);
        } else {
          t.inst = inst;
        }
        if (cap.length > 0 && t.cap != cap) {
          System.arraycopy(cap, 0, t.cap, 0, cap.length);
        }
        d.thread = t;
        t = null;
        break;
    }
    return t;
  }

}
