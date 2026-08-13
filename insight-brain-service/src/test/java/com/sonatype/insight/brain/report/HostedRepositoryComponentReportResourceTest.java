/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.IOException;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;

import com.sonatype.insight.brain.dataaccess.repository.HostedRepositoryComponentDAO;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.organization.LatestReportInformation;
import com.sonatype.insight.brain.organization.ReportMetadataDTO;
import com.sonatype.insight.brain.report.pdf.PdfGeneratorService;
import com.sonatype.insight.brain.sbom.policy.SbomPolicyService;
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

/**
 * Unit tests for {@link HostedRepositoryComponentReportResource}. Verifies each read handler
 * resolves the HRC through {@link HostedRepositoryComponentDAO#getByIdNotNull(String)} and
 * delegates to the same Owner-scoped service method as the App path.
 * <p>
 * {@code reevaluatePolicy} is covered in {@code HostedComponentScanQueueConsumerTest}
 * ({@code hostedReevaluateResource_*}) rather than here, against a real database. A Mockito
 * delegation assertion cannot distinguish the two wirings that matter for that handler:
 * {@code getByIdNotNull(hrcId).getId()} and a bare {@code hrcId} produce the same argument, because
 * {@code getByIdNotNull} is a primary-key lookup. Only exercising an unknown id — which must 404
 * from the DAO before any evaluation work starts — pins that the lookup is load-bearing.
 */
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

  @Mock
  private AuditLogReader auditLogReader;

  @Mock
  private SbomPolicyService sbomPolicyService;

  @Mock
  private ApplicationService applicationService;

  @InjectMocks
  private HostedRepositoryComponentReportResource resource;

  private HostedRepositoryComponent hrc;

  @Before
  public void setUp() {
    // AspectJ compile-time weaving inserts a @HasFeature aspect on the resource class and an
    // @Authorize aspect on its service-call sites. Both fire during Mockito unit tests that
    // bypass the Spring proxy. Disabling enforcement short-circuits both to the mocked service
    // call — see SecurityAspectControl's javadoc for the intended use. This also covers
    // @HasFeature(HOSTED_REPOSITORY_EVALUATION), so the feature must not be toggled here:
    // SystemConfigurationPropertyFeature.setEnabled reaches for a statically injected
    // SystemConfigurationPropertyDAO that a plain MockitoJUnitRunner never wires.
    SecurityAspectControl.disableEnforcement();
    hrc = new HostedRepositoryComponent("repo-1", "path/lib.jar", "hash-abc");
    hrc.setId(HRC_ID);
    when(hostedRepositoryComponentDAO.getByIdNotNull(HRC_ID)).thenReturn(hrc);
  }

  @After
  public void tearDown() {
    SecurityAspectControl.enableEnforcement();
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

  @Test
  public void auditLog_delegatesToAuditLogReaderWithResolvedHrc() throws IOException {
    String auditPath = "licenses.json";
    String encodedKey = null;
    Response expected = mock(Response.class);
    when(auditLogReader.readAuditLog(hrc, auditPath, encodedKey)).thenReturn(expected);

    Response actual = resource.auditLog(HRC_ID, auditPath, encodedKey);

    assertThat(actual).isSameAs(expected);
    verify(hostedRepositoryComponentDAO).getByIdNotNull(HRC_ID);
    verify(auditLogReader).readAuditLog(hrc, auditPath, encodedKey);
  }

  @Test
  public void getSbomPolicyViolationReport_missingReportEntry_returns404() throws IOException {
    String sbomVersion = "1.0";
    when(sbomPolicyService.getPolicyViolationsReportEntry(hrc, sbomVersion)).thenReturn(null);
    HttpServletRequest req = mock(HttpServletRequest.class);

    Response actual = resource.getSbomPolicyViolationReport(HRC_ID, sbomVersion, null, null, null, req);

    assertThat(actual.getStatus()).isEqualTo(404);
    verify(hostedRepositoryComponentDAO).getByIdNotNull(HRC_ID);
    verify(sbomPolicyService).getPolicyViolationsReportEntry(hrc, sbomVersion);
  }

  @Test
  public void getSbomPolicyViolationReport_withReportEntry_returnsOk() throws IOException {
    String sbomVersion = "1.0";
    ReportEntry entry = new ReportEntry("policythreats.json", System.currentTimeMillis(), new byte[]{'{', '}'});
    when(sbomPolicyService.getPolicyViolationsReportEntry(hrc, sbomVersion)).thenReturn(entry);
    HttpServletRequest req = mock(HttpServletRequest.class);
    ServletContext servletContext = mock(ServletContext.class);
    when(req.getServletContext()).thenReturn(servletContext);
    when(servletContext.getMimeType(entry.name)).thenReturn("application/json");

    Response actual = resource.getSbomPolicyViolationReport(HRC_ID, sbomVersion, null, null, null, req);

    assertThat(actual.getStatus()).isEqualTo(200);
    verify(hostedRepositoryComponentDAO).getByIdNotNull(HRC_ID);
  }

  @Test
  public void getLatestReportInformation_delegatesToApplicationServiceWithResolvedHrc() {
    String stageTypeId = "build";
    LatestReportInformation expected = new LatestReportInformation("scan-uuid", true);
    when(applicationService.getLatestReportInformation(hrc, stageTypeId)).thenReturn(expected);

    LatestReportInformation actual = resource.getLatestReportInformation(HRC_ID, stageTypeId);

    assertThat(actual).isSameAs(expected);
    verify(hostedRepositoryComponentDAO).getByIdNotNull(HRC_ID);
    verify(applicationService).getLatestReportInformation(hrc, stageTypeId);
  }
}
