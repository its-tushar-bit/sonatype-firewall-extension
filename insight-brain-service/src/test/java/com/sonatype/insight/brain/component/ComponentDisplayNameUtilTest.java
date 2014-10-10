/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.io.IOException;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.clm.dto.model.component.ComponentDisplayFieldValue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;

import static com.sonatype.insight.brain.component.ComponentDisplayNameUtil.*;
import static com.sonatype.insight.brain.component.DisplayFieldValueAssertionUtil.*;
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
    injectDisplayName(jsonNode);
    JsonNode mavenNode = jsonNode.get("maven");
    JsonNode infoNode = jsonNode.get("info");

    assertThat(mavenNode, is(notNullValue()));
    assertThat(mavenNode.get("groupId").textValue(), is("g"));
    assertThat(mavenNode.get("artifactId").textValue(), is("a"));
    assertThat(mavenNode.get("version").textValue(), is("v"));
    assertThat(infoNode, is(notNullValue()));
    assertThat(infoNode.get("format").textValue(), is("maven"));

    jsonNode = (ObjectNode) mapper.readTree("{\"filenames\":[\"foo.jar\",\"bar.ear\",\"baz.war\"]}");
    injectDisplayName(jsonNode);
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
    injectDisplayName(jsonNode);

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
    injectDisplayName(jsonNode);
    JsonNode infoNode = jsonNode.get("info");

    ArrayNode displayNode = (ArrayNode) infoNode.get("displayName").get("parts");
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

    List<ComponentDisplayFieldValue> displayFieldValues = fromJsonNode(jsonNode).parts;
    assertThat(displayFieldValues, is(notNullValue()));
    assertThat(displayFieldValues.size(), is(3));
    assertThat(displayFieldValues.get(0).field, is("ID"));
    assertThat(displayFieldValues.get(0).value, is("i"));
    assertThat(displayFieldValues.get(1).field, is(nullValue()));
    assertThat(displayFieldValues.get(1).value, is(" "));
    assertThat(displayFieldValues.get(2).field, is("Version"));
    assertThat(displayFieldValues.get(2).value, is("v"));
  }

  @Test
  public void testInjectDisplayName_Filename() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode jsonNode = (ObjectNode) mapper.readTree("{\"filenames\":[\"foo.jar\",\"bar.ear\",\"baz.war\"]}");
    injectDisplayName(jsonNode);
    JsonNode infoNode = jsonNode.get("info");

    ArrayNode displayNode = (ArrayNode) infoNode.get("displayName").get("parts");
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
    injectDisplayName(jsonNode);
    JsonNode infoNode = jsonNode.get("info");

    ArrayNode displayNode = (ArrayNode) infoNode.get("displayName").get("parts");
    assertThat(displayNode, is(notNullValue()));
    assertThat(displayNode.size(), is(2));
    assertThat(displayNode.get(0).get("field"), is(nullValue()));
    assertThat(displayNode.get(0).get("value").textValue(), is("(Anonymized Path) SHA1: "));
    assertThat(displayNode.get(1).get("field").textValue(), is("Hash"));
    assertThat(displayNode.get(1).get("value").textValue(), is("h"));
  }

  @Test
  public void testCreateComponentNameFromGAV() {
    ComponentDisplayName componentNameDTO = fromGav("foo", "bar", "1.0");
    assertDisplayFieldValuesForGAV(componentNameDTO.parts, "foo", "bar", "1.0");
  }

  @Test
  public void testCreateComponentNameFromGAVMissingGroup () {
    ComponentDisplayName componentNameDTO = fromGav(null, "bar", "1.0");
    assertThat(componentNameDTO, nullValue());
  }

  @Test
  public void testCreateComponentNameFromPolicyViolation() {
    PolicyViolation policyViolation = new PolicyViolation();
    policyViolation.setGroupId("foo");
    policyViolation.setArtifactId("bar");
    policyViolation.setVersion("1.0");

    ComponentDisplayName componentNameDTO = fromPolicyViolation(policyViolation);
    assertDisplayFieldValuesForGAV(componentNameDTO.parts, "foo", "bar", "1.0");
  }

  @Test
  public void testCreateComponentNameFromPolicyViolationMissingGav () {
    ComponentDisplayName componentNameDTO = fromPolicyViolation(new PolicyViolation());
    assertThat(componentNameDTO, nullValue());
  }

  @Test
  public void testInjectDisplayName() {
    ComponentFact componentFact = new ComponentFact("g", "a", "v", "h");
    injectDisplayName(componentFact);
    ComponentDisplayName componentDisplayName = componentFact.getDisplayName();

    assertDisplayFieldValuesForGAV(componentDisplayName.parts, "g", "a", "v");
  }
}
