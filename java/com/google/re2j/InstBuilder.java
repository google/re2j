// Copyright 2010 The Go Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

// Original Go source here:
// http://code.google.com/p/go/source/browse/src/pkg/regexp/syntax/prog.go

package com.google.re2j;

/**
 * An instruction builder
 * @see http://swtch.com/~rsc/regexp/regexp2.html
 */
final class InstBuilder {
  

  int op;
  int out;  // all but MATCH, FAIL
  int arg;  // ALT, ALT_MATCH, CAPTURE, EMPTY_WIDTH
  int[] runes;  // length==1 => exact match
                // otherwise a list of [lo,hi] pairs.  hi is *inclusive*.
                // REVIEWERS: why not half-open intervals?

  InstBuilder(int op) {
    this.op = op;
  }



  Inst build() {
    return new Inst(op, out, arg, runes);
  }
}
