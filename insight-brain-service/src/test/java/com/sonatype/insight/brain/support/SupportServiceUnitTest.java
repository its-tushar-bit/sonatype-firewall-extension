/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.util.HashMap;
import java.util.Map;

import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.policy.violation.AbstractPolicyViolationLogger;
import com.sonatype.insight.brain.service.InsightConfig;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.logging.DefaultLoggingFactory;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SupportServiceUnitTest
{
  private static final String LOG_GOOD =
      "{ \"appenders\": [{\"type\": \"file\", \"currentLogFilename\": \"name.log\" }] }";

  private static final String LOG_NO_FILE_TYPE = "{ \"appenders\": [{\"type\": \"console\" }] }";

  private static final String LOG_NO_FILE_NAME = "{ \"appenders\": [{\"type\": \"file\" }] }";

  private static final String LOG_NO_APPENDER = "{ \"appenders\": [] }";

  private static final String LOG_CONSOLE_THEN_FILE_TYPE =
      "{ \"appenders\": [{\"type\": \"console\" }, {\"type\": \"file\", \"currentLogFilename\": \"name.log\" }] }";

  private static final String LOG_TEXT_NODE = "\"INFO\"";

  @Test
  public void testGetAuditLog() throws Exception {
    assertThat(SupportService.getAuditLog(getAuditConfig(LOG_GOOD)).getName()).isEqualTo("name.log");
  }

  @Test
  public void testGetAuditLog_NoAuditLogger() {
    assertThat(SupportService.getAuditLog(new InsightConfig())).isNull();
  }

  @Test
  public void testGetAuditLog_NoFileType() throws Exception {
    assertThat(SupportService.getAuditLog(getAuditConfig(LOG_NO_FILE_TYPE))).isNull();
  }

  @Test
  public void testGetAuditLog_NoLogFileName() throws Exception {
    assertThat(SupportService.getAuditLog(getAuditConfig(LOG_NO_FILE_NAME))).isNull();
  }

  @Test
  public void testGetAuditLog_NoAppender() throws Exception {
    assertThat(SupportService.getAuditLog(getAuditConfig(LOG_NO_APPENDER))).isNull();
  }

  @Test
  public void testGetAuditLog_OnlyTextNode() throws Exception {
    assertThat(SupportService.getAuditLog(getAuditConfig(LOG_TEXT_NODE))).isNull();
  }

  @Test
  public void testGetAuditLog_FileFollowsConsole() throws Exception {
    assertThat(SupportService.getAuditLog(getAuditConfig(LOG_CONSOLE_THEN_FILE_TYPE)).getName()).isEqualTo("name.log");
  }

  @Test
  public void testGetPolicyViolationLog() throws Exception {
    assertThat(SupportService.getPolicyViolationLog(getPolicyViolationConfig(LOG_GOOD)).getName())
        .isEqualTo("name.log");
  }

  @Test
  public void testGetPolicyViolationLog_NoPolicyViolationLogger() {
    assertThat(SupportService.getPolicyViolationLog(new InsightConfig())).isNull();
  }

  @Test
  public void testGetPolicyViolationLog_NoFileType() throws Exception {
    assertThat(SupportService.getPolicyViolationLog(getPolicyViolationConfig(LOG_NO_FILE_TYPE))).isNull();
  }

  @Test
  public void testGetPolicyViolationLog_NoLogFileName() throws Exception {
    assertThat(SupportService.getPolicyViolationLog(getPolicyViolationConfig(LOG_NO_FILE_NAME))).isNull();
  }

  @Test
  public void testGetPolicyViolationLog_NoAppender() throws Exception {
    assertThat(SupportService.getPolicyViolationLog(getPolicyViolationConfig(LOG_NO_APPENDER))).isNull();
  }

  @Test
  public void testGetPolicyViolationLog_OnlyTextNode() throws Exception {
    assertThat(SupportService.getPolicyViolationLog(getPolicyViolationConfig(LOG_TEXT_NODE))).isNull();
  }

  @Test
  public void testGetPolicyViolationLog_FileFollowsConsole() throws Exception {
    assertThat(SupportService.getPolicyViolationLog(getPolicyViolationConfig(LOG_CONSOLE_THEN_FILE_TYPE)).getName())
        .isEqualTo("name.log");
  }

  private InsightConfig getAuditConfig(String logString) throws Exception {
    return getConfig(logString, AuditRecorder.BASE_LOGGER_NAME);
  }

  private InsightConfig getPolicyViolationConfig(String logString) throws Exception {
    return getConfig(logString, AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);
  }

  private InsightConfig getConfig(String logString, String loggerName) throws Exception {
    DefaultLoggingFactory loggingFactory = new DefaultLoggingFactory();
    Map<String, JsonNode> logger = new HashMap<>();
    JsonNode loggerNode = new ObjectMapper().readTree(logString);
    logger.put(loggerName, loggerNode);
    loggingFactory.setLoggers(logger);
    InsightConfig config = new InsightConfig();
    config.setLoggingFactory(loggingFactory);
    return config;
  }
}
