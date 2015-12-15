package com.google.re2j;

import static com.google.re2j.Options.Algorithm.DFA;
import static com.google.re2j.Options.Algorithm.NFA;

/**
 * Stores information about specified {@link Options} for constructing {@link Pattern} and {@link
 * RE2} instances in tests.
 */
public abstract class OptionsTest {

  public static final Options RUN_WITH_DFA = Options.builder().setAlgorithm(DFA).build();
  public static final Options RUN_WITH_NFA = Options.builder().setAlgorithm(NFA).build();

  protected final Options options;

  protected OptionsTest(Options options) {
    this.options = options;
  }
}
