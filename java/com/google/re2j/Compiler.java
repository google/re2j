// Copyright 2010 The Go Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

// Original Go source here:
// http://code.google.com/p/go/source/browse/src/pkg/regexp/syntax/compile.go

package com.google.re2j;

import java.util.LinkedList;
import java.util.List;

import static com.google.re2j.Inst.Op.BYTE;
import static com.google.re2j.Inst.Op.BYTE1;
import static com.google.re2j.RE2.FOLD_CASE;
import static com.google.re2j.Unicode.RUNE_SELF;
import static com.google.re2j.Unicode.UTF_MAX;
import static com.google.re2j.Unicode.codePointToUtf8;
import static com.google.re2j.Unicode.maxRune;
import static com.google.re2j.Unicode.simpleFold;

/**
 * Compiler from {@code Regexp} (RE2 abstract syntax) to {@code RE2} (compiled regular expression).
 *
 * The only entry point is {@link #compileRegexp}.
 */
class Compiler {

  /**
   * A fragment of a compiled regular expression program.
   *
   * @see http://swtch.com/~rsc/regexp/regexp1.html
   */
  private static class Frag {
    final int i;        // an instruction address (pc).
    int out;            // a patch list; see explanation in Prog.java

    Frag() {
      this(0, 0);
    }

    Frag(int i) {
      this(i, 0);
    }

    Frag(int i, int out) {
      this.i = i;
      this.out = out;
    }
  }

  private final Prog prog = new Prog();  // Program being built

  private Compiler() {
    newInst(Inst.Op.FAIL);  // always the first instruction
  }

  static Prog compileRegexp(Regexp re) {
    Compiler c = new Compiler();
    Frag f = c.compile(re);
    c.prog.patch(f.out, c.newInst(Inst.Op.MATCH).i);
    c.prog.start = f.i;
    return c.prog;
  }

  private Frag newInst(Inst.Op op) {
    // TODO(rsc): impose length limit.
    prog.addInst(op);
    return new Frag(prog.numInst() - 1);
  }

  // Returns a no-op fragment.  Sometimes unavoidable.
  private Frag nop() {
    Frag f = newInst(Inst.Op.NOP);
    f.out = f.i << 1;
    return f;
  }

  private Frag fail() {
    return new Frag();
  }

  // Given fragment a, returns (a) capturing as \n.
  // Given a fragment a, returns a fragment with capturing parens around a.
  private Frag cap(int arg) {
    Frag f = newInst(Inst.Op.CAPTURE);
    f.out = f.i << 1;
    prog.getInst(f.i).arg = arg;
    if (prog.numCap < arg + 1) {
      prog.numCap = arg + 1;
    }
    return f;
  }

  // Given fragments a and b, returns ab; a|b
  private Frag cat(Frag f1, Frag f2) {
    // concat of failure is failure
    if (f1.i == 0 || f2.i == 0) {
      return fail();
    }
    // TODO(rsc): elide nop
    prog.patch(f1.out, f2.i);
    return new Frag(f1.i, f2.out);
  }

  // Given fragments for a and b, returns fragment for a|b.
  private Frag alt(Frag f1, Frag f2) {
    // alt of failure is other
    if (f1.i == 0) {
      return f2;
    }
    if (f2.i == 0) {
      return f1;
    }
    Frag f = newInst(Inst.Op.ALT);
    Inst i = prog.getInst(f.i);
    i.out = f1.i;
    i.arg = f2.i;
    f.out = prog.append(f1.out, f2.out);
    return f;
  }

  // Given a fragment for a, returns a fragment for a? or a?? (if nongreedy)
  private Frag quest(Frag f1, boolean nongreedy) {
    Frag f = newInst(Inst.Op.ALT);
    Inst i = prog.getInst(f.i);
    if (nongreedy) {
      i.arg = f1.i;
      f.out = f.i << 1;
    } else {
      i.out = f1.i;
      f.out = f.i << 1 | 1;
    }
    f.out = prog.append(f.out, f1.out);
    return f;
  }

  // Given a fragment a, returns a fragment for a* or a*? (if nongreedy)
  private Frag star(Frag f1, boolean nongreedy) {
    Frag f = newInst(Inst.Op.ALT);
    Inst i = prog.getInst(f.i);
    if (nongreedy) {
      i.arg = f1.i;
      f.out = f.i << 1;
    } else {
      i.out = f1.i;
      f.out = f.i<<1 | 1;
    }
    prog.patch(f1.out, f.i);
    return f;
  }

  // Given a fragment for a, returns a fragment for a+ or a+? (if nongreedy)
  private Frag plus(Frag f1, boolean nongreedy) {
    return new Frag(f1.i, star(f1, nongreedy).out);
  }

  // op is a bitmask of EMPTY_* flags.
  private Frag empty(int op) {
    Frag f = newInst(Inst.Op.EMPTY_WIDTH);
    prog.getInst(f.i).arg = op;
    f.out = f.i << 1;
    return f;
  }

  private Frag rune(int rune, int flags) {
    if ((flags & FOLD_CASE) == 0) {
      return rune(new int[]{rune}, flags);
    } else {
      return alt(rune(new int[]{rune}, flags), rune(new int[]{simpleFold(rune)}, flags));
    }
  }

  // flags : parser flags
  private Frag rune(int[] runes, int flags) {
    if (runes.length == 1) {
      return rune(runes[0], runes[0], flags);
    } else if (runes.length > 1) {
      List<Integer> expandedRunes = new LinkedList<>();
      for (int i = 0; i < runes.length; i += 2) {
        expandRuneRange(runes[i], runes[i + 1], expandedRunes);
      }
      Frag alt = rune(expandedRunes.get(0), expandedRunes.get(1), flags);
      for (int i = 2; i < expandedRunes.size(); i += 2) {
        alt = alt(alt, rune(expandedRunes.get(i), expandedRunes.get(i + 1), flags));
      }
      return alt;
    } else {
      return fail();
    }
  }

  private void expandRuneRange(int lo, int hi, List<Integer> runes) {
    // Split range into same-length sized ranges.
    for (int i = 1; i < UTF_MAX; i++) {
      int max = maxRune(i);
      if (lo <= max && max < hi) {
        expandRuneRange(lo, max, runes);
        expandRuneRange(max + 1, hi, runes);
        return;
      }
    }

    // ASCII range is always a special case.
    if (hi < RUNE_SELF) {
      runes.add(lo);
      runes.add(hi);
      return;
    }

    // Split range into sections that agree on leading bytes.
    for (int i = 1; i < UTF_MAX; i++) {
      int m = (1 << (6 * i)) - 1;  // last i bytes of a UTF-8 sequence
      if ((lo & ~m) != (hi & ~m)) {
        if ((lo & m) != 0) {
          expandRuneRange(lo, lo | m, runes);
          expandRuneRange((lo | m) + 1, hi, runes);
          return;
        }
        if ((hi & m) != m) {
          expandRuneRange(lo, (hi & ~m) - 1, runes);
          expandRuneRange(hi & ~m, hi, runes);
          return;
        }
      }
    }

    runes.add(lo);
    runes.add(hi);
  }

  private Frag rune(int lo, int hi, int flags) {
    return bytes(codePointToUtf8(lo), codePointToUtf8(hi), flags);
  }

  private Frag bytes(byte[] lo, byte[] hi, int flags) {
    Frag prefix = byteRange(new byte[]{lo[0], hi[0]}, flags);
    for (int i = 1; i < lo.length; ++i) {
      prefix = cat(prefix, byteRange(new byte[]{lo[i], hi[i]}, flags));
    }
    return prefix;
  }

  private Frag byteRange(byte[] byteRanges, int flags) {
    Frag f = newInst(BYTE);
    Inst i = prog.getInst(f.i);
    i.byteRanges = byteRanges;
    i.arg = flags;
    f.out = f.i << 1;
    // Special cases for exec machine.
    if (byteRanges.length == 1 || (byteRanges.length == 2 && byteRanges[0] == byteRanges[1])) {
      i.op = BYTE1;
      i.byteRanges = new byte[]{byteRanges[0]};
    }
    return f;
  }

  private static final int[] ANY_RUNE_NOT_NL = {
    0, '\n' - 1, '\n' + 1, Unicode.MAX_RUNE
  };
  private static final int[] ANY_RUNE = { 0, Unicode.MAX_RUNE };

  private Frag compile(Regexp re) {
    switch (re.op) {
      case NO_MATCH:
        return fail();
      case EMPTY_MATCH:
        return nop();
      case LITERAL:
        if (re.runes.length == 0) {
          return nop();
        } else {
          Frag f = null;
          for (int r : re.runes) {
            Frag f1 = rune(r, re.flags);
            f = (f == null) ? f1 : cat(f, f1);
          }
          return f;
        }
      case CHAR_CLASS:
        return rune(re.runes, re.flags);
      case ANY_CHAR_NOT_NL:
        return rune(ANY_RUNE_NOT_NL, 0);
      case ANY_CHAR:
        return rune(ANY_RUNE, 0);
      case BEGIN_LINE:
        return empty(Utils.EMPTY_BEGIN_LINE);
      case END_LINE:
        return empty(Utils.EMPTY_END_LINE);
      case BEGIN_TEXT:
        return empty(Utils.EMPTY_BEGIN_TEXT);
      case END_TEXT:
        return empty(Utils.EMPTY_END_TEXT);
      case WORD_BOUNDARY:
        return empty(Utils.EMPTY_WORD_BOUNDARY);
      case NO_WORD_BOUNDARY:
        return empty(Utils.EMPTY_NO_WORD_BOUNDARY);
      case CAPTURE: {
        Frag bra = cap(re.cap << 1),
             sub = compile(re.subs[0]),
             ket = cap(re.cap << 1 | 1);
        return cat(cat(bra, sub), ket);
      }
      case STAR:
        return star(compile(re.subs[0]), (re.flags & RE2.NON_GREEDY) != 0);
      case PLUS:
        return plus(compile(re.subs[0]), (re.flags & RE2.NON_GREEDY) != 0);
      case QUEST:
        return quest(compile(re.subs[0]), (re.flags & RE2.NON_GREEDY) != 0);
      case CONCAT:
        if (re.subs.length == 0) {
          return nop();
        } else {
          Frag f = null;
          for (Regexp sub : re.subs) {
            Frag f1 = compile(sub);
            f = (f == null) ? f1 : cat(f, f1);
          }
          return f;
        }
      case ALTERNATE: {
        if (re.subs.length == 0) {
          return nop();
        } else {
          Frag f = null;
          for (Regexp sub : re.subs) {
            Frag f1 = compile(sub);
            f = (f == null) ? f1 : alt(f, f1);
          }
          return f;
        }
      }
      default:
        throw new IllegalStateException("regexp: unhandled case in compile");
    }
  }
}
