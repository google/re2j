// Copyright 2010 The Go Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

// Original Go source here:
// http://code.google.com/p/go/source/browse/src/pkg/regexp/exec.go

package com.google.re2j;

import com.google.re2j.RE2.Anchor;

import java.util.Arrays;

import static com.google.re2j.MachineInput.EOF;
import static com.google.re2j.RE2.MatchKind.LONGEST_MATCH;
import static com.google.re2j.Utils.emptyOpContext;
import static com.google.re2j.Utils.isRuneStart;
import static java.lang.System.arraycopy;

/**
 * A {@link Machine} implementation using a simple NFA.
 */
class NFAMachine implements Machine {

  private static final int INITIAL_POOL_SIZE = 10;

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
  private class Queue {

    class Entry {
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
    void clear() {
      for(int i = 0; i < size; ++i) {
        Entry entry = dense[i];
        if (entry != null && entry.thread != null) {
          free(entry.thread);
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

  // Program instructions.
  private final Inst[] instructions;

  // Two queues for runq, nextq.
  private final Queue q0, q1;

  // pool of available threads
  private Thread pool[] = new Thread[INITIAL_POOL_SIZE];
  private int poolSize = 0;

  // Whether a match was found.
  private boolean matched;

  // Capture information for the match.
  private int[] matchcap;

  /**
   * Constructs a matching Machine for the specified {@code RE2}.
   */
  NFAMachine(RE2 re2) {
    this.prog = re2.prog;
    this.instructions = prog.getInst();
    this.re2 = re2;
    this.q0 = new Queue(prog.numInst());
    this.q1 = new Queue(prog.numInst());
    this.matchcap = new int[0];
  }

  // alloc() allocates a new thread with the given instruction.
  // It uses the free pool if possible.
  private Thread alloc(Inst inst) {
    Thread t;
    if (poolSize > 0) {
      t = pool[--poolSize];
    } else {
      t = new Thread(matchcap.length);
    }

    t.inst = inst;
    return t;
  }

  // free() returns t to the free pool.
  private void free(Thread t) {
    if (poolSize >= pool.length) {
      Thread[] newPool = new Thread[pool.length * 2];
      arraycopy(pool, 0, newPool, 0, pool.length);
      pool = newPool;
    }

    pool[poolSize++] = t;
  }

  @Override
  public boolean match(MachineInput in, int pos, Anchor anchor, int[] submatches) {
    init(submatches.length);

    int startCond = re2.cond;
    if (startCond == Utils.EMPTY_ALL) {  // impossible
      return false;
    }
    if (anchor.isAnchorStart() && pos != 0) {
      return false;
    }
    matched = false;
    Arrays.fill(matchcap, -1);
    Queue runq = q0, nextq = q1;
    byte b = in.getByte(pos);
    byte b1 = in.getByte(pos + 1);
    int flag; // bitmask of EMPTY_* flags
    if (pos == 0) {
      flag = emptyOpContext(EOF, b);
    } else {
      flag = emptyOpContext(in.getByte(pos - 1), b);
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
        if (re2.prefixUTF8.length() > 0) {
          // Match requires literal prefix; fast search for it.
          int advance = in.index(re2, pos);
          if (advance < 0) {
            break;
          }
          pos += advance;
          b = in.getByte(pos);
          b1 = in.getByte(pos + 1);
        }
      }
      if (!matched && (pos == 0 || anchor.isUnanchored()) && (b == EOF || isRuneStart(b))) {
        // If we are anchoring at begin then only add threads that begin
        // at |pos| = 0.
        if (matchcap.length > 0) {
          matchcap[0] = pos;
        }
        add(runq, prog.start, pos, matchcap, flag, null);
      }
      flag = emptyOpContext(b, b1);
      step(runq, nextq, pos, pos + 1, b, flag, anchor, pos == in.endPos());
      if (b == EOF) {
        break;
      }
      if (matchcap.length == 0 && matched) {
        // Found a match and not paying attention
        // to where it is, so any match will do.
        break;
      }
      pos++;
      b = b1;
      b1 = in.getByte(pos + 1);
      Queue tmpq = runq;
      runq = nextq;
      nextq = tmpq;
    }
    nextq.clear();

    arraycopy(matchcap, 0, submatches, 0, submatches.length);
    return matched;
  }

  private void init(int ncap) {
    if (matchcap.length >= ncap) {
      return;
    }

    for (int i = 0; i < poolSize; ++i) {
      pool[i].cap = new int[ncap];
    }
    matchcap = new int[ncap];
  }

  // step() executes one step of the machine, running each of the threads
  // on |runq| and appending new threads to |nextq|.
  // The step processes the byte |b| (which may be -1 for EOF),
  // which starts at position |pos| and ends at |nextPos|.
  // |nextCond| gives the setting for the EMPTY_* flags after |b|.
  // |anchor| is the anchoring flag and |atEnd| signals if we are at the end of
  // the input string.
  private void step(Queue runq, Queue nextq, int pos, int nextPos, byte b,
                    int nextCond, Anchor anchor, boolean atEnd) {
    boolean longest = re2.matchKind == LONGEST_MATCH;
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
        free(t);
        continue;
      }
      Inst i = t.inst;
      boolean add = false;
      switch (i.op) {
        case MATCH:
          if (anchor.isAnchorBoth() && !atEnd) {
            // Don't match if we anchor at both start and end and those
            // expectations aren't met.
            break;
          }
          if (t.cap.length > 0 && (!longest || !matched || matchcap[1] < pos)) {
            t.cap[1] = pos;
            arraycopy(t.cap, 0, matchcap, 0, t.cap.length);
          }
          if (!longest) {
            // First-match mode: cut off all lower-priority threads.
            for (int k = j + 1; k < runq.size; ++k) {
              Queue.Entry d = runq.dense[k];
              if (d.thread != null) {
                free(d.thread);
              }
            }
            runq.size = 0;
          }
          matched = true;
          break;

        case BYTE:
          add = i.matchByte(b);
          break;

        case BYTE1:
          add = b == i.byteRanges[0];
          break;

        default:
          throw new IllegalStateException("bad inst");
      }
      if (add) {
        t = add(nextq, i.out, nextPos, t.cap, nextCond, t);
      }
      if (t != null) {
        free(t);
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
    Inst inst = instructions[pc];
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
      case BYTE:
      case BYTE1:
        if (t == null) {
          t = alloc(inst);
        } else {
          t.inst = inst;
        }
        if (cap.length > 0 && t.cap != cap) {
          arraycopy(cap, 0, t.cap, 0, cap.length);
        }
        d.thread = t;
        t = null;
        break;
    }
    return t;
  }

}
