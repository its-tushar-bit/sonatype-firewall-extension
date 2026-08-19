/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.io.IOException;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.component.ComponentDisplayNamePart;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dashboard.ComponentRiskDTO;
import com.sonatype.insight.brain.dashboard.DashboardViolationRiskDTO;
import com.sonatype.insight.brain.model.policy.PolicyViolation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.component.ComponentDisplayNameUtil.deriveComponentName;
import static com.sonatype.insight.brain.component.ComponentDisplayNameUtil.fromFilename;
import static com.sonatype.insight.brain.component.ComponentDisplayNameUtil.fromJsonNode;
import static com.sonatype.insight.brain.component.ComponentDisplayNameUtil.fromPolicyViolation;
import static com.sonatype.insight.brain.component.ComponentDisplayNameUtil.injectDisplayName;
import static com.sonatype.insight.brain.utils.DisplayFieldValueAssertionUtil.assertDisplayFieldValuesForGAV;
import static org.assertj.core.api.Assertions.assertThat;

public class ComponentDisplayNameUtilTest
{
  @Test
  public void testInjectDisplayName_Maven() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode jsonNode = (ObjectNode) mapper.readTree("{\"componentIdentifier\":" +
        "{\"format\":\"maven\",\"coordinates\":{\"groupId\":\"g\", \"artifactId\":\"a\", \"version\":\"v\"}}}");
    injectDisplayName(jsonNode);

    ArrayNode displayNode = (ArrayNode) jsonNode.get("displayName").get("parts");
    assertThat(displayNode).hasSize(5);
    assertThat(displayNode.get(0).get("field").textValue()).isEqualTo("Group");
    assertThat(displayNode.get(0).get("value").textValue()).isEqualTo("g");
    assertThat(displayNode.get(1).get("field")).isNull();
    assertThat(displayNode.get(1).get("value").textValue()).isEqualTo(" : ");
    assertThat(displayNode.get(2).get("field").textValue()).isEqualTo("Artifact");
    assertThat(displayNode.get(2).get("value").textValue()).isEqualTo("a");
    assertThat(displayNode.get(3).get("field")).isNull();
    assertThat(displayNode.get(3).get("value").textValue()).isEqualTo(" : ");
    assertThat(displayNode.get(4).get("field").textValue()).isEqualTo("Version");
    assertThat(displayNode.get(4).get("value").textValue()).isEqualTo("v");
  }

  @Test
  public void testInjectDisplayName_Nuget() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode jsonNode = (ObjectNode) mapper.readTree("{\"componentIdentifier\":" +
        "{\"format\":\"nuget\",\"coordinates\":{\"packageId\":\"i\",\"version\":\"v\"}}}");

    List<ComponentDisplayNamePart> displayFieldValues = fromJsonNode(jsonNode).parts;
    assertThat(displayFieldValues).hasSize(3);
    assertThat(displayFieldValues.get(0).field).isEqualTo("ID");
    assertThat(displayFieldValues.get(0).value).isEqualTo("i");
    assertThat(displayFieldValues.get(1).field).isNull();
    assertThat(displayFieldValues.get(1).value).isEqualTo(" ");
    assertThat(displayFieldValues.get(2).field).isEqualTo("Version");
    assertThat(displayFieldValues.get(2).value).isEqualTo("v");
  }

  @Test
  public void testInjectDisplayName_Filename() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode jsonNode = (ObjectNode) mapper.readTree("{\"filenames\":[\"foo.jar\",\"bar.ear\",\"baz.war\"]}");
    injectDisplayName(jsonNode);

    ArrayNode displayNode = (ArrayNode) jsonNode.get("displayName").get("parts");
    assertThat(displayNode).hasSize(5);
    assertThat(displayNode.get(0).get("field").textValue()).isEqualTo("Filename");
    assertThat(displayNode.get(0).get("value").textValue()).isEqualTo("foo.jar");
    assertThat(displayNode.get(1).get("field")).isNull();
    assertThat(displayNode.get(1).get("value").textValue()).isEqualTo(", ");
    assertThat(displayNode.get(2).get("field").textValue()).isEqualTo("Filename");
    assertThat(displayNode.get(2).get("value").textValue()).isEqualTo("bar.ear");
    assertThat(displayNode.get(3).get("field")).isNull();
    assertThat(displayNode.get(3).get("value").textValue()).isEqualTo(", ");
    assertThat(displayNode.get(4).get("field").textValue()).isEqualTo("Filename");
    assertThat(displayNode.get(4).get("value").textValue()).isEqualTo("baz.war");
  }

  @Test
  public void testInjectDisplayName_Hash() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode jsonNode = (ObjectNode) mapper.readTree("{\"hash\":\"h\"}");
    injectDisplayName(jsonNode);

    ArrayNode displayNode = (ArrayNode) jsonNode.get("displayName").get("parts");
    assertThat(displayNode).hasSize(2);
    assertThat(displayNode.get(0).get("field")).isNull();
    assertThat(displayNode.get(0).get("value").textValue()).isEqualTo("(Anonymized Path) SHA1: ");
    assertThat(displayNode.get(1).get("field").textValue()).isEqualTo("Hash");
    assertThat(displayNode.get(1).get("value").textValue()).isEqualTo("h");
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
    assertThat(componentNameDTO).isNull();
  }

  @Test
  public void testDeriveComponentName() {
    String filename = "c.jar";
    ComponentDisplayName displayName = fromFilename(filename, "hash");
    assertThat(deriveComponentName(createDashboardViolationRiskDTO(displayName, null))).isEqualTo("c.jar");

    assertThat(deriveComponentName(createDashboardViolationRiskDTO(null, "c.jar"))).isEqualTo("c.jar");

    assertThat(deriveComponentName(createDashboardViolationRiskDTO(null, null))).isEqualTo("Unknown");

    assertThat(deriveComponentName(createComponentRiskDTO(displayName, null))).isEqualTo("c.jar");

    assertThat(deriveComponentName(createComponentRiskDTO(null, filename))).isEqualTo("c.jar");

    assertThat(deriveComponentName(createComponentRiskDTO(null, null))).isEqualTo("Unknown");
  }

  private DashboardViolationRiskDTO createDashboardViolationRiskDTO(ComponentDisplayName displayName, String filename) {
    DashboardViolationRiskDTO dto = new DashboardViolationRiskDTO();
    dto.displayName = displayName;
    dto.filename = filename;
    return dto;
  }

  private ComponentRiskDTO createComponentRiskDTO(ComponentDisplayName displayName, String filename) {
    ComponentRiskDTO dto = new ComponentRiskDTO();
    dto.displayName = displayName;
    dto.filename = filename;
    return dto;
  }
}
