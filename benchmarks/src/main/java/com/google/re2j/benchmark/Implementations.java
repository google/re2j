/*
 * Copyright (c) 2020 The Go Authors. All rights reserved.
 *
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
package com.google.re2j.benchmark;

public enum Implementations {
  JDK,
  RE2J;

  public abstract static class Matcher {

    public abstract boolean find();

    public abstract boolean matches();

    public abstract String group();

    public static class Re2Matcher extends Matcher {
      private final com.google.re2j.Matcher matcher;

      public Re2Matcher(com.google.re2j.Matcher matcher) {
        this.matcher = matcher;
      }

      @Override
      public boolean find() {
        return matcher.find();
      }

      @Override
      public boolean matches() {
        return matcher.matches();
      }

      @Override
      public String group() {
        return matcher.group();
      }
    }

    public static class JdkMatcher extends Matcher {
      private final java.util.regex.Matcher matcher;

      public JdkMatcher(java.util.regex.Matcher matcher) {
        this.matcher = matcher;
      }

      @Override
      public boolean find() {
        return matcher.find();
      }

      @Override
      public boolean matches() {
        return matcher.matches();
      }

      @Override
      public String group() {
        return matcher.group();
      }
    }
  }

  public abstract static class Pattern {

    // FLAG_CASE_INSENSITIVE is an implementation-agnostic bitmask flag
    // indicating that a pattern should be case-insensitive.
    public static final int FLAG_CASE_INSENSITIVE = 1;

    public static Pattern compile(Implementations impl, String pattern) {
      return compile(impl, pattern, 0);
    }

    public static Pattern compile(Implementations impl, String pattern, int flags) {
      switch (impl) {
        case JDK:
          return new JdkPattern(pattern, flags);
        case RE2J:
          return new Re2Pattern(pattern, flags);
        default:
          throw new AssertionError();
      }
    }

    public abstract Matcher matcher(String str);

    public abstract Matcher matcher(byte[] bytes);

    public abstract String[] split(String str);

    public static class JdkPattern extends Pattern {

      private final java.util.regex.Pattern pattern;

      public JdkPattern(String pattern, int flags) {
        int jdkPatternFlags = 0;

        // For case-insensitive matching, explicitly enable both case-insensitive matching
        // and Unicode-aware case folding for this j.u.r.Pattern.
        // Merely enabling case-insensitive matching will cause the j.u.r.Pattern to assume
        // ASCII input and skip Unicode-aware case folding.
        if ((flags & FLAG_CASE_INSENSITIVE) > 0) {
          jdkPatternFlags |= java.util.regex.Pattern.CASE_INSENSITIVE;
          jdkPatternFlags |= java.util.regex.Pattern.UNICODE_CASE;
        }

        this.pattern = java.util.regex.Pattern.compile(pattern, jdkPatternFlags);
      }

      @Override
      public Matcher matcher(String str) {
        return new Matcher.JdkMatcher(pattern.matcher(str));
      }

      @Override
      public Matcher matcher(byte[] bytes) {
        return new Matcher.JdkMatcher(pattern.matcher(new String(bytes)));
      }

      @Override
      public String[] split(String str) {
        return pattern.split(str);
      }
    }

    public static class Re2Pattern extends Pattern {

      private final com.google.re2j.Pattern pattern;

      public Re2Pattern(String pattern, int flags) {
        int re2PatternFlags = 0;
        if ((flags & FLAG_CASE_INSENSITIVE) > 0) {
          re2PatternFlags |= com.google.re2j.Pattern.CASE_INSENSITIVE;
        }
        this.pattern = com.google.re2j.Pattern.compile(pattern, re2PatternFlags);
      }

      @Override
      public Matcher matcher(String str) {
        return new Matcher.Re2Matcher(pattern.matcher(str));
      }

      @Override
      public Matcher matcher(byte[] bytes) {
        return new Matcher.Re2Matcher(pattern.matcher(bytes));
      }

      @Override
      public String[] split(String str) {
        return pattern.split(str);
      }
    }
  }
}
