/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.Validator;

import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.policy.violation.AbstractPolicyViolationLogger;
import com.sonatype.insight.brain.telemetry.UserTelemetryRequestLoggingFilter;

import ch.qos.logback.access.common.spi.IAccessEvent;
import com.fasterxml.jackson.databind.JsonMappingException.Reference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Sets;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationParsingException;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.logging.common.AbstractAppenderFactory;
import io.dropwizard.logging.common.AppenderFactory;
import io.dropwizard.logging.common.DefaultLoggingFactory;
import io.dropwizard.logging.common.filter.FilterFactory;
import io.dropwizard.request.logging.LogbackAccessRequestLogFactory;
import io.dropwizard.request.logging.RequestLogFactory;
import io.dropwizard.request.logging.old.LogbackClassicRequestLogFactory;
import io.dropwizard.core.server.AbstractServerFactory;
import io.dropwizard.util.Duration;

public class InsightConfigurationFactory
    extends YamlConfigurationFactory<InsightConfig>
{
  static final String DEFAULT_REQUEST_LOG_FORMAT =
      "%clientHost %l %user [%date] \"%requestURL\" %statusCode %bytesSent %elapsedTime \"%header{User-Agent}\"";

  static final int DEFAULT_APPLICATION_PORT = 8070;

  static final int DEFAULT_ADMIN_PORT = 8071;

  static final Duration DEFAULT_IDLE_TIMEOUT = Duration.minutes(15);

  static final String SUGGEST_UPDATE_CONFIG_EXCEPTION_MESSAGE =
      "\n================================================================================================================="
          +
          "\nYour configuration file contains properties that are only compatible with Nexus IQ Server version 1.42 and lower."
          +
          "\nUpdate your configuration file to be compatible with this version of Nexus IQ Server." +
          "\nRefer to our configuration update guide at:" +
          "\nhttps://links.sonatype.com/products/nxiq/doc/updating-your-configuration" +
          "\n=================================================================================================================";

  private static final Set<String> DROPWIZARD_062_PROPERTIES = Sets
      .newHashSet("http", "logging.console", "logging.file", "logging.syslog");

  static final String NO_CONFIGURATION_EXCEPTION_MESSAGE =
      "\n================================================================================================================="
          +
          "\nNo configuration file was specified/found." +
          "\nYou must provide the path to your configuration file." +
          "\nRefer to our help documentation at:" +
          "\nhttps://links.sonatype.com/products/nxiq/doc/iq-server-installation" +
          "\nhttps://links.sonatype.com/products/nxiq/doc/configuring" +
          "\n=================================================================================================================";

  public InsightConfigurationFactory(
      final Class<InsightConfig> klass,
      final Validator validator,
      final ObjectMapper objectMapper,
      final String propertyPrefix)
  {
    super(klass, validator, objectMapper, propertyPrefix);
  }

  @Override
  public InsightConfig build(
      ConfigurationSourceProvider provider,
      String path) throws IOException, ConfigurationException
  {
    if (!new File(path).exists()) {
      throw new RuntimeException(NO_CONFIGURATION_EXCEPTION_MESSAGE);
    }
    try {
      InsightConfig insightConfig = super.build(provider, path);
      setDefaultRequestLogSettings(insightConfig);
      setDefaultLogSettings(insightConfig);
      return insightConfig;
    }
    catch (ConfigurationParsingException e) {
      if (e.getCause() instanceof UnrecognizedPropertyException &&
          DROPWIZARD_062_PROPERTIES.contains(pathToString(((UnrecognizedPropertyException) e.getCause()).getPath())))
      {
        throw new RuntimeException(SUGGEST_UPDATE_CONFIG_EXCEPTION_MESSAGE, e);
      }
      throw e;
    }
  }

  private String pathToString(Collection<Reference> references) {
    return references == null
        ? null
        : references.stream().map(Reference::getFieldName).collect(Collectors.joining("."));
  }

  @Override
  public InsightConfig build() throws IOException, ConfigurationException {
    throw new RuntimeException(NO_CONFIGURATION_EXCEPTION_MESSAGE);
  }

  private void setDefaultRequestLogSettings(InsightConfig insightConfig) {
    RequestLogFactory<?> requestLogFactory = ((AbstractServerFactory) insightConfig.getServerFactory())
        .getRequestLogFactory();
    if (requestLogFactory instanceof LogbackAccessRequestLogFactory) {
      LogbackAccessRequestLogFactory logbackRequestLogFactory = (LogbackAccessRequestLogFactory) requestLogFactory;
      Collection<? extends AppenderFactory<IAccessEvent>> appenderFactories = logbackRequestLogFactory.getAppenders();

      setAppenderFactoriesLogFormats(appenderFactories, AbstractAppenderFactory.class, DEFAULT_REQUEST_LOG_FORMAT);
      setDefaultRequestLogFilterFactory(appenderFactories, new UserTelemetryRequestLoggingFilter());
      configureAsyncAppendersForNoLoss(appenderFactories);
    }
    else if (requestLogFactory instanceof LogbackClassicRequestLogFactory) {
      Collection<? extends AppenderFactory<?>> appenderFactories =
          ((LogbackClassicRequestLogFactory) requestLogFactory).getAppenders();
      configureAsyncAppendersForNoLoss(appenderFactories);
    }
  }

  private void configureAsyncAppendersForNoLoss(Collection<? extends AppenderFactory<?>> appenderFactories) {
    for (AppenderFactory<?> appenderFactory : appenderFactories) {
      if (appenderFactory instanceof AbstractAppenderFactory) {
        AbstractAppenderFactory<?> factory = (AbstractAppenderFactory<?>) appenderFactory;
        factory.setDiscardingThreshold(0);
        factory.setNeverBlock(false);
      }
    }
  }

  private void setAppenderFactoriesLogFormats(
      Collection<? extends AppenderFactory<?>> appenderFactories,
      @SuppressWarnings("rawtypes") Class<? extends AbstractAppenderFactory> appenderFactoryType,
      String logFormat)
  {
    appenderFactories.stream()
        .filter(appenderFactoryType::isInstance)
        .map(appenderFactoryType::cast)
        .filter(abtractAppenderFactory -> abtractAppenderFactory.getLogFormat() == null)
        .forEach(abtractAppenderFactory -> abtractAppenderFactory.setLogFormat(logFormat));
  }

  private void setDefaultRequestLogFilterFactory(
      Collection<? extends AppenderFactory<IAccessEvent>> appenderFactories,
      FilterFactory<IAccessEvent> filterFactory)
  {
    for (AppenderFactory<IAccessEvent> appenderFac : appenderFactories) {
      AbstractAppenderFactory<IAccessEvent> appenderFactory = (AbstractAppenderFactory<IAccessEvent>) appenderFac;
      List<FilterFactory<IAccessEvent>> existingFilters = appenderFactory.getFilterFactories();
      List<FilterFactory<IAccessEvent>> filters = new ArrayList<>(existingFilters);

      filters.add(filterFactory);

      appenderFactory.setFilterFactories(filters);
    }
  }

  private void setDefaultLogSettings(InsightConfig insightConfig) {
    DefaultLoggingFactory loggingFactory = (DefaultLoggingFactory) insightConfig.getLoggingFactory();
    Map<String, JsonNode> loggersByName = new HashMap<>(loggingFactory.getLoggers());
    setAuditLogSettings(loggersByName);
    setPolicyViolationLogSettings(loggersByName);
    setDefaultLogLevelsIfNotConfigured(loggersByName);
    configureAsyncAppendersForNoLoss(loggersByName);
    configureAsyncAppendersForNoLoss(loggingFactory.getAppenders());
    loggingFactory.setLoggers(loggersByName);
  }

  private void setDefaultLogLevelsIfNotConfigured(Map<String, JsonNode> loggers) {
    loggers.putIfAbsent("org.jooq.tools", new TextNode("WARN"));
    loggers.putIfAbsent("org.jooq.Constants", new TextNode("OFF"));
    loggers.putIfAbsent(
        "com.sonatype.insight.brain.policy.evaluator.queue.EvaluationQueueProducer", new TextNode("INFO"));
    loggers.putIfAbsent(
        "com.sonatype.insight.brain.policy.evaluator.queue.EvaluationQueueConsumer", new TextNode("INFO"));
  }

  private void configureAsyncAppendersForNoLoss(Map<String, JsonNode> loggers) {
    for (JsonNode logger : loggers.values()) {
      JsonNode appenders = logger.path("appenders");
      if (appenders.isArray()) {
        for (JsonNode appender : appenders) {
          if (appender.isObject()) {
            ((ObjectNode) appender).put("neverBlock", false).put("discardingThreshold", 0);
          }
        }
      }
    }
  }

  private void setAuditLogSettings(Map<String, JsonNode> loggers) {
    JsonNode auditLogger = loggers.putIfAbsent(AuditRecorder.BASE_LOGGER_NAME, createDefaultAuditLogger());
    if (auditLogger instanceof ObjectNode) {
      setIndependentJsonLogSettings((ObjectNode) auditLogger);
    }
  }

  private void setPolicyViolationLogSettings(Map<String, JsonNode> loggers) {
    JsonNode policyViolationLogger = loggers
        .putIfAbsent(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME, new TextNode("OFF"));
    if (policyViolationLogger instanceof ObjectNode) {
      setIndependentJsonLogSettings((ObjectNode) policyViolationLogger);
    }
  }

  private void setIndependentJsonLogSettings(ObjectNode logger) {
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
    setIndependentJsonLogSettings(auditLogger);
    return auditLogger;
  }
}
