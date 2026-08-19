/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.access.jetty.RequestLogImpl;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Appender;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.LoggerFactory;

public class OpenTelemetryLogbackInstallerTest
{
  @RegisterExtension
  public static OpenTelemetryExtension otelRule = OpenTelemetryExtension.create();

  private LoggerContext loggerContext;

  private RequestLogImpl requestLog;

  @BeforeEach
  public void setUp() {
    loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    requestLog = new RequestLogImpl();
    requestLog.setQuiet(true);
  }

  @Test
  public void installAccessAppender_attachesAppenderToRequestLog() {
    OpenTelemetryLogbackInstaller.installAccessAppender(loggerContext, requestLog);

    assertThat(appendersOn(requestLog)).hasSize(1);
  }

  @Test
  public void installAccessAppender_isIdempotent() {
    OpenTelemetryLogbackInstaller.installAccessAppender(loggerContext, requestLog);
    OpenTelemetryLogbackInstaller.installAccessAppender(loggerContext, requestLog);
    OpenTelemetryLogbackInstaller.installAccessAppender(loggerContext, requestLog);

    // Repeated installs do not double-attach — second/third calls would double-emit every request.
    assertThat(appendersOn(requestLog)).hasSize(1);
  }

  @Test
  public void append_emitsLogRecordWithSemconvAttributes() {
    appendEvent(accessEvent("GET", "/api/v2/applications", 200, "10.0.0.5", "curl/8.0", 1234L, 250L));

    List<LogRecordData> records = otelRule.getLogRecords();
    assertThat(records).hasSize(1);

    LogRecordData record = records.get(0);
    assertThat(record.getBodyValue().asString()).isEqualTo("GET /api/v2/applications");
    assertThat(record.getInstrumentationScopeInfo().getName()).isEqualTo("com.sonatype.insight.brain.access");
    assertThat(record.getAttributes().get(AttributeKey.stringKey("http.request.method"))).isEqualTo("GET");
    assertThat(record.getAttributes().get(AttributeKey.stringKey("url.path"))).isEqualTo("/api/v2/applications");
    assertThat(record.getAttributes().get(AttributeKey.longKey("http.response.status_code"))).isEqualTo(200L);
    assertThat(record.getAttributes().get(AttributeKey.stringKey("client.address"))).isEqualTo("10.0.0.5");
    assertThat(record.getAttributes().get(AttributeKey.stringKey("user_agent.original"))).isEqualTo("curl/8.0");
    assertThat(record.getAttributes().get(AttributeKey.longKey("http.response.body.size"))).isEqualTo(1234L);
  }

  @Test
  public void append_convertsDurationMillisToSeconds() {
    // IAccessEvent.getElapsedTime() returns milliseconds; OTel semconv defines
    // http.server.request.duration in seconds.
    appendEvent(accessEvent("GET", "/x", 200, "1.1.1.1", "ua", 0L, 1500L));

    Double duration = otelRule.getLogRecords()
        .get(0)
        .getAttributes()
        .get(AttributeKey.doubleKey("http.server.request.duration"));
    assertThat(duration).isEqualTo(1.5);
  }

  @Test
  public void append_skipsBodySizeAttributeWhenContentLengthIsNegative() {
    // IAccessEvent.getContentLength() returns -1 for chunked / unknown-length responses;
    // emitting -1 would poison aggregations.
    appendEvent(accessEvent("GET", "/x", 200, "1.1.1.1", "ua", -1L, 10L));

    LogRecordData record = otelRule.getLogRecords().get(0);
    assertThat(record.getAttributes().get(AttributeKey.longKey("http.response.body.size"))).isNull();
  }

  @Test
  public void append_preservesZeroBodySize() {
    // A legitimate 0 (e.g. 302 redirect) should still be reported.
    appendEvent(accessEvent("GET", "/platform/", 302, "1.1.1.1", "ELB-HealthChecker/2.0", 0L, 0L));

    Long size = otelRule.getLogRecords()
        .get(0)
        .getAttributes()
        .get(AttributeKey.longKey("http.response.body.size"));
    assertThat(size).isEqualTo(0L);
  }

  @Test
  public void append_mapsStatusToSeverity_2xx_isInfo() {
    appendEvent(accessEvent("GET", "/x", 200, "1.1.1.1", "ua", 0L, 1L));
    assertThat(otelRule.getLogRecords().get(0).getSeverity()).isEqualTo(Severity.INFO);
  }

  @Test
  public void append_mapsStatusToSeverity_3xx_isInfo() {
    appendEvent(accessEvent("GET", "/x", 302, "1.1.1.1", "ua", 0L, 1L));
    assertThat(otelRule.getLogRecords().get(0).getSeverity()).isEqualTo(Severity.INFO);
  }

  @Test
  public void append_mapsStatusToSeverity_4xx_isWarn() {
    appendEvent(accessEvent("GET", "/x", 401, "1.1.1.1", "ua", 0L, 1L));
    assertThat(otelRule.getLogRecords().get(0).getSeverity()).isEqualTo(Severity.WARN);
  }

  @Test
  public void append_mapsStatusToSeverity_5xx_isError() {
    appendEvent(accessEvent("GET", "/x", 500, "1.1.1.1", "ua", 0L, 1L));
    assertThat(otelRule.getLogRecords().get(0).getSeverity()).isEqualTo(Severity.ERROR);
  }

  @Test
  public void append_swallowsExceptionsFromBrokenEvent() {
    // A misbehaving IAccessEvent must not break the request log path. The bridge logs an internal
    // error via AppenderBase.addError but does not throw.
    IAccessEvent broken = mock(IAccessEvent.class);
    when(broken.getRequestURI()).thenThrow(new RuntimeException("boom"));
    when(broken.getMethod()).thenReturn("GET");

    OpenTelemetryLogbackInstaller.installAccessAppender(loggerContext, requestLog);
    Appender<IAccessEvent> appender = appendersOn(requestLog).get(0);

    // Should not throw.
    appender.doAppend(broken);

    assertThat(otelRule.getLogRecords()).isEmpty();
  }

  private void appendEvent(final IAccessEvent event) {
    OpenTelemetryLogbackInstaller.installAccessAppender(loggerContext, requestLog);
    Appender<IAccessEvent> appender = appendersOn(requestLog).get(0);
    appender.doAppend(event);
  }

  private static IAccessEvent accessEvent(
      final String method,
      final String uri,
      final int status,
      final String remoteAddr,
      final String userAgent,
      final long contentLength,
      final long elapsedMs)
  {
    IAccessEvent event = mock(IAccessEvent.class);
    when(event.getMethod()).thenReturn(method);
    when(event.getRequestURI()).thenReturn(uri);
    when(event.getStatusCode()).thenReturn(status);
    when(event.getRemoteAddr()).thenReturn(remoteAddr);
    when(event.getRequestHeader("User-Agent")).thenReturn(userAgent);
    when(event.getContentLength()).thenReturn(contentLength);
    when(event.getElapsedTime()).thenReturn(elapsedMs);
    when(event.getTimeStamp()).thenReturn(1_700_000_000_000L);
    return event;
  }

  private static List<Appender<IAccessEvent>> appendersOn(final RequestLogImpl requestLog) {
    List<Appender<IAccessEvent>> out = new java.util.ArrayList<>();
    Iterator<Appender<IAccessEvent>> it = requestLog.iteratorForAppenders();
    while (it.hasNext()) {
      out.add(it.next());
    }
    return out;
  }
}
