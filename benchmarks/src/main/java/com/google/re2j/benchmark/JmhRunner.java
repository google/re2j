// Copyright 2019 Google Inc. All Rights Reserved.

package com.google.re2j.benchmark;

import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

public class JmhRunner {

  public static void main(String[] args) throws RunnerException {
    String benchmarks = System.getProperty("benchmarks");
    System.out.println("Running benchmarks: " + benchmarks);

    Options opts =
        new OptionsBuilder()
            .include(benchmarks)
            .forks(1)
            .warmupIterations(5)
            .warmupTime(TimeValue.seconds(1))
            .measurementIterations(5)
            .measurementTime(TimeValue.seconds(1))
            .mode(Mode.AverageTime)
            .build();

    new Runner(opts).run();
  }
}
