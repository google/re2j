/*
 * Copyright (c) 2022 The Go Authors. All rights reserved.
 *
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
package com.google.re2j.benchmark;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class BenchmarkUtils {

  // readResourceFile reads the contents of the Java resource file at the given path.
  public static byte[] readResourceFile(String name) {
    try (InputStream in = BenchmarkUtils.class.getClassLoader().getResourceAsStream(name);
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      int read;
      while ((read = in.read()) > -1) {
        out.write(read);
      }
      return out.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private BenchmarkUtils() {}
}
