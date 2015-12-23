// Copyright 2015 The RE2 Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

// Original RE2 source here:
// https://github.com/google/re2/blob/master/re2/dfa.cc

package com.google.re2j;

import java.util.Arrays;

import static com.google.re2j.Utils.arrayFirstElementsEqual;

final class DFAStateKey {
  private final int[] instIndexes;
  private final int nIndexes;
  private final int flag;

  DFAStateKey(int[] instIndexes, int nIndexes, int flag) {
    this.instIndexes = instIndexes;
    this.nIndexes = nIndexes;
    this.flag = flag;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DFAStateKey that = (DFAStateKey) o;

    return nIndexes == that.nIndexes && flag == that.flag && arrayFirstElementsEqual(instIndexes, that.instIndexes, nIndexes);
  }

  @Override
  public int hashCode() {
    int result = Arrays.hashCode(instIndexes);
    result = 31 * result + nIndexes;
    result = 31 * result + flag;
    return result;
  }
}