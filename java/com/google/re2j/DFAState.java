// Copyright 2015 The RE2 Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

// Original RE2 source here:
// https://github.com/google/re2/blob/master/re2/dfa.cc

package com.google.re2j;

import static com.google.re2j.DFA.FLAG_MATCH;
import static com.google.re2j.DFAState.StateType.DEAD;
import static com.google.re2j.DFAState.StateType.REGULAR;
import static java.lang.System.arraycopy;

final class DFAState {
  public static final DFAState DEAD_STATE = new DFAState(DEAD);

  public enum StateType {
    DEAD,       // no possible match out of this state
    REGULAR     // all other states
  }

  private final StateType type;                       // the state type.  Lets us create DEAD_STATE and FULL_MATCH_STATE
  private final int[] instIndexes;                    // indexes into prog instructions for this state
  private final int flag;                             // empty width flags
  private final DFAState[] next = new DFAState[256];  // Maps bytes to the next state to follow

  public DFAState(int[] instIndexes, int nIndexes, int flag) {
    this.type = REGULAR;
    this.instIndexes = new int[nIndexes];
    arraycopy(instIndexes, 0, this.instIndexes, 0, nIndexes);
    this.flag = flag;
  }

  private DFAState(StateType type) {
    this.type = type;
    this.instIndexes = new int[0];
    this.flag = 0;
  }

  public StateType getType() {
    return type;
  }

  public int getFlag() {
    return flag;
  }

  public int[] getInstIndexes() {
    return instIndexes;
  }

  public boolean isMatch() {
    return (flag & FLAG_MATCH) != 0;
  }

  public boolean isDead() {
    return type == DEAD;
  }

  public DFAState getNextState(byte b) {
    return next[b & 0xff];
  }

  public void setNextState(byte b, DFAState state) {
    next[b & 0xff] = state;
  }
}
