// Copyright 2011 The Go Authors.  All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

// Original Go source here:
// http://code.google.com/p/go/source/browse/src/pkg/regexp/syntax/prog_test.go

package com.google.re2j;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ProgTest {
  private static final String[][] COMPILE_TESTS = {
    {"a",
     "0       fail\n" +
     "1*      byte1 97 -> 2\n" +
     "2       match\n"
    },
    {"[A-M][n-z]",
     "0       fail\n" +
     "1*      byte [65,77] -> 2\n" +
     "2       byte [110,122] -> 3\n" +
     "3       match\n"
    },
    {"",
     "0       fail\n" +
     "1*      nop -> 2\n" +
     "2       match\n"
    },
    {"a?",
     "0       fail\n" +
     "1       byte1 97 -> 3\n" +
     "2*      alt -> 1, 3\n" +
     "3       match\n"
    },
    {"a??",
     "0       fail\n" +
     "1       byte1 97 -> 3\n" +
     "2*      alt -> 3, 1\n" +
     "3       match\n"
    },
    {"a+",
     "0       fail\n" +
     "1*      byte1 97 -> 2\n" +
     "2       alt -> 1, 3\n" +
     "3       match\n"
    },
    {"a+?",
     "0       fail\n" +
     "1*      byte1 97 -> 2\n" +
     "2       alt -> 3, 1\n" +
     "3       match\n"
    },
    {"a*",
     "0       fail\n" +
     "1       byte1 97 -> 2\n" +
     "2*      alt -> 1, 3\n" +
     "3       match\n"
    },
    {"a*?",
     "0       fail\n" +
     "1       byte1 97 -> 2\n" +
     "2*      alt -> 3, 1\n" +
     "3       match\n"
    },
    {"a+b+",
     "0       fail\n" +
     "1*      byte1 97 -> 2\n" +
     "2       alt -> 1, 3\n" +
     "3       byte1 98 -> 4\n" +
     "4       alt -> 3, 5\n" +
     "5       match\n"
    },
    {"(a+)(b+)",
     "0       fail\n" +
     "1*      cap 2 -> 2\n" +
     "2       byte1 97 -> 3\n" +
     "3       alt -> 2, 4\n" +
     "4       cap 3 -> 5\n" +
     "5       cap 4 -> 6\n" +
     "6       byte1 98 -> 7\n" +
     "7       alt -> 6, 8\n" +
     "8       cap 5 -> 9\n" +
     "9       match\n"
    },
    {"a+|b+",
     "0       fail\n" +
     "1       byte1 97 -> 2\n" +
     "2       alt -> 1, 6\n" +
     "3       byte1 98 -> 4\n" +
     "4       alt -> 3, 6\n" +
     "5*      alt -> 1, 3\n" +
     "6       match\n"
    },
    {"A[Aa]",
     "0       fail\n" +
     "1*      byte1 65 -> 4\n" +
     "2       byte1 65 -> 5\n" +
     "3       byte1 97 -> 5\n" +
     "4       alt -> 2, 3\n" +
     "5       match\n"
    },
    {"(?:(?:^).)",
     "0       fail\n" +
     "1*      empty 4 -> 30\n" +
     "2       byte [0,9] -> 31\n" +
     "3       byte [11,127] -> 31\n" +
     "4       alt -> 2, 3\n" +
     "5       byte [194,223] -> 6\n" +
     "6       byte [128,191] -> 31\n" +
     "7       alt -> 4, 5\n" +
     "8       byte1 224 -> 9\n" +
     "9       byte [160,191] -> 10\n" +
     "10      byte [128,191] -> 31\n" +
     "11      alt -> 7, 8\n" +
     "12      byte [225,239] -> 13\n" +
     "13      byte [128,191] -> 14\n" +
     "14      byte [128,191] -> 31\n" +
     "15      alt -> 11, 12\n" +
     "16      byte1 240 -> 17\n" +
     "17      byte [144,191] -> 18\n" +
     "18      byte [128,191] -> 19\n" +
     "19      byte [128,191] -> 31\n" +
     "20      alt -> 15, 16\n" +
     "21      byte [241,243] -> 22\n" +
     "22      byte [128,191] -> 23\n" +
     "23      byte [128,191] -> 24\n" +
     "24      byte [128,191] -> 31\n" +
     "25      alt -> 20, 21\n" +
     "26      byte1 244 -> 27\n" +
     "27      byte [128,143] -> 28\n" +
     "28      byte [128,191] -> 29\n" +
     "29      byte [128,191] -> 31\n" +
     "30      alt -> 25, 26\n" +
     "31      match\n"
    },
  };

  @Test
  public void testCompile() throws Exception {
    for (String[] test : COMPILE_TESTS) {
      Regexp re = Parser.parse(test[0], RE2.PERL);
      Prog p = Compiler.compileRegexp(re);
      String s = p.toString();
      assertEquals("compiled: " + test[0], test[1], s);
    }
  }
}
