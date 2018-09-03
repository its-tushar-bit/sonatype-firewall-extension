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

/**
 * Central consumer of audit data, handling its export to log file and database.
 */
@Named
@Singleton
public class AuditRecorder
{
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
        return "bad-request";
      case 401:
        if (auditData.getUsername() != null) {
          return "bad-authentication";
        }
        else {
          RequestData requestData = auditData.getRequestData();
          if (requestData != null && requestData.getSessionId() != null) {
            return "bad-session";
          }
        }
        return "unauthenticated";
      case 402:
        return "unlicensed";
      case 403:
        return "unauthorized";
      case 404:
        return "not-found";
      case 502:
        return "bad-gateway";
      case 503:
        return "service-unavailable";
      case 504:
        return "gateway-timeout";
    }
    if (httpStatus >= 500) {
      return "server-error";
    }
    if (httpStatus >= 400) {
      return "client-error";
    }
    return null;
  }

  private void commitAuditData(RecordingAuditData auditData) {
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

  private void recordAuditData(RecordingAuditData auditData, String error) {
    // only record dependent sub events if the compound event succeeded, avoid noise otherwise
    if (error == null) {
      for (RecordingAuditData child : auditData.getChildren()) {
        recordAuditData(child, error);
      }
    }

    // write to log, add to database, etc.
    AuditEvent event = auditData.getEvent();
    RequestData requestData = auditData.getRequestData();
    System.out.println("AUDIT-DATA: " + auditData.getTimestamp() + ", " + auditData.getUsername() + ", " +
        requestData.getRemoteIpAddress() + ", " + requestData.getMethod() + " " + requestData.getPath() + ", " +
        requestData.getUserAgent() + ", " + event.getDomain() + ":" + event.getType() + ", " + error + ", " +
        JsonUtils.writeUnformatted(auditData.getData()));
  }
}
