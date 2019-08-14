// Copyright 2019 Google Inc. All Rights Reserved.

package com.google.re2j.benchmark;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class BenchmarkFullMatch {

  @Param({"JDK", "RE2J"})
  private Implementations impl;

  private Implementations.Pattern pattern;

  @Setup
  public void setup() {
    pattern =
        Implementations.Pattern.compile(
            impl, "12345|123123|qwerty|mypass|abcdefg|hello|secret|admin|root|password");
  }

  @Benchmark
  public void matched(Blackhole bh) {
    Implementations.Matcher matcher = pattern.matcher("password");
    boolean matches = matcher.matches();
    if (!matches) {
      throw new AssertionError();
    }
    bh.consume(matches);
  }

  @Benchmark
  public void notMatched(Blackhole bh) {
    Implementations.Matcher matcher = pattern.matcher("l0ngpassword");
    boolean matches = matcher.matches();
    if (matches) {
      throw new AssertionError();
    }
    bh.consume(matches);
  }
}
