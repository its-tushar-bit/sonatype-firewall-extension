/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.IOException;

import jakarta.ws.rs.core.Response;

import com.sonatype.insight.brain.dataaccess.repository.HostedRepositoryComponentDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.organization.ReportMetadataDTO;
import com.sonatype.insight.brain.report.pdf.PdfGeneratorService;
import com.sonatype.insight.brain.security.SecurityAspectControl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HostedRepositoryComponentReportResourceTest
{
  private static final String HRC_ID = "hrc-1";

  private static final String SCAN_ID = "scan-1";

  private static final String PATH = "path/to/entry.json";

  private static final String SBOM_VERSION = "1.0";

  @Mock
  private ReportService reportService;

  @Mock
  private PdfGeneratorService pdfGeneratorService;

  @Mock
  private HostedRepositoryComponentDAO hostedRepositoryComponentDAO;

  @InjectMocks
  private HostedRepositoryComponentReportResource resource;

  private HostedRepositoryComponent hrc;

  @Before
  public void setUp() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);
    SecurityAspectControl.disableEnforcement();
    hrc = new HostedRepositoryComponent("repo-1", "path/lib.jar", "hash-abc");
    hrc.setId(HRC_ID);
    when(hostedRepositoryComponentDAO.getByIdNotNull(HRC_ID)).thenReturn(hrc);
  }

  @After
  public void tearDown() {
    SecurityAspectControl.enableEnforcement();
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(false);
  }

  @Test
  public void browseReport_delegatesToReportServiceWithResolvedHrc() {
    ReportEntry expected = new ReportEntry("entry.json", 0L, new byte[0]);
    when(reportService.processBrowseReport(hrc, SCAN_ID, PATH)).thenReturn(expected);

    ReportEntry actual = resource.browseReport(HRC_ID, SCAN_ID, PATH);

    assertThat(actual).isSameAs(expected);
    verify(hostedRepositoryComponentDAO).getByIdNotNull(HRC_ID);
    verify(reportService).processBrowseReport(hrc, SCAN_ID, PATH);
  }

  @Test
  public void getReportMetadata_delegatesToReportServiceWithResolvedHrc() throws IOException {
    ReportMetadataDTO expected = new ReportMetadataDTO();
    when(reportService.getReportMetadata(hrc, SCAN_ID)).thenReturn(expected);

    ReportMetadataDTO actual = resource.getReportMetadata(HRC_ID, SCAN_ID);

    assertThat(actual).isSameAs(expected);
    verify(hostedRepositoryComponentDAO).getByIdNotNull(HRC_ID);
    verify(reportService).getReportMetadata(hrc, SCAN_ID);
  }

  @Test
  public void printReport_delegatesToPdfGeneratorServiceWithResolvedHrc() throws IOException {
    Response expected = mock(Response.class);
    when(pdfGeneratorService.printReport(hrc, SCAN_ID)).thenReturn(expected);

    Response actual = resource.printReport(HRC_ID, SCAN_ID);

    assertThat(actual).isSameAs(expected);
    verify(hostedRepositoryComponentDAO).getByIdNotNull(HRC_ID);
    verify(pdfGeneratorService).printReport(hrc, SCAN_ID);
  }

  @Test
  public void printSbomReport_delegatesToPdfGeneratorServiceWithResolvedHrc() throws IOException {
    Response expected = mock(Response.class);
    when(pdfGeneratorService.printSbomReport(hrc, SBOM_VERSION)).thenReturn(expected);

    Response actual = resource.printSbomReport(HRC_ID, SBOM_VERSION);

    assertThat(actual).isSameAs(expected);
    verify(hostedRepositoryComponentDAO).getByIdNotNull(HRC_ID);
    verify(pdfGeneratorService).printSbomReport(hrc, SBOM_VERSION);
  }
}
