/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.util.Map;

public class AuditDTO
{
  public long timestamp;

  public String method;

  public String path;

  public String remoteIpAddress;

  public String forwarded;

  public String userAgent;

  public String sessionId;

  public String username;

  public String logger;

  public String event;

  public int httpStatus;

  public String error;

  public Map<String, Object> data;

  @SuppressWarnings("unused")
  public AuditDTO() {
    // supports deserialization
  }

  public AuditDTO(RecordingAuditData recordingAuditData, String error) {
    timestamp = recordingAuditData.getTimestamp();
    method = recordingAuditData.getRequestData().getMethod();
    path = recordingAuditData.getRequestData().getPath();
    remoteIpAddress = recordingAuditData.getRequestData().getRemoteIpAddress();
    forwarded = recordingAuditData.getRequestData().getForwarded();
    userAgent = recordingAuditData.getRequestData().getUserAgent();
    sessionId = recordingAuditData.getRequestData().getSessionId();
    username = recordingAuditData.getUsername();
    logger = AuditRecorder.toLoggerName(recordingAuditData.getEvent());
    event = recordingAuditData.getEvent().name();
    httpStatus = recordingAuditData.getHttpStatus();
    this.error = error;
    data = recordingAuditData.getData();
  }
}
