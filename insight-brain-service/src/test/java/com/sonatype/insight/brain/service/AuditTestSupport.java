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
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.security.User;
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
    await("Expect audit event " + auditEvent).atMost(5, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(getLogEntries(auditEvent)).hasSizeGreaterThanOrEqualTo(count));
    return getLogEntries(auditEvent);
  }

  default List<AuditDTO> getLogEntries(AuditEvent auditEvent) {
    return getLogOutput().getInfoMessages(AuditRecorder.toLoggerName(auditEvent.getDomain())).stream()
        .map(AuditTestSupport::parseAuditLog).filter(dto -> auditEvent.getType().equals(dto.type))
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

  default void assertApplicationData(AuditDTO auditDTO, Application application) {
    assertApplicationData(auditDTO, application.getId(), application.getPublicId(), application.getName());
  }

  default void assertApplicationData(AuditDTO auditDTO,
                                     String applicationId,
                                     String applicationPublicId,
                                     String applicationName)
  {
    assertCustomData(auditDTO, "applicationId", applicationId);
    assertCustomData(auditDTO, "applicationPublicId", applicationPublicId);
    assertCustomData(auditDTO, "applicationName", applicationName);
  }

  default void assertRepositoryData(AuditDTO auditDTO, Repository repository) {
    assertRepositoryData(auditDTO, repository.getId(), repository.getPublicId());
  }

  default void assertRepositoryData(AuditDTO auditDTO, String repositoryId, String repositoryPublicId) {
    assertCustomData(auditDTO, "repositoryId", repositoryId);
    assertCustomData(auditDTO, "repositoryPublicId", repositoryPublicId);
  }

  default void assertRepositoryContainerData(AuditDTO auditDTO) {
    assertThat(auditDTO.data).containsEntry("scope", "all-repositories");
  }

  default void assertGlobalData(AuditDTO auditDTO) {
    assertThat(auditDTO.data).containsEntry("scope", "global");
  }

  default void assertEvaluationAuditLog(String error,
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

  default void assertEvaluationAuditLog(AuditDTO auditDTO,
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

  default void assertEvaluationAuditLog(AuditDTO auditDTO,
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
    assertCustomData(auditDTO, "username", user.getUsername());
    assertCustomData(auditDTO, "firstName", user.getFirstName());
    assertCustomData(auditDTO, "lastName", user.getLastName());
    assertCustomData(auditDTO, "emailAddress", user.getEmail());
  }
}
