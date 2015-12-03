// Copyright 2011 The Go Authors.  All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

// Original Go source here:
// http://code.google.com/p/go/source/browse/src/pkg/regexp/syntax/prog_test.go

package com.google.re2j;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ProgTest {
  private static final String[][] COMPILE_TESTS = {
    {"\\Aa",
     "0       fail\n" +
     "1*@     empty 4 -> 2\n" +
     "2       byte1 97 -> 3\n" +
     "3       match\n"
    },
    {"\\A[A-M][n-z]",
     "0       fail\n" +
     "1*@     empty 4 -> 2\n" +
     "2       byte [65,77] -> 3\n" +
     "3       byte [110,122] -> 4\n" +
     "4       match\n"
    },
    {"",
     "0       fail\n" +
     "1*      nop -> 2\n" +
     "2       match\n" +
     "3       byte [0,255] -> 4\n" +
     "4@      alt -> 1, 3\n"
    },
    {"\\Aa?",
     "0       fail\n" +
     "1*@     empty 4 -> 3\n" +
     "2       byte1 97 -> 4\n" +
     "3       alt -> 2, 4\n" +
     "4       match\n"
    },
    {"\\Aa??",
     "0       fail\n" +
     "1*@     empty 4 -> 3\n" +
     "2       byte1 97 -> 4\n" +
     "3       alt -> 4, 2\n" +
     "4       match\n"
    },
    {"\\Aa+",
     "0       fail\n" +
     "1*@     empty 4 -> 2\n" +
     "2       byte1 97 -> 3\n" +
     "3       alt -> 2, 4\n" +
     "4       match\n"
    },
    {"\\Aa+?",
     "0       fail\n" +
     "1*@     empty 4 -> 2\n" +
     "2       byte1 97 -> 3\n" +
     "3       alt -> 4, 2\n" +
     "4       match\n"
    },
    {"\\Aa*",
     "0       fail\n" +
     "1*@     empty 4 -> 3\n" +
     "2       byte1 97 -> 3\n" +
     "3       alt -> 2, 4\n" +
     "4       match\n"
    },
    {"\\Aa*?",
     "0       fail\n" +
     "1*@     empty 4 -> 3\n" +
     "2       byte1 97 -> 3\n" +
     "3       alt -> 4, 2\n" +
     "4       match\n"
    },
    {"\\Aa+b+",
     "0       fail\n" +
     "1*@     empty 4 -> 2\n" +
     "2       byte1 97 -> 3\n" +
     "3       alt -> 2, 4\n" +
     "4       byte1 98 -> 5\n" +
     "5       alt -> 4, 6\n" +
     "6       match\n"
    },
    {"\\A(a+)(b+)",
     "0       fail\n" +
     "1*@     empty 4 -> 2\n" +
     "2       cap 2 -> 3\n" +
     "3       byte1 97 -> 4\n" +
     "4       alt -> 3, 5\n" +
     "5       cap 3 -> 6\n" +
     "6       cap 4 -> 7\n" +
     "7       byte1 98 -> 8\n" +
     "8       alt -> 7, 9\n" +
     "9       cap 5 -> 10\n" +
     "10      match\n"
    },
    {"\\Aa+|\\Ab+",
     "0       fail\n" +
     "1*@     empty 4 -> 6\n" +
     "2       byte1 97 -> 3\n" +
     "3       alt -> 2, 7\n" +
     "4       byte1 98 -> 5\n" +
     "5       alt -> 4, 7\n" +
     "6       alt -> 2, 4\n" +
     "7       match\n"
    },
    {"\\AA[Aa]",
     "0       fail\n" +
     "1*@     empty 4 -> 2\n" +
     "2       byte1 65 -> 5\n" +
     "3       byte1 65 -> 6\n" +
     "4       byte1 97 -> 6\n" +
     "5       alt -> 3, 4\n" +
     "6       match\n"
    },
    {"\\A(?:(?:^).)",
     "0       fail\n" +
     "1*@     empty 4 -> 2\n" +
     "2       empty 4 -> 31\n" +
     "3       byte [0,9] -> 32\n" +
     "4       byte [11,127] -> 32\n" +
     "5       alt -> 3, 4\n" +
     "6       byte [194,223] -> 7\n" +
     "7       byte [128,191] -> 32\n" +
     "8       alt -> 5, 6\n" +
     "9       byte1 224 -> 10\n" +
     "10      byte [160,191] -> 11\n" +
     "11      byte [128,191] -> 32\n" +
     "12      alt -> 8, 9\n" +
     "13      byte [225,239] -> 14\n" +
     "14      byte [128,191] -> 15\n" +
     "15      byte [128,191] -> 32\n" +
     "16      alt -> 12, 13\n" +
     "17      byte1 240 -> 18\n" +
     "18      byte [144,191] -> 19\n" +
     "19      byte [128,191] -> 20\n" +
     "20      byte [128,191] -> 32\n" +
     "21      alt -> 16, 17\n" +
     "22      byte [241,243] -> 23\n" +
     "23      byte [128,191] -> 24\n" +
     "24      byte [128,191] -> 25\n" +
     "25      byte [128,191] -> 32\n" +
     "26      alt -> 21, 22\n" +
     "27      byte1 244 -> 28\n" +
     "28      byte [128,143] -> 29\n" +
     "29      byte [128,191] -> 30\n" +
     "30      byte [128,191] -> 32\n" +
     "31      alt -> 26, 27\n" +
     "32      match\n"
    },
  };

  private static final String[][] COMPILE_REVERSED_TESTS = {
    {"\\Aa",
     "0       fail\n" +
     "1       empty 8 -> 3\n" +
     "2*      byte1 97 -> 1\n" +
     "3       match\n" +
     "4       byte [0,255] -> 5\n" +
     "5@      alt -> 2, 4\n"
    },
    {"a\\z",
     "0       fail\n" +
     "1       byte1 97 -> 3\n" +
     "2*@     empty 4 -> 1\n" +
     "3       match\n"
    },
    {".\\z",
     "0       fail\n" +
     "1       byte [0,9] -> 31\n" +
     "2       byte [11,127] -> 31\n" +
     "3       alt -> 1, 2\n" +
     "4       byte [194,223] -> 31\n" +
     "5       byte [128,191] -> 4\n" +
     "6       alt -> 3, 5\n" +
     "7       byte1 224 -> 31\n" +
     "8       byte [160,191] -> 7\n" +
     "9       byte [128,191] -> 8\n" +
     "10      alt -> 6, 9\n" +
     "11      byte [225,239] -> 31\n" +
     "12      byte [128,191] -> 11\n" +
     "13      byte [128,191] -> 12\n" +
     "14      alt -> 10, 13\n" +
     "15      byte1 240 -> 31\n" +
     "16      byte [144,191] -> 15\n" +
     "17      byte [128,191] -> 16\n" +
     "18      byte [128,191] -> 17\n" +
     "19      alt -> 14, 18\n" +
     "20      byte [241,243] -> 31\n" +
     "21      byte [128,191] -> 20\n" +
     "22      byte [128,191] -> 21\n" +
     "23      byte [128,191] -> 22\n" +
     "24      alt -> 19, 23\n" +
     "25      byte1 244 -> 31\n" +
     "26      byte [128,143] -> 25\n" +
     "27      byte [128,191] -> 26\n" +
     "28      byte [128,191] -> 27\n" +
     "29      alt -> 24, 28\n" +
     "30*@    empty 4 -> 29\n" +
     "31      match\n"
    }
  };

  @Test
  public void testCompile() throws Exception {
    for (String[] test : COMPILE_TESTS) {
      Regexp re = Parser.parse(test[0], RE2.PERL);
      Prog p = Compiler.compileRegexp(re, false);
      String s = p.toString();
      assertEquals("compiled: " + test[0], test[1], s);
    }
  }

  @Test
  public void testCompileReversed() throws Exception {
    for (String[] test : COMPILE_REVERSED_TESTS) {
      Regexp re = Parser.parse(test[0], RE2.PERL);
      Prog p = Compiler.compileRegexp(re, true);
      String s = p.toString();
      assertEquals("compiled: " + test[0], test[1], s);
    }
  }
}
