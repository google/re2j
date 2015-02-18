// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.re2j;

import java.io.IOException;
import java.io.Reader;

/**
 * A simple reader of lines from a UNIX character stream, like
 * java.io.BufferedReader, but doesn't consider '\r' a line terminator.
 *
 * @author adonovan@google.com (Alan Donovan)
 */
class UNIXBufferedReader extends Reader {

  private final Reader r;
  private final char[] buf = new char[4096];
  private int buflen = 0;  // length prefix of |buf| that is filled
  private int inext = 0;  // index in buf of next char

  UNIXBufferedReader(Reader r) {
    super(r);
    this.r = r;
  }

  public String readLine() throws IOException {
    StringBuffer s = null;  // holds '\n'-free gulps of input
    int istart;  // index of first char
    for (;;) {
      // Should we refill the buffer?
      if (inext >= buflen) {
        int n;
        do {
          n = r.read(buf, 0, buf.length);
        } while (n == 0);
        if (n > 0) {
          buflen = n;
          inext = 0;
        }
      }
      // Did we reach end-of-file?
      if (inext >= buflen) {
        return s != null && s.length() > 0
            ? s.toString()
            : null;
      }
      // Did we read a newline?
      int i;
      for (i = inext; i < buflen; i++) {
        if (buf[i] == '\n') {
          istart = inext;
          inext = i;
          String str;
          if (s == null) {
            str = new String(buf, istart, i - istart);
          } else {
            s.append(buf, istart, i - istart);
            str = s.toString();
          }
          inext++;
          return str;
        }
      }
      istart = inext;
      inext = i;
      if (s == null) {
        s = new StringBuffer(80);
      }
      s.append(buf, istart, i - istart);
    }
  }

  @Override public void close() throws IOException {
    r.close();
  }

  // Unimplemented:

  @Override public int read(char buf[], int off, int len) throws IOException {
    throw new UnsupportedOperationException();
  }
  @Override public int read() throws IOException {
    throw new UnsupportedOperationException();
  }
  @Override public long skip(long n) throws IOException {
    throw new UnsupportedOperationException();
  }
  @Override public boolean ready() throws IOException {
    throw new UnsupportedOperationException();
  }
  @Override public boolean markSupported() {
    throw new UnsupportedOperationException();
  }
  @Override public void mark(int readAheadLimit) throws IOException {
    throw new UnsupportedOperationException();
  }
  @Override public void reset() throws IOException {
    throw new UnsupportedOperationException();
  }

}
