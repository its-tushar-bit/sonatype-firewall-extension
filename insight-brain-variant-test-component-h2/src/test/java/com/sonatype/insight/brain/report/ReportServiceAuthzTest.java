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

import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.NotFoundException;
import jakarta.inject.Inject;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@ComponentH2Test
public class ReportServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ReportService reportService;

  @Mock
  private ReportDataStore reportDataStore;

  @BeforeEach
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
        .isThrownBy(() -> reportService.getReportMetadata(app, scanId))
        .withMessage("Could not find a report with ID 12345678");
  }

  @Test
  public void testGetReportMetadata_Unauthenticated() throws Exception {
    assertThatExceptionOfType(UnauthenticatedException.class)
        .isThrownBy(() -> reportService.getReportMetadata(app, "12345678"));
  }

  @Test
  public void testGetReportMetadata_Unauthorized() throws Exception {
    login();
    assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(() -> reportService.getReportMetadata(app, "12345678"));
  }

  @Test
  public void testProcessBrowseReport_Authorized() {
    grantReadPermission(app.getId());
    String scanId = "12345678";
    when(
        reportDataStore.getLifecycleReport(argThat(arg -> arg != null && arg.getId().equals(app.getId())),
            eq(scanId))).thenReturn(mock(LifecycleReport.class));

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> reportService.processBrowseReport(app, scanId, "path"))
        .withMessage("Could not find a report with ID 12345678");
  }

  @Test
  public void testProcessBrowseReport_Unauthenticated() {
    assertThatExceptionOfType(UnauthenticatedException.class)
        .isThrownBy(() -> reportService.processBrowseReport(app, "unrealId", "path"));
  }

  @Test
  public void testProcessBrowseReport_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(() -> reportService.processBrowseReport(app, "unrealId", "path"));
  }

  @Test
  public void testGetReportMetadata_Hrc_Unauthenticated() throws Exception {
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    assertThatExceptionOfType(UnauthenticatedException.class)
        .isThrownBy(() -> reportService.getReportMetadata(hrc, "12345678"));
  }

  @Test
  public void testGetReportMetadata_Hrc_Unauthorized() throws Exception {
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    login();
    assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(() -> reportService.getReportMetadata(hrc, "12345678"));
  }

  @Test
  public void testGetReportMetadata_Hrc_Authorized() {
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    grantReadPermission(hrc.getId());
    String scanId = "12345678";
    when(
        reportDataStore.getLifecycleReport(argThat(arg -> arg != null && arg.getId().equals(hrc.getId())),
            eq(scanId))).thenReturn(mock(LifecycleReport.class));
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> reportService.getReportMetadata(hrc, scanId))
        .withMessage("Could not find a report with ID 12345678");
  }

  @Test
  public void testProcessBrowseReport_Hrc_Unauthenticated() {
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    assertThatExceptionOfType(UnauthenticatedException.class)
        .isThrownBy(() -> reportService.processBrowseReport(hrc, "unrealId", "path"));
  }

  @Test
  public void testProcessBrowseReport_Hrc_Unauthorized() {
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    login();
    assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(() -> reportService.processBrowseReport(hrc, "unrealId", "path"));
  }

  @Test
  public void testProcessBrowseReport_Hrc_Authorized() {
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    grantReadPermission(hrc.getId());
    String scanId = "12345678";
    when(
        reportDataStore.getLifecycleReport(argThat(arg -> arg != null && arg.getId().equals(hrc.getId())),
            eq(scanId))).thenReturn(mock(LifecycleReport.class));

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> reportService.processBrowseReport(hrc, scanId, "path"))
        .withMessage("Could not find a report with ID 12345678");
  }
}
