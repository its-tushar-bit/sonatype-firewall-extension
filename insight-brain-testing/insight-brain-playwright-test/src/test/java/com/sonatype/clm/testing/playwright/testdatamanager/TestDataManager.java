/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.testdatamanager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Loads JSON test-data fixtures from the test classpath into typed Java records.
 * <p>
 * <b>When to use a JSON fixture vs. plain Java constants in the test class:</b>
 * <ul>
 * <li><b>Java constants</b> &mdash; preferred default. Compile-time safety, IDE refactor support,
 * inline Javadoc. Use for static literals (one user, one URL, a fixed expected message).</li>
 * <li><b>JSON fixture</b> &mdash; use when the data meets one of:
 * <ol>
 * <li>The fixture is <b>data-driven</b> (e.g. a list of scenarios for a {@code for}-loop); or</li>
 * <li>The fixture is <b>reused across multiple test classes</b> so a single file keeps them in sync; or</li>
 * <li>Non-engineers need to edit the data without a Java toolchain; or</li>
 * <li>The fixture holds structured HDS mock payloads that are forwarded as raw JSON; or</li>
 * <li>The data set is <b>large</b> (rough threshold: more than ~15 fields), where an inline
 * constants block in the test class would dominate the file and obscure the test logic.</li>
 * </ol>
 * </li>
 * </ul>
 *
 * <h3>Layout</h3>
 * Fixtures live under {@code src/test/resources/test-data/<name>.json} and are loaded by short
 * name: {@code load("login", LoginData.class)} &rarr; {@code test-data/login.json}.
 *
 * <h3>Strict binding</h3>
 * {@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES} is enabled — JSON keys must match
 * the target record exactly. A typo or drift between the JSON file and the record fails at
 * class-load time, not silently at runtime.
 */
public final class TestDataManager
{
  private static final String BASE_DIR = "/test-data/";

  private static final ObjectMapper MAPPER = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

  private TestDataManager() {
  }

  /**
   * Load the fixture and deserialise into a typed record or POJO.
   *
   * @throws IllegalArgumentException if the resource is not on the classpath
   * @throws IllegalStateException if the resource cannot be parsed or mapped to {@code type}
   */
  public static <T> T load(String name, Class<T> type) {
    Objects.requireNonNull(type, "type");
    String path = BASE_DIR + name + ".json";
    try (InputStream in = TestDataManager.class.getResourceAsStream(path)) {
      if (in == null) {
        throw new IllegalArgumentException("Test data resource not found on classpath: " + path);
      }
      return MAPPER.readValue(in, type);
    }
    catch (IOException e) {
      throw new IllegalStateException(
          "Failed to map test-data fixture '" + name + "' to " + type.getName(), e);
    }
  }
}
