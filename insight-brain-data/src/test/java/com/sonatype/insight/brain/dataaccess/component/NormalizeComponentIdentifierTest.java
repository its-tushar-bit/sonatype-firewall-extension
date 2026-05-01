/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.component;

import java.util.LinkedHashMap;
import java.util.Map;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NormalizeComponentIdentifierTest
{
  @Test
  public void testNormalize_nullInput_returnsNull() {
    assertThat(ComponentLoader.normalizeComponentIdentifier(null)).isNull();
  }

  @Test
  public void testNormalize_allCoordinatesPopulated_returnsSameInstance() {
    Map<String, String> coords = new LinkedHashMap<>();
    coords.put("groupId", "com.example");
    coords.put("artifactId", "lib");
    coords.put("version", "1.0");
    ComponentIdentifier ci = new ComponentIdentifier("maven", coords);

    ComponentIdentifier result = ComponentLoader.normalizeComponentIdentifier(ci);

    assertThat(result).isSameAs(ci);
  }

  @Test
  public void testNormalize_emptyStringCoordinate_stripsIt() {
    Map<String, String> coords = new LinkedHashMap<>();
    coords.put("groupId", "com.example");
    coords.put("artifactId", "lib");
    coords.put("version", "1.0");
    coords.put("classifier", "");
    ComponentIdentifier ci = new ComponentIdentifier("maven", coords);

    ComponentIdentifier result = ComponentLoader.normalizeComponentIdentifier(ci);

    assertThat(result).isNotSameAs(ci);
    assertThat(result.getFormat()).isEqualTo("maven");
    assertThat(result.getCoordinates()).containsOnlyKeys("groupId", "artifactId", "version");
  }

  @Test
  public void testNormalize_nullCoordinate_alreadyStrippedByConstructor() {
    Map<String, String> coords = new LinkedHashMap<>();
    coords.put("groupId", "com.example");
    coords.put("artifactId", "lib");
    coords.put("version", null);
    ComponentIdentifier ci = new ComponentIdentifier("maven", coords);

    ComponentIdentifier result = ComponentLoader.normalizeComponentIdentifier(ci);

    // ComponentIdentifier constructor already strips null values, so normalization is a no-op
    assertThat(result).isSameAs(ci);
    assertThat(result.getCoordinates()).containsOnlyKeys("groupId", "artifactId");
  }

  @Test
  public void testNormalize_mixedEmptyAndNull_stripsAll() {
    Map<String, String> coords = new LinkedHashMap<>();
    coords.put("groupId", "com.example");
    coords.put("artifactId", "");
    coords.put("version", null);
    coords.put("extension", "jar");
    ComponentIdentifier ci = new ComponentIdentifier("maven", coords);

    ComponentIdentifier result = ComponentLoader.normalizeComponentIdentifier(ci);

    assertThat(result.getCoordinates()).containsOnlyKeys("groupId", "extension");
    assertThat(result.getCoordinates().get("groupId")).isEqualTo("com.example");
    assertThat(result.getCoordinates().get("extension")).isEqualTo("jar");
  }
}
