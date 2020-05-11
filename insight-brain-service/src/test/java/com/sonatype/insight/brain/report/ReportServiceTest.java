/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.organization.ReportMetadataDTO;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.thirdparty.ThirdPartyApplicationReportDTO;
import com.sonatype.insight.brain.thirdparty.ThirdPartyBillOfMaterialsRowDTO;
import com.sonatype.insight.brain.thirdparty.ThirdPartyDataService;
import com.sonatype.insight.brain.thirdparty.ThirdPartyHealthCheckReportSecurityRowDTO;
import com.sonatype.insight.brain.thirdparty.ThirdPartyLicenseRowDTO;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.error.exception.NotFoundException;

import de.schlichtherle.truezip.file.TFile;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ReportServiceTest
    extends AbstractComponentTest
{
  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  @Inject
  private InsightWork insightWork;

  private Application app;

  private String scanId = "ReportServiceTestScanId";

  @Inject
  private InsightConfig insightConfig;

  @Inject
  private ThirdPartyDataService thirdPartyDataService;

  // No default constructor, can't use @Spy
  private ThirdPartyDataService thirdPartyDataServiceSpy;

  /**
   * To be configured/mocked by each test.
   */
  private ReportDownloader reportDownloader;

  @Before
  public void before() throws Exception {
    thirdPartyDataServiceSpy = spy(thirdPartyDataService);
    app = tempEntity.newApplicationWithParent();
  }

  private ReportService createReportService() {
    return new ReportService(insightWork, reportDownloader, new PolicyEvaluationDAO(), insightConfig,
        new ApplicationDAO(), thirdPartyDataServiceSpy);
  }

  @Test
  public void testFetchReport_Exists() throws Exception {
    createReportFile();

    ReportService reportService = createReportService();
    File report = reportService.fetchReport(app, scanId);
    assertThat(report).isNotNull();
    assertThat(report).isFile();
    assertThat(report.getName()).isEqualTo("report.zip");
    verify(thirdPartyDataServiceSpy, never()).deleteByScanId(eq(scanId));
  }

  @Test
  public void testFetchReport_DoesNotExist() throws Exception {
    MockReportDownloader mockReportDownloader = new MockReportDownloader();
    mockReportDownloader.mockDownloadReport(scanId, "/ReportServiceTest/report");
    reportDownloader = mockReportDownloader.getMock();

    ReportService reportService = createReportService();
    when(thirdPartyDataServiceSpy.getScanData(scanId)).thenReturn(new ThirdPartyApplicationReportDTO());

    File report = reportService.fetchReport(app, scanId);
    assertThat(report).isNotNull();
    assertThat(report).isFile();
    assertThat(report.getName()).isEqualTo("report.zip");
    verify(reportDownloader).downloadReport(eq(scanId), any(File.class), eq(2100), eq(5));
    verify(thirdPartyDataServiceSpy).deleteByScanId(eq(scanId));
  }

  @Test
  public void testGetReport_Exists() throws Exception {
    createReportFile();
    ReportService reportService = createReportService();
    File report = reportService.getReport(app.getId(), scanId);
    assertThat(report).isNotNull();
    assertThat(report).isFile();
    assertThat(report.getName()).isEqualTo("report.zip");
  }

  @Test
  public void testGetReport_DoesNotExist() throws Exception {
    ReportService reportService = createReportService();
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      reportService.getReport(app.getId(), scanId);
    }).withMessage("Could not find a report with ID ReportServiceTestScanId");
  }

  @Test
  public void testGetReport_DoesNotExistAndEvaluationExist() throws Exception {
    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.BUILD.getId(), scanId);
    ReportService reportService = createReportService();
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      reportService.getReport(app.getId(), scanId);
    }).withMessageContaining("The report for application ID " + app.getId() + " and scan ID " + scanId
        + " does not exist. Usually this means the report was deemed obsolete according "
        + "to the data retention policies and hence purged to the trash.");
  }

  @Test
  public void testGetReportMetadata() throws Exception {
    final String scanId1 = "ScanId1";
    final String scanId2 = "ScanId2";

    // ReportResource.getReport requires a report.zip to exist when evaluations exist
    createReportFile(app.getId(), scanId1, zipReportDir("/ReportResourceTest/report-expanded_coverage_false"));
    // use an older data.json to make sure they still work
    createReportFile(app.getId(), scanId2, zipReportDir("/ReportResourceTest/report"));

    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId1);
    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, scanId2);

    ReportService reportService = createReportService();

    // Verify Response for scan 1
    ReportMetadataDTO metadata = reportService.getReportMetadata(app.getPublicId(), scanId1);
    assertThat(metadata.getApplication().getId()).isEqualTo(app.getId());
    assertThat(metadata.getReportTitle()).isEqualTo("Build Report");
    assertThat(metadata.getReportTime()).isEqualTo(eval1.getTime());
    assertThat(metadata.getStageId()).isEqualTo("build");

    // Verify Response for scan 2
    metadata = reportService.getReportMetadata(app.getPublicId(), scanId2);
    assertThat(metadata.getApplication().getId()).isEqualTo(app.getId());
    assertThat(metadata.getReportTitle()).isEqualTo("Release Report");
    assertThat(metadata.getReportTime()).isEqualTo(eval2.getTime());
    assertThat(metadata.getStageId()).isEqualTo("release");

    // Unknown scan id
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      reportService.getReportMetadata(app.getPublicId(), "12345678");
    }).withMessage("Could not find a report with ID 12345678");
  }

  @Test
  public void testGetReportMetadata_expandedCoverage() throws Exception {
    createReportFile(app.getId(), scanId, zipReportDir("/ReportResourceTest/report-expanded_coverage"));
    ReportService reportService = createReportService();

    // Verify Response for scan
    ReportMetadataDTO metadata = reportService.getReportMetadata(app.getPublicId(), scanId);
    assertThat(metadata).isNotNull();
    assertThat(metadata.getApplication().getId()).isEqualTo(app.getId());
    assertThat(metadata.getReportTitle()).isEqualTo("Expanded Coverage Report");
    assertThat(metadata.getReportTime().getTime()).isEqualTo(1503511338632L);
  }

  @Test
  public void testGetReportMetadata_ScanLabelForNVS() throws Exception {
    createReportFile(app.getId(), scanId, zipReportDir("/" + getClass().getSimpleName() + "/report-scan_label"));
    ReportService reportService = createReportService();
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId);

    ReportMetadataDTO metadata = reportService.getReportMetadata(app.getPublicId(), scanId);
    assertThat(metadata).isNotNull();
    assertThat(metadata.getApplication().getName()).isEqualTo("My Awesome Artifact");
    assertThat(metadata.getReportTitle()).isEqualTo("Report");
  }

  @Test
  public void testPrepareExpandedCoverageReport() throws Exception {
    HdsClient hdsClient = mock(HdsClient.class);
    Map<String, String> queryParams = null;
    // The tested method is supposed to wait for the report to become available on the HDS.
    // We verify that it waits by returning NotFound on the first attempt to download the report for HDS.
    // Only the second attempt is successful. If the tested method does not wait, then it fails on the first attempt.
    when(hdsClient.get(eq(InputStream.class), eq(ReportDownloader.HDS_PATH), eq(queryParams), eq(scanId)))
        .thenThrow(new NotFoundException("test exception"))
        .thenReturn(new FileInputStream(zipReportDir("/ReportResourceTest/report-expanded_coverage")));
    reportDownloader = new ReportDownloader(hdsClient, new FileCleaner());
    ReportService reportService = createReportService();

    File reportFile = insightWork.getReportFile(app.getId(), scanId);
    assertThat(reportFile).doesNotExist();

    reportService.prepareExpandedCoverageReport(app.getPublicId(), scanId);

    assertThat(reportFile).isFile();
  }

  @Test
  public void testIncludeThirdPartyData() throws Exception {
    final File reportZip = zipReportDir("/ReportServiceTest/report");

    ThirdPartyApplicationReportDTO dto = new ThirdPartyApplicationReportDTO();
    final ComponentIdentifier coord = ComponentIdentifier.createRpmCoordinates("n1", "v1", "a1");
    dto.billOfMaterials.add(new ThirdPartyBillOfMaterialsRowDTO(coord, "hash1"));
    dto.securityRows.add(new ThirdPartyHealthCheckReportSecurityRowDTO(coord, "hash1"));
    dto.licenseRows.add(new ThirdPartyLicenseRowDTO(coord, "hash1"));

    createReportService().includeThirdPartyData(reportZip, dto);

    assertThatReportZipContains(reportZip, "thirdparty-bom.json");
    assertThatReportZipContains(reportZip, "thirdparty-security.json");
    assertThatReportZipContains(reportZip, "thirdparty-license.json");
  }

  @Test
  public void testGetBomForPolicyEvaluation() throws URISyntaxException, IOException {
    createReportFile(app.getId(), "SCAN_ID", zipReportDir("/ReportServiceTest/report"));
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "SCAN_ID");

    ReportEntry reportEntry = createReportService().getBomForPolicyEvaluation(policyEvaluation);

    assertThat(reportEntry).isNotNull();
    assertThat(reportEntry.buf).isNotNull();
    assertThat(reportEntry.buf).isNotEmpty();
  }

  @Test
  public void testGetBomForPolicyEvaluation_NoBomFile() throws URISyntaxException, IOException {
    createReportFile(app.getId(), "SCAN_ID", zipReportDir("/ReportServiceTest/report-missing-bom-json"));
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "SCAN_ID");

    ReportEntry reportEntry = createReportService().getBomForPolicyEvaluation(policyEvaluation);

    assertThat(reportEntry).isNull();
  }

  @Test
  public void testGetBomForPolicyEvaluation_NullPolicyEvaluation() throws IOException {
    ReportEntry reportEntry = createReportService().getBomForPolicyEvaluation(null);

    assertThat(reportEntry).isNull();
  }

  @Test
  public void testGetBomForPolicyEvaluation_NoPolicyEvaluation() {
    PolicyEvaluation policyEvaluation = new PolicyEvaluation(app.getId(), BuildStageType.ID, "SCAN_ID");

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> createReportService().getBomForPolicyEvaluation(policyEvaluation));
  }

  private void assertThatReportZipContains(File zipFile, final String thirdPartyFile) {
    assertThat(Stream.of(new TFile(zipFile).listFiles()).anyMatch(f -> f.getName().endsWith(thirdPartyFile)))
        .isTrue();
  }

  private void createReportFile() throws IOException, URISyntaxException {
    createReportFile(app.getId(), scanId, zipReportDir("/ReportServiceTest/report"));
  }

  private void createReportFile(String appId, String scanId, File reportFile) throws IOException {
    FileUtils.copyFile(reportFile, insightWork.getReportFile(appId, scanId));
  }

  private File zipReportDir(String reportResourceName) throws URISyntaxException {
    return Paths.get(ReportHelper.zipReport(reportResourceName, tempDir).toURI()).toFile();
  }
}
