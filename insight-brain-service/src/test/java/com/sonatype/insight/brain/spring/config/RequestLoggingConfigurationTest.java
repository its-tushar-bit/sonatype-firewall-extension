/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.telemetry.UserTelemetryRequestLoggingFilter;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jetty.JettyWebServer;
import org.springframework.boot.jetty.servlet.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

public class RequestLoggingConfigurationTest
{
  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void shouldFilterTelemetryRequestsOutOfRequestLog() throws Exception {
    File requestLog = tempFolder.newFile("request.log");

    InsightConfig insightConfig = new InsightConfig();
    insightConfig.setRequestLogFilename(requestLog.getAbsolutePath());

    RequestLoggingConfiguration configuration = new RequestLoggingConfiguration();
    UserTelemetryRequestLoggingFilter telemetryFilter = new UserTelemetryRequestLoggingFilter();
    JettyServletWebServerFactory factory = new JettyServletWebServerFactory(0);
    StandardEnvironment environment = environmentWithRequestLogThreshold("INFO");

    configuration.requestLoggingCustomizer(insightConfig, telemetryFilter, environment).customize(factory);

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
  public void shouldPreferFileAppenderLogFormatWhenPresent() {
    RequestLoggingConfiguration configuration = new RequestLoggingConfiguration();
    StandardEnvironment environment = new StandardEnvironment();
    environment.getPropertySources()
        .addFirst(new MapPropertySource("dropwizardConfig", Map.of(
            "server.requestLog.timeZone", "UTC",
            "server.requestLog.appenders", List.of(
                Map.of(
                    "type", "console",
                    "logFormat", "CONSOLE-FORMAT"),
                Map.of(
                    "type", "file",
                    "threshold", "INFO",
                    "logFormat", "FILE-FORMAT",
                    "archivedFileCount", 5,
                    "archivedLogFilenamePattern", "logs/request-%d.log.gz")))));

    RequestLoggingConfiguration.RequestLogSettings requestLogSettings = configuration.requestLogSettings(environment);

    assertThat(requestLogSettings.format()).isEqualTo("FILE-FORMAT");
    assertThat(requestLogSettings.archivedLogFilenamePattern()).isEqualTo("logs/request-%d.log.gz");
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
    StandardEnvironment environment = new StandardEnvironment();
    environment.getPropertySources()
        .addFirst(new MapPropertySource("dropwizardConfig", Map.of(
            "server.requestLog.type", "access",
            "server.requestLog.timeZone", "UTC",
            "server.requestLog.appenders", List.of(
                Map.of(
                    "type", "file",
                    "threshold", "INFO")))));

    assertThat(configuration.requestLogSettings(environment).enabled()).isFalse();
  }

  @Test
  public void shouldUsePlainFileAppenderWhenArchiveIsDisabled() throws Exception {
    File requestLog = tempFolder.newFile("request-no-archive.log");

    RequestLoggingConfiguration configuration = new RequestLoggingConfiguration();
    StandardEnvironment environment = new StandardEnvironment();
    environment.getPropertySources()
        .addFirst(new MapPropertySource("dropwizardConfig", Map.of(
            "server.requestLog.timeZone", "UTC",
            "server.requestLog.appenders", List.of(
                Map.of(
                    "type", "file",
                    "threshold", "INFO",
                    "archive", false,
                    "archivedLogFilenamePattern", "logs/request-%d.log.gz")))));

    RequestLoggingConfiguration.RequestLogSettings requestLogSettings = configuration.requestLogSettings(environment);
    Appender<ILoggingEvent> appender = configuration.requestLogAppender(
        (LoggerContext) LoggerFactory.getILoggerFactory(),
        "test.requestlog",
        requestLog.getAbsolutePath(),
        requestLogSettings);

    try {
      assertThat(requestLogSettings.archiveEnabled()).isFalse();
      assertThat(appender).isInstanceOf(FileAppender.class);
      assertThat(appender).isNotInstanceOf(RollingFileAppender.class);
    }
    finally {
      if (appender instanceof FileAppender<?> fileAppender) {
        fileAppender.stop();
      }
    }
  }

  @Test
  public void shouldNotInstallRequestLoggingWhenFileAppenderThresholdIsOff() throws Exception {
    File requestLog = tempFolder.newFile("request-disabled.log");

    InsightConfig insightConfig = new InsightConfig();
    insightConfig.setRequestLogFilename(requestLog.getAbsolutePath());

    RequestLoggingConfiguration configuration = new RequestLoggingConfiguration();
    UserTelemetryRequestLoggingFilter telemetryFilter = new UserTelemetryRequestLoggingFilter();
    JettyServletWebServerFactory factory = new JettyServletWebServerFactory(0);
    StandardEnvironment environment = environmentWithRequestLogThreshold("OFF");

    configuration.requestLoggingCustomizer(insightConfig, telemetryFilter, environment).customize(factory);

    WebServer webServer = createWebServer(factory);

    try {
      webServer.start();
      int port = ((JettyWebServer) webServer).getPort();

      assertThat(sendRequest(port, "/api/v2/applications")).isEqualTo(200);
    }
    finally {
      webServer.stop();
    }

    assertThat(Files.readString(requestLog.toPath())).isEmpty();
  }

  private StandardEnvironment environmentWithRequestLogThreshold(final String threshold) {
    StandardEnvironment environment = new StandardEnvironment();
    environment.getPropertySources()
        .addFirst(new MapPropertySource("dropwizardConfig", Map.of(
            "server.requestLog.timeZone", "UTC",
            "server.requestLog.appenders", List.of(
                Map.of(
                    "type", "console",
                    "logFormat",
                    "%clientHost %l %user [%date] \"%requestURL\" %statusCode %bytesSent %elapsedTime \"%header{User-Agent}\""),
                Map.of(
                    "type", "file",
                    "threshold", threshold,
                    "archivedFileCount", 5,
                    "archivedLogFilenamePattern", "logs/request-%d.log.gz")))));
    return environment;
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
