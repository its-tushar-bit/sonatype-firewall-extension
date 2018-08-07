/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.Arrays;
import java.util.List;

import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.ResourceConfigurationSourceProvider;
import io.dropwizard.jetty.ConnectorFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.jetty.HttpsConnectorFactory;
import io.dropwizard.logging.AbstractAppenderFactory;
import io.dropwizard.logging.AppenderFactory;
import io.dropwizard.logging.ConsoleAppenderFactory;
import io.dropwizard.logging.DefaultLoggingFactory;
import io.dropwizard.logging.FileAppenderFactory;
import io.dropwizard.logging.SyslogAppenderFactory;
import io.dropwizard.request.logging.LogbackAccessRequestLogFactory;
import io.dropwizard.request.logging.old.LogbackClassicRequestLogFactory;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.server.ServerFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.util.Duration;
import io.dropwizard.setup.Environment;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import com.sonatype.insight.brain.telemetry.UserTelemetryRequestLoggingFilter;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import ch.qos.logback.classic.Level;

public class InsightConfigurationFactoryTest
{
  private static final List<Class<?>> CONSOLE_FILE_SYSLOG_CLASSES = Arrays
      .asList(ConsoleAppenderFactory.class, FileAppenderFactory.class, SyslogAppenderFactory.class);
  
  private static final int DROPWIZARD_HTTPS_PORT = new HttpsConnectorFactory().getPort();

  @Test
  public void testBuild_ConfigRequestLogFilterFactories_UsesUserTelemetryRequestLoggingFilter() throws Exception {
    InsightConfig insightConfig = build("config-without-logback-access-request-log-formats.yml");
    DefaultServerFactory serverFactory = assertDefaultServerFactory(insightConfig);
    LogbackAccessRequestLogFactory requestLogFactory =
        (LogbackAccessRequestLogFactory) serverFactory.getRequestLogFactory();

    assertAppenderFactoryFilterFactories(requestLogFactory, UserTelemetryRequestLoggingFilter.class);
  }

  @Test
  public void testBuild_ConfigWithoutTelemetryClientLogger_SetsInfoLevel() throws Exception {
    InsightConfig insightConfig = build("config-without-telemetry-client-logger.yml");
    DefaultLoggingFactory loggingFactory = (DefaultLoggingFactory) insightConfig.getLoggingFactory();

    assertLoggerLevel(loggingFactory, "com.sonatype.insight.brain.hds.UserTelemetryHdsClient", Level.INFO);
  }

  @Test
  public void testBuild_ConfigWithTelemetryClientLogger_UsesGivenValue() throws Exception {
    InsightConfig insightConfig = build("config-with-telemetry-client-logger.yml");
    DefaultLoggingFactory loggingFactory = (DefaultLoggingFactory) insightConfig.getLoggingFactory();

    assertLoggerLevel(loggingFactory, "com.sonatype.insight.brain.hds.UserTelemetryHdsClient", Level.DEBUG);
  }

  @Test
  public void testBuild_ConfigWithLogbackAccessRequestAppendersWithoutLogFormats_UsesOurRequestLogFormat()
      throws Exception
  {
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
  public void testBuild_ConfigWithLogbackClassicRequestAppendersWithoutLogFormats_UsesDropwizardRequestLogFormats()
      throws Exception
  {
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
  public void testBuild_ConfigWithServerAppendersWithoutLogFormats_UsesDropwizardLogFormats() throws Exception
  {
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
  public void testBuild_NoServer_UsesOurDefaultConnectors() throws Exception
  {
    InsightConfig insightConfig = build("config-no-server.yml");

    DefaultServerFactory defaultServerFactory = assertDefaultServerFactory(insightConfig);
    assertConnector(defaultServerFactory.getApplicationConnectors(), HttpConnectorFactory.class,
        InsightConfigurationFactory.DEFAULT_APPLICATION_PORT, InsightConfigurationFactory.DEFAULT_IDLE_TIMEOUT);
    assertConnector(defaultServerFactory.getAdminConnectors(), HttpConnectorFactory.class,
        InsightConfigurationFactory.DEFAULT_ADMIN_PORT, InsightConfigurationFactory.DEFAULT_IDLE_TIMEOUT);
  }

  @Test
  public void testBuild_EmptyServer_UsesOurDefaultConnectors() throws Exception
  {
    InsightConfig insightConfig = build("config-empty-server.yml");

    DefaultServerFactory defaultServerFactory = assertDefaultServerFactory(insightConfig);
    assertConnector(defaultServerFactory.getApplicationConnectors(), HttpConnectorFactory.class,
        InsightConfigurationFactory.DEFAULT_APPLICATION_PORT, InsightConfigurationFactory.DEFAULT_IDLE_TIMEOUT);
    assertConnector(defaultServerFactory.getAdminConnectors(), HttpConnectorFactory.class,
        InsightConfigurationFactory.DEFAULT_ADMIN_PORT, InsightConfigurationFactory.DEFAULT_IDLE_TIMEOUT);
  }

  @Test
  public void testBuild_HttpConnectorsWithoutSettings_UseOurDefaultSettings() throws Exception
  {
    InsightConfig insightConfig = build("config-http-connectors-without-settings.yml");

    DefaultServerFactory defaultServerFactory = assertDefaultServerFactory(insightConfig);
    assertConnector(defaultServerFactory.getApplicationConnectors(), HttpConnectorFactory.class,
        InsightConfigurationFactory.DEFAULT_APPLICATION_PORT, InsightConfigurationFactory.DEFAULT_IDLE_TIMEOUT);
    assertConnector(defaultServerFactory.getAdminConnectors(), HttpConnectorFactory.class,
        InsightConfigurationFactory.DEFAULT_APPLICATION_PORT, InsightConfigurationFactory.DEFAULT_IDLE_TIMEOUT);
  }

  @Test
  public void testBuild_HttpConnectorsWithSettings_UseGivenSettings() throws Exception
  {
    InsightConfig insightConfig = build("config-http-connectors-with-settings.yml");

    DefaultServerFactory defaultServerFactory = assertDefaultServerFactory(insightConfig);
    assertConnector(defaultServerFactory.getApplicationConnectors(), HttpConnectorFactory.class, 9070,
        Duration.minutes(30));
    assertConnector(defaultServerFactory.getAdminConnectors(), HttpConnectorFactory.class, 9071, Duration.minutes(30));
  }

  @Test
  public void testBuild_HttpsConnectorsWithoutSettings_UseOurIdleTimeout() throws Exception
  {
    InsightConfig insightConfig = build("config-https-connectors-without-settings.yml");

    DefaultServerFactory defaultServerFactory = assertDefaultServerFactory(insightConfig);
    assertConnector(defaultServerFactory.getApplicationConnectors(), HttpsConnectorFactory.class, DROPWIZARD_HTTPS_PORT,
        InsightConfigurationFactory.DEFAULT_IDLE_TIMEOUT);
    assertConnector(defaultServerFactory.getAdminConnectors(), HttpsConnectorFactory.class, DROPWIZARD_HTTPS_PORT,
        InsightConfigurationFactory.DEFAULT_IDLE_TIMEOUT);
  }

  @Test
  public void testBuild_HttpsConnectorsWithSettings_UseGivenSettings() throws Exception
  {
    InsightConfig insightConfig = build("config-https-connectors-with-settings.yml");

    DefaultServerFactory defaultServerFactory = assertDefaultServerFactory(insightConfig);
    assertConnector(defaultServerFactory.getApplicationConnectors(), HttpsConnectorFactory.class, 9070,
        Duration.minutes(30));
    assertConnector(defaultServerFactory.getAdminConnectors(), HttpsConnectorFactory.class, 9071, Duration.minutes(30));
  }

  private InsightConfig build(String filename) throws Exception {
    InsightBrainService insightBrainService = new InsightBrainService();
    Bootstrap<InsightConfig> bootstrap = new Bootstrap<>(insightBrainService);
    insightBrainService.initialize(bootstrap);
    ConfigurationFactory<InsightConfig> configurationFactory = bootstrap.getConfigurationFactoryFactory()
        .create(InsightConfig.class, bootstrap.getValidatorFactory().getValidator(), bootstrap.getObjectMapper(), "dw");
    InsightConfig insightConfig = configurationFactory
        .build(new ResourceConfigurationSourceProvider(), "/InsightConfigurationFactoryTest/" + filename);
    insightConfig.getServerFactory().build(
        new Environment(bootstrap.getApplication().getName(), bootstrap.getObjectMapper(),
            bootstrap.getValidatorFactory().getValidator(), bootstrap.getMetricRegistry(), bootstrap.getClassLoader(),
            bootstrap.getHealthCheckRegistry()));
    insightConfig.getLoggingFactory().configure(bootstrap.getMetricRegistry(), bootstrap.getApplication().getName());
    return insightConfig;
  }

  private void assertAppenderFactories(List<? extends AppenderFactory<?>> appenderFactories, List<String> formats)
  {
    assertAppenderFactories(appenderFactories, CONSOLE_FILE_SYSLOG_CLASSES, formats);
  }

  private void assertAppenderFactories(List<? extends AppenderFactory<?>> appenderFactories,
                                       List<Class<?>> appenderFactoryClasses,
                                       List<String> formats)
  {
    for (int index = 0; index < appenderFactories.size(); index++) {
      assertThat(appenderFactories.get(index), instanceOf(appenderFactoryClasses.get(index)));
      assertThat(((AbstractAppenderFactory<?>) appenderFactories.get(index)).getLogFormat(), is(formats.get(index)));
    }
  }

  private DefaultServerFactory assertDefaultServerFactory(InsightConfig insightConfig) {
    ServerFactory serverFactory = insightConfig.getServerFactory();
    assertThat(serverFactory, is(instanceOf(DefaultServerFactory.class)));
    return (DefaultServerFactory) serverFactory;
  }

  private void assertConnector(List<ConnectorFactory> connectors,
                               Class<? extends HttpConnectorFactory> cls,
                               int port,
                               Duration idleTimeout)
  {
    assertThat(connectors, is(notNullValue()));
    assertThat(connectors, hasSize(1));
    assertThat(connectors.get(0), is(instanceOf(cls)));
    HttpConnectorFactory connector = (HttpConnectorFactory) connectors.get(0);
    assertThat(connector.getPort(), is(port));
    assertThat(connector.getIdleTimeout(), is(idleTimeout));
  }

  /**
   * assert that all appenders on this request log have a filter factory of the specified class
   */
  private void assertAppenderFactoryFilterFactories(LogbackAccessRequestLogFactory requestLogFactory,
                                                    Class<?> filterFactoryClass)
  {
    for (AppenderFactory<?> appenderFac : requestLogFactory.getAppenders()) {
      AbstractAppenderFactory<?> appenderFactory = (AbstractAppenderFactory<?>) appenderFac;

      assertThat(appenderFactory.getFilterFactories(), hasItem(instanceOf(filterFactoryClass)));
    }
  }

  private void assertLoggerLevel(DefaultLoggingFactory loggingFactory, String loggerName, Level expected) {
    ImmutableMap<String, JsonNode> loggerLevels = loggingFactory.getLoggers();
    JsonNode jsonNode = loggerLevels.get(loggerName);
    String levelString = jsonNode.asText();
    Level level = Level.valueOf(levelString);

    assertThat(level, is(expected));
  }
}
