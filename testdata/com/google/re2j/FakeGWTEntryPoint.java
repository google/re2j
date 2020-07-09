/*
 * Copyright (c) 2020 The Go Authors. All rights reserved.
 *
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
package com.google.re2j;

import com.google.gwt.core.client.EntryPoint;
import com.google.re2j.Pattern;

class FakeGWTEntryPoint implements EntryPoint {
  @Override
  public void onModuleLoad() {
    Pattern p = Pattern.compile("foo");
    p.matcher("bar");
  }
}
