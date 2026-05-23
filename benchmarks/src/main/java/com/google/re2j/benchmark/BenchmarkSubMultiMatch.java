/*
 * Copyright (c) 2020 The Go Authors. All rights reserved.
 *
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
package com.google.re2j.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class BenchmarkSubMultiMatch {

  @Param({"JDK", "RE2J"})
  private Implementations impl;

  @Param({"true", "false"})
  private boolean binary;

  @Param({"true", "false"})
  private boolean successMatch;

  @Param({"true", "false"})
  private boolean resolveGroups;

  byte[] bytes = BenchmarkUtils.readResourceFile("google-maps-contact-info.html");
  private String html = new String(bytes, StandardCharsets.UTF_8);

  private String sucessPatternUrlString =
      "(https?:\\/\\/(www\\.)?([-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,4})\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*))";
  private String failurePatternUrlString =
      "(https?:\\/\\/(www\\.)?([-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{1})\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*))";
  private Implementations.Pattern successPattern;
  private Implementations.Pattern failurePattern;
  private Implementations.Pattern successPatternResolveGroups;
  private Implementations.Pattern failurePatternResolveGroups;

  @Setup
  public void setup() {
    successPattern = Implementations.Pattern.compile(impl, sucessPatternUrlString);
    successPatternResolveGroups =
        Implementations.Pattern.compile(
            impl, sucessPatternUrlString, Implementations.Pattern.FLAG_RESOLVE_GROUPS_MATCH);
    failurePattern = Implementations.Pattern.compile(impl, failurePatternUrlString);
    failurePatternResolveGroups =
        Implementations.Pattern.compile(
            impl, failurePatternUrlString, Implementations.Pattern.FLAG_RESOLVE_GROUPS_MATCH);
  }

  @Benchmark
  public void findDomains(Blackhole bh) {
    Implementations.Pattern pattern =
        successMatch
            ? (resolveGroups ? successPatternResolveGroups : successPattern)
            : (resolveGroups ? failurePatternResolveGroups : failurePattern);
    Implementations.Matcher matcher = binary ? pattern.matcher(bytes) : pattern.matcher(html);
    int count = 0;
    while (matcher.find()) {
      bh.consume(matcher.group(3));
      count++;
    }
    int expectedMatchers = successMatch ? 178 : 0;
    if (count != expectedMatchers) {
      throw new AssertionError("Expected " + expectedMatchers + " matches.");
    }
  }
}
