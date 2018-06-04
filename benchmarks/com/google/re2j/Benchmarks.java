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

import static org.junit.Assert.fail;

/**
 * Benchmarks for common RE2J operations. The easiest way to run these benchmarks is probably to do:
 *
 * <pre>
 *   mvn test-compile
 *   mvn exec:java -Dexec.mainClass=com.google.re2j.Benchmarks -Dexec.classpathScope=test
 * </pre>
 */
@VmOptions({
  "-XX:-TieredCompilation", // http://stackoverflow.com/questions/29199509
  "-Xms8G",
  "-Xmx8G"
}) // These seem to be necessary to avoid GC during the JDK benchmarks.
// A GC during an experiment causes Caliper to discard the results.
public class Benchmarks {
  private enum Implementation {
    RE2J,
    JDK
  }

  @Param({"RE2J", "JDK"})
  private Implementation implementation;

  private static final String LONG_DATA;

  static {
    StringBuilder sb = new StringBuilder(26 << 15);
    for (int i = 0; i < 1 << 15; i++) {
      sb.append("abcdefghijklmnopqrstuvwxyz");
    }
    LONG_DATA = sb.toString();
  }

  private Matcher pathologicalBacktracking;
  private Matcher literal;
  private Matcher notLiteral;
  private Matcher matchClassMatcher;
  private Matcher inRangeMatchClassMatcher;
  private Matcher replaceAllMatcher;
  private Matcher anchoredLiteralNonMatchingMatcher;
  private Matcher longAnchoredLiteralMatchingMatcher;
  private Matcher anchoredMatchingMatcher;

  private interface Matcher {
    boolean match(String input);

    String replaceAll(String input, String replacement);
  }

  @BeforeExperiment
  public void setupExpressions() {
    pathologicalBacktracking =
        compile("a?a?a?a?a?a?a?a?a?a?a?a?a?a?a?a?a?a?a?a?a?a?a?a?" + "aaaaaaaaaaaaaaaaaaaaaaaa");
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
      final Pattern p = Pattern.compile(re);
      return new Matcher() {
        @Override
        public boolean match(String input) {
          return p.matcher(input).find();
        }

        @Override
        public String replaceAll(String input, String replacement) {
          return p.matcher(input).replaceAll(replacement);
        }
      };
    } else { // com.google.re2
      final com.google.re2j.Pattern p = com.google.re2j.Pattern.compile(re);
      return new Matcher() {
        @Override
        public boolean match(String input) {
          return p.matcher(input).find();
        }

        @Override
        public String replaceAll(String input, String replacement) {
          return p.matcher(input).replaceAll(replacement);
        }
      };
    }
  }

  // See http://swtch.com/~rsc/regexp/regexp1.html.
  @Benchmark
  public void benchmarkPathologicalBacktracking() {
    if (!pathologicalBacktracking.match("aaaaaaaaaaaaaaaaaaaaaaaa")) {
      fail("no match!");
    }
  }

  // The following benchmarks were ported from
  // http://code.google.com/p/go/source/browse/src/pkg/regexp/all_test.go

  @Benchmark
  public void benchmarkLiteral(int nreps) {
    String x = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxy";
    for (int i = 0; i < nreps; i++) {
      if (!literal.match(x)) {
        fail("no match!");
      }
    }
  }

  @Benchmark
  public void benchmarkNotLiteral(int nreps) {
    String x = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxy";
    for (int i = 0; i < nreps; i++) {
      if (!notLiteral.match(x)) {
        fail("no match!");
      }
    }
  }

  @Benchmark
  public void benchmarkMatchClass(int nreps) {
    String x =
        "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
            + "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
            + "w";
    for (int i = 0; i < nreps; i++) {
      if (!matchClassMatcher.match(x)) {
        fail("no match!");
      }
    }
  }

  @Benchmark
  public void benchmarkMatchClass_InRange(int nreps) {
    // 'b' is between 'a' and 'c', so the charclass
    // range checking is no help here.
    String x =
        "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
            + "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
            + "c";
    for (int i = 0; i < nreps; i++) {
      if (!inRangeMatchClassMatcher.match(x)) {
        fail("no match!");
      }
    }
  }

  @Benchmark
  public void benchmarkReplaceAll(int nreps) {
    String x = "abcdefghijklmnopqrstuvwxyz";
    for (int i = 0; i < nreps; i++) {
      replaceAllMatcher.replaceAll(x, "");
    }
  }

  @Benchmark
  public void benchmarkAnchoredLiteralShortNonMatch(int nreps) {
    String x = "abcdefghijklmnopqrstuvwxyz";
    for (int i = 0; i < nreps; i++) {
      anchoredLiteralNonMatchingMatcher.match(x);
    }
  }

  @Benchmark
  public void benchmarkAnchoredLiteralLongNonMatch(int nreps) {
    for (int i = 0; i < nreps; i++) {
      anchoredLiteralNonMatchingMatcher.match(LONG_DATA);
    }
  }

  @Benchmark
  public void benchmarkAnchoredShortMatch(int nreps) {
    String x = "abcdefghijklmnopqrstuvwxyz";
    for (int i = 0; i < nreps; i++) {
      anchoredMatchingMatcher.match(x);
    }
  }

  @Benchmark
  public void benchmarkAnchoredLongMatch(int nreps) {
    for (int i = 0; i < nreps; i++) {
      anchoredMatchingMatcher.match(LONG_DATA);
    }
  }

  public static void main(String[] args) {
    CaliperMain.main(Benchmarks.class, args);
  }
}
