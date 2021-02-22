/*
 * Copyright (c) 2020 The Go Authors. All rights reserved.
 *
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
package com.google.re2j.benchmark;

import static java.nio.charset.StandardCharsets.UTF_8;

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

  @Param({"true", "false"})
  private boolean binary;

  private Implementations.Pattern pattern;

  private String password = "password";
  private byte[] password_bytes = password.getBytes(UTF_8);

  private String l0ngpassword = "l0ngpassword";
  private byte[] l0ngpassword_bytes = "l0ngpassword".getBytes(UTF_8);

  @Setup
  public void setup() {
    pattern =
        Implementations.Pattern.compile(
            impl, "12345|123123|qwerty|mypass|abcdefg|hello|secret|admin|root|password");
  }

  @Benchmark
  public void matched(Blackhole bh) {
    Implementations.Matcher matcher =
        binary ? pattern.matcher(password_bytes) : pattern.matcher(password);
    boolean matches = matcher.matches();
    if (!matches) {
      throw new AssertionError();
    }
    bh.consume(matches);
  }

  @Benchmark
  public void notMatched(Blackhole bh) {
    Implementations.Matcher matcher =
        binary ? pattern.matcher(l0ngpassword_bytes) : pattern.matcher(l0ngpassword);
    boolean matches = matcher.matches();
    if (matches) {
      throw new AssertionError();
    }
    bh.consume(matches);
  }
}
