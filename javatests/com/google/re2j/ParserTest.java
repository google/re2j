/*
 * Copyright (c) 2020 The Go Authors. All rights reserved.
 *
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
// Original Go source here:
// http://code.google.com/p/go/source/browse/src/pkg/regexp/syntax/parse_test.go

package com.google.re2j;

import static com.google.re2j.RE2.FOLD_CASE;
import static com.google.re2j.RE2.LITERAL;
import static com.google.re2j.RE2.MATCH_NL;
import static com.google.re2j.RE2.NON_GREEDY;
import static com.google.re2j.RE2.PERL;
import static com.google.re2j.RE2.PERL_X;
import static com.google.re2j.RE2.POSIX;
import static com.google.re2j.RE2.UNICODE_GROUPS;
import static com.google.re2j.RE2.WAS_DOLLAR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;

import com.google.common.truth.Truth;
import org.junit.Test;

/**
 * @author adonovan@google.com (Alan Donovan)
 */
public class ParserTest {

  private interface RunePredicate {
    boolean applies(int rune);
  }

  private static final RunePredicate IS_UPPER =
      new RunePredicate() {
        @Override
        public boolean applies(int r) {
          return Unicode.isUpper(r);
        }
      };

  private static final RunePredicate IS_UPPER_FOLD =
      new RunePredicate() {
        @Override
        public boolean applies(int r) {
          if (Unicode.isUpper(r)) {
            return true;
          }
          for (int c = Unicode.simpleFold(r); c != r; c = Unicode.simpleFold(c)) {
            if (Unicode.isUpper(c)) {
              return true;
            }
          }
          return false;
        }
      };

  private static final Map<Regexp.Op, String> OP_NAMES =
      new EnumMap<Regexp.Op, String>(Regexp.Op.class);

  static {
    OP_NAMES.put(Regexp.Op.NO_MATCH, "no");
    OP_NAMES.put(Regexp.Op.EMPTY_MATCH, "emp");
    OP_NAMES.put(Regexp.Op.LITERAL, "lit");
    OP_NAMES.put(Regexp.Op.CHAR_CLASS, "cc");
    OP_NAMES.put(Regexp.Op.ANY_CHAR_NOT_NL, "dnl");
    OP_NAMES.put(Regexp.Op.ANY_CHAR, "dot");
    OP_NAMES.put(Regexp.Op.BEGIN_LINE, "bol");
    OP_NAMES.put(Regexp.Op.END_LINE, "eol");
    OP_NAMES.put(Regexp.Op.BEGIN_TEXT, "bot");
    OP_NAMES.put(Regexp.Op.END_TEXT, "eot");
    OP_NAMES.put(Regexp.Op.WORD_BOUNDARY, "wb");
    OP_NAMES.put(Regexp.Op.NO_WORD_BOUNDARY, "nwb");
    OP_NAMES.put(Regexp.Op.CAPTURE, "cap");
    OP_NAMES.put(Regexp.Op.STAR, "star");
    OP_NAMES.put(Regexp.Op.PLUS, "plus");
    OP_NAMES.put(Regexp.Op.QUEST, "que");
    OP_NAMES.put(Regexp.Op.REPEAT, "rep");
    OP_NAMES.put(Regexp.Op.CONCAT, "cat");
    OP_NAMES.put(Regexp.Op.ALTERNATE, "alt");
  }

  private static final int TEST_FLAGS = MATCH_NL | PERL_X | UNICODE_GROUPS;

  private static final String[][] PARSE_TESTS = {
    // Base cases
    {"a", "lit{a}"},
    {"a.", "cat{lit{a}dot{}}"},
    {"a.b", "cat{lit{a}dot{}lit{b}}"},
    {"ab", "str{ab}"},
    {"a.b.c", "cat{lit{a}dot{}lit{b}dot{}lit{c}}"},
    {"abc", "str{abc}"},
    {"a|^", "alt{lit{a}bol{}}"},
    {"a|b", "cc{0x61-0x62}"},
    {"(a)", "cap{lit{a}}"},
    {"(a)|b", "alt{cap{lit{a}}lit{b}}"},
    {"a*", "star{lit{a}}"},
    {"a+", "plus{lit{a}}"},
    {"a?", "que{lit{a}}"},
    {"a{2}", "rep{2,2 lit{a}}"},
    {"a{2,3}", "rep{2,3 lit{a}}"},
    {"a{2,}", "rep{2,-1 lit{a}}"},
    {"a*?", "nstar{lit{a}}"},
    {"a+?", "nplus{lit{a}}"},
    {"a??", "nque{lit{a}}"},
    {"a{2}?", "nrep{2,2 lit{a}}"},
    {"a{2,3}?", "nrep{2,3 lit{a}}"},
    {"a{2,}?", "nrep{2,-1 lit{a}}"},
    // Malformed { } are treated as literals.
    {"x{1001", "str{x{1001}"},
    {"x{9876543210", "str{x{9876543210}"},
    {"x{9876543210,", "str{x{9876543210,}"},
    {"x{2,1", "str{x{2,1}"},
    {"x{1,9876543210", "str{x{1,9876543210}"},
    {"", "emp{}"},
    {"|", "emp{}"}, // alt{emp{}emp{}} but got factored
    {"|x|", "alt{emp{}lit{x}emp{}}"},
    {".", "dot{}"},
    {"^", "bol{}"},
    {"$", "eol{}"},
    {"\\|", "lit{|}"},
    {"\\(", "lit{(}"},
    {"\\)", "lit{)}"},
    {"\\*", "lit{*}"},
    {"\\+", "lit{+}"},
    {"\\?", "lit{?}"},
    {"{", "lit{{}"},
    {"}", "lit{}}"},
    {"\\.", "lit{.}"},
    {"\\^", "lit{^}"},
    {"\\$", "lit{$}"},
    {"\\\\", "lit{\\}"},
    {"[ace]", "cc{0x61 0x63 0x65}"},
    {"[abc]", "cc{0x61-0x63}"},
    {"[a-z]", "cc{0x61-0x7a}"},
    {"[a]", "lit{a}"},
    {"\\-", "lit{-}"},
    {"-", "lit{-}"},
    {"\\_", "lit{_}"},
    {"abc", "str{abc}"},
    {"abc|def", "alt{str{abc}str{def}}"},
    {"abc|def|ghi", "alt{str{abc}str{def}str{ghi}}"},

    // Posix and Perl extensions
    {"[[:lower:]]", "cc{0x61-0x7a}"},
    {"[a-z]", "cc{0x61-0x7a}"},
    {"[^[:lower:]]", "cc{0x0-0x60 0x7b-0x10ffff}"},
    {"[[:^lower:]]", "cc{0x0-0x60 0x7b-0x10ffff}"},
    {"(?i)[[:lower:]]", "cc{0x41-0x5a 0x61-0x7a 0x17f 0x212a}"},
    {"(?i)[a-z]", "cc{0x41-0x5a 0x61-0x7a 0x17f 0x212a}"},
    {"(?i)[^[:lower:]]", "cc{0x0-0x40 0x5b-0x60 0x7b-0x17e 0x180-0x2129 0x212b-0x10ffff}"},
    {"(?i)[[:^lower:]]", "cc{0x0-0x40 0x5b-0x60 0x7b-0x17e 0x180-0x2129 0x212b-0x10ffff}"},
    {"\\d", "cc{0x30-0x39}"},
    {"\\D", "cc{0x0-0x2f 0x3a-0x10ffff}"},
    {"\\s", "cc{0x9-0xa 0xc-0xd 0x20}"},
    {"\\S", "cc{0x0-0x8 0xb 0xe-0x1f 0x21-0x10ffff}"},
    {"\\w", "cc{0x30-0x39 0x41-0x5a 0x5f 0x61-0x7a}"},
    {"\\W", "cc{0x0-0x2f 0x3a-0x40 0x5b-0x5e 0x60 0x7b-0x10ffff}"},
    {"(?i)\\w", "cc{0x30-0x39 0x41-0x5a 0x5f 0x61-0x7a 0x17f 0x212a}"},
    {"(?i)\\W", "cc{0x0-0x2f 0x3a-0x40 0x5b-0x5e 0x60 0x7b-0x17e 0x180-0x2129 0x212b-0x10ffff}"},
    {"[^\\\\]", "cc{0x0-0x5b 0x5d-0x10ffff}"},
    //  { "\\C", "byte{}" },  // probably never

    // Unicode, negatives, and a double negative.
    {"\\p{Braille}", "cc{0x2800-0x28ff}"},
    {"\\P{Braille}", "cc{0x0-0x27ff 0x2900-0x10ffff}"},
    {"\\p{^Braille}", "cc{0x0-0x27ff 0x2900-0x10ffff}"},
    {"\\P{^Braille}", "cc{0x2800-0x28ff}"},
    {"\\pZ", "cc{0x20 0xa0 0x1680 0x180e 0x2000-0x200a 0x2028-0x2029 0x202f 0x205f 0x3000}"},
    {"[\\p{Braille}]", "cc{0x2800-0x28ff}"},
    {"[\\P{Braille}]", "cc{0x0-0x27ff 0x2900-0x10ffff}"},
    {"[\\p{^Braille}]", "cc{0x0-0x27ff 0x2900-0x10ffff}"},
    {"[\\P{^Braille}]", "cc{0x2800-0x28ff}"},
    {"[\\pZ]", "cc{0x20 0xa0 0x1680 0x180e 0x2000-0x200a 0x2028-0x2029 0x202f 0x205f 0x3000}"},
    {"\\p{Lu}", mkCharClass(IS_UPPER)},
    {"[\\p{Lu}]", mkCharClass(IS_UPPER)},
    {"(?i)[\\p{Lu}]", mkCharClass(IS_UPPER_FOLD)},
    {"\\p{Any}", "dot{}"},
    {"\\p{^Any}", "cc{}"},

    // Hex, octal.
    {"[\\012-\\234]\\141", "cat{cc{0xa-0x9c}lit{a}}"},
    {"[\\x{41}-\\x7a]\\x61", "cat{cc{0x41-0x7a}lit{a}}"},

    // More interesting regular expressions.
    {"a{,2}", "str{a{,2}}"},
    {"\\.\\^\\$\\\\", "str{.^$\\}"},
    {"[a-zABC]", "cc{0x41-0x43 0x61-0x7a}"},
    {"[^a]", "cc{0x0-0x60 0x62-0x10ffff}"},
    {"[α-ε☺]", "cc{0x3b1-0x3b5 0x263a}"}, // utf-8
    {"a*{", "cat{star{lit{a}}lit{{}}"},

    // Test precedences
    {"(?:ab)*", "star{str{ab}}"},
    {"(ab)*", "star{cap{str{ab}}}"},
    {"ab|cd", "alt{str{ab}str{cd}}"},
    {"a(b|c)d", "cat{lit{a}cap{cc{0x62-0x63}}lit{d}}"},

    // Test flattening.
    {"(?:a)", "lit{a}"},
    {"(?:ab)(?:cd)", "str{abcd}"},
    {"(?:a+b+)(?:c+d+)", "cat{plus{lit{a}}plus{lit{b}}plus{lit{c}}plus{lit{d}}}"},
    {"(?:a+|b+)|(?:c+|d+)", "alt{plus{lit{a}}plus{lit{b}}plus{lit{c}}plus{lit{d}}}"},
    {"(?:a|b)|(?:c|d)", "cc{0x61-0x64}"},
    {"a|.", "dot{}"},
    {".|a", "dot{}"},
    {"(?:[abc]|A|Z|hello|world)", "alt{cc{0x41 0x5a 0x61-0x63}str{hello}str{world}}"},
    {"(?:[abc]|A|Z)", "cc{0x41 0x5a 0x61-0x63}"},

    // Test Perl quoted literals
    {"\\Q+|*?{[\\E", "str{+|*?{[}"},
    {"\\Q+\\E+", "plus{lit{+}}"},
    {"\\Qab\\E+", "cat{lit{a}plus{lit{b}}}"},
    {"\\Q\\\\E", "lit{\\}"},
    {"\\Q\\\\\\E", "str{\\\\}"},

    // Test Perl \A and \z
    {"(?m)^", "bol{}"},
    {"(?m)$", "eol{}"},
    {"(?-m)^", "bot{}"},
    {"(?-m)$", "eot{}"},
    {"(?m)\\A", "bot{}"},
    {"(?m)\\z", "eot{\\z}"},
    {"(?-m)\\A", "bot{}"},
    {"(?-m)\\z", "eot{\\z}"},

    // Test named captures
    {"(?P<name>a)", "cap{name:lit{a}}"},

    // Case-folded literals
    {"[Aa]", "litfold{A}"},
    {"[\\x{100}\\x{101}]", "litfold{Ā}"},
    {"[Δδ]", "litfold{Δ}"},

    // Strings
    {"abcde", "str{abcde}"},
    {"[Aa][Bb]cd", "cat{strfold{AB}str{cd}}"},

    // Factoring.
    {
      "abc|abd|aef|bcx|bcy",
      "alt{cat{lit{a}alt{cat{lit{b}cc{0x63-0x64}}str{ef}}}cat{str{bc}cc{0x78-0x79}}}"
    },
    {
      "ax+y|ax+z|ay+w",
      "cat{lit{a}alt{cat{plus{lit{x}}lit{y}}cat{plus{lit{x}}lit{z}}cat{plus{lit{y}}lit{w}}}}"
    },

    // Bug fixes.

    {"(?:.)", "dot{}"},
    {"(?:x|(?:xa))", "cat{lit{x}alt{emp{}lit{a}}}"},
    {"(?:.|(?:.a))", "cat{dot{}alt{emp{}lit{a}}}"},
    {"(?:A(?:A|a))", "cat{lit{A}litfold{A}}"},
    {"(?:A|a)", "litfold{A}"},
    {"A|(?:A|a)", "litfold{A}"},
    {"(?s).", "dot{}"},
    {"(?-s).", "dnl{}"},
    {"(?:(?:^).)", "cat{bol{}dot{}}"},
    {"(?-s)(?:(?:^).)", "cat{bol{}dnl{}}"},
    {"[\\x00-\\x{10FFFF}]", "dot{}"},
    {"[^\\x00-\\x{10FFFF}]", "cc{}"},
    {"(?:[a][a-])", "cat{lit{a}cc{0x2d 0x61}}"},

    // RE2 prefix_tests
    {"abc|abd", "cat{str{ab}cc{0x63-0x64}}"},
    {"a(?:b)c|abd", "cat{str{ab}cc{0x63-0x64}}"},
    {
      "abc|abd|aef|bcx|bcy",
      "alt{cat{lit{a}alt{cat{lit{b}cc{0x63-0x64}}str{ef}}}" + "cat{str{bc}cc{0x78-0x79}}}"
    },
    {"abc|x|abd", "alt{str{abc}lit{x}str{abd}}"},
    {"(?i)abc|ABD", "cat{strfold{AB}cc{0x43-0x44 0x63-0x64}}"},
    {"[ab]c|[ab]d", "cat{cc{0x61-0x62}cc{0x63-0x64}}"},
    {".c|.d", "cat{dot{}cc{0x63-0x64}}"},
    {"x{2}|x{2}[0-9]", "cat{rep{2,2 lit{x}}alt{emp{}cc{0x30-0x39}}}"},
    {"x{2}y|x{2}[0-9]y", "cat{rep{2,2 lit{x}}alt{lit{y}cat{cc{0x30-0x39}lit{y}}}}"},
    {"a.*?c|a.*?b", "cat{lit{a}alt{cat{nstar{dot{}}lit{c}}cat{nstar{dot{}}lit{b}}}}"},
  };

  // TODO(adonovan): add some tests for:
  // - ending a regexp with "\\"
  // - Java UTF-16 things.

  @Test
  public void testParseSimple() {
    testParseDump(PARSE_TESTS, TEST_FLAGS);
  }

  private static final String[][] FOLDCASE_TESTS = {
    {"AbCdE", "strfold{ABCDE}"},
    {"[Aa]", "litfold{A}"},
    {"a", "litfold{A}"},

    // 0x17F is an old English long s (looks like an f) and folds to s.
    // 0x212A is the Kelvin symbol and folds to k.
    {"A[F-g]", "cat{litfold{A}cc{0x41-0x7a 0x17f 0x212a}}"}, // [Aa][A-z...]
    {"[[:upper:]]", "cc{0x41-0x5a 0x61-0x7a 0x17f 0x212a}"},
    {"[[:lower:]]", "cc{0x41-0x5a 0x61-0x7a 0x17f 0x212a}"}
  };

  @Test
  public void testParseFoldCase() {
    testParseDump(FOLDCASE_TESTS, FOLD_CASE);
  }

  private static final String[][] LITERAL_TESTS = {
    {"(|)^$.[*+?]{5,10},\\", "str{(|)^$.[*+?]{5,10},\\}"},
  };

  @Test
  public void testParseLiteral() {
    testParseDump(LITERAL_TESTS, LITERAL);
  }

  private static final String[][] MATCHNL_TESTS = {
    {".", "dot{}"},
    {"\n", "lit{\n}"},
    {"[^a]", "cc{0x0-0x60 0x62-0x10ffff}"},
    {"[a\\n]", "cc{0xa 0x61}"},
  };

  @Test
  public void testParseMatchNL() {
    testParseDump(MATCHNL_TESTS, MATCH_NL);
  }

  private static final String[][] NOMATCHNL_TESTS = {
    {".", "dnl{}"},
    {"\n", "lit{\n}"},
    {"[^a]", "cc{0x0-0x9 0xb-0x60 0x62-0x10ffff}"},
    {"[a\\n]", "cc{0xa 0x61}"},
  };

  @Test
  public void testParseNoMatchNL() {
    testParseDump(NOMATCHNL_TESTS, 0);
  }

  // Test Parse -> Dump.
  private void testParseDump(String[][] tests, int flags) {
    for (String[] test : tests) {
      try {
        Regexp re = Parser.parse(test[0], flags);
        String d = dump(re);
        Truth.assertWithMessage("parse/dump of " + test[0]).that(d).isEqualTo(test[1]);
      } catch (PatternSyntaxException e) {
        throw new RuntimeException("Parsing failed: " + test[0], e);
      }
    }
  }

  // dump prints a string representation of the regexp showing
  // the structure explicitly.
  private static String dump(Regexp re) {
    StringBuilder b = new StringBuilder();
    dumpRegexp(b, re);
    return b.toString();
  }

  // dumpRegexp writes an encoding of the syntax tree for the regexp |re|
  // to |b|.  It is used during testing to distinguish between parses that
  // might print the same using re's toString() method.
  private static void dumpRegexp(StringBuilder b, Regexp re) {
    String name = OP_NAMES.get(re.op);
    if (name == null) {
      b.append("op").append(re.op);
    } else {
      switch (re.op) {
        case STAR:
        case PLUS:
        case QUEST:
        case REPEAT:
          if ((re.flags & NON_GREEDY) != 0) {
            b.append('n');
          }
          b.append(name);
          break;
        case LITERAL:
          if (re.runes.length > 1) {
            b.append("str");
          } else {
            b.append("lit");
          }
          if ((re.flags & FOLD_CASE) != 0) {
            for (int r : re.runes) {
              if (Unicode.simpleFold(r) != r) {
                b.append("fold");
                break;
              }
            }
          }
          break;
        default:
          b.append(name);
          break;
      }
    }
    b.append('{');
    switch (re.op) {
      case END_TEXT:
        if ((re.flags & WAS_DOLLAR) == 0) {
          b.append("\\z");
        }
        break;
      case LITERAL:
        for (int r : re.runes) {
          b.appendCodePoint(r);
        }
        break;
      case CONCAT:
      case ALTERNATE:
        for (Regexp sub : re.subs) {
          dumpRegexp(b, sub);
        }
        break;
      case STAR:
      case PLUS:
      case QUEST:
        dumpRegexp(b, re.subs[0]);
        break;
      case REPEAT:
        b.append(re.min).append(',').append(re.max).append(' ');
        dumpRegexp(b, re.subs[0]);
        break;
      case CAPTURE:
        if (re.name != null && !re.name.isEmpty()) {
          b.append(re.name);
          b.append(':');
        }
        dumpRegexp(b, re.subs[0]);
        break;
      case CHAR_CLASS:
        {
          String sep = "";
          for (int i = 0; i < re.runes.length; i += 2) {
            b.append(sep);
            sep = " ";
            int lo = re.runes[i], hi = re.runes[i + 1];
            if (lo == hi) {
              b.append(String.format("%#x", lo));
            } else {
              b.append(String.format("%#x-%#x", lo, hi));
            }
          }
          break;
        }
    }
    b.append('}');
  }

  private static String mkCharClass(RunePredicate f) {
    Regexp re = new Regexp(Regexp.Op.CHAR_CLASS);
    ArrayList<Integer> runes = new ArrayList<Integer>();
    int lo = -1;
    for (int i = 0; i <= Unicode.MAX_RUNE; i++) {
      if (f.applies(i)) {
        if (lo < 0) {
          lo = i;
        }
      } else {
        if (lo >= 0) {
          runes.add(lo);
          runes.add(i - 1);
          lo = -1;
        }
      }
    }
    if (lo >= 0) {
      runes.add(lo);
      runes.add(Unicode.MAX_RUNE);
    }
    re.runes = new int[runes.size()];
    int j = 0;
    for (Integer i : runes) {
      re.runes[j++] = i;
    }
    return dump(re);
  }

  @Test
  public void testAppendRangeCollapse() {
    // AppendRange should collapse each of the new ranges
    // into the earlier ones (it looks back two ranges), so that
    // the slice never grows very large.
    // Note that we are not calling cleanClass.
    CharClass cc = new CharClass();
    // Add 'A', 'a', 'B', 'b', etc.
    for (int i = 'A'; i <= 'Z'; i++) {
      cc.appendRange(i, i);
      cc.appendRange(i + 'a' - 'A', i + 'a' - 'A');
    }
    assertEquals("AZaz", runesToString(cc.toArray()));
  }

  // Converts an array of Unicode runes to a Java UTF-16 string.
  private static String runesToString(int[] runes) {
    StringBuilder out = new StringBuilder();
    for (int rune : runes) {
      out.appendCodePoint(rune);
    }
    return out.toString();
  }

  private static final String[] INVALID_REGEXPS = {
    "(",
    ")",
    "(a",
    "(a|b|",
    "(a|b",
    "[a-z",
    "([a-z)",
    "x{1001}",
    "x{9876543210}",
    "x{2,1}",
    "x{1,9876543210}",
    // Java string literals can't contain Invalid UTF-8.
    // "\\xff",
    // "[\xff]",
    // "[\\\xff]",
    // "\\\xff",
    "(?P<name>a",
    "(?P<name>",
    "(?P<name",
    "(?P<x y>a)",
    "(?P<>a)",
    "[a-Z]",
    "(?i)[a-Z]",
    "a{100000}",
    "a{100000,}",
    // Group names may not be repeated
    "(?P<foo>bar)(?P<foo>baz)",
    "\\x", // https://github.com/google/re2j/issues/103
    "\\xv", // https://github.com/google/re2j/issues/103
  };

  private static final String[] ONLY_PERL = {
    "[a-b-c]",
    "\\Qabc\\E",
    "\\Q*+?{[\\E",
    "\\Q\\\\E",
    "\\Q\\\\\\E",
    "\\Q\\\\\\\\E",
    "\\Q\\\\\\\\\\E",
    "(?:a)",
    "(?P<name>a)",
  };

  private static final String[] ONLY_POSIX = {
    "a++", "a**", "a?*", "a+*", "a{1}*", ".{1}{2}.{3}",
  };

  @Test
  public void testParseInvalidRegexps() throws PatternSyntaxException {
    for (String regexp : INVALID_REGEXPS) {
      try {
        Regexp re = Parser.parse(regexp, PERL);
        fail("Parsing (PERL) " + regexp + " should have failed, instead got " + dump(re));
      } catch (PatternSyntaxException e) {
        /* ok */
      }
      try {
        Regexp re = Parser.parse(regexp, POSIX);
        fail("parsing (POSIX) " + regexp + " should have failed, instead got " + dump(re));
      } catch (PatternSyntaxException e) {
        /* ok */
      }
    }
    for (String regexp : ONLY_PERL) {
      Parser.parse(regexp, PERL);
      try {
        Regexp re = Parser.parse(regexp, POSIX);
        fail("parsing (POSIX) " + regexp + " should have failed, instead got " + dump(re));
      } catch (PatternSyntaxException e) {
        /* ok */
      }
    }
    for (String regexp : ONLY_POSIX) {
      try {
        Regexp re = Parser.parse(regexp, PERL);
        fail("parsing (PERL) " + regexp + " should have failed, instead got " + dump(re));
      } catch (PatternSyntaxException e) {
        /* ok */
      }
      Parser.parse(regexp, POSIX);
    }
  }

  @Test
  public void testToStringEquivalentParse() throws PatternSyntaxException {
    for (String[] tt : PARSE_TESTS) {
      Regexp re = Parser.parse(tt[0], TEST_FLAGS);
      String d = dump(re);
      assertEquals(d, tt[1]); // (already ensured by testParseSimple)

      String s = re.toString();
      if (!s.equals(tt[0])) {
        // If toString didn't return the original regexp,
        // it must have found one with fewer parens.
        // Unfortunately we can't check the length here, because
        // toString produces "\\{" for a literal brace,
        // but "{" is a shorter equivalent in some contexts.
        Regexp nre = Parser.parse(s, TEST_FLAGS);
        String nd = dump(nre);
        assertEquals(String.format("parse(%s) -> %s", tt[0], s), d, nd);

        String ns = nre.toString();
        assertEquals(String.format("parse(%s) -> %s", tt[0], s), s, ns);
      }
    }
  }
}
