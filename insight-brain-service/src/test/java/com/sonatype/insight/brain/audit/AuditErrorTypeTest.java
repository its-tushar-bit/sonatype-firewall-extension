/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.util.List;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;

/**
 * Tests for {@link AuditErrorType} enum functionality.
 */
public class AuditErrorTypeTest
{
  @Test
  public void testGetValue() {
    assertThat(AuditErrorType.BAD_REQUEST.getValue(), is("bad-request"));
    assertThat(AuditErrorType.UNAUTHORIZED.getValue(), is("unauthorized"));
    assertThat(AuditErrorType.NOT_FOUND.getValue(), is("not-found"));
    assertThat(AuditErrorType.SERVER_ERROR.getValue(), is("server-error"));
  }

  @Test
  public void testGetAllValues() {
    List<String> allValues = AuditErrorType.getAllValues();

    assertThat(allValues, notNullValue());
    assertThat(allValues, hasSize(12)); // All 12 enum values

    // Verify it contains expected error type values
    assertThat(allValues, contains(
        "bad-request",
        "bad-authentication",
        "bad-session",
        "unauthenticated",
        "unlicensed",
        "unauthorized",
        "not-found",
        "bad-gateway",
        "service-unavailable",
        "gateway-timeout",
        "server-error",
        "client-error"));
  }
}
