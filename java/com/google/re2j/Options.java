package com.google.re2j;

import java.io.Serializable;

import static com.google.re2j.Options.Algorithm.DFA;
import static java.util.Objects.requireNonNull;

public final class Options implements Serializable {
  public static final Options DEFAULT_OPTIONS = builder().build();

  private Algorithm algorithm = DFA;

  public enum Algorithm {
    DFA,
    NFA
  }

  public Algorithm getAlgorithm() {
    return algorithm;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Options options = (Options) o;

    return algorithm == options.algorithm;
  }

  @Override
  public int hashCode() {
    return algorithm.hashCode();
  }

  @Override
  public String toString() {
    return "Options{" +
        "algorithm=" + algorithm +
        '}';
  }

  public static OptionsBuilder builder() {
    return new OptionsBuilder();
  }

  public static final class OptionsBuilder {
    private Options options = new Options();

    public OptionsBuilder setAlgorithm(Algorithm algorithm) {
      options.algorithm = requireNonNull(algorithm);
      return this;
    }

    public Options build() {
      return options;
    }
  }
}
