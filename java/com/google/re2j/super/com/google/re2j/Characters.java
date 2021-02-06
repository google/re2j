/*
 * Copyright (c) 2021 The Go Authors. All rights reserved.
 *
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
package com.google.re2j;

/** GWT supersource for {@link Character#toLowerCase}. */
final class Characters {
  private Characters() {}

  static int toLowerCase(int codePoint) {
    return new String(new int[] {codePoint}, 0, 1).toLowerCase().codePointAt(0);
  }

  static int toUpperCase(int codePoint) {
    return new String(new int[] {codePoint}, 0, 1).toUpperCase().codePointAt(0);
  }
}
