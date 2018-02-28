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
import io.dropwizard.logging.AbstractAppenderFactory;
import io.dropwizard.logging.AppenderFactory;
import io.dropwizard.logging.ConsoleAppenderFactory;
import io.dropwizard.logging.DefaultLoggingFactory;
import io.dropwizard.logging.FileAppenderFactory;
import io.dropwizard.logging.SyslogAppenderFactory;
import io.dropwizard.request.logging.LogbackAccessRequestLogFactory;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.setup.Bootstrap;
import org.junit.Test;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class InsightConfigurationFactoryTest
{
  private static final List<Class<?>> CONSOLE_FILE_SYSLOG_CLASSES = Arrays
      .asList(ConsoleAppenderFactory.class, FileAppenderFactory.class, SyslogAppenderFactory.class);

  @Test
  public void testBuild_ConfigWithRequestAppendersWithoutLogFormats_UsesOurRequestLogFormat() throws Exception
  {
    InsightConfig insightConfig = build("config-without-request-log-formats.yml");

    assertAppenderFactories(((LogbackAccessRequestLogFactory) ((DefaultServerFactory) insightConfig.getServerFactory())
        .getRequestLogFactory()).getAppenders(), Arrays.asList(InsightConfigurationFactory.DEFAULT_REQUEST_LOG_FORMAT,
        InsightConfigurationFactory.DEFAULT_REQUEST_LOG_FORMAT,
        InsightConfigurationFactory.DEFAULT_REQUEST_LOG_FORMAT));
  }

  @Test
  public void testBuild_ConfigWithRequestAppendersWithLogFormats_SetsNothing() throws Exception {
    InsightConfig insightConfig = build("config-with-request-log-formats.yml");

    assertAppenderFactories(((LogbackAccessRequestLogFactory) ((DefaultServerFactory) insightConfig.getServerFactory())
            .getRequestLogFactory()).getAppenders(),
        Arrays.asList("consoleRequestLogFormat", "fileRequestLogFormat", "syslogRequestLogFormat"));
  }

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

  private InsightConfig build(String filename) throws Exception {
    InsightBrainService insightBrainService = new InsightBrainService();
    Bootstrap<InsightConfig> bootstrap = new Bootstrap<>(insightBrainService);
    insightBrainService.initialize(bootstrap);
    ConfigurationFactory<InsightConfig> configurationFactory = bootstrap.getConfigurationFactoryFactory()
        .create(InsightConfig.class, bootstrap.getValidatorFactory().getValidator(), bootstrap.getObjectMapper(), "dw");
    return configurationFactory
        .build(new ResourceConfigurationSourceProvider(), "/InsightConfigurationFactoryTest/" + filename);
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
}
