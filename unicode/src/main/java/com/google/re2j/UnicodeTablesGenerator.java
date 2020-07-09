/*
 * Copyright (c) 2020 The Go Authors. All rights reserved.
 *
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
package com.google.re2j;

import com.google.common.collect.Sets;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import com.ibm.icu.impl.UPropertyAliases;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UCharacterCategory;
import com.ibm.icu.lang.UCharacterEnums;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Generates Unicode data tables for RE2J. The output of this program should be written to
 * {@code UnicodeTables.java}. Code is google-java-formatted before being emitted.
 */
public class UnicodeTablesGenerator {
  private static final TypeName RANGE_MAP_TYPE =
      ParameterizedTypeName.get(Map.class, String.class, int[][].class);
  private static final TypeName RANGE_HASHMAP_TYPE =
      ParameterizedTypeName.get(HashMap.class, String.class, int[][].class);

  // Represents the final output type.
  private final TypeSpec.Builder unicodeTables =
      TypeSpec.classBuilder("UnicodeTables").addModifiers(Modifier.FINAL);

  private final SortedMap<Integer, Integer> sortedOrbits = generateCaseFoldOrbits();

  public static void main(String[] args) {
    new UnicodeTablesGenerator();
  }

  public UnicodeTablesGenerator() {
    Map<Byte, String> aliases = new HashMap<>();
    aliases.put(UCharacterEnums.ECharacterCategory.UPPERCASE_LETTER, "Upper");

    Map<String, CodepointRange> containers = new HashMap<>();

    // Maps category (e.g. Punctuation, dash) to ranges of codepoints in that category.
    Map<Integer, CodepointRange> ranges = new HashMap<>();

    // Maps script (e.g. Latin) to ranges of codepoints in that script.
    Map<Integer, CodepointRange> scriptRanges = new HashMap<>();

    // A map of UScript -> sorted(symbol) for generating script fold exceptions.
    SortedSetMultimap<Integer, Integer> scriptMap = TreeMultimap.create();

    // A map of UCharacter.getType() -> sorted(symbol).
    SortedSetMultimap<Integer, Integer> categoryMap = TreeMultimap.create();

    for (int i = 0; i < UCharacter.MAX_CODE_POINT; i++) {
      if (!UCharacter.isValidCodePoint(i)) {
        continue;
      }

      int type = UCharacter.getType(i);
      if (type == UCharacterCategory.UNASSIGNED) {
        continue;
      }

      String shortName = getShortName(type);
      if (shortName == null) {
        throw new IllegalStateException(
            "missing short name for " + UCharacterCategory.toString(type));
      }
      String containerName = shortName.substring(0, 1);

      int script = UScript.getScript(i);
      scriptMap.put(script, i);
      CodepointRange range = ranges.computeIfAbsent(type, (key) -> new CodepointRange(shortName));
      CodepointRange containerRange =
          containers.computeIfAbsent(containerName, CodepointRange::new);
      CodepointRange scriptRange =
          scriptRanges.computeIfAbsent(script, (key) -> new CodepointRange(UScript.getName(key)));

      categoryMap.put(type, i);
      containerRange.add(i);
      range.add(i);
      scriptRange.add(i);
    }

    // Emit code fold orbits. In order to avoid a binary search at runtime, this code emits a sparse
    // array of codepoint to the next codepoint in a case folding orbit, e.g.
    // k -> K -> K (Kelvin) -> k.
    {
      FieldSpec.Builder caseOrbitField =
          FieldSpec.builder(char[].class, "CASE_ORBIT", Modifier.STATIC, Modifier.FINAL);
      CodeBlock.Builder staticInitBlock = CodeBlock.builder();
      staticInitBlock.addStatement("CASE_ORBIT = new char[$L]", sortedOrbits.lastKey() + 1);
      for (Map.Entry<Integer, Integer> entry : sortedOrbits.entrySet()) {
        staticInitBlock.addStatement(
            "CASE_ORBIT[0x$L] = 0x$L",
            Integer.toHexString(entry.getKey()),
            Integer.toHexString(entry.getValue()));
      }
      unicodeTables.addField(caseOrbitField.build());
      unicodeTables.addStaticBlock(staticInitBlock.build());
    }

    // Emit range maps (e.g. Lu -> ranges of lowercase symbols).
    for (CodepointRange r : ranges.values()) {
      r.finish();

      FieldSpec fieldSpec =
          FieldSpec.builder(int[][].class, r.getName(), Modifier.STATIC, Modifier.FINAL)
              .initializer("$N()", addMakeMethod(r))
              .build();

      unicodeTables.addField(fieldSpec);
    }

    // Emit container maps (e.g. L = Ll + Lm + Lo + Lu) to ranges of codepoints in that container.
    for (Map.Entry<String, CodepointRange> container : containers.entrySet()) {
      container.getValue().finish();

      FieldSpec fieldSpec =
          FieldSpec.builder(int[][].class, container.getKey(), Modifier.STATIC, Modifier.FINAL)
              .initializer("$N()", addMakeMethod(container.getValue()))
              .build();

      unicodeTables.addField(fieldSpec);
    }

    // Emit script maps (e.g. Latin -> ranges of Latin codepoints).
    for (Map.Entry<Integer, CodepointRange> script : scriptRanges.entrySet()) {
      script.getValue().finish();

      FieldSpec fieldSpec =
          FieldSpec.builder(
                  int[][].class, UScript.getName(script.getKey()), Modifier.STATIC, Modifier.FINAL)
              .initializer("$N()", addMakeMethod(script.getValue()))
              .build();

      unicodeTables.addField(fieldSpec);
    }

    for (Map.Entry<Byte, String> alias : aliases.entrySet()) {
      FieldSpec fieldSpec =
          FieldSpec.builder(int[][].class, alias.getValue(), Modifier.STATIC, Modifier.FINAL)
              .initializer("$N", getShortName(alias.getKey()))
              .build();
      unicodeTables.addField(fieldSpec);
    }

    // Add Categories map (e.g. "Lm" -> Lm).
    {
      MethodSpec.Builder categoryMethod =
          MethodSpec.methodBuilder("Categories")
              .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
              .returns(RANGE_MAP_TYPE)
              .addStatement("$T map = new $T()", RANGE_MAP_TYPE, RANGE_HASHMAP_TYPE);

      for (int category : ranges.keySet()) {
        String shortName = getShortName(category);
        categoryMethod.addStatement("map.put($S, $L)", shortName, shortName);
      }
      for (Map.Entry<String, CodepointRange> container : containers.entrySet()) {
        categoryMethod.addStatement("map.put($S, $L)", container.getKey(), container.getKey());
      }
      categoryMethod.addStatement("return $T.unmodifiableMap(map)", Collections.class);
      unicodeTables.addMethod(categoryMethod.build());

      unicodeTables.addField(
          FieldSpec.builder(RANGE_MAP_TYPE, "CATEGORIES", Modifier.STATIC, Modifier.FINAL)
              .initializer("Categories()")
              .build());
    }

    // Add scripts map (e.g. "Katakana" -> Katakana).
    {
      MethodSpec.Builder scriptsMethod =
          MethodSpec.methodBuilder("Scripts")
              .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
              .returns(RANGE_MAP_TYPE)
              .addStatement("$T map = new $T()", RANGE_MAP_TYPE, RANGE_HASHMAP_TYPE);

      for (int script : scriptRanges.keySet()) {
        String scriptName = UScript.getName(script);
        scriptsMethod.addStatement("map.put($S, $L)", scriptName, scriptName);
      }
      scriptsMethod.addStatement("return $T.unmodifiableMap(map)", Collections.class);

      unicodeTables.addMethod(scriptsMethod.build());
      unicodeTables.addField(
          FieldSpec.builder(RANGE_MAP_TYPE, "SCRIPTS", Modifier.STATIC, Modifier.FINAL)
              .initializer("Scripts()")
              .build());
    }

    {
      MethodSpec.Builder foldScriptSpec =
          MethodSpec.methodBuilder("FoldScript")
              .returns(RANGE_MAP_TYPE)
              .addModifiers(Modifier.PRIVATE, Modifier.STATIC);
      foldScriptSpec.addCode("$T map = new $T();", RANGE_MAP_TYPE, RANGE_HASHMAP_TYPE);

      for (Integer script : scriptMap.keySet()) {
        String name = UScript.getName(script);
        if (addFoldExceptions("fold" + name, scriptMap.get(script))) {
          foldScriptSpec.addCode("map.put($S, $L);", name, "fold" + name);
        }
      }

      foldScriptSpec.addCode("return map;");
      unicodeTables.addMethod(foldScriptSpec.build());

      unicodeTables.addField(
          FieldSpec.builder(RANGE_MAP_TYPE, "FOLD_SCRIPT")
              .addModifiers(Modifier.STATIC, Modifier.FINAL)
              .initializer("FoldScript()")
              .build());
    }

    {
      MethodSpec.Builder foldScriptSpec =
          MethodSpec.methodBuilder("FoldCategory")
              .returns(RANGE_MAP_TYPE)
              .addModifiers(Modifier.PRIVATE, Modifier.STATIC);
      foldScriptSpec.addCode("$T map = new $T();", RANGE_MAP_TYPE, RANGE_HASHMAP_TYPE);

      for (Integer category : categoryMap.keySet()) {
        String name = getShortName(category);
        if (addFoldExceptions("fold" + name, categoryMap.get(category))) {
          foldScriptSpec.addCode("map.put($S, $L);", name, "fold" + name);
        }
      }

      foldScriptSpec.addCode("return map;");
      unicodeTables.addMethod(foldScriptSpec.build());

      unicodeTables.addField(
          FieldSpec.builder(RANGE_MAP_TYPE, "FOLD_CATEGORIES")
              .addModifiers(Modifier.STATIC, Modifier.FINAL)
              .initializer("FoldCategory()")
              .build());
    }

    // No instantiating this class!
    unicodeTables.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build());

    JavaFile javaFile =
        JavaFile.builder("com.google.re2j", unicodeTables.build())
            .skipJavaLangImports(true)
            .addFileComment(
                "Generated at $L by Java $L using Unicode version $L.\n",
                Instant.now(),
                System.getProperty("java.version"),
                UCharacter.getUnicodeVersion())
            .addFileComment(
                "Do not change this file, your edits will be lost. Instead change $L.java.",
                UnicodeTablesGenerator.class.getSimpleName())
            .build();

    String source = javaFile.toString();
    try {
      source = new Formatter().formatSource(source);
    } catch (FormatterException e) {
      System.err.println("google-java-format reported an error, the output is most likely invalid");
      e.printStackTrace();
    }

    System.out.print(source);
  }

  // Returns the short name of the given character type.
  private static String getShortName(int type) {
    return UPropertyAliases.INSTANCE.getPropertyValueName(
        UProperty.GENERAL_CATEGORY, type, UProperty.NameChoice.SHORT);
  }

  private String addMakeMethod(CodepointRange r) {
    String name = "make_" + r.getName();
    unicodeTables.addMethod(
        MethodSpec.methodBuilder(name)
            .returns(int[][].class)
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .addCode("return $L;", r.getInitializer())
            .build());
    return name;
  }

  private SortedMap<Integer, Integer> generateCaseFoldOrbits() {
    SortedSetMultimap<Integer, Integer> orbits = TreeMultimap.create();

    for (int i = 0; i < UCharacter.MAX_CODE_POINT; i++) {
      int f = UCharacter.foldCase(i, true);
      if (f == 0) {
        continue;
      }

      Set<Integer> orbit = orbits.get(f);
      orbit.add(f);
      orbit.add(i);
    }

    for (int i = 0; i < UCharacter.MAX_CODE_POINT; i++) {
      SortedSet<Integer> orb = orbits.get(i);
      int u = UCharacter.toUpperCase(i);
      int l = UCharacter.toLowerCase(i);

      if (orb.size() == 1 && u == i && l == i) {
        orbits.removeAll(i);
      } else if (orb.size() == 2) {
        int first = orb.first();
        int second = orb.last();
        if (UCharacter.toLowerCase(first) == second && UCharacter.toUpperCase(second) == first) {
          orbits.removeAll(i);
        }
        if (UCharacter.toUpperCase(first) == second && UCharacter.toLowerCase(second) == first) {
          orbits.removeAll(i);
        }
      }
    }

    SortedMap<Integer, Integer> finalResult = new TreeMap<>();
    for (Map.Entry<Integer, Collection<Integer>> orbit : orbits.asMap().entrySet()) {
      SortedSet<Integer> orbitWithKey = new TreeSet<>(orbit.getValue());
      orbitWithKey.add(orbit.getKey());

      int a = orbitWithKey.first();
      for (Integer i : orbitWithKey.tailSet(a + 1)) {
        finalResult.put(a, i);
        a = i;
      }
      finalResult.put(orbitWithKey.last(), orbitWithKey.first());
    }

    return finalResult;
  }

  // For every codepoint that has a canonical case folding not in codepointClass, emits
  // an exception rule. Returns true if such an exception rule was generated.
  private boolean addFoldExceptions(String name, Set<Integer> codepointClass) {
    Set<Integer> exceptionCodepoints = new TreeSet<>();
    // Each entry is a script ID -> sorted collection of all codepoints in that script.
    for (int codepoint : codepointClass) {
      if (!sortedOrbits.containsKey(codepoint)) {
        // Just uppercase and lowercase.
        int u = UCharacter.toLowerCase(codepoint);
        if (u != codepoint) {
          exceptionCodepoints.add(u);
        }
        int l = UCharacter.toLowerCase(codepoint);
        if (l != codepoint) {
          exceptionCodepoints.add(l);
        }
        exceptionCodepoints.add(codepoint);
      } else {
        int start = codepoint;
        do {
          exceptionCodepoints.add(codepoint);
          codepoint = sortedOrbits.get(codepoint);
        } while (codepoint != start);
      }
    }

    Sets.SetView<Integer> diff = Sets.difference(exceptionCodepoints, codepointClass);
    if (!diff.isEmpty()) {
      CodepointRange range = new CodepointRange(name);
      range.addAll(diff);
      range.finish();

      FieldSpec field =
          FieldSpec.builder(int[][].class, name, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
              .initializer("$N()", addMakeMethod(range))
              .build();
      unicodeTables.addField(field);
      return true;
    }

    return false;
  }

  private static class CodepointRange {
    private final String name;
    private final StringBuilder builder;
    private int setStart = -1;
    private int setStride = -1;
    private int lastInSet = -1;
    private boolean first = true;

    public CodepointRange(String name) {
      this.name = name;
      this.builder = new StringBuilder("new int[][] {");
    }

    public void add(int codepoint) {
      if (setStart == -1) {
        setStart = codepoint;
      } else if (setStride == -1) {
        setStride = codepoint - lastInSet;
      } else if (codepoint - lastInSet != setStride) {
        if (!first) {
          builder.append(",\n");
        }
        // gotta start a new set
        builder.append(String.format("{0x%04X, 0x%04X, %d}", setStart, lastInSet, setStride));
        setStart = codepoint;
        setStride = -1;
        first = false;
      }
      lastInSet = codepoint;
    }

    public void addAll(Collection<Integer> codepoints) {
      for (int i : codepoints) {
        add(i);
      }
    }

    public void finish() {
      if (setStart != -1) {
        if (!first) {
          builder.append(",\n");
        }
        builder.append(
            String.format(
                "{0x%04X, 0x%04X, %d}", setStart, lastInSet, setStride == -1 ? 1 : setStride));
      }
      builder.append("}");
    }

    public String getName() {
      return name;
    }

    public String getInitializer() {
      return builder.toString();
    }
  }
}
