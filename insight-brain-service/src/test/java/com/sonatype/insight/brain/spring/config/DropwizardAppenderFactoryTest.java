/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.net.SSLSocketAppender;
import ch.qos.logback.classic.net.SyslogAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.spi.FilterReply;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DropwizardAppenderFactoryTest
{
  private LoggerContext context;

  @Before
  public void setUp() {
    context = new LoggerContext();
    context.start();
  }

  @After
  public void tearDown() {
    context.stop();
  }

  @Test
  public void testCreateFileAppender_withArchive_returnsRollingFileAppender() {
    var appender = DropwizardAppenderFactory.createFileAppender(
        context, "test", "/logs/app.log", "/logs/app-%d.log", 7, null, true);

    assertThat(appender).isInstanceOf(RollingFileAppender.class);
    RollingFileAppender<?> rolling = (RollingFileAppender<?>) appender;
    assertThat(rolling.getFile()).isEqualTo("/logs/app.log");
    TimeBasedRollingPolicy<?> policy = (TimeBasedRollingPolicy<?>) rolling.getRollingPolicy();
    assertThat(policy.getFileNamePattern()).isEqualTo("/logs/app-%d.log");
    assertThat(policy.getMaxHistory()).isEqualTo(7);
  }

  @Test
  public void testCreateFileAppender_withoutArchive_returnsPlainFileAppender() {
    var appender = DropwizardAppenderFactory.createFileAppender(
        context, "test", "/logs/app.log", null, null, null, false);

    assertThat(appender).isInstanceOf(FileAppender.class);
    assertThat(appender).isNotInstanceOf(RollingFileAppender.class);
    assertThat(((FileAppender<?>) appender).getFile()).isEqualTo("/logs/app.log");
  }

  @Test
  public void testCreateFileAppender_withNullArchivePattern_usesDerivedPattern() {
    var appender = DropwizardAppenderFactory.createFileAppender(
        context, "test", "/logs/app.log", null, null, null, true);

    assertThat(appender).isInstanceOf(RollingFileAppender.class);
    TimeBasedRollingPolicy<?> policy =
        (TimeBasedRollingPolicy<?>) ((RollingFileAppender<?>) appender).getRollingPolicy();
    assertThat(policy.getFileNamePattern()).isEqualTo("/logs/app-%d.log");
  }

  @Test
  public void testCreateFileAppender_withCustomLogFormat_encoderUsesPattern() {
    String customFormat = "%d{HH:mm:ss} [%thread] %-5level %logger - %msg%n";

    var appender = DropwizardAppenderFactory.createFileAppender(
        context, "test", "/logs/app.log", null, null, customFormat, false);

    assertThat(appender).isInstanceOf(FileAppender.class);
    LayoutWrappingEncoder<?> encoder = (LayoutWrappingEncoder<?>) ((FileAppender<?>) appender).getEncoder();
    PatternLayout layout = (PatternLayout) encoder.getLayout();
    assertThat(layout.getPattern()).isEqualTo(customFormat);
  }

  @Test
  public void testCreateConsoleAppender_returnsConsoleAppender() {
    var appender = DropwizardAppenderFactory.createConsoleAppender(context, "console", null, null);

    assertThat(appender).isInstanceOf(ConsoleAppender.class);
    assertThat(appender.getName()).isEqualTo("console");
  }

  @Test
  public void testCreateConsoleAppender_withStderrTarget_usesSystemErr() {
    ConsoleAppender<?> appender =
        (ConsoleAppender<?>) DropwizardAppenderFactory.createConsoleAppender(
            context, "console", null, "stderr");

    assertThat(appender.getTarget()).isEqualTo("System.err");
  }

  @Test
  public void testCreateSyslogAppender_setsHostPortAndFacility() {
    SyslogAppender appender =
        (SyslogAppender) DropwizardAppenderFactory.createSyslogAppender(
            context, "syslog", "loghost.example.com", 5140, "LOCAL7", null, null);

    assertThat(appender.getSyslogHost()).isEqualTo("loghost.example.com");
    assertThat(appender.getPort()).isEqualTo(5140);
    assertThat(appender.getFacility()).isEqualTo("LOCAL7");
  }

  @Test
  public void testCreateSyslogAppender_withSuffixPatternAndStackTracePrefix() {
    SyslogAppender appender =
        (SyslogAppender) DropwizardAppenderFactory.createSyslogAppender(
            context, "syslog", "localhost", 514, "LOCAL0",
            "%thread: %msg", "\t");

    assertThat(appender.getSuffixPattern()).isEqualTo("%thread: %msg");
    assertThat(appender.getStackTracePattern()).isEqualTo("\t");
  }

  @Test
  public void testCreateTcpAppender_setsHostAndPort() {
    var appender = DropwizardAppenderFactory.createTcpAppender(
        context, "tcp", "logserver.example.com", 4560, null, false);

    assertThat(appender).isNotNull();
    assertThat(appender.getName()).isEqualTo("tcp");
  }

  @Test
  public void testCreateTlsAppender_returnsSSLSocketAppender() {
    var appender = DropwizardAppenderFactory.createTlsAppender(
        context, "tls", "logserver.example.com", 6514, false);

    assertThat(appender).isInstanceOf(SSLSocketAppender.class);
    assertThat(appender.getName()).isEqualTo("tls");
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testApplyThresholdFilter_deniesEventsBelowThreshold() {
    var appender = DropwizardAppenderFactory.createConsoleAppender(context, "console", null, null);
    DropwizardAppenderFactory.applyThresholdFilter(appender, "WARN");

    Filter<ILoggingEvent> filter =
        (Filter<ILoggingEvent>) (Filter<?>) appender.getCopyOfAttachedFiltersList().get(0);

    LoggingEvent debugEvent = new LoggingEvent();
    debugEvent.setLevel(Level.DEBUG);
    assertThat(filter.decide(debugEvent)).isEqualTo(FilterReply.DENY);

    LoggingEvent warnEvent = new LoggingEvent();
    warnEvent.setLevel(Level.WARN);
    assertThat(filter.decide(warnEvent)).isEqualTo(FilterReply.NEUTRAL);

    LoggingEvent errorEvent = new LoggingEvent();
    errorEvent.setLevel(Level.ERROR);
    assertThat(filter.decide(errorEvent)).isEqualTo(FilterReply.NEUTRAL);
  }

  @Test
  public void testDeriveArchivePattern_withExtension_insertsDateBeforeExtension() {
    assertThat(DropwizardAppenderFactory.deriveArchivePattern("/logs/app.log"))
        .isEqualTo("/logs/app-%d.log");
  }

  @Test
  public void testDeriveArchivePattern_withoutExtension_appendsDateSuffix() {
    assertThat(DropwizardAppenderFactory.deriveArchivePattern("/logs/app"))
        .isEqualTo("/logs/app-%d");
  }

  @Test
  public void testDeriveArchivePattern_withDotInDirectory_handlesCorrectly() {
    assertThat(DropwizardAppenderFactory.deriveArchivePattern("/opt/app.d/logfile"))
        .isEqualTo("/opt/app.d/logfile-%d");
  }

  @Test
  public void testTranslateLogFormat_replacesDwRExWithREx() {
    assertThat(DropwizardAppenderFactory.translateLogFormat("%-5p %c: %m%n%dwREx"))
        .isEqualTo("%-5p %c: %m%n%rEx");
  }

  @Test
  public void testTranslateLogFormat_replacesDwExWithEx() {
    assertThat(DropwizardAppenderFactory.translateLogFormat("%-5p %c: %m%n%dwEx"))
        .isEqualTo("%-5p %c: %m%n%ex");
  }

  @Test
  public void testTranslateLogFormat_replacesDwXExWithXEx() {
    assertThat(DropwizardAppenderFactory.translateLogFormat("%-5p %c: %m%n%dwXEx"))
        .isEqualTo("%-5p %c: %m%n%xEx");
  }

  @Test
  public void testTranslateLogFormat_preservesStandardLogbackPatterns() {
    String standard = "%d{HH:mm:ss} [%thread] %-5level %logger - %msg%n";
    assertThat(DropwizardAppenderFactory.translateLogFormat(standard)).isEqualTo(standard);
  }

  @Test
  public void testWrapAsync_wrapsAppenderInAsyncAppender() {
    var inner = DropwizardAppenderFactory.createConsoleAppender(context, "console", null, null);
    var wrapped = DropwizardAppenderFactory.wrapAsync(context, inner, 512, 20, true);

    assertThat(wrapped).isInstanceOf(ch.qos.logback.classic.AsyncAppender.class);
    ch.qos.logback.classic.AsyncAppender async = (ch.qos.logback.classic.AsyncAppender) wrapped;
    assertThat(async.getQueueSize()).isEqualTo(512);
    assertThat(async.getDiscardingThreshold()).isEqualTo(20);
    assertThat(async.isNeverBlock()).isTrue();
  }

  @Test
  public void testWrapAsync_withZeroQueueSize_returnsOriginalAppender() {
    var inner = DropwizardAppenderFactory.createConsoleAppender(context, "console", null, null);
    var result = DropwizardAppenderFactory.wrapAsync(context, inner, 0, -1, false);

    assertThat(result).isSameAs(inner);
  }
}
