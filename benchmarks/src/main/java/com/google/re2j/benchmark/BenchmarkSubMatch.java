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

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class BenchmarkSubMatch {

  @Param({"JDK", "RE2J"})
  private Implementations impl;

  @Param({"true", "false"})
  private boolean binary;

  byte[] bytes = BenchmarkUtils.readResourceFile("google-maps-contact-info.html");
  private String html = new String(bytes, StandardCharsets.UTF_8);

  private Implementations.Pattern pattern;

  @Setup
  public void setup() {
    pattern = Implementations.Pattern.compile(impl, "([0-9]{3}-[0-9]{3}-[0-9]{4})");
  }

  @Benchmark
  public void findPhoneNumbers(Blackhole bh) {
    Implementations.Matcher matcher = binary ? pattern.matcher(bytes) : pattern.matcher(html);
    int count = 0;
    while (matcher.find()) {
      bh.consume(matcher.group());
      count++;
    }
    if (count != 1) {
      throw new AssertionError("Expected to match one phone number.");
    }
  }
}
