/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NormalizeCoordinatesTest
{
  @Test
  public void testNormalize_allPopulated_returnsAll() {
    Map<String, String> coords = new LinkedHashMap<>();
    coords.put("groupId", "com.example");
    coords.put("artifactId", "lib");

    SortedMap<String, String> result = LicenseOverrideInternalDAO.normalizeCoordinates(coords);

    assertThat(result).containsOnlyKeys("groupId", "artifactId");
  }

  @Test
  public void testNormalize_blankValue_stripsIt() {
    Map<String, String> coords = new LinkedHashMap<>();
    coords.put("groupId", "com.example");
    coords.put("classifier", "");

    SortedMap<String, String> result = LicenseOverrideInternalDAO.normalizeCoordinates(coords);

    assertThat(result).containsOnlyKeys("groupId");
  }

  @Test
  public void testNormalize_whitespaceOnly_stripsIt() {
    Map<String, String> coords = new LinkedHashMap<>();
    coords.put("groupId", "com.example");
    coords.put("classifier", "   ");

    SortedMap<String, String> result = LicenseOverrideInternalDAO.normalizeCoordinates(coords);

    assertThat(result).containsOnlyKeys("groupId");
  }

  @Test
  public void testNormalize_nullValue_stripsIt() {
    Map<String, String> coords = new LinkedHashMap<>();
    coords.put("groupId", "com.example");
    coords.put("version", null);

    SortedMap<String, String> result = LicenseOverrideInternalDAO.normalizeCoordinates(coords);

    assertThat(result).containsOnlyKeys("groupId");
  }

  @Test
  public void testNormalize_resultIsSorted() {
    Map<String, String> coords = new LinkedHashMap<>();
    coords.put("version", "1.0");
    coords.put("artifactId", "lib");
    coords.put("groupId", "com.example");

    SortedMap<String, String> result = LicenseOverrideInternalDAO.normalizeCoordinates(coords);

    assertThat(result.keySet()).containsExactly("artifactId", "groupId", "version");
  }
}
