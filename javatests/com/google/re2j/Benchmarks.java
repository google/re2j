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

import java.util.regex.Pattern;

import static org.junit.Assert.fail;

/**
 * Benchmarks for common RE2J operations. The easiest way to run these benchmarks is probably to do:
 *
 * <pre>
 *   mvn exec:java -Dexec.mainClass=com.google.caliper.runner.CaliperMain com.google.re2j.Benchmarks
 * </pre>
 */
@VmOptions("-XX:-TieredCompilation") // http://stackoverflow.com/questions/29199509
public class Benchmarks {
  @Param({"false", "true"})
  private boolean useJdk;

  private Matcher pathologicalBacktracking;
  private RE2 literal;
  private RE2 notLiteral;

  private interface Matcher {
    boolean match(String input);
  }

  @BeforeExperiment
  public void setupExpressions() {
    pathologicalBacktracking = compile("a?a?a?a?a?a?a?a?a?a?a?a?a?a?a?a?a?a?a?a?a?a?a?a?"
        + "aaaaaaaaaaaaaaaaaaaaaaaa");
    literal = RE2.compile("y");
    notLiteral = RE2.compile(".y");
  }

  private Matcher compile(String re) {
    if (useJdk) {
      // The JDK implementation appears dramatically faster for these
      // inputs, possibly due to its use of right-to-left matching via
      // Boyer-Moore for anchored patterns. We should totally do that.
      final Pattern r = Pattern.compile(re);
      return new Matcher() {
        public boolean match(String input) {
          return r.matcher(input).matches();
        }
      };
    } else { // com.google.re2
      final RE2 r = RE2.compile(re);
      return new Matcher() {
        public boolean match(String input) {
          return r.match(input);
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
    String x = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
        + "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" + "w";
    RE2 re = RE2.compile("[abcdw]");
    for (int i = 0; i < nreps; i++) {
      if (!re.match(x)) {
        fail("no match!");
      }
    }
  }

  @Benchmark
  public void benchmarkMatchClass_InRange(int nreps) {
    // 'b' is between 'a' and 'c', so the charclass
    // range checking is no help here.
    String x = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
        + "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb" + "c";
    RE2 re = RE2.compile("[ac]");
    for (int i = 0; i < nreps; i++) {
      if (!re.match(x)) {
        fail("no match!");
      }
    }
  }

  @Benchmark
  public void benchmarkReplaceAll(int nreps) {
    String x = "abcdefghijklmnopqrstuvwxyz";
    RE2 re = RE2.compile("[cjrw]");
    for (int i = 0; i < nreps; i++) {
      re.replaceAll(x, "");
    }
  }

  @Benchmark
  public void benchmarkAnchoredLiteralShortNonMatch(int nreps) {
    String x = "abcdefghijklmnopqrstuvwxyz";
    RE2 re = RE2.compile("^zbc(d|e)");
    for (int i = 0; i < nreps; i++) {
      re.match(x);
    }
  }

  private static final String LONG_DATA;

  static {
    StringBuilder sb = new StringBuilder(26 << 15);
    for (int i = 0; i < 1 << 15; i++) {
      sb.append("abcdefghijklmnopqrstuvwxyz");
    }
    LONG_DATA = sb.toString();
  }

  @Benchmark
  public void benchmarkAnchoredLiteralLongNonMatch(int nreps) {
    RE2 re = RE2.compile("^zbc(d|e)");
    for (int i = 0; i < nreps; i++) {
      re.match(LONG_DATA);
    }
  }

  @Benchmark
  public void benchmarkAnchoredShortMatch(int nreps) {
    String x = "abcdefghijklmnopqrstuvwxyz";
    RE2 re = RE2.compile("^.bc(d|e)");
    for (int i = 0; i < nreps; i++) {
      re.match(x);
    }
  }

  @Benchmark
  public void benchmarkAnchoredLongMatch(int nreps) {
    RE2 re = RE2.compile("^.bc(d|e)");
    for (int i = 0; i < nreps; i++) {
      re.match(LONG_DATA);
    }
  }
}