// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.re2j;

import java.io.UnsupportedEncodingException;

// Utilities to make JUnit act a little more like Go's "testing" package.
class GoTestUtils {
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
}
