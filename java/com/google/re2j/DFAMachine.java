package com.google.re2j;

import com.google.re2j.RE2.Anchor;
import com.google.re2j.RE2.MatchKind;

import java.util.concurrent.ConcurrentHashMap;

import static com.google.re2j.DFA.NO_MATCH;
import static com.google.re2j.RE2.Anchor.ANCHOR_START;
import static com.google.re2j.RE2.MatchKind.FIRST_MATCH;
import static com.google.re2j.RE2.MatchKind.LONGEST_MATCH;

/**
 * A {@link Machine} implementation using a DFA.
 */
class DFAMachine implements Machine {

  private static final int MAX_DFA_KEY = 4;

  @SuppressWarnings("unchecked")
  private final ConcurrentHashMap<DFAStateKey, DFAState>[] stateCache = new ConcurrentHashMap[MAX_DFA_KEY];
  @SuppressWarnings("unchecked")
  private final ThreadLocal<DFA>[] dfaCache = new ThreadLocal[MAX_DFA_KEY];
  private final RE2 re2;

  DFAMachine(RE2 re2) {
    this.re2 = re2;

    for (int i = 0; i < MAX_DFA_KEY; ++i) {
      stateCache[i] = new ConcurrentHashMap<>();
    }

    setDfaThreadLocal(LONGEST_MATCH, true);
    setDfaThreadLocal(LONGEST_MATCH, false);
    setDfaThreadLocal(FIRST_MATCH, true);
    setDfaThreadLocal(FIRST_MATCH, false);
  }

  @Override
  public boolean match(MachineInput in, int pos, Anchor anchor, int[] submatches) {
    // Don't ask for the location if we won't use it. SearchDFA can do extra optimizations in that case.
    boolean wantMatchPosition = true;
    if (submatches.length == 0) {
      wantMatchPosition = false;
    }

    // Use DFA to find exact location of match, filter out non-matches.
    int matchStart;
    int matchEnd;
    switch (anchor) {
      case UNANCHORED:
        matchEnd = searchDFA(in, pos, in.endPos(), anchor, wantMatchPosition, re2.matchKind, false);
        if (matchEnd == NO_MATCH) {
          return false;
        }

        // Matched. Don't care where
        if (!wantMatchPosition) {
          return true;
        }

        // SearchDFA gives match end position but we don't know where the match started. Run the
        // regexp backwards from end position to find the longest possible match -- that's where it started.
        matchStart = searchDFA(in, pos, matchEnd, ANCHOR_START, true, LONGEST_MATCH, true);
        if (matchStart == NO_MATCH) {
          throw new IllegalStateException("reverse DFA did not found a match");
        }

        break;
      case ANCHOR_BOTH:
      case ANCHOR_START:
        matchEnd = searchDFA(in, pos, in.endPos(), anchor, wantMatchPosition, re2.matchKind, false);
        if (matchEnd == NO_MATCH) {
          return false;
        }
        matchStart = 0;
        break;
      default:
        throw new IllegalStateException("bad anchor");
    }

    if (submatches.length == 2) {
      submatches[0] = matchStart;
      submatches[1] = matchEnd;
    } else {
      if (!re2.nfaMachine.get().match(in, matchStart, anchor, submatches)) {
        throw new IllegalStateException("NFA inconsistency");
      }
    }

    return true;
  }

  private int searchDFA(MachineInput in, int startPos, int endPos, Anchor anchor, boolean wantMatchPosition, MatchKind matchKind, boolean reversed) {
    boolean hasCarat = reversed ? anchor.isAnchorEnd() : anchor.isAnchorStart();
    if (hasCarat && startPos != 0) {
      return NO_MATCH;
    }

    // Handle end match by running an anchored longest match and then checking if it covers all of text.
    boolean anchored = anchor.isAnchorStart();
    boolean endMatch = false;
    if (anchor.isAnchorEnd()) {
      endMatch = true;
      matchKind = LONGEST_MATCH;
    }

    // If the caller doesn't care where the match is (just whether one exists),
    // then we can stop at the very first match we find, the so-called
    // "earliest match".
    boolean wantEarliestMatch = false;
    if (!wantMatchPosition && !endMatch) {
      wantEarliestMatch = true;
      matchKind = LONGEST_MATCH;
    }

    DFA dfa = getDfa(matchKind, reversed);
    int match = dfa.search(in, startPos, endPos, anchored, wantEarliestMatch);

    if (match == NO_MATCH) {
      return NO_MATCH;
    }

    if (endMatch) {
      if ((reversed && match != startPos) || (!reversed && match != endPos)) {
        return NO_MATCH;
      }
    }

    return match;
  }

  private DFA getDfa(MatchKind matchKind, boolean reversed) {
    return dfaCache[dfaKey(matchKind, reversed)].get();
  }

  private int dfaKey(MatchKind matchKind, boolean reversed) {
    int longestInt = matchKind == LONGEST_MATCH ? 1 : 0;
    int reversedInt = reversed ? 1 : 0;
    return longestInt | (reversedInt << 1);
  }

  private void setDfaThreadLocal(MatchKind matchKind, boolean reversed) {
    int dfaKey = dfaKey(matchKind, reversed);
    Prog prog = reversed ? re2.reverseProg : re2.prog;
    dfaCache[dfaKey] = new ThreadLocal<DFA>() {
      @Override
      public DFA initialValue() {
        return new DFA(prog, matchKind, reversed, stateCache[dfaKey]);
      }
    };
  }
}
