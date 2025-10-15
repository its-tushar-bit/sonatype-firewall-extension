/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enumeration of all possible audit error types used in audit logging.
 * Centralizes error type constants to avoid duplication across the codebase.
 */
public enum AuditErrorType
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

  private final String value;

  AuditErrorType(String value) {
    this.value = value;
  }

  /**
   * @return the string value used in audit logs and APIs
   */
  public String getValue() {
    return value;
  }

  /**
   * @return all error type values as a list of strings
   */
  public static List<String> getAllValues() {
    return Arrays.stream(values())
        .map(AuditErrorType::getValue)
        .collect(Collectors.toList());
  }
}
