RE2/J: linear time regular expression matching in Java
======================================================

[![Build Status](https://travis-ci.org/google/re2j.svg?branch=master)](https://travis-ci.org/google/re2j)

RE2 is a regular expression engine that runs in time linear in the size of the
input. RE2/J is a port of RE2 to pure Java.

Java's standard regular expression package, `java.util.regex`, and many other
widely used regular expression packages such as PCRE, Perl and Python use a
backtracking implementation strategy: when a pattern presents two alternatives
such as `a|b`, the engine will try to match subpattern `a` first, and if that
yields no match, it will reset the input stream and try to match `b` instead.

If such choices are deeply nested, this strategy requires an exponential number
of passes over the input data before it can detect whether the input matches.
If the input is large, it is easy to construct a pattern whose running time
would exceed the lifetime of the universe. This creates a security risk when
accepting regular expression patterns from untrusted sources, such as users of
a web application.

In contrast, the RE2 algorithm explores all matches simultaneously in a single
pass over the input data by using a _nondeterministic_ finite automaton.

There are certain features of PCRE or Perl regular expressions that cannot be
implemented in linear time, for example, backreferences, but the vast majority
of regular expressions patterns in practise avoid such features.

# Why should I switch?

If you use regular expression patterns with a high degree of alternation, your
code may run faster with RE2/J. In the worst case, the `java.util.regex`
matcher may run forever, or exceed the available stack space and fail; this
will never happen with RE2/J.

# Caveats

This is not an official Google product (experimental or otherwise), it is just
code that happens to be owned by Google.

RE2/J is not a drop-in replacement for `java.util.regex`. Aside from the
different package name, it doesn't support the following parts of the
interface:

* the MatchResult class
* Matcher.group(String)
* Matcher.hasAnchoringBounds()
* Matcher.hasTransparentBounds()
* Matcher.hitEnd()
* Matcher.quoteReplacement(String)
* Matcher.region(int, int)
* Matcher.regionEnd()
* Matcher.regionStart()
* Matcher.requireEnd()
* Matcher.toMatchResult()
* Matcher.useAnchoringBounds(boolean)
* Matcher.usePattern(Pattern)
* Matcher.useTransparentBounds(boolean)
* CANON_EQ
* COMMENTS
* LITERAL
* UNICODE_CASE
* UNICODE_CHARACTER_CLASS
* UNIX_LINES
* PatternSyntaxException.getMessage()

It also doesn't have parity with the full set of Java's character classes and
special regular expression constructs.

# Who wrote this?

RE2 was designed and implemented in C++ by Russ Cox. The C++ implementation
includes both NFA and DFA engines and numerous optimisations. Russ also ported
a simplified version of the NFA to Go. Alan Donovan ported the NFA-based Go
implementation to Java. Afroz Mohiuddin wrapped the engine in a familiar Java
`Matcher` / `Pattern` API.
