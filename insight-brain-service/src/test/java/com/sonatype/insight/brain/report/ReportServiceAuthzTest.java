/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.NotFoundException;
import jakarta.inject.Inject;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class ReportServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ReportService reportService;

  @Mock
  private ReportDataStore reportDataStore;

  @Before
  public void before() throws Exception {
    Assertions.setMaxStackTraceElementsDisplayed(22);
  }

  @Test
  public void testGetReportMetadata_Authorized() {
    grantReadPermission(app.getId());
    String scanId = "12345678";
    when(
        reportDataStore.getLifecycleReport(argThat(arg -> arg != null && arg.getId().equals(app.getId())),
            eq(scanId))).thenReturn(mock(LifecycleReport.class));
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> reportService.getReportMetadata(app.getPublicId(), scanId))
        .withMessage("Could not find a report with ID 12345678");
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
  public void testProcessBrowseReport_Authorized() {
    grantReadPermission(app.getId());
    String scanId = "12345678";
    when(
        reportDataStore.getLifecycleReport(argThat(arg -> arg != null && arg.getId().equals(app.getId())),
            eq(scanId))).thenReturn(mock(LifecycleReport.class));

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> reportService.processBrowseReport(app.getId(), scanId, "path"))
        .withMessage("Could not find a report with ID 12345678");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testProcessBrowseReport_Unauthenticated() {
    reportService.processBrowseReport(app.getId(), "unrealId", "path");
  }

  @Test(expected = UnauthorizedException.class)
  public void testProcessBrowseReport_Unauthorized() {
    login();
    reportService.processBrowseReport(app.getId(), "unrealId", "path");
  }
}
