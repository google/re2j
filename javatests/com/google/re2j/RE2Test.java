// Copyright 2011 The Go Authors.  All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

// Original Go source here:
// http://code.google.com/p/go/source/browse/src/pkg/regexp/syntax/all_test.go

package com.google.re2j;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

/** Tests of RE2 API. */
public class RE2Test extends GoTestCase {
  // (pattern, replacement, input, output)
  private static final String[][] REPLACE_TESTS = {
    // Test empty input and/or replacement,
    // with pattern that matches the empty string.
    {"", "", "", ""},
    {"", "x", "", "x"},
    {"", "", "abc", "abc"},
    {"", "x", "abc", "xaxbxcx"},

    // Test empty input and/or replacement,
    // with pattern that does not match the empty string.
    {"b", "", "", ""},
    {"b", "x", "", ""},
    {"b", "", "abc", "ac"},
    {"b", "x", "abc", "axc"},
    {"y", "", "", ""},
    {"y", "x", "", ""},
    {"y", "", "abc", "abc"},
    {"y", "x", "abc", "abc"},

    // Multibyte characters -- verify that we don't try to match in the middle
    // of a character.
    {"[a-c]*", "x", "\u65e5", "x\u65e5x"},
    {"[^\u65e5]", "x", "abc\u65e5def", "xxx\u65e5xxx"},

    // Start and end of a string.
    {"^[a-c]*", "x", "abcdabc", "xdabc"},
    {"[a-c]*$", "x", "abcdabc", "abcdx"},
    {"^[a-c]*$", "x", "abcdabc", "abcdabc"},
    {"^[a-c]*", "x", "abc", "x"},
    {"[a-c]*$", "x", "abc", "x"},
    {"^[a-c]*$", "x", "abc", "x"},
    {"^[a-c]*", "x", "dabce", "xdabce"},
    {"[a-c]*$", "x", "dabce", "dabcex"},
    {"^[a-c]*$", "x", "dabce", "dabce"},
    {"^[a-c]*", "x", "", "x"},
    {"[a-c]*$", "x", "", "x"},
    {"^[a-c]*$", "x", "", "x"},

    {"^[a-c]+", "x", "abcdabc", "xdabc"},
    {"[a-c]+$", "x", "abcdabc", "abcdx"},
    {"^[a-c]+$", "x", "abcdabc", "abcdabc"},
    {"^[a-c]+", "x", "abc", "x"},
    {"[a-c]+$", "x", "abc", "x"},
    {"^[a-c]+$", "x", "abc", "x"},
    {"^[a-c]+", "x", "dabce", "dabce"},
    {"[a-c]+$", "x", "dabce", "dabce"},
    {"^[a-c]+$", "x", "dabce", "dabce"},
    {"^[a-c]+", "x", "", ""},
    {"[a-c]+$", "x", "", ""},
    {"^[a-c]+$", "x", "", ""},

    // Other cases.
    {"abc", "def", "abcdefg", "defdefg"},
    {"bc", "BC", "abcbcdcdedef", "aBCBCdcdedef"},
    {"abc", "", "abcdabc", "d"},
    {"x", "xXx", "xxxXxxx", "xXxxXxxXxXxXxxXxxXx"},
    {"abc", "d", "", ""},
    {"abc", "d", "abc", "d"},
    {".+", "x", "abc", "x"},
    {"[a-c]*", "x", "def", "xdxexfx"},
    {"[a-c]+", "x", "abcbcdcdedef", "xdxdedef"},
    {"[a-c]*", "x", "abcbcdcdedef", "xdxdxexdxexfx"},
  };

  private static final RE2.ReplaceFunc REPLACE_XSY = new RE2.ReplaceFunc() {
      public String replace(String s) { return "x" + s + "y"; }
      public String toString() { return "REPLACE_XSY"; }
    };

  // Each row is (String pattern, input, output, ReplaceFunc replacement).
  // Conceptually the replacement func is a table column---but for now
  // it's always REPLACE_XSY.
  private static final String[][] REPLACE_FUNC_TESTS = {
    {"[a-c]",  "defabcdef", "defxayxbyxcydef"},
    {"[a-c]+", "defabcdef", "defxabcydef"},
    {"[a-c]*", "defabcdef", "xydxyexyfxabcydxyexyfxy"},
  };

  private void replaceTestHelper(String [][] testArray, boolean first) {
    for (String[] tc : testArray) {
      RE2 re;
      try {
        re = RE2.compile(tc[0]);
      } catch (PatternSyntaxException e) {
        errorf("Unexpected error compiling %s: %s", tc[0], e.getMessage());
        continue;
      }
      String actual = first ? re.replaceFirst(tc[2], tc[1]) : re.replaceAll(tc[2], tc[1]);
      if (!actual.equals(tc[3])) {
        errorf("%s.replaceAll(%s,%s) = %s; want %s",
               tc[0], tc[2], tc[1], actual, tc[3]);
      }
    }
  }

  public void testReplaceAll() {
    replaceTestHelper(REPLACE_TESTS, false);
  }

  public void testReplaceAllFunc() {
    for (String[] tc : REPLACE_FUNC_TESTS) {
      RE2 re;
      try {
        re = RE2.compile(tc[0]);
      } catch (PatternSyntaxException e) {
        errorf("Unexpected error compiling %s: %s", tc[0], e.getMessage());
        continue;
      }
      String actual = re.replaceAllFunc(tc[1], REPLACE_XSY, tc[1].length());
      if (!actual.equals(tc[2])) {
        errorf("%s.replaceAllFunc(%s,%s) = %s; want %s",
               tc[0], tc[1], REPLACE_XSY, actual, tc[2]);
      }
    }
  }

  private static final String[][] REPLACE_FIRST_TESTS = {
    // Test empty input and/or replacement,
    // with pattern that matches the empty string.
    {"", "", "", ""},
    {"", "x", "", "x"},
    {"", "", "abc", "abc"},
    {"", "x", "abc", "xabc"},

    // Test empty input and/or replacement,
    // with pattern that does not match the empty string.
    {"b", "", "", ""},
    {"b", "x", "", ""},
    {"b", "", "abc", "ac"},
    {"b", "x", "abc", "axc"},
    {"y", "", "", ""},
    {"y", "x", "", ""},
    {"y", "", "abc", "abc"},
    {"y", "x", "abc", "abc"},

    // Multibyte characters -- verify that we don't try to match in the middle
    // of a character.
    {"[a-c]*", "x", "\u65e5", "x\u65e5"},
    {"[^\u65e5]", "x", "abc\u65e5def", "xbc\u65e5def"},

    // Start and end of a string.
    {"^[a-c]*", "x", "abcdabc", "xdabc"},
    {"[a-c]*$", "x", "abcdabc", "abcdx"},
    {"^[a-c]*$", "x", "abcdabc", "abcdabc"},
    {"^[a-c]*", "x", "abc", "x"},
    {"[a-c]*$", "x", "abc", "x"},
    {"^[a-c]*$", "x", "abc", "x"},
    {"^[a-c]*", "x", "dabce", "xdabce"},
    {"[a-c]*$", "x", "dabce", "dabcex"},
    {"^[a-c]*$", "x", "dabce", "dabce"},
    {"^[a-c]*", "x", "", "x"},
    {"[a-c]*$", "x", "", "x"},
    {"^[a-c]*$", "x", "", "x"},

    {"^[a-c]+", "x", "abcdabc", "xdabc"},
    {"[a-c]+$", "x", "abcdabc", "abcdx"},
    {"^[a-c]+$", "x", "abcdabc", "abcdabc"},
    {"^[a-c]+", "x", "abc", "x"},
    {"[a-c]+$", "x", "abc", "x"},
    {"^[a-c]+$", "x", "abc", "x"},
    {"^[a-c]+", "x", "dabce", "dabce"},
    {"[a-c]+$", "x", "dabce", "dabce"},
    {"^[a-c]+$", "x", "dabce", "dabce"},
    {"^[a-c]+", "x", "", ""},
    {"[a-c]+$", "x", "", ""},
    {"^[a-c]+$", "x", "", ""},

    // Other cases.
    {"abc", "def", "abcdefg", "defdefg"},
    {"bc", "BC", "abcbcdcdedef", "aBCbcdcdedef"},
    {"abc", "", "abcdabc", "dabc"},
    {"x", "xXx", "xxxXxxx", "xXxxxXxxx"},
    {"abc", "d", "", ""},
    {"abc", "d", "abc", "d"},
    {".+", "x", "abc", "x"},
    {"[a-c]*", "x", "def", "xdef"},
    {"[a-c]+", "x", "abcbcdcdedef", "xdcdedef"},
    {"[a-c]*", "x", "abcbcdcdedef", "xdcdedef"},
  };

  public void testReplaceFirst() {
    replaceTestHelper(REPLACE_FIRST_TESTS, true);
  }

  // (pattern, output, literal, isLiteral)
  private static final String[][] META_TESTS = {
    {"", "", "", "true"},
    {"foo", "foo", "foo", "true"},
    // has meta but no operator:
    {"foo\\.\\$", "foo\\\\\\.\\\\\\$", "foo.$", "true"},
    // has escaped operators and real operators:
    {"foo.\\$", "foo\\.\\\\\\$", "foo", "false"},
    {"!@#$%^&*()_+-=[{]}\\|,<.>/?~",
     "!@#\\$%\\^&\\*\\(\\)_\\+-=\\[\\{\\]\\}\\\\\\|,<\\.>/\\?~",
     "!@#",
     "false"},
  };

  public void testQuoteMeta() {
    for (String[] tc : META_TESTS) {
      // Verify that quoteMeta returns the expected string.
      String quoted = RE2.quoteMeta(tc[0]);
      if (!quoted.equals(tc[1])) {
        errorf("RE2.quoteMeta(\"%s\") = \"%s\"; want \"%s\"",
               tc[0], quoted, tc[1]);
        continue;
      }

      // Verify that the quoted string is in fact treated as expected
      // by compile -- i.e. that it matches the original, unquoted string.
      if (!tc[0].isEmpty()) {
        RE2 re;
        try {
          re = RE2.compile(quoted);
        } catch (PatternSyntaxException e) {
          errorf("Unexpected error compiling quoteMeta(\"%s\"): %s",
                 tc[0], e.getMessage());
          continue;
        }
        String src = "abc" + tc[0] + "def";
        String repl = "xyz";
        String replaced = re.replaceAll(src, repl);
        String expected = "abcxyzdef";
        if (!replaced.equals(expected)) {
          errorf("quoteMeta(`%s`).replace(`%s`,`%s`) = `%s`; want `%s`",
                 tc[0], src, repl, replaced, expected);
        }
      }
    }
  }

  public void testLiteralPrefix() throws PatternSyntaxException {
    for (String[] tc : META_TESTS) {
      // Literal method needs to scan the pattern.
      RE2 re = RE2.compile(tc[0]);
      if (re.prefixComplete != Boolean.parseBoolean(tc[3])) {
        errorf("literalPrefix(\"%s\") = %s; want %s",
               tc[0], re.prefixComplete, tc[3]);
      }
      if (!re.prefix.equals(tc[2])) {
        errorf("literalPrefix(\"%s\") = \"%s\"; want \"%s\"",
               tc[0], re.prefix, tc[2]);
      }
    }
  }

  private static final String[][] NUM_SUBEXP_CASES = {
    {"", "0"},
    {".*", "0"},
    {"abba", "0"},
    {"ab(b)a", "1"},
    {"ab(.*)a", "1"},
    {"(.*)ab(.*)a", "2"},
    {"(.*)(ab)(.*)a", "3"},
    {"(.*)((a)b)(.*)a", "4"},
    {"(.*)(\\(ab)(.*)a", "3"},
    {"(.*)(\\(a\\)b)(.*)a", "3"},
  };

  public void testNumSubexp() throws PatternSyntaxException {
    for (String[] c : NUM_SUBEXP_CASES) {
      RE2 re = RE2.compile(c[0]);
      int actual = Integer.parseInt(c[1]);
      int expected = re.numberOfCapturingGroups();
      if (expected != actual) {
        errorf("numSubexp for %s returned %d, expected %d",
               c[0], actual, expected);
      }
    }
  }

  public void testFullMatch() {
    assertEquals(true, new RE2("ab+c").match(
        "abbbbbc", 0, 7, RE2.ANCHOR_BOTH, null, 0));
    assertEquals(false, new RE2("ab+c").match(
      "xabbbbbc", 0, 8, RE2.ANCHOR_BOTH, null, 0));
  }

  public void testFindEnd() {
    RE2 r = new RE2("abc.*def");
    assertEquals(true, r.match("yyyabcxxxdefzzz",
                               0, 15, RE2.UNANCHORED, null, 0));
    assertEquals(true, r.match("yyyabcxxxdefzzz",
                               0, 12, RE2.UNANCHORED, null, 0));
    assertEquals(true, r.match("yyyabcxxxdefzzz",
                               3, 15, RE2.UNANCHORED, null, 0));
    assertEquals(true, r.match("yyyabcxxxdefzzz",
                               3, 12, RE2.UNANCHORED, null, 0));
    assertEquals(false, r.match("yyyabcxxxdefzzz",
                                4, 12, RE2.UNANCHORED, null, 0));
    assertEquals(false, r.match("yyyabcxxxdefzzz",
                                3, 11, RE2.UNANCHORED, null, 0));
  }
}
