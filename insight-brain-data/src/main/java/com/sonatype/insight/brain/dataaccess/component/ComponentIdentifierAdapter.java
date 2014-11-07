/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.component;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import com.sonatype.clm.dto.model.ide.ComponentIdentifier;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import static com.sonatype.clm.dto.model.ide.ComponentIdentifier.MAVEN_ARTIFACT_ID;
import static com.sonatype.clm.dto.model.ide.ComponentIdentifier.MAVEN_GROUP_ID;
import static com.sonatype.clm.dto.model.ide.ComponentIdentifier.VERSION;

/**
 * Provides utility methods for extracting ComponentIdentifier details from JSON.
 *
 * @since 1.13.0
 */
public class ComponentIdentifierAdapter
{
  public static final String COMPONENT_IDENTIFIER = "componentIdentifier";

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
    if (Strings.isNullOrEmpty(componentIdentifier.format)) {
      throw new IllegalStateException("Invalid ComponentIdentifier provided: " + componentIdentifier.toString());
    }
    return componentIdentifier;
  }

  /**
   * Remove existing GAV fields and replace with ComponentIdentifier structure.
   */
  public static void replaceGavWithComponentIdentifier(final ObjectNode component) {
    if (!component.hasNonNull(COMPONENT_IDENTIFIER)) {
      ComponentIdentifier componentIdentifier = getComponentIdentifier(component);
      if (componentIdentifier != null) {
        component.remove(Arrays.asList(MAVEN_GROUP_ID, MAVEN_ARTIFACT_ID, VERSION));
        component.put(COMPONENT_IDENTIFIER, JsonUtils.asTree(componentIdentifier));
      }
    }
  }

  /**
   * Returns a map of coordinates that contains only maven GAV coordinates (no classifier or extension).
   * The result is ordered, hence the LinkedHashMap instead of Map.
   */
  public static LinkedHashMap<String, String> toGavOnlyCoordinates(final Map<String, String> coordinates) {
    LinkedHashMap<String, String> gavCoordinates = new LinkedHashMap<>();
    gavCoordinates.put(MAVEN_GROUP_ID, coordinates.get(MAVEN_GROUP_ID));
    gavCoordinates.put(MAVEN_ARTIFACT_ID, coordinates.get(MAVEN_ARTIFACT_ID));
    gavCoordinates.put(VERSION, coordinates.get(VERSION));
    return gavCoordinates;
  }
}

