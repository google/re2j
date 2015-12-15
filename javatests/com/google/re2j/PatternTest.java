// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.re2j;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static io.airlift.slice.Slices.utf8Slice;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;

/**
 * This class checks that the behaviour of Pattern and JDK's Pattern are same, and we expect them
 * that way too.
 *
 * @author afrozm@google.com (Afroz Mohiuddin)
 */
@RunWith(Enclosed.class)
public class PatternTest {

  public static class DFA extends PatternTestBase {
    public DFA() {
      super(RUN_WITH_DFA);
    }
  }

  public static class NFA extends PatternTestBase {
    public NFA() {
      super(RUN_WITH_NFA);
    }
  }

  public static abstract class PatternTestBase extends ApiTest {

    protected PatternTestBase(Options options) {
      super(options);
    }

    @Test
    public void testCompile() {
      Pattern p = Pattern.compile("abc", options);
      assertEquals("abc", p.pattern());
      assertEquals(0, p.flags());
    }

    @Test
    public void testToString() {
      Pattern p = Pattern.compile("abc", options);
      assertEquals("abc", p.toString());
    }

    @Test
    public void testCompileFlags() {
      Pattern p = Pattern.compile("abc", 5, options);
      assertEquals("abc", p.pattern());
      assertEquals(5, p.flags());
    }

    @Test
    public void testSyntaxError() {
      boolean caught = false;
      try {
        Pattern.compile("abc(", options);
      } catch (PatternSyntaxException e) {
        assertEquals(-1, e.getIndex());
        assertNotSame("", e.getDescription());
        assertNotSame("", e.getMessage());
        assertEquals("abc(", e.getPattern());
        caught = true;
      }
      assertEquals(true, caught);
    }

    @Test
    public void testMatchesNoFlags() {
      testMatches("ab+c", "abbbc", "cbbba");
      testMatches("ab.*c", "abxyzc", "ab\nxyzc");
      testMatches("^ab.*c$", "abc", "xyz\nabc\ndef");
    }

    @Test
    public void testMatchesWithFlags() {
      testMatchesRE2("ab+c", 0, "abbbc", "cbba");
      testMatchesRE2("ab+c", Pattern.CASE_INSENSITIVE, "abBBc",
          "cbbba");
      testMatchesRE2("ab.*c", 0, "abxyzc", "ab\nxyzc");
      testMatchesRE2("ab.*c", Pattern.DOTALL, "ab\nxyzc",
          "aB\nxyzC");
      testMatchesRE2("ab.*c",
          Pattern.DOTALL | Pattern.CASE_INSENSITIVE,
          "aB\nxyzC", "z");
      testMatchesRE2("^ab.*c$", 0, "abc", "xyz\nabc\ndef");

      testMatchesRE2("^ab.*c$", Pattern.MULTILINE, "abc",
          "xyz\nabc\ndef");
      testMatchesRE2("^ab.*c$", Pattern.MULTILINE, "abc", "");
      testMatchesRE2("^ab.*c$", Pattern.DOTALL | Pattern.MULTILINE,
          "ab\nc", "AB\nc");
      testMatchesRE2("^ab.*c$", Pattern.DOTALL | Pattern.MULTILINE |
          Pattern.CASE_INSENSITIVE, "AB\nc", "z");
    }

    private void testFind(String regexp, int flag, String match, String nonMatch) {
      assertEquals(true, Pattern.compile(regexp, flag, options).matcher(utf8Slice(match)).find());
      assertEquals(false, Pattern.compile(regexp, flag, options).matcher(utf8Slice(nonMatch)).find());
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
      testFind("^ab.*c$", Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE,
          "xyz\nAB\nc\ndef", "z");
    }

    @Test
    public void testSplit() {
      testSplit("/", "abcde", new String[]{"abcde"});
      testSplit("/", "a/b/cc//d/e//",
          new String[]{"a", "b", "cc", "", "d", "e"});
      testSplit("/", "a/b/cc//d/e//", 3,
          new String[]{"a", "b", "cc//d/e//"});
      testSplit("/", "a/b/cc//d/e//", 4,
          new String[]{"a", "b", "cc", "/d/e//"});
      testSplit("/", "a/b/cc//d/e//", 5,
          new String[]{"a", "b", "cc", "", "d/e//"});
      testSplit("/", "a/b/cc//d/e//", 6,
          new String[]{"a", "b", "cc", "", "d", "e//"});
      testSplit("/", "a/b/cc//d/e//", 7,
          new String[]{"a", "b", "cc", "", "d", "e", "/"});
      testSplit("/", "a/b/cc//d/e//", 8,
          new String[]{"a", "b", "cc", "", "d", "e", "", ""});
      testSplit("/", "a/b/cc//d/e//", 9,
          new String[]{"a", "b", "cc", "", "d", "e", "", ""});

      // The tests below are listed at
      // http://docs.oracle.com/javase/1.5.0/docs/api/java/util/regex/Pattern.html#split(java.lang.CharSequence, int)

      String s = "boo:and:foo";
      String regexp1 = ":";
      String regexp2 = "o";

      testSplit(regexp1, s, 2, new String[]{"boo", "and:foo"});
      testSplit(regexp1, s, 5, new String[]{"boo", "and", "foo"});
      testSplit(regexp1, s, -2, new String[]{"boo", "and", "foo"});
      testSplit(regexp2, s, 5,
          new String[]{"b", "", ":and:f", "", ""});
      testSplit(regexp2, s, -2,
          new String[]{"b", "", ":and:f", "", ""});
      testSplit(regexp2, s, 0, new String[]{"b", "", ":and:f"});
      testSplit(regexp2, s, new String[]{"b", "", ":and:f"});
    }

    @Test
    public void testGroupCount() {
      // It is a simple delegation, but still test it.
      testGroupCount("(.*)ab(.*)a", 2);
      testGroupCount("(.*)(ab)(.*)a", 3);
      testGroupCount("(.*)((a)b)(.*)a", 4);
      testGroupCount("(.*)(\\(ab)(.*)a", 3);
      testGroupCount("(.*)(\\(a\\)b)(.*)a", 3);
    }

    @Test
    public void testQuote() {
      testMatchesRE2(Pattern.quote("ab+c"), 0, "ab+c", "abc");
    }

    private Pattern reserialize(Pattern object) {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      try {
        ObjectOutputStream out = new ObjectOutputStream(bytes);
        out.writeObject(object);
        ObjectInputStream in = new ObjectInputStream(
            new ByteArrayInputStream(bytes.toByteArray()));
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
      assertEquals(p.options(), reserialized.options());
    }

    @Test
    public void testSerialize() {
      assertSerializes(Pattern.compile("ab+c", options));
      assertSerializes(Pattern.compile("^ab.*c$", Pattern.DOTALL | Pattern.MULTILINE, options));
      assertFalse(reserialize(Pattern.compile("abc", options)).matcher(utf8Slice("def")).find());
    }
  }
}
