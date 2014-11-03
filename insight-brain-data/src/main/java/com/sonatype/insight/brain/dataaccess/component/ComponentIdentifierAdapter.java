/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.component;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.sonatype.clm.dto.model.ide.ComponentIdentifier;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;

import static com.sonatype.clm.dto.model.ide.ComponentIdentifier.MAVEN_ARTIFACT_ID;
import static com.sonatype.clm.dto.model.ide.ComponentIdentifier.MAVEN_CLASSIFIER;
import static com.sonatype.clm.dto.model.ide.ComponentIdentifier.MAVEN_EXTENSION;
import static com.sonatype.clm.dto.model.ide.ComponentIdentifier.MAVEN_GROUP_ID;
import static com.sonatype.clm.dto.model.ide.ComponentIdentifier.VERSION;

/**
 * Provides utility methods for extracting ComponentIdentifier details from JSON.
 *
 * @since 1.13.0
 */
public class ComponentIdentifierAdapter
{
  private static final String COMPONENT_IDENTIFIER = "componentIdentifier";

  /**
   * @since 1.13.0
   * Keys that didn't exist prior to this version and which will not be present in the migrated database json
   * that we will be querying against.
   */
  private static final List<String> NON_LEGACY_KEYS = Arrays.asList(MAVEN_EXTENSION, MAVEN_CLASSIFIER);

  /**
   * Extract ComponentIdentifier or create one as needed from existing GAV data.
   */
  public static ComponentIdentifier getComponentIdentifier(final JsonNode objectNode) {
    if (objectNode.hasNonNull(COMPONENT_IDENTIFIER)) {
      return toComponentIdentifier(objectNode.get(COMPONENT_IDENTIFIER));
    }
    final String groupId = JsonUtils.getNullableString(objectNode.get(MAVEN_GROUP_ID));
    final String artifactId = JsonUtils.getNullableString(objectNode.get(MAVEN_ARTIFACT_ID));
    final String version = JsonUtils.getNullableString(objectNode.get(VERSION));

    if (!Strings.isNullOrEmpty(groupId)) {
      return ComponentIdentifier.createMavenCoordinates(groupId, artifactId, version);
    }
    return null;
  }

  /**
   * Convert JSON representation of ComponentIdentifier to the concrete class.
   */
  static ComponentIdentifier toComponentIdentifier(final JsonNode componentIdentifierNode) {
    if (componentIdentifierNode == null) {
      return null;
    }
    ComponentIdentifier componentIdentifier;
    try {
      componentIdentifier = JsonUtils.asPojo(componentIdentifierNode, ComponentIdentifier.class);
    }
    catch (IOException e) {
      throw new RuntimeException("Error deserializing ComponentIdentifier", e);
    }
    if (Strings.isNullOrEmpty(componentIdentifier.format) || componentIdentifier.coordinates.isEmpty()) {
      throw new IllegalStateException("Invalid ComponentIdentifier provided: " + componentIdentifier.toString());
    }
    return componentIdentifier;
  }

  /**
   * hackity hack, remove classifier and extension, at least until other GAV-centric structures can account for them
   * migrated data will never have these and so incoming ComponentDetails that contain values(even if null) will not
   * match
   */
  public static Map<String, String> removeNonLegacyCoordinates(final Map<String, String> coordinates) {
    Map<String, String> sanitizedCoordinates = new LinkedHashMap<>(coordinates);
    Iterator<Entry<String, String>> iterator = sanitizedCoordinates.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<String, String> entry = iterator.next();
      if (NON_LEGACY_KEYS.contains(entry.getKey()) && Strings.isNullOrEmpty(entry.getValue())) {
        iterator.remove();
      }
    }
    return sanitizedCoordinates;
  }
}

