/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.io.IOException;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.component.ComponentDisplayNamePart;
import com.sonatype.clm.dto.model.ide.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.insight.brain.model.policy.PolicyViolation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;

import static com.sonatype.insight.brain.component.ComponentDisplayNameUtil.fromJsonNode;
import static com.sonatype.insight.brain.component.ComponentDisplayNameUtil.fromPolicyViolation;
import static com.sonatype.insight.brain.component.ComponentDisplayNameUtil.injectDisplayName;
import static com.sonatype.insight.brain.component.DisplayFieldValueAssertionUtil.assertDisplayFieldValuesForGAV;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class ComponentDisplayNameUtilTest
{
  @Test
  public void testInjectDisplayName_Maven() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode jsonNode = (ObjectNode) mapper.readTree(
        "{\"componentIdentifier\":{\"format\":\"maven\",\"coordinates\":{\"groupId\":\"g\", \"artifactId\":\"a\", \"version\":\"v\"}}}");
    injectDisplayName(jsonNode);

    ArrayNode displayNode = (ArrayNode) jsonNode.get("displayName").get("parts");
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
    ObjectNode jsonNode = (ObjectNode) mapper.readTree(
        "{\"componentIdentifier\":{\"format\":\"nuget\",\"coordinates\":{\"packageId\":\"i\",\"version\":\"v\"}}}");

    List<ComponentDisplayNamePart> displayFieldValues = fromJsonNode(jsonNode).parts;
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

    ArrayNode displayNode = (ArrayNode) jsonNode.get("displayName").get("parts");
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

    ArrayNode displayNode = (ArrayNode) jsonNode.get("displayName").get("parts");
    assertThat(displayNode, is(notNullValue()));
    assertThat(displayNode.size(), is(2));
    assertThat(displayNode.get(0).get("field"), is(nullValue()));
    assertThat(displayNode.get(0).get("value").textValue(), is("(Anonymized Path) SHA1: "));
    assertThat(displayNode.get(1).get("field").textValue(), is("Hash"));
    assertThat(displayNode.get(1).get("value").textValue(), is("h"));
  }

  @Test
  public void testCreateComponentNameFromPolicyViolation() {
    PolicyViolation policyViolation = new PolicyViolation();
    policyViolation.setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("foo", "bar", "1.0"));

    ComponentDisplayName componentNameDTO = fromPolicyViolation(policyViolation);
    assertDisplayFieldValuesForGAV(componentNameDTO.parts, "foo", "bar", "1.0");
  }

  @Test
  public void testCreateComponentNameFromPolicyViolationMissingComponentIdentifier() {
    ComponentDisplayName componentNameDTO = fromPolicyViolation(new PolicyViolation());
    assertThat(componentNameDTO, nullValue());
  }

  @Test
  public void testInjectDisplayName() {
    ComponentFact componentFact = new ComponentFact(ComponentIdentifier.createMavenCoordinates("g", "a", "v"), "h");
    injectDisplayName(componentFact);
    ComponentDisplayName componentDisplayName = componentFact.getDisplayName();

    assertDisplayFieldValuesForGAV(componentDisplayName.parts, "g", "a", "v");
  }
}
