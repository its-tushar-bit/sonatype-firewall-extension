/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.service.InsightConfig;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.logging.DefaultLoggingFactory;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SupportServiceUnitTest
{
  private static final String AUDIT_LOG_GOOD =
      "{ \"appenders\": [{\"type\": \"file\", \"currentLogFilename\": \"audit1.log\" }] }";

  private static final String AUDIT_LOG_NO_FILE_TYPE =
      "{ \"appenders\": [{\"type\": \"console\" }] }";

  private static final String AUDIT_LOG_NO_FILE_NAME = "{ \"appenders\": [{\"type\": \"file\" }] }";

  private static final String AUDIT_LOG_NO_APPENDER = "{ \"appenders\": [] }";

  private static final String AUDIT_LOG_CONSOLE_THEN_FILE_TYPE =
      "{ \"appenders\": [{\"type\": \"console\" }, {\"type\": \"file\", \"currentLogFilename\": \"audit1.log\" }] }";

  private static final String AUDIT_LOG_TEXT_NODE = "\"INFO\"";

  @Test
  public void testGetAuditLog() throws Exception {
    File auditLog = SupportService.getAuditLog(getConfig(AUDIT_LOG_GOOD));
    assertThat(auditLog.getName()).isEqualTo("audit1.log");
  }

  @Test
  public void testGetAuditLog_NoAuditLogger() {
    File auditLog = SupportService.getAuditLog(new InsightConfig());
    assertThat(auditLog).isNull();
  }

  @Test
  public void testGetAuditLog_NoFileType() throws Exception {
    File auditLog = SupportService.getAuditLog(getConfig(AUDIT_LOG_NO_FILE_TYPE));
    assertThat(auditLog).isNull();
  }

  @Test
  public void testGetAuditLog_NoLogFileName() throws Exception {
    File auditLog = SupportService.getAuditLog(getConfig(AUDIT_LOG_NO_FILE_NAME));
    assertThat(auditLog).isNull();
  }

  @Test
  public void testGetAuditLog_NoAppender() throws Exception {
    File auditLog = SupportService.getAuditLog(getConfig(AUDIT_LOG_NO_APPENDER));
    assertThat(auditLog).isNull();
  }

  @Test
  public void testGetAuditLog_OnlyTextNode() throws Exception {
    File auditLog = SupportService.getAuditLog(getConfig(AUDIT_LOG_TEXT_NODE));
    assertThat(auditLog).isNull();
  }

  @Test
  public void testGetAuditLog_FileFollowsConsole() throws Exception {
    File auditLog = SupportService.getAuditLog(getConfig(AUDIT_LOG_CONSOLE_THEN_FILE_TYPE));
    assertThat(auditLog.getName()).isEqualTo("audit1.log");
  }

  private InsightConfig getConfig(String auditLogString) throws IOException {
    DefaultLoggingFactory loggingFactory = new DefaultLoggingFactory();
    loggingFactory.setLoggers(getAuditLoggers(auditLogString));
    InsightConfig config = new InsightConfig();
    config.setLoggingFactory(loggingFactory);
    return config;
  }

  private Map<String, JsonNode> getAuditLoggers(String auditLogString) throws IOException {
    Map<String, JsonNode> logger = new HashMap<>();
    JsonNode loggerNode = new ObjectMapper().readTree(auditLogString);
    logger.put(AuditRecorder.BASE_LOGGER_NAME, loggerNode);
    return logger;
  }
}
