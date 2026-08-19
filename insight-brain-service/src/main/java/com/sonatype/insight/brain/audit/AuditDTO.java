/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import com.sonatype.insight.brain.audit.AuditEvent.Domain;
import com.sonatype.insight.brain.security.MDCUsernameScope;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class AuditDTO
{
  public String timestamp;

  public String requestMethod;

  public String requestUri;

  public String remoteIpAddress;

  public String forwarded;

  public String userAgent;

  public String username;

  public String domain;

  public String type;

  public String error;

  public Map<String, Object> data;

  public AuditDTO() {
    // supports deserialization
  }

  public AuditDTO(RecordingAuditData recordingAuditData, String error) {
    timestamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(recordingAuditData.getTimestamp()), ZoneId.systemDefault())
        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    RequestData requestData = recordingAuditData.getRequestData();
    domain = recordingAuditData.getEvent().getDomain();
    if (requestData != null) {
      if (domain.equals(Domain.AUTHENTICATION)) {
        requestMethod = requestData.getMethod();
        requestUri = requestData.getUri();
      }
      remoteIpAddress = requestData.getRemoteIpAddress();
      forwarded = requestData.getForwarded();
      userAgent = requestData.getUserAgent();
    }
    username = recordingAuditData.getUsername();
    if (username == null) {
      username = MDCUsernameScope.ANONYMOUS;
    }
    type = recordingAuditData.getEvent().getType();
    this.error = error;
    if (!recordingAuditData.getData().isEmpty()) {
      data = recordingAuditData.getData();
    }
  }
}
