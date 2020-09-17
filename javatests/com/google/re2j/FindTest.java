/*
 * Copyright (c) 2020 The Go Authors. All rights reserved.
 *
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
// Original Go source here:
// http://code.google.com/p/go/source/browse/src/pkg/regexp/find_test.go
//
// The original Go function names are documented because of the
// potential for confusion arising from systematic renamings
// (e.g. "String" -> "", "" -> "UTF8", "Test" -> "test", etc.)

package com.google.re2j;

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class FindTest {

  // For each pattern/text pair, what is the expected output of each
  // function?  We can derive the textual results from the indexed
  // results, the non-submatch results from the submatched results, the
  // single results from the 'all' results, and the String results from
  // the UTF-8 results. Therefore the table includes only the
  // findAllUTF8SubmatchIndex result.

  static class Test {
    // The n and x parameters construct a [][]int by extracting n
    // sequences from x.  This represents n matches with len(x)/n
    // submatches each.
    Test(String pat, String text, int n, int... x) {
      this.pat = pat;
      this.text = text;
      this.textUTF8 = GoTestUtils.utf8(text);
      this.matches = new int[n][];
      if (n > 0) {
        int runLength = x.length / n;
        for (int j = 0, i = 0; i < n; i++) {
          matches[i] = new int[runLength];
          System.arraycopy(x, j, matches[i], 0, runLength);
          j += runLength;
          if (j > x.length) {
            fail("invalid build entry");
          }
        }
      }
    }

    final String pat;
    final String text;
    final byte[] textUTF8;
    // Each element is an even-length array of indices into textUTF8.  Not null.
    final int[][] matches;

    byte[] submatchBytes(int i, int j) {
      return Utils.subarray(textUTF8, matches[i][2 * j], matches[i][2 * j + 1]);
    }

    String submatchString(int i, int j) {
      return GoTestUtils.fromUTF8(submatchBytes(i, j)); // yikes
    }

    @Override
    public String toString() {
      return String.format("pat=%s text=%s", pat, text);
    }
  }

  // Used by RE2Test also.
  static final Test[] FIND_TESTS = {
    new Test("", "", 1, 0, 0),
    new Test("^abcdefg", "abcdefg", 1, 0, 7),
    new Test("a+", "baaab", 1, 1, 4),
    new Test("abcd..", "abcdef", 1, 0, 6),
    new Test("a", "a", 1, 0, 1),
    new Test("x", "y", 0),
    new Test("b", "abc", 1, 1, 2),
    new Test(".", "a", 1, 0, 1),
    new Test(".*", "abcdef", 1, 0, 6),
    new Test("^", "abcde", 1, 0, 0),
    new Test("$", "abcde", 1, 5, 5),
    new Test("^abcd$", "abcd", 1, 0, 4),
    new Test("^bcd'", "abcdef", 0),
    new Test("^abcd$", "abcde", 0),
    new Test("a+", "baaab", 1, 1, 4),
    new Test("a*", "baaab", 3, 0, 0, 1, 4, 5, 5),
    new Test("[a-z]+", "abcd", 1, 0, 4),
    new Test("[^a-z]+", "ab1234cd", 1, 2, 6),
    new Test("[a\\-\\]z]+", "az]-bcz", 2, 0, 4, 6, 7),
    new Test("[^\\n]+", "abcd\n", 1, 0, 4),
    new Test("[日本語]+", "日本語日本語", 1, 0, 18),
    new Test("日本語+", "日本語", 1, 0, 9),
    new Test("日本語+", "日本語語語語", 1, 0, 18),
    new Test("()", "", 1, 0, 0, 0, 0),
    new Test("(a)", "a", 1, 0, 1, 0, 1),
    new Test("(.)(.)", "日a", 1, 0, 4, 0, 3, 3, 4),
    new Test("(.*)", "", 1, 0, 0, 0, 0),
    new Test("(.*)", "abcd", 1, 0, 4, 0, 4),
    new Test("(..)(..)", "abcd", 1, 0, 4, 0, 2, 2, 4),
    new Test("(([^xyz]*)(d))", "abcd", 1, 0, 4, 0, 4, 0, 3, 3, 4),
    new Test("((a|b|c)*(d))", "abcd", 1, 0, 4, 0, 4, 2, 3, 3, 4),
    new Test("(((a|b|c)*)(d))", "abcd", 1, 0, 4, 0, 4, 0, 3, 2, 3, 3, 4),
    new Test("\\a\\f\\n\\r\\t\\v", "\007\f\n\r\t\013", 1, 0, 6),
    new Test("[\\a\\f\\n\\r\\t\\v]+", "\007\f\n\r\t\013", 1, 0, 6),
    new Test("a*(|(b))c*", "aacc", 1, 0, 4, 2, 2, -1, -1),
    new Test("(.*).*", "ab", 1, 0, 2, 0, 2),
    new Test("[.]", ".", 1, 0, 1),
    new Test("/$", "/abc/", 1, 4, 5),
    new Test("/$", "/abc", 0),

    // multiple matches
    new Test(".", "abc", 3, 0, 1, 1, 2, 2, 3),
    new Test("(.)", "abc", 3, 0, 1, 0, 1, 1, 2, 1, 2, 2, 3, 2, 3),
    new Test(".(.)", "abcd", 2, 0, 2, 1, 2, 2, 4, 3, 4),
    new Test("ab*", "abbaab", 3, 0, 3, 3, 4, 4, 6),
    new Test("a(b*)", "abbaab", 3, 0, 3, 1, 3, 3, 4, 4, 4, 4, 6, 5, 6),

    // fixed bugs
    new Test("ab$", "cab", 1, 1, 3),
    new Test("axxb$", "axxcb", 0),
    new Test("data", "daXY data", 1, 5, 9),
    new Test("da(.)a$", "daXY data", 1, 5, 9, 7, 8),
    new Test("zx+", "zzx", 1, 1, 3),
    new Test("ab$", "abcab", 1, 3, 5),
    new Test("(aa)*$", "a", 1, 1, 1, -1, -1),
    new Test("(?:.|(?:.a))", "", 0),
    new Test("(?:A(?:A|a))", "Aa", 1, 0, 2),
    new Test("(?:A|(?:A|a))", "a", 1, 0, 1),
    new Test("(a){0}", "", 1, 0, 0, -1, -1),
    new Test("(?-s)(?:(?:^).)", "\n", 0),
    new Test("(?s)(?:(?:^).)", "\n", 1, 0, 1),
    new Test("(?:(?:^).)", "\n", 0),
    new Test("\\b", "x", 2, 0, 0, 1, 1),
    new Test("\\b", "xx", 2, 0, 0, 2, 2),
    new Test("\\b", "x y", 4, 0, 0, 1, 1, 2, 2, 3, 3),
    new Test("\\b", "xx yy", 4, 0, 0, 2, 2, 3, 3, 5, 5),
    new Test("\\B", "x", 0),
    new Test("\\B", "xx", 1, 1, 1),
    new Test("\\B", "x y", 0),
    new Test("\\B", "xx yy", 2, 1, 1, 4, 4),

    // RE2 tests
    new Test("[^\\S\\s]", "abcd", 0),
    new Test("[^\\S[:space:]]", "abcd", 0),
    new Test("[^\\D\\d]", "abcd", 0),
    new Test("[^\\D[:digit:]]", "abcd", 0),
    new Test("(?i)\\W", "x", 0),
    new Test("(?i)\\W", "k", 0),
    new Test("(?i)\\W", "s", 0),

    // can backslash-escape any punctuation
    new Test(
        "\\!\\\"\\#\\$\\%\\&\\'\\(\\)\\*\\+\\,\\-\\.\\/\\:\\;\\<\\=\\>\\?\\@\\[\\\\\\]\\^\\_\\{\\|\\}\\~",
        "!\"#$%&'()*+,-./:;<=>?@[\\]^_{|}~",
        1,
        0,
        31),
    new Test(
        "[\\!\\\"\\#\\$\\%\\&\\'\\(\\)\\*\\+\\,\\-\\.\\/\\:\\;\\<\\=\\>\\?\\@\\[\\\\\\]\\^\\_\\{\\|\\}\\~]+",
        "!\"#$%&'()*+,-./:;<=>?@[\\]^_{|}~",
        1,
        0,
        31),
    new Test("\\`", "`", 1, 0, 1),
    new Test("[\\`]+", "`", 1, 0, 1),

    // long set of matches
    new Test(
        ".",
        "qwertyuiopasdfghjklzxcvbnm1234567890",
        36,
        0,
        1,
        1,
        2,
        2,
        3,
        3,
        4,
        4,
        5,
        5,
        6,
        6,
        7,
        7,
        8,
        8,
        9,
        9,
        10,
        10,
        11,
        11,
        12,
        12,
        13,
        13,
        14,
        14,
        15,
        15,
        16,
        16,
        17,
        17,
        18,
        18,
        19,
        19,
        20,
        20,
        21,
        21,
        22,
        22,
        23,
        23,
        24,
        24,
        25,
        25,
        26,
        26,
        27,
        27,
        28,
        28,
        29,
        29,
        30,
        30,
        31,
        31,
        32,
        32,
        33,
        33,
        34,
        34,
        35,
        35,
        36),
  };

  @Parameters
  public static Test[] testCases() {
    return FIND_TESTS;
  }

  private final Test test;

  public FindTest(Test test) {
    this.test = test;
  }

  // First the simple cases.

  @org.junit.Test
  public void testFindUTF8() {
    RE2 re = RE2.compile(test.pat);
    if (!re.toString().equals(test.pat)) {
      fail(String.format("RE2.toString() = \"%s\"; should be \"%s\"", re.toString(), test.pat));
    }
    byte[] result = re.findUTF8(test.textUTF8);
    if (test.matches.length == 0 && GoTestUtils.len(result) == 0) {
      // ok
    } else if (test.matches.length == 0 && result != null) {
      fail(String.format("findUTF8: expected no match; got one: %s", test));
    } else if (test.matches.length > 0 && result == null) {
      fail(String.format("findUTF8: expected match; got none: %s", test));
    } else {
      byte[] expect = test.submatchBytes(0, 0);
      if (!Arrays.equals(expect, result)) {
        fail(
            String.format(
                "findUTF8: expected %s; got %s: %s",
                GoTestUtils.fromUTF8(expect),
                GoTestUtils.fromUTF8(result),
                test));
      }
    }
  }

  @org.junit.Test
  public void testFind() {
    String result = RE2.compile(test.pat).find(test.text);
    if (test.matches.length == 0 && result.isEmpty()) {
      // ok
    } else if (test.matches.length == 0 && !result.isEmpty()) {
      fail(String.format("find: expected no match; got one: %s", test));
    } else if (test.matches.length > 0 && result.isEmpty()) {
      // Tricky because an empty result has two meanings:
      // no match or empty match.
      int[] match = test.matches[0];
      if (match[0] != match[1]) {
        fail(String.format("find: expected match; got none: %s", test));
      }
    } else {
      String expect = test.submatchString(0, 0);
      if (!expect.equals(result)) {
        fail(String.format("find: expected %s got %s: %s", expect, result, test));
      }
    }
  }

  private void testFindIndexCommon(
      String testName, Test test, int[] result, boolean resultIndicesAreUTF8) {
    if (test.matches.length == 0 && GoTestUtils.len(result) == 0) {
      // ok
    } else if (test.matches.length == 0 && result != null) {
      fail(String.format("%s: expected no match; got one: %s", testName, test));
    } else if (test.matches.length > 0 && result == null) {
      fail(String.format("%s: expected match; got none: %s", testName, test));
    } else {
      if (!resultIndicesAreUTF8) {
        result = GoTestUtils.utf16IndicesToUtf8(result, test.text);
      }
      int[] expect = test.matches[0]; // UTF-8 indices
      if (expect[0] != result[0] || expect[1] != result[1]) {
        fail(
            String.format(
                "%s: expected %s got %s: %s",
                testName,
                Arrays.toString(expect),
                Arrays.toString(result),
                test));
      }
    }
  }

  @org.junit.Test
  public void testFindUTF8Index() {
    testFindIndexCommon(
        "testFindUTF8Index", test, RE2.compile(test.pat).findUTF8Index(test.textUTF8), true);
  }

  @org.junit.Test
  public void testFindIndex() {
    int[] result = RE2.compile(test.pat).findIndex(test.text);
    testFindIndexCommon("testFindIndex", test, result, false);
  }

  // Now come the simple All cases.

  @org.junit.Test
  public void testFindAllUTF8() {
    List<byte[]> result = RE2.compile(test.pat).findAllUTF8(test.textUTF8, -1);
    if (test.matches.length == 0 && result == null) {
      // ok
    } else if (test.matches.length == 0 && result != null) {
      fail(String.format("findAllUTF8: expected no match; got one: %s", test));
    } else if (test.matches.length > 0 && result == null) {
      throw new AssertionError("findAllUTF8: expected match; got none: " + test);
    } else {
      if (test.matches.length != result.size()) {
        fail(
            String.format(
                "findAllUTF8: expected %d matches; got %d: %s",
                test.matches.length,
                result.size(),
                test));
      }
      for (int i = 0; i < test.matches.length; i++) {
        byte[] expect = test.submatchBytes(i, 0);
        if (!Arrays.equals(expect, result.get(i))) {
          fail(
              String.format(
                  "findAllUTF8: match %d: expected %s; got %s: %s",
                  i / 2,
                  GoTestUtils.fromUTF8(expect),
                  GoTestUtils.fromUTF8(result.get(i)),
                  test));
        }
      }
    }
  }

  @org.junit.Test
  public void testFindAll() {
    List<String> result = RE2.compile(test.pat).findAll(test.text, -1);
    if (test.matches.length == 0 && result == null) {
      // ok
    } else if (test.matches.length == 0 && result != null) {
      fail(String.format("findAll: expected no match; got one: %s", test));
    } else if (test.matches.length > 0 && result == null) {
      fail(String.format("findAll: expected match; got none: %s", test));
    } else {
      if (test.matches.length != result.size()) {
        fail(
            String.format(
                "findAll: expected %d matches; got %d: %s",
                test.matches.length,
                result.size(),
                test));
      }
      for (int i = 0; i < test.matches.length; i++) {
        String expect = test.submatchString(i, 0);
        if (!expect.equals(result.get(i))) {
          fail(String.format("findAll: expected %s; got %s: %s", expect, result, test));
        }
      }
    }
  }

  private void testFindAllIndexCommon(
      String testName, Test test, List<int[]> result, boolean resultIndicesAreUTF8) {
    if (test.matches.length == 0 && result == null) {
      // ok
    } else if (test.matches.length == 0 && result != null) {
      fail(String.format("%s: expected no match; got one: %s", testName, test));
    } else if (test.matches.length > 0 && result == null) {
      fail(String.format("%s: expected match; got none: %s", testName, test));
    } else {
      if (test.matches.length != result.size()) {
        fail(
            String.format(
                "%s: expected %d matches; got %d: %s",
                testName,
                test.matches.length,
                result.size(),
                test));
      }
      for (int k = 0; k < test.matches.length; k++) {
        int[] e = test.matches[k];
        int[] res = result.get(k);
        if (!resultIndicesAreUTF8) {
          res = GoTestUtils.utf16IndicesToUtf8(res, test.text);
        }
        if (e[0] != res[0] || e[1] != res[1]) {
          fail(
              String.format(
                  "%s: match %d: expected %s; got %s: %s",
                  testName,
                  k,
                  Arrays.toString(e), // (only 1st two elements matter here)
                  Arrays.toString(res),
                  test));
        }
      }
    }
  }

  @org.junit.Test
  public void testFindAllUTF8Index() {
    testFindAllIndexCommon(
        "testFindAllUTF8Index",
        test,
        RE2.compile(test.pat).findAllUTF8Index(test.textUTF8, -1),
        true);
  }

  @org.junit.Test
  public void testFindAllIndex() {
    testFindAllIndexCommon(
        "testFindAllIndex", test, RE2.compile(test.pat).findAllIndex(test.text, -1), false);
  }

  // Now come the Submatch cases.

  private void testSubmatchBytes(String testName, FindTest.Test test, int n, byte[][] result) {
    int[] submatches = test.matches[n];
    if (submatches.length != GoTestUtils.len(result) * 2) {
      fail(
          String.format(
              "%s %d: expected %d submatches; got %d: %s",
              testName,
              n,
              submatches.length / 2,
              GoTestUtils.len(result),
              test));
    }
    for (int k = 0; k < GoTestUtils.len(result); k++) {
      if (submatches[k * 2] == -1) {
        if (result[k] != null) {
          fail(String.format("%s %d: expected null got %s: %s", testName, n, Arrays.toString(result), test));
        }
        continue;
      }
      byte[] expect = test.submatchBytes(n, k);
      if (!Arrays.equals(expect, result[k])) {
        fail(
            String.format(
                "%s %d: expected %s; got %s: %s",
                testName,
                n,
                GoTestUtils.fromUTF8(expect),
                GoTestUtils.fromUTF8(result[k]),
                test));
      }
    }
  }

  @org.junit.Test
  public void testFindUTF8Submatch() {
    byte[][] result = RE2.compile(test.pat).findUTF8Submatch(test.textUTF8);
    if (test.matches.length == 0 && result == null) {
      // ok
    } else if (test.matches.length == 0 && result != null) {
      fail(String.format("expected no match; got one: %s", test));
    } else if (test.matches.length > 0 && result == null) {
      fail(String.format("expected match; got none: %s", test));
    } else {
      testSubmatchBytes("testFindUTF8Submatch", test, 0, result);
    }
  }

  // (Go: testSubmatchString)
  private void testSubmatch(String testName, Test test, int n, String[] result) {
    int[] submatches = test.matches[n];
    if (submatches.length != GoTestUtils.len(result) * 2) {
      fail(
          String.format(
              "%s %d: expected %d submatches; got %d: %s",
              testName,
              n,
              submatches.length / 2,
              GoTestUtils.len(result),
              test));
    }
    for (int k = 0; k < submatches.length; k += 2) {
      if (submatches[k] == -1) {
        if (result[k / 2] != null && !result[k / 2].isEmpty()) {
          fail(
              String.format(
                  "%s %d: expected null got %s: %s", testName, n, Arrays.toString(result), test));
        }
        continue;
      }
      System.err.println(testName + "  " + test + " " + n + " " + k + " ");
      String expect = test.submatchString(n, k / 2);
      if (!expect.equals(result[k / 2])) {
        fail(String.format("%s %d: expected %s got %s: %s", testName, n, expect, Arrays.toString(result), test));
      }
    }
  }

  // (Go: TestFindStringSubmatch)
  @org.junit.Test
  public void testFindSubmatch() {
    String[] result = RE2.compile(test.pat).findSubmatch(test.text);
    if (test.matches.length == 0 && result == null) {
      // ok
    } else if (test.matches.length == 0 && result != null) {
      fail(String.format("expected no match; got one: %s", test));
    } else if (test.matches.length > 0 && result == null) {
      fail(String.format("expected match; got none: %s", test));
    } else {
      testSubmatch("testFindSubmatch", test, 0, result);
    }
  }

  private void testSubmatchIndices(
      String testName, Test test, int n, int[] result, boolean resultIndicesAreUTF8) {
    int[] expect = test.matches[n];
    if (expect.length != GoTestUtils.len(result)) {
      fail(
          String.format(
              "%s %d: expected %d matches; got %d: %s",
              testName,
              n,
              expect.length / 2,
              GoTestUtils.len(result) / 2,
              test));
      return;
    }
    if (!resultIndicesAreUTF8) {
      result = GoTestUtils.utf16IndicesToUtf8(result, test.text);
    }
    for (int k = 0; k < expect.length; ++k) {
      if (expect[k] != result[k]) {
        fail(
            String.format(
                "%s %d: submatch error: expected %s got %s: %s",
                testName,
                n,
                Arrays.toString(expect),
                Arrays.toString(result),
                test));
      }
    }
  }

  private void testFindSubmatchIndexCommon(
      String testName, Test test, int[] result, boolean resultIndicesAreUTF8) {
    if (test.matches.length == 0 && result == null) {
      // ok
    } else if (test.matches.length == 0 && result != null) {
      fail(String.format("%s: expected no match; got one: %s", testName, test));
    } else if (test.matches.length > 0 && result == null) {
      fail(String.format("%s: expected match; got none: %s", testName, test));
    } else {
      testSubmatchIndices(testName, test, 0, result, resultIndicesAreUTF8);
    }
  }

  @org.junit.Test
  public void testFindUTF8SubmatchIndex() {
    testFindSubmatchIndexCommon(
        "testFindSubmatchIndex",
        test,
        RE2.compile(test.pat).findUTF8SubmatchIndex(test.textUTF8),
        true);
  }

  // (Go: TestFindStringSubmatchIndex)
  @org.junit.Test
  public void testFindSubmatchIndex() {
    testFindSubmatchIndexCommon(
        "testFindStringSubmatchIndex",
        test,
        RE2.compile(test.pat).findSubmatchIndex(test.text),
        false);
  }

  // Now come the monster AllSubmatch cases.

  // (Go: TestFindAllSubmatch)
  @org.junit.Test
  public void testFindAllUTF8Submatch() {
    List<byte[][]> result = RE2.compile(test.pat).findAllUTF8Submatch(test.textUTF8, -1);
    if (test.matches.length == 0 && result == null) {
      // ok
    } else if (test.matches.length == 0 && result != null) {
      fail(String.format("expected no match; got one: %s", test));
    } else if (test.matches.length > 0 && result == null) {
      fail(String.format("expected match; got none: %s", test));
    } else if (test.matches.length != result.size()) {
      fail(
          String.format(
              "expected %d matches; got %d: %s", test.matches.length, result.size(), test));
    } else {
      for (int k = 0; k < test.matches.length; ++k) {
        testSubmatchBytes("testFindAllSubmatch", test, k, result.get(k));
      }
    }
  }

  // (Go: TestFindAllStringSubmatch)
  @org.junit.Test
  public void testFindAllSubmatch() {
    List<String[]> result = RE2.compile(test.pat).findAllSubmatch(test.text, -1);
    if (test.matches.length == 0 && result == null) {
      // ok
    } else if (test.matches.length == 0 && result != null) {
      fail(String.format("expected no match; got one: %s", test));
    } else if (test.matches.length > 0 && result == null) {
      fail(String.format("expected match; got none: %s", test));
    } else if (test.matches.length != result.size()) {
      fail(
          String.format(
              "expected %d matches; got %d: %s", test.matches.length, result.size(), test));
    } else {
      for (int k = 0; k < test.matches.length; ++k) {
        testSubmatch("testFindAllStringSubmatch", test, k, result.get(k));
      }
    }
  }

  // (Go: testFindSubmatchIndex)
  private void testFindAllSubmatchIndexCommon(
      String testName, Test test, List<int[]> result, boolean resultIndicesAreUTF8) {
    if (test.matches.length == 0 && result == null) {
      // ok
    } else if (test.matches.length == 0 && result != null) {
      fail(String.format("%s: expected no match; got one: %s", testName, test));
    } else if (test.matches.length > 0 && result == null) {
      fail(String.format("%s: expected match; got none: %s", testName, test));
    } else if (test.matches.length != result.size()) {
      fail(
          String.format(
              "%s: expected %d matches; got %d: %s",
              testName,
              test.matches.length,
              result.size(),
              test));
    } else {
      for (int k = 0; k < test.matches.length; ++k) {
        testSubmatchIndices(testName, test, k, result.get(k), resultIndicesAreUTF8);
      }
    }
  }

  // (Go: TestFindAllSubmatchIndex)
  @org.junit.Test
  public void testFindAllUTF8SubmatchIndex() {
    testFindAllSubmatchIndexCommon(
        "testFindAllUTF8SubmatchIndex",
        test,
        RE2.compile(test.pat).findAllUTF8SubmatchIndex(test.textUTF8, -1),
        true);
  }

  // (Go: TestFindAllStringSubmatchIndex)
  @org.junit.Test
  public void testFindAllSubmatchIndex() {
    testFindAllSubmatchIndexCommon(
        "testFindAllSubmatchIndex",
        test,
        RE2.compile(test.pat).findAllSubmatchIndex(test.text, -1),
        false);
  }

  // The find_test.go benchmarks are ported to Benchmarks.java.
}
