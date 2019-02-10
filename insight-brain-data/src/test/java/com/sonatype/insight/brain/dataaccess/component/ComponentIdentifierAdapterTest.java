/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.InvalidComponentIdentifierException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;

import static com.sonatype.clm.dto.model.component.ComponentIdentifier.MAVEN_ARTIFACT_ID;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.MAVEN_GROUP_ID;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.VERSION;
import static org.assertj.core.api.Assertions.assertThat;

public class ComponentIdentifierAdapterTest
{
  private static final String ANY_CONTENT =
      "{\"componentIdentifier\": {\"format\": \"any\", \"coordinates\":{\"a\":\"a\", \"v\":\"v\"}}}";

  private static final ComponentIdentifier ANY_COMPONENT_ID;

  static {
    LinkedHashMap<String, String> coordinates = new LinkedHashMap<>();
    coordinates.put("a", "a");
    coordinates.put("v", "v");
    ANY_COMPONENT_ID = new ComponentIdentifier("any", coordinates);
  }

  private static final String MAVEN_CONTENT =
      "{\"componentIdentifier\": {\"format\": \"maven\", \"coordinates\":{\"groupId\":\"g\",\"artifactId\":\"a\", "
          + "\"version\":\"v\"}}}";

  private static final ComponentIdentifier MAVEN_COMPONENT = ComponentIdentifier.createMavenCoordinates("g", "a", "v");

  private static final String NUGET_CONTENT =
      "{\"componentIdentifier\": {\"format\": \"nuget\", \"coordinates\":{\"packageId\":\"a\", \"version\":\"v\"}}}";

  private static final ComponentIdentifier NUGET_COMPONENT = ComponentIdentifier.createNugetCoordinates("a", "v");

  private static final String GAV_CONTENT = "{\"groupId\":\"g\", \"artifactId\":\"a\", \"version\":\"v\"}";

  private static ObjectMapper mapper = new ObjectMapper();

  @Test
  public void testGetComponentIdentifier() throws Exception {
    JsonNode jsonNode = mapper.readTree(MAVEN_CONTENT);
    ComponentIdentifier componentIdentifier = ComponentIdentifierAdapter.getComponentIdentifier(jsonNode);
    assertThat(componentIdentifier).isEqualTo(MAVEN_COMPONENT);
  }

  @Test
  public void testGetComponentIdentifierLegacy() throws Exception {
    JsonNode jsonNode = mapper.readTree(GAV_CONTENT);
    assertThat(ComponentIdentifierAdapter.getComponentIdentifier(jsonNode)).isEqualTo(MAVEN_COMPONENT);
  }

  @Test
  public void testGetComponentIdentifierNuget() throws Exception {
    JsonNode jsonNode = mapper.readTree(NUGET_CONTENT);
    ComponentIdentifier componentIdentifier = ComponentIdentifierAdapter.getComponentIdentifier(jsonNode);
    assertThat(componentIdentifier).isEqualTo(NUGET_COMPONENT);
  }

  @Test
  public void testGetComponentIdentifierUnknownFormat() throws Exception {
    JsonNode jsonNode = mapper.readTree(ANY_CONTENT);
    ComponentIdentifier componentIdentifier = ComponentIdentifierAdapter.getComponentIdentifier(jsonNode);
    assertThat(componentIdentifier).isEqualTo(ANY_COMPONENT_ID);
  }

  @Test(expected = InvalidComponentIdentifierException.class)
  public void testToComponentIdentifierMissingExpectedData() throws Exception {
    JsonNode jsonNode = mapper.readTree("{\"blah\":{}}");
    ComponentIdentifierAdapter.toComponentIdentifier(jsonNode);
  }

  @Test
  public void testToComponentIdentifierNull() throws Exception {
    assertThat(ComponentIdentifierAdapter.toComponentIdentifier(null)).isNull();
  }

  @Test
  public void testReplaceGAV() throws Exception {
    JsonNode jsonNode = mapper.readTree(GAV_CONTENT);
    JsonNode copy = jsonNode.deepCopy();
    ComponentIdentifierAdapter.replaceGavWithComponentIdentifier((ObjectNode) jsonNode);
    assertThat(copy).isNotEqualTo(jsonNode);
    for (String key : Arrays.asList(MAVEN_GROUP_ID, MAVEN_ARTIFACT_ID, VERSION)) {
      assertThat(jsonNode.hasNonNull(key)).isFalse();
    }
    assertThat(jsonNode.hasNonNull(ComponentIdentifierAdapter.COMPONENT_IDENTIFIER)).isTrue();
    assertThat(ComponentIdentifierAdapter.getComponentIdentifier(jsonNode)).isEqualTo(MAVEN_COMPONENT);
  }

  @Test
  public void testInjectComponentIdentifier() throws Exception {
    JsonNode jsonNode = mapper.readTree(GAV_CONTENT);
    JsonNode copy = jsonNode.deepCopy();
    ComponentIdentifierAdapter.injectComponentIdentifier((ObjectNode) jsonNode);
    assertThat(copy).isNotEqualTo(jsonNode);
    for (String key : Arrays.asList(MAVEN_GROUP_ID, MAVEN_ARTIFACT_ID, VERSION)) {
      assertThat(jsonNode.hasNonNull(key)).isTrue();
    }
    assertThat(jsonNode.hasNonNull(ComponentIdentifierAdapter.COMPONENT_IDENTIFIER)).isTrue();
    assertThat(ComponentIdentifierAdapter.getComponentIdentifier(jsonNode)).isEqualTo(MAVEN_COMPONENT);
  }

  @Test
  public void testInjectWithExistingComponentIdentifier() throws Exception {
    JsonNode jsonNode = mapper.readTree(MAVEN_CONTENT);
    JsonNode copy = jsonNode.deepCopy();
    ComponentIdentifierAdapter.injectComponentIdentifier((ObjectNode) jsonNode);
    assertThat(copy).isEqualTo(jsonNode);
  }

  @Test
  public void testInjectOnNonGAVStructure() throws Exception {
    JsonNode jsonNode = mapper.readTree("{\"blah\":{}}");
    JsonNode copy = jsonNode.deepCopy();
    ComponentIdentifierAdapter.injectComponentIdentifier((ObjectNode) jsonNode);
    assertThat(copy).isEqualTo(jsonNode);
  }

  @Test
  public void testToJsonComponentIdentifier() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    String json = ComponentIdentifierAdapter.toJson(componentIdentifier);
    assertThat(json).isEqualTo("{\"format\":\"maven\",\"coordinates\":{\"artifactId\":\"a\","
        + "\"classifier\":\"c\",\"extension\":\"e\",\"groupId\":\"g\",\"version\":\"v\"}}");
  }

  @Test
  public void testToJsonComponentIdentifierNull() {
    ComponentIdentifier componentIdentifier = null;
    String json = ComponentIdentifierAdapter.toJson(componentIdentifier);
    assertThat(json).isEqualTo("null");
  }

  @Test
  public void testToJsonCoordinates() throws Exception {
    Map<String, String> coordinates = new HashMap<>();
    coordinates.put("groupId", "tomcat");// throw in a unicode character just for the hell of it
    coordinates.put("artifactId", "tomcat-util");
    coordinates.put("version", "5.5.23");

    // generated from H2 as in schema_incremental_0060.sql
    String h2ConcatenatedValue = "{\"groupId\":\"tomcat\uF8FF\",\"artifactId\":\"tomcat-util\",\"version\":\"5.5.23\"}";
    assertThat(ComponentIdentifierAdapter.toJson(coordinates)).isEqualTo(h2ConcatenatedValue);
  }
}
