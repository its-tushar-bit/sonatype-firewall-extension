/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.IOException;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.inject.Binder;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReportServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ReportService reportService;

  @Mock
  private ReportDataStore reportDataStore;

  @Override
  public void configure(Binder binder) {
    binder.bind(ReportDataStore.class).toInstance(reportDataStore);
    super.configure(binder);
  }

  @Before
  public void before() throws Exception {
    Assertions.setMaxStackTraceElementsDisplayed(22);
  }

  @Test
  public void testGetReportMetadata_Authorized() {
    grantReadPermission(app.getId());
    String scanId = "12345678";
    when(reportDataStore.getFileReport(app.getId(), scanId)).thenReturn(mock(ApplicationReport.class));
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

  @Test(expected = NotFoundException.class)
  public void testUpdateReportEntry_Authorized() throws IOException {
    grantWritePermission(app.getId());
    String scanId = "unrealId";
    when(reportDataStore.getFileReport(app.getId(), scanId)).thenReturn(mock(ApplicationReport.class));
    reportService.updateReportEntry(app.getId(), scanId, ReportDataStore.SECURITY_JSON_FILENAME, null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdateReportEntry_Unauthenticated() throws IOException {
    reportService.updateReportEntry(app.getId(), "unrealId", ReportDataStore.SECURITY_JSON_FILENAME, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdateReportEntry_Unauthorized() throws IOException {
    login();
    reportService.updateReportEntry(app.getId(), "unrealId", ReportDataStore.SECURITY_JSON_FILENAME, null);
  }

  @Test
  public void testProcessBrowseReport_Authorized() {
    grantReadPermission(app.getId());
    when(reportDataStore.toEntryName("path")).thenReturn("dummy-entry-name");
    String scanId = "12345678";
    when(reportDataStore.getFileReport(app.getId(), scanId)).thenReturn(mock(ApplicationReport.class));

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
