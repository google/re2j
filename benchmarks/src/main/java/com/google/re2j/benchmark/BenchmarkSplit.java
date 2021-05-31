/*
 * Copyright (c) 2021 The Go Authors. All rights reserved.
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
public class BenchmarkSplit {
  @Param({"JDK", "RE2J"})
  private Implementations impl;

  private Implementations.Pattern pattern;

  private String input;

  @Setup
  public void setup() {
    pattern = Implementations.Pattern.compile(impl, "a");

    StringBuilder b = new StringBuilder();
    for (int i = 0; i < 100; i++) {
      b.append("this should be a pretty big string containing a few delimiters to split on");
    }

    input = b.toString();
  }

  @Benchmark
  public void benchmarkSplit(Blackhole bh) {
    bh.consume(pattern.split(input));
  }
}
