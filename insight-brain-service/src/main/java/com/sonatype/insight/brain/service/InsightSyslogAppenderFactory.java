/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.dropwizard.logging.common.SyslogAppenderFactory;
import io.dropwizard.logging.common.async.AsyncAppenderFactory;
import io.dropwizard.logging.common.filter.LevelFilterFactory;
import io.dropwizard.logging.common.layout.LayoutFactory;

@JsonDeserialize(as = InsightSyslogAppenderFactory.class)
public class InsightSyslogAppenderFactory
    extends SyslogAppenderFactory
{
  public static class Module
      extends SimpleModule
  {
    private static final long serialVersionUID = -2731230613451928047L;

    public Module() {
      setMixInAnnotation(SyslogAppenderFactory.class, InsightSyslogAppenderFactory.class);
    }
  }

  public InsightSyslogAppenderFactory() {
    // Due to SyslogAppenderFactory setting an initial log format
    // https://github.com/dropwizard/dropwizard/blob/v1.2.3/
    // dropwizard-logging/src/main/java/io/dropwizard/logging/SyslogAppenderFactory.java#L128-L129
    // this is necessary to allow InsightConfigurationFactory to determine if the user did not set the log format and
    // thus if it should set the desired default log format
    setLogFormat(null);
  }

  @Override
  public Appender<ILoggingEvent> build(
      LoggerContext context,
      String applicationName,
      LayoutFactory<ILoggingEvent> layoutFactory,
      LevelFilterFactory<ILoggingEvent> levelFilterFactory,
      AsyncAppenderFactory<ILoggingEvent> asyncAppenderFactory)
  {
    // Avoids a NPE at
    // https://github.com/dropwizard/dropwizard/blob/v1.2.3/
    // dropwizard-logging/src/main/java/io/dropwizard/logging/SyslogAppenderFactory.java#L210-L211
    if (getLogFormat() == null) {
      setLogFormat(new SyslogAppenderFactory().getLogFormat());
    }
    return super.build(context, applicationName, layoutFactory, levelFilterFactory, asyncAppenderFactory);
  }
}
