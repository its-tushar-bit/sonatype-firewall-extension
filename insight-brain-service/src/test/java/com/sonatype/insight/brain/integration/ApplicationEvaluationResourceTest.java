/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationReceipt;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationStatus;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationSubStatus;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.PolicyEvaluationHelper;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.hds.ScanUploader;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.policy.evaluator.AbstractPolicyEvaluationTest;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.sbom.export.SbomExportParams.ExportSpecification;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.thirdparty.SbomScanType;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.insight.scan.file.SbomFormat;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.scan.model.ScanFileNames;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import static com.sonatype.insight.mock.hds.HdsMockServer.RestHandler.SCAN_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationEvaluationResourceTest
    extends AbstractResourceTest
{
  private static final String EVALUATE_PATH = Paths.get(ApplicationEvaluationResource.RESOURCE_PATH,
      ApplicationEvaluationResource.EVALUATE_PATH).toString();

  private static final String COMPONENT_ANALYSIS_PATH = Paths.get(ApplicationEvaluationResource.RESOURCE_PATH,
      ApplicationEvaluationResource.COMPONENT_ANALYSIS_PATH).toString();

  private PolicyEvaluationDAO policyEvaluationDAO;

  private PolicyDAO policyDAO;

  private ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private PolicyEvaluationHelper policyEvaluationHelper;

  @Before
  public void setUp() throws Exception {
    licenseManager.setProducts(
        ProductLicenseDetails.PRODUCT_SBOM_MANAGER,
        ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS
    );
    installLicense();

    policyEvaluationDAO = lookup(PolicyEvaluationDAO.class);
    policyDAO = lookup(PolicyDAO.class);
    thirdPartySbomMetadataDAO = lookup(ThirdPartySbomMetadataDAO.class);
    policyEvaluationHelper = lookup(PolicyEvaluationHelper.class);
  }

  private HttpRequest makeRequest(
      IntegrationType integrationType,
      String applicationPublicId,
      String stageId,
      ClientScanType scanType,
      boolean withFile,
      String path) throws IOException, URISyntaxException
  {
    HttpRequest request = restRequest()
        .path(path)
        .query("scanType", scanType).parameter(applicationPublicId, integrationType, stageId);

    if (withFile) {
      URL resource = getClass().getResource("/ApplicationEvaluationResourceTest/container-scan.xml");
      File mockScanXml = tempDir.newFile(ScanFileNames.SONATYPE_SCAN_FILENAME);
      try (GZIPOutputStream gzipStream = new GZIPOutputStream(new FileOutputStream(mockScanXml))) {
        FileUtils.copyFile(new File(resource.toURI()), gzipStream);
      }

      request.body(mockScanXml);
    }

    return request;
  }

  private HttpRequest pollEvaluationResultRequest(String appId, String statusId) {
    return restRequest()
        .path(ApplicationEvaluationResource.RESOURCE_PATH, ApplicationEvaluationResource.STATUS_PATH)
        .parameter(appId, statusId);
  }

  @Test
  public void testEvaluateWithPollingAndPollEvaluationResult() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    String testClientUserAgent = "testClientUserAgent";

    Policy policy = tempEntity.newPolicy(app);
    policy.setAction(BuildStageType.ID, Action.ID_FAIL);
    policyDAO.update(policy);

    // Simulate that the report is available
    String scanId = mockReport("/" + getClass().getSimpleName() + "/report");
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(scanId);

    mockScanReceipt(scanReceipt);

    // evaluate policy
    HttpResponse response =
        makeRequest(IntegrationType.CLI, app.getPublicId(), BuildStageType.ID,
            ClientScanType.SONATYPE, false, EVALUATE_PATH) //
            .header(HdsClient.CLM_CLIENT_USER_AGENT_HEADER, testClientUserAgent) //
            .post();
    assertResponseStatus(200, response);

    PolicyEvaluationReceipt receipt = response.getBody(PolicyEvaluationReceipt.class);
    assertThat(receipt).isNotNull();
    assertThat(receipt.getStatusId()).isNotNull();

    policyEvaluationHelper.awaitEvaluationCompleted(app.getId(), receipt.getStatusId());

    PolicyEvaluationPollingResult policyEvaluationPollingResult = pollEvaluationResultRequest(app.getPublicId(),
        receipt.getStatusId()).get().getBody(PolicyEvaluationPollingResult.class);

    assertThat(policyEvaluationPollingResult.getStatus()).isEqualTo(PolicyEvaluationStatus.COMPLETED);
    assertThat(policyEvaluationPollingResult.getReason()).isNull();
    assertThat(policyEvaluationPollingResult.getResult()).isNotNull();
    assertThat(policyEvaluationPollingResult.getScanReceipt()).isNotNull();
    assertThat(policyEvaluationPollingResult.getScanReceipt().getScanId()).isEqualTo(scanReceipt.getScanId());

    PolicyEvaluationResult policyEvaluationResult = policyEvaluationPollingResult.getResult();
    assertThat(policyEvaluationResult.getAffectedComponentCount()).isEqualTo(7);
    assertThat(policyEvaluationResult.getCriticalComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getSevereComponentCount()).isEqualTo(7);
    assertThat(policyEvaluationResult.getModerateComponentCount()).isEqualTo(0);
    List<PolicyAlert> policyAlerts = policyEvaluationResult.getAlerts();
    assertThat(policyAlerts).hasSize(36);
    for (PolicyAlert policyAlert : policyAlerts) {
      AbstractPolicyEvaluationTest.assertFactCounts(1, 1, policyAlert);
      assertThat(policyAlert.getActions().get(0).getActionTypeId()).isEqualTo(Action.ID_FAIL);
    }

    PolicyEvaluation policyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndScanId(app.getId(), scanId);
    assertThat(policyEvaluation.isReevaluation()).isFalse();
    assertThat(policyEvaluation.isForObsoleteScan()).isFalse();

    assertThat(getHdsServer().getCapturedRequestHttpHeaders(ScanUploader.HDS_PATH)
        .get(HdsClient.CLM_CLIENT_USER_AGENT_HEADER)).isEqualTo(testClientUserAgent);
  }

  @Test
  public void testEvaluateWithPollingAndPollEvaluationResult_complianceStage() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    String testClientUserAgent = "testClientUserAgent";

    Policy policy = tempEntity.newPolicy(app);
    policy.setAction(ComplianceStageType.ID, Action.ID_FAIL);
    policyDAO.update(policy);

    // Simulate that the report is available
    String scanId = mockReport("/" + getClass().getSimpleName() + "/report");
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(scanId);

    mockScanReceipt(scanReceipt);

    // evaluate policy
    HttpResponse response =
        makeRequest(IntegrationType.CLI, app.getPublicId(), ComplianceStageType.ID,
            ClientScanType.SONATYPE_THIRD_PARTY, true, EVALUATE_PATH) //
            .header(HdsClient.CLM_CLIENT_USER_AGENT_HEADER, testClientUserAgent) //
            .post();
    assertResponseStatus(200, response);

    PolicyEvaluationReceipt receipt = response.getBody(PolicyEvaluationReceipt.class);
    policyEvaluationHelper.awaitEvaluationCompleted(app.getId(), receipt.getStatusId());

    ThirdPartySbomMetadata sbomMetadata = thirdPartySbomMetadataDAO.getByScanId(scanId);
    assertThat(sbomMetadata).isNotNull();
    assertThat(sbomMetadata.getSbomVersion()).isNotEmpty();
    assertThat(sbomMetadata.getFilename()).isNotEmpty();
    assertThat(sbomMetadata.getScanType()).isEqualTo(SbomScanType.BINARY.toString());
    assertThat(sbomMetadata.getSpec()).isEqualTo(SbomSpecification.CYCLONEDX.toString());
    assertThat(sbomMetadata.getSpecFormat()).isEqualTo(SbomFormat.JSON.toString());
    assertThat(sbomMetadata.getSpecVersion()).isEqualTo(ExportSpecification.DEFAULT.getVersion());
    assertThat(sbomMetadata.getStatus()).isEqualTo(ACTIVE);
  }

  @Test
  public void testAnalyzeComponentsWithPollingAndPollEvaluationResult_Success() throws Exception {
    SystemConfigurationPropertyFeature.NEW_SCAN_PROCESS.setEnabled(true);
    Application app = tempEntity.newApplicationWithParent();
    String testClientUserAgent = "testClientUserAgent";

    Policy policy = tempEntity.newPolicy(app);
    policy.setAction(BuildStageType.ID, Action.ID_FAIL);
    policyDAO.update(policy);

    // Simulate that the report is available
    String scanId = mockReport("/" + getClass().getSimpleName() + "/report");
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(scanId);

    mockScanReceipt(scanReceipt);

    HttpResponse response =
        makeRequest(IntegrationType.CLI, app.getPublicId(), BuildStageType.ID,
            ClientScanType.SONATYPE, false, COMPONENT_ANALYSIS_PATH) //
            .header(HdsClient.CLM_CLIENT_USER_AGENT_HEADER, testClientUserAgent) //
            .post();
    assertResponseStatus(200, response);

    PolicyEvaluationReceipt receipt = response.getBody(PolicyEvaluationReceipt.class);
    policyEvaluationHelper.awaitComponentAnalysisCompleted(app.getId(), receipt.getStatusId());
    assertThat(receipt).isNotNull();
    assertThat(receipt.getStatusId()).isNotNull();

    PolicyEvaluationPollingResult policyEvaluationPollingResult = pollEvaluationResultRequest(app.getPublicId(),
        receipt.getStatusId()).get().getBody(PolicyEvaluationPollingResult.class);

    assertThat(policyEvaluationPollingResult.getStatus()).isEqualTo(PolicyEvaluationStatus.PENDING);
    assertThat(policyEvaluationPollingResult.getSubStatus())
        .isEqualTo(PolicyEvaluationSubStatus.COMPONENT_ANALYSIS_COMPLETE);
    assertThat(policyEvaluationPollingResult.getReason()).isNull();
    assertThat(policyEvaluationPollingResult.getScanReceipt()).isNotNull();
    assertThat(policyEvaluationPollingResult.getScanReceipt().getScanId()).isEqualTo(scanReceipt.getScanId());

    assertThat(getHdsServer().getCapturedRequestHttpHeaders(ScanUploader.HDS_PATH))
        .containsEntry(HdsClient.CLM_CLIENT_USER_AGENT_HEADER, testClientUserAgent);
  }

  @Test
  public void testAnalyzeComponentsWithPollingAndPollEvaluationResult_Failure() throws Exception {
    SystemConfigurationPropertyFeature.NEW_SCAN_PROCESS.setEnabled(true);
    Application app = tempEntity.newApplicationWithParent();
    String testClientUserAgent = "testClientUserAgent";

    Policy policy = tempEntity.newPolicy(app);
    policy.setAction(BuildStageType.ID, Action.ID_FAIL);
    policyDAO.update(policy);

    // Skipping simulating that the report is available
    HttpResponse response =
        makeRequest(IntegrationType.CLI, app.getPublicId(), BuildStageType.ID,
            ClientScanType.SONATYPE, false, COMPONENT_ANALYSIS_PATH) //
            .header(HdsClient.CLM_CLIENT_USER_AGENT_HEADER, testClientUserAgent) //
            .post();
    assertResponseStatus(200, response);

    PolicyEvaluationReceipt receipt = response.getBody(PolicyEvaluationReceipt.class);
    policyEvaluationHelper.awaitComponentAnalysisFailed(app.getId(), receipt.getStatusId());

    PolicyEvaluationPollingResult policyEvaluationPollingResult = pollEvaluationResultRequest(app.getPublicId(),
        receipt.getStatusId()).get().getBody(PolicyEvaluationPollingResult.class);

    assertThat(policyEvaluationPollingResult.getStatus()).isEqualTo(PolicyEvaluationStatus.FAILED);
    assertThat(policyEvaluationPollingResult.getSubStatus())
        .isEqualTo(PolicyEvaluationSubStatus.COMPONENT_ANALYSIS_PENDING);
    assertThat(policyEvaluationPollingResult.getReason()).isEqualTo("Could not download the report for scan ID "
        + SCAN_ID);
  }

  @Test
  public void testAnalyzeComponentsWithPollingAndPollEvaluationResult_FeatureFlagDisabled() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    String testClientUserAgent = "testClientUserAgent";

    Policy policy = tempEntity.newPolicy(app);
    policy.setAction(BuildStageType.ID, Action.ID_FAIL);
    policyDAO.update(policy);

    // Simulate that the report is available
    String scanId = mockReport("/" + getClass().getSimpleName() + "/report");
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(scanId);

    mockScanReceipt(scanReceipt);

    HttpResponse response =
        makeRequest(IntegrationType.CLI, app.getPublicId(), BuildStageType.ID,
            ClientScanType.SONATYPE, false, COMPONENT_ANALYSIS_PATH) //
            .header(HdsClient.CLM_CLIENT_USER_AGENT_HEADER, testClientUserAgent) //
            .post();
    assertResponseStatus(403, response);
    assertThat(response.getBodyText()).isEqualTo("new-scan-process feature is disabled.");
  }

  @Test
  public void testAnalyzeComponentsWithPollingAndPollEvaluationResult_UnsupportedStage() throws Exception {
    SystemConfigurationPropertyFeature.NEW_SCAN_PROCESS.setEnabled(true);
    Application app = tempEntity.newApplicationWithParent();
    String testClientUserAgent = "testClientUserAgent";

    Policy policy = tempEntity.newPolicy(app);
    policy.setAction(BuildStageType.ID, Action.ID_FAIL);
    policyDAO.update(policy);

    // Simulate that the report is available
    String scanId = mockReport("/" + getClass().getSimpleName() + "/report");
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(scanId);

    mockScanReceipt(scanReceipt);

    HttpResponse response =
        makeRequest(IntegrationType.CLI, app.getPublicId(), ComplianceStageType.ID,
            ClientScanType.SONATYPE, false, COMPONENT_ANALYSIS_PATH) //
            .header(HdsClient.CLM_CLIENT_USER_AGENT_HEADER, testClientUserAgent) //
            .post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Compliance scans are not supported for component analysis." +
        " Please use the policy evaluation endpoint.");
  }

  @Test
  public void testAnalyzeComponentsWithPollingAndPollEvaluationResult_InvalidStage() throws Exception {
    SystemConfigurationPropertyFeature.NEW_SCAN_PROCESS.setEnabled(true);
    Application app = tempEntity.newApplicationWithParent();
    String testClientUserAgent = "testClientUserAgent";

    Policy policy = tempEntity.newPolicy(app);
    policy.setAction(BuildStageType.ID, Action.ID_FAIL);
    policyDAO.update(policy);

    // Simulate that the report is available
    String scanId = mockReport("/" + getClass().getSimpleName() + "/report");
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(scanId);

    mockScanReceipt(scanReceipt);

    String invalidStage = "invalid-stage";
    HttpResponse response =
        makeRequest(IntegrationType.CLI, app.getPublicId(), invalidStage,
            ClientScanType.SONATYPE, false, COMPONENT_ANALYSIS_PATH) //
            .header(HdsClient.CLM_CLIENT_USER_AGENT_HEADER, testClientUserAgent) //
            .post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid stage id=" + invalidStage);
  }

  @Test
  public void testAnalyzeComponentsWithPollingAndPollEvaluationResult_UnlicensedStage() throws Exception {
    licenseManager.setStageTypes(StageTypes.BUILD);
    SystemConfigurationPropertyFeature.NEW_SCAN_PROCESS.setEnabled(true);
    Application app = tempEntity.newApplicationWithParent();
    String testClientUserAgent = "testClientUserAgent";

    Policy policy = tempEntity.newPolicy(app);
    policy.setAction(BuildStageType.ID, Action.ID_FAIL);
    policyDAO.update(policy);

    // Simulate that the report is available
    String scanId = mockReport("/" + getClass().getSimpleName() + "/report");
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(scanId);

    mockScanReceipt(scanReceipt);

    String unlicensedStage = ReleaseStageType.ID;
    HttpResponse response =
        makeRequest(IntegrationType.CLI, app.getPublicId(), unlicensedStage,
            ClientScanType.SONATYPE, false, COMPONENT_ANALYSIS_PATH) //
            .header(HdsClient.CLM_CLIENT_USER_AGENT_HEADER, testClientUserAgent) //
            .post();
    assertResponseStatus(402, response);
    assertThat(response.getBodyText()).isEqualTo(String.format("Stage '%s' is not supported by your license.",
        unlicensedStage));
  }
}
