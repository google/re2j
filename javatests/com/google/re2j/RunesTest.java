package com.google.re2j;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RunesTest {

    @Test
    public void testRunes() {
        RE2 compile = RE2.compile("[0-13-46-78-9b-ce-fh-i]");
        assertTrue(compile.match("0"));
        assertTrue(compile.match("1"));
        assertTrue(compile.match("3"));
        assertTrue(compile.match("4"));
        assertTrue(compile.match("6"));
        assertTrue(compile.match("7"));
        assertTrue(compile.match("8"));
        assertTrue(compile.match("9"));
        assertTrue(compile.match("b"));
        assertTrue(compile.match("c"));
        assertTrue(compile.match("e"));
        assertTrue(compile.match("f"));
        assertTrue(compile.match("h"));
        assertTrue(compile.match("i"));
        
        assertFalse(compile.match("2"));
        assertFalse(compile.match("5"));
        assertFalse(compile.match("a"));
        assertFalse(compile.match("d"));
        assertFalse(compile.match("g"));
        assertFalse(compile.match("j"));
    }
    @Test
    public void testRunesWithFold() {
        Pattern pattern = Pattern.compile("ak", Pattern.CASE_INSENSITIVE);
        String upperCase = "AK";
        String withKelvin = "AK";
        assertFalse(withKelvin.equals(upperCase));// check we use the kelvin sign 
        
        assertTrue(pattern.matches("ak"));
        assertTrue(pattern.matches(upperCase));
        assertTrue(pattern.matches("aK"));
        assertTrue(pattern.matches("Ak"));
        assertTrue(pattern.matches(withKelvin));
    }
}
