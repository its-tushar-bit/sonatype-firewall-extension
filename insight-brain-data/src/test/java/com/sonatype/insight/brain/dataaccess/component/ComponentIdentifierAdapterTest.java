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
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static com.sonatype.clm.dto.model.component.ComponentIdentifier.*;
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

  private static final ObjectMapper mapper = new ObjectMapper();

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
  public void testToComponentIdentifierNull() {
    assertThat(ComponentIdentifierAdapter.toComponentIdentifier((JsonNode) null)).isNull();
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
  public void testToJsonCoordinates() {
    Map<String, String> coordinates = new HashMap<>();
    coordinates.put("groupId", "tomcat");// throw in a unicode character just for the hell of it
    coordinates.put("artifactId", "tomcat-util");
    coordinates.put("version", "5.5.23");

    // generated from H2 as in schema_incremental_0060.sql
    String h2ConcatenatedValue = "{\"groupId\":\"tomcat\uF8FF\",\"artifactId\":\"tomcat-util\",\"version\":\"5.5.23\"}";
    assertThat(ComponentIdentifierAdapter.toJson(coordinates)).isEqualTo(h2ConcatenatedValue);
  }

  @Test
  public void testToComponentIdentifier_Purl() {
    final ComponentIdentifier identifier =
        ComponentIdentifierAdapter.toComponentIdentifier("pkg:maven/group/artifact@1.0?classifier=c1&type=jar");

    assertThat(identifier.getFormat()).isEqualTo("maven");
    assertThat(identifier.getCoordinates()).hasSize(5)
        .containsExactlyInAnyOrderEntriesOf(ImmutableMap
            .of(MAVEN_GROUP_ID, "group", MAVEN_ARTIFACT_ID, "artifact", VERSION, "1.0", MAVEN_CLASSIFIER, "c1",
                MAVEN_EXTENSION, "jar"));
  }

  @Test
  public void testToComponentIdentifier_cpeFromPathnames() throws JsonProcessingException {
    JsonNode jsonNode = mapper.readTree("{\"pathnames\" : [ " +
        "\"dependency:/cpe-test-bom.json/pkg:cpe\\\\acme\\\\application@9.1?edition=en&update=beta\" ]}");
    ComponentIdentifier identifier = ComponentIdentifierAdapter.getComponentIdentifier(jsonNode);

    assertThat(identifier).isNotNull();
    assertThat(identifier.getFormat()).isEqualTo(FORMAT_CPE);
    assertThat(identifier.getCoordinates()).hasSize(5)
        .containsExactlyInAnyOrderEntriesOf(ImmutableMap
            .of(GENERIC_NAMESPACE, "acme", GENERIC_NAME, "application", VERSION, "9.1", CPE_EDITION, "en",
                CPE_UPDATE, "beta"));
  }

  @Test
  public void testToComponentIdentifier_swidFromPathnames() throws JsonProcessingException {
    JsonNode jsonNode = mapper.readTree("{\"pathnames\" : [ " +
        "\"dependency:/swid-test-bom.json/pkg:swid\\\\acme\\\\application@2.0?tag_id=1234-5678\" ]}");
    ComponentIdentifier identifier = ComponentIdentifierAdapter.getComponentIdentifier(jsonNode);

    assertThat(identifier).isNotNull();
    assertThat(identifier.getFormat()).isEqualTo(FORMAT_SWID);
    assertThat(identifier.getCoordinates()).hasSize(4)
        .containsExactlyInAnyOrderEntriesOf(ImmutableMap
            .of(GENERIC_NAMESPACE, "acme", GENERIC_NAME, "application", VERSION, "2.0",
                SWID_TAG_ID, "1234-5678"));
  }

  @Test
  public void testGetPackageUrlIdentifier_InvalidPath() {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode node = mapper.createObjectNode();
    ArrayNode list = node.putArray("pathnames");
    list.add("dependency:/bom.xml/pkg:nuget\\name@4.0.0");
    PackageUrlIdentifier purl = ComponentIdentifierAdapter.getPackageUrlIdentifier(node);

    assertThat(purl.getPackageUrl()).isEqualTo("pkg:nuget/name@4.0.0");
  }

  @Test
  public void testGetPackageUrlIdentifier_MisleadingPurlLikePath() {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode node = mapper.createObjectNode();
    ArrayNode list = node.putArray("pathnames");
    list.add("dependency:/org.example:maven-java:jar:1.0/com.example.pkg:some-pkg:jar:1.0");
    PackageUrlIdentifier purl = ComponentIdentifierAdapter.getPackageUrlIdentifier(node);

    assertThat(purl.getPackageUrl()).isEqualTo("pkg:maven/com.example.pkg/some-pkg@1.0?type=jar");
  }

  @Test
  public void testToComponentIdentifier_FromFormatNameAndVersion() {
    final ComponentIdentifier identifier =
        ComponentIdentifierAdapter.toComponentIdentifier("nuget", "package", "2.0");

    assertThat(identifier.getFormat()).isEqualTo("nuget");
    assertThat(identifier.getCoordinates()).hasSize(2)
        .containsExactlyInAnyOrderEntriesOf(ImmutableMap.of(NUGET_PACKAGE_ID, "package", VERSION, "2.0"));
  }

  @Test
  public void testToComponentIdentifier_FromFormatNameAndVersion_UnknownFormat() {
    final ComponentIdentifier identifier =
        ComponentIdentifierAdapter.toComponentIdentifier("deb-9", "glibc", "f6536+45");

    assertThat(identifier.getFormat()).isEqualTo("deb-9");
    assertThat(identifier.getCoordinates()).hasSize(2)
        .containsExactlyInAnyOrderEntriesOf(ImmutableMap.of("name", "glibc", VERSION, "f6536+45"));
  }

  @Test
  public void testFormatAndJsonToComponentIdentifier() {
    ComponentIdentifier result = ComponentIdentifierAdapter.formatAndJsonToComponentIdentifier("maven",
        "{\"groupId\":\"g\",\"artifactId\":\"a\",\"version\":\"v\"}");
    assertThat(result).isEqualTo(ComponentIdentifier.createMavenCoordinates("g", "a", "v"));

    result = ComponentIdentifierAdapter.formatAndJsonToComponentIdentifier("nuget",
        "{\"packageId\":\"package\",\"version\":\"1.0\"}");
    assertThat(result).isEqualTo(ComponentIdentifier.createNugetCoordinates("package", "1.0"));
  }

  @Test
  public void testFormatAndJsonToComponentIdentifier_NullFormat() {
    ComponentIdentifier result = ComponentIdentifierAdapter.formatAndJsonToComponentIdentifier(null /* format */,
        "{\"groupId\":\"g\",\"artifactId\":\"a\",\"version\":\"v\"}");
    assertThat(result).isNull();
  }

  @Test
  public void testFormatAndJsonToComponentIdentifier_NullCoordinates() {
    ComponentIdentifier result =
        ComponentIdentifierAdapter.formatAndJsonToComponentIdentifier("maven", null /* coordinatesJson */);
    assertThat(result).isNull();
  }

  @Test
  public void testParsePathToId_properlyHandlesBlankPaths() {
    PackageUrlIdentifier identifier = ComponentIdentifierAdapter.parsePathToId(null);
    assertThat(identifier).isNull();

    identifier = ComponentIdentifierAdapter.parsePathToId("");
    assertThat(identifier).isNull();
  }

  @Test
  public void testParsePathToId_properlyHandlesPurlInPath() {
    String path = "dependency:/some-1.tar.gz/x/app/bom.json/pkg:maven\\org.app\\lib@1?type=jar:123";
    PackageUrlIdentifier identifier = ComponentIdentifierAdapter.parsePathToId(path);

    assertThat(identifier).isNotNull();
    assertThat(identifier.getPackageUrl()).isEqualTo("pkg:maven/org.app/lib@1?type=jar%3A123");
  }

  @Test
  public void testParsePathToId_properlyHandlesComponentIdInPath() {
    String path = "dependency:/some:pom:1/org.comp:app:jar:1";
    PackageUrlIdentifier identifier = ComponentIdentifierAdapter.parsePathToId(path);

    assertThat(identifier).isNotNull();
    assertThat(identifier.getPackageUrl()).isEqualTo("pkg:maven/org.comp/app@1?type=jar");
  }
}
