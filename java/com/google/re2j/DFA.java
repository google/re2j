//This is a modified version of RE2.
// Original RE2 code can be found here:
// https://github.com/google/re2/blob/master/re2/dfa.cc


package com.google.re2j;

import com.google.re2j.RE2.MatchKind;

import java.util.concurrent.ConcurrentHashMap;

import static com.google.re2j.DFAState.DEAD_STATE;
import static com.google.re2j.Inst.Op.EMPTY_WIDTH;
import static com.google.re2j.MachineInput.EOF;
import static com.google.re2j.RE2.MatchKind.FIRST_MATCH;
import static com.google.re2j.RE2.MatchKind.LONGEST_MATCH;
import static com.google.re2j.Utils.EMPTY_BEGIN_LINE;
import static com.google.re2j.Utils.EMPTY_BEGIN_TEXT;
import static com.google.re2j.Utils.EMPTY_END_LINE;
import static com.google.re2j.Utils.EMPTY_END_TEXT;
import static com.google.re2j.Utils.EMPTY_NO_WORD_BOUNDARY;
import static com.google.re2j.Utils.EMPTY_WORD_BOUNDARY;
import static com.google.re2j.Utils.isRuneStart;
import static com.google.re2j.Utils.isWordByte;
import static java.util.Arrays.sort;

class DFA {

  static final int NO_MATCH = -1;

  static final int FLAG_MATCH = 0x100;      //This is a matching state
  static final int FLAG_LAST_WORD = 0x200;  //The last byte was word character
  static final int FLAG_NEED_SHIFT = 16;
  static final int FLAG_EMPTY_MASK = 0xFF;  // Mask for the empty flag bits

  private static final int MARK = -1;       // Separates priorities of items in a WorkQueue

  // Do we have the first byte that will get us out of the start state
  private static final byte FIRST_BYTE_MANY = -1;    // there are several such possibilities
  private static final byte FIRST_BYTE_NONE = -2;    // There are no such bytes

  // Total number of start parameters (total number of empty flag combinations plus anchored flag)
  private static final int START_PARAMS_CACHE_SIZE = 1 << 13;
  private static final int START_PARAMS_CACHE_SHIFT = 12;

  // Info for the search
  private final Prog prog;

  // Program instructions.
  private final Inst[] instructions;

  // Search for longest match like egrep or POSIX or for first match like Perl, PCRE
  private final MatchKind matchKind;

  // Should input bytes be read forward or backward
  private final boolean runForward;

  // some preallocated workspace.
  private WorkQueue currentWorkQ;
  private WorkQueue nextWorkQ;
  private final int[] instStack;

  private final StartParams[] startParamsCache = new StartParams[START_PARAMS_CACHE_SIZE];
  private final ConcurrentHashMap<DFAStateKey, DFAState> statesCache;

  public DFA(Prog prog, MatchKind matchKind, boolean reversed, ConcurrentHashMap<DFAStateKey, DFAState> statesCache) {
    this.prog = prog;
    this.instructions = prog.getInst();
    this.matchKind = matchKind;
    this.runForward = !reversed;
    this.statesCache = statesCache;

    int progSize = prog.numInst();
    int nMarks = 0;
    if (matchKind == LONGEST_MATCH) {
      nMarks = progSize;
    }

    currentWorkQ = new WorkQueue(progSize, nMarks);
    nextWorkQ = new WorkQueue(progSize, nMarks);
    int stackSize = 2 * progSize + nMarks;
    instStack = new int[stackSize];
  }

  // Public entry point to run the search. Creates the SearchParams, and then runs the search loop.
  public int search(MachineInput in, int startPos, int endPos, boolean anchored, boolean wantEarliestMatch) {
    StartParams startParams = analyzeSearch(in, startPos, endPos, anchored);
    if (startParams.startState.isDead()) {
      return NO_MATCH;
    }

    return searchLoop(in, startPos, endPos, wantEarliestMatch, startParams);
  }

  // Converts the WorkQueue q to a state.
  // TODO: sawmatch optimization from dfa.cc
  private DFAState workQueueToCachedState(WorkQueue q, int flag) {
    int[] instIndexes = new int[q.getMaxSize()];
    int nIndexes = 0; //number of indexes in instIndexes so far
    int neededFlags = 0;
    int size = q.getSize();
    for (int i = 0; i < size; i++) {
      int instIndex = q.getValueAt(i);

      if (q.isMark(instIndex)) {
        if (nIndexes > 0 && instIndexes[nIndexes - 1] != MARK) {
          instIndexes[nIndexes++] = MARK;
        }
        continue;
      }

      Inst inst = instructions[instIndex];
      switch (inst.op()) {
        case ALT_MATCH:
        case BYTE:
        case EMPTY_WIDTH:
        case MATCH:
        case ALT:
          instIndexes[nIndexes++] = instIndex;
          if (inst.op() == EMPTY_WIDTH) {
            neededFlags |= inst.arg;
          }
          break;
        default:
          break;
      }
    }

    // if the last inst is a mark remove it
    if (nIndexes > 0 && instIndexes[nIndexes - 1] == MARK) {
      nIndexes--;
    }

    // If there are no empty-width instructions waiting to execute,
    // the extra flag bits will not be used. Discard them to reduce
    // number of distinct states.
    if (neededFlags == 0) {
      flag &= FLAG_MATCH;
    }

    // No match possibilities
    if (nIndexes == 0 && flag == 0) {
      return DEAD_STATE;
    }

    // If we're in the longest match mode, the state is a sequence of
    // unordered state sets separated by Marks. Sort each set to
    // canonicalize, to reduce the number of distinct sets stored.
    if (matchKind == LONGEST_MATCH) {
      int ip = 0;
      while (ip < nIndexes) {
        int markp = ip;
        while (markp < nIndexes && instIndexes[markp] != MARK) {
          markp++;
        }
        sort(instIndexes, ip, markp);
        if (markp < nIndexes) {
          markp++;
        }
        ip = markp;
      }
    }

    flag |= neededFlags << FLAG_NEED_SHIFT;
    return getCachedState(instIndexes, nIndexes, flag);
  }

  private DFAState getCachedState(int[] instIndexes, int nIndexes, int flag) {
    DFAStateKey key = new DFAStateKey(instIndexes, nIndexes, flag);
    DFAState state = statesCache.get(key);

    if (state == null) {
      // create new state with trimmed instruction array
      state = new DFAState(instIndexes, nIndexes, flag);
      key = new DFAStateKey(state.getInstIndexes(), nIndexes, flag);

      DFAState previousState = statesCache.putIfAbsent(key, state);

      // it is possible that somebody simultaneously inserted state for the same key
      if (previousState != null) {
        return previousState;
      }
    }

    return state;
  }

  // Use queue to create a WorkQueue from the state
  private void stateToWorkQueue(DFAState state, WorkQueue queue) {
    queue.clear();
    int[] instIndexes = state.getInstIndexes();
    for (int index : instIndexes) {
      if (index == MARK) {
        queue.mark();
      } else {
        queue.insertNew(index);
      }
    }
  }


  // Add id and instructions that follow from it (if it's e.g. an ALT instruction)
  // to the WorkQueue q. flag contains the empty width instruction flags
  private void addToQueue(WorkQueue q, int id, int flag) {

    // Use instSack to hold the stack of instructions still to process.
    // It is sized to have room for 2* (prog.numInsts) + nmark instructions.
    // Each instruction can be processed by the switch below only once, and the processing
    // pushes at most two instructions plus maybe a mark.
    // (If we are using marks, nmark = prog.numInsts().  Otherwise nmark = 0;
    int stackSize = 0;
    instStack[stackSize++] = id;
    while (stackSize > 0) {
      id = instStack[--stackSize];

      if (id == MARK) {
        q.mark();
        continue;
      }

      // If id is already on the queue, there's nothing to do.  Otherwise add it.
      // We don't actually keep all the ones that get added -- for example, kInstAlt is ignored
      // when on a work queue -- but adding all of the instructions here increases the likelihood
      // of q.contains(id), reducing the amound of duplicated work
      if (q.contains(id)) {
        continue;
      }

      q.insertNew(id);

      Inst inst = instructions[id];
      switch (inst.op()) {
        case FAIL: //nothing to do for these
        case BYTE:
        case MATCH:
          break;

        case CAPTURE:
        case NOP:
          instStack[stackSize++] = inst.out;
          break;

        case ALT:
        case ALT_MATCH:
          instStack[stackSize++] = inst.arg;

          if (currentWorkQ.maxMark > 0 && id == prog.startUnanchored && id != prog.start) {
            instStack[stackSize++] = MARK;
          }

          instStack[stackSize++] = inst.out;
          break;
        case EMPTY_WIDTH:
          //continue if all of the empty width flags match up.
          if ((inst.arg & ~flag) != 0) {
            break;
          }
          instStack[stackSize++] = inst.out;
      }
    }
  }

  // Runs currentWorkQ on the empty string flags, and populates nextWorkQ with new insts
  private void runWorkQueueOnEmptyString(int flag) {
    nextWorkQ.clear();
    for (int i = 0; i < currentWorkQ.getSize(); i++) {
      int instIndex = currentWorkQ.getValueAt(i);
      if (currentWorkQ.isMark(instIndex)) {
        addToQueue(nextWorkQ, MARK, flag);
      } else {
        addToQueue(nextWorkQ, instIndex, flag);
      }
    }
  }

  // Runs the byte against the work in currentWorkQ.
  // Populates nextWorkQ with new instructions.
  // Returns whether a match was found.
  private boolean runWorkQueueOnByte(byte b, int flag) {
    nextWorkQ.clear();
    boolean isMatch = false;
    for (int i = 0; i < currentWorkQ.getSize(); i++) {
      int instIndex = currentWorkQ.getValueAt(i);
      if (currentWorkQ.isMark(instIndex)) {
        if (isMatch) {
          return true;
        }
        nextWorkQ.mark();
        continue;
      }

      Inst inst = prog.getInst(instIndex);
      switch (inst.op()) {
        case FAIL:    // never succeeds
        case CAPTURE: // already followed all following
        case NOP:
        case ALT:
        case ALT_MATCH:
        case EMPTY_WIDTH:
          break;
        case BYTE:
          if (inst.matchByte(b)) {
            addToQueue(nextWorkQ, inst.out, flag);
          }
          break;
        case MATCH:
          isMatch = true;
          if (matchKind == FIRST_MATCH) {
            return true;
          }
          break;
      }
    }
    return isMatch;
  }

  // Run the state on the given byte.  If next state has already been found, get it directly.
  // Otherwise create a WorkQueue from the state and run it against the byte to create the next state.
  // Return the next state.
  private DFAState runStateOnByte(DFAState state, byte b) {
    if (state.isDead()) {
      throw new IllegalArgumentException("cannot run byte on DEAD STATE");
    }

    DFAState nextState = state.getNextState(b);
    if (nextState != null) {
      return nextState;
    }

    stateToWorkQueue(state, currentWorkQ);

    // Add implicit empty width flags
    int needFlag = state.getFlag() >> FLAG_NEED_SHIFT;
    int beforeFlag = state.getFlag() & FLAG_EMPTY_MASK;
    int oldBeforeFlag = beforeFlag;
    int afterFlag = 0;

    if (b == '\n') {
      beforeFlag |= EMPTY_END_LINE;
      afterFlag |= EMPTY_BEGIN_LINE;
    }

    if (b == EOF) {
      beforeFlag |= EMPTY_END_LINE | EMPTY_END_TEXT;
    }

    boolean isLastWord = (state.getFlag() & FLAG_LAST_WORD) != 0;  //last byte processed was a word character
    boolean isWord = b != EOF && isWordByte(b);
    if (isWord == isLastWord) {
      beforeFlag |= EMPTY_NO_WORD_BOUNDARY;
    } else {
      beforeFlag |= EMPTY_WORD_BOUNDARY;
    }

    if ((beforeFlag & ~oldBeforeFlag & needFlag) != 0) {
      runWorkQueueOnEmptyString(beforeFlag);
      switchWorkQueues();
    }

    boolean isMatch = runWorkQueueOnByte(b, afterFlag);

    // We're done with the currentWorkQ.  Switch it with nextWorkQ
    switchWorkQueues();

    int flag = afterFlag;
    if (isMatch) {
      flag |= FLAG_MATCH;
    }
    if (isWord) {
      flag |= FLAG_LAST_WORD;
    }

    nextState = workQueueToCachedState(currentWorkQ, flag);
    state.setNextState(b, nextState);
    return nextState;
  }

  private void switchWorkQueues() {
    WorkQueue tmpQueue = currentWorkQ;
    currentWorkQ = nextWorkQ;
    nextWorkQ = tmpQueue;
  }

  // Analyzes the search to build the SearchParams
  private StartParams analyzeSearch(MachineInput in, int startPos, int endPos, boolean anchored) {
    if (startPos < 0 || startPos > in.endPos()) {
      return new StartParams(DEAD_STATE, FIRST_BYTE_NONE);
    }

    int flags = 0;
    if (runForward) {
      if (startPos == 0) {
        flags = EMPTY_BEGIN_TEXT | EMPTY_BEGIN_LINE;
      } else if (in.getByte(startPos - 1) == '\n') {
        flags = EMPTY_BEGIN_LINE;
      } else if (isWordByte(in.getByte(startPos - 1))) {
        flags = FLAG_LAST_WORD;
      }
    } else {
      if (endPos == in.endPos()) {
        flags = EMPTY_BEGIN_TEXT | EMPTY_BEGIN_LINE;
      } else if (in.getByte(endPos) == '\n') {
        flags = EMPTY_BEGIN_LINE;
      } else if (isWordByte(in.getByte(endPos))) {
        flags = FLAG_LAST_WORD;
      }
    }

    return getCachedStartParams(anchored, flags);
  }

  private StartParams getCachedStartParams(boolean anchored, int flags) {
    int key = startParamsKey(anchored, flags);
    if (startParamsCache[key] != null) {
      return startParamsCache[key];
    }
    StartParams startParams = computeStartParams(anchored, flags);
    startParamsCache[key] = startParams;
    return startParams;
  }

  private int startParamsKey(boolean anchored, int flags) {
    return flags | ((anchored ? 1 : 0) << START_PARAMS_CACHE_SHIFT);
  }

  private StartParams computeStartParams(boolean anchored, int flags) {
    currentWorkQ.clear();
    if (anchored) {
      addToQueue(currentWorkQ, prog.start, flags);
    } else {
      addToQueue(currentWorkQ, prog.startUnanchored, flags);
    }

    DFAState startState = workQueueToCachedState(currentWorkQ, flags);
    if (startState.isDead()) {
      return new StartParams(DEAD_STATE, FIRST_BYTE_NONE);
    }

    // compute the first byte by running over all possible bytes and
    // seeing if there is exactly one that changes the state.
    int firstByte = FIRST_BYTE_NONE;
    for (int i = 0; i < 256; i++) {
      DFAState state = runStateOnByte(startState, (byte) i);
      if (state == startState) {
        continue;
      }

      // This byte brought us to a new state
      if (firstByte == FIRST_BYTE_NONE) {  //first time that happened
        firstByte = i;
      } else {  //too many
        firstByte = FIRST_BYTE_MANY;
        break;
      }
    }

    return new StartParams(startState, firstByte);
  }

  // the main search loop
  private int searchLoop(MachineInput in, int startPos, int endPos, boolean wantEarliestMatch, StartParams startParams) {
    int lastMatchIndex = NO_MATCH;
    DFAState currentState = startParams.startState;

    int currentIndex;
    int endIndex;

    if (runForward) {
      currentIndex = startPos;
      endIndex = in.endPos();
    } else {
      currentIndex = endPos;
      endIndex = startPos;
    }

    while (currentIndex != endIndex) {
      byte b;
      if (runForward) {
        b = in.getByte(currentIndex++);
      } else {
        b = in.getByte(--currentIndex);
      }

      if (currentState == startParams.startState) {
        // in start state we can skip bytes that don't match first byte
        if (startParams.firstByte >= 0 && ((b & 0xff) != startParams.firstByte)) {
          continue;
        }

        // in forward search make sure we don't start in a middle of rune
        // (backward search starts correctly because it is anchored at match end)
        if (runForward && !isRuneStart(b)) {
          continue;
        }
      }

      currentState = getNextState(currentState, b);
      if (currentState.isDead()) {
        return lastMatchIndex;
      }

      if (currentState.isMatch()) {
        // The DFA notices the match one byte late, so adjust p before using it in the match.
        if (runForward) {
          lastMatchIndex = currentIndex - 1;
        } else {
          lastMatchIndex = currentIndex + 1;
        }
        if (wantEarliestMatch) {
          return lastMatchIndex;
        }
      }
    }

    byte lastByte;
    if (runForward) {
      if (endPos == in.endPos()) {
        lastByte = EOF;
      } else {
        lastByte = in.getByte(endPos);
      }
    } else {
      if (startPos == 0) {
        lastByte = EOF;
      } else {
        lastByte = in.getByte(startPos - 1);
      }
    }

    // Process one more byte to see if it triggers a match (Remember, matches are delayed one byte).
    currentState = getNextState(currentState, lastByte);
    if (currentState.isMatch()) {
      lastMatchIndex = currentIndex;
    }

    return lastMatchIndex;
  }

  private DFAState getNextState(DFAState currentState, byte currentByte) {
    DFAState nextState = currentState.getNextState(currentByte);

    // Null means the next state has not been found yet. Compute it.
    if (nextState == null) {
      nextState = runStateOnByte(currentState, currentByte);
    }

    return nextState;
  }

  /**
   * This is a WorkQueue created from the insts in a DFAState. A mark seperates priorities for
   * LEFT_LONGEST_MATCH mode. Matches found before a mark have priority, as they are farther left
   * than those after it.
   */
  private static class WorkQueue extends SparseSet {
    final int normalSlots;
    final int maxMark;

    int nextMark;
    boolean wasLastMark;

    WorkQueue(int normalSlots, int maxMark) {
      super(normalSlots + maxMark);
      this.normalSlots = normalSlots;
      this.maxMark = maxMark;
      this.nextMark = normalSlots;
      this.wasLastMark = false;
    }

    boolean isMark(int i) {
      return i >= normalSlots;
    }

    void clear() {
      super.clear();
      nextMark = normalSlots;
    }

    void mark() {
      if (!wasLastMark) {
        wasLastMark = true;
        add(nextMark++);
      }
    }

    int getMaxSize() {
      return normalSlots + maxMark;
    }

    //inserts a new instruction into the WorkQueue
    void insertNew(int id) {
      wasLastMark = false;
      add(id);
    }
  }

  private final static class StartParams {
    final DFAState startState;
    final int firstByte; // a single byte that gets us out of the start state if one exists

    StartParams(DFAState startState, int firstByte) {
      this.startState = startState;
      this.firstByte = firstByte;
    }
  }
}
