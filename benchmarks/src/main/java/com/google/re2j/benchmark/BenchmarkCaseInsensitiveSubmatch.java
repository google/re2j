/*
 * Copyright (c) 2022 The Go Authors. All rights reserved.
 *
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
package com.google.re2j.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

// BenchmarkCaseInsensitiveSubmatch tests the performance of case-insensitive matching
// by testing a mostly ASCII regex pattern versus a moderately large text containing both
// ASCII and Unicode characters.
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class BenchmarkCaseInsensitiveSubmatch {
  @Param({"JDK", "RE2J"})
  private Implementations impl;

  @Param({"true", "false"})
  private boolean binary;

  private final byte[] bytes = BenchmarkUtils.readResourceFile("unicode-sample-text.txt");

  private final String text = new String(bytes, StandardCharsets.UTF_8);

  private Implementations.Pattern pattern;

  @Setup
  public void setup() {
    pattern =
        Implementations.Pattern.compile(
            impl,
            "(prepaid|my)(estub|htspace|mercy|nstrom|paycard|milestonecard|bpcreditcard|groundbiz|giftcardsite|pascoconnect|loweslife|balancenow|aarpmedicare|ccpay|cardstatement|cardstatus)\\.[a-z]{2,6}",
            Implementations.Pattern.FLAG_CASE_INSENSITIVE);
  }

  @Benchmark
  public void caseInsensitiveSubMatch(Blackhole bh) {
    Implementations.Matcher matcher = binary ? pattern.matcher(bytes) : pattern.matcher(text);
    int count = 0;
    while (matcher.find()) {
      bh.consume(matcher.group());
      count++;
    }
    if (count != 0) {
      throw new AssertionError("Expected to not match anything");
    }
  }
}
