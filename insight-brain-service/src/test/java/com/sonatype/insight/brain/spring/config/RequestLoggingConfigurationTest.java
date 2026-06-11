/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.net.SSLSocketAppender;
import ch.qos.logback.classic.net.SocketAppender;
import ch.qos.logback.classic.net.SyslogAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AsyncAppenderBase;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.telemetry.UserTelemetryRequestLoggingFilter;
import io.dropwizard.logging.common.AbstractAppenderFactory;
import io.dropwizard.logging.common.AppenderFactory;
import io.dropwizard.logging.common.ConsoleAppenderFactory;
import io.dropwizard.logging.common.FileAppenderFactory;
import io.dropwizard.logging.common.SyslogAppenderFactory;
import io.dropwizard.logging.common.TcpSocketAppenderFactory;
import io.dropwizard.logging.common.TlsSocketAppenderFactory;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jetty.JettyWebServer;
import org.springframework.boot.jetty.servlet.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServer;

public class RequestLoggingConfigurationTest
{
  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void shouldFilterTelemetryRequestsOutOfRequestLog() throws Exception {
    File requestLog = tempFolder.newFile("request.log");

    InsightConfig insightConfig =
        insightConfigWithRequestLog(requestLog.getAbsolutePath(), requestLogConfigWithThreshold("INFO"));

    RequestLoggingConfiguration configuration = new RequestLoggingConfiguration();
    UserTelemetryRequestLoggingFilter telemetryFilter = new UserTelemetryRequestLoggingFilter();
    JettyServletWebServerFactory factory = new JettyServletWebServerFactory(0);

    configuration.requestLoggingCustomizer(insightConfig, telemetryFilter).customize(factory);

    WebServer webServer = createWebServer(factory);

    try {
      webServer.start();
      int port = ((JettyWebServer) webServer).getPort();

      assertThat(sendRequest(port, "/rest/user-telemetry/events")).isEqualTo(200);
      assertThat(sendRequest(port, "/api/v2/applications")).isEqualTo(200);

      await().atMost(5, SECONDS).untilAsserted(() -> {
        assertThat(requestLog).exists();
        assertThat(Files.readString(requestLog.toPath())).contains("/api/v2/applications");
      });
    }
    finally {
      webServer.stop();
    }

    String logContents = Files.readString(requestLog.toPath());
    assertThat(logContents).contains("/api/v2/applications");
    assertThat(logContents).doesNotContain("/rest/user-telemetry/events");
  }

  @Test
  public void shouldFilterTelemetryRequestsOutOfAccessPathRequestLog() throws Exception {
    // The classic path's telemetry filtering is covered above; this covers the separate logback-access
    // (IAccessEvent) path used by the default no-section case, type: logback-access, and access-json configs.
    File requestLog = tempFolder.newFile("request-access.log");

    RequestLogConfig accessRequestLog = requestLogConfig("logback-access", List.of(
        Map.of("type", "file", "currentLogFilename", requestLog.getAbsolutePath(), "archive", false)));
    DropwizardServerConfig server = new DropwizardServerConfig();
    server.requestLog = accessRequestLog;
    InsightConfig insightConfig = new InsightConfig();
    insightConfig.setServer(server);

    RequestLoggingConfiguration configuration = new RequestLoggingConfiguration();
    UserTelemetryRequestLoggingFilter telemetryFilter = new UserTelemetryRequestLoggingFilter();
    JettyServletWebServerFactory factory = new JettyServletWebServerFactory(0);

    configuration.requestLoggingCustomizer(insightConfig, telemetryFilter).customize(factory);

    WebServer webServer = createWebServer(factory);

    try {
      webServer.start();
      int port = ((JettyWebServer) webServer).getPort();

      assertThat(sendRequest(port, "/rest/user-telemetry/events")).isEqualTo(200);
      assertThat(sendRequest(port, "/api/v2/applications")).isEqualTo(200);

      await().atMost(5, SECONDS).untilAsserted(() -> {
        assertThat(requestLog).exists();
        assertThat(Files.readString(requestLog.toPath())).contains("/api/v2/applications");
      });
    }
    finally {
      webServer.stop();
    }

    String logContents = Files.readString(requestLog.toPath());
    assertThat(logContents).contains("/api/v2/applications");
    assertThat(logContents).doesNotContain("/rest/user-telemetry/events");
  }

  @Test
  public void accessPathRendersLegacyNcsaLineFormat() throws Exception {
    // No test asserts the actual rendered request-log line. The access path is pre-migration's default factory, so
    // its output is the user-visible format the PR must preserve: the legacy IQ NCSA pattern rendered by logback-
    // access (clientHost, ident '-', user, [date], "request line", status, bytes, elapsed, "user-agent"), NOT a raw
    // %message dump. This locks that format so a future change to the access layout wiring is caught.
    File requestLog = tempFolder.newFile("request-format.log");

    RequestLogConfig accessRequestLog = requestLogConfig("logback-access", List.of(
        Map.of("type", "file", "currentLogFilename", requestLog.getAbsolutePath(), "archive", false)));
    DropwizardServerConfig server = new DropwizardServerConfig();
    server.requestLog = accessRequestLog;
    InsightConfig insightConfig = new InsightConfig();
    insightConfig.setServer(server);

    RequestLoggingConfiguration configuration = new RequestLoggingConfiguration();
    JettyServletWebServerFactory factory = new JettyServletWebServerFactory(0);
    configuration.requestLoggingCustomizer(insightConfig, new UserTelemetryRequestLoggingFilter()).customize(factory);

    WebServer webServer = createWebServer(factory);
    try {
      webServer.start();
      int port = ((JettyWebServer) webServer).getPort();

      assertThat(sendRequest(port, "/api/v2/applications")).isEqualTo(200);

      await().atMost(5, SECONDS)
          .untilAsserted(() -> assertThat(Files.readString(requestLog.toPath())).contains("/api/v2/applications"));
    }
    finally {
      webServer.stop();
    }

    String line = Files.readString(requestLog.toPath()).strip();
    // <clientHost> - <user> [<date>] "GET /api/v2/applications HTTP/1.1" 200 <bytes> <elapsed> "<user-agent>"
    assertThat(line).matches(
        "^\\S+ - \\S+ \\[[^\\]]+\\] \"GET /api/v2/applications HTTP/1\\.1\" 200 \\S+ \\S+ \".*\"$");
  }

  @Test
  public void shouldUseFirstAppenderLogFormatInClassicMode() {
    RequestLoggingConfiguration configuration = new RequestLoggingConfiguration();
    RequestLogConfig requestLog = requestLogConfig(null, List.of(
        Map.of("type", "console", "logFormat", "CONSOLE-FORMAT"),
        Map.of("type", "file", "threshold", "INFO", "logFormat", "FILE-FORMAT", "archivedFileCount", 5,
            "archivedLogFilenamePattern", "logs/request-%d.log.gz")));

    RequestLoggingConfiguration.RequestLogSettings requestLogSettings = configuration.requestLogSettings(requestLog);

    // Classic mode is single-format; the format is taken from the first appender that sets a logFormat (the console
    // appender here, where the shipped config places REQUEST_LOG_FORMAT).
    assertThat(requestLogSettings.format()).isEqualTo("CONSOLE-FORMAT");
  }

  @Test
  public void shouldFallBackToDefaultFormatWhenConfiguredFormatIsInvalid() {
    RequestLoggingConfiguration configuration = new RequestLoggingConfiguration();
    // Reproduces the shipped REQUEST_LOG_FORMAT default after env substitution displaces the brace of
    // %header{User-Agent} - the unconverted %header would otherwise crash Jetty's CustomRequestLog with "%h".
    RequestLogConfig requestLog = requestLogConfig(null, List.of(Map.of("type", "console", "logFormat",
        "%clientHost %l %user [%date] \"%requestURL\" %statusCode %bytesSent %elapsedTime \"%header{User-Agent\"}")));

    String format = configuration.requestLogSettings(requestLog).format();

    assertThat(format).doesNotContain("%header");
    assertThat(format).isEqualTo(
        RequestLoggingConfiguration.DEFAULT_REQUEST_LOG_FORMAT.replace("__TIME_ZONE__", "UTC"));
  }

  @Test
  public void shouldConvertLegacyFormatToSingleBracketDateAndMillisecondElapsed() {
    RequestLoggingConfiguration configuration = new RequestLoggingConfiguration();
    // A customer variant of the legacy format (extra literal so it is not the verbatim legacy string and goes
    // through conversion). Jetty's %t self-brackets the date, so "[%date]" must convert to a bare %t (not "[%t]",
    // which renders [[date]]); %elapsedTime was milliseconds in logback-access, so it maps to %{ms}T, not the
    // microsecond %D.
    RequestLogConfig requestLog = requestLogConfig(null, List.of(Map.of("type", "console", "logFormat",
        "x %clientHost %l %user [%date] \"%requestURL\" %statusCode %bytesSent %elapsedTime"
            + " \"%header{User-Agent}\"")));

    String format = configuration.requestLogSettings(requestLog).format();

    assertThat(format).contains("%{dd/MMM/yyyy:HH:mm:ss Z|UTC}t");
    assertThat(format).doesNotContain("[%{dd/MMM/yyyy:HH:mm:ss Z|UTC}t]");
    assertThat(format).contains("%{ms}T");
    assertThat(format).doesNotContain("%D");
  }

  @Test
  public void shouldIgnoreLogFormatOfOffAppendersInClassicMode() {
    RequestLoggingConfiguration configuration = new RequestLoggingConfiguration();
    // A disabled (threshold OFF) appender must not dictate the single classic format - consistent with
    // resolveActiveAppenders, which excludes OFF appenders from the request log entirely.
    RequestLogConfig requestLog = requestLogConfig(null, List.of(
        Map.of("type", "file", "threshold", "OFF", "currentLogFilename", "off.log", "logFormat", "OFF-FORMAT"),
        Map.of("type", "console")));

    String format = configuration.requestLogSettings(requestLog).format();

    assertThat(format).isNotEqualTo("OFF-FORMAT");
    assertThat(format).isEqualTo(
        RequestLoggingConfiguration.DEFAULT_REQUEST_LOG_FORMAT.replace("__TIME_ZONE__", "UTC"));
  }

  @Test
  public void shouldConvertDocumentedReverseProxyHeaderFormats() {
    RequestLoggingConfiguration configuration = new RequestLoggingConfiguration();
    // The logging help docs tell reverse-proxy users to extend the format with extra %header{...} tokens
    // (REMOTE_USER, x-forwarded-*). Each must convert to Jetty's %{NAME}i; an unconverted %header reads as the
    // unsupported %h code and the whole format would fall back to the default, silently dropping those columns.
    RequestLogConfig requestLog = requestLogConfig(null, List.of(Map.of("type", "console", "logFormat",
        "%clientHost %l %user [%date] \"%requestURL\" %statusCode %bytesSent %elapsedTime \"%header{User-Agent}\""
            + " %header{REMOTE_USER} %header{x-forwarded-host} %header{x-forwarded-proto}")));

    String format = configuration.requestLogSettings(requestLog).format();

    assertThat(format).contains("%{User-Agent}i");
    assertThat(format).contains("%{REMOTE_USER}i");
    assertThat(format).contains("%{x-forwarded-host}i");
    assertThat(format).contains("%{x-forwarded-proto}i");
    assertThat(format).doesNotContain("%header");
    assertThat(format).isNotEqualTo(
        RequestLoggingConfiguration.DEFAULT_REQUEST_LOG_FORMAT.replace("__TIME_ZONE__", "UTC"));
  }

  @Test
  public void shouldDeriveArchivePatternFromRequestLogFilenameWhenMissing() {
    RequestLoggingConfiguration configuration = new RequestLoggingConfiguration();

    assertThat(DropwizardAppenderFactory.deriveArchivePattern("/var/log/request.log"))
        .isEqualTo("/var/log/request-%d.log");
    assertThat(DropwizardAppenderFactory.deriveArchivePattern("request"))
        .isEqualTo("request-%d");
  }

  @Test
  public void shouldDisableRequestLoggingForUnsupportedRequestLogType() {
    RequestLoggingConfiguration configuration = new RequestLoggingConfiguration();
    RequestLogConfig requestLog = requestLogConfig("access", List.of(Map.of("type", "file", "threshold", "INFO")));

    assertThat(configuration.requestLogSettings(requestLog).enabled()).isFalse();
  }

  @Test
  public void shouldEnableRequestLoggingForLogbackAccessType() {
    RequestLoggingConfiguration configuration = new RequestLoggingConfiguration();
    RequestLogConfig requestLog = requestLogConfig("logback-access", List.of(Map.of("type", "console")));

    // requestLogSettings treats 'logback-access' as enabled. In production the customizer routes that type to the
    // access (IAccessEvent) path rather than here; requestLogSettings drives only the classic path.
    assertThat(configuration.requestLogSettings(requestLog).enabled()).isTrue();
  }

  @Test
  public void shouldUsePlainFileAppenderWhenArchiveIsDisabled() throws Exception {
    File requestLog = tempFolder.newFile("request-no-archive.log");
    RequestLoggingConfiguration configuration = new RequestLoggingConfiguration();

    configuration.requestLogWriter(requestLog.getAbsolutePath(), List.of(
        Map.of("type", "file", "threshold", "INFO", "archive", false)));

    Logger logger = requestLogLogger(requestLog);
    try {
      Appender<ILoggingEvent> inner = unwrapAsync(logger.iteratorForAppenders().next());
      assertThat(inner).isInstanceOf(FileAppender.class);
      assertThat(inner).isNotInstanceOf(RollingFileAppender.class);
    }
    finally {
      logger.detachAndStopAllAppenders();
    }
  }

  @Test
  public void shouldInstallConsoleRequestLoggingWhenFileAppenderThresholdIsOff() throws Exception {
    File requestLog = tempFolder.newFile("request-disabled.log");

    InsightConfig insightConfig =
        insightConfigWithRequestLog(requestLog.getAbsolutePath(), requestLogConfigWithThreshold("OFF"));

    RequestLoggingConfiguration configuration = new RequestLoggingConfiguration();
    UserTelemetryRequestLoggingFilter telemetryFilter = new UserTelemetryRequestLoggingFilter();
    JettyServletWebServerFactory factory = new JettyServletWebServerFactory(0);

    configuration.requestLoggingCustomizer(insightConfig, telemetryFilter).customize(factory);

    WebServer webServer = createWebServer(factory);
    Logger logger = requestLogLogger(requestLog);

    try {
      webServer.start();
      int port = ((JettyWebServer) webServer).getPort();

      assertThat(sendRequest(port, "/api/v2/applications")).isEqualTo(200);

      // Shipped default (console active, file OFF): request logging is installed via the console appender only;
      // the file appender is skipped, so the request log file is never opened. Matches pre-Spring behaviour.
      List<Appender<ILoggingEvent>> appenders = appendersOf(logger);
      assertThat(appenders).hasSize(1);
      assertThat(unwrapAsync(appenders.get(0))).isInstanceOf(ConsoleAppender.class);
    }
    finally {
      webServer.stop();
      logger.detachAndStopAllAppenders();
    }

    assertThat(Files.readString(requestLog.toPath())).isEmpty();
  }

  @Test
  public void shouldSkipFileAppenderWhenThresholdIsBareOffFromYaml() throws Exception {
    // The shipped config uses an unquoted `threshold: OFF`, which YAML parses as a Boolean; it must still be treated
    // as OFF so the file appender is skipped (console only). Goes through the real reader, not Map.of with strings.
    File requestLog = tempFolder.newFile("request-yaml-off.log");
    File configFile = tempFolder.newFile("config-off.yml");
    Files.writeString(configFile.toPath(), String.join("\n",
        "server:",
        "  requestLog:",
        "    type: classic",
        "    appenders:",
        "      - type: console",
        "      - type: file",
        "        threshold: OFF",
        "        currentLogFilename: " + requestLog.getAbsolutePath(),
        "        archive: false",
        ""));

    InsightConfig insightConfig = new DropwizardConfigConfiguration()
        .insightConfig(configFile.getAbsolutePath(), InsightConfig.class.getName());

    RequestLoggingConfiguration configuration = new RequestLoggingConfiguration();
    UserTelemetryRequestLoggingFilter telemetryFilter = new UserTelemetryRequestLoggingFilter();
    JettyServletWebServerFactory factory = new JettyServletWebServerFactory(0);
    configuration.requestLoggingCustomizer(insightConfig, telemetryFilter).customize(factory);

    WebServer webServer = createWebServer(factory);
    Logger logger = requestLogLogger(requestLog);
    try {
      webServer.start();
      int port = ((JettyWebServer) webServer).getPort();

      assertThat(sendRequest(port, "/api/v2/applications")).isEqualTo(200);

      List<Appender<ILoggingEvent>> appenders = appendersOf(logger);
      assertThat(appenders).hasSize(1);
      assertThat(unwrapAsync(appenders.get(0))).isInstanceOf(ConsoleAppender.class);
    }
    finally {
      webServer.stop();
      logger.detachAndStopAllAppenders();
    }

    assertThat(Files.readString(requestLog.toPath())).isEmpty();
  }

  @Test
  public void requestLogWriterWrapsFileAppenderInNoLossAsync() throws Exception {
    File requestLog = tempFolder.newFile("request-async.log");
    RequestLoggingConfiguration configuration = new RequestLoggingConfiguration();

    configuration.requestLogWriter(requestLog.getAbsolutePath(),
        List.of(Map.of("type", "file", "threshold", "INFO")));

    Logger logger = requestLogLogger(requestLog);
    Appender<ILoggingEvent> appender = logger.iteratorForAppenders().next();
    try {
      assertThat(appender).isInstanceOf(AsyncAppender.class);
      AsyncAppender async = (AsyncAppender) appender;
      assertThat(async.getDiscardingThreshold()).isEqualTo(0);
      assertThat(async.isNeverBlock()).isFalse();
      assertThat(async.iteratorForAppenders().next()).isInstanceOf(FileAppender.class);
    }
    finally {
      logger.detachAndStopAllAppenders();
    }
  }

  @Test
  public void requestLogWriterHonoursEachFileAppendersOwnFilename() throws Exception {
    File fileA = tempFolder.newFile("request-a.log");
    File fileB = tempFolder.newFile("request-b.log");
    RequestLoggingConfiguration configuration = new RequestLoggingConfiguration();

    // Two file appenders, each with its own currentLogFilename - pre-Spring wrote a separate file per appender
    // instead of collapsing them onto the single resolved request-log filename.
    configuration.requestLogWriter(fileA.getAbsolutePath(), List.of(
        Map.of("type", "file", "threshold", "INFO", "currentLogFilename", fileA.getAbsolutePath(), "archive", false),
        Map.of("type", "file", "threshold", "INFO", "currentLogFilename", fileB.getAbsolutePath(), "archive", false)));

    Logger logger = requestLogLogger(fileA);
    try {
      List<String> files = new ArrayList<>();
      logger.iteratorForAppenders()
          .forEachRemaining(
              appender -> files.add(((FileAppender<ILoggingEvent>) unwrapAsync(appender)).getFile()));
      assertThat(files).containsExactlyInAnyOrder(fileA.getAbsolutePath(), fileB.getAbsolutePath());
    }
    finally {
      logger.detachAndStopAllAppenders();
    }
  }

  @Test
  public void requestLogWriterHonoursConfiguredAsyncOverridesOnFileAppender() throws Exception {
    File requestLog = tempFolder.newFile("request-async-overrides.log");
    RequestLoggingConfiguration configuration = new RequestLoggingConfiguration();
    configuration.requestLogWriter(requestLog.getAbsolutePath(), List.of(Map.of(
        "type", "file",
        "threshold", "INFO",
        "queueSize", 512,
        "discardingThreshold", 25,
        "neverBlock", true)));

    Logger logger = requestLogLogger(requestLog);
    try {
      AsyncAppender async = (AsyncAppender) logger.iteratorForAppenders().next();
      assertThat(async.getQueueSize()).isEqualTo(512);
      assertThat(async.getDiscardingThreshold()).isEqualTo(25);
      assertThat(async.isNeverBlock()).isTrue();
    }
    finally {
      logger.detachAndStopAllAppenders();
    }
  }

  @Test
  public void requestLogWriterAttachesConsoleAndFileEachInNoLossAsync() throws Exception {
    File requestLog = tempFolder.newFile("request-both.log");
    RequestLoggingConfiguration configuration = new RequestLoggingConfiguration();
    configuration.requestLogWriter(requestLog.getAbsolutePath(), List.of(
        Map.of("type", "console"),
        Map.of("type", "file", "threshold", "INFO")));

    Logger logger = requestLogLogger(requestLog);
    try {
      List<Appender<ILoggingEvent>> appenders = appendersOf(logger);
      assertThat(appenders).hasSize(2);
      assertThat(appenders).allMatch(a -> a instanceof AsyncAppender);
      assertThat(appenders).noneMatch(a -> ((AsyncAppender) a).isNeverBlock());
      assertThat(appenders).allMatch(a -> ((AsyncAppender) a).getDiscardingThreshold() == 0);
      assertThat(appenders.stream().map(this::unwrapAsync).toList())
          .hasAtLeastOneElementOfType(ConsoleAppender.class)
          .hasAtLeastOneElementOfType(FileAppender.class);
    }
    finally {
      logger.detachAndStopAllAppenders();
    }
  }

  @Test
  public void requestLogWriterReadsAsyncSettingsPerAppender() throws Exception {
    File requestLog = tempFolder.newFile("request-per-appender.log");
    RequestLoggingConfiguration configuration = new RequestLoggingConfiguration();
    configuration.requestLogWriter(requestLog.getAbsolutePath(), List.of(
        Map.of("type", "console", "queueSize", 128, "neverBlock", true),
        Map.of("type", "file", "threshold", "INFO", "queueSize", 512)));

    Logger logger = requestLogLogger(requestLog);
    try {
      AsyncAppender console = (AsyncAppender) appenderWithInner(logger, ConsoleAppender.class);
      AsyncAppender file = (AsyncAppender) appenderWithInner(logger, FileAppender.class);
      assertThat(console.getQueueSize()).isEqualTo(128);
      assertThat(console.isNeverBlock()).isTrue();
      assertThat(file.getQueueSize()).isEqualTo(512);
      assertThat(file.isNeverBlock()).isFalse();
    }
    finally {
      logger.detachAndStopAllAppenders();
    }
  }

  @Test
  public void requestLogWriterAppliesThresholdFilterOnlyWhenThresholdPresent() throws Exception {
    File requestLog = tempFolder.newFile("request-threshold.log");
    RequestLoggingConfiguration configuration = new RequestLoggingConfiguration();
    configuration.requestLogWriter(requestLog.getAbsolutePath(), List.of(
        Map.of("type", "console"),
        Map.of("type", "file", "threshold", "INFO")));

    Logger logger = requestLogLogger(requestLog);
    try {
      assertThat(unwrapAsync(appenderWithInner(logger, ConsoleAppender.class))
          .getCopyOfAttachedFiltersList()).isEmpty();
      assertThat(unwrapAsync(appenderWithInner(logger, FileAppender.class))
          .getCopyOfAttachedFiltersList()).isNotEmpty();
    }
    finally {
      logger.detachAndStopAllAppenders();
    }
  }

  @Test
  public void requestLogWriterSupportsSyslogTcpAndTlsAppenders() throws Exception {
    File requestLog = tempFolder.newFile("request-extended.log");
    RequestLoggingConfiguration configuration = new RequestLoggingConfiguration();

    configuration.requestLogWriter(requestLog.getAbsolutePath(), List.of(
        Map.of("type", "syslog", "host", "localhost", "port", 514),
        Map.of("type", "tcp", "host", "localhost", "port", 4560),
        Map.of("type", "tls", "host", "localhost", "port", 6514)));

    Logger logger = requestLogLogger(requestLog);
    try {
      List<Appender<ILoggingEvent>> inner = appendersOf(logger).stream().map(this::unwrapAsync).toList();
      assertThat(inner).hasSize(3);
      assertThat(inner).hasAtLeastOneElementOfType(SyslogAppender.class);
      assertThat(inner).hasAtLeastOneElementOfType(SocketAppender.class);
      assertThat(inner).hasAtLeastOneElementOfType(SSLSocketAppender.class);
    }
    finally {
      logger.detachAndStopAllAppenders();
    }
  }

  @Test
  public void requestLogWriterReturnsNullWhenNoAppendersAttach() {
    RequestLoggingConfiguration configuration = new RequestLoggingConfiguration();

    // A file appender but no request-log filename: nothing attaches, so the writer is null and the customizer
    // skips installing request logging (rather than attaching a zero-appender logger that logback would warn about).
    var writer = configuration.requestLogWriter(null, List.of(Map.of("type", "file", "threshold", "INFO")));

    assertThat(writer).isNull();
  }

  @Test
  public void requestLogWriterRejectsUnknownAppenderField() {
    RequestLoggingConfiguration configuration = new RequestLoggingConfiguration();

    // Unknown keys on a request-log appender are rejected (strict parsing) rather than silently ignored, matching
    // pre-Spring Dropwizard parsing and the application-logging path.
    assertThatThrownBy(() -> configuration.requestLogWriter("/tmp/request.log",
        List.of(Map.of("type", "file", "currentLogFilename", "/tmp/request.log", "bogusUnknownKey", "x"))))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void shouldIgnoreUdpRequestLogAppender() {
    RequestLoggingConfiguration configuration = new RequestLoggingConfiguration();
    RequestLogConfig requestLog = requestLogConfig(null, List.of(Map.of("type", "udp", "host", "localhost")));

    // udp is recognized (so it doesn't disable request logging) but ignored, leaving no active appenders.
    assertThat(configuration.requestLogSettings(requestLog).enabled()).isFalse();
  }

  @Test
  public void shouldDisableRequestLoggingWhenAllAppendersAreOff() {
    RequestLoggingConfiguration configuration = new RequestLoggingConfiguration();
    RequestLogConfig requestLog = requestLogConfig(null, List.of(
        Map.of("type", "console", "threshold", "OFF"),
        Map.of("type", "file", "threshold", "OFF")));

    assertThat(configuration.requestLogSettings(requestLog).enabled()).isFalse();
  }

  @Test
  public void requestLogSettingsDoesNotFailOnAppenderWithoutType() {
    RequestLoggingConfiguration configuration = new RequestLoggingConfiguration();
    RequestLogConfig requestLog = requestLogConfig(null, List.of(Map.of("currentLogFilename", "request.log")));

    // An appender entry without a 'type' must be treated as unsupported (request logging disabled), not crash
    // with an NPE from Set.of(...).contains(null).
    assertThat(configuration.requestLogSettings(requestLog).enabled()).isFalse();
  }

  @Test
  public void accessAppenderFactoriesParseConsoleWithAccessJsonLayout() {
    RequestLoggingConfiguration configuration = new RequestLoggingConfiguration();

    List<AppenderFactory<IAccessEvent>> factories = configuration.accessAppenderFactories(
        List.of(Map.of("type", "console", "layout", Map.of("type", "access-json"))));

    assertThat(factories).hasSize(1);
    assertThat(factories.get(0)).isInstanceOf(ConsoleAppenderFactory.class);
  }

  @Test
  public void accessAppenderFactoriesRespectConfiguredAsyncSettings() {
    RequestLoggingConfiguration configuration = new RequestLoggingConfiguration();

    List<AppenderFactory<IAccessEvent>> factories = configuration.accessAppenderFactories(List.of(Map.of(
        "type", "console",
        "layout", Map.of("type", "access-json"),
        "queueSize", 128,
        "discardingThreshold", 64)));

    // Async settings are read from config rather than force-overridden, consistent with the application-log path.
    AbstractAppenderFactory<IAccessEvent> factory = (AbstractAppenderFactory<IAccessEvent>) factories.get(0);
    assertThat(factory.getQueueSize()).isEqualTo(128);
    assertThat(factory.getDiscardingThreshold()).isEqualTo(64);
  }

  @Test
  public void accessAppenderFactoriesSupportEveryAppenderType() {
    RequestLoggingConfiguration configuration = new RequestLoggingConfiguration();

    List<AppenderFactory<IAccessEvent>> factories = configuration.accessAppenderFactories(List.of(
        Map.of("type", "console", "layout", Map.of("type", "access-json")),
        Map.of("type", "file", "currentLogFilename", "/tmp/access.log", "archive", false,
            "layout", Map.of("type", "access-json")),
        Map.of("type", "syslog", "host", "127.0.0.1", "port", 514, "layout", Map.of("type", "access-json")),
        Map.of("type", "tcp", "host", "127.0.0.1", "port", 4560, "layout", Map.of("type", "access-json")),
        Map.of("type", "tls", "host", "127.0.0.1", "port", 4560, "layout", Map.of("type", "access-json"))));

    assertThat(factories).hasSize(5);
    assertThat(factories.get(0)).isInstanceOf(ConsoleAppenderFactory.class);
    assertThat(factories.get(1)).isInstanceOf(FileAppenderFactory.class);
    assertThat(factories.get(2)).isInstanceOf(SyslogAppenderFactory.class);
    assertThat(factories.get(3)).isInstanceOf(TcpSocketAppenderFactory.class);
    assertThat(factories.get(4)).isInstanceOf(TlsSocketAppenderFactory.class);
  }

  @Test
  public void accessAppenderFactoriesPreserveDistinctPerAppenderLogFormats() {
    RequestLoggingConfiguration configuration = new RequestLoggingConfiguration();

    // Under logback-access each appender keeps its OWN logFormat (per-appender layouts), unlike the classic
    // single-format path.
    List<AppenderFactory<IAccessEvent>> factories = configuration.accessAppenderFactories(List.of(
        Map.of("type", "console", "logFormat", "CONSOLE-FMT"),
        Map.of("type", "file", "currentLogFilename", "/tmp/access.log", "archive", false, "logFormat", "FILE-FMT"),
        Map.of("type", "syslog", "host", "127.0.0.1", "port", 514, "logFormat", "SYSLOG-FMT")));

    assertThat(factories).hasSize(3);
    assertThat(((AbstractAppenderFactory<IAccessEvent>) factories.get(0)).getLogFormat()).isEqualTo("CONSOLE-FMT");
    assertThat(((AbstractAppenderFactory<IAccessEvent>) factories.get(1)).getLogFormat()).isEqualTo("FILE-FMT");
    assertThat(((AbstractAppenderFactory<IAccessEvent>) factories.get(2)).getLogFormat()).isEqualTo("SYSLOG-FMT");
  }

  @Test
  public void accessAppenderFactoriesInjectDefaultFormatWhenNeitherLayoutNorLogFormatSet() {
    RequestLoggingConfiguration configuration = new RequestLoggingConfiguration();

    List<AppenderFactory<IAccessEvent>> factories = configuration.accessAppenderFactories(List.of(
        Map.of("type", "console"),
        Map.of("type", "console", "layout", Map.of("type", "access-json"))));

    // A plain appender gets the IQ default format; an access-json appender keeps its layout (no logFormat injected).
    assertThat(((AbstractAppenderFactory<IAccessEvent>) factories.get(0)).getLogFormat())
        .isEqualTo(RequestLoggingConfiguration.LEGACY_REQUEST_LOG_FORMAT);
    assertThat(((AbstractAppenderFactory<IAccessEvent>) factories.get(1)).getLogFormat()).isNull();
  }

  @Test
  public void accessAppenderFactoriesDropAppendersWithOffThreshold() {
    RequestLoggingConfiguration configuration = new RequestLoggingConfiguration();

    List<AppenderFactory<IAccessEvent>> factories = configuration.accessAppenderFactories(List.of(
        Map.of("type", "console"),
        Map.of("type", "file", "threshold", "OFF", "currentLogFilename", "/tmp/access.log")));

    assertThat(factories).hasSize(1);
    assertThat(factories.get(0)).isInstanceOf(ConsoleAppenderFactory.class);
  }

  @Test
  public void accessAppenderFactoriesDeriveArchivePatternFromCurrentLogFilename() {
    RequestLoggingConfiguration configuration = new RequestLoggingConfiguration();

    List<AppenderFactory<IAccessEvent>> factories = configuration.accessAppenderFactories(List.of(
        Map.of("type", "file", "currentLogFilename", "/tmp/access.log")));

    // archive defaults to on, which requires an archive pattern (pre-Spring's bean validation enforced this);
    // convertValue runs no validation, so the pattern is derived like the classic path does instead of building a
    // rolling appender with no pattern that silently fails to start.
    FileAppenderFactory<IAccessEvent> file = (FileAppenderFactory<IAccessEvent>) factories.get(0);
    assertThat(file.getArchivedLogFilenamePattern()).isEqualTo("/tmp/access-%d.log");
  }

  @Test
  public void accessAppenderFactoriesSkipFileAppenderWithNoTargetFile() {
    RequestLoggingConfiguration configuration = new RequestLoggingConfiguration();

    // No currentLogFilename and no archive pattern: nothing to write to, so the appender is warned-and-skipped
    // (consistent with the classic path) rather than silently failing to start.
    List<AppenderFactory<IAccessEvent>> factories = configuration.accessAppenderFactories(List.of(
        Map.of("type", "console"),
        Map.of("type", "file")));

    assertThat(factories).hasSize(1);
    assertThat(factories.get(0)).isInstanceOf(ConsoleAppenderFactory.class);
  }

  @Test
  public void accessAppenderFactoriesSkipUdpAppenders() {
    RequestLoggingConfiguration configuration = new RequestLoggingConfiguration();

    // udp is warned-and-skipped on the access path too (consistent with the classic path).
    List<AppenderFactory<IAccessEvent>> factories = configuration.accessAppenderFactories(List.of(
        Map.of("type", "console"),
        Map.of("type", "udp", "host", "127.0.0.1", "port", 514)));

    assertThat(factories).hasSize(1);
    assertThat(factories.get(0)).isInstanceOf(ConsoleAppenderFactory.class);
  }

  @Test
  public void accessAppenderFactoriesRejectUnknownAppenderKeys() {
    RequestLoggingConfiguration configuration = new RequestLoggingConfiguration();

    // Strict parsing, consistent with the classic path and pre-Spring: a typo'd key fails rather than being ignored.
    assertThatThrownBy(() -> configuration.accessAppenderFactories(
        List.of(Map.of("type", "console", "totallyBogusKey", "x"))))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void shouldInstallDefaultConsoleRequestLogWhenSectionOmitted() throws Exception {
    DropwizardServerConfig server = new DropwizardServerConfig();
    InsightConfig insightConfig = new InsightConfig();
    insightConfig.setServer(server);

    RequestLoggingConfiguration configuration = new RequestLoggingConfiguration();
    JettyServletWebServerFactory factory = new JettyServletWebServerFactory(0);
    configuration.requestLoggingCustomizer(insightConfig, new UserTelemetryRequestLoggingFilter()).customize(factory);

    WebServer webServer = createWebServer(factory);
    try {
      webServer.start();
      // No server.requestLog section -> pre-Spring default-on: a request log is installed.
      assertThat(((JettyWebServer) webServer).getServer().getRequestLog()).isNotNull();
    }
    finally {
      webServer.stop();
    }
  }

  @Test
  public void shouldNotInstallRequestLogForExternalType() throws Exception {
    DropwizardServerConfig server = new DropwizardServerConfig();
    RequestLogConfig requestLog = new RequestLogConfig();
    requestLog.type = "external";
    requestLog.enabled = true;
    server.requestLog = requestLog;
    InsightConfig insightConfig = new InsightConfig();
    insightConfig.setServer(server);

    RequestLoggingConfiguration configuration = new RequestLoggingConfiguration();
    JettyServletWebServerFactory factory = new JettyServletWebServerFactory(0);
    configuration.requestLoggingCustomizer(insightConfig, new UserTelemetryRequestLoggingFilter()).customize(factory);

    WebServer webServer = createWebServer(factory);
    try {
      webServer.start();
      // type: external installs no request log (and does not throw).
      assertThat(((JettyWebServer) webServer).getServer().getRequestLog()).isNull();
    }
    finally {
      webServer.stop();
    }
  }

  private Logger requestLogLogger(final File requestLog) {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    return context.getLogger(RequestLoggingConfiguration.REQUEST_LOGGER_NAME_PREFIX
        + Integer.toHexString(requestLog.getAbsolutePath().hashCode()));
  }

  private List<Appender<ILoggingEvent>> appendersOf(final Logger logger) {
    List<Appender<ILoggingEvent>> appenders = new ArrayList<>();
    logger.iteratorForAppenders().forEachRemaining(appenders::add);
    return appenders;
  }

  @SuppressWarnings("unchecked")
  private Appender<ILoggingEvent> unwrapAsync(final Appender<ILoggingEvent> appender) {
    if (appender instanceof AsyncAppenderBase<?> async) {
      return (Appender<ILoggingEvent>) async.iteratorForAppenders().next();
    }
    return appender;
  }

  private Appender<ILoggingEvent> appenderWithInner(final Logger logger, final Class<?> innerType) {
    return appendersOf(logger).stream()
        .filter(appender -> innerType.isInstance(unwrapAsync(appender)))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No request-log appender wrapping " + innerType.getSimpleName()));
  }

  private RequestLogConfig requestLogConfig(final String type, final List<Map<String, Object>> appenders) {
    RequestLogConfig config = new RequestLogConfig();
    config.type = type;
    config.timeZone = "UTC";
    config.appenders = appenders;
    return config;
  }

  private RequestLogConfig requestLogConfigWithThreshold(final String threshold) {
    // type: classic exercises the single-format Jetty CustomRequestLog path these tests assert on.
    return requestLogConfig("classic", List.of(
        Map.of("type", "console", "logFormat",
            "%clientHost %l %user [%date] \"%requestURL\" %statusCode %bytesSent %elapsedTime \"%header{User-Agent}\""),
        Map.of("type", "file", "threshold", threshold, "archivedFileCount", 5,
            "archivedLogFilenamePattern", "logs/request-%d.log.gz")));
  }

  private InsightConfig insightConfigWithRequestLog(
      final String requestLogFilename,
      final RequestLogConfig requestLog)
  {
    DropwizardServerConfig server = new DropwizardServerConfig();
    server.requestLog = requestLog;
    InsightConfig config = new InsightConfig();
    config.setServer(server);
    config.setRequestLogFilename(requestLogFilename);
    return config;
  }

  private WebServer createWebServer(final JettyServletWebServerFactory factory) {
    return factory.getWebServer(servletContext -> {
      HttpServlet servlet = new HttpServlet()
      {
        @Override
        protected void doGet(final HttpServletRequest request, final HttpServletResponse response) {
          response.setStatus(HttpServletResponse.SC_OK);
        }
      };
      servletContext.addServlet("test-servlet", servlet).addMapping("/*");
    });
  }

  private int sendRequest(final int port, final String path) throws Exception {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("http://127.0.0.1:" + port + path))
        .GET()
        .build();

    return HttpClient.newHttpClient()
        .send(request, HttpResponse.BodyHandlers.discarding())
        .statusCode();
  }
}
