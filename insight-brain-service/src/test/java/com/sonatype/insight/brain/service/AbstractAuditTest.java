/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.json.store.UncheckedIOException;
import com.sonatype.insight.test.LogOutput;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Rule;

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

public abstract class AbstractAuditTest
    extends AbstractResourceTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(AuditRecorder.BASE_LOGGER_NAME);

  public static final String SYSTEM_USER = MDCUsernameScope.SYSTEM;

  protected User unauthorizedUser;

  protected ObjectMapper objectMapper;

  @Before
  public void setupLogger() {
    logOutput.before();
    unauthorizedUser = tempEntity.newUser();
    objectMapper = new ObjectMapper();
  }

  protected Consumer<HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUser.getUsername(), unauthorizedUser.getPassword());
  }

  protected List<AuditDTO> awaitLogEntries(AuditEvent auditEvent, int count) {
    String loggerName = AuditRecorder.toLoggerName(auditEvent.getDomain());
    return await().atMost(5, SECONDS).until(
        () -> logOutput.getInfoMessages(loggerName).stream().map(AbstractAuditTest::parseAuditLog)
            .filter(dto -> auditEvent.getType().equals(dto.type)).collect(toList()),
        hasSize(greaterThanOrEqualTo(count)));
  }

  private static AuditDTO parseAuditLog(String auditLogEntry) {
    try {
      return JsonUtils.parse(auditLogEntry, AuditDTO.class);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  protected AuditDTO assertAuditLog(AuditEvent auditEvent, String error) {
    return assertAuditLogs(auditEvent, 1, error).get(0);
  }

  protected AuditDTO assertAuditLog(AuditEvent auditEvent, String error, String username) {
    return assertAuditLogs(auditEvent, 1, error, username).get(0);
  }

  protected List<AuditDTO> assertAuditLogs(AuditEvent auditEvent, int number, String error) {
    return assertAuditLogs(auditEvent, number, error, null);
  }

  protected List<AuditDTO> assertAuditLogs(AuditEvent auditEvent, int number, String error, String username) {
    List<AuditDTO> auditDTOs = awaitLogEntries(auditEvent, number);
    auditDTOs.forEach(auditDTO -> assertStandardData(auditDTO, auditEvent, error, username));
    return auditDTOs;
  }

  protected void assertStandardData(AuditDTO auditDTO, AuditEvent auditEvent, String error) {
    assertStandardData(auditDTO, auditEvent, error, null);
  }

  protected void assertStandardData(AuditDTO auditDTO, AuditEvent auditEvent, String error, String username) {
    boolean systemEvent = SYSTEM_USER.equals(username);
    if (username == null) {
      username = "unauthorized".equals(error) ? unauthorizedUser.getUsername() : User.ADMIN_USERNAME;
    }
    assertThat(auditDTO.domain, is(auditEvent.getDomain()));
    assertThat(auditDTO.type, is(auditEvent.getType()));
    assertThat(auditDTO.error, is(error));
    assertThat(auditDTO.timestamp, not(isEmptyOrNullString()));
    assertThat(auditDTO.requestMethod, is(nullValue()));
    assertThat(auditDTO.requestUri, is(nullValue()));
    assertThat(auditDTO.remoteIpAddress, systemEvent ? nullValue() : not(isEmptyOrNullString()));
    assertThat(auditDTO.forwarded, is(nullValue()));
    assertThat(auditDTO.userAgent, systemEvent ? nullValue() : not(isEmptyOrNullString()));
    assertThat(auditDTO.username, is(username));
  }

  protected void assertCustomData(AuditDTO auditDTO, String key, Object value) {
    assertThat(auditDTO.data, value == null ? not(hasKey(key)) : hasEntry(key, value));
  }

  protected void assertCustomObject(AuditDTO auditDTO, String key, Object pojo) {
    if (pojo instanceof Collection<?>) {
      assertCustomData(auditDTO, key, ((Collection<?>) pojo).stream().map(p -> objectMapper.convertValue(p, Map.class))
          .collect(Collectors.toList()));
    }
    else {
      assertCustomData(auditDTO, key, objectMapper.convertValue(pojo, Map.class));
    }
  }

  protected void assertOrganizationData(AuditDTO auditDTO, Organization organization) {
    assertOrganizationData(auditDTO, organization.getId(), organization.getName());
  }

  protected void assertOrganizationData(AuditDTO auditDTO, String organizationId, String organizationName) {
    assertCustomData(auditDTO, "organizationId", organizationId);
    assertCustomData(auditDTO, "organizationName", organizationName);
  }

  protected void assertApplicationData(AuditDTO auditDTO, Application application) {
    assertApplicationData(auditDTO, application.getId(), application.getPublicId(), application.getName());
  }

  protected void assertApplicationData(AuditDTO auditDTO,
                                       String applicationId,
                                       String applicationPublicId,
                                       String applicationName)
  {
    assertCustomData(auditDTO, "applicationId", applicationId);
    assertCustomData(auditDTO, "applicationPublicId", applicationPublicId);
    assertCustomData(auditDTO, "applicationName", applicationName);
  }

  protected void assertRepositoryData(AuditDTO auditDTO, Repository repository) {
    assertRepositoryData(auditDTO, repository.getId(), repository.getPublicId());
  }

  protected void assertRepositoryData(AuditDTO auditDTO, String repositoryId, String repositoryPublicId) {
    assertCustomData(auditDTO, "repositoryId", repositoryId);
    assertCustomData(auditDTO, "repositoryPublicId", repositoryPublicId);
  }

  protected void assertRepositoryContainerData(AuditDTO auditDTO) {
    assertThat(auditDTO.data, hasEntry("scope", "all-repositories"));
  }

  protected void assertGlobalData(AuditDTO auditDTO) {
    assertThat(auditDTO.data, hasEntry("scope", "global"));
  }

  protected void assertEvaluationAuditLog(String error,
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

  protected void assertEvaluationAuditLog(AuditDTO auditDTO,
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

  protected void assertEvaluationAuditLog(AuditDTO auditDTO,
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
}
