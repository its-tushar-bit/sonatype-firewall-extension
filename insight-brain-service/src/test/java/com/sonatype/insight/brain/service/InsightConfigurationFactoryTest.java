/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.policy.violation.AbstractPolicyViolationLogger;
import com.sonatype.insight.brain.search.SearchConfig;
import com.sonatype.insight.brain.search.LuceneSearchConfig;
import com.sonatype.insight.brain.search.SearchConfig.AwsHttpOpenSearchConfig;
import com.sonatype.insight.brain.search.SearchConfig.HttpOpenSearchConfig;
import com.sonatype.insight.brain.search.SearchMode;
import com.sonatype.insight.brain.telemetry.UserTelemetryRequestLoggingFilter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.databind.JsonNode;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.ConfigurationParsingException;
import io.dropwizard.core.server.DefaultServerFactory;
import io.dropwizard.core.server.ServerFactory;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jetty.ConnectorFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.jetty.HttpsConnectorFactory;
import io.dropwizard.logging.common.AbstractAppenderFactory;
import io.dropwizard.logging.common.AppenderFactory;
import io.dropwizard.logging.common.ConsoleAppenderFactory;
import io.dropwizard.logging.common.DefaultLoggingFactory;
import io.dropwizard.logging.common.FileAppenderFactory;
import io.dropwizard.logging.common.LoggerConfiguration;
import io.dropwizard.logging.common.SyslogAppenderFactory;
import io.dropwizard.request.logging.LogbackAccessRequestLogFactory;
import io.dropwizard.request.logging.old.LogbackClassicRequestLogFactory;
import io.dropwizard.util.Duration;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@Category(SlowTest.class)
public class InsightConfigurationFactoryTest
{
  private static final List<Class<?>> CONSOLE_FILE_SYSLOG_CLASSES = Arrays
      .asList(ConsoleAppenderFactory.class, FileAppenderFactory.class, SyslogAppenderFactory.class);

  private static final int DROPWIZARD_HTTPS_PORT = new HttpsConnectorFactory().getPort();

  @ClassRule
  public static TemporaryFolder tempDir = new TemporaryFolder();

  @Rule
  public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @AfterClass
  public static void resetLogging() throws Exception {
    build("/config-test.yml");
    new File("log/audit.log").delete();
    new File("log/policy-violation.log").delete();
  }

  @Test
  public void testBuild_ConfigRequestLogFilterFactories_UsesUserTelemetryRequestLoggingFilter() throws Exception {
    InsightConfig insightConfig = build("config-without-logback-access-request-log-formats.yml");
    DefaultServerFactory serverFactory = assertDefaultServerFactory(insightConfig);
    LogbackAccessRequestLogFactory requestLogFactory =
        (LogbackAccessRequestLogFactory) serverFactory.getRequestLogFactory();

    assertAppenderFactoryFilterFactories(requestLogFactory, UserTelemetryRequestLoggingFilter.class);
  }

  @Test
  public void testBuild_ConfigWithLogbackAccessRequestAppendersWithoutLogFormats_UsesOurRequestLogFormat() throws Exception {
    InsightConfig insightConfig = build("config-without-logback-access-request-log-formats.yml");

    assertAppenderFactories(((LogbackAccessRequestLogFactory) ((DefaultServerFactory) insightConfig.getServerFactory())
        .getRequestLogFactory()).getAppenders(), Arrays.asList(InsightConfigurationFactory.DEFAULT_REQUEST_LOG_FORMAT,
            InsightConfigurationFactory.DEFAULT_REQUEST_LOG_FORMAT,
            InsightConfigurationFactory.DEFAULT_REQUEST_LOG_FORMAT));
  }

  @Test
  public void testBuild_ConfigWithLogbackAccessRequestAppendersWithLogFormats_SetsNothing() throws Exception {
    InsightConfig insightConfig = build("config-with-logback-access-request-log-formats.yml");

    assertAppenderFactories(((LogbackAccessRequestLogFactory) ((DefaultServerFactory) insightConfig.getServerFactory())
        .getRequestLogFactory()).getAppenders(),
        Arrays.asList("consoleRequestLogFormat", "fileRequestLogFormat", "syslogRequestLogFormat"));
  }

  @Test
  public void testBuild_ConfigWithLogbackClassicRequestAppendersWithoutLogFormats_UsesDropwizardRequestLogFormats() throws Exception {
    InsightConfig insightConfig = build("config-without-logback-classic-request-log-formats.yml");

    assertAppenderFactories(((LogbackClassicRequestLogFactory) ((DefaultServerFactory) insightConfig.getServerFactory())
        .getRequestLogFactory()).getAppenders(), Arrays
            .asList(new ConsoleAppenderFactory<>().getLogFormat(), new FileAppenderFactory<>().getLogFormat(),
                new SyslogAppenderFactory().getLogFormat()));
  }

  @Test
  public void testBuild_ConfigWithLogbackClassicRequestAppendersWithLogFormats_SetsNothing() throws Exception {
    InsightConfig insightConfig = build("config-with-logback-classic-request-log-formats.yml");

    assertAppenderFactories(((LogbackClassicRequestLogFactory) ((DefaultServerFactory) insightConfig.getServerFactory())
        .getRequestLogFactory()).getAppenders(),
        Arrays.asList("consoleRequestLogFormat", "fileRequestLogFormat", "syslogRequestLogFormat"));
  }

  @SuppressWarnings("rawtypes")
  @Test
  public void testBuild_ConfigWithServerAppendersWithoutLogFormats_UsesDropwizardLogFormats() throws Exception {
    InsightConfig insightConfig = build("config-without-server-log-formats.yml");

    assertAppenderFactories(((DefaultLoggingFactory) insightConfig.getLoggingFactory()).getAppenders(), Arrays
        .asList(new ConsoleAppenderFactory().getLogFormat(), new FileAppenderFactory().getLogFormat(),
            new SyslogAppenderFactory().getLogFormat()));
  }

  @Test
  public void testBuild_ConfigWithServerAppendersWithLogFormats_SetsNothing() throws Exception {
    InsightConfig insightConfig = build("config-with-server-log-formats.yml");

    assertAppenderFactories(((DefaultLoggingFactory) insightConfig.getLoggingFactory()).getAppenders(),
        Arrays.asList("consoleServerLogFormat", "fileServerLogFormat", "syslogServerLogFormat"));
  }

  @Test
  public void testBuild_NoServer_UsesOurDefaultConnectors() throws Exception {
    InsightConfig insightConfig = build("config-no-server.yml");

    DefaultServerFactory defaultServerFactory = assertDefaultServerFactory(insightConfig);
    assertConnector(defaultServerFactory.getApplicationConnectors(), HttpConnectorFactory.class,
        InsightConfigurationFactory.DEFAULT_APPLICATION_PORT, InsightConfigurationFactory.DEFAULT_IDLE_TIMEOUT);
    assertConnector(defaultServerFactory.getAdminConnectors(), HttpConnectorFactory.class,
        InsightConfigurationFactory.DEFAULT_ADMIN_PORT, InsightConfigurationFactory.DEFAULT_IDLE_TIMEOUT);
  }

  @Test
  public void testBuild_EmptyServer_UsesOurDefaultConnectors() throws Exception {
    InsightConfig insightConfig = build("config-empty-server.yml");

    DefaultServerFactory defaultServerFactory = assertDefaultServerFactory(insightConfig);
    assertConnector(defaultServerFactory.getApplicationConnectors(), HttpConnectorFactory.class,
        InsightConfigurationFactory.DEFAULT_APPLICATION_PORT, InsightConfigurationFactory.DEFAULT_IDLE_TIMEOUT);
    assertConnector(defaultServerFactory.getAdminConnectors(), HttpConnectorFactory.class,
        InsightConfigurationFactory.DEFAULT_ADMIN_PORT, InsightConfigurationFactory.DEFAULT_IDLE_TIMEOUT);
  }

  @Test
  public void testBuild_HttpConnectorsWithoutSettings_UseOurDefaultSettings() throws Exception {
    InsightConfig insightConfig = build("config-http-connectors-without-settings.yml");

    DefaultServerFactory defaultServerFactory = assertDefaultServerFactory(insightConfig);
    assertConnector(defaultServerFactory.getApplicationConnectors(), HttpConnectorFactory.class,
        InsightConfigurationFactory.DEFAULT_APPLICATION_PORT, InsightConfigurationFactory.DEFAULT_IDLE_TIMEOUT);
    assertConnector(defaultServerFactory.getAdminConnectors(), HttpConnectorFactory.class,
        InsightConfigurationFactory.DEFAULT_APPLICATION_PORT, InsightConfigurationFactory.DEFAULT_IDLE_TIMEOUT);
  }

  @Test
  public void testBuild_HttpConnectorsWithSettings_UseGivenSettings() throws Exception {
    InsightConfig insightConfig = build("config-http-connectors-with-settings.yml");

    DefaultServerFactory defaultServerFactory = assertDefaultServerFactory(insightConfig);
    assertConnector(defaultServerFactory.getApplicationConnectors(), HttpConnectorFactory.class, 9070,
        Duration.minutes(30));
    assertConnector(defaultServerFactory.getAdminConnectors(), HttpConnectorFactory.class, 9071, Duration.minutes(30));
  }

  @Test
  public void testBuild_HttpsConnectorsWithoutSettings_UseOurIdleTimeout() throws Exception {
    InsightConfig insightConfig = build("config-https-connectors-without-settings.yml");

    DefaultServerFactory defaultServerFactory = assertDefaultServerFactory(insightConfig);
    assertConnector(defaultServerFactory.getApplicationConnectors(), HttpsConnectorFactory.class, DROPWIZARD_HTTPS_PORT,
        InsightConfigurationFactory.DEFAULT_IDLE_TIMEOUT);
    assertConnector(defaultServerFactory.getAdminConnectors(), HttpsConnectorFactory.class, DROPWIZARD_HTTPS_PORT,
        InsightConfigurationFactory.DEFAULT_IDLE_TIMEOUT);
  }

  @Test
  public void testBuild_HttpsConnectorsWithSettings_UseGivenSettings() throws Exception {
    InsightConfig insightConfig = build("config-https-connectors-with-settings.yml");

    DefaultServerFactory defaultServerFactory = assertDefaultServerFactory(insightConfig);
    assertConnector(defaultServerFactory.getApplicationConnectors(), HttpsConnectorFactory.class, 9070,
        Duration.minutes(30));
    assertConnector(defaultServerFactory.getAdminConnectors(), HttpsConnectorFactory.class, 9071, Duration.minutes(30));
  }

  @Test
  public void testBuild_NoAuditLogSettings_UseDefault() throws Exception {
    InsightConfig insightConfig = build("config-no-server.yml");

    DefaultLoggingFactory defaultLoggingFactory = (DefaultLoggingFactory) insightConfig.getLoggingFactory();
    JsonNode auditLogger = defaultLoggingFactory.getLoggers().get(AuditRecorder.BASE_LOGGER_NAME);
    LoggerConfiguration loggerConfiguration = Jackson.newObjectMapper()
        .treeToValue(auditLogger, LoggerConfiguration.class);
    assertThat(loggerConfiguration.getLevel()).isEqualTo(Level.INFO.toString());
    assertThat(loggerConfiguration.isAdditive()).isFalse();
    assertThat(loggerConfiguration.getAppenders()).hasSize(1);
    assertThat(loggerConfiguration.getAppenders().get(0)).isInstanceOf(FileAppenderFactory.class);
    FileAppenderFactory<ILoggingEvent> auditLogAppenderFactory =
        (FileAppenderFactory<ILoggingEvent>) loggerConfiguration.getAppenders().get(0);
    assertThat(auditLogAppenderFactory.getCurrentLogFilename()).isEqualTo("./log/audit.log");
    assertThat(auditLogAppenderFactory.getLogFormat()).isEqualTo("%message%n");
    assertThat(auditLogAppenderFactory.getDiscardingThreshold()).isEqualTo(0);
    assertThat(auditLogAppenderFactory.getArchivedLogFilenamePattern()).isEqualTo("./log/audit-%d.log.gz");
    assertThat(auditLogAppenderFactory.getArchivedFileCount()).isEqualTo(50);
  }

  @Test
  public void testBuild_NoPolicyViolationLogSettings_NotEnabled() throws Exception {
    InsightConfig insightConfig = build("config-no-server.yml");

    DefaultLoggingFactory defaultLoggingFactory = (DefaultLoggingFactory) insightConfig.getLoggingFactory();
    JsonNode policyViolationLogger = defaultLoggingFactory.getLoggers()
        .get(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);
    assertThat(policyViolationLogger).isNotNull();
    assertThat(policyViolationLogger.asText()).isEqualTo("OFF");
  }

  @Test
  public void testBuild_JooqToolsNotConfigured_DefaultsToWarn() throws Exception {
    InsightConfig insightConfig = build("config-no-server.yml");

    DefaultLoggingFactory defaultLoggingFactory = (DefaultLoggingFactory) insightConfig.getLoggingFactory();
    JsonNode jooqToolsLogger = defaultLoggingFactory.getLoggers().get("org.jooq.tools");
    assertThat(jooqToolsLogger).isNotNull();
    assertThat(jooqToolsLogger.asText()).isEqualTo("WARN");
  }

  @Test
  public void testBuild_JooqToolsConfigured_KeepsExistingLevel() throws Exception {
    InsightConfig insightConfig = build("config-with-jooq-tools-log-level.yml");

    DefaultLoggingFactory defaultLoggingFactory = (DefaultLoggingFactory) insightConfig.getLoggingFactory();
    JsonNode jooqToolsLogger = defaultLoggingFactory.getLoggers().get("org.jooq.tools");
    assertThat(jooqToolsLogger).isNotNull();
    assertThat(jooqToolsLogger.asText()).isEqualTo("DEBUG");
  }

  @Test
  public void testBuild_AuditLogOnlyLevelSetting_NoError() throws Exception {
    build("config-audit-text-node.yml");
  }

  @Test
  public void testBuild_PolicyViolationLogOnlyLevelSetting_NoError() throws Exception {
    build("config-policy-violation-log-text-node.yml");
  }

  @Test
  public void testBuild_AuditLogSettings_Empty() throws Exception {
    build("config-audit-empty.yml");
  }

  @Test
  public void testBuild_PolicyViolationLogSettings_Empty() throws Exception {
    build("config-policy-violation-log-empty.yml");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBuild_AuditLogSettings_NonObjectAppender() throws Exception {
    build("config-audit-non-object-appender.yml");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBuild_PolicyViolationLogSettings_NonObjectAppender() throws Exception {
    build("config-policy-violation-log-non-object-appender.yml");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBuild_AuditLogSettings_EmptyObjectAppender() throws Exception {
    build("config-audit-empty-appender.yml");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBuild_PolicyViolationLogSettings_EmptyObjectAppender() throws Exception {
    build("config-policy-violation-log-empty-appender.yml");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBuild_AuditLogSettings_NonTextType() throws Exception {
    build("config-audit-non-text-type.yml");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBuild_PolicyViolationLogSettings_NonTextType() throws Exception {
    build("config-policy-violation-log-non-text-type.yml");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBuild_AuditLogSettings_NonStandardAppender() throws Exception {
    build("config-audit-non-standard-appender.yml");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBuild_PolicyViolationLogSettings_NonStandardAppender() throws Exception {
    build("config-policy-violation-log-non-standard-appender.yml");
  }

  @Test
  public void testBuild_AuditLogSettings_MissingRequired() throws Exception {
    assertRequiredLogSettings(build("config-audit-missing-required.yml"), AuditRecorder.BASE_LOGGER_NAME);
  }

  @Test
  public void testBuild_PolicyViolationLogSettings_MissingRequired() throws Exception {
    assertRequiredLogSettings(build("config-policy-violation-log-missing-required.yml"),
        AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);
  }

  private void assertRequiredLogSettings(InsightConfig insightConfig, String baseLoggerName) throws Exception {
    DefaultLoggingFactory defaultLoggingFactory = (DefaultLoggingFactory) insightConfig.getLoggingFactory();
    JsonNode logger = defaultLoggingFactory.getLoggers().get(baseLoggerName);
    LoggerConfiguration loggerConfiguration = Jackson.newObjectMapper().treeToValue(logger, LoggerConfiguration.class);
    assertThat(loggerConfiguration.isAdditive()).isFalse();
    assertThat(loggerConfiguration.getAppenders()).hasSize(3);
    for (AppenderFactory<ILoggingEvent> appenderFactory : loggerConfiguration.getAppenders()) {
      assertThat(appenderFactory).isInstanceOf(AbstractAppenderFactory.class);
      AbstractAppenderFactory<ILoggingEvent> abstractAppenderFactory =
          (AbstractAppenderFactory<ILoggingEvent>) appenderFactory;
      assertThat(abstractAppenderFactory.getDiscardingThreshold()).isEqualTo(0);
      assertThat(abstractAppenderFactory.getLogFormat()).isEqualTo("%message%n");
    }
  }

  @Test
  public void testBuild_AuditLogSettings_OverridesDiscardingThreshold() throws Exception {
    assertOverridesDiscardingThreshold(build("config-audit-overrides-discarding-threshold.yml"),
        AuditRecorder.BASE_LOGGER_NAME);
  }

  @Test
  public void testBuild_PolicyViolationLogSettings_OverridesDiscardingThreshold() throws Exception {
    assertOverridesDiscardingThreshold(build("config-policy-violation-log-overrides-discarding-threshold.yml"),
        AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);
  }

  private void assertOverridesDiscardingThreshold(InsightConfig insightConfig, String baseLoggerName) throws Exception {
    DefaultLoggingFactory defaultLoggingFactory = (DefaultLoggingFactory) insightConfig.getLoggingFactory();
    JsonNode logger = defaultLoggingFactory.getLoggers().get(baseLoggerName);
    LoggerConfiguration loggerConfiguration = Jackson.newObjectMapper().treeToValue(logger, LoggerConfiguration.class);
    assertThat(loggerConfiguration.isAdditive()).isTrue();
    assertThat(loggerConfiguration.getAppenders()).hasSize(3);
    for (AppenderFactory<ILoggingEvent> appenderFactory : loggerConfiguration.getAppenders()) {
      assertThat(appenderFactory).isInstanceOf(AbstractAppenderFactory.class);
      AbstractAppenderFactory<ILoggingEvent> abstractAppenderFactory =
          (AbstractAppenderFactory<ILoggingEvent>) appenderFactory;
      assertThat(abstractAppenderFactory.getDiscardingThreshold()).isEqualTo(0);
      assertThat(abstractAppenderFactory.getLogFormat()).isEqualTo("logFormat");
    }
  }

  @Test
  public void testBuild_PolicyViolationLogSettings_DefaultConfiguration() throws Exception {
    InsightConfig insightConfig = build("config-policy-violation-log-default.yml");

    DefaultLoggingFactory defaultLoggingFactory = (DefaultLoggingFactory) insightConfig.getLoggingFactory();
    JsonNode logger = defaultLoggingFactory.getLoggers()
        .get(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);
    LoggerConfiguration loggerConfiguration = Jackson.newObjectMapper().treeToValue(logger, LoggerConfiguration.class);
    assertThat(loggerConfiguration.getAppenders()).hasSize(1);
    assertThat(loggerConfiguration.getAppenders().get(0)).isInstanceOf(FileAppenderFactory.class);
    FileAppenderFactory<ILoggingEvent> fileAppenderFactory = (FileAppenderFactory<ILoggingEvent>) loggerConfiguration
        .getAppenders()
        .get(0);
    assertThat(fileAppenderFactory.getCurrentLogFilename()).isEqualTo("./log/policy-violation.log");
    assertThat(fileAppenderFactory.getArchivedLogFilenamePattern()).isEqualTo("./log/policy-violation-%d.log.gz");
    assertThat(fileAppenderFactory.getArchivedFileCount()).isEqualTo(5);
  }

  @Test
  public void testBuild_NoArguments() {
    InsightBrainService insightBrainService = new InsightBrainService();
    Bootstrap<InsightConfig> bootstrap = new Bootstrap<>(insightBrainService);
    insightBrainService.initialize(bootstrap);
    ConfigurationFactory<InsightConfig> configurationFactory = bootstrap.getConfigurationFactoryFactory()
        .create(InsightConfig.class, bootstrap.getValidatorFactory().getValidator(), bootstrap.getObjectMapper(), "dw");

    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(configurationFactory::build)
        .withMessage(InsightConfigurationFactory.NO_CONFIGURATION_EXCEPTION_MESSAGE);
  }

  @Test
  public void testBuild_MissingConfigurationFile() {
    InsightBrainService insightBrainService = new InsightBrainService();
    Bootstrap<InsightConfig> bootstrap = new Bootstrap<>(insightBrainService);
    insightBrainService.initialize(bootstrap);
    ConfigurationFactory<InsightConfig> configurationFactory = bootstrap.getConfigurationFactoryFactory()
        .create(InsightConfig.class, bootstrap.getValidatorFactory().getValidator(), bootstrap.getObjectMapper(), "dw");

    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> configurationFactory.build(bootstrap.getConfigurationSourceProvider(), "doesNotExist"))
        .withMessage(InsightConfigurationFactory.NO_CONFIGURATION_EXCEPTION_MESSAGE);
  }

  @Test
  public void testBuild_ConfigWithHttp_SuggestsUpdateConfig() {
    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> build("config-with-http.yml"))
        .withMessage(InsightConfigurationFactory.SUGGEST_UPDATE_CONFIG_EXCEPTION_MESSAGE);
  }

  @Test
  public void testBuild_ConfigWithLoggingConsole_SuggestsUpdateConfig() {
    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> build("config-with-logging-console.yml"))
        .withMessage(InsightConfigurationFactory.SUGGEST_UPDATE_CONFIG_EXCEPTION_MESSAGE);
  }

  @Test
  public void testBuild_ConfigWithLoggingFile_SuggestsUpdateConfig() {
    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> build("config-with-logging-file.yml"))
        .withMessage(InsightConfigurationFactory.SUGGEST_UPDATE_CONFIG_EXCEPTION_MESSAGE);
  }

  @Test
  public void testBuild_ConfigWithLoggingSyslog_SuggestsUpdateConfig() {
    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> build("config-with-logging-syslog.yml"))
        .withMessage(InsightConfigurationFactory.SUGGEST_UPDATE_CONFIG_EXCEPTION_MESSAGE);
  }

  @Test
  public void testBuild_ConfigWithOtherUnknown_DoesNotSuggestUpdateConfig() {
    assertThatExceptionOfType(ConfigurationParsingException.class)
        .isThrownBy(() -> build("config-with-other-unknown.yml"))
        .satisfies(e -> assertThat(e.getMessage())
            .isNotEqualTo(InsightConfigurationFactory.SUGGEST_UPDATE_CONFIG_EXCEPTION_MESSAGE));
  }

  @Test
  public void testBuild_ConfigWithServer_DoesNotThrowException() throws Exception {
    build("config-with-server.yml");
  }

  private static InsightConfig build(String filename) throws Exception {
    String configResource = filename;
    if (!configResource.startsWith("/")) {
      configResource = "/InsightConfigurationFactoryTest/" + configResource;
    }
    File configFile = tempDir.newFile();
    FileUtils.copyURLToFile(InsightConfigurationFactoryTest.class.getResource(configResource), configFile);
    InsightBrainService insightBrainService = new InsightBrainService();
    Bootstrap<InsightConfig> bootstrap = new Bootstrap<>(insightBrainService);
    insightBrainService.initialize(bootstrap);
    ConfigurationFactory<InsightConfig> configurationFactory = bootstrap.getConfigurationFactoryFactory()
        .create(InsightConfig.class, bootstrap.getValidatorFactory().getValidator(), bootstrap.getObjectMapper(), "dw");
    InsightConfig insightConfig =
        configurationFactory.build(bootstrap.getConfigurationSourceProvider(), configFile.getPath());
    insightConfig.getServerFactory()
        .build(
            new Environment(bootstrap.getApplication().getName(), bootstrap.getObjectMapper(),
                bootstrap.getValidatorFactory(), bootstrap.getMetricRegistry(), bootstrap.getClassLoader(),
                bootstrap.getHealthCheckRegistry(), insightConfig));
    insightConfig.getLoggingFactory().configure(bootstrap.getMetricRegistry(), bootstrap.getApplication().getName());
    return insightConfig;
  }

  private void assertAppenderFactories(List<? extends AppenderFactory<?>> appenderFactories, List<String> formats) {
    assertAppenderFactories(appenderFactories, CONSOLE_FILE_SYSLOG_CLASSES, formats);
  }

  private void assertAppenderFactories(
      List<? extends AppenderFactory<?>> appenderFactories,
      List<Class<?>> appenderFactoryClasses,
      List<String> formats)
  {
    for (int index = 0; index < appenderFactories.size(); index++) {
      assertThat(appenderFactories.get(index)).isInstanceOf(appenderFactoryClasses.get(index));
      assertThat(((AbstractAppenderFactory<?>) appenderFactories.get(index)).getLogFormat())
          .isEqualTo(formats.get(index));
    }
  }

  private DefaultServerFactory assertDefaultServerFactory(InsightConfig insightConfig) {
    ServerFactory serverFactory = insightConfig.getServerFactory();
    assertThat(serverFactory).isInstanceOf(DefaultServerFactory.class);
    return (DefaultServerFactory) serverFactory;
  }

  private void assertConnector(
      List<ConnectorFactory> connectors,
      Class<? extends HttpConnectorFactory> cls,
      int port,
      Duration idleTimeout)
  {
    assertThat(connectors).hasSize(1);
    assertThat(connectors.get(0)).isInstanceOf(cls);
    HttpConnectorFactory connector = (HttpConnectorFactory) connectors.get(0);
    assertThat(connector.getPort()).isEqualTo(port);
    assertThat(connector.getIdleTimeout()).isEqualTo(idleTimeout);
  }

  /**
   * assert that all appenders on this request log have a filter factory of the specified class
   */
  private void assertAppenderFactoryFilterFactories(
      LogbackAccessRequestLogFactory requestLogFactory,
      Class<?> filterFactoryClass)
  {
    for (AppenderFactory<?> appenderFac : requestLogFactory.getAppenders()) {
      AbstractAppenderFactory<?> appenderFactory = (AbstractAppenderFactory<?>) appenderFac;
      assertThat(appenderFactory.getFilterFactories()).hasAtLeastOneElementOfType(filterFactoryClass);
    }
  }

  @Test
  public void testBuild_LosslessAsyncAppenders_ClassicRequestLog() throws Exception {
    InsightConfig insightConfig = build("config-with-logback-classic-request-log-formats.yml");

    LogbackClassicRequestLogFactory logFactory =
        (LogbackClassicRequestLogFactory) ((DefaultServerFactory) insightConfig.getServerFactory())
            .getRequestLogFactory();
    assertThat(logFactory.getAppenders()).hasSize(3).allSatisfy(this::assertLosslessAppender);
  }

  @Test
  public void testBuild_LosslessAsyncAppenders_AccessRequestLog() throws Exception {
    InsightConfig insightConfig = build("config-with-logback-access-request-log-formats.yml");

    LogbackAccessRequestLogFactory logFactory =
        (LogbackAccessRequestLogFactory) ((DefaultServerFactory) insightConfig.getServerFactory())
            .getRequestLogFactory();
    assertThat(logFactory.getAppenders()).hasSize(3).allSatisfy(this::assertLosslessAppender);
  }

  @Test
  public void testBuild_LosslessAsyncAppenders_ServerLog() throws Exception {
    InsightConfig insightConfig = build("config-server-log-appenders.yml");

    DefaultLoggingFactory loggingFactory = (DefaultLoggingFactory) insightConfig.getLoggingFactory();
    assertThat(loggingFactory.getAppenders()).hasSize(3).allSatisfy(this::assertLosslessAppender);
    LoggerConfiguration loggerConfiguration = Jackson.newObjectMapper()
        .treeToValue(loggingFactory.getLoggers().get("com.sonatype.insight.test"), LoggerConfiguration.class);
    assertThat(loggerConfiguration.getAppenders()).hasSize(3).allSatisfy(this::assertLosslessAppender);
  }

  private void assertLosslessAppender(AppenderFactory<?> appenderFactory) {
    assertThat(appenderFactory).hasFieldOrPropertyWithValue("neverBlock", false)
        .hasFieldOrPropertyWithValue("discardingThreshold", 0);
  }

  @Test
  public void testBuild_EnvironmentVariable_WithoutDefault() throws Exception {
    String licenseFile = "someLicenseFile.lic";
    environmentVariables.set("LICENSE_FILE", licenseFile);
    InsightConfig insightConfig = build("config-environment-variable-without-default.yml");

    assertThat(insightConfig.getLicenseFile()).isEqualTo(licenseFile);
  }

  @Test
  public void testBuild_EnvironmentVariable_WithoutDefault_Missing() throws Exception {
    InsightConfig insightConfig = build("config-environment-variable-without-default.yml");

    assertThat(insightConfig.getLicenseFile()).isEqualTo("${LICENSE_FILE}");
  }

  @Test
  public void testBuild_EnvironmentVariable_WithDefault() throws Exception {
    String licenseFile = "someLicenseFile.lic";
    environmentVariables.set("LICENSE_FILE", licenseFile);
    InsightConfig insightConfig = build("config-environment-variable-with-default.yml");

    assertThat(insightConfig.getLicenseFile()).isEqualTo(licenseFile);
  }

  @Test
  public void testBuild_EnvironmentVariable_WithDefault_Missing() throws Exception {
    InsightConfig insightConfig = build("config-environment-variable-with-default.yml");

    assertThat(insightConfig.getLicenseFile()).isEqualTo("defaultLicenseFile.lic");
  }

  @Test
  public void testBuild_SubstitutionInEnvironmentVariable() throws Exception {
    environmentVariables.set("LICENSE_FILE_VERSION", "1");
    String licenseFile = "someLicenseFile.lic";
    environmentVariables.set("LICENSE_FILE_VERSION_1", licenseFile);
    InsightConfig insightConfig = build("config-substitution-in-environment-variable.yml");

    assertThat(insightConfig.getLicenseFile()).isEqualTo(licenseFile);
  }

  @Test
  public void testBuild_DropwizardWebConfig_NoErrorAndSetsHstsConfigCorrectly() throws Exception {
    InsightConfig insightConfig = build("config-no-server.yml");

    assertThat(insightConfig.getWebConfiguration()).isNotNull();
    assertThat(insightConfig.getWebConfiguration().getHstsHeaderFactory()).isNotNull();
    // verify that the HSTS headers are set by default even if not specified in the config
    assertThat(insightConfig.getWebConfiguration().getHstsHeaderFactory().isEnabled()).isTrue();
    assertThat(insightConfig.getWebConfiguration().getHstsHeaderFactory().getMaxAge()).isEqualTo(Duration.days(365));
    assertThat(insightConfig.getWebConfiguration().getHstsHeaderFactory().isIncludeSubDomains()).isTrue();

    insightConfig = build("config-dropwizard-web-enable.yml");

    // verify that the HSTS headers can be configured using config file
    assertThat(insightConfig.getWebConfiguration().getHstsHeaderFactory().isEnabled()).isTrue();
    assertThat(insightConfig.getWebConfiguration().getHstsHeaderFactory().getMaxAge()).isEqualTo(Duration.days(90));
    assertThat(insightConfig.getWebConfiguration().getHstsHeaderFactory().isIncludeSubDomains()).isFalse();

    insightConfig = build("config-dropwizard-web-disable.yml");

    // verify that the HSTS headers can be disabled
    assertThat(insightConfig.getWebConfiguration().getHstsHeaderFactory().isEnabled()).isFalse();
  }

  @Test
  public void testBuild_OpenSearch_Default() throws Exception {
    InsightConfig insightConfig = build("config-opensearch-default.yml");

    assertThat(insightConfig.getSearchConfig()).isNull();
  }

  @Test
  public void testBuild_OpenSearch_Http() throws Exception {
    InsightConfig insightConfig = build("config-opensearch-http.yml");

    SearchConfig searchConfig = insightConfig.getSearchConfig();
    assertThat(searchConfig).isNotNull();
    assertThat(searchConfig).isInstanceOf(HttpOpenSearchConfig.class);

    HttpOpenSearchConfig httpOpenSearchConfig = (HttpOpenSearchConfig) searchConfig;
    assertThat(httpOpenSearchConfig.getUri()).isEqualTo(URI.create("https://example.com:123"));
    assertThat(httpOpenSearchConfig.getUsername()).isEqualTo("john");
    assertThat(httpOpenSearchConfig.getPassword()).isEqualTo("secret");
  }

  @Test
  public void testBuild_OpenSearch_Http_DefaultMode() throws Exception {
    InsightConfig insightConfig = build("config-opensearch-http.yml");

    SearchConfig searchConfig = insightConfig.getSearchConfig();
    assertThat(searchConfig.getMode()).isEqualTo(SearchMode.HYBRID);
  }

  @Test
  public void testBuild_OpenSearch_Http_OpenSearchMode() throws Exception {
    InsightConfig insightConfig = build("config-opensearch-http-with-mode.yml");

    SearchConfig searchConfig = insightConfig.getSearchConfig();
    assertThat(searchConfig).isInstanceOf(HttpOpenSearchConfig.class);
    assertThat(searchConfig.getMode()).isEqualTo(SearchMode.OPENSEARCH);

    HttpOpenSearchConfig httpOpenSearchConfig = (HttpOpenSearchConfig) searchConfig;
    assertThat(httpOpenSearchConfig.getUri()).isEqualTo(URI.create("https://example.com:123"));
    assertThat(httpOpenSearchConfig.getUsername()).isEqualTo("john");
    assertThat(httpOpenSearchConfig.getPassword()).isEqualTo("secret");
  }

  @Test
  public void testBuild_LuceneSearchConfig_LuceneMode() throws Exception {
    InsightConfig insightConfig = build("config-lucene-mode.yml");

    SearchConfig searchConfig = insightConfig.getSearchConfig();
    assertThat(searchConfig).isInstanceOf(LuceneSearchConfig.class);
    assertThat(searchConfig.getMode()).isEqualTo(SearchMode.LUCENE);
  }

  @Test
  public void testBuild_OpenSearch_Http_LuceneMode() throws Exception {
    InsightConfig insightConfig = build("config-opensearch-http-lucene-mode.yml");

    SearchConfig searchConfig = insightConfig.getSearchConfig();
    assertThat(searchConfig).isInstanceOf(HttpOpenSearchConfig.class);
    assertThat(searchConfig.getMode()).isEqualTo(SearchMode.LUCENE);
  }

  @Test
  public void testBuild_OpenSearch_Aws_DefaultMode() throws Exception {
    InsightConfig insightConfig = build("config-opensearch-aws.yml");

    SearchConfig searchConfig = insightConfig.getSearchConfig();
    assertThat(searchConfig.getMode()).isEqualTo(SearchMode.HYBRID);
  }

  @Test
  public void testBuild_OpenSearch_Aws() throws Exception {
    InsightConfig insightConfig = build("config-opensearch-aws.yml");

    SearchConfig searchConfig = insightConfig.getSearchConfig();
    assertThat(searchConfig).isNotNull();
    assertThat(searchConfig).isInstanceOf(AwsHttpOpenSearchConfig.class);

    AwsHttpOpenSearchConfig awsHttpOpenSearchConfig = (AwsHttpOpenSearchConfig) searchConfig;
    assertThat(awsHttpOpenSearchConfig.getDomain()).isEqualTo(URI.create("https://example.com:123"));
    assertThat(awsHttpOpenSearchConfig.getRegion()).isEqualTo("us-east-1");
    assertThat(awsHttpOpenSearchConfig.getMaxConcurrency()).isEqualTo(25);
    assertThat(awsHttpOpenSearchConfig.getConnectionTimeout()).isEqualTo(java.time.Duration.ofSeconds(30));
    assertThat(awsHttpOpenSearchConfig.getConnectionAcquisitionTimeout()).isEqualTo(java.time.Duration.ofSeconds(30));
  }

  @Test
  public void testBuild_OpenSearch_Aws_OpenSearchMode() throws Exception {
    InsightConfig insightConfig = build("config-opensearch-aws-with-mode.yml");

    SearchConfig searchConfig = insightConfig.getSearchConfig();
    assertThat(searchConfig).isInstanceOf(AwsHttpOpenSearchConfig.class);
    assertThat(searchConfig.getMode()).isEqualTo(SearchMode.OPENSEARCH);

    AwsHttpOpenSearchConfig awsHttpOpenSearchConfig = (AwsHttpOpenSearchConfig) searchConfig;
    assertThat(awsHttpOpenSearchConfig.getDomain()).isEqualTo(URI.create("https://example.com:123"));
    assertThat(awsHttpOpenSearchConfig.getRegion()).isEqualTo("us-east-1");
  }

  @Test
  public void testBuild_OpenSearch_Aws_LuceneMode() throws Exception {
    InsightConfig insightConfig = build("config-opensearch-aws-lucene-mode.yml");

    SearchConfig searchConfig = insightConfig.getSearchConfig();
    assertThat(searchConfig).isInstanceOf(AwsHttpOpenSearchConfig.class);
    assertThat(searchConfig.getMode()).isEqualTo(SearchMode.LUCENE);
  }
}
