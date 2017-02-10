// Copyright 2015 The RE2 Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

// Original RE2 source here:
// https://github.com/google/re2/blob/master/util/sparse_set.h

package com.google.re2j;

class SparseSet {
  private final int[] dense;  // may contain stale Entries in slots >= size
  private final int[] sparse; // may contain stale but in-bounds values.
  private int size;           // of prefix of |dense| that is logically populated

  SparseSet(int n) {
    this.sparse = new int[n];
    this.dense = new int[n];
  }

  boolean contains(int i) {
    return sparse[i] < size && dense[sparse[i]] == i;
  }

  boolean isEmpty() {
    return size == 0;
  }

  void add(int i) {
    dense[size] = i;
    sparse[i] = size;
    size++;
  }

  void clear() {
    size = 0;
  }

  int getValueAt(int i) {
    if (i >= size) {
      throw new IndexOutOfBoundsException(String.format("Cannot get index %d.  SparseSet is size %d", i, size));
    }
    return dense[i];
  }

  int getSize() {
    return size;
  }
}
