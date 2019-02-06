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
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.AbstractPolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.test.LogOutput;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;

public class PolicyViolationLogDTOAssert
{
  public static List<PolicyViolationLogDTO> assertPolicyViolationLogDTOs(LogOutput logOutput, int expected)
      throws Exception
  {
    List<String> infoMessages = logOutput.getInfoMessages(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);
    assertThat(infoMessages).hasSize(expected);
    ObjectMapper objectMapper = new ObjectMapper();
    List<PolicyViolationLogDTO> policyViolationLogDTOs = new ArrayList<>();
    for (String infoMessage : infoMessages) {
      PolicyViolationLogDTO policyViolationLogDTO =
          JsonUtils.asPojo((ObjectNode) objectMapper.readTree(infoMessage), PolicyViolationLogDTO.class);
      policyViolationLogDTOs.add(policyViolationLogDTO);
    }
    return policyViolationLogDTOs;
  }

  public static void assertApplicationPolicyViolationData(List<PolicyViolationLogDTO> policyViolationLogDTOs,
                                                          PolicyViolationLogEvent policyViolationLogEvent,
                                                          Organization organization,
                                                          Application application,
                                                          PolicyViolation policyViolation) throws Exception
  {
    PolicyViolationLogDTO policyViolationLogDTO = policyViolationLogDTOs.stream()
        .filter(dto -> dto.policyViolationId.equals(policyViolation.getId())).findFirst().orElse(null);
    assertThat(policyViolationLogDTO)
        .as("No policy violation log DTO found with policyViolationId=" + policyViolation.getId()).isNotNull();
    assertApplicationPolicyViolationData(policyViolationLogDTO, policyViolationLogEvent, organization, application,
        policyViolation);
  }

  public static void assertApplicationPolicyViolationData(PolicyViolationLogDTO policyViolationLogDTO,
                                                          PolicyViolationLogEvent policyViolationLogEvent,
                                                          Organization organization,
                                                          Application application,
                                                          PolicyViolation policyViolation) throws Exception
  {
    assertEventData(policyViolationLogDTO, policyViolationLogEvent,
        assertTime(policyViolationLogEvent, policyViolation));
    assertThat(policyViolationLogDTO.policyViolationId).isEqualTo(policyViolation.getId());
    assertThat(policyViolationLogDTO.stageTypeId).isEqualTo(policyViolation.getStageTypeId());
    assertStagePolicyActionData(policyViolationLogDTO, policyViolationLogEvent, policyViolation);
    assertPolicyData(policyViolationLogDTO, policyViolation);
    assertOrganizationData(policyViolationLogDTO, organization);
    assertApplicationData(policyViolationLogDTO, application);
    assertComponentData(policyViolationLogDTO, policyViolation.getComponentIdentifier(), policyViolation.getHash());
  }

  private static Date assertTime(PolicyViolationLogEvent policyViolationLogEvent, PolicyViolation policyViolation) {
    Date time = null;
    switch (policyViolationLogEvent) {
      case CREATE: {
        time = policyViolation.getOpenTime();
        break;
      }
      case FIX: {
        time = policyViolation.getFixTime();
        break;
      }
    }
    assertThat(time).isNotNull();
    return time;
  }

  public static void assertRepositoryPolicyViolationData(PolicyViolationLogDTO policyViolationLogDTO,
                                                         PolicyViolationLogEvent policyViolationLogEvent,
                                                         Repository repository,
                                                         Date before,
                                                         Date after,
                                                         RepositoryPolicyViolation policyViolation) throws Exception
  {
    if (PolicyViolationLogEvent.CREATE.equals(policyViolationLogEvent)) {
      assertEventData(policyViolationLogDTO, policyViolationLogEvent, policyViolation.getTime());
    }
    else {
      assertEventData(policyViolationLogDTO, policyViolationLogEvent, before, after);
    }
    assertThat(policyViolationLogDTO.policyViolationId).isEqualTo(policyViolation.getId());
    assertThat(policyViolationLogDTO.stageTypeId).isEqualTo(StageTypes.PROXY.getId());
    assertStagePolicyActionData(policyViolationLogDTO, policyViolationLogEvent, policyViolation);
    assertPolicyData(policyViolationLogDTO, policyViolation);
    assertRepositoryData(policyViolationLogDTO, repository);
    assertComponentData(policyViolationLogDTO, policyViolation.getComponentIdentifier(), policyViolation.getHash());
  }

  public static void assertRepositoryPolicyViolationData(List<PolicyViolationLogDTO> policyViolationLogDTOs,
                                                         PolicyViolationLogEvent policyViolationLogEvent,
                                                         Repository repository,
                                                         Date before,
                                                         Date after,
                                                         RepositoryPolicyViolation policyViolation) throws Exception
  {
    PolicyViolationLogDTO policyViolationLogDTO = policyViolationLogDTOs.stream()
        .filter(dto -> dto.policyViolationId.equals(policyViolation.getId())).findFirst().orElse(null);
    assertThat(policyViolationLogDTO)
        .as("No policy violation log DTO found with policyViolationId=" + policyViolation.getId()).isNotNull();
    assertRepositoryPolicyViolationData(policyViolationLogDTO, policyViolationLogEvent, repository, before, after,
        policyViolation);
  }

  private static void assertEventData(PolicyViolationLogDTO policyViolationLogDTO,
                                      PolicyViolationLogEvent policyViolationLogEvent,
                                      Date eventTime)
  {
    assertThat(policyViolationLogDTO.eventType).isEqualTo(policyViolationLogEvent.name().toLowerCase(Locale.ROOT));
    ZonedDateTime parsed = ZonedDateTime
        .parse(policyViolationLogDTO.eventTimestamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    ZonedDateTime time = ZonedDateTime.ofInstant(Instant.ofEpochMilli(eventTime.getTime()), ZoneId.systemDefault());
    assertThat(parsed).isEqualTo(time);
  }

  private static void assertEventData(PolicyViolationLogDTO policyViolationLogDTO,
                                      PolicyViolationLogEvent policyViolationLogEvent,
                                      Date before,
                                      Date after)
  {
    assertThat(policyViolationLogDTO.eventType).isEqualTo(policyViolationLogEvent.name().toLowerCase(Locale.ROOT));
    ZonedDateTime parsed =
        ZonedDateTime.parse(policyViolationLogDTO.eventTimestamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    ZonedDateTime beforeZonedDateTime =
        ZonedDateTime.ofInstant(Instant.ofEpochMilli(before.getTime()), ZoneId.systemDefault());
    ZonedDateTime afterZonedDateTime =
        ZonedDateTime.ofInstant(Instant.ofEpochMilli(after.getTime()), ZoneId.systemDefault());
    assertThat(parsed).isAfterOrEqualTo(beforeZonedDateTime);
    assertThat(parsed).isBeforeOrEqualTo(afterZonedDateTime);
  }

  private static void assertStagePolicyActionData(PolicyViolationLogDTO policyViolationLogDTO,
                                                  PolicyViolationLogEvent policyViolationLogEvent,
                                                  PolicyViolation policyViolation)
  {
    if (PolicyViolationLogEvent.CREATE.equals(policyViolationLogEvent) && !policyViolation.isGrandfathered() &&
        !policyViolation.isWaived()) {
      assertThat(policyViolationLogDTO.stagePolicyAction)
          .isEqualTo(policyViolation.getActionTypeId() == null ? "none" : policyViolation.getActionTypeId());
    }
    else {
      assertThat(policyViolationLogDTO.stagePolicyAction).isNull();
    }
  }

  private static void assertStagePolicyActionData(PolicyViolationLogDTO policyViolationLogDTO,
                                                  PolicyViolationLogEvent policyViolationLogEvent,
                                                  RepositoryPolicyViolation policyViolation)
  {
    if (PolicyViolationLogEvent.CREATE.equals(policyViolationLogEvent) && !policyViolation.isWaived()) {
      assertThat(policyViolationLogDTO.stagePolicyAction)
          .isEqualTo(policyViolation.getActionTypeId() == null ? "none" : policyViolation.getActionTypeId());
    }
    else {
      assertThat(policyViolationLogDTO.stagePolicyAction).isNull();
    }
  }

  private static void assertPolicyData(PolicyViolationLogDTO policyViolationLogDTO,
                                       AbstractPolicyViolation policyViolation)
  {
    assertThat(policyViolationLogDTO.policyId).isEqualTo(policyViolation.getPolicyId());
    assertThat(policyViolationLogDTO.policyName).isEqualTo(policyViolation.getPolicyName());
    assertThat(policyViolationLogDTO.policyThreatCategory).isEqualTo(policyViolation.getThreatCategory().getName());
    assertThat(policyViolationLogDTO.policyThreatLevel).isEqualTo(policyViolation.getThreatLevel());
  }

  private static void assertOrganizationData(PolicyViolationLogDTO policyViolationLogDTO, Organization organization) {
    assertThat(policyViolationLogDTO.organizationId).isEqualTo(organization.getId());
    assertThat(policyViolationLogDTO.organizationName).isEqualTo(organization.getName());
  }

  private static void assertApplicationData(PolicyViolationLogDTO policyViolationLogDTO, Application application) {
    assertThat(policyViolationLogDTO.applicationId).isEqualTo(application.getId());
    assertThat(policyViolationLogDTO.applicationPublicId).isEqualTo(application.getPublicId());
    assertThat(policyViolationLogDTO.applicationName).isEqualTo(application.getName());
  }

  private static void assertRepositoryData(PolicyViolationLogDTO policyViolationLogDTO, Repository repository) {
    assertThat(policyViolationLogDTO.repositoryId).isEqualTo(repository.getId());
    assertThat(policyViolationLogDTO.repositoryPublicId).isEqualTo(repository.getPublicId());
  }

  private static void assertComponentData(PolicyViolationLogDTO policyViolationLogDTO,
                                          ComponentIdentifier componentIdentifier,
                                          String componentHash)
  {
    if (componentIdentifier == null) {
      assertThat(policyViolationLogDTO.componentIdentifier).isNull();
    }
    else {
      assertThat(policyViolationLogDTO.componentIdentifier).isEqualTo(componentIdentifier);
    }
    assertThat(policyViolationLogDTO.componentHash).isEqualTo(componentHash);
  }
}
