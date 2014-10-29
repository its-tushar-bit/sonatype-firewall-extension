/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.component;

import java.util.LinkedHashMap;

import com.sonatype.clm.dto.model.ide.ComponentIdentifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ComponentIdentifierAdapterTest
{

  private static final String ANY_CONTENT =
    "{\"componentIdentifier\": {\"format\": \"any\", \"coordinates\":{\"a\":\"a\", " +
      "\"v\":\"v\"}}}";

  private static final ComponentIdentifier ANY_COMPONENT_ID = new ComponentIdentifier();

  {
    ANY_COMPONENT_ID.format = "any";
    LinkedHashMap<String, String> coordinates = new LinkedHashMap<>();
    coordinates.put("a", "a");
    coordinates.put("v", "v");
    ANY_COMPONENT_ID.coordinates = coordinates;
  }

  private static final String MAVEN_CONTENT =
    "{\"componentIdentifier\": {\"format\": \"maven\", \"coordinates\":{\"groupId\":\"g\",\"artifactId\":\"a\", " +
      "\"version\":\"v\"}}}";

  private static final ComponentIdentifier MAVEN_COMPONENT = ComponentIdentifier.createMavenCoordinates("g", "a", "v");

  private static final String NUGET_CONTENT =
    "{\"componentIdentifier\": {\"format\": \"nuget\", \"coordinates\":{\"packageId\":\"a\", " +
      "\"version\":\"v\"}}}";

  private static final ComponentIdentifier NUGET_COMPONENT = ComponentIdentifier.createNugetCoordinates("a", "v");

  private static ObjectMapper mapper = new ObjectMapper();

  @Test
  public void testGetComponentIdentifier() throws Exception {
    JsonNode jsonNode =  mapper.readTree(MAVEN_CONTENT);
    ComponentIdentifier componentIdentifier = ComponentIdentifierAdapter.getComponentIdentifier(jsonNode);
    assertThat(componentIdentifier, equalTo(MAVEN_COMPONENT));
  }

  @Test
  public void testGetComponentIdentifierLegacy() throws Exception {
    JsonNode jsonNode =  mapper.readTree("{\"groupId\":\"g\", \"artifactId\":\"a\", \"version\":\"v\"}");
    assertThat(ComponentIdentifierAdapter.getComponentIdentifier(jsonNode), equalTo(MAVEN_COMPONENT));
  }

  @Test
  public void testGetComponentIdentifierNuget() throws Exception {
    JsonNode jsonNode =  mapper.readTree(NUGET_CONTENT);
    ComponentIdentifier componentIdentifier = ComponentIdentifierAdapter.getComponentIdentifier(jsonNode);
    assertThat(componentIdentifier, equalTo(NUGET_COMPONENT));
  }

  @Test
  public void testGetComponentIdentifierUnknownFormat() throws Exception {
    JsonNode jsonNode =  mapper.readTree(ANY_CONTENT);
    ComponentIdentifier componentIdentifier = ComponentIdentifierAdapter.getComponentIdentifier(jsonNode);
    assertThat(componentIdentifier, equalTo(ANY_COMPONENT_ID));
  }

  @Test
  public void testToComponentIdentifierMissingExpectedData() throws Exception {
    JsonNode jsonNode =  mapper.readTree("{\"blah\":{}}");
    try {
      ComponentIdentifierAdapter.toComponentIdentifier(jsonNode);
      fail("Should have thrown IllegalStateException");
    }
    catch (IllegalStateException e) {
      assertThat(e.getMessage(), is("Invalid ComponentIdentifier provided: null: {}"));
    }
  }

  @Test
  public void testToComponentIdentifierNull() throws Exception {
    assertThat(ComponentIdentifierAdapter.toComponentIdentifier(null), nullValue());
  }
}
