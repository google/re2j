package com.google.re2j;

import java.io.Serializable;

import static com.google.re2j.Options.Algorithm.DFA;
import static java.util.Objects.requireNonNull;

public final class Options implements Serializable {
  public static final Options DEFAULT_OPTIONS = builder().build();

  // Start state + end state
  private static final int MINIMUM_NUMBER_OF_DFA_STATES = 2;
  private static final int DEFAULT_NUMBER_OF_DFA_RETRIES = 5;

  private Algorithm algorithm = DFA;
  private EventsListener eventsListener = null;
  private int maximumNumberOfDFAStates = Integer.MAX_VALUE;
  private int numberOfDFARetries = DEFAULT_NUMBER_OF_DFA_RETRIES;

  public enum Algorithm {
    // Use DFA exclusively, throw an exception when maximum number of DFA states is reached n times.
    // DFA machine is reset each time states cache is full.
    DFA,
    // Use DFA, fallback to NFA when maximum number of DFA states is reached n times. DFA machine
    // is reset each time states cache is full.
    DFA_FALLBACK_TO_NFA,
    // use NFA exclusively
    NFA
  }

  public Algorithm getAlgorithm() {
    return algorithm;
  }

  public EventsListener getEventsListener() {
    return eventsListener;
  }

  public int getMaximumNumberOfDFAStates() {
    return maximumNumberOfDFAStates;
  }

  public int getNumberOfDFARetries() {
    return numberOfDFARetries;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Options options = (Options) o;

    return maximumNumberOfDFAStates == options.maximumNumberOfDFAStates
        && numberOfDFARetries == options.numberOfDFARetries
        && algorithm == options.algorithm
        && !(eventsListener != null ? !eventsListener.equals(options.eventsListener) : options.eventsListener != null);

  }

  @Override
  public int hashCode() {
    int result = algorithm.hashCode();
    result = 31 * result + (eventsListener != null ? eventsListener.hashCode() : 0);
    result = 31 * result + maximumNumberOfDFAStates;
    result = 31 * result + numberOfDFARetries;
    return result;
  }

  @Override
  public String toString() {
    return "Options{" +
        "algorithm=" + algorithm +
        ", eventsListener=" + eventsListener +
        ", maximumNumberOfDFAStates=" + maximumNumberOfDFAStates +
        ", numberOfDFARetries=" + numberOfDFARetries +
        '}';
  }

  public static OptionsBuilder builder() {
    return new OptionsBuilder();
  }

  /**
   * Interface for RE2J events listening.
   */
  public interface EventsListener {

    /**
     * Called when NFA is being used instead of DFA because too many {@link DFAState}s has been
     * created.
     */
    void fallbackToNFA();
  }

  public static final class OptionsBuilder {
    private Options options = new Options();

    public OptionsBuilder setAlgorithm(Algorithm algorithm) {
      options.algorithm = requireNonNull(algorithm);
      return this;
    }

    public OptionsBuilder setMaximumNumberOfDFAStates(int maximumNumberOfDFAStates) {
      if (maximumNumberOfDFAStates < MINIMUM_NUMBER_OF_DFA_STATES) {
        throw new IllegalArgumentException("maximum number of DFA states must be larger or equal to " + MINIMUM_NUMBER_OF_DFA_STATES);
      }
      options.maximumNumberOfDFAStates = maximumNumberOfDFAStates;
      return this;
    }

    public OptionsBuilder setNumberOfDFARetries(int numberOfDFARetries) {
      if (numberOfDFARetries < 0) {
        throw new IllegalArgumentException("number of DFA retries cannot be below 0");
      }
      options.numberOfDFARetries = numberOfDFARetries;
      return this;
    }

    public OptionsBuilder setEventsListener(EventsListener eventsListener) {
      options.eventsListener = requireNonNull(eventsListener);
      return this;
    }

    public Options build() {
      return options;
    }
  }
}
