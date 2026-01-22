/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.util.Objects;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpServletRequest;

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

  static final ObjectMapper AUDIT_OBJECT_MAPPER = new ObjectMapper();

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
        return AuditErrorType.BAD_REQUEST.getValue();
      case 401:
        if (auditData.getUsername() != null) {
          return AuditErrorType.BAD_AUTHENTICATION.getValue();
        }
        else {
          RequestData requestData = auditData.getRequestData();
          if (requestData != null && requestData.getSessionId() != null) {
            return AuditErrorType.BAD_SESSION.getValue();
          }
        }
        return AuditErrorType.UNAUTHENTICATED.getValue();
      case 402:
        return AuditErrorType.UNLICENSED.getValue();
      case 403:
        return AuditErrorType.UNAUTHORIZED.getValue();
      case 404:
        return AuditErrorType.NOT_FOUND.getValue();
      case 502:
        return AuditErrorType.BAD_GATEWAY.getValue();
      case 503:
        return AuditErrorType.SERVICE_UNAVAILABLE.getValue();
      case 504:
        return AuditErrorType.GATEWAY_TIMEOUT.getValue();
      default:
        // fallthrough
    }
    if (httpStatus >= 500) {
      return AuditErrorType.SERVER_ERROR.getValue();
    }
    if (httpStatus >= 400) {
      return AuditErrorType.CLIENT_ERROR.getValue();
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
  void log(RecordingAuditData auditData, String error) {
    toLogger(auditData.getEvent()).info(toObjectNode(auditData, error).toString());
  }

  ObjectNode toObjectNode(RecordingAuditData recordingAuditData, String error) {
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
