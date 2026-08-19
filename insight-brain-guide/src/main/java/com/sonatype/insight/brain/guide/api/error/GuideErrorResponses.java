/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.error;

import com.sonatype.insight.brain.guide.api.dto.GuideErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.UUID;
import org.slf4j.Logger;

/**
 * Builds the JSON {@link Response} that {@link GuideExceptionMapper} returns for every error
 * thrown from a Guide JAX-RS resource. Centralizes the body shape, status-code handling, error
 * lookup ID generation, and request logging so that every Guide error response carries the
 * same envelope as Guide SaaS's {@code GlobalExceptionHandler}.
 *
 * <h2>Response body</h2>
 *
 * <p>
 * Always serializes a {@link GuideErrorResponse} record:
 *
 * <pre>
 * { "success": false, "message": "..." }
 * </pre>
 *
 * Field names and types are intentionally identical to SaaS so a single client SDK can target
 * either deployment and parse error bodies the same way. The response carries
 * {@code Content-Type: application/json}.
 *
 * <h2>Message text</h2>
 *
 * <ul>
 * <li><b>4xx (client errors)</b>: the exception's raw {@code getMessage()} is forwarded
 * verbatim. Null messages stay null in the body to match SaaS, which has the same
 * behavior — {@code GuideErrorResponseSerializationTest.serializesNullMessage} locks
 * this in.</li>
 * <li><b>5xx (server errors)</b>: a UUID error-lookup ID is generated and embedded in a
 * scrubbed message of the form
 * {@code "Internal Server Error (Error lookup ID: <uuid>)"}. The same UUID is logged at
 * ERROR level alongside the stack trace so support can correlate a customer-reported
 * error to a log line. The exception's raw message is intentionally never exposed to the
 * client to avoid leaking stack-trace details, SQL fragments, or other internals.</li>
 * </ul>
 *
 * <h2>Logging</h2>
 *
 * <p>
 * Every 5xx is logged at ERROR with the lookup UUID and full stack trace. Every 4xx is
 * logged at INFO with HTTP method, request URI, and exception description — closing the
 * audit-trail gap from before this class existed where Guide client errors were never logged.
 * Both log paths route exception descriptions through {@link #describeForLog} so a null
 * {@code getMessage()} (e.g. from a no-arg {@code WebApplicationException}) doesn't render as
 * the literal string {@code "null"} in log analyzers like Splunk or Observe.
 *
 * <h2>Why a static helper</h2>
 *
 * <p>
 * Two callers today — the production {@link GuideExceptionMapper} for
 * {@link GuideApiException} and any future provider that wants the same envelope. The
 * pass-through {@link Logger} parameter lets each caller emit log lines under its own logger
 * name, which keeps log filtering clean if additional callers are added.
 */
final class GuideErrorResponses
{
  private GuideErrorResponses() {
  }

  /**
   * Builds the JAX-RS {@link Response} for an error thrown from a Guide resource.
   *
   * @param status the HTTP status code to return; {@code >= 500} triggers UUID-based scrubbing,
   *          anything else propagates the exception's message verbatim
   * @param exception the exception that triggered this response; never {@code null}
   * @param request the active servlet request, used to enrich 4xx log lines with method + URI;
   *          may be {@code null} if no request context is injected
   * @param log the logger of the calling provider, so log lines emit under that logger's name
   * @return a JAX-RS {@code Response} carrying the JSON {@link GuideErrorResponse} body
   */
  static Response build(int status, Throwable exception, HttpServletRequest request, Logger log) {
    String message;
    if (status >= 500) {
      String errorLookupId = UUID.randomUUID().toString();
      log.error("Request failed [Error ID: {}]: {}", errorLookupId, describeForLog(exception), exception);
      message = reasonPhrase(status) + " (Error lookup ID: " + errorLookupId + ")";
    }
    else {
      message = exception.getMessage();
      logClientError(request, status, exception, log);
    }

    Response.ResponseBuilder builder = Response.status(status)
        .type(MediaType.APPLICATION_JSON_TYPE)
        .entity(new GuideErrorResponse(false, message));
    if (exception instanceof GuideLicenseUnavailableException) {
      builder.header(GuideLicenseUnavailableException.LICENSE_HEADER,
          GuideLicenseUnavailableException.LICENSE_UNAVAILABLE);
    }
    return builder.build();
  }

  private static String reasonPhrase(int status) {
    Response.Status s = Response.Status.fromStatusCode(status);
    return s != null ? s.getReasonPhrase() : "Server Error";
  }

  private static void logClientError(HttpServletRequest request, int status, Throwable exception, Logger log) {
    String description = describeForLog(exception);
    if (request != null) {
      log.info("Client error [{}] {} {}: {}", status, request.getMethod(), request.getRequestURI(), description);
    }
    else {
      log.info("Client error [{}]: {}", status, description);
    }
  }

  /**
   * Returns a non-null, non-blank description of an exception for log lines. Falls back to the
   * class's simple name when {@code getMessage()} returns null or blank — otherwise SLF4J would
   * emit the literal string {@code "null"} for the {@code {}} placeholder, hurting log-analysis
   * correlation in tools like Splunk and Observe. This is logging-only; the client-visible
   * response body intentionally preserves null messages to match Guide SaaS exactly.
   */
  private static String describeForLog(Throwable exception) {
    String message = exception.getMessage();
    return message != null && !message.isBlank() ? message : exception.getClass().getSimpleName();
  }
}
