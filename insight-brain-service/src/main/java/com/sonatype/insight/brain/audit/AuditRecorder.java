/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.json.store.JsonUtils;

import com.google.common.annotations.VisibleForTesting;

/**
 * Central consumer of audit data, handling its export to log file and database.
 */
@Named
@Singleton
public class AuditRecorder
{
  enum HttpStatusString
  {
    BAD_REQUEST("bad-request"),
    BAD_AUTHENTICATION("bad-authentication"),
    BAD_SESSION("bad-session"),
    UNAUTHENTICATED("unauthenticated"),
    UNLICENSED("unlicensed"),
    UNAUTHORIZED("unauthorized"),
    NOT_FOUND("not-found"),
    BAD_GATEWAY("bad-gateway"),
    SERVICE_UNAVAILABLE("service-unavailable"),
    GATEWAY_TIMEOUT("gateway-timeout"),
    SERVER_ERROR("server-error"),
    CLIENT_ERROR("client-error");

    private String logString;

    HttpStatusString(String logString) {
      this.logString = logString;
    }

    String getLogString() {
      return logString;
    }
  }

  private final ErrorResponseGenerator errorResponseGenerator;

  @Inject
  public AuditRecorder(ErrorResponseGenerator errorResponseGenerator) {
    this.errorResponseGenerator = errorResponseGenerator;
  }

  public AuditSession recordUserEvent(HttpServletRequest httpRequest) {
    AuditData auditData = new RecordingAuditData(this::commitAuditData, RequestData.newInstance(httpRequest));
    return new AuditSession(new ProxyAuditData(auditData));
  }

  public AuditSession recordSystemEvent(AuditEvent event) {
    AuditData auditData = new RecordingAuditData(this::commitAuditData, null);
    auditData.setEvent(event);
    auditData.setUsername(MDCUsernameScope.SYSTEM);
    return new AuditSession(new ProxyAuditData(auditData));
  }

  private String getError(RecordingAuditData auditData) {
    String error = auditData.getError();
    if (error != null) {
      return error;
    }
    int httpStatus = auditData.getHttpStatus();
    if (httpStatus < 400) {
      Throwable exception = auditData.getException();
      if (exception != null) {
        httpStatus = errorResponseGenerator.mapException(exception).getStatusCode();
      }
    }
    return getHttpStatusString(auditData, httpStatus);
  }

  private String getHttpStatusString(final RecordingAuditData auditData, final int httpStatus) {
    switch (httpStatus) {
      case 400:
        return HttpStatusString.BAD_REQUEST.getLogString();
      case 401:
        if (auditData.getUsername() != null) {
          return HttpStatusString.BAD_AUTHENTICATION.getLogString();
        }
        else {
          RequestData requestData = auditData.getRequestData();
          if (requestData != null && requestData.getSessionId() != null) {
            return HttpStatusString.BAD_SESSION.getLogString();
          }
        }
        return HttpStatusString.UNAUTHENTICATED.getLogString();
      case 402:
        return HttpStatusString.UNLICENSED.getLogString();
      case 403:
        return HttpStatusString.UNAUTHORIZED.getLogString();
      case 404:
        return HttpStatusString.NOT_FOUND.getLogString();
      case 502:
        return HttpStatusString.BAD_GATEWAY.getLogString();
      case 503:
        return HttpStatusString.SERVICE_UNAVAILABLE.getLogString();
      case 504:
        return HttpStatusString.GATEWAY_TIMEOUT.getLogString();
    }
    if (httpStatus >= 500) {
      return HttpStatusString.SERVER_ERROR.getLogString();
    }
    if (httpStatus >= 400) {
      return HttpStatusString.CLIENT_ERROR.getLogString();
    }
    return null;
  }

  @VisibleForTesting
  void commitAuditData(RecordingAuditData auditData) {
    if (auditData.getEvent() == null) {
      if (auditData.getHttpStatus() == 401) {
        auditData.setEvent(AuditEvent.AUTHENTICATION_FAILURE);
      }
      else {
        return;
      }
    }
    String error = getError(auditData);
    recordAuditData(auditData, error);
  }

  @VisibleForTesting
  void recordAuditData(RecordingAuditData auditData, String error) {
    // only record dependent sub events if the compound event succeeded, avoid noise otherwise
    if (error == null) {
      for (RecordingAuditData child : auditData.getChildren()) {
        recordAuditData(child, error);
      }
    }

    logData(auditData, error);
  }

  @VisibleForTesting
  void logData(final RecordingAuditData auditData, final String error) {
    AuditEvent event = auditData.getEvent();
    RequestData requestData = auditData.getRequestData();
    System.out.println("AUDIT-DATA: " + auditData.getTimestamp() + ", " + auditData.getUsername() + ", " +
        requestData.getRemoteIpAddress() + ", " + requestData.getMethod() + " " + requestData.getPath() + ", " +
        requestData.getUserAgent() + ", " + event.getDomain() + ":" + event.getType() + ", " + error + ", " +
        JsonUtils.writeUnformatted(auditData.getData()));
  }
}
