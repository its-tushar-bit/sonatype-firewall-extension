/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.json.store.UncheckedIOException;
import com.sonatype.insight.test.LogOutput;

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

  protected User unauthorizedUser;

  @Before
  public void setupLogger() {
    logOutput.before();
    unauthorizedUser = tempEntity.newUser();
  }

  protected List<AuditDTO> awaitLogEntries(AuditEvent auditEvent, int count) {
    String loggerName = AuditRecorder.toLoggerName(auditEvent.getDomain());
    return await().atMost(5, SECONDS).until(
        () -> logOutput.getInfoMessages(loggerName).stream().map(AbstractAuditTest::parseAuditLog).collect(toList()),
        hasSize(greaterThanOrEqualTo(count)));
  }

  private void assertEntryOrAbsentIfNullValue(Map<String, Object> map, String key, Object value) {
    if (value == null) {
      assertThat(map, not(hasKey(key)));
    }
    else {
      assertThat(map, hasEntry(key, value));
    }
  }

  private static AuditDTO parseAuditLog(String auditLogEntry) {
    try {
      return JsonUtils.parse(auditLogEntry, AuditDTO.class);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  protected void assertStandardData(AuditDTO auditDTO, AuditEvent auditEvent, String error) {
    assertStandardData(auditDTO, auditEvent, error, User.ADMIN_USERNAME);
  }

  protected void assertStandardData(AuditDTO auditDTO, AuditEvent auditEvent, String error, String username) {
    boolean systemEvent = MDCUsernameScope.SYSTEM.equals(username);
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
        isReevaluation, User.ADMIN_USERNAME);
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
    assertEntryOrAbsentIfNullValue(auditDTO.data, "applicationId", applicationId);
    assertEntryOrAbsentIfNullValue(auditDTO.data, "applicationPublicId", applicationPublicId);
    assertEntryOrAbsentIfNullValue(auditDTO.data, "applicationName", applicationName);
    assertEntryOrAbsentIfNullValue(auditDTO.data, "stageId", stageId);
    assertEntryOrAbsentIfNullValue(auditDTO.data, "scanId", scanId);
    assertEntryOrAbsentIfNullValue(auditDTO.data, "isReevaluation", isReevaluation);
  }
}
