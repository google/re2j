// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.re2j;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import io.airlift.slice.DynamicSliceOutput;
import io.airlift.slice.Slice;
import io.airlift.slice.SliceOutput;

import static io.airlift.slice.Slices.utf8Slice;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Testing the RE2Matcher class.
 *
 * @author afrozm@google.com (Afroz Mohiuddin)
 */
@RunWith(Enclosed.class)
public class MatcherTest {

  private static final int BUFFER_SIZE = 100;

  public static class DFA extends MatcherTestBase {
    public DFA() {
      super(RUN_WITH_DFA);
    }
  }

  public static class NFA extends MatcherTestBase {
    public NFA() {
      super(RUN_WITH_NFA);
    }
  }

  public static abstract class MatcherTestBase extends ApiTest {

    protected MatcherTestBase(Options options) {
      super(options);
    }

    @Test
    public void testLookingAt() {
      verifyLookingAt("abcdef", "abc", true);
      verifyLookingAt("ab", "abc", false);
    }

    @Test
    public void testMatches() {
      testMatcherMatches("ab+c", "abbbc", "cbbba");
      testMatcherMatches("ab.*c", "abxyzc", "ab\nxyzc");
      testMatcherMatches("^ab.*c$", "abc", "xyz\nabc\ndef");
      testMatcherMatches("ab+c", "abbbc", "abbcabc");
    }

    @Test
    public void testReplaceAll() {
      testReplaceAll("What the Frog's Eye Tells the Frog's Brain",
          "Frog", "Lizard",
          "What the Lizard's Eye Tells the Lizard's Brain");
      testReplaceAll("What the Frog's Eye Tells the Frog's Brain",
          "F(rog)", "\\$Liza\\rd$1",
          "What the $Lizardrog's Eye Tells the $Lizardrog's Brain");
      testReplaceAll("abcdefghijklmnopqrstuvwxyz123",
          "(.)(.)(.)(.)(.)(.)(.)(.)(.)(.)(.)(.)(.)", "$10$20", "jb0wo0123");
      testReplaceAll("\u00e1\u0062\u00e7\u2655", "(.)", "<$1>",
          "<\u00e1><\u0062><\u00e7><\u2655>");
      testReplaceAll("\u00e1\u0062\u00e7\u2655", "[\u00e0-\u00e9]",
          "<$0>", "<\u00e1>\u0062<\u00e7>\u2655");
      testReplaceAll("hello world", "z*", "x",
          "xhxexlxlxox xwxoxrxlxdx");
      // test replaceAll with alternation
      testReplaceAll("123:foo", "(?:\\w+|\\d+:foo)", "x", "x:x");
      testReplaceAll("123:foo", "(?:\\d+:foo|\\w+)", "x", "x");
      testReplaceAll("aab", "a*", "<$0>", "<aa><>b<>");
      testReplaceAll("aab", "a*?", "<$0>", "<>a<>a<>b<>");
    }

    @Test
    public void testReplaceFirst() {
      testReplaceFirst("What the Frog's Eye Tells the Frog's Brain",
          "Frog", "Lizard",
          "What the Lizard's Eye Tells the Frog's Brain");
      testReplaceFirst("What the Frog's Eye Tells the Frog's Brain",
          "F(rog)", "\\$Liza\\rd$1",
          "What the $Lizardrog's Eye Tells the Frog's Brain");
      testReplaceFirst("abcdefghijklmnopqrstuvwxyz123",
          "(.)(.)(.)(.)(.)(.)(.)(.)(.)(.)(.)(.)(.)", "$10$20",
          "jb0nopqrstuvwxyz123");
      testReplaceFirst("\u00e1\u0062\u00e7\u2655", "(.)", "<$1>",
          "<\u00e1>\u0062\u00e7\u2655");
      testReplaceFirst("\u00e1\u0062\u00e7\u2655",
          "[\u00e0-\u00e9]",
          "<$0>", "<\u00e1>\u0062\u00e7\u2655");
      testReplaceFirst("hello world", "z*", "x", "xhello world");
      testReplaceFirst("aab", "a*", "<$0>", "<aa>b");
      testReplaceFirst("aab", "a*?", "<$0>", "<>aab");
    }

    @Test
    public void testGroupCount() {
      testGroupCount("(a)(b(c))d?(e)", 4);
    }

    @Test
    public void testGroup() {
      testGroup("xabdez", "(a)(b(c)?)d?(e)",
          new String[]{"abde", "a", "b", null, "e"});
      testGroup(
          "abc", "(a)(b$)?(b)?", new String[]{"ab", "a", null, "b"});
      testGroup(
          "abc", "(^b)?(b)?c", new String[]{"bc", null, "b"});
      testGroup(
          " a b", "\\b(.).\\b", new String[]{"a ", "a"});

      // Not allowed to use UTF-8 except in comments, per Java style guide.
      // ("αβξδεφγ", "(.)(..)(...)", new String[] {"αβξδεφ", "α", "βξ", "δεφ"});
      testGroup("\u03b1\u03b2\u03be\u03b4\u03b5\u03c6\u03b3",
          "(.)(..)(...)",
          new String[]{"\u03b1\u03b2\u03be\u03b4\u03b5\u03c6",
              "\u03b1", "\u03b2\u03be", "\u03b4\u03b5\u03c6"});
    }

    @Test
    public void testFind() {
      testFind("abcdefgh", ".*[aeiou]", 0, "abcde");
      testFind("abcdefgh", ".*[aeiou]", 1, "bcde");
      testFind("abcdefgh", ".*[aeiou]", 2, "cde");
      testFind("abcdefgh", ".*[aeiou]", 3, "de");
      testFind("abcdefgh", ".*[aeiou]", 4, "e");
      testFindNoMatch("abcdefgh", ".*[aeiou]", 5);
      testFindNoMatch("abcdefgh", ".*[aeiou]", 6);
      testFindNoMatch("abcdefgh", ".*[aeiou]", 7);
    }

    @Test
    public void testInvalidFind() {
      try {
        testFind("abcdef", ".*", 10, "xxx");
        fail();
      } catch (IndexOutOfBoundsException e) {
      /* ok */
      }
    }

    @Test
    public void testInvalidReplacement() {
      try {
        testReplaceFirst("abc", "abc", "$4", "xxx");
        fail();
      } catch (IndexOutOfBoundsException e) {
      /* ok */
        assertTrue(true);
      }
    }

    @Test
    public void testInvalidGroupNoMatch() {
      try {
        testInvalidGroup("abc", "xxx", 0);
        fail();
      } catch (IllegalStateException e) {
        // Linter complains on empty catch block.
        assertTrue(true);
      }
    }

    @Test
    public void testInvalidGroupOutOfRange() {
      try {
        testInvalidGroup("abc", "abc", 1);
        fail();
      } catch (IndexOutOfBoundsException e) {
        // Linter complains on empty catch block.
        assertTrue(true);
      }
    }

    /**
     * Test the NullPointerException is thrown on null input.
     */
    @Test
    public void testThrowsOnNullInputReset() {
      // null in constructor.
      try {
        new Matcher(Pattern.compile("pattern", options), null);
        fail();
      } catch (NullPointerException n) {
        // Linter complains on empty catch block.
        assertTrue(true);
      }
    }

    @Test
    public void testThrowsOnNullInputCtor() {
      // null in constructor.
      try {
        new Matcher(null, utf8Slice("input"));
        fail();
      } catch (NullPointerException n) {
        // Linter complains on empty catch block.
        assertTrue(true);
      }
    }

    /**
     * Test that IllegalStateException is thrown if start/end are called before calling find
     */
    @Test
    public void testStartEndBeforeFind() {
      try {
        Matcher m = Pattern.compile("a", options).matcher(utf8Slice("abaca"));
        m.start();
        fail();
      } catch (IllegalStateException ise) {
        assertTrue(true);
      }
    }

    /**
     * Test for b/6891357. Basically matches should behave like find when it comes to updating the
     * information of the match.
     */
    @Test
    public void testMatchesUpdatesMatchInformation() {
      Matcher m = Pattern.compile("a+", options).matcher(utf8Slice("aaa"));
      if (m.matches()) {
        assertEquals(utf8Slice("aaa"), m.group(0));
      }
    }

    /**
     * Test for b/6891133. Test matches in case of alternation.
     */
    @Test
    public void testAlternationMatches() {
      String s = "123:foo";
      assertTrue(Pattern.compile("(?:\\w+|\\d+:foo)", options).matcher(utf8Slice(s)).matches());
      assertTrue(Pattern.compile("(?:\\d+:foo|\\w+)", options).matcher(utf8Slice(s)).matches());
    }

    void helperTestMatchEndUTF8(String string, int num, final int end) {
      String pattern = "[" + string + "]";
      RE2 re = new RE2(pattern, options) {
        @Override
        public boolean match(Slice input, int start, Anchor anchor,
                             int[] group, int ngroup) {
          assertEquals(input.length(), end);
          return super.match(input, start, anchor, group, ngroup);
        }
      };
      Pattern pat = new Pattern(pattern, 0, re, options);
      Matcher m = pat.matcher(utf8Slice(string));

      int found = 0;
      while (m.find()) {
        found++;
      }
      assertEquals("Matches Expected " + num + " but found " + found +
          ", for input " + string, num, found);
    }

    /**
     * Test for variable length encoding, test whether RE2's match function gets the required
     * parameter based on UTF8 codes and not chars and Runes.
     */
    @Test
    public void testMatchEndUTF8() {
      // Latin alphabetic chars such as these 5 lower-case, acute vowels have multi-byte UTF-8
      // encodings but fit in a single UTF-16 code, so the final match is at UTF16 offset 5.
      String vowels = "\225\233\237\243\250";
      helperTestMatchEndUTF8(vowels, 5, 10);

      // But surrogates are encoded as two UTF16 codes, so we should expect match
      // to get 6 rather than 3.
      String utf16 = new StringBuilder().appendCodePoint(0x10000).
          appendCodePoint(0x10001).appendCodePoint(0x10002).toString();
      assertEquals(utf16, "\uD800\uDC00\uD800\uDC01\uD800\uDC02");
      helperTestMatchEndUTF8(utf16, 3, 12);
    }

    @Test
    public void testAppendTail() {
      Pattern p = Pattern.compile("cat", options);
      Matcher m = p.matcher(utf8Slice("one cat two cats in the yard"));
      SliceOutput so = new DynamicSliceOutput(BUFFER_SIZE);
      while (m.find()) {
        m.appendReplacement(so, utf8Slice("dog"));
      }
      m.appendTail(so);
      m.appendTail(so);
      assertEquals("one dog two dogs in the yards in the yard", so.slice().toStringUtf8());
    }

    @Test
    public void testResetOnFindInt() {
      SliceOutput buffer;
      Matcher matcher = Pattern.compile("a", options).matcher(utf8Slice("zza"));

      assertTrue(matcher.find());

      buffer = new DynamicSliceOutput(BUFFER_SIZE);
      matcher.appendReplacement(buffer, utf8Slice("foo"));
      assertEquals("1st time",
          "zzfoo", buffer.slice().toStringUtf8());

      assertTrue(matcher.find(0));

      buffer = new DynamicSliceOutput(BUFFER_SIZE);
      matcher.appendReplacement(buffer, utf8Slice("foo"));
      assertEquals("2nd time",
          "zzfoo", buffer.slice().toStringUtf8());
    }

    @Test
    public void testEmptyReplacementGroups() {
      SliceOutput buffer = new DynamicSliceOutput(BUFFER_SIZE);
      Matcher matcher = Pattern.compile("(a)(b$)?(b)?", options).matcher(utf8Slice("abc"));
      assertTrue(matcher.find());
      matcher.appendReplacement(buffer, utf8Slice("$1-$2-$3"));
      assertEquals("a--b", buffer.slice().toStringUtf8());
      matcher.appendTail(buffer);
      assertEquals("a--bc", buffer.slice().toStringUtf8());

      buffer = new DynamicSliceOutput(BUFFER_SIZE);
      matcher = Pattern.compile("(a)(b$)?(b)?", options).matcher(utf8Slice("ab"));
      assertTrue(matcher.find());
      matcher.appendReplacement(buffer, utf8Slice("$1-$2-$3"));
      matcher.appendTail(buffer);
      assertEquals("a-b-", buffer.slice().toStringUtf8());

      buffer = new DynamicSliceOutput(BUFFER_SIZE);
      matcher = Pattern.compile("(^b)?(b)?c", options).matcher(utf8Slice("abc"));
      assertTrue(matcher.find());
      matcher.appendReplacement(buffer, utf8Slice("$1-$2"));
      matcher.appendTail(buffer);
      assertEquals("a-b", buffer.slice().toStringUtf8());

      buffer = new DynamicSliceOutput(BUFFER_SIZE);
      matcher = Pattern.compile("^(.)[^-]+(-.)?(.*)", options).matcher(utf8Slice("Name"));
      assertTrue(matcher.find());
      matcher.appendReplacement(buffer, utf8Slice("$1$2"));
      matcher.appendTail(buffer);
      assertEquals("N", buffer.slice().toStringUtf8());
    }

    // This example is documented in the com.google.re2j package.html.
    @Test
    public void testDocumentedExample() {
      Pattern p = Pattern.compile("b(an)*(.)", options);
      Matcher m = p.matcher(utf8Slice("by, band, banana"));
      assertTrue(m.lookingAt());
      m.reset();
      assertTrue(m.find());
      assertEquals(utf8Slice("by"), m.group(0));
      assertEquals(null, m.group(1));
      assertEquals(utf8Slice("y"), m.group(2));
      assertTrue(m.find());
      assertEquals(utf8Slice("band"), m.group(0));
      assertEquals(utf8Slice("an"), m.group(1));
      assertEquals(utf8Slice("d"), m.group(2));
      assertTrue(m.find());
      assertEquals(utf8Slice("banana"), m.group(0));
      assertEquals(utf8Slice("an"), m.group(1));
      assertEquals(utf8Slice("a"), m.group(2));
      assertFalse(m.find());
    }
  }
}
