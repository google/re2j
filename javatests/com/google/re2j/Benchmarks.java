// Copyright 2011 The Go Authors.  All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

// Uncreative and literal-minded port from Go to Java by
// Alan Donovan <adonovan@google.com>, 2011.
//
// Original Go source here:
// http://code.google.com/p/go/source/browse/src/pkg/regexp/syntax/exec_test.go

package com.google.re2j;

import static org.junit.Assert.fail;

import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import com.carrotsearch.junitbenchmarks.BenchmarkRule;

@RunWith(Parameterized.class)
public class Benchmarks {

  @Rule
  public TestRule benchmarkRule = new BenchmarkRule();

  private final boolean useJdk;

  private Matcher pathologicalBacktracking;
  private RE2 literal;
  private RE2 notLiteral;

  public Benchmarks(Object useJdk) {
    this.useJdk = (Boolean) useJdk;
  }

  @Parameters(name = "useJdk={0}")
  public static Object[] getParameters() {
    return new Object[] { false, true };
  }

  private interface Matcher {
    boolean match(String input);
  }

  @Before
  public void setupExpressions() {
    pathologicalBacktracking = compile("a?a?a?a?a?a?a?a?a?a?a?a?" + "a?a?a?a?a?a?a?a?a?a?a?a?"
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
  @Test
  @BenchmarkOptions(benchmarkRounds = 100, warmupRounds = 10)
  public void benchmarkPathologicalBacktracking() {
    if (!pathologicalBacktracking.match("aaaaaaaaaaaaaaaaaaaaaaaa")) {
      fail("no match!");
    }
  }

  // The following benchmarks were ported from
  // http://code.google.com/p/go/source/browse/src/pkg/regexp/all_test.go

  @Test
  @BenchmarkOptions(benchmarkRounds = 10000, warmupRounds = 1500)
  public void benchmarkLiteral() {
    String x = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxy";
    if (!literal.match(x)) {
      fail("no match!");
    }
  }

  @Test
  @BenchmarkOptions(benchmarkRounds = 10000, warmupRounds = 1500)
  public void benchmarkNotLiteral() {
    String x = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxy";
    if (!notLiteral.match(x)) {
      fail("no match!");
    }
  }

  @Test
  @BenchmarkOptions(benchmarkRounds = 10000, warmupRounds = 1500)
  public void benchmarkMatchClass() {
    String x = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
        + "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" + "w";
    RE2 re = RE2.compile("[abcdw]");
    if (!re.match(x)) {
      fail("no match!");
    }
  }

  @Test
  @BenchmarkOptions(benchmarkRounds = 10000, warmupRounds = 1500)
  public void benchmarkMatchClass_InRange() {
    // 'b' is between 'a' and 'c', so the charclass
    // range checking is no help here.
    String x = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
        + "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb" + "c";
    RE2 re = RE2.compile("[ac]");
    if (!re.match(x)) {
      fail("no match!");
    }
  }

  @Test
  @BenchmarkOptions(benchmarkRounds = 10000, warmupRounds = 1500)
  public void benchmarkReplaceAll() {
    String x = "abcdefghijklmnopqrstuvwxyz";
    RE2 re = RE2.compile("[cjrw]");
    re.replaceAll(x, "");
  }

  @Test
  @BenchmarkOptions(benchmarkRounds = 10000, warmupRounds = 1500)
  public void benchmarkAnchoredLiteralShortNonMatch() {
    String x = "abcdefghijklmnopqrstuvwxyz";
    RE2 re = RE2.compile("^zbc(d|e)");
    re.match(x);
  }

  private static final String LONG_DATA;

  static {
    StringBuilder sb = new StringBuilder(26 << 15);
    for (int i = 0; i < 1 << 15; i++) {
      sb.append("abcdefghijklmnopqrstuvwxyz");
    }
    LONG_DATA = sb.toString();
  }

  @Test
  @BenchmarkOptions(benchmarkRounds = 10000, warmupRounds = 1500)
  public void benchmarkAnchoredLiteralLongNonMatch() {
    RE2 re = RE2.compile("^zbc(d|e)");
    re.match(LONG_DATA);
  }

  @Test
  @BenchmarkOptions(benchmarkRounds = 10000, warmupRounds = 1500)
  public void benchmarkAnchoredShortMatch() {
    String x = "abcdefghijklmnopqrstuvwxyz";
    RE2 re = RE2.compile("^.bc(d|e)");
    re.match(x);
  }

  @Test
  @BenchmarkOptions(benchmarkRounds = 10000, warmupRounds = 1500)
  public void benchmarkAnchoredLongMatch() {
    RE2 re = RE2.compile("^.bc(d|e)");
    re.match(LONG_DATA);
  }
}