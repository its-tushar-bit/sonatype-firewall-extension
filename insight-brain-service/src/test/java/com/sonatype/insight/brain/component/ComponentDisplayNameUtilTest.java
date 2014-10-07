/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class ComponentDisplayNameUtilTest
{
  @Test
  public void testAugmentJsonNode() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode jsonNode = (ObjectNode) mapper.readTree("{\"groupId\":\"g\", \"artifactId\":\"a\", \"version\":\"v\"}");
    ComponentDisplayNameUtil.injectDisplayName(jsonNode);
    JsonNode mavenNode = jsonNode.get("maven");
    JsonNode infoNode = jsonNode.get("info");

    assertThat(mavenNode, is(notNullValue()));
    assertThat(mavenNode.get("groupId").textValue(), is("g"));
    assertThat(mavenNode.get("artifactId").textValue(), is("a"));
    assertThat(mavenNode.get("version").textValue(), is("v"));
    assertThat(infoNode, is(notNullValue()));
    assertThat(infoNode.get("format").textValue(), is("maven"));

    jsonNode = (ObjectNode) mapper.readTree("{\"filenames\":[\"foo.jar\",\"bar.ear\",\"baz.war\"]}");
    ComponentDisplayNameUtil.injectDisplayName(jsonNode);
    infoNode = jsonNode.get("info");

    assertThat(infoNode, is(notNullValue()));
    assertThat(infoNode.get("format").textValue(), is("unknown"));
    ArrayNode fileNameNode = (ArrayNode) infoNode.get("filenames");
    assertThat(fileNameNode, is(notNullValue()));
    assertThat(fileNameNode.size(), is(3));
    assertThat(fileNameNode.get(0).textValue(), is("foo.jar"));
    assertThat(fileNameNode.get(1).textValue(), is("bar.ear"));
    assertThat(fileNameNode.get(2).textValue(), is("baz.war"));

    jsonNode = (ObjectNode) mapper.readTree("{\"hash\":\"h\"}");
    ComponentDisplayNameUtil.injectDisplayName(jsonNode);

    infoNode = jsonNode.get("info");
    assertThat(infoNode, is(notNullValue()));
    assertThat(infoNode.get("format").textValue(), is("unknown"));
    JsonNode hashNode = infoNode.get("hash");
    assertThat(hashNode, is(notNullValue()));
    assertThat(hashNode.get("sha1_20").textValue(), is("h"));
  }

  @Test
  public void testInjectDisplayName_Maven() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode jsonNode = (ObjectNode) mapper.readTree("{\"groupId\":\"g\", \"artifactId\":\"a\", \"version\":\"v\"}");
    ComponentDisplayNameUtil.injectDisplayName(jsonNode);
    JsonNode infoNode = jsonNode.get("info");

    ArrayNode displayNode = (ArrayNode) infoNode.get("displayName");
    assertThat(displayNode, is(notNullValue()));
    assertThat(displayNode.size(), is(5));
    assertThat(displayNode.get(0).get("field").textValue(), is("Group"));
    assertThat(displayNode.get(0).get("value").textValue(), is("g"));
    assertThat(displayNode.get(1).get("field"), is(nullValue()));
    assertThat(displayNode.get(1).get("value").textValue(), is(" : "));
    assertThat(displayNode.get(2).get("field").textValue(), is("Artifact"));
    assertThat(displayNode.get(2).get("value").textValue(), is("a"));
    assertThat(displayNode.get(3).get("field"), is(nullValue()));
    assertThat(displayNode.get(3).get("value").textValue(), is(" : "));
    assertThat(displayNode.get(4).get("field").textValue(), is("Version"));
    assertThat(displayNode.get(4).get("value").textValue(), is("v"));
  }

  @Test
  public void testInjectDisplayName_Nuget() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode jsonNode = (ObjectNode) mapper
        .readTree("{\"info\":{\"format\":\"nuget\"},\"nuget\":{\"id\":\"i\",\"version\":\"v\"}}");

    List<DisplayFieldValue> displayFieldValues = ComponentDisplayNameUtil.generateDisplayFieldValues(jsonNode);
    assertThat(displayFieldValues, is(notNullValue()));
    assertThat(displayFieldValues.size(), is(3));
    assertThat(displayFieldValues.get(0).getField(), is("ID"));
    assertThat(displayFieldValues.get(0).getValue(), is("i"));
    assertThat(displayFieldValues.get(1).getField(), is(nullValue()));
    assertThat(displayFieldValues.get(1).getValue(), is(" "));
    assertThat(displayFieldValues.get(2).getField(), is("Version"));
    assertThat(displayFieldValues.get(2).getValue(), is("v"));
  }

  @Test
  public void testInjectDisplayName_Filename() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode jsonNode = (ObjectNode) mapper.readTree("{\"filenames\":[\"foo.jar\",\"bar.ear\",\"baz.war\"]}");
    ComponentDisplayNameUtil.injectDisplayName(jsonNode);
    JsonNode infoNode = jsonNode.get("info");

    ArrayNode displayNode = (ArrayNode) infoNode.get("displayName");
    assertThat(displayNode, is(notNullValue()));
    assertThat(displayNode.size(), is(5));
    assertThat(displayNode.get(0).get("field").textValue(), is("Filename"));
    assertThat(displayNode.get(0).get("value").textValue(), is("foo.jar"));
    assertThat(displayNode.get(1).get("field"), is(nullValue()));
    assertThat(displayNode.get(1).get("value").textValue(), is(", "));
    assertThat(displayNode.get(2).get("field").textValue(), is("Filename"));
    assertThat(displayNode.get(2).get("value").textValue(), is("bar.ear"));
    assertThat(displayNode.get(3).get("field"), is(nullValue()));
    assertThat(displayNode.get(3).get("value").textValue(), is(", "));
    assertThat(displayNode.get(4).get("field").textValue(), is("Filename"));
    assertThat(displayNode.get(4).get("value").textValue(), is("baz.war"));
  }

  @Test
  public void testInjectDisplayName_Hash() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode jsonNode = (ObjectNode) mapper.readTree("{\"hash\":\"h\"}");
    ComponentDisplayNameUtil.injectDisplayName(jsonNode);
    JsonNode infoNode = jsonNode.get("info");

    ArrayNode displayNode = (ArrayNode) infoNode.get("displayName");
    assertThat(displayNode, is(notNullValue()));
    assertThat(displayNode.size(), is(2));
    assertThat(displayNode.get(0).get("field"), is(nullValue()));
    assertThat(displayNode.get(0).get("value").textValue(), is("(Anonymized Path) SHA1: "));
    assertThat(displayNode.get(1).get("field").textValue(), is("Hash"));
    assertThat(displayNode.get(1).get("value").textValue(), is("h"));
  }
}
