/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.enterprise.reporting;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.junit.Test;

public class EnterpriseReportingServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  final String clientUserAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 " +
      "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

  @Inject
  private EnterpriseReportingService enterpriseReportingService;

  @Test(expected = UnauthenticatedException.class)
  public void testAcquireEmbedSession_UnAuthenticated() {
    enterpriseReportingService.acquireEmbedSession("rolling-recap", "http://localhost:8080", clientUserAgent);
  }

  @Test(expected = BadRequestException.class)
  public void testAcquireEmbedSession_UnAuthorized() {
    login();
    enterpriseReportingService.acquireEmbedSession(null, "http://localhost:8080", clientUserAgent);
  }
}
