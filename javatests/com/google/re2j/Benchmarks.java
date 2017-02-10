// Copyright 2011 The Go Authors.  All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

// Uncreative and literal-minded port from Go to Java by
// Alan Donovan <adonovan@google.com>, 2011.
//
// Original Go source here:
// http://code.google.com/p/go/source/browse/src/pkg/regexp/syntax/exec_test.go

package com.google.re2j;

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import com.google.caliper.api.VmOptions;
import com.google.caliper.runner.CaliperMain;

import java.util.regex.Pattern;

import io.airlift.slice.Slice;

import static com.google.re2j.Options.DEFAULT_OPTIONS;
import static io.airlift.slice.Slices.EMPTY_SLICE;
import static io.airlift.slice.Slices.utf8Slice;
import static org.junit.Assert.fail;

/**
 * Benchmarks for common RE2J operations. The easiest way to run these benchmarks is probably to do:
 *
 * <pre>
 *   mvn test-compile
 *   mvn exec:java -Dexec.mainClass=com.google.re2j.Benchmarks -Dexec.classpathScope=test
 * </pre>
 */
@VmOptions({"-XX:-TieredCompilation", // http://stackoverflow.com/questions/29199509
        "-Xms8G", "-Xmx8G"}) // These seem to be necessary to avoid GC during the JDK benchmarks.
                             // A GC during an experiment causes Caliper to discard the results.
public class Benchmarks {
  private enum Implementation {
    RE2J, JDK;
  }
  @Param({"RE2J", "JDK"})
  private Implementation implementation;

  private static final String LONG_DATA;
  private static final Slice LONG_DATA_SLICE;

  private static final String BENCHMARK_PATHOLOGICAL_BACKTRACKING = "aaaaaaaaaaaaaaaaaaaaaaaa";
  private static final Slice BENCHMARK_PATHOLOGICAL_BACKTRACKING_SLICE = utf8Slice(BENCHMARK_PATHOLOGICAL_BACKTRACKING);

  private static final String BENCHMARK_LITERAL = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxy";
  private static final Slice BENCHMARK_LITERAL_SLICE = utf8Slice(BENCHMARK_LITERAL);

  private static final String BENCHMARK_NOT_LITERAL = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxy";
  private static final Slice BENCHMARK_NOT_LITERAL_SLICE = utf8Slice(BENCHMARK_NOT_LITERAL);

  private static final String BENCHMARK_MATCH_CLASS = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
      + "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" + "w";
  private static final Slice BENCHMARK_MATCH_CLASS_SLICE = utf8Slice(BENCHMARK_MATCH_CLASS);

  // 'b' is between 'a' and 'c', so the charclass
  // range checking is no help here.
  private static final String BENCHMARK_MATCH_CLASS_IN_RANGE = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
      + "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb" + "c";
  private static final Slice BENCHMARK_MATCH_CLASS_IN_RANGE_SLICE = utf8Slice(BENCHMARK_MATCH_CLASS_IN_RANGE);

  private static final String BENCHMARK_REPLACE_ALL = "abcdefghijklmnopqrstuvwxyz";
  private static final Slice BENCHMARK_REPLACE_ALL_SLICE = utf8Slice(BENCHMARK_REPLACE_ALL);

  private static final String BENCHMARK_ANCHORED_LITERAL_SHORT_NON_MATCH = "abcdefghijklmnopqrstuvwxyz";
  private static final Slice BENCHMARK_ANCHORED_LITERAL_SHORT_NON_MATCH_SLICE = utf8Slice(BENCHMARK_ANCHORED_LITERAL_SHORT_NON_MATCH);

  private static final String BENCHMARK_ANCHORED_SHORT_MATCH = "abcdefghijklmnopqrstuvwxyz";
  private static final Slice BENCHMARK_ANCHORED_SHORT_MATCH_SLICE = utf8Slice(BENCHMARK_ANCHORED_SHORT_MATCH);

  static {
    StringBuilder sb = new StringBuilder(26 << 15);
    for (int i = 0; i < 1 << 15; i++) {
      sb.append("abcdefghijklmnopqrstuvwxyz");
    }
    LONG_DATA = sb.toString();
    LONG_DATA_SLICE = utf8Slice(LONG_DATA);
  }

  private Matcher pathologicalBacktracking;
  private Matcher literal;
  private Matcher notLiteral;
  private Matcher matchClassMatcher;
  private Matcher inRangeMatchClassMatcher;
  private Matcher replaceAllMatcher;
  private Matcher anchoredLiteralNonMatchingMatcher;
  private Matcher anchoredMatchingMatcher;

  private interface Matcher {
    boolean match(String inputString, Slice inputSlice);

    void replaceAll(String inputString, String replacementString, Slice inputSlice, Slice replacementSlice);
  }

  @BeforeExperiment
  public void setupExpressions() {
    pathologicalBacktracking = compile("a?a?a?a?a?a?a?a?a?a?a?a?a?a?a?a?a?a?a?a?a?a?a?a?"
        + "aaaaaaaaaaaaaaaaaaaaaaaa");
    literal = compile("y");
    notLiteral = compile(".y");
    matchClassMatcher = compile("[abcdw]");
    inRangeMatchClassMatcher = compile("[ac]");
    replaceAllMatcher = compile("[cjrw]");
    anchoredLiteralNonMatchingMatcher = compile("^zbc(d|e)");
    anchoredMatchingMatcher = compile("^.bc(d|e)");

    System.gc();
  }

  private Matcher compile(String re) {
    if (implementation == Implementation.JDK) {
      // The JDK implementation appears dramatically faster for these
      // inputs, possibly due to its use of right-to-left matching via
      // Boyer-Moore for anchored patterns. We should totally do that.
      final Pattern r = Pattern.compile(re);
      return new Matcher() {
        public boolean match(String inputString, Slice inputSlice) {
          return r.matcher(inputString).find();
        }

        @Override
        public void replaceAll(String inputString, String replacementString, Slice inputSlice, Slice replacementSlice) {
          r.matcher(inputString).replaceAll(replacementString);
        }
      };
    } else { // com.google.re2
      final RE2 r = RE2.compile(re, DEFAULT_OPTIONS);
      return new Matcher() {
        public boolean match(String inputString, Slice inputSlice) {
          return r.match(inputSlice);
        }

        @Override
        public void replaceAll(String inputString, String replacementString, Slice inputSlice, Slice replacementSlice) {
          r.replaceAll(inputSlice, replacementSlice);
        }
      };
    }
  }

  // See http://swtch.com/~rsc/regexp/regexp1.html.
  @Benchmark
  public void benchmarkPathologicalBacktracking() {
    if (!pathologicalBacktracking.match(BENCHMARK_PATHOLOGICAL_BACKTRACKING, BENCHMARK_PATHOLOGICAL_BACKTRACKING_SLICE)) {
      fail("no match!");
    }
  }

  // The following benchmarks were ported from
  // http://code.google.com/p/go/source/browse/src/pkg/regexp/all_test.go

  @Benchmark
  public void benchmarkLiteral(int nreps) {
    for (int i = 0; i < nreps; i++) {
      if (!literal.match(BENCHMARK_LITERAL, BENCHMARK_LITERAL_SLICE)) {
        fail("no match!");
      }
    }
  }

  @Benchmark
  public void benchmarkNotLiteral(int nreps) {
    for (int i = 0; i < nreps; i++) {
      if (!notLiteral.match(BENCHMARK_NOT_LITERAL, BENCHMARK_NOT_LITERAL_SLICE)) {
        fail("no match!");
      }
    }
  }

  @Benchmark
  public void benchmarkMatchClass(int nreps) {
    for (int i = 0; i < nreps; i++) {
      if (!matchClassMatcher.match(BENCHMARK_MATCH_CLASS, BENCHMARK_MATCH_CLASS_SLICE)) {
        fail("no match!");
      }
    }
  }

  @Benchmark
  public void benchmarkMatchClass_InRange(int nreps) {
    for (int i = 0; i < nreps; i++) {
      if (!inRangeMatchClassMatcher.match(BENCHMARK_MATCH_CLASS_IN_RANGE, BENCHMARK_MATCH_CLASS_IN_RANGE_SLICE)) {
        fail("no match!");
      }
    }
  }

  @Benchmark
  public void benchmarkReplaceAll(int nreps) {
    for (int i = 0; i < nreps; i++) {
      replaceAllMatcher.replaceAll(BENCHMARK_REPLACE_ALL, "", BENCHMARK_REPLACE_ALL_SLICE, EMPTY_SLICE);
    }
  }

  @Benchmark
  public void benchmarkAnchoredLiteralShortNonMatch(int nreps) {
    String x = "abcdefghijklmnopqrstuvwxyz";
    for (int i = 0; i < nreps; i++) {
      anchoredLiteralNonMatchingMatcher.match(BENCHMARK_ANCHORED_LITERAL_SHORT_NON_MATCH, BENCHMARK_ANCHORED_LITERAL_SHORT_NON_MATCH_SLICE);
    }
  }

  @Benchmark
  public void benchmarkAnchoredLiteralLongNonMatch(int nreps) {
    for (int i = 0; i < nreps; i++) {
      anchoredLiteralNonMatchingMatcher.match(LONG_DATA, LONG_DATA_SLICE);
    }
  }

  @Benchmark
  public void benchmarkAnchoredShortMatch(int nreps) {
    for (int i = 0; i < nreps; i++) {
      anchoredMatchingMatcher.match(BENCHMARK_ANCHORED_SHORT_MATCH, BENCHMARK_ANCHORED_SHORT_MATCH_SLICE);
    }
  }

  @Benchmark
  public void benchmarkAnchoredLongMatch(int nreps) {
    for (int i = 0; i < nreps; i++) {
      anchoredMatchingMatcher.match(LONG_DATA, LONG_DATA_SLICE);
    }
  }

  public static void main(String[] args) {
    CaliperMain.main(Benchmarks.class, args);
  }
}