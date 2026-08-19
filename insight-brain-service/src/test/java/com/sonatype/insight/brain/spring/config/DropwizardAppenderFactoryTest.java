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
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DropwizardAppenderFactoryTest
{
  private LoggerContext context;

  private final Map<String, String> savedSslProperties = new HashMap<>();

  @BeforeEach
  public void setUp() {
    context = new LoggerContext();
    context.start();
    // logback's default SSLConfiguration lazily derives a key/trust store from these JVM-wide system properties
    // (SSLContextFactoryBean#getKeyStore). Clear them so assertions about the absence of a configured store are
    // deterministic regardless of what an earlier test left set in the shared surefire fork (reuseForks=true).
    clearAndSave("javax.net.ssl.keyStore");
    clearAndSave("javax.net.ssl.trustStore");
  }

  @AfterEach
  public void tearDown() {
    context.stop();
    savedSslProperties.forEach((key, value) -> {
      if (value == null) {
        System.clearProperty(key);
      }
      else {
        System.setProperty(key, value);
      }
    });
    savedSslProperties.clear();
  }

  private void clearAndSave(final String key) {
    savedSslProperties.put(key, System.getProperty(key));
    System.clearProperty(key);
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
    DropwizardAppenderConfig.Tls config = new DropwizardAppenderConfig.Tls();
    config.host = "logserver.example.com";
    config.port = 6514;

    var appender = DropwizardAppenderFactory.createTlsAppender(context, "tls", config);

    assertThat(appender).isInstanceOf(SSLSocketAppender.class);
    assertThat(appender.getName()).isEqualTo("tls");
    // No keystore configured -> the appender keeps its default SSL context (no key store wired in).
    assertThat(((SSLSocketAppender) appender).getSsl().getKeyStore()).isNull();
  }

  @Test
  public void testCreateTlsAppender_appliesKeyStoreConfiguration() {
    DropwizardAppenderConfig.Tls config = new DropwizardAppenderConfig.Tls();
    config.host = "logserver.example.com";
    config.port = 6514;
    config.keyStorePath = "/etc/ssl/client.p12";
    config.keyStorePassword = "secret";
    config.keyStoreType = "PKCS12";
    config.trustStorePath = "/etc/ssl/truststore.p12";

    var appender = (SSLSocketAppender) DropwizardAppenderFactory.createTlsAppender(context, "tls", config);

    // Bare filesystem paths (the form Dropwizard/Jetty accepted) must become file: URLs - logback's
    // KeyStoreFactoryBean resolves a scheme-less location as a classpath resource and the appender fails to start.
    assertThat(appender.getSsl()).isNotNull();
    assertThat(appender.getSsl().getKeyStore().getLocation()).isEqualTo("file:/etc/ssl/client.p12");
    assertThat(appender.getSsl().getKeyStore().getType()).isEqualTo("PKCS12");
    assertThat(appender.getSsl().getTrustStore().getLocation()).isEqualTo("file:/etc/ssl/truststore.p12");
  }

  @Test
  public void testCreateTlsAppender_bareTrustStorePath_startsWithRealKeyStore() throws Exception {
    // End-to-end proof for the file: mapping: a real PKCS12 file referenced by bare path must yield a started
    // appender (start() builds the SSL context; with the classpath misresolution it fails and never starts).
    File trustStore = File.createTempFile("truststore", ".p12");
    trustStore.deleteOnExit();
    KeyStore cacerts = KeyStore.getInstance(KeyStore.getDefaultType());
    try (InputStream in = Files.newInputStream(Path.of(System.getProperty("java.home"), "lib", "security",
        "cacerts")))
    {
      cacerts.load(in, null);
    }
    KeyStore keyStore = KeyStore.getInstance("PKCS12");
    keyStore.load(null, null);
    // JSSE rejects an empty trust store ("trustAnchors parameter must be non-empty"), so seed it with any CA cert.
    String alias = cacerts.aliases().nextElement();
    keyStore.setCertificateEntry("test-ca", cacerts.getCertificate(alias));
    try (FileOutputStream out = new FileOutputStream(trustStore)) {
      keyStore.store(out, "changeit".toCharArray());
    }
    DropwizardAppenderConfig.Tls config = new DropwizardAppenderConfig.Tls();
    // start() resolves the host (it does not connect), so it must be resolvable for the appender to start.
    config.host = "localhost";
    config.port = 6514;
    config.trustStorePath = trustStore.getAbsolutePath();
    config.trustStorePassword = "changeit";
    config.trustStoreType = "PKCS12";

    var appender = DropwizardAppenderFactory.createTlsAppender(context, "tls", config);

    assertThat(appender.isStarted()).isTrue();
  }

  @Test
  public void testToKeyStoreLocation_preservesExplicitSchemes() {
    assertThat(DropwizardAppenderFactory.toKeyStoreLocation("/etc/ssl/ts.p12")).isEqualTo("file:/etc/ssl/ts.p12");
    assertThat(DropwizardAppenderFactory.toKeyStoreLocation("file:/etc/ssl/ts.p12"))
        .isEqualTo("file:/etc/ssl/ts.p12");
    assertThat(DropwizardAppenderFactory.toKeyStoreLocation("classpath:ssl/ts.p12"))
        .isEqualTo("classpath:ssl/ts.p12");
  }

  @Test
  public void testCreateTlsAppender_appliesConnectionTimeout() {
    DropwizardAppenderConfig.Tls config = new DropwizardAppenderConfig.Tls();
    config.host = "logserver.example.com";
    config.port = 6514;
    config.connectionTimeout = "10 seconds";

    var appender = (SSLSocketAppender) DropwizardAppenderFactory.createTlsAppender(context, "tls", config);

    assertThat(appender.getReconnectionDelay().getMilliseconds()).isEqualTo(10_000L);
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

  @Test
  public void testToLevel_booleanAndStringValues() {
    // Bare YAML OFF/ON parse as Boolean (and strict conversion coerces them to "false"/"true"); both, plus explicit
    // OFF/INFO strings, must map like pre-Spring Dropwizard's DefaultLoggingFactory.toLevel.
    assertThat(DropwizardAppenderFactory.toLevel(Boolean.FALSE, Level.ALL)).isEqualTo(Level.OFF);
    assertThat(DropwizardAppenderFactory.toLevel("false", Level.ALL)).isEqualTo(Level.OFF);
    assertThat(DropwizardAppenderFactory.toLevel("OFF", Level.ALL)).isEqualTo(Level.OFF);
    assertThat(DropwizardAppenderFactory.toLevel(Boolean.TRUE, Level.OFF)).isEqualTo(Level.ALL);
    assertThat(DropwizardAppenderFactory.toLevel("INFO", Level.ALL)).isEqualTo(Level.INFO);
    assertThat(DropwizardAppenderFactory.toLevel(null, Level.ALL)).isEqualTo(Level.ALL);
    assertThat(DropwizardAppenderFactory.toLevel("   ", Level.ALL)).isEqualTo(Level.ALL);
  }
}
