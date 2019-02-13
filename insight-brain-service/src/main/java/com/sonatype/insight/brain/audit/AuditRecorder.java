/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central consumer of audit data, handling its export to log file and database.
 */
@Named
@Singleton
public class AuditRecorder
{
  public static final String BASE_LOGGER_NAME = "com.sonatype.insight.audit";

  private static final String LOGGER_NAME_PREFIX = BASE_LOGGER_NAME + ".";

  private static final ObjectMapper AUDIT_OBJECT_MAPPER = new ObjectMapper();

  static final String BAD_REQUEST = "bad-request";

  static final String BAD_AUTHENTICATION = "bad-authentication";

  static final String BAD_SESSION = "bad-session";

  static final String UNAUTHENTICATED = "unauthenticated";

  static final String UNLICENSED = "unlicensed";

  static final String UNAUTHORIZED = "unauthorized";

  static final String NOT_FOUND = "not-found";

  static final String BAD_GATEWAY = "bad-gateway";

  static final String SERVICE_UNAVAILABLE = "service-unavailable";

  static final String GATEWAY_TIMEOUT = "gateway-timeout";

  static final String SERVER_ERROR = "server-error";

  static final String CLIENT_ERROR = "client-error";

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
    auditData.setEvent(Objects.requireNonNull(event));
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
        return BAD_REQUEST;
      case 401:
        if (auditData.getUsername() != null) {
          return BAD_AUTHENTICATION;
        }
        else {
          RequestData requestData = auditData.getRequestData();
          if (requestData != null && requestData.getSessionId() != null) {
            return BAD_SESSION;
          }
        }
        return UNAUTHENTICATED;
      case 402:
        return UNLICENSED;
      case 403:
        return UNAUTHORIZED;
      case 404:
        return NOT_FOUND;
      case 502:
        return BAD_GATEWAY;
      case 503:
        return SERVICE_UNAVAILABLE;
      case 504:
        return GATEWAY_TIMEOUT;
      default:
        // fallthrough
    }
    if (httpStatus >= 500) {
      return SERVER_ERROR;
    }
    if (httpStatus >= 400) {
      return CLIENT_ERROR;
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

    log(auditData, error);
  }

  @VisibleForTesting
  static void log(RecordingAuditData auditData, String error) {
    toLogger(auditData.getEvent()).info(toObjectNode(auditData, error).toString());
  }

  @VisibleForTesting
  static ObjectNode toObjectNode(RecordingAuditData recordingAuditData, String error) {
    return (ObjectNode) AUDIT_OBJECT_MAPPER.valueToTree(new AuditDTO(recordingAuditData, error));
  }

  @VisibleForTesting
  static Logger toLogger(AuditEvent auditEvent) {
    return LoggerFactory.getLogger(toLoggerName(auditEvent.getDomain()));
  }

  public static String toLoggerName(String domain) {
    return LOGGER_NAME_PREFIX + domain;
  }
}
