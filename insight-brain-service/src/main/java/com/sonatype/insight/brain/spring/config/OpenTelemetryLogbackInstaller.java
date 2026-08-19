/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.access.jetty.RequestLogImpl;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Severity;
import java.time.Instant;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 * Bridges logback-access {@link IAccessEvent} events to the OpenTelemetry Logs API.
 * <p>
 * The OTel Java agent instruments {@code ch.qos.logback.classic.Logger.callAppenders} via bytecode,
 * which captures every classic event at the originating logger regardless of additivity. No
 * explicit bridge is needed for logback-classic. logback-access, however, is a separate framework
 * (events extend {@link IAccessEvent}, not {@code ILoggingEvent}) and the agent ships no
 * instrumentation for it — so HTTP request log events would never reach OTLP without an explicit
 * appender. {@link #installAccessAppender} attaches that bridge.
 * <p>
 * When the OTel SDK is not configured (e.g. single-tenant on-prem), {@link GlobalOpenTelemetry#get()}
 * returns the no-op SDK and the bridge silently drops events.
 */
final class OpenTelemetryLogbackInstaller
{
  private OpenTelemetryLogbackInstaller() {
  }

  /**
   * Attaches a custom {@link AppenderBase} to a logback-access {@link RequestLogImpl} that emits
   * each {@link IAccessEvent} as an OTel log record. Safe to call repeatedly; idempotent for a
   * given {@code RequestLogImpl}.
   */
  static void installAccessAppender(final LoggerContext context, final RequestLogImpl requestLog) {
    String appenderName = OtelAccessAppender.class.getName();
    Iterator<Appender<IAccessEvent>> existing = requestLog.iteratorForAppenders();
    while (existing.hasNext()) {
      if (appenderName.equals(existing.next().getName())) {
        return;
      }
    }
    OtelAccessAppender appender = new OtelAccessAppender();
    appender.setContext(context);
    appender.setName(appenderName);
    appender.start();
    requestLog.addAppender(appender);
  }

  /**
   * Bridges logback-access events to the OTel Logs API. Each access event becomes an OTel log
   * record whose body is the request line, with structured HTTP attributes attached.
   */
  private static final class OtelAccessAppender
      extends AppenderBase<IAccessEvent>
  {
    private static final String INSTRUMENTATION_SCOPE = "com.sonatype.insight.brain.access";

    private static final AttributeKey<String> HTTP_METHOD = AttributeKey.stringKey("http.request.method");

    private static final AttributeKey<String> URL_PATH = AttributeKey.stringKey("url.path");

    private static final AttributeKey<Long> HTTP_STATUS = AttributeKey.longKey("http.response.status_code");

    private static final AttributeKey<String> CLIENT_ADDRESS = AttributeKey.stringKey("client.address");

    private static final AttributeKey<String> USER_AGENT = AttributeKey.stringKey("user_agent.original");

    private static final AttributeKey<Long> RESPONSE_BODY_SIZE = AttributeKey.longKey("http.response.body.size");

    // OTel semconv defines http.server.request.duration in seconds. IAccessEvent.getElapsedTime()
    // returns milliseconds, so we convert to seconds (double) on emit.
    private static final AttributeKey<Double> REQUEST_DURATION = AttributeKey.doubleKey("http.server.request.duration");

    @Override
    protected void append(final IAccessEvent event) {
      try {
        // Resolve the OTel logger per call, matching the upstream OpenTelemetryAppender pattern.
        // Caching it in an instance field would freeze a no-op if construction races ahead of SDK
        // initialization in tests or unusual startup sequences.
        io.opentelemetry.api.logs.Logger otelLogger =
            GlobalOpenTelemetry.get().getLogsBridge().get(INSTRUMENTATION_SCOPE);
        int status = event.getStatusCode();
        String body = event.getMethod() + " " + event.getRequestURI();
        var builder = otelLogger.logRecordBuilder()
            .setBody(body)
            .setSeverity(severityForStatus(status))
            .setTimestamp(event.getTimeStamp(), TimeUnit.MILLISECONDS)
            .setObservedTimestamp(Instant.now())
            .setAttribute(HTTP_METHOD, event.getMethod())
            .setAttribute(URL_PATH, event.getRequestURI())
            .setAttribute(HTTP_STATUS, (long) status)
            .setAttribute(CLIENT_ADDRESS, event.getRemoteAddr())
            .setAttribute(USER_AGENT, event.getRequestHeader("User-Agent"))
            .setAttribute(REQUEST_DURATION, event.getElapsedTime() / 1000.0);
        // IAccessEvent.getContentLength() returns -1 when the response length is unknown
        // (chunked transfer, connection reset). Emitting -1 would poison sum/avg aggregations.
        long contentLength = event.getContentLength();
        if (contentLength >= 0) {
          builder.setAttribute(RESPONSE_BODY_SIZE, contentLength);
        }
        builder.emit();
      }
      catch (Exception e) {
        // Never let log emission break the request log path.
        addError("Failed to emit OTel log record for access event", e);
      }
    }

    /**
     * Maps HTTP status code to OTel log severity so backends can filter/alert on server errors:
     * 5xx → ERROR, 4xx → WARN, everything else → INFO.
     */
    private static Severity severityForStatus(final int status) {
      if (status >= 500) {
        return Severity.ERROR;
      }
      if (status >= 400) {
        return Severity.WARN;
      }
      return Severity.INFO;
    }
  }
}
