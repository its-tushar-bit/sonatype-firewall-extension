/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.regex.Pattern;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.component.ProprietaryComponentName;
import com.sonatype.insight.brain.model.repository.ProprietaryComponentNamePattern;

/**
 * Thread-safe matching for component names of a given format. In addition to converting the persistence model into a
 * structure suitable for efficient matching, this also indicates whether a given pattern is new and hence needs to be
 * persisted, thereby avoiding costly database trips.
 */
public class ComponentNameMatcher
{
  private static final Pattern PYPI_TO_DASH_REGEX = Pattern.compile("[-_.]+");

  private final long createTime = System.currentTimeMillis();

  private final String format;

  private final Function<String, String> normalizer;

  /**
   * For matching "my-namespace".
   */
  private final Map<String, Collection<String>> namespaces = new ConcurrentHashMap<>();

  /**
   * For matching "my-namespace*".
   */
  private final SortedMap<Integer, Map<String, Collection<String>>> namespacePrefixesByLength =
      new ConcurrentSkipListMap<>();

  /**
   * For matching "my-name".
   */
  private final Map<String, Collection<String>> names = new ConcurrentHashMap<>();

  /**
   * For matching "my-name*".
   */
  private final SortedMap<Integer, Map<String, Collection<String>>> namePrefixesByLength =
      new ConcurrentSkipListMap<>();

  /**
   * For matching "*my-name".
   */
  private final SortedMap<Integer, Map<String, Collection<String>>> nameSuffixesByLength =
      new ConcurrentSkipListMap<>();

  /**
   * Used to deduplicate the strings that describe the pattern source. All patterns from one repo have the same source,
   * so lots of memory to save.
   */
  private final Map<String, String> sourcePool = new ConcurrentHashMap<>();

  public ComponentNameMatcher(String format, Collection<ProprietaryComponentNamePattern> patterns) {
    this.format = format;
    this.normalizer = getNormalizer(format);
    add(patterns);
  }

  private static Function<String, String> getNormalizer(String format) {
    if (ComponentIdentifier.FORMAT_PYPI.equals(format)) {
      return ComponentNameMatcher::normalizePypi;
    }
    if (!ComponentIdentifier.isCaseSensitive(format)) {
      return ComponentNameMatcher::toLowerCase;
    }
    return Function.identity();
  }

  private static String toLowerCase(String str) {
    return str.toLowerCase(Locale.ROOT);
  }

  private static String normalizePypi(String str) {
    // https://www.python.org/dev/peps/pep-0503/#normalized-names
    return PYPI_TO_DASH_REGEX.matcher(str).replaceAll("-").toLowerCase(Locale.ROOT);
  }

  public Collection<ProprietaryComponentNamePattern> add(Collection<ProprietaryComponentNamePattern> patterns) {
    Collection<ProprietaryComponentNamePattern> newlyAdded = new ArrayList<>();
    for (ProprietaryComponentNamePattern pattern : patterns) {
      if (add(pattern)) {
        newlyAdded.add(pattern);
      }
    }
    return newlyAdded;
  }

  private boolean add(ProprietaryComponentNamePattern pattern) {
    if (!format.equals(pattern.getFormat())) {
      throw new IllegalArgumentException("expected format " + format + " but got " + pattern.getFormat());
    }
    boolean changed = false;
    String namespacePattern = pattern.getNamespacePattern();
    String namePattern = pattern.getNamePattern();
    if (namespacePattern != null) {
      namespacePattern = normalizer.apply(namespacePattern);
      if (namespacePattern.endsWith("*")) {
        String prefix = namespacePattern.substring(0, namespacePattern.length() - 1);
        changed = addPattern(prefix, pattern, namespacePrefixesByLength);
      }
      else {
        changed = addPattern(namespacePattern, pattern, namespaces);
        if (changed && ComponentIdentifier.FORMAT_MAVEN.equals(format)) {
          addPattern(namespacePattern + '.', pattern, namespacePrefixesByLength);
        }
      }
    }
    else if (namePattern != null) {
      namePattern = normalizer.apply(namePattern);
      if (namePattern.endsWith("*")) {
        String prefix = namePattern.substring(0, namePattern.length() - 1);
        changed = addPattern(prefix, pattern, namePrefixesByLength);
      }
      else if (namePattern.startsWith("*")) {
        String suffix = namePattern.substring(1);
        changed = addPattern(suffix, pattern, nameSuffixesByLength);
      }
      else {
        changed = addPattern(namePattern, pattern, names);
      }
    }
    return changed;
  }

  private boolean addPattern(
      String value,
      ProprietaryComponentNamePattern pattern,
      SortedMap<Integer, Map<String, Collection<String>>> valuesByLength)
  {
    Map<String, Collection<String>> sourcesByValue =
        valuesByLength.computeIfAbsent(value.length(), length -> new ConcurrentHashMap<>());
    return addPattern(value, pattern, sourcesByValue);
  }

  private boolean addPattern(
      String value,
      ProprietaryComponentNamePattern pattern,
      Map<String, Collection<String>> sourcesByValue)
  {
    String source = pattern.getRepositoryId();
    Collection<String> sources = sourcesByValue.computeIfAbsent(value, key -> new CopyOnWriteArrayList<>());
    if (sources.contains(source)) {
      return false;
    }
    source = sourcePool.computeIfAbsent(source, Function.identity());
    sources.add(source);
    return true;
  }

  public ProprietaryComponentName findMatch(String namespace, String name) {
    if (namespace != null) {
      namespace = normalizer.apply(namespace);
      Collection<String> repos = namespaces.get(namespace);
      if (repos != null) {
        return newMatch(namespace + "/*", repos);
      }
      Map.Entry<String, Collection<String>> prefixMatch = getPrefixMatch(namespace, namespacePrefixesByLength);
      if (prefixMatch != null) {
        return newMatch(prefixMatch.getKey() + "*/*", prefixMatch.getValue());
      }
    }
    else if (name != null) {
      name = normalizer.apply(name);
      Collection<String> repos = names.get(name);
      if (repos != null) {
        return newMatch(name, repos);
      }
      Map.Entry<String, Collection<String>> prefixMatch = getPrefixMatch(name, namePrefixesByLength);
      if (prefixMatch != null) {
        return newMatch(prefixMatch.getKey() + "*", prefixMatch.getValue());
      }
      Map.Entry<String, Collection<String>> suffixMatch = getSuffixMatch(name, nameSuffixesByLength);
      if (suffixMatch != null) {
        return newMatch("*" + suffixMatch.getKey(), suffixMatch.getValue());
      }
    }
    return null;
  }

  private static Map.Entry<String, Collection<String>> getPrefixMatch(
      String value,
      SortedMap<Integer, Map<String, Collection<String>>> prefixesByLength)
  {
    for (Map.Entry<Integer, Map<String, Collection<String>>> entry : prefixesByLength.entrySet()) {
      int prefixLength = entry.getKey();
      if (value.length() >= prefixLength) {
        String prefix = value.substring(0, prefixLength);
        Map<String, Collection<String>> prefixes = entry.getValue();
        Collection<String> repos = prefixes.get(prefix);
        if (repos != null) {
          return new SimpleEntry<>(prefix, repos);
        }
      }
      else {
        break;
      }
    }
    return null;
  }

  private static Map.Entry<String, Collection<String>> getSuffixMatch(
      String value,
      SortedMap<Integer, Map<String, Collection<String>>> suffixesByLength)
  {
    for (Map.Entry<Integer, Map<String, Collection<String>>> entry : suffixesByLength.entrySet()) {
      int suffixLength = entry.getKey();
      if (value.length() >= suffixLength) {
        String suffix = value.substring(value.length() - suffixLength);
        Map<String, Collection<String>> suffixes = entry.getValue();
        Collection<String> repos = suffixes.get(suffix);
        if (repos != null) {
          return new SimpleEntry<>(suffix, repos);
        }
      }
      else {
        break;
      }
    }
    return null;
  }

  private static ProprietaryComponentName newMatch(String namePattern, Collection<String> sourceRepositories) {
    String source =
        sourceRepositories.isEmpty() ? "[unknown]" : sourceRepositories.iterator().next();
    return new ProprietaryComponentName(namePattern, source);
  }

  public long getCreateTime() {
    return createTime;
  }
}
