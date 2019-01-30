/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.violation;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.test.LogOutput;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;

public class PolicyViolationLogDTOAssert
{
  public static List<ObjectNode> assertPolicyViolationLogDTOObjectNodes(LogOutput logOutput, int expected)
      throws Exception
  {
    List<String> infoMessages = logOutput.getInfoMessages(PolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);
    assertThat(infoMessages).hasSize(expected);
    ObjectMapper objectMapper = new ObjectMapper();
    List<ObjectNode> policyViolationLogDTOObjectNodes = new ArrayList<>();
    for (String infoMessage : infoMessages) {
      policyViolationLogDTOObjectNodes.add((ObjectNode) objectMapper.readTree(infoMessage));
    }
    return policyViolationLogDTOObjectNodes;
  }

  public static void assertApplicationPolicyViolationData(List<ObjectNode> policyViolationLogDTOObjectNodes,
                                                          PolicyViolationLogEvent policyViolationLogEvent,
                                                          Organization organization,
                                                          Application application,
                                                          PolicyViolation policyViolation) throws Exception
  {
    ObjectNode policyViolationLogDTOObjectNode = policyViolationLogDTOObjectNodes.stream()
        .filter(objectNode -> objectNode.get("policyViolationId").asText().equals(policyViolation.getId())).findFirst()
        .orElse(null);
    assertThat(policyViolationLogDTOObjectNode)
        .as("No policy violation log DTO found with policyViolationId=" + policyViolation.getId()).isNotNull();
    assertApplicationPolicyViolationData(policyViolationLogDTOObjectNode, policyViolationLogEvent, organization,
        application, policyViolation);
  }

  public static void assertApplicationPolicyViolationData(ObjectNode policyViolationLogDTOObjectNode,
                                                          PolicyViolationLogEvent policyViolationLogEvent,
                                                          Organization organization,
                                                          Application application,
                                                          PolicyViolation policyViolation) throws Exception
  {
    PolicyViolationLogDTO policyViolationLogDTO = JsonUtils
        .asPojo(policyViolationLogDTOObjectNode, PolicyViolationLogDTO.class);
    assertEventData(policyViolationLogDTO, policyViolationLogEvent, policyViolation);
    assertPolicyViolationData(policyViolationLogDTO, policyViolation);
    assertStagePolicyActionData(policyViolationLogDTO, policyViolation);
    assertPolicyData(policyViolationLogDTO, policyViolation);
    assertOrganizationData(policyViolationLogDTO, organization);
    assertApplicationData(policyViolationLogDTO, application);
    assertComponentData(policyViolationLogDTOObjectNode, policyViolationLogDTO,
        policyViolation.getComponentIdentifier(), policyViolation.getHash());
  }

  public static void assertEventData(PolicyViolationLogDTO policyViolationLogDTO,
                                     PolicyViolationLogEvent policyViolationLogEvent,
                                     PolicyViolation policyViolation)
  {
    assertThat(policyViolationLogDTO.eventType).isEqualTo(policyViolationLogEvent.name().toLowerCase(Locale.ROOT));
    ZonedDateTime parsed = ZonedDateTime
        .parse(policyViolationLogDTO.eventTimestamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    ZonedDateTime open = ZonedDateTime
        .ofInstant(Instant.ofEpochMilli(policyViolation.getOpenTime().getTime()), ZoneId.systemDefault());
    assertThat(parsed).isEqualTo(open);
  }

  public static void assertPolicyViolationData(PolicyViolationLogDTO policyViolationLogDTO,
                                               PolicyViolation policyViolation)
  {
    assertThat(policyViolationLogDTO.policyViolationId).isEqualTo(policyViolation.getId());
    assertThat(policyViolationLogDTO.stageTypeId).isEqualTo(policyViolation.getStageTypeId());
  }

  public static void assertStagePolicyActionData(PolicyViolationLogDTO policyViolationLogDTO,
                                                 PolicyViolation policyViolation)
  {
    assertThat(policyViolationLogDTO.stagePolicyAction)
        .isEqualTo(policyViolation.getActionTypeId() == null ? "none" : policyViolation.getActionTypeId());
  }

  public static void assertPolicyData(PolicyViolationLogDTO policyViolationLogDTO, PolicyViolation policyViolation) {
    assertThat(policyViolationLogDTO.policyId).isEqualTo(policyViolation.getPolicyId());
    assertThat(policyViolationLogDTO.policyName).isEqualTo(policyViolation.getPolicyName());
    assertThat(policyViolationLogDTO.policyThreatCategory).isEqualTo(policyViolation.getThreatCategory().getName());
    assertThat(policyViolationLogDTO.policyThreatLevel).isEqualTo(policyViolation.getThreatLevel());
  }

  public static void assertOrganizationData(PolicyViolationLogDTO policyViolationLogDTO, Organization organization) {
    assertThat(policyViolationLogDTO.organizationId).isEqualTo(organization.getId());
    assertThat(policyViolationLogDTO.organizationName).isEqualTo(organization.getName());
  }

  public static void assertApplicationData(PolicyViolationLogDTO policyViolationLogDTO, Application application) {
    assertThat(policyViolationLogDTO.applicationId).isEqualTo(application.getId());
    assertThat(policyViolationLogDTO.applicationPublicId).isEqualTo(application.getPublicId());
    assertThat(policyViolationLogDTO.applicationName).isEqualTo(application.getName());
  }

  public static void assertComponentData(ObjectNode policyViolationLogDTOObjectNode,
                                         PolicyViolationLogDTO policyViolationLogDTO,
                                         ComponentIdentifier componentIdentifier,
                                         String componentHash)
  {
    if (componentIdentifier == null) {
      assertThat(policyViolationLogDTOObjectNode.has("componentIdentifier")).isFalse();
      assertThat(policyViolationLogDTO.componentIdentifier).isNull();
    }
    else {
      assertThat(policyViolationLogDTO.componentIdentifier).isEqualTo(componentIdentifier);
    }
    assertThat(policyViolationLogDTO.componentHash).isEqualTo(componentHash);
  }
}
