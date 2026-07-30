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
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
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

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

public class PolicyViolationLogDTOAssert
{
  private final RepositoryManagerDAO repositoryManagerDAO;

  public PolicyViolationLogDTOAssert(final RepositoryManagerDAO repositoryManagerDAO) {
    this.repositoryManagerDAO = repositoryManagerDAO;
  }

  public static List<PolicyViolationLogDTO> assertPolicyViolationLogDTOs(
      LogOutput logOutput,
      PolicyViolationLogEvent policyViolationLogEvent,
      int expected) throws Exception
  {
    List<String> infoMessages = logOutput.getInfoMessages(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);
    ObjectMapper objectMapper = new ObjectMapper();
    List<PolicyViolationLogDTO> policyViolationLogDTOs = new ArrayList<>();
    for (String infoMessage : infoMessages) {
      PolicyViolationLogDTO policyViolationLogDTO =
          JsonUtils.asPojo((ObjectNode) objectMapper.readTree(infoMessage), PolicyViolationLogDTO.class);
      if (policyViolationLogEvent == null
          || policyViolationLogEvent.name().toLowerCase(Locale.ROOT).equals(policyViolationLogDTO.eventType))
      {
        policyViolationLogDTOs.add(policyViolationLogDTO);
      }
    }
    assertThat(policyViolationLogDTOs).hasSize(expected);
    return policyViolationLogDTOs;
  }

  public static List<PolicyViolationLogDTO> assertPolicyViolationLogDTOs(
      LogOutput logOutput,
      int expected) throws Exception
  {
    return assertPolicyViolationLogDTOs(logOutput, null /* policyViolationLogEvent */, expected);
  }

  public static void assertApplicationPolicyViolationData(
      List<PolicyViolationLogDTO> policyViolationLogDTOs,
      PolicyViolationLogEvent policyViolationLogEvent,
      Organization organization,
      Application application,
      Date evaluationTime,
      List<PolicyViolation> policyViolations,
      String userName) throws Exception
  {
    assertApplicationPolicyViolationData(policyViolationLogDTOs, policyViolationLogEvent, organization, application,
        evaluationTime, evaluationTime, policyViolations, userName);
  }

  public static void assertApplicationPolicyViolationData(
      List<PolicyViolationLogDTO> policyViolationLogDTOs,
      PolicyViolationLogEvent policyViolationLogEvent,
      Organization organization,
      Application application,
      Date before,
      Date after,
      List<PolicyViolation> policyViolations,
      String userName) throws Exception
  {
    for (PolicyViolation policyViolation : policyViolations) {
      PolicyViolationLogDTO policyViolationLogDTO = policyViolationLogDTOs.stream()
          .filter(matchingDTO(policyViolation))
          .findFirst()
          .orElse(null);
      assertThat(policyViolationLogDTO)
          .as("No matching policy violation log DTO found for policyViolationId=" + policyViolation.getId())
          .isNotNull();
      assertApplicationPolicyViolationData(policyViolationLogDTO, policyViolationLogEvent, organization, application,
          before, after, policyViolation, userName);
    }
  }

  public static void assertOrganizationPolicyViolationData(
      PolicyViolationLogDTO policyViolationLogDTO,
      PolicyViolationLogEvent policyViolationLogEvent,
      Organization organization,
      Date before,
      Date after,
      String userName)
  {
    assertEventData(policyViolationLogDTO, policyViolationLogEvent, before, after);
    assertOrganizationData(policyViolationLogDTO, organization);
    assertUserData(policyViolationLogDTO, userName);
  }

  public static void assertApplicationPolicyViolationData(
      PolicyViolationLogDTO policyViolationLogDTO,
      PolicyViolationLogEvent policyViolationLogEvent,
      Organization organization,
      Application application,
      Date before,
      Date after)
  {
    assertEventData(policyViolationLogDTO, policyViolationLogEvent, before, after);
    assertOrganizationData(policyViolationLogDTO, organization);
    assertApplicationData(policyViolationLogDTO, application);
  }

  public void assertRepositoryPolicyViolationData(
      PolicyViolationLogDTO policyViolationLogDTO,
      PolicyViolationLogEvent policyViolationLogEvent,
      Repository repository,
      Date before,
      Date after)
  {
    assertEventData(policyViolationLogDTO, policyViolationLogEvent, before, after);
    assertRepositoryData(policyViolationLogDTO, repository);
  }

  public static void assertApplicationPolicyViolationData(
      PolicyViolationLogDTO policyViolationLogDTO,
      PolicyViolationLogEvent policyViolationLogEvent,
      Organization organization,
      Application application,
      Date before,
      Date after,
      PolicyViolation policyViolation,
      String userName)
  {
    assertEventData(policyViolationLogDTO, policyViolationLogEvent, before, after);
    assertThat(policyViolationLogDTO.stageTypeId).isEqualTo(policyViolation.getStageTypeId());
    assertStagePolicyActionData(policyViolationLogDTO, policyViolationLogEvent, policyViolation);
    assertPolicyData(policyViolationLogDTO, policyViolation);
    assertPolicyConditionTriggerData(policyViolationLogDTO, policyViolation);
    assertOrganizationData(policyViolationLogDTO, organization);
    assertApplicationData(policyViolationLogDTO, application);
    assertComponentData(policyViolationLogDTO, policyViolation.getComponentIdentifier(), policyViolation.getHash());
    assertUserData(policyViolationLogDTO, userName);
  }

  public static void assertApplicationPolicyViolationData(
      PolicyViolationLogDTO policyViolationLogDTO,
      PolicyViolationLogEvent policyViolationLogEvent,
      Organization organization,
      Application application,
      Date evaluationTime,
      PolicyViolation policyViolation,
      String userName) throws Exception
  {
    assertApplicationPolicyViolationData(policyViolationLogDTO, policyViolationLogEvent, organization, application,
        evaluationTime, evaluationTime, policyViolation, userName);
  }

  public void assertRepositoryPolicyViolationData(
      PolicyViolationLogDTO policyViolationLogDTO,
      PolicyViolationLogEvent policyViolationLogEvent,
      Repository repository,
      Date before,
      Date after,
      RepositoryPolicyViolation policyViolation,
      String userName)
  {
    if (PolicyViolationLogEvent.CREATE.equals(policyViolationLogEvent)) {
      assertEventData(policyViolationLogDTO, policyViolationLogEvent, policyViolation.getTime());
    }
    else {
      assertEventData(policyViolationLogDTO, policyViolationLogEvent, before, after);
    }
    assertThat(policyViolationLogDTO.stageTypeId).isEqualTo(StageTypes.PROXY.getId());
    assertStagePolicyActionData(policyViolationLogDTO, policyViolationLogEvent, policyViolation);
    assertPolicyData(policyViolationLogDTO, policyViolation);
    assertPolicyConditionTriggerData(policyViolationLogDTO, policyViolation);
    assertRepositoryData(policyViolationLogDTO, repository);
    assertComponentData(policyViolationLogDTO, policyViolation.getComponentIdentifier(), policyViolation.getHash());
    assertUserData(policyViolationLogDTO, userName);
  }

  public void assertRepositoryPolicyViolationData(
      List<PolicyViolationLogDTO> policyViolationLogDTOs,
      PolicyViolationLogEvent policyViolationLogEvent,
      Repository repository,
      Date before,
      Date after,
      List<RepositoryPolicyViolation> policyViolations,
      String userName) throws Exception
  {
    for (RepositoryPolicyViolation policyViolation : policyViolations) {
      PolicyViolationLogDTO policyViolationLogDTO = policyViolationLogDTOs.stream()
          .filter(matchingDTO(policyViolation))
          .findFirst()
          .orElse(null);
      assertThat(policyViolationLogDTO)
          .as("No matching policy violation log DTO found for policyViolationId=" + policyViolation.getId())
          .isNotNull();
      assertRepositoryPolicyViolationData(policyViolationLogDTO, policyViolationLogEvent, repository, before, after,
          policyViolation, userName);
    }
  }

  private static Predicate<PolicyViolationLogDTO> matchingDTO(PolicyViolation violation) {
    return dto -> violation.getOwnerId().equals(dto.applicationId) && isMatching(violation, dto);
  }

  private static Predicate<PolicyViolationLogDTO> matchingDTO(RepositoryPolicyViolation violation) {
    return dto -> violation.getRepositoryId().equals(dto.repositoryId) && isMatching(violation, dto);
  }

  private static boolean isMatching(AbstractPolicyViolation violation, PolicyViolationLogDTO dto) {
    if (!violation.getPolicyId().equals(dto.policyId) || !violation.getHash().equals(dto.componentHash)) {
      return false;
    }
    // If constraint facts are not loaded, we can't compare them - just match on policyId and hash
    if (!violation.constraintFactsAreLoaded()) {
      return true;
    }
    Set<String> violationReasons = violation.getConstraintFacts()
        .stream()
        .flatMap(constraintFact -> constraintFact.getConditionFacts().stream())
        .map(ConditionFact::getReason)
        .collect(toSet());
    Set<String> dtoReasons = dto.policyConditionTriggers == null
        ? Set.of()
        : dto.policyConditionTriggers.stream().map(trigger -> trigger.reason).collect(toSet());
    return violationReasons.equals(dtoReasons);
  }

  private static void assertEventData(
      PolicyViolationLogDTO policyViolationLogDTO,
      PolicyViolationLogEvent policyViolationLogEvent,
      Date eventTime)
  {
    assertThat(policyViolationLogDTO.eventType).isEqualTo(policyViolationLogEvent.name().toLowerCase(Locale.ROOT));
    ZonedDateTime parsed = ZonedDateTime
        .parse(policyViolationLogDTO.eventTimestamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    ZonedDateTime time = ZonedDateTime.ofInstant(Instant.ofEpochMilli(eventTime.getTime()), ZoneId.systemDefault());
    assertThat(parsed).isEqualTo(time);
  }

  private static void assertEventData(
      PolicyViolationLogDTO policyViolationLogDTO,
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

  private static void assertStagePolicyActionData(
      PolicyViolationLogDTO policyViolationLogDTO,
      PolicyViolationLogEvent policyViolationLogEvent,
      PolicyViolation policyViolation)
  {
    if (PolicyViolationLogEvent.CREATE.equals(policyViolationLogEvent) && !policyViolation.isLegacyViolation() &&
        !policyViolation.isWaived())
    {
      assertThat(policyViolationLogDTO.stagePolicyAction)
          .isEqualTo(policyViolation.getActionTypeId() == null ? "none" : policyViolation.getActionTypeId());
    }
    else {
      assertThat(policyViolationLogDTO.stagePolicyAction).isNull();
    }
  }

  private static void assertStagePolicyActionData(
      PolicyViolationLogDTO policyViolationLogDTO,
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

  private static void assertPolicyData(
      PolicyViolationLogDTO policyViolationLogDTO,
      AbstractPolicyViolation policyViolation)
  {
    assertThat(policyViolationLogDTO.policyId).isEqualTo(policyViolation.getPolicyId());
    assertThat(policyViolationLogDTO.policyName).isEqualTo(policyViolation.getPolicyName());
    assertThat(policyViolationLogDTO.policyThreatCategory).isEqualTo(policyViolation.getThreatCategory().getName());
    assertThat(policyViolationLogDTO.policyThreatLevel).isEqualTo(policyViolation.getThreatLevel());
  }

  private static void assertPolicyConditionTriggerData(
      PolicyViolationLogDTO policyViolationLogDTO,
      AbstractPolicyViolation policyViolation)
  {
    // If constraint facts are not loaded, we can't assert them - skip this check
    if (!policyViolation.constraintFactsAreLoaded()) {
      // When constraint facts are not loaded, the DTO should have null policyConditionTriggers
      assertThat(policyViolationLogDTO.policyConditionTriggers).isNull();
      return;
    }
    Set<String> expectedReasons = policyViolation.getConstraintFacts()
        .stream()
        .flatMap(constraintFact -> constraintFact.getConditionFacts().stream())
        .map(ConditionFact::getReason)
        .collect(Collectors.toSet());
    if (expectedReasons.isEmpty()) {
      assertThat(policyViolationLogDTO.policyConditionTriggers).isNullOrEmpty();
    }
    else {
      assertThat(policyViolationLogDTO.policyConditionTriggers).isNotNull();
      Set<String> actualReasons = policyViolationLogDTO.policyConditionTriggers.stream()
          .map(policyConditionTrigger -> policyConditionTrigger.reason)
          .collect(Collectors.toSet());
      assertThat(actualReasons).isEqualTo(expectedReasons);
    }
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

  private void assertRepositoryData(PolicyViolationLogDTO policyViolationLogDTO, Repository repository) {
    assertThat(policyViolationLogDTO.repositoryId).isEqualTo(repository.getId());
    assertThat(policyViolationLogDTO.repositoryPublicId).isEqualTo(repository.getPublicId());
    assertThat(policyViolationLogDTO.repositoryManagerId).isEqualTo(repository.getRepositoryManagerId());
    assertThat(policyViolationLogDTO.repositoryManagerInstanceId).isEqualTo(
        repositoryManagerDAO.getById(repository.getRepositoryManagerId()).getInstanceId());
    assertThat(policyViolationLogDTO.repositoryManagerName).isEqualTo(
        repositoryManagerDAO.getById(repository.getRepositoryManagerId()).getName());
  }

  private static void assertUserData(PolicyViolationLogDTO policyViolationLogDTO, String userName) {
    assertThat(policyViolationLogDTO.userName).isEqualTo(userName);
  }

  private static void assertComponentData(
      PolicyViolationLogDTO policyViolationLogDTO,
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
