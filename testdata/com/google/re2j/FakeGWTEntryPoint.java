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