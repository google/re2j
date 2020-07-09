/*
 * Copyright (c) 2020 The Go Authors. All rights reserved.
 *
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
// Original Go source here:
// http://golang.org/src/pkg/strconv/quote.go

// While this is a port of production Go source, it is currently
// only used by ExecTest, which is why it appears beneath javatests/.

package com.google.re2j;

class Strconv {

  // unquoteChar decodes the first character or byte in the escaped
  // string or character literal represented by the Go literal encoded
  // in UTF-16 in s.
  //
  // On success, it advances the UTF-16 cursor i[0] (an in/out
  // parameter) past the consumed codes and returns the decoded Unicode
  // code point or byte value.  On failure, it throws
  // IllegalArgumentException or StringIndexOutOfBoundsException
  //
  // |quote| specifies the type of literal being parsed
  // and therefore which escaped quote character is permitted.
  // If set to a single quote, it permits the sequence \' and disallows
  // unescaped '.
  // If set to a double quote, it permits \" and disallows unescaped ".
  // If set to zero, it does not permit either escape and allows both
  // quote characters to appear unescaped.
  private static int unquoteChar(String s, int[] i, char quote) {
    int c = s.codePointAt(i[0]);
    i[0] = s.offsetByCodePoints(i[0], 1); // (throws if falls off end)

    // easy cases
    if (c == quote && (quote == '\'' || quote == '"')) {
      throw new IllegalArgumentException("unescaped quotation mark in literal");
    }
    if (c != '\\') {
      return c;
    }

    // hard case: c is backslash
    c = s.codePointAt(i[0]);
    i[0] = s.offsetByCodePoints(i[0], 1); // (throws if falls off end)

    switch (c) {
      case 'a':
        return 0x07;
      case 'b':
        return '\b';
      case 'f':
        return '\f';
      case 'n':
        return '\n';
      case 'r':
        return '\r';
      case 't':
        return '\t';
      case 'v':
        return 0x0B;
      case 'x':
      case 'u':
      case 'U':
        {
          int n = 0;
          switch (c) {
            case 'x':
              n = 2;
              break;
            case 'u':
              n = 4;
              break;
            case 'U':
              n = 8;
              break;
          }
          int v = 0;
          for (int j = 0; j < n; j++) {
            int d = s.codePointAt(i[0]);
            i[0] = s.offsetByCodePoints(i[0], 1); // (throws if falls off end)

            int x = Utils.unhex(d);
            if (x == -1) {
              throw new IllegalArgumentException("not a hex char: " + d);
            }
            v = (v << 4) | x;
          }
          if (c == 'x') {
            return v;
          }
          if (v > Unicode.MAX_RUNE) {
            throw new IllegalArgumentException("Unicode code point out of range");
          }
          return v;
        }
      case '0':
      case '1':
      case '2':
      case '3':
      case '4':
      case '5':
      case '6':
      case '7':
        {
          int v = c - '0';
          for (int j = 0; j < 2; j++) { // one digit already; two more
            int d = s.codePointAt(i[0]);
            i[0] = s.offsetByCodePoints(i[0], 1); // (throws if falls off end)

            int x = d - '0';
            if (x < 0 || x > 7) {
              throw new IllegalArgumentException("illegal octal digit");
            }
            v = (v << 3) | x;
          }
          if (v > 255) {
            throw new IllegalArgumentException("octal value out of range");
          }
          return v;
        }
      case '\\':
        return '\\';
      case '\'':
      case '"':
        if (c != quote) {
          throw new IllegalArgumentException("unnecessary backslash escape");
        }
        return c;
      default:
        throw new IllegalArgumentException("unexpected character");
    }
  }

  // Unquote interprets s as a single-quoted, double-quoted,
  // or backquoted Go string literal, returning the string value
  // that s quotes.  (If s is single-quoted, it would be a Go
  // character literal; Unquote returns the corresponding
  // one-character string.)
  static String unquote(String s) throws IllegalArgumentException {
    int n = s.length();
    if (n < 2) {
      throw new IllegalArgumentException("too short");
    }
    char quote = s.charAt(0);
    if (quote != s.charAt(n - 1)) {
      throw new IllegalArgumentException("quotes don't match");
    }
    s = s.substring(1, n - 1);
    if (quote == '`') {
      if (s.indexOf('`') >= 0) {
        throw new IllegalArgumentException("backquoted string contains '`'");
      }
      return s;
    }
    if (quote != '"' && quote != '\'') {
      throw new IllegalArgumentException("invalid quotation mark");
    }
    if (s.indexOf('\n') >= 0) {
      throw new IllegalArgumentException("multiline string literal");
    }
    // Is it trivial?  Avoid allocation.
    if (s.indexOf('\\') < 0 && s.indexOf(quote) < 0) {
      if (quote == '"'
          || // "abc"
          s.codePointCount(0, s.length()) == 1) { // 'a'
        // if s == "\\" then this return is wrong.
        return s;
      }
    }

    int i[] = {0}; // UTF-16 index, an in/out-parameter of unquoteChar.
    StringBuilder buf = new StringBuilder();
    int len = s.length();
    while (i[0] < len) {
      buf.appendCodePoint(unquoteChar(s, i, quote));
      if (quote == '\'' && i[0] != len) {
        throw new IllegalArgumentException("single-quotation must be one char");
      }
    }

    return buf.toString();
  }

  private Strconv() {} // uninstantiable
}
