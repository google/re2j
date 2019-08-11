// Copyright 2019 Google Inc. All Rights Reserved.

package com.google.re2j.benchmark;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class BenchmarkCompile {

  @Param({"JDK", "RE2J"})
  private Implementations impl;

  @Param({"DATE", "EMAIL", "PHONE", "RANDOM", "SOCIAL", "STATES"})
  private Regex regex;

  @Benchmark
  public void compile(Blackhole bh) {
    bh.consume(Implementations.Pattern.compile(impl, regex.pattern));
  }

  public enum Regex {
    DATE("([0-9]{4})-?(1[0-2]|0[1-9])-?(3[01]|0[1-9]|[12][0-9])"),
    EMAIL("[\\w\\.]+@[\\w\\.]+"),
    PHONE("([0-9]{3})-([0-9]{3})-([0-9]{4})"),
    RANDOM("($+((((($+((a+a*)+(b+c))*)((cc)(b+b))+a)+((b+c*)+(c+c)))+a)+(c*a+($+(c+c)b))))+c"),
    SOCIAL("[0-8][0-9]{2}-[0-9]{2}-[0-9]{4}"),
    STATES(
        "A[ZLRK]|C[TAO]|D[CE]|FL|GA|HI|I[ALND]|K[SY]|LA|M[ADEINOST]|"
            + "N[HCDEJMVY]|O[HKR]|PA|RI|S[CD]|T[XN]|UT|V[AT]|W[VAIY]");

    private final String pattern;

    Regex(String pattern) {
      this.pattern = pattern;
    }
  }
}
