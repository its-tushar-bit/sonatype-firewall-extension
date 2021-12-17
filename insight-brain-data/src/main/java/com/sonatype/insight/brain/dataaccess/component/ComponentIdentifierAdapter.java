/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.InvalidComponentIdentifierException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import com.github.packageurl.PackageURLBuilder;
import com.google.common.base.Strings;

import static com.sonatype.clm.dto.model.component.ComponentIdentifier.MAVEN_ARTIFACT_ID;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.MAVEN_CLASSIFIER;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.MAVEN_EXTENSION;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.MAVEN_GROUP_ID;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.VERSION;

/**
 * Provides utility methods for extracting ComponentIdentifier details from JSON.
 *
 * @since 1.13.0
 */
public class ComponentIdentifierAdapter
{
  public static final String COMPONENT_IDENTIFIER = "componentIdentifier";

  public static final String PURL_IDENTIFIER = "packageUrl";

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
    final String extension = JsonUtils.getNullableString(objectNode.get(MAVEN_EXTENSION));
    final String classifier = JsonUtils.getNullableString(objectNode.get(MAVEN_CLASSIFIER));

    if (!Strings.isNullOrEmpty(groupId)) {
      return ComponentIdentifier.createMavenCoordinates(groupId, artifactId, version, classifier, extension);
    }
    return null;
  }

  public static PackageUrlIdentifier getPackageUrlIdentifier(JsonNode objectNode) {
    if (objectNode.hasNonNull(PURL_IDENTIFIER)) {
      return new PackageUrlIdentifier(objectNode.get(PURL_IDENTIFIER).asText());
    }

    return PackageUrlIdentifier.fromComponentIdentifier(getComponentIdentifier(objectNode));
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
      throw new UncheckedIOException("Error deserializing ComponentIdentifier", e);
    }
    componentIdentifier.validate();
    return componentIdentifier;
  }

  /**
   * Serializes a ComponentIdentifier to unformatted json string.
   * 
   * @since 1.13.0
   */
  public static String toJson(ComponentIdentifier componentIdentifier) {
    return JsonUtils.writeUnformatted(componentIdentifier);
  }

  /**
   * Serializes a map of component identifier coordinates to unformatted json string.
   * 
   * @since 1.13.0
   */
  public static String toJson(Map<String, String> coordinates) {
    return JsonUtils.writeUnformatted(coordinates);
  }

  /**
   * Remove existing GAV fields and replace with ComponentIdentifier structure.
   */
  public static void replaceGavWithComponentIdentifier(final ObjectNode component) {
    injectComponentIdentifier(component);
    component.remove(Arrays.asList(MAVEN_GROUP_ID, MAVEN_ARTIFACT_ID, VERSION, MAVEN_EXTENSION, MAVEN_CLASSIFIER));
  }

  /**
   * Inject ComponentIdentifier structure if it is absent.
   */
  public static void injectComponentIdentifier(final ObjectNode component) {
    if (!component.hasNonNull(COMPONENT_IDENTIFIER)) {
      ComponentIdentifier componentIdentifier = getComponentIdentifier(component);
      if (componentIdentifier != null) {
        component.set(COMPONENT_IDENTIFIER, JsonUtils.asTree(componentIdentifier));
      }
    }
  }

  /**
   * Returns a map of coordinates that contains only maven GAV coordinates (no classifier or extension).
   * The result is ordered alphabetically by keys, hence the SortedMap instead of Map. (It must match the ordering of
   * coordinates in {@link ComponentIdentifier}.)
   */
  public static SortedMap<String, String> toGavOnlyCoordinates(final Map<String, String> coordinates) {
    TreeMap<String, String> gavCoordinates = new TreeMap<>();
    gavCoordinates.put(MAVEN_GROUP_ID, coordinates.get(MAVEN_GROUP_ID));
    gavCoordinates.put(MAVEN_ARTIFACT_ID, coordinates.get(MAVEN_ARTIFACT_ID));
    gavCoordinates.put(VERSION, coordinates.get(VERSION));
    return gavCoordinates;
  }

  public static ComponentIdentifier toComponentIdentifier(final String packageUrl) {
    PackageUrlIdentifier packageUrlIdentifier = new PackageUrlIdentifier(packageUrl);
    return packageUrlIdentifier.toComponentIdentifier();
  }

  public static ComponentIdentifier toComponentIdentifier(
      final String format,
      final String name,
      final String version)
  {
    try {
      final PackageURL packageURL = PackageURLBuilder.aPackageURL()
          .withType(format).withName(name).withVersion(version).build();
      return toComponentIdentifier(packageURL.canonicalize());
    }
    catch (MalformedPackageURLException e) {
      throw new InvalidComponentIdentifierException("Error transforming to component identifier: " + e.getMessage());
    }
  }

  @SuppressWarnings("unchecked")
  public static ComponentIdentifier formatAndJsonToComponentIdentifier(String format, String coordinatesJson) {
    try {
      return new ComponentIdentifier(format, JsonUtils.parse(coordinatesJson, Map.class));
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
