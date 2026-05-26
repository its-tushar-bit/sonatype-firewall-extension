/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Vendored/copied from hosted-data-services/insight-jaxrs-utils
package com.sonatype.insight.jaxrs.error;

import com.sonatype.insight.error.HttpStatusCode;
import jakarta.inject.Named;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named("baseErrorResponseGenerator")
public class ErrorResponseGenerator
{
  private static final Logger log = LoggerFactory.getLogger(ErrorResponseGenerator.class);

  public ErrorResponse mapException(Throwable e) {
    e = unwrap(e);

    ErrorResponse errorResponse = buildErrorResponse(e);
    completeErrorResponse(errorResponse, e);

    if (errorResponse.getStatusCode() >= Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
      String incidentId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
      errorResponse.setIncidentId(incidentId);
      errorResponse.setMessageBody(errorResponse.getMessageBody() + " (ID " + incidentId + ")");
    }

    errorResponse.setMessageBody(errorResponse.getMessageBody().replace("\r\n", "\n"));

    log.trace("ErrorResponse: [{}] {}", errorResponse.getStatusCode(), errorResponse.getMessageBody());

    return errorResponse;
  }

  public ErrorResponse mapExceptionAndLog(Throwable e) {
    ErrorResponse errorResponse = mapException(e);
    logErrorResponse(errorResponse, e);
    return errorResponse;
  }

  protected ErrorResponse buildErrorResponse(Throwable e) {
    int statusCode = getStatusCode(e);
    return new ErrorResponse(statusCode, null);
  }

  private int getStatusCode(Throwable e) {
    int statusCode = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();

    if (e instanceof WebApplicationException) {
      Response resp = ((WebApplicationException) e).getResponse();
      statusCode = resp.getStatus();
    }
    else {
      HttpStatusCode hsc = e.getClass().getAnnotation(HttpStatusCode.class);
      if (hsc != null) {
        statusCode = hsc.value();
      }
    }

    return statusCode;
  }

  private void completeErrorResponse(ErrorResponse errorResponse, Throwable e) {
    if (errorResponse.getStatusCode() <= 0) {
      errorResponse.setStatusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    String messageBody = errorResponse.getMessageBody();
    if (messageBody == null || messageBody.isEmpty()) {
      int statusCode = errorResponse.getStatusCode();
      if (statusCode == Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
        // As per INSIGHT-319, don't reveal any specifics but use a generic message
        messageBody = Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase();
      }
      else if (statusCode == Response.Status.NOT_FOUND.getStatusCode() && e instanceof WebApplicationException) {
        // Jersey produces crappy messages for that one
        messageBody = "Resource not found, please check your request URL.";
      }
      else if (statusCode == Response.Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode()
          && e instanceof WebApplicationException)
      {
        // Jersey produces crappy messages for that one
        messageBody = "Unsupported media type"
            + ", please check your request URL, the supplied data and its content type.";
      }
      else {
        messageBody = e.getMessage();
        if (messageBody == null || messageBody.isEmpty()) {
          Response.Status status = Response.Status.fromStatusCode(statusCode);
          if (status != null) {
            messageBody = status.getReasonPhrase();
          }
          else {
            messageBody = "Error: " + statusCode;
          }
        }
      }
      errorResponse.setMessageBody(messageBody);
    }
  }

  public void logErrorResponse(ErrorResponse errorResponse, Throwable e) {
    String message = e.getMessage();
    if (errorResponse.getIncidentId() != null) {
      message += " (ID " + errorResponse.getIncidentId() + ")";
    }
    if (errorResponse.getStatusCode() >= 500) {
      log.error("{} {}", errorResponse.getStatusCode(), message, e);
    }
    else {
      // don't pollute logs with request problems unless trace is requested
      log.debug("{} {}", errorResponse.getStatusCode(), message, log.isTraceEnabled() ? e : null);
    }
  }

  /**
   * @return The first throwable, or child cause, annotated with {@link HttpStatusCode} or the original throwable if
   *         none are annotated.
   */
  Throwable unwrap(Throwable e) {
    Throwable unwrapped = unwrapCause(e);
    if (unwrapped != null) {
      return unwrapped;
    }

    return e;
  }

  private Throwable unwrapCause(Throwable e) {
    if (e == null) {
      return e;
    }

    if (e.getClass().isAnnotationPresent(HttpStatusCode.class)) {
      return e;
    }

    return unwrapCause(e.getCause());
  }
}
