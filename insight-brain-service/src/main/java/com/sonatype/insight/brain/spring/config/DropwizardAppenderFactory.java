/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.net.SSLSocketAppender;
import ch.qos.logback.classic.net.SocketAppender;
import ch.qos.logback.classic.net.SyslogAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.spi.FilterReply;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.logging.json.EventJsonLayoutBaseFactory;
import java.util.Map;
import java.util.TimeZone;

final class DropwizardAppenderFactory
{
  static final String DEFAULT_LOG_FORMAT = "%-5p [%d{ISO8601,UTC}] %c: %m%n%rEx";

  static final int DEFAULT_QUEUE_SIZE = 256;

  static final int DEFAULT_DISCARDING_THRESHOLD = -1;

  private DropwizardAppenderFactory() {
  }

  static Appender<ILoggingEvent> createFileAppender(
      LoggerContext context,
      String name,
      String filename,
      String archivePattern,
      Integer archiveCount,
      String logFormat,
      boolean archive)
  {
    return createFileAppender(context, name, filename, archivePattern, archiveCount, logFormat, archive, null);
  }

  static Appender<ILoggingEvent> createFileAppender(
      LoggerContext context,
      String name,
      String filename,
      String archivePattern,
      Integer archiveCount,
      String logFormat,
      boolean archive,
      Object layoutConfig)
  {
    LayoutWrappingEncoder<ILoggingEvent> encoder = createEncoderWithLayout(context, logFormat, layoutConfig);

    if (!archive) {
      FileAppender<ILoggingEvent> appender = new FileAppender<>();
      appender.setName(name);
      appender.setContext(context);
      appender.setFile(filename);
      appender.setAppend(true);
      appender.setEncoder(encoder);
      appender.start();
      return appender;
    }

    RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
    appender.setName(name);
    appender.setContext(context);
    appender.setFile(filename);
    appender.setEncoder(encoder);

    TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<>();
    rollingPolicy.setContext(context);
    rollingPolicy.setFileNamePattern(
        archivePattern != null ? archivePattern : deriveArchivePattern(filename));
    if (archiveCount != null) {
      rollingPolicy.setMaxHistory(archiveCount);
    }
    rollingPolicy.setParent(appender);
    rollingPolicy.start();

    appender.setRollingPolicy(rollingPolicy);
    appender.start();
    return appender;
  }

  static Appender<ILoggingEvent> createConsoleAppender(
      LoggerContext context,
      String name,
      String logFormat,
      String target)
  {
    return createConsoleAppender(context, name, logFormat, target, null);
  }

  static Appender<ILoggingEvent> createConsoleAppender(
      LoggerContext context,
      String name,
      String logFormat,
      String target,
      Object layoutConfig)
  {
    ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
    appender.setName(name);
    appender.setContext(context);
    appender.setEncoder(createEncoderWithLayout(context, logFormat, layoutConfig));
    if ("stderr".equalsIgnoreCase(target)) {
      appender.setTarget("System.err");
    }
    appender.start();
    return appender;
  }

  static Appender<ILoggingEvent> createSyslogAppender(
      LoggerContext context,
      String name,
      String host,
      int port,
      String facility,
      String suffixPattern,
      String stackTracePrefix)
  {
    SyslogAppender appender = new SyslogAppender();
    appender.setName(name);
    appender.setContext(context);
    appender.setSyslogHost(host != null ? host : "localhost");
    appender.setPort(port > 0 ? port : 514);
    appender.setFacility(facility != null ? facility : "LOCAL0");
    if (suffixPattern != null) {
      appender.setSuffixPattern(suffixPattern);
    }
    if (stackTracePrefix != null) {
      appender.setStackTracePattern(stackTracePrefix);
    }
    appender.start();
    return appender;
  }

  static Appender<ILoggingEvent> createTcpAppender(
      LoggerContext context,
      String name,
      String host,
      int port,
      java.time.Duration connectionTimeout,
      boolean includeCallerData)
  {
    SocketAppender appender = new SocketAppender();
    appender.setName(name);
    appender.setContext(context);
    appender.setRemoteHost(host != null ? host : "localhost");
    appender.setPort(port > 0 ? port : 4560);
    if (connectionTimeout != null) {
      appender.setReconnectionDelay(new ch.qos.logback.core.util.Duration(connectionTimeout.toMillis()));
    }
    appender.setIncludeCallerData(includeCallerData);
    appender.start();
    return appender;
  }

  static Appender<ILoggingEvent> createTlsAppender(
      LoggerContext context,
      String name,
      String host,
      int port,
      boolean includeCallerData)
  {
    SSLSocketAppender appender = new SSLSocketAppender();
    appender.setName(name);
    appender.setContext(context);
    appender.setRemoteHost(host != null ? host : "localhost");
    appender.setPort(port > 0 ? port : 4560);
    appender.setIncludeCallerData(includeCallerData);
    appender.start();
    return appender;
  }

  static void applyThresholdFilter(Appender<ILoggingEvent> appender, String threshold) {
    if (threshold == null || threshold.isBlank()) {
      return;
    }
    Level level = Level.toLevel(threshold, null);
    if (level == null) {
      return;
    }
    appender.addFilter(new Filter<>()
    {
      @Override
      public FilterReply decide(ILoggingEvent event) {
        if (event.getLevel().isGreaterOrEqual(level)) {
          return FilterReply.NEUTRAL;
        }
        return FilterReply.DENY;
      }
    });
  }

  @SuppressWarnings("unchecked")
  static LayoutWrappingEncoder<ILoggingEvent> createEncoderWithLayout(
      LoggerContext context,
      String logFormat,
      Object layoutConfig)
  {
    if (layoutConfig instanceof Map<?, ?> layoutMap
        && "json".equals(((Map<String, Object>) layoutMap).get("type")))
    {
      return createJsonEncoder(context, layoutMap);
    }
    return createEncoder(context, logFormat);
  }

  static LayoutWrappingEncoder<ILoggingEvent> createJsonEncoder(LoggerContext context, Map<?, ?> layoutMap) {
    EventJsonLayoutBaseFactory factory =
        Jackson.newObjectMapper().convertValue(layoutMap, EventJsonLayoutBaseFactory.class);
    Layout<ILoggingEvent> layout = factory.build(context, TimeZone.getTimeZone("UTC"));
    layout.setContext(context);
    layout.start();

    LayoutWrappingEncoder<ILoggingEvent> encoder = new LayoutWrappingEncoder<>();
    encoder.setContext(context);
    encoder.setLayout(layout);
    encoder.start();
    return encoder;
  }

  static LayoutWrappingEncoder<ILoggingEvent> createEncoder(LoggerContext context, String logFormat) {
    LayoutWrappingEncoder<ILoggingEvent> encoder = new LayoutWrappingEncoder<>();
    PatternLayout layout = new PatternLayout();
    layout.setContext(context);
    layout.setPattern(translateLogFormat(logFormat != null ? logFormat : DEFAULT_LOG_FORMAT));
    layout.start();
    encoder.setContext(context);
    encoder.setLayout(layout);
    encoder.start();
    return encoder;
  }

  static String translateLogFormat(String format) {
    return format
        .replace("%dwRootException", "%rEx")
        .replace("%dwREx", "%rEx")
        .replace("%dwXException", "%xEx")
        .replace("%dwXThrowable", "%xEx")
        .replace("%dwXEx", "%xEx")
        .replace("%dwException", "%ex")
        .replace("%dwThrowable", "%ex")
        .replace("%dwEx", "%ex");
  }

  static Appender<ILoggingEvent> wrapAsync(
      LoggerContext context,
      Appender<ILoggingEvent> appender,
      int queueSize,
      int discardingThreshold,
      boolean neverBlock)
  {
    if (queueSize <= 0) {
      return appender;
    }
    AsyncAppender asyncAppender = new AsyncAppender();
    asyncAppender.setName(appender.getName() + ".async");
    asyncAppender.setContext(context);
    asyncAppender.setQueueSize(queueSize);
    if (discardingThreshold >= 0) {
      asyncAppender.setDiscardingThreshold(discardingThreshold);
    }
    asyncAppender.setNeverBlock(neverBlock);
    asyncAppender.addAppender(appender);
    asyncAppender.start();
    return asyncAppender;
  }

  static String deriveArchivePattern(String filename) {
    int extensionIndex = filename.lastIndexOf('.');
    int pathSeparatorIndex = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
    if (extensionIndex > pathSeparatorIndex) {
      return filename.substring(0, extensionIndex) + "-%d" + filename.substring(extensionIndex);
    }
    return filename + "-%d";
  }
}
