// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.re2j;

import java.io.UnsupportedEncodingException;
import junit.framework.TestCase;

// Utilities to make JUnit act a little more like Go's "testing" package.
// In particular, don't rely on exceptions for error reporting, since they
// cripple data-driven tests.
class GoTestCase extends TestCase {

  private int nerrors = 0;

  // Format an error and report it.
  protected void errorf(String format, Object ... args) {
    System.err.println(String.format(format, args));
    nerrors++;
  }

  @Override
  protected void tearDown() throws Exception {
    if (nerrors > 0) {
      throw new AssertionError("There were " + nerrors + " errors");
    }
    super.tearDown();
  }

  // Other utilities:

  protected static int len(Object[] array) {
    return array == null ? 0 : array.length;
  }
  protected static int len(int[] array) {
    return array == null ? 0 : array.length;
  }
  protected static int len(byte[] array) {
    return array == null ? 0 : array.length;
  }

  protected static byte[] utf8(String s) {
    try {
      return s.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("can't happen");
    }
  }

  // Beware: logically this operation can fail, but Java doesn't detect it.
  protected static String fromUTF8(byte[] b) {
    try {
    return new String(b, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("can't happen");
    }
  }

  // Convert |idx16|, which are Java (UTF-16) string indices, into the
  // corresponding indices in the UTF-8 encoding of |text|.
  //
  // TODO(adonovan): eliminate duplication w.r.t. ExecTest.
  protected static int[] utf16IndicesToUtf8(int[] idx16, String text) {
    try {
      int[] idx8 = new int[idx16.length];
      for (int i = 0; i < idx16.length; ++i) {
        idx8[i] = idx16[i] == -1
            ? -1
            : text.substring(0, idx16[i]).getBytes("UTF-8").length;  // yikes
      }
      return idx8;
    } catch (java.io.UnsupportedEncodingException e) {
      throw new IllegalStateException(e);
    }
  }

}
