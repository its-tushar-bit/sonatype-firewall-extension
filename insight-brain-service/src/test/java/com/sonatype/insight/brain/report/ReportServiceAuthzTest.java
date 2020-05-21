/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.inject.Binder;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ReportServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ReportService reportService;

  @Mock
  private ReportDownloader reportDownloaderMock;

  @Override
  public void configure(Binder binder) {
    binder.bind(ReportDownloader.class).toInstance(reportDownloaderMock);
    super.configure(binder);
  }

  @Test
  public void testGetReportMetadata_Authorized() throws Exception {
    grantReadPermission(app.getId());

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      reportService.getReportMetadata(app.getPublicId(), "12345678");
    }).withMessage("Could not find a report with ID 12345678");
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

  @Test
  public void testPrepareExpandedCoverageReport_Authorized() throws Exception {
    grantPermission(app.getId(), Permission.EVALUATE_APPLICATION);

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      reportService.prepareExpandedCoverageReport(app.getPublicId(), "12345678");
    }).withMessage("Could not download the report for scan ID 12345678");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testPrepareExpandedCoverageReport_Unauthenticated() throws Exception {
    reportService.prepareExpandedCoverageReport(app.getPublicId(), "12345678");
  }

  @Test(expected = UnauthorizedException.class)
  public void testPrepareExpandedCoverageReport_Unauthorized() throws Exception {
    login();
    reportService.prepareExpandedCoverageReport(app.getPublicId(), "12345678");
  }
}
