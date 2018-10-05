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
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.json.store.UncheckedIOException;
import com.sonatype.insight.test.LogOutput;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;

import static java.util.concurrent.TimeUnit.SECONDS;
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
  protected static final String EVALUATION_AUDIT_LOGGER = AuditRecorder.BASE_LOGGER_NAME + ".evaluation";

  @Rule
  public LogOutput logOutput = new LogOutput(AuditRecorder.BASE_LOGGER_NAME);

  @Before
  public void setupLogger() {
    logOutput.before();
  }

  protected List<String> awaitLogMessages(String logger, int count) {
    return await().atMost(5, SECONDS)
        .until(() -> logOutput.getInfoMessages(logger), hasSize(greaterThanOrEqualTo(count)));
  }

  private void assertEntryOrAbsentIfNullValue(Map<String, Object> map, String key, Object value) {
    if (value == null) {
      assertThat(map, not(hasKey(key)));
    }
    else {
      assertThat(map, hasEntry(key, value));
    }
  }

  protected static AuditDTO parseAuditLog(String auditLogEntry) {
    try {
      return JsonUtils.parse(auditLogEntry, AuditDTO.class);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  protected void assertEvaluationAuditLog(String error,
                                          String applicationId,
                                          String applicationPublicId,
                                          String applicationName,
                                          String stageId,
                                          String scanId,
                                          Boolean isReevaluation)
  {
    assertEvaluationAuditLog(awaitLogMessages(EVALUATION_AUDIT_LOGGER, 1).get(0), error, applicationId,
        applicationPublicId, applicationName, stageId, scanId, isReevaluation);
  }

  protected void assertEvaluationAuditLog(String message,
                                          String error,
                                          String applicationId,
                                          String applicationPublicId,
                                          String applicationName,
                                          String stageId,
                                          String scanId,
                                          Boolean isReevaluation)
  {
    assertEvaluationAuditLog(message, error, applicationId, applicationPublicId, applicationName, stageId, scanId,
        isReevaluation, not(isEmptyOrNullString()), not(isEmptyOrNullString()), not(isEmptyOrNullString()));
  }

  protected void assertEvaluationAuditLog(String message,
                                          String error,
                                          String applicationId,
                                          String applicationPublicId,
                                          String applicationName,
                                          String stageId,
                                          String scanId,
                                          Boolean isReevaluation,
                                          Matcher<? super String> matchWithUsername,
                                          Matcher<? super String> matchWithRemoteIp,
                                          Matcher<? super String> matchWithUserAgent)
  {
    AuditDTO auditDTO = parseAuditLog(message);
    assertThat(auditDTO.timestamp, not(isEmptyOrNullString()));
    assertThat(auditDTO.requestMethod, is(nullValue()));
    assertThat(auditDTO.requestUri, is(nullValue()));
    assertThat(auditDTO.remoteIpAddress, matchWithRemoteIp);
    assertThat(auditDTO.forwarded, is(nullValue()));
    assertThat(auditDTO.userAgent, matchWithUserAgent);
    assertThat(auditDTO.username, matchWithUsername);
    assertThat(auditDTO.domain, is(AuditEvent.EVALUATE_APPLICATION.getDomain()));
    assertThat(auditDTO.type, is(AuditEvent.EVALUATE_APPLICATION.getType()));
    assertThat(auditDTO.error, is(error));
    assertEntryOrAbsentIfNullValue(auditDTO.data, "applicationId", applicationId);
    assertEntryOrAbsentIfNullValue(auditDTO.data, "applicationPublicId", applicationPublicId);
    assertEntryOrAbsentIfNullValue(auditDTO.data, "applicationName", applicationName);
    assertEntryOrAbsentIfNullValue(auditDTO.data, "stageId", stageId);
    assertEntryOrAbsentIfNullValue(auditDTO.data, "scanId", scanId);
    assertEntryOrAbsentIfNullValue(auditDTO.data, "isReevaluation", isReevaluation);
  }
}
