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

    public static Pattern compile(Implementations impl, String pattern) {
      switch (impl) {
        case JDK:
          return new JdkPattern(pattern);
        case RE2J:
          return new Re2Pattern(pattern);
        default:
          throw new AssertionError();
      }
    }

    public abstract Matcher matcher(String str);

    public static class JdkPattern extends Pattern {

      private final java.util.regex.Pattern pattern;

      public JdkPattern(String pattern) {
        this.pattern = java.util.regex.Pattern.compile(pattern);
      }

      @Override
      public Matcher matcher(String str) {
        return new Matcher.JdkMatcher(pattern.matcher(str));
      }
    }

    public static class Re2Pattern extends Pattern {

      private final com.google.re2j.Pattern pattern;

      public Re2Pattern(String pattern) {
        this.pattern = com.google.re2j.Pattern.compile(pattern);
      }

      @Override
      public Matcher matcher(String str) {
        return new Matcher.Re2Matcher(pattern.matcher(str));
      }
    }
  }
}
