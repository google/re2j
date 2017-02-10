// Copyright 2010 The Go Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

// Original Go source here:
// http://code.google.com/p/go/source/browse/src/pkg/regexp/exec.go

package com.google.re2j;

import com.google.re2j.RE2.Anchor;

/**
 * A Machine matches an input string of Unicode characters against an RE2 instance.
 */
interface Machine {

  /**
   * Runs the machine over the input |in| starting at |pos| with the RE2 Anchor |anchor|.
   * |submatches| contains group positions after a successful match.
   *
   * @return reports whether a match was found.
   */
  boolean match(MachineInput in, int pos, Anchor anchor, int[] submatches);
}
