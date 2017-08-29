/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ReportServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ReportService reportService;

  @Test
  public void testGetReportMetadata_Authorized() throws Exception {
    grantReadPermission(app.getId());

    try {
      reportService.getReportMetadata(app.getPublicId(), "12345678");
    }
    catch (NotFoundException e) {
      assertThat(e.getMessage(), is("Could not download the report for scan ID 12345678"));
    }
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetReportMetadata_Unauthenticated() throws Exception {
    reportService.getReportMetadata(app.getPublicId(), "12345678");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetReportMetadata_Unauthorized() throws Exception {
    login();
    reportService.getReportMetadata(app.getPublicId(), "12345678");
  }
}
