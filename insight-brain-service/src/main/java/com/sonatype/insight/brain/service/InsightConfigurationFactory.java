/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Validator;

import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.policy.violation.AbstractPolicyViolationLogger;
import com.sonatype.insight.brain.telemetry.UserTelemetryRequestLoggingFilter;

import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.logging.AbstractAppenderFactory;
import io.dropwizard.logging.AppenderFactory;
import io.dropwizard.logging.DefaultLoggingFactory;
import io.dropwizard.logging.filter.FilterFactory;
import io.dropwizard.request.logging.LogbackAccessRequestLogFactory;
import io.dropwizard.request.logging.RequestLogFactory;
import io.dropwizard.server.AbstractServerFactory;
import io.dropwizard.util.Duration;

public class InsightConfigurationFactory
    extends YamlConfigurationFactory<InsightConfig>
{
  static final String DEFAULT_REQUEST_LOG_FORMAT =
      "%clientHost %l %user [%date] \"%requestURL\" %statusCode %bytesSent %elapsedTime \"%header{User-Agent}\"";

  static final int DEFAULT_APPLICATION_PORT = 8070;

  static final int DEFAULT_ADMIN_PORT = 8071;

  static final Duration DEFAULT_IDLE_TIMEOUT = Duration.minutes(15);

  public InsightConfigurationFactory(final Class<InsightConfig> klass,
                                     final Validator validator,
                                     final ObjectMapper objectMapper,
                                     final String propertyPrefix)
  {
    super(klass, validator, objectMapper, propertyPrefix);
  }

  @Override
  public InsightConfig build(ConfigurationSourceProvider provider, String path)
      throws IOException, ConfigurationException
  {
    InsightConfig insightConfig = super.build(provider, path);
    setDefaultRequestLogSettings(insightConfig);
    setDefaultLogSettings(insightConfig);
    return insightConfig;
  }

  private void setDefaultRequestLogSettings(InsightConfig insightConfig) {
    RequestLogFactory<?> requestLogFactory = ((AbstractServerFactory) insightConfig.getServerFactory())
        .getRequestLogFactory();
    if (requestLogFactory instanceof LogbackAccessRequestLogFactory) {
      LogbackAccessRequestLogFactory logbackRequestLogFactory = (LogbackAccessRequestLogFactory) requestLogFactory;
      Collection<? extends AppenderFactory<IAccessEvent>> appenderFactories = logbackRequestLogFactory.getAppenders();

      setAppenderFactoriesLogFormats(appenderFactories, AbstractAppenderFactory.class, DEFAULT_REQUEST_LOG_FORMAT);
      setDefaultRequestLogFilterFactory(appenderFactories, new UserTelemetryRequestLoggingFilter());
    }
  }

  private void setAppenderFactoriesLogFormats(
      Collection<? extends AppenderFactory<?>> appenderFactories,
      @SuppressWarnings("rawtypes") Class<? extends AbstractAppenderFactory> appenderFactoryType,
      String logFormat)
  {
    appenderFactories.stream().filter(appenderFactoryType::isInstance).map(appenderFactoryType::cast)
        .filter(abtractAppenderFactory -> abtractAppenderFactory.getLogFormat() == null)
        .forEach(abtractAppenderFactory -> abtractAppenderFactory.setLogFormat(logFormat));
  }

  private void setDefaultRequestLogFilterFactory(Collection<? extends AppenderFactory<IAccessEvent>> appenderFactories,
                                                 FilterFactory<IAccessEvent> filterFactory)
  {
    for (AppenderFactory<IAccessEvent> appenderFac : appenderFactories) {
      AbstractAppenderFactory<IAccessEvent> appenderFactory = (AbstractAppenderFactory<IAccessEvent>) appenderFac;
      ImmutableList<FilterFactory<IAccessEvent>> existingFilters = appenderFactory.getFilterFactories();
      List<FilterFactory<IAccessEvent>> filters = new ArrayList<>(existingFilters);

      filters.add(filterFactory);

      appenderFactory.setFilterFactories(filters);
    }
  }

  private void setDefaultLogSettings(InsightConfig insightConfig) {
    DefaultLoggingFactory loggingFactory = (DefaultLoggingFactory) insightConfig.getLoggingFactory();
    ImmutableMap<String, JsonNode> loggerLevels = loggingFactory.getLoggers();
    Map<String, JsonNode> newLoggerLevels = new HashMap<>(loggerLevels);
    newLoggerLevels.putIfAbsent("com.sonatype.insight.brain.hds.UserTelemetryHdsClient",
        new TextNode(Level.INFO.toString()));

    setAuditLogSettings(newLoggerLevels);
    setPolicyViolationLogSettings(newLoggerLevels);
    loggingFactory.setLoggers(newLoggerLevels);
  }

  private void setAuditLogSettings(Map<String, JsonNode> loggers) {
    JsonNode auditLogger = loggers.putIfAbsent(AuditRecorder.BASE_LOGGER_NAME, createDefaultAuditLogger());
    if (auditLogger instanceof ObjectNode) {
      setRequiredLogSettings((ObjectNode) auditLogger);
    }
  }

  private void setPolicyViolationLogSettings(Map<String, JsonNode> loggers) {
    JsonNode policyViolationLogger = loggers
        .putIfAbsent(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME, new TextNode("OFF"));
    if (policyViolationLogger instanceof ObjectNode) {
      setRequiredLogSettings((ObjectNode) policyViolationLogger);
    }
  }

  private void setRequiredLogSettings(ObjectNode logger) {
    if (!logger.has("additive")) {
      logger.put("additive", false);
    }
    JsonNode logAppenders = logger.get("appenders");
    if (!(logAppenders instanceof ArrayNode)) {
      return;
    }
    for (int index = 0; index < logAppenders.size(); index++) {
      if (!(logAppenders.get(index) instanceof ObjectNode)) {
        continue;
      }
      ObjectNode logAppender = (ObjectNode) logAppenders.get(index);
      String type = logAppender.path("type").asText();
      if (!type.equals("file") && !type.equals("console") && !type.equals("syslog")) {
        continue;
      }
      logAppender.put("discardingThreshold", 0);
      if (!logAppender.has("logFormat")) {
        logAppender.put("logFormat", "%message%n");
      }
    }
  }

  private JsonNode createDefaultAuditLogger() {
    ObjectNode auditLogger = mapper.createObjectNode();
    ArrayNode auditLogAppenders = auditLogger.putArray("appenders");
    ObjectNode auditLogAppender = mapper.createObjectNode();
    auditLogAppender.put("type", "file");
    auditLogAppender.put("currentLogFilename", "./log/audit.log");
    auditLogAppender.put("archivedLogFilenamePattern", "./log/audit-%d.log.gz");
    auditLogAppender.put("archivedFileCount", 50);
    auditLogAppenders.add(auditLogAppender);
    setRequiredLogSettings(auditLogger);
    return auditLogger;
  }
}
