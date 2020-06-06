// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.re2j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.truth.Truth;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Testing the RE2Matcher class.
 *
 * @author afrozm@google.com (Afroz Mohiuddin)
 */
@RunWith(JUnit4.class)
public class MatcherTest {

  @Test
  public void testLookingAt() {
    ApiTestUtils.verifyLookingAt("abcdef", "abc", true);
    ApiTestUtils.verifyLookingAt("ab", "abc", false);
  }

  @Test
  public void testMatches() {
    ApiTestUtils.testMatcherMatches("ab+c", "abbbc", "cbbba");
    ApiTestUtils.testMatcherMatches("ab.*c", "abxyzc", "ab\nxyzc");
    ApiTestUtils.testMatcherMatches("^ab.*c$", "abc", "xyz\nabc\ndef");
    ApiTestUtils.testMatcherMatches("ab+c", "abbbc", "abbcabc");
  }

  @Test
  public void testReplaceAll() {
    ApiTestUtils.testReplaceAll(
        "What the Frog's Eye Tells the Frog's Brain",
        "Frog",
        "Lizard",
        "What the Lizard's Eye Tells the Lizard's Brain");
    ApiTestUtils.testReplaceAll(
        "What the Frog's Eye Tells the Frog's Brain",
        "F(rog)",
        "\\$Liza\\rd$1",
        "What the $Lizardrog's Eye Tells the $Lizardrog's Brain");
    ApiTestUtils.testReplaceAll(
        "abcdefghijklmnopqrstuvwxyz123",
        "(.)(.)(.)(.)(.)(.)(.)(.)(.)(.)(.)(.)(.)",
        "$10$20",
        "jb0wo0123");
    ApiTestUtils.testReplaceAll(
        "\u00e1\u0062\u00e7\u2655", "(.)", "<$1>", "<\u00e1><\u0062><\u00e7><\u2655>");
    ApiTestUtils.testReplaceAll(
        "\u00e1\u0062\u00e7\u2655", "[\u00e0-\u00e9]", "<$0>", "<\u00e1>\u0062<\u00e7>\u2655");
    ApiTestUtils.testReplaceAll("hello world", "z*", "x", "xhxexlxlxox xwxoxrxlxdx");
    // test replaceAll with alternation
    ApiTestUtils.testReplaceAll("123:foo", "(?:\\w+|\\d+:foo)", "x", "x:x");
    ApiTestUtils.testReplaceAll("123:foo", "(?:\\d+:foo|\\w+)", "x", "x");
    ApiTestUtils.testReplaceAll("aab", "a*", "<$0>", "<aa><>b<>");
    ApiTestUtils.testReplaceAll("aab", "a*?", "<$0>", "<>a<>a<>b<>");
  }

  @Test
  public void testReplaceFirst() {
    ApiTestUtils.testReplaceFirst(
        "What the Frog's Eye Tells the Frog's Brain",
        "Frog",
        "Lizard",
        "What the Lizard's Eye Tells the Frog's Brain");
    ApiTestUtils.testReplaceFirst(
        "What the Frog's Eye Tells the Frog's Brain",
        "F(rog)",
        "\\$Liza\\rd$1",
        "What the $Lizardrog's Eye Tells the Frog's Brain");
    ApiTestUtils.testReplaceFirst(
        "abcdefghijklmnopqrstuvwxyz123",
        "(.)(.)(.)(.)(.)(.)(.)(.)(.)(.)(.)(.)(.)",
        "$10$20",
        "jb0nopqrstuvwxyz123");
    ApiTestUtils.testReplaceFirst(
        "\u00e1\u0062\u00e7\u2655", "(.)", "<$1>", "<\u00e1>\u0062\u00e7\u2655");
    ApiTestUtils.testReplaceFirst(
        "\u00e1\u0062\u00e7\u2655", "[\u00e0-\u00e9]", "<$0>", "<\u00e1>\u0062\u00e7\u2655");
    ApiTestUtils.testReplaceFirst("hello world", "z*", "x", "xhello world");
    ApiTestUtils.testReplaceFirst("aab", "a*", "<$0>", "<aa>b");
    ApiTestUtils.testReplaceFirst("aab", "a*?", "<$0>", "<>aab");
  }

  @Test
  public void testGroupCount() {
    ApiTestUtils.testGroupCount("(a)(b(c))d?(e)", 4);
  }

  @Test
  public void testGroup() {
    ApiTestUtils.testGroup("xabdez", "(a)(b(c)?)d?(e)", new String[] {"abde", "a", "b", null, "e"});
    ApiTestUtils.testGroup("abc", "(a)(b$)?(b)?", new String[] {"ab", "a", null, "b"});
    ApiTestUtils.testGroup("abc", "(^b)?(b)?c", new String[] {"bc", null, "b"});
    ApiTestUtils.testGroup(" a b", "\\b(.).\\b", new String[] {"a ", "a"});

    // Not allowed to use UTF-8 except in comments, per Java style guide.
    // ("αβξδεφγ", "(.)(..)(...)", new String[] {"αβξδεφ", "α", "βξ", "δεφ"});
    ApiTestUtils.testGroup(
        "\u03b1\u03b2\u03be\u03b4\u03b5\u03c6\u03b3",
        "(.)(..)(...)",
        new String[] {
          "\u03b1\u03b2\u03be\u03b4\u03b5\u03c6", "\u03b1", "\u03b2\u03be", "\u03b4\u03b5\u03c6"
        });
  }

  @Test
  public void testFind() {
    ApiTestUtils.testFind("abcdefgh", ".*[aeiou]", 0, "abcde");
    ApiTestUtils.testFind("abcdefgh", ".*[aeiou]", 1, "bcde");
    ApiTestUtils.testFind("abcdefgh", ".*[aeiou]", 2, "cde");
    ApiTestUtils.testFind("abcdefgh", ".*[aeiou]", 3, "de");
    ApiTestUtils.testFind("abcdefgh", ".*[aeiou]", 4, "e");
    ApiTestUtils.testFindNoMatch("abcdefgh", ".*[aeiou]", 5);
    ApiTestUtils.testFindNoMatch("abcdefgh", ".*[aeiou]", 6);
    ApiTestUtils.testFindNoMatch("abcdefgh", ".*[aeiou]", 7);
  }

  @Test
  public void testInvalidFind() {
    try {
      ApiTestUtils.testFind("abcdef", ".*", 10, "xxx");
      fail();
    } catch (IndexOutOfBoundsException e) {
      /* ok */
    }
  }

  @Test
  public void testInvalidReplacement() {
    try {
      ApiTestUtils.testReplaceFirst("abc", "abc", "$4", "xxx");
      fail();
    } catch (IndexOutOfBoundsException e) {
      /* ok */
      assertTrue(true);
    }
  }

  @Test
  public void testInvalidGroupNoMatch() {
    try {
      ApiTestUtils.testInvalidGroup("abc", "xxx", 0);
      fail();
    } catch (IllegalStateException e) {
      // Linter complains on empty catch block.
      assertTrue(true);
    }
  }

  @Test
  public void testInvalidGroupOutOfRange() {
    try {
      ApiTestUtils.testInvalidGroup("abc", "abc", 1);
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
      new Matcher(Pattern.compile("pattern"), (String) null);
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
      new Matcher(null, "input");
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
      Matcher m = Pattern.compile("a").matcher("abaca");
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
    Matcher m = Pattern.compile("a+").matcher("aaa");
    if (m.matches()) {
      assertEquals("aaa", m.group(0));
    }
  }

  /**
   * Test for b/6891133. Test matches in case of alternation.
   */
  @Test
  public void testAlternationMatches() {
    String s = "123:foo";
    assertTrue(Pattern.compile("(?:\\w+|\\d+:foo)").matcher(s).matches());
    assertTrue(Pattern.compile("(?:\\d+:foo|\\w+)").matcher(s).matches());
  }

  void helperTestMatchEndUTF16(String string, int num, final int end) {
    String pattern = "[" + string + "]";
    RE2 re =
        new RE2(pattern) {
          @Override
          public boolean match(
              CharSequence input, int start, int e, int anchor, int[] group, int ngroup) {
            assertEquals(end, e);
            return super.match(input, start, e, anchor, group, ngroup);
          }
        };
    Pattern pat = new Pattern(pattern, 0, re);
    Matcher m = pat.matcher(string);

    int found = 0;
    while (m.find()) {
      found++;
    }
    assertEquals(
        "Matches Expected " + num + " but found " + found + ", for input " + string, num, found);
  }

  /**
   * Test for variable length encoding, test whether RE2's match function gets the required
   * parameter based on UTF16 codes and not chars and Runes.
   */
  @Test
  public void testMatchEndUTF16() {
    // Latin alphabetic chars such as these 5 lower-case, acute vowels have multi-byte UTF-8
    // encodings but fit in a single UTF-16 code, so the final match is at UTF16 offset 5.
    String vowels = "\225\233\237\243\250";
    helperTestMatchEndUTF16(vowels, 5, 5);

    // But surrogates are encoded as two UTF16 codes, so we should expect match
    // to get 6 rather than 3.
    String utf16 =
        new StringBuilder()
            .appendCodePoint(0x10000)
            .appendCodePoint(0x10001)
            .appendCodePoint(0x10002)
            .toString();
    assertEquals(utf16, "\uD800\uDC00\uD800\uDC01\uD800\uDC02");
    helperTestMatchEndUTF16(utf16, 3, 6);
  }

  @Test
  public void testAppendTail_StringBuffer() {
    Pattern p = Pattern.compile("cat");
    Matcher m = p.matcher("one cat two cats in the yard");
    StringBuffer sb = new StringBuffer();
    while (m.find()) {
      m.appendReplacement(sb, "dog");
    }
    m.appendTail(sb);
    m.appendTail(sb);
    assertEquals("one dog two dogs in the yards in the yard", sb.toString());
  }

  @Test
  public void testAppendTail_StringBuilder() {
    Pattern p = Pattern.compile("cat");
    Matcher m = p.matcher("one cat two cats in the yard");
    StringBuilder sb = new StringBuilder();
    while (m.find()) {
      m.appendReplacement(sb, "dog");
    }
    m.appendTail(sb);
    m.appendTail(sb);
    assertEquals("one dog two dogs in the yards in the yard", sb.toString());
  }

  @Test
  public void testResetOnFindInt_StringBuffer() {
    StringBuffer buffer;
    Matcher matcher = Pattern.compile("a").matcher("zza");

    assertTrue(matcher.find());

    buffer = new StringBuffer();
    matcher.appendReplacement(buffer, "foo");
    assertEquals("1st time", "zzfoo", buffer.toString());

    assertTrue(matcher.find(0));

    buffer = new StringBuffer();
    matcher.appendReplacement(buffer, "foo");
    assertEquals("2nd time", "zzfoo", buffer.toString());
  }

  @Test
  public void testResetOnFindInt_StringBuilder() {
    StringBuilder buffer;
    Matcher matcher = Pattern.compile("a").matcher("zza");

    assertTrue(matcher.find());

    buffer = new StringBuilder();
    matcher.appendReplacement(buffer, "foo");
    assertEquals("1st time", "zzfoo", buffer.toString());

    assertTrue(matcher.find(0));

    buffer = new StringBuilder();
    matcher.appendReplacement(buffer, "foo");
    assertEquals("2nd time", "zzfoo", buffer.toString());
  }

  @Test
  public void testEmptyReplacementGroups_StringBuffer() {
    StringBuffer buffer = new StringBuffer();
    Matcher matcher = Pattern.compile("(a)(b$)?(b)?").matcher("abc");
    assertTrue(matcher.find());
    matcher.appendReplacement(buffer, "$1-$2-$3");
    assertEquals("a--b", buffer.toString());
    matcher.appendTail(buffer);
    assertEquals("a--bc", buffer.toString());

    buffer = new StringBuffer();
    matcher = Pattern.compile("(a)(b$)?(b)?").matcher("ab");
    assertTrue(matcher.find());
    matcher.appendReplacement(buffer, "$1-$2-$3");
    matcher.appendTail(buffer);
    assertEquals("a-b-", buffer.toString());

    buffer = new StringBuffer();
    matcher = Pattern.compile("(^b)?(b)?c").matcher("abc");
    assertTrue(matcher.find());
    matcher.appendReplacement(buffer, "$1-$2");
    matcher.appendTail(buffer);
    assertEquals("a-b", buffer.toString());

    buffer = new StringBuffer();
    matcher = Pattern.compile("^(.)[^-]+(-.)?(.*)").matcher("Name");
    assertTrue(matcher.find());
    matcher.appendReplacement(buffer, "$1$2");
    matcher.appendTail(buffer);
    assertEquals("N", buffer.toString());
  }

  @Test
  public void testEmptyReplacementGroups_StringBuilder() {
    StringBuilder buffer = new StringBuilder();
    Matcher matcher = Pattern.compile("(a)(b$)?(b)?").matcher("abc");
    assertTrue(matcher.find());
    matcher.appendReplacement(buffer, "$1-$2-$3");
    assertEquals("a--b", buffer.toString());
    matcher.appendTail(buffer);
    assertEquals("a--bc", buffer.toString());

    buffer = new StringBuilder();
    matcher = Pattern.compile("(a)(b$)?(b)?").matcher("ab");
    assertTrue(matcher.find());
    matcher.appendReplacement(buffer, "$1-$2-$3");
    matcher.appendTail(buffer);
    assertEquals("a-b-", buffer.toString());

    buffer = new StringBuilder();
    matcher = Pattern.compile("(^b)?(b)?c").matcher("abc");
    assertTrue(matcher.find());
    matcher.appendReplacement(buffer, "$1-$2");
    matcher.appendTail(buffer);
    assertEquals("a-b", buffer.toString());

    buffer = new StringBuilder();
    matcher = Pattern.compile("^(.)[^-]+(-.)?(.*)").matcher("Name");
    assertTrue(matcher.find());
    matcher.appendReplacement(buffer, "$1$2");
    matcher.appendTail(buffer);
    assertEquals("N", buffer.toString());
  }

  // This example is documented in the com.google.re2j package.html.
  @Test
  public void testDocumentedExample() {
    Pattern p = Pattern.compile("b(an)*(.)");
    Matcher m = p.matcher("by, band, banana");
    assertTrue(m.lookingAt());
    m.reset();
    assertTrue(m.find());
    assertEquals("by", m.group(0));
    assertNull(m.group(1));
    assertEquals("y", m.group(2));
    assertTrue(m.find());
    assertEquals("band", m.group(0));
    assertEquals("an", m.group(1));
    assertEquals("d", m.group(2));
    assertTrue(m.find());
    assertEquals("banana", m.group(0));
    assertEquals("an", m.group(1));
    assertEquals("a", m.group(2));
    assertFalse(m.find());
  }

  @Test
  public void testMutableCharSequence() {
    Pattern p = Pattern.compile("b(an)*(.)");
    StringBuilder b = new StringBuilder("by, band, banana");
    Matcher m = p.matcher(b);
    assertTrue(m.find(0));
    int start = b.indexOf("ban");
    b.replace(b.indexOf("ban"), start + 3, "b");
    assertTrue(m.find(b.indexOf("ban")));
  }

  @Test
  public void testNamedGroups() {
    Pattern p =
        Pattern.compile(
            "(?P<baz>f(?P<foo>b*a(?P<another>r+)){0,10})" + "(?P<bag>bag)?(?P<nomatch>zzz)?");
    Matcher m = p.matcher("fbbarrrrrbag");
    assertTrue(m.matches());
    assertEquals("fbbarrrrr", m.group("baz"));
    assertEquals("bbarrrrr", m.group("foo"));
    assertEquals("rrrrr", m.group("another"));
    assertEquals(0, m.start("baz"));
    assertEquals(1, m.start("foo"));
    assertEquals(4, m.start("another"));
    assertEquals(9, m.end("baz"));
    assertEquals(9, m.end("foo"));
    assertEquals("bag", m.group("bag"));
    assertEquals(9, m.start("bag"));
    assertEquals(12, m.end("bag"));
    assertNull(m.group("nomatch"));
    assertEquals(-1, m.start("nomatch"));
    assertEquals(-1, m.end("nomatch"));
    assertEquals("whatbbarrrrreverbag", appendReplacement(m, "what$2ever${bag}"));

    try {
      m.group("nonexistent");
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      // Expected
    }
  }

  private String appendReplacement(Matcher m, String replacement) {
    StringBuilder b = new StringBuilder();
    m.appendReplacement(b, replacement);
    return b.toString();
  }

  // See https://github.com/google/re2j/issues/96.
  // Ensures that RE2J generates the correct zero-width assertions (e.g. END_LINE, END_TEXT) when matching on
  // a substring of a larger input. For example:
  //
  // pattern: (\d{2} ?(\d|[a-z])?)($|[^a-zA-Z])
  // input: "22 bored"
  //
  // pattern.find(input) is true matcher.group(0) will contain "22 b". When retrieving group(1) from this matcher,
  // RE2J re-matches the group, but only considers "22 b" as the input. If it incorrectly treats 'b' as END_OF_LINE
  // and END_OF_TEXT, then group(1) will contain "22 b" when it should actually contain "22".
  @Test
  public void testGroupZeroWidthAssertions() {
    Matcher m = Pattern.compile("(\\d{2} ?(\\d|[a-z])?)($|[^a-zA-Z])").matcher("22 bored");
    Truth.assertThat(m.find()).isTrue();
    Truth.assertThat(m.group(1)).isEqualTo("22");
  }

  @Test
  public void testPatternLongestMatch() {
    final String pattern = "(?:a+)|(?:a+ b+)";
    final String text = "xxx aaa bbb yyy";
    {
      final Matcher matcher = Pattern.compile(pattern).matcher(text);
      assertTrue(matcher.find());
      assertEquals("aaa", text.substring(matcher.start(), matcher.end()));
    }
    {
      final Matcher matcher = Pattern.compile(pattern, Pattern.LONGEST_MATCH).matcher(text);
      assertTrue(matcher.find());
      assertEquals("aaa bbb", text.substring(matcher.start(), matcher.end()));
    }
  }
}
