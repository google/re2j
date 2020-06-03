RE2/J: linear time regular expression matching in Java
======================================================

[![Build Status](https://travis-ci.org/google/re2j.svg?branch=master)](https://travis-ci.org/google/re2j)
[![Coverage Status](https://coveralls.io/repos/github/google/re2j/badge.svg)](https://coveralls.io/github/google/re2j)

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
of regular expressions patterns in practice avoid such features.

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
* Matcher.hasAnchoringBounds()
* Matcher.hasTransparentBounds()
* Matcher.hitEnd()
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

# Getting RE2/J

If you're using Maven, you can use the following snippet in your `pom.xml` to get RE2/J:

```xml
<dependency>
  <groupId>com.google.re2j</groupId>
  <artifactId>re2j</artifactId>
  <version>1.3</version>
</dependency>
```

You can use the same artifact details in any build system compatible with the Maven Central repositories (e.g. Gradle, Ivy).

You can also download RE2/J the old-fashioned way: go to [the RE2/J release tag](https://github.com/google/re2j/releases), download the RE2/J JAR and add it to your CLASSPATH.

# Discussion and contribution

We have set up a Google Group for discussion, please join the [RE2/J discussion
list](http://groups.google.com/group/re2j-discuss) if you'd like to get in
touch.

If you would like to contribute patches, please see the [instructions for
contributors](CONTRIBUTING.md).

# Who wrote this?

RE2 was designed and implemented in C++ by Russ Cox. The C++ implementation
includes both NFA and DFA engines and numerous optimisations. Russ also ported
a simplified version of the NFA to Go. Alan Donovan ported the NFA-based Go
implementation to Java. Afroz Mohiuddin wrapped the engine in a familiar Java
`Matcher` / `Pattern` API. James Ring prepared the open-source release
and has been its primary maintainer since then.
