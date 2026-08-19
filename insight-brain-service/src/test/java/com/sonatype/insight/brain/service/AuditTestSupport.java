/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.sonatype.insight.brain.audit.ApplicationAuditDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.audit.OrganizationAuditDTO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.OAuth2User;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.policy.ActionDTO;
import com.sonatype.insight.brain.policy.ConstraintDTO;
import com.sonatype.insight.brain.policy.NotificationDTO;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.test.LogOutput;

import com.fasterxml.jackson.databind.ObjectMapper;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public interface AuditTestSupport
{
  ObjectMapper JSON = new ObjectMapper();

  String SYSTEM_USER = MDCUsernameScope.SYSTEM;

  LogOutput getLogOutput();

  default String getUnauthorizedUsername() {
    return null;
  }

  default List<AuditDTO> awaitLogEntries(AuditEvent auditEvent, int count) {
    await("Expect audit event " + auditEvent).atMost(10, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(getLogEntries(auditEvent)).hasSizeGreaterThanOrEqualTo(count));
    return getLogEntries(auditEvent);
  }

  default List<AuditDTO> getLogEntries(AuditEvent auditEvent) {
    return getLogOutput().getInfoMessages(AuditRecorder.toLoggerName(auditEvent.getDomain()))
        .stream()
        .map(AuditTestSupport::parseAuditLog)
        .filter(dto -> auditEvent.getType().equals(dto.type))
        .collect(toCollection(ArrayList::new));
  }

  static AuditDTO parseAuditLog(String auditLogEntry) {
    try {
      return JSON.readValue(auditLogEntry, AuditDTO.class);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  default AuditDTO assertAuditLog(AuditEvent auditEvent, String error) {
    return assertAuditLogs(auditEvent, 1, error).get(0);
  }

  default AuditDTO assertAuditLog(AuditEvent auditEvent, String error, String username) {
    return assertAuditLogs(auditEvent, 1, error, username).get(0);
  }

  default List<AuditDTO> assertAuditLogs(AuditEvent auditEvent, int number, String error) {
    return assertAuditLogs(auditEvent, number, error, null);
  }

  default List<AuditDTO> assertAuditLogs(AuditEvent auditEvent, int number, String error, String username) {
    List<AuditDTO> auditDTOs = awaitLogEntries(auditEvent, number);
    auditDTOs.forEach(auditDTO -> assertStandardData(auditDTO, auditEvent, error, username));
    return auditDTOs;
  }

  default void assertStandardData(AuditDTO auditDTO, AuditEvent auditEvent, String error) {
    assertStandardData(auditDTO, auditEvent, error, null);
  }

  default void assertStandardData(AuditDTO auditDTO, AuditEvent auditEvent, String error, String username) {
    boolean systemEvent = SYSTEM_USER.equals(username);
    if (username == null) {
      username = "unauthorized".equals(error) ? getUnauthorizedUsername() : User.ADMIN_USERNAME;
    }
    assertThat(auditDTO.domain).isEqualTo(auditEvent.getDomain());
    assertThat(auditDTO.type).isEqualTo(auditEvent.getType());
    assertThat(auditDTO.error).isEqualTo(error);
    assertThat(auditDTO.timestamp).matches("2[0-9]{3}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}[-+0-9Z.:]+");
    assertThat(auditDTO.requestMethod).isNull();
    assertThat(auditDTO.requestUri).isNull();
    assertThat(auditDTO.forwarded).isNull();
    if (systemEvent) {
      assertThat(auditDTO.remoteIpAddress).isNull();
      assertThat(auditDTO.userAgent).isNull();
    }
    else {
      assertThat(auditDTO.remoteIpAddress).isNotEmpty();
      assertThat(auditDTO.userAgent).isNotEmpty();
    }
    assertThat(auditDTO.username).isEqualTo(username);
  }

  default void assertCustomData(AuditDTO auditDTO, String key, Object value) {
    if (value == null) {
      assertThat(auditDTO.data).doesNotContainKey(key);
    }
    else {
      assertThat(auditDTO.data).containsEntry(key, value);
    }
  }

  default void assertCustomObject(AuditDTO auditDTO, String key, Object pojo) {
    if (pojo instanceof Collection<?>) {
      assertCustomData(auditDTO, key,
          ((Collection<?>) pojo).stream().map(element -> JSON.convertValue(element, Map.class)).collect(toList()));
    }
    else {
      assertCustomData(auditDTO, key, JSON.convertValue(pojo, Map.class));
    }
  }

  default void assertOrganizationData(AuditDTO auditDTO, Organization organization) {
    assertOrganizationData(auditDTO, organization.getId(), organization.getName());
  }

  default void assertOrganizationData(AuditDTO auditDTO, String organizationId, String organizationName) {
    assertCustomData(auditDTO, "organizationId", organizationId);
    assertCustomData(auditDTO, "organizationName", organizationName);
  }

  default void assertOrganizationAndParentData(AuditDTO auditDTO, Organization childOrg, Organization parentOrg) {
    assertCustomData(auditDTO, "organizationId", childOrg.getId());
    assertCustomData(auditDTO, "organizationName", childOrg.getName());
    assertCustomData(auditDTO, "parentOrganizationId", parentOrg.getId());
    assertCustomData(auditDTO, "parentOrganizationName", parentOrg.getName());
  }

  default void assertApplicationData(AuditDTO auditDTO, Application application) {
    assertApplicationData(auditDTO, application.getId(), application.getPublicId(), application.getName());
  }

  default void assertOwnerData(AuditDTO auditDTO, Owner owner) {
    switch (owner.getType()) {
      case APPLICATION:
        assertApplicationData(auditDTO, (Application) owner);
        return;
      case ORGANIZATION:
        assertOrganizationData(auditDTO, (Organization) owner);
        return;
      case REPOSITORY:
        assertRepositoryData(auditDTO, (Repository) owner);
        return;
      case REPOSITORY_MANAGER:
        assertRepositoryManagerData(auditDTO, (RepositoryManager) owner);
        return;
      case REPOSITORY_CONTAINER:
        assertRepositoryContainerData(auditDTO);
        return;
      default:
        throw new IllegalArgumentException("unsupported owner type " + owner.getType());
    }
  }

  default void assertApplicationData(
      AuditDTO auditDTO,
      String applicationId,
      String applicationPublicId,
      String applicationName)
  {
    assertCustomData(auditDTO, "applicationId", applicationId);
    assertCustomData(auditDTO, "applicationPublicId", applicationPublicId);
    assertCustomData(auditDTO, "applicationName", applicationName);
  }

  default void assertRepositoryData(AuditDTO auditDTO, Repository repository) {
    assertCustomData(auditDTO, "repositoryId", repository.getId());
    assertCustomData(auditDTO, "repositoryPublicId", repository.getPublicId());
    assertCustomData(auditDTO, "format", repository.getFormat());
    assertCustomData(auditDTO, "type", repository.getRepositoryType().name());
    assertCustomData(auditDTO, "auditEnabled", repository.isAuditEnabled());
    assertCustomData(auditDTO, "quarantineEnabled", repository.isQuarantineEnabled());
    assertCustomData(auditDTO, "policyCompliantComponentSelectionEnabled",
        repository.isPolicyCompliantComponentSelectionEnabled());
    assertCustomData(auditDTO, "namespaceConfusionProtectionEnabled",
        repository.isNamespaceConfusionProtectionEnabled());
  }

  default void assertRepositoryContainerData(AuditDTO auditDTO) {
    assertThat(auditDTO.data).containsEntry("scope", "all-repositories");
  }

  default void assertGlobalData(AuditDTO auditDTO) {
    assertThat(auditDTO.data).containsEntry("scope", "global");
  }

  default void assertEvaluationAuditLog(
      String error,
      String applicationId,
      String applicationPublicId,
      String applicationName,
      String stageId,
      String scanId,
      Boolean isReevaluation)
  {
    assertEvaluationAuditLog(awaitLogEntries(AuditEvent.EVALUATE_APPLICATION, 1).get(0), error, applicationId,
        applicationPublicId, applicationName, stageId, scanId, isReevaluation);
  }

  default void assertEvaluationAuditLog(
      AuditDTO auditDTO,
      String error,
      String applicationId,
      String applicationPublicId,
      String applicationName,
      String stageId,
      String scanId,
      Boolean isReevaluation)
  {
    assertEvaluationAuditLog(auditDTO, error, applicationId, applicationPublicId, applicationName, stageId, scanId,
        isReevaluation, null);
  }

  default void assertEvaluationAuditLog(
      AuditDTO auditDTO,
      String error,
      String applicationId,
      String applicationPublicId,
      String applicationName,
      String stageId,
      String scanId,
      Boolean isReevaluation,
      String username)
  {
    assertStandardData(auditDTO, AuditEvent.EVALUATE_APPLICATION, error, username);
    assertApplicationData(auditDTO, applicationId, applicationPublicId, applicationName);
    assertCustomData(auditDTO, "stageId", stageId);
    assertCustomData(auditDTO, "scanId", scanId);
    assertCustomData(auditDTO, "isReevaluation", isReevaluation);
  }

  default void assertParentOrganizationData(final AuditDTO auditDTO, Organization organization) {
    assertCustomData(auditDTO, "parentOrganizationId", organization.getId());
    assertCustomData(auditDTO, "parentOrganizationName", organization.getName());
  }

  default AuditDTO findFirstByDataKeyValue(Collection<AuditDTO> auditDTOs, String dataKey, Object dataValue) {
    AuditDTO auditDTO = auditDTOs.stream().filter(a -> a.data.get(dataKey).equals(dataValue)).findFirst().orElse(null);
    assertThat(auditDTO).as("Failed to find an audit dto with " + dataKey + " equal to " + dataValue).isNotNull();
    return auditDTO;
  }

  default void assertSelectedApplications(AuditDTO auditDTO, ApplicationAuditDTO... expected) {
    ApplicationAuditDTO[] actuals = JSON.convertValue(auditDTO.data.get("selectedApplications"),
        ApplicationAuditDTO[].class);
    assertThat(actuals).containsExactlyInAnyOrder(expected);
  }

  default void assertSelectedOrganizations(AuditDTO auditDTO, OrganizationAuditDTO... expected) {
    OrganizationAuditDTO[] actuals = JSON.convertValue(auditDTO.data.get("selectedOrganizations"),
        OrganizationAuditDTO[].class);
    assertThat(actuals).containsExactlyInAnyOrder(expected);
  }

  default void assertUserData(AuditDTO auditDTO, User user) {
    assertUserData(auditDTO, User.INTERNAL_REALM_ID, user);
  }

  default void assertUserData(AuditDTO auditDTO, String realmId, User user) {
    assertUserData(auditDTO, realmId, user.getUsername(), user.getFirstName(), user.getLastName(), user.getEmail());
  }

  default void assertUserData(AuditDTO auditDTO, String realmId, SamlUser samlUser) {
    assertUserData(auditDTO, realmId, samlUser.getUsername(), samlUser.getFirstName(), samlUser.getLastName(),
        samlUser.getEmail());
  }

  default void assertUserData(AuditDTO auditDTO, String realmId, OAuth2User oAuth2User) {
    assertUserData(auditDTO, realmId, oAuth2User.getUsername(), oAuth2User.getFirstName(), oAuth2User.getLastName(),
        oAuth2User.getEmail());
  }

  default void assertUserData(
      AuditDTO auditDTO,
      String realmId,
      String username,
      String firstName,
      String lastName,
      String email)
  {
    assertCustomData(auditDTO, "realm", realmId);
    assertCustomData(auditDTO, "username", username);
    assertCustomData(auditDTO, "firstName", firstName);
    assertCustomData(auditDTO, "lastName", lastName);
    assertCustomData(auditDTO, "emailAddress", email);
  }

  default void assertPolicyData(final AuditDTO auditDTO, final Policy policy, boolean policyDeleted) {
    assertPolicyData(auditDTO, policy, policyDeleted, ConstraintDTO.transcribe(policy.getConstraints()));
  }

  default void assertPolicyData(
      AuditDTO auditDTO,
      Policy policy,
      boolean policyDeleted,
      List<ConstraintDTO> constraints)
  {
    String auditedPolicyId = (String) auditDTO.data.get("policyId");
    assertThat(auditedPolicyId).isNotNull();
    if (!policyDeleted) {
      assertThat(getPolicyDAO().getById(auditedPolicyId)).isNotNull();
    }
    else {
      assertThat(auditedPolicyId).isEqualTo(policy.getId());
    }
    assertCustomData(auditDTO, "policyName", policy.getName());
    assertCustomData(auditDTO, "policyThreatLevel", policy.getThreatLevel());
    assertCustomData(auditDTO, "legacyViolationMode",
        policy.isLegacyViolationAllowed() ? "allow" : "disallow");
    assertCustomData(auditDTO, "policyActionsOverrideMode",
        policy.isPolicyActionsOverrideAllowed() ? "allow" : "disallow");
    assertCustomObject(auditDTO, "policyConstraints", constraints);
    assertCustomObject(auditDTO, "actions", ActionDTO.transcribe(policy.getActions()));
    assertCustomObject(auditDTO, "notifications", NotificationDTO.transcribe(policy.getNotifications()));
  }

  default void assertRepositoryManagerData(AuditDTO auditDTO, RepositoryManager repositoryManager) {
    assertCustomData(auditDTO, "repositoryManagerId", repositoryManager.getId());
    assertCustomData(auditDTO, "repositoryManagerInstanceId", repositoryManager.getInstanceId());
    assertCustomData(auditDTO, "repositoryManagerName", repositoryManager.getName());
  }

  PolicyDAO getPolicyDAO();
}
