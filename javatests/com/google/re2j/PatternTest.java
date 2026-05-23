/*
 * Copyright (c) 2020 The Go Authors. All rights reserved.
 *
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
package com.google.re2j;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableMap;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.Collections;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * This class checks that the behaviour of Pattern and JDK's Pattern are same, and we expect them
 * that way too.
 *
 * @author afrozm@google.com (Afroz Mohiuddin)
 */
@RunWith(JUnit4.class)
public class PatternTest {

  @Test
  public void testCompile() {
    Pattern p = Pattern.compile("abc");
    assertEquals("abc", p.pattern());
    assertEquals(0, p.flags());
  }

  @Test
  public void testCompileExceptionWithDuplicateGroups() {
    try {
      Pattern.compile("(?P<any>.*)(?P<any>.*");
      fail();
    } catch (PatternSyntaxException e) {
      assertEquals("error parsing regexp: duplicate capture group name: `any`", e.getMessage());
    }
  }

  @Test
  public void testToString() {
    Pattern p = Pattern.compile("abc");
    assertEquals("abc", p.toString());
  }

  @Test
  public void testCompileFlags() {
    Pattern p = Pattern.compile("abc", 5);
    assertEquals("abc", p.pattern());
    assertEquals(5, p.flags());
  }

  @Test
  public void testSyntaxError() {
    try {
      Pattern.compile("abc(");
      fail("should have thrown");
    } catch (PatternSyntaxException e) {
      assertEquals(-1, e.getIndex());
      assertNotSame("", e.getDescription());
      assertNotSame("", e.getMessage());
      assertEquals("abc(", e.getPattern());
    }
  }

  @Test
  public void testMatchesNoFlags() {
    ApiTestUtils.testMatches("ab+c", "abbbc", "cbbba");
    ApiTestUtils.testMatches("ab.*c", "abxyzc", "ab\nxyzc");
    ApiTestUtils.testMatches("^ab.*c$", "abc", "xyz\nabc\ndef");

    // Test quoted codepoints that require a surrogate pair. See https://github.com/google/re2j/issues/123.
    String source = new StringBuilder().appendCodePoint(110781).toString();
    ApiTestUtils.testMatches(source, source, "blah");
    ApiTestUtils.testMatches("\\Q" + source + "\\E", source, "blah");
  }

  @Test
  public void testMatchesWithFlags() {
    ApiTestUtils.testMatchesRE2("ab+c", 0, "abbbc", "cbba");
    ApiTestUtils.testMatchesRE2("ab+c", Pattern.CASE_INSENSITIVE, "abBBc", "cbbba");
    ApiTestUtils.testMatchesRE2("ab.*c", 0, "abxyzc", "ab\nxyzc");
    ApiTestUtils.testMatchesRE2("ab.*c", Pattern.DOTALL, "ab\nxyzc", "aB\nxyzC");
    ApiTestUtils.testMatchesRE2(
        "ab.*c", Pattern.DOTALL | Pattern.CASE_INSENSITIVE, "aB\nxyzC", "z");
    ApiTestUtils.testMatchesRE2("^ab.*c$", 0, "abc", "xyz\nabc\ndef");

    ApiTestUtils.testMatchesRE2("^ab.*c$", Pattern.MULTILINE, "abc", "xyz\nabc\ndef");
    ApiTestUtils.testMatchesRE2("^ab.*c$", Pattern.MULTILINE, "abc", "");
    ApiTestUtils.testMatchesRE2("^ab.*c$", Pattern.DOTALL | Pattern.MULTILINE, "ab\nc", "AB\nc");
    ApiTestUtils.testMatchesRE2(
        "^ab.*c$", Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE, "AB\nc", "z");
  }

  private void testFind(String regexp, int flag, String match, String nonMatch) {
    assertTrue(Pattern.compile(regexp, flag).matcher(match).find());
    assertFalse(Pattern.compile(regexp, flag).matcher(nonMatch).find());
  }

  @Test
  public void testFind() {
    testFind("ab+c", 0, "xxabbbc", "cbbba");
    testFind("ab+c", Pattern.CASE_INSENSITIVE, "abBBc", "cbbba");
    testFind("ab.*c", 0, "xxabxyzc", "ab\nxyzc");
    testFind("ab.*c", Pattern.DOTALL, "ab\nxyzc", "aB\nxyzC");
    testFind("ab.*c", Pattern.DOTALL | Pattern.CASE_INSENSITIVE, "xaB\nxyzCz", "z");
    testFind("^ab.*c$", 0, "abc", "xyz\nabc\ndef");
    testFind("^ab.*c$", Pattern.MULTILINE, "xyz\nabc\ndef", "xyz\nab\nc\ndef");
    testFind("^ab.*c$", Pattern.DOTALL | Pattern.MULTILINE, "xyz\nab\nc\ndef", "xyz\nAB\nc\ndef");
    testFind(
        "^ab.*c$",
        Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE,
        "xyz\nAB\nc\ndef",
        "z");
  }

  @Test
  public void testSplit() {
    ApiTestUtils.testSplit("/", "abcde", new String[] {"abcde"});
    ApiTestUtils.testSplit("/", "a/b/cc//d/e//", new String[] {"a", "b", "cc", "", "d", "e"});
    ApiTestUtils.testSplit("/", "a/b/cc//d/e//", 3, new String[] {"a", "b", "cc//d/e//"});
    ApiTestUtils.testSplit("/", "a/b/cc//d/e//", 4, new String[] {"a", "b", "cc", "/d/e//"});
    ApiTestUtils.testSplit("/", "a/b/cc//d/e//", 5, new String[] {"a", "b", "cc", "", "d/e//"});
    ApiTestUtils.testSplit("/", "a/b/cc//d/e//", 6, new String[] {"a", "b", "cc", "", "d", "e//"});
    ApiTestUtils.testSplit(
        "/", "a/b/cc//d/e//", 7, new String[] {"a", "b", "cc", "", "d", "e", "/"});
    ApiTestUtils.testSplit(
        "/", "a/b/cc//d/e//", 8, new String[] {"a", "b", "cc", "", "d", "e", "", ""});
    ApiTestUtils.testSplit(
        "/", "a/b/cc//d/e//", 9, new String[] {"a", "b", "cc", "", "d", "e", "", ""});

    // The tests below are listed at
    // http://docs.oracle.com/javase/1.5.0/docs/api/java/util/regex/Pattern.html#split(java.lang.CharSequence, int)

    String s = "boo:and:foo";

    ApiTestUtils.testSplit(":", s, 2, new String[] {"boo", "and:foo"});
    ApiTestUtils.testSplit(":", s, 5, new String[] {"boo", "and", "foo"});
    ApiTestUtils.testSplit(":", s, -2, new String[] {"boo", "and", "foo"});
    ApiTestUtils.testSplit("o", s, 5, new String[] {"b", "", ":and:f", "", ""});
    ApiTestUtils.testSplit("o", s, -2, new String[] {"b", "", ":and:f", "", ""});
    ApiTestUtils.testSplit("o", s, 0, new String[] {"b", "", ":and:f"});
    ApiTestUtils.testSplit("o", s, new String[] {"b", "", ":and:f"});

    // From https://github.com/google/re2j/issues/131.
    ApiTestUtils.testSplit("x*", "foo", new String[] {"f", "o", "o"});
    ApiTestUtils.testSplit("x*", "foo", 1, new String[] {"foo"});
    ApiTestUtils.testSplit("x*", "f", 2, new String[] {"f", ""});
    ApiTestUtils.testSplit(":", ":a::b", new String[] {"", "a", "", "b"});
  }

  @Test
  public void testProgramSize() {
    ApiTestUtils.testProgramSize("", 3);
    ApiTestUtils.testProgramSize("a", 3);
    ApiTestUtils.testProgramSize("^", 3);
    ApiTestUtils.testProgramSize("^$", 4);
    ApiTestUtils.testProgramSize("a+b", 5);
    ApiTestUtils.testProgramSize("a+b?", 6);
    ApiTestUtils.testProgramSize("(a+b)", 7);
    ApiTestUtils.testProgramSize("a+b.*", 7);
    ApiTestUtils.testProgramSize("(a+b?)", 8);
  }

  @Test
  public void testGroupCount() {
    ApiTestUtils.testGroupCount("(.*)ab(.*)a", 2);
    ApiTestUtils.testGroupCount("(.*)(ab)(.*)a", 3);
    ApiTestUtils.testGroupCount("(.*)((a)b)(.*)a", 4);
    ApiTestUtils.testGroupCount("(.*)(\\(ab)(.*)a", 3);
    ApiTestUtils.testGroupCount("(.*)(\\(a\\)b)(.*)a", 3);
  }

  @Test
  public void testNamedGroups() {
    assertNamedGroupsEquals(ImmutableMap.of("foo", 1), "(?P<foo>\\d{2})");
    assertNamedGroupsEquals(Collections.<String, Integer>emptyMap(), "\\d{2}");
    assertNamedGroupsEquals(Collections.<String, Integer>emptyMap(), "hello");
    assertNamedGroupsEquals(Collections.<String, Integer>emptyMap(), "(.*)");
    assertNamedGroupsEquals(ImmutableMap.of("any", 1), "(?P<any>.*)");
    assertNamedGroupsEquals(ImmutableMap.of("foo", 1, "bar", 2), "(?P<foo>.*)(?P<bar>.*)");
  }

  private static void assertNamedGroupsEquals(Map<String, Integer> expected, String pattern) {
    assertEquals(expected, Pattern.compile(pattern).namedGroups());
  }

  // See https://github.com/google/re2j/issues/93.
  @Test
  public void testIssue93() {
    Pattern p1 = Pattern.compile("(a.*?c)|a.*?b");
    Pattern p2 = Pattern.compile("a.*?c|a.*?b");

    Matcher m1 = p1.matcher("abc");
    m1.find();
    Matcher m2 = p2.matcher("abc");
    m2.find();

    assertThat(m2.group()).isEqualTo(m1.group());
  }

  @Test
  public void testQuote() {
    ApiTestUtils.testMatchesRE2(Pattern.quote("ab+c"), 0, "ab+c", "abc");
  }

  private Pattern reserialize(Pattern object) {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try {
      ObjectOutputStream out = new ObjectOutputStream(bytes);
      out.writeObject(object);
      ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()));
      return (Pattern) in.readObject();
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  private void assertSerializes(Pattern p) {
    Pattern reserialized = reserialize(p);
    assertEquals(p.pattern(), reserialized.pattern());
    assertEquals(p.flags(), reserialized.flags());
  }

  @Test
  public void testSerialize() {
    assertSerializes(Pattern.compile("ab+c"));
    assertSerializes(Pattern.compile("^ab.*c$", Pattern.DOTALL | Pattern.MULTILINE));
    assertFalse(reserialize(Pattern.compile("abc")).matcher("def").find());
  }

  @Test
  public void testEquals() {
    Pattern pattern1 = Pattern.compile("abc");
    Pattern pattern2 = Pattern.compile("abc");
    Pattern pattern3 = Pattern.compile("def");
    Pattern pattern4 = Pattern.compile("abc", Pattern.CASE_INSENSITIVE);
    assertThat(pattern1).isEqualTo(pattern2);
    assertThat(pattern1).isNotEqualTo(pattern3);
    assertThat(pattern1.hashCode()).isEqualTo(pattern2.hashCode());
    assertThat(pattern1).isNotEqualTo(pattern4);
  }

  @Test
  public void testUnicodeWordBoundary() {
    final String pattern = "l\\p{L}*\\b";
    final String text = "l\u00E0";
    {
      final java.util.regex.Matcher matcher =
          java.util.regex.Pattern.compile(pattern).matcher(text);
      assertEquals(true, matcher.find());
      assertEquals("l\u00E0", text.substring(matcher.start(), matcher.end()));
    }
    {
      final com.google.re2j.Matcher matcher =
          com.google.re2j.Pattern.compile(pattern).matcher(text);
      assertEquals(true, matcher.find());
      assertEquals("l\u00E0", text.substring(matcher.start(), matcher.end()));
    }
  }

  @Test
  public void testUnicodeWordBoundary2() {
    final String pattern = "d\u00E9\\p{L}*\\b";
    {
      final String text = "d\u00E9s";
      {
        final java.util.regex.Matcher matcher =
            java.util.regex.Pattern.compile(pattern).matcher(text);
        assertEquals(true, matcher.find());
        assertEquals("d\u00E9s", text.substring(matcher.start(), matcher.end()));
      }
      {
        final com.google.re2j.Matcher matcher =
            com.google.re2j.Pattern.compile(pattern).matcher(text);
        assertEquals(true, matcher.find());
        assertEquals("d\u00E9s", text.substring(matcher.start(), matcher.end()));
      }
    }
    {
      final String text = "d\u00E9";
      {
        final java.util.regex.Matcher matcher =
            java.util.regex.Pattern.compile(pattern).matcher(text);
        assertEquals(true, matcher.find());
        assertEquals("d\u00E9", text.substring(matcher.start(), matcher.end()));
      }
      {
        final com.google.re2j.Matcher matcher =
            com.google.re2j.Pattern.compile(pattern).matcher(text);
        assertEquals(true, matcher.find());
        assertEquals("d\u00E9", text.substring(matcher.start(), matcher.end()));
      }
    }
  }
}
