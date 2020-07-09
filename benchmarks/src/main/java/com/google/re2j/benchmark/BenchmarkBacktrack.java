/*
 * Copyright (c) 2020 The Go Authors. All rights reserved.
 *
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
package com.google.re2j.benchmark;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class BenchmarkBacktrack {

  @Param({"JDK", "RE2J"})
  private Implementations impl;

  @Param({"5", "10", "15", "20"})
  private int repeats;

  private Implementations.Pattern pattern;

  @Setup
  public void setup() {
    pattern = Implementations.Pattern.compile(impl, repeat("a?", repeats) + repeat("a", repeats));
  }

  @Benchmark
  public void matched(Blackhole bh) {
    Implementations.Matcher matcher = pattern.matcher(repeat("a", repeats));
    boolean matches = matcher.matches();
    if (!matches) {
      throw new AssertionError();
    }
    bh.consume(matches);
  }

  private String repeat(String str, int n) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < n; i++) {
      sb.append(str);
    }
    return sb.toString();
  }
}
