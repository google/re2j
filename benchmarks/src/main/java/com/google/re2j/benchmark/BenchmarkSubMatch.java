// Copyright 2019 Google Inc. All Rights Reserved.

package com.google.re2j.benchmark;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class BenchmarkSubMatch {

  @Param({"JDK", "RE2J"})
  private Implementations impl;

  private String html =
      new String(readFile("google-maps-contact-info.html"), StandardCharsets.UTF_8);
  private Implementations.Pattern pattern;

  @Setup
  public void setup() {
    pattern = Implementations.Pattern.compile(impl, "([0-9]{3}-[0-9]{3}-[0-9]{4})");
  }

  @Benchmark
  public void findPhoneNumbers(Blackhole bh) {
    Implementations.Matcher matcher = pattern.compile(html);
    int count = 0;
    while (matcher.find()) {
      bh.consume(matcher.group());
      count++;
    }
    if (count != 1) {
      throw new AssertionError("Expected to match one phone number.");
    }
  }

  private static byte[] readFile(String name) {
    try (InputStream in = BenchmarkSubMatch.class.getClassLoader().getResourceAsStream(name);
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      int read;
      while ((read = in.read()) > -1) {
        out.write(read);
      }
      return out.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
