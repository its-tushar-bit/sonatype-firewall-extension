/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Minimal JUnit 5 replacement for the JUnit 4 {@code org.junit.contrib.java.lang.system.EnvironmentVariables} rule
 * (from {@code system-rules}), which has no JUnit 5 equivalent. Instances keep the same {@code set(name, value)} /
 * {@code clear(name)} API as the legacy rule so call sites do not change; call {@link #restore()} from an
 * {@code @AfterEach} method to undo every mutation, mirroring the rule's automatic per-test restore.
 *
 * <p>
 * Known technical debt: this mutates the JVM-internal {@code Collections$UnmodifiableMap.m} backing field of the
 * process environment. It is not public API and may require {@code --add-opens} on future JDKs; replace with a
 * JUnit 5 environment helper (e.g. {@code system-stubs-jupiter}) when one is adopted. Works today under Java 25.
 * </p>
 */
public final class TestEnvironmentVariables
{
  // Original value per touched key. A null value means the variable was absent before the test mutated it.
  // containsKey(name) distinguishes "already captured" from "not yet captured".
  private final Map<String, String> originalValues = new LinkedHashMap<>();

  /**
   * Sets a process environment variable for the duration of the test. Restored by {@link #restore()}.
   */
  public void set(final String name, final String value) {
    rememberOriginal(name);
    if (value == null) {
      // Mirror the JUnit 4 EnvironmentVariables rule: a null value makes System.getenv(name) return null,
      // which the process environment models as an absent key (its backing map rejects null values).
      writableEnvironment().remove(name);
    }
    else {
      writableEnvironment().put(name, value);
    }
  }

  /**
   * Removes a process environment variable for the duration of the test. Restored by {@link #restore()}.
   */
  public void clear(final String name) {
    rememberOriginal(name);
    writableEnvironment().remove(name);
  }

  /**
   * Restores every environment variable touched via {@link #set} / {@link #clear} to its original value
   * (removing it if it was absent before). Safe to call multiple times.
   */
  public void restore() {
    Map<String, String> environment = writableEnvironment();
    for (Map.Entry<String, String> entry : originalValues.entrySet()) {
      if (entry.getValue() == null) {
        environment.remove(entry.getKey());
      }
      else {
        environment.put(entry.getKey(), entry.getValue());
      }
    }
    originalValues.clear();
  }

  private void rememberOriginal(final String name) {
    Objects.requireNonNull(name, "name");
    if (!originalValues.containsKey(name)) {
      originalValues.put(name, System.getenv(name));
    }
  }

  @SuppressWarnings("unchecked")
  private static Map<String, String> writableEnvironment() {
    try {
      Map<String, String> environment = System.getenv();
      Class<?> unmodifiableMapClass = Class.forName("java.util.Collections$UnmodifiableMap");
      Field backingMapField = unmodifiableMapClass.getDeclaredField("m");
      backingMapField.setAccessible(true);
      return (Map<String, String>) backingMapField.get(environment);
    }
    catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to access the writable process environment map for tests", e);
    }
  }
}
