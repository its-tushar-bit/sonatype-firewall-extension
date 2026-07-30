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
import java.util.List;
import java.util.zip.GZIPOutputStream;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationReceipt;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationStatus;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationSubStatus;
import com.sonatype.clm.dto.model.signature.VulnerabilitySignatureAnalysisDTO;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.PolicyEvaluationHelper;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PersistedPolicyEvaluationPollingResultDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.hds.ScanUploader;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.PersistedPolicyEvaluationPollingResult;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.*;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.organization.PolicyEvaluationRequestDTO;
import com.sonatype.insight.brain.policy.evaluator.AbstractPolicyEvaluationTest;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.sbom.export.SbomExportParams.ExportSpecification;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.thirdparty.SbomScanType;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.insight.scan.file.SbomFormat;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.scan.model.ScanFileNames;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.clm.dto.model.component.ComponentIdentifier.createMavenCoordinates;
import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import static com.sonatype.insight.brain.utils.VulnerabilitySignatureAnalysisDTOHelper.createTestAnalysisDTO;
import static com.sonatype.insight.mock.hds.HdsMockServer.RestServlet.SCAN_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationEvaluationResourceTest
    extends AbstractResourceTest
{
  private static final String EVALUATE_PATH = ApplicationEvaluationResource.RESOURCE_PATH + "/" +
      ApplicationEvaluationResource.EVALUATE_PATH;

  private static final String COMPONENT_ANALYSIS_PATH = ApplicationEvaluationResource.RESOURCE_PATH + "/" +
      ApplicationEvaluationResource.COMPONENT_ANALYSIS_PATH;

  private static final String POLICY_EVALUATION_PATH = ApplicationEvaluationResource.RESOURCE_PATH + "/" +
      ApplicationEvaluationResource.POLICY_EVALUATION_PATH;

  private PolicyEvaluationDAO policyEvaluationDAO;

  private PolicyDAO policyDAO;

  private ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private PolicyEvaluationHelper policyEvaluationHelper;

  private OrganizationDAO organizationDAO;

  private RepositoryDAO repositoryDAO;

  @Inject
  private PersistedPolicyEvaluationPollingResultDAO persistedPolicyEvaluationPollingResultDAO;

  @Before
  public void setUp() throws Exception {
    licenseManager.setProducts(
        ProductLicenseDetails.PRODUCT_SBOM_MANAGER,
        ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    installLicense();

    policyEvaluationDAO = lookup(PolicyEvaluationDAO.class);
    policyDAO = lookup(PolicyDAO.class);
    thirdPartySbomMetadataDAO = lookup(ThirdPartySbomMetadataDAO.class);
    policyEvaluationHelper = lookup(PolicyEvaluationHelper.class);
    persistedPolicyEvaluationPollingResultDAO = lookup(PersistedPolicyEvaluationPollingResultDAO.class);
    organizationDAO = lookup(OrganizationDAO.class);
    repositoryDAO = lookup(RepositoryDAO.class);
  }

  private HttpRequest makeRequest(
      IntegrationType integrationType,
      String applicationPublicId,
      String stageId,
      ClientScanType scanType,
      boolean withFile,
      String path) throws IOException, URISyntaxException
  {
    return makeRequest(integrationType, applicationPublicId, stageId, scanType, withFile, path, null);
  }

  private HttpRequest makeRequest(
      IntegrationType integrationType,
      String applicationPublicId,
      String stageId,
      ClientScanType scanType,
      boolean withFile,
      String path,
      String sbomVersion) throws IOException, URISyntaxException
  {
    HttpRequest request = restRequest()
        .path(path)
        .query("scanType", scanType)
        .parameter(applicationPublicId, integrationType, stageId);

    if (sbomVersion != null) {
      request.query("sbomVersion", sbomVersion);
    }

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

  private HttpRequest makeRequest(
      IntegrationType integrationType,
      String applicationPublicId,
      String stageId,
      ClientScanType scanType,
      String path,
      String statusId,
      PolicyEvaluationRequestDTO policyEvaluationRequestDTO)
  {
    return restRequest()
        .path(path)
        .query("scanType", scanType)
        .query("statusId", statusId)
        .parameter(applicationPublicId, integrationType, stageId)
        .body(policyEvaluationRequestDTO);
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

    PolicyEvaluation policyEvaluation = policyEvaluationDAO.getLastByOwnerIdAndScanId(app.getId(), scanId);
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
  public void testEvaluateWithPollingAndPollEvaluationResult_proxyStage() throws Exception {
    licenseManager.setProducts(
        ProductLicenseDetails.PRODUCT_FIREWALL);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
    licenseManager.setFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION, LicensedFeature.COMPONENT_EVALUATION);
    installLicense();
    Repository repository = tempEntity.newRepository("publicId");
    Organization organization = tempEntity.newOrganization("org");
    organization.setRelatedRepositoryId(repository.getId());
    repository.setRelatedOrganizationId(organization.getId());
    repositoryDAO.update(repository);
    organizationDAO.update(organization);
    Application app = tempEntity.newApplication(organization.getId());
    String testClientUserAgent = "testClientUserAgent";

    // For container image evaluation, create policy owned by repository
    Policy policy = tempEntity.newPolicy(repository.getId());
    policy.setAction(ComplianceStageType.ID, Action.ID_FAIL);
    policyDAO.update(policy);

    // Simulate that the report is available
    String scanId = mockReport("/" + getClass().getSimpleName() + "/report");
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(scanId);

    mockScanReceipt(scanReceipt);

    // evaluate policy
    HttpResponse response =
        makeRequest(IntegrationType.CLI, app.getPublicId(), ProxyStageType.ID,
            ClientScanType.SONATYPE_THIRD_PARTY, true, EVALUATE_PATH) //
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

    PolicyEvaluation policyEvaluation = policyEvaluationDAO.getLastByOwnerIdAndScanId(app.getId(), scanId);
    assertThat(policyEvaluation.isReevaluation()).isFalse();
    assertThat(policyEvaluation.isForObsoleteScan()).isFalse();

    assertThat(getHdsServer().getCapturedRequestHttpHeaders(ScanUploader.HDS_PATH)
        .get(HdsClient.CLM_CLIENT_USER_AGENT_HEADER)).isEqualTo(testClientUserAgent);
  }

  @Test
  public void testEvaluateWithPollingAndPollEvaluationResult_ValidateFeature() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    String testClientUserAgent = "testClientUserAgent";
    licenseManager.setFeatures();

    HttpResponse response =
        makeRequest(IntegrationType.CLI, app.getPublicId(), BuildStageType.ID,
            ClientScanType.SONATYPE_THIRD_PARTY, true, EVALUATE_PATH) //
                .header(HdsClient.CLM_CLIENT_USER_AGENT_HEADER, testClientUserAgent) //
                .post();
    assertResponseStatus(402, response);
  }

  @Test
  public void testEvaluateWithPollingForContainerImageEvaluation_NotFeatureFlag() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    String testClientUserAgent = "testClientUserAgent";
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(false);
    licenseManager.setFeatures();

    HttpResponse response =
        makeRequest(IntegrationType.CLI, app.getPublicId(), ProxyStageType.ID,
            ClientScanType.SONATYPE_THIRD_PARTY, true, EVALUATE_PATH) //
                .header(HdsClient.CLM_CLIENT_USER_AGENT_HEADER, testClientUserAgent) //
                .post();
    assertResponseStatus(404, response);
  }

  @Test
  public void testEvaluateWithPollingForContainerImageEvaluation_NotContainerImageEvaluationFeature() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    String testClientUserAgent = "testClientUserAgent";
    licenseManager.setFeatures();

    HttpResponse response =
        makeRequest(IntegrationType.CLI, app.getPublicId(), ProxyStageType.ID,
            ClientScanType.SONATYPE_THIRD_PARTY, true, EVALUATE_PATH) //
                .header(HdsClient.CLM_CLIENT_USER_AGENT_HEADER, testClientUserAgent) //
                .post();
    assertResponseStatus(402, response);
    response.getBodyText();
  }

  @Test
  public void testAnalyzeComponentsWithPollingAndPollEvaluationResult() throws Exception {
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
  public void testAnalyzeComponentsWithPollingAndPollEvaluationResult_UnsupportedStage() throws Exception {
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

  @Test
  public void testEvaluateWithPollingByStatusId_ComponentAnalysisPending() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    String testClientUserAgent = "testClientUserAgent";

    Policy policy = tempEntity.newPolicy(app);
    policy.setAction(BuildStageType.ID, Action.ID_FAIL);
    policyDAO.update(policy);

    PolicyEvaluationPollingResult policyEvaluationPollingResult = new PolicyEvaluationPollingResult();
    policyEvaluationPollingResult.setStatus(PolicyEvaluationStatus.PENDING);
    policyEvaluationPollingResult.setSubStatus(PolicyEvaluationSubStatus.COMPONENT_ANALYSIS_PENDING);

    String statusId = "statusId";
    persistedPolicyEvaluationPollingResultDAO.insert(
        new PersistedPolicyEvaluationPollingResult(
            app.getId(),
            statusId,
            policyEvaluationPollingResult));

    VulnerabilitySignatureAnalysisDTO analysisDTO = getVulnerabilitySignatureAnalysisDTO(app);

    PolicyEvaluationRequestDTO policyEvaluationRequestDTO = new PolicyEvaluationRequestDTO();
    policyEvaluationRequestDTO.setAnalysisDTO(analysisDTO);

    // evaluate policy
    HttpResponse response =
        makeRequest(IntegrationType.CLI, app.getPublicId(), BuildStageType.ID,
            ClientScanType.SONATYPE, POLICY_EVALUATION_PATH, statusId, policyEvaluationRequestDTO) //
                .header(HdsClient.CLM_CLIENT_USER_AGENT_HEADER, testClientUserAgent) //
                .post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo(
        String.format("Component analysis has not completed for public application id: %1$s and status ID: %2$s " +
            "The current status is %3$s and the current sub status is %4$s",
            app.getPublicId(), statusId, PolicyEvaluationStatus.PENDING, policyEvaluationPollingResult.getSubStatus()));
  }

  @Test
  public void testEvaluateWithPollingByStatusId_ComponentAnalysisComplete_Success() throws Exception {
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

    // perform component analysis
    HttpResponse response =
        makeRequest(IntegrationType.CLI, app.getPublicId(), BuildStageType.ID,
            ClientScanType.SONATYPE, false, COMPONENT_ANALYSIS_PATH)
                .header(HdsClient.CLM_CLIENT_USER_AGENT_HEADER, testClientUserAgent)
                .post();
    assertResponseStatus(200, response);

    PolicyEvaluationReceipt componentAnalyzeEvaluationReceipt = response.getBody(PolicyEvaluationReceipt.class);
    assertThat(componentAnalyzeEvaluationReceipt).isNotNull();
    assertThat(componentAnalyzeEvaluationReceipt.getStatusId()).isNotNull();
    policyEvaluationHelper
        .awaitComponentAnalysisCompleted(app.getId(), componentAnalyzeEvaluationReceipt.getStatusId());

    PersistedPolicyEvaluationPollingResult componentAnalyzePollingResult = persistedPolicyEvaluationPollingResultDAO
        .getByApplicationIdAndStatusId(
            app.getId(),
            componentAnalyzeEvaluationReceipt.getStatusId());

    assertThat(componentAnalyzeEvaluationReceipt.getStatusId()).isEqualTo(componentAnalyzePollingResult.getStatusId());

    PolicyEvaluationPollingResult policyEvaluationPollingResult =
        componentAnalyzePollingResult.getPolicyEvaluationPollingResult();
    assertThat(policyEvaluationPollingResult.getSubStatus())
        .isEqualTo(PolicyEvaluationSubStatus.COMPONENT_ANALYSIS_COMPLETE);

    VulnerabilitySignatureAnalysisDTO analysisDTO = createTestAnalysisDTO(
        app.getId(),
        policyEvaluationPollingResult.getScanReceipt().getScanId(),
        createMavenCoordinates("tomcat", "tomcat-util", "5.5.23"),
        "CVE-2012-0022",
        lookup(InsightWork.class));

    PolicyEvaluationRequestDTO policyEvaluationRequestDTO = new PolicyEvaluationRequestDTO();
    policyEvaluationRequestDTO.setAnalysisDTO(analysisDTO);

    String statusId = componentAnalyzeEvaluationReceipt.getStatusId();

    // evaluate policy
    response =
        makeRequest(IntegrationType.CLI, app.getPublicId(), BuildStageType.ID,
            ClientScanType.SONATYPE, POLICY_EVALUATION_PATH,
            statusId, policyEvaluationRequestDTO)
                .header(HdsClient.CLM_CLIENT_USER_AGENT_HEADER, testClientUserAgent)
                .post();

    assertResponseStatus(200, response);

    PolicyEvaluationReceipt receipt = response.getBody(PolicyEvaluationReceipt.class);
    assertThat(receipt).isNotNull();
    assertThat(receipt.getStatusId()).isEqualTo(statusId);

    policyEvaluationPollingResult = policyEvaluationHelper
        .awaitEvaluationFinished(app.getId(), receipt.getStatusId());

    assertThat(policyEvaluationPollingResult.getStatus()).isEqualTo(PolicyEvaluationStatus.COMPLETED);
    assertThat(policyEvaluationPollingResult.getSubStatus())
        .isEqualTo(PolicyEvaluationSubStatus.POLICY_EVALUATION_COMPLETE);
    assertThat(policyEvaluationPollingResult.getReason()).isNull();
    assertThat(policyEvaluationPollingResult.getScanReceipt()).isNotNull();
    assertThat(policyEvaluationPollingResult.getScanReceipt().getScanId()).isEqualTo(scanReceipt.getScanId());

    assertThat(getHdsServer().getCapturedRequestHttpHeaders(ScanUploader.HDS_PATH))
        .containsEntry(HdsClient.CLM_CLIENT_USER_AGENT_HEADER, testClientUserAgent);
  }

  @Test
  public void testEvaluateWithPollingByStatusId_ComponentAnalysisComplete_IncorrectStatusId() throws Exception {
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

    // perform component analysis
    HttpResponse response =
        makeRequest(IntegrationType.CLI, app.getPublicId(), BuildStageType.ID,
            ClientScanType.SONATYPE, false, COMPONENT_ANALYSIS_PATH)
                .header(HdsClient.CLM_CLIENT_USER_AGENT_HEADER, testClientUserAgent)
                .post();
    assertResponseStatus(200, response);

    PolicyEvaluationReceipt componentAnalyzeEvaluationReceipt = response.getBody(PolicyEvaluationReceipt.class);
    assertThat(componentAnalyzeEvaluationReceipt).isNotNull();
    assertThat(componentAnalyzeEvaluationReceipt.getStatusId()).isNotNull();
    policyEvaluationHelper
        .awaitComponentAnalysisCompleted(app.getId(), componentAnalyzeEvaluationReceipt.getStatusId());

    PersistedPolicyEvaluationPollingResult componentAnalyzePollingResult = persistedPolicyEvaluationPollingResultDAO
        .getByApplicationIdAndStatusId(
            app.getId(),
            componentAnalyzeEvaluationReceipt.getStatusId());

    assertThat(componentAnalyzeEvaluationReceipt.getStatusId()).isEqualTo(componentAnalyzePollingResult.getStatusId());

    PolicyEvaluationPollingResult policyEvaluationPollingResult =
        componentAnalyzePollingResult.getPolicyEvaluationPollingResult();
    assertThat(policyEvaluationPollingResult.getSubStatus())
        .isEqualTo(PolicyEvaluationSubStatus.COMPONENT_ANALYSIS_COMPLETE);

    VulnerabilitySignatureAnalysisDTO analysisDTO = createTestAnalysisDTO(
        app.getId(),
        policyEvaluationPollingResult.getScanReceipt().getScanId(),
        createMavenCoordinates("tomcat", "tomcat-util", "5.5.23"),
        "CVE-2012-0022",
        lookup(InsightWork.class));

    PolicyEvaluationRequestDTO policyEvaluationRequestDTO = new PolicyEvaluationRequestDTO();
    policyEvaluationRequestDTO.setAnalysisDTO(analysisDTO);

    String incorrectStatusId = "someIncorrectId";

    // evaluate policy
    response =
        makeRequest(IntegrationType.CLI, app.getPublicId(), BuildStageType.ID,
            ClientScanType.SONATYPE, POLICY_EVALUATION_PATH,
            incorrectStatusId, policyEvaluationRequestDTO)
                .header(HdsClient.CLM_CLIENT_USER_AGENT_HEADER, testClientUserAgent)
                .post();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo(
        String.format("Component Analysis not found for Application ID: %1$s and Status ID: %2$s",
            app.getPublicId(), incorrectStatusId));
  }

  @Test
  public void testEvaluateWithPollingByStatusId_ComponentAnalysisComplete_NullStatusId() throws Exception {
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

    // perform component analysis
    HttpResponse response =
        makeRequest(IntegrationType.CLI, app.getPublicId(), BuildStageType.ID,
            ClientScanType.SONATYPE, false, COMPONENT_ANALYSIS_PATH)
                .header(HdsClient.CLM_CLIENT_USER_AGENT_HEADER, testClientUserAgent)
                .post();
    assertResponseStatus(200, response);

    PolicyEvaluationReceipt componentAnalyzeEvaluationReceipt = response.getBody(PolicyEvaluationReceipt.class);
    assertThat(componentAnalyzeEvaluationReceipt).isNotNull();
    assertThat(componentAnalyzeEvaluationReceipt.getStatusId()).isNotNull();
    policyEvaluationHelper
        .awaitComponentAnalysisCompleted(app.getId(), componentAnalyzeEvaluationReceipt.getStatusId());

    PersistedPolicyEvaluationPollingResult componentAnalyzePollingResult = persistedPolicyEvaluationPollingResultDAO
        .getByApplicationIdAndStatusId(
            app.getId(),
            componentAnalyzeEvaluationReceipt.getStatusId());

    assertThat(componentAnalyzeEvaluationReceipt.getStatusId()).isEqualTo(componentAnalyzePollingResult.getStatusId());

    PolicyEvaluationPollingResult policyEvaluationPollingResult =
        componentAnalyzePollingResult.getPolicyEvaluationPollingResult();
    assertThat(policyEvaluationPollingResult.getSubStatus())
        .isEqualTo(PolicyEvaluationSubStatus.COMPONENT_ANALYSIS_COMPLETE);

    VulnerabilitySignatureAnalysisDTO analysisDTO = createTestAnalysisDTO(
        app.getId(),
        policyEvaluationPollingResult.getScanReceipt().getScanId(),
        createMavenCoordinates("tomcat", "tomcat-util", "5.5.23"),
        "CVE-2012-0022",
        lookup(InsightWork.class));

    PolicyEvaluationRequestDTO policyEvaluationRequestDTO = new PolicyEvaluationRequestDTO();
    policyEvaluationRequestDTO.setAnalysisDTO(analysisDTO);

    // evaluate policy
    response =
        makeRequest(IntegrationType.CLI, app.getPublicId(), BuildStageType.ID,
            ClientScanType.SONATYPE, POLICY_EVALUATION_PATH,
            null, policyEvaluationRequestDTO)
                .header(HdsClient.CLM_CLIENT_USER_AGENT_HEADER, testClientUserAgent)
                .post();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo(
        String.format("Component Analysis not found for Application ID: %1$s and Status ID: %2$s",
            app.getPublicId(), null));
  }

  @Test
  public void testEvaluateWithPollingByStatusId_ComponentAnalysisComplete_WithoutVulnAnalysisDto() throws Exception {
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

    // perform component analysis
    HttpResponse response =
        makeRequest(IntegrationType.CLI, app.getPublicId(), BuildStageType.ID,
            ClientScanType.SONATYPE, false, COMPONENT_ANALYSIS_PATH)
                .header(HdsClient.CLM_CLIENT_USER_AGENT_HEADER, testClientUserAgent)
                .post();
    assertResponseStatus(200, response);

    PolicyEvaluationReceipt componentAnalyzeEvaluationReceipt = response.getBody(PolicyEvaluationReceipt.class);
    assertThat(componentAnalyzeEvaluationReceipt).isNotNull();
    assertThat(componentAnalyzeEvaluationReceipt.getStatusId()).isNotNull();
    policyEvaluationHelper
        .awaitComponentAnalysisCompleted(app.getId(), componentAnalyzeEvaluationReceipt.getStatusId());

    PersistedPolicyEvaluationPollingResult componentAnalyzePollingResult = persistedPolicyEvaluationPollingResultDAO
        .getByApplicationIdAndStatusId(
            app.getId(),
            componentAnalyzeEvaluationReceipt.getStatusId());

    assertThat(componentAnalyzeEvaluationReceipt.getStatusId()).isEqualTo(componentAnalyzePollingResult.getStatusId());

    PolicyEvaluationPollingResult policyEvaluationPollingResult =
        componentAnalyzePollingResult.getPolicyEvaluationPollingResult();
    assertThat(policyEvaluationPollingResult.getSubStatus())
        .isEqualTo(PolicyEvaluationSubStatus.COMPONENT_ANALYSIS_COMPLETE);

    // Set the vulnerability analysis dto to be null. Evaluation should still complete.
    PolicyEvaluationRequestDTO policyEvaluationRequestDTO = new PolicyEvaluationRequestDTO();
    policyEvaluationRequestDTO.setAnalysisDTO(null);

    String statusId = componentAnalyzeEvaluationReceipt.getStatusId();

    // evaluate policy
    response =
        makeRequest(IntegrationType.CLI, app.getPublicId(), BuildStageType.ID,
            ClientScanType.SONATYPE, POLICY_EVALUATION_PATH,
            statusId, policyEvaluationRequestDTO)
                .header(HdsClient.CLM_CLIENT_USER_AGENT_HEADER, testClientUserAgent)
                .post();
    assertResponseStatus(200, response);

    PolicyEvaluationReceipt receipt = response.getBody(PolicyEvaluationReceipt.class);
    assertThat(receipt).isNotNull();
    assertThat(receipt.getStatusId()).isEqualTo(statusId);

    policyEvaluationPollingResult = policyEvaluationHelper
        .awaitEvaluationFinished(app.getId(), receipt.getStatusId());

    assertThat(policyEvaluationPollingResult.getStatus()).isEqualTo(PolicyEvaluationStatus.COMPLETED);
    assertThat(policyEvaluationPollingResult.getSubStatus())
        .isEqualTo(PolicyEvaluationSubStatus.POLICY_EVALUATION_COMPLETE);
    assertThat(policyEvaluationPollingResult.getReason()).isNull();
    assertThat(policyEvaluationPollingResult.getScanReceipt()).isNotNull();
    assertThat(policyEvaluationPollingResult.getScanReceipt().getScanId()).isEqualTo(scanReceipt.getScanId());

    assertThat(getHdsServer().getCapturedRequestHttpHeaders(ScanUploader.HDS_PATH))
        .containsEntry(HdsClient.CLM_CLIENT_USER_AGENT_HEADER, testClientUserAgent);
  }

  @Test
  public void testEvaluateWithPollingByStatusId_UnlicensedStage() throws Exception {
    licenseManager.setStageTypes(StageTypes.BUILD);

    Application app = tempEntity.newApplicationWithParent();
    String testClientUserAgent = "testClientUserAgent";

    Policy policy = tempEntity.newPolicy(app);
    policy.setAction(BuildStageType.ID, Action.ID_FAIL);
    policyDAO.update(policy);

    String unlicensedStage = ReleaseStageType.ID;

    VulnerabilitySignatureAnalysisDTO analysisDTO = getVulnerabilitySignatureAnalysisDTO(app);

    PolicyEvaluationRequestDTO policyEvaluationRequestDTO = new PolicyEvaluationRequestDTO();
    policyEvaluationRequestDTO.setAnalysisDTO(analysisDTO);

    HttpResponse response =
        makeRequest(IntegrationType.CLI, app.getPublicId(), unlicensedStage,
            ClientScanType.SONATYPE, POLICY_EVALUATION_PATH, "statusId", policyEvaluationRequestDTO)
                .header(HdsClient.CLM_CLIENT_USER_AGENT_HEADER, testClientUserAgent)
                .post();
    assertResponseStatus(402, response);
    assertThat(response.getBodyText()).isEqualTo(String.format("Stage '%s' is not supported by your license.",
        unlicensedStage));
  }

  @Test
  public void testEvaluateWithPollingByStatusId_InvalidStage() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    String testClientUserAgent = "testClientUserAgent";

    Policy policy = tempEntity.newPolicy(app);
    policy.setAction(BuildStageType.ID, Action.ID_FAIL);
    policyDAO.update(policy);

    String invalidStage = "invalid-stage";
    VulnerabilitySignatureAnalysisDTO analysisDTO = getVulnerabilitySignatureAnalysisDTO(app);

    PolicyEvaluationRequestDTO policyEvaluationRequestDTO = new PolicyEvaluationRequestDTO();
    policyEvaluationRequestDTO.setAnalysisDTO(analysisDTO);

    HttpResponse response =
        makeRequest(IntegrationType.CLI, app.getPublicId(), invalidStage,
            ClientScanType.SONATYPE, POLICY_EVALUATION_PATH, "statusId", policyEvaluationRequestDTO)
                .header(HdsClient.CLM_CLIENT_USER_AGENT_HEADER, testClientUserAgent)
                .post();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid stage id=" + invalidStage);
  }

  @Test
  public void testEvaluateWithPollingByStatusId_ExceedingLicenseLimit() throws Exception {
    testProductLicense.setMaxSbom(0);

    Application app = tempEntity.newApplicationWithParent();
    String testClientUserAgent = "testClientUserAgent";

    Policy policy = tempEntity.newPolicy(app);
    policy.setAction(BuildStageType.ID, Action.ID_FAIL);
    policyDAO.update(policy);

    VulnerabilitySignatureAnalysisDTO analysisDTO = getVulnerabilitySignatureAnalysisDTO(app);

    PolicyEvaluationRequestDTO policyEvaluationRequestDTO = new PolicyEvaluationRequestDTO();
    policyEvaluationRequestDTO.setAnalysisDTO(analysisDTO);

    HttpResponse response =
        makeRequest(IntegrationType.CLI, app.getPublicId(), ComplianceStageType.ID,
            ClientScanType.SONATYPE, POLICY_EVALUATION_PATH, "statusId", policyEvaluationRequestDTO)
                .header(HdsClient.CLM_CLIENT_USER_AGENT_HEADER, testClientUserAgent)
                .post();

    assertResponseStatus(402, response);
    assertThat(response.getBodyText()).isEqualTo(
        String.format("You have exceeded the licensed limit of %s sboms.", testProductLicense.getMaxSboms()));
  }

  @Test
  public void testEvaluateWithPolling_acceptsSbomVersion_forCliCompliance() throws Exception {
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

    // evaluate policy with sbomVersion
    HttpResponse response =
        makeRequest(IntegrationType.CLI, app.getPublicId(), ComplianceStageType.ID,
            ClientScanType.SONATYPE_THIRD_PARTY, true, EVALUATE_PATH, "1.2.3") //
                .header(HdsClient.CLM_CLIENT_USER_AGENT_HEADER, testClientUserAgent) //
                .post();
    assertResponseStatus(200, response);

    PolicyEvaluationReceipt receipt = response.getBody(PolicyEvaluationReceipt.class);
    assertThat(receipt).isNotNull();
    assertThat(receipt.getStatusId()).isNotNull();
  }

  @Test
  public void testEvaluateWithPolling_rejectsSbomVersion_forCiIntegration() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    HttpResponse response =
        makeRequest(IntegrationType.CI, app.getPublicId(), ComplianceStageType.ID,
            ClientScanType.SONATYPE_THIRD_PARTY, false, EVALUATE_PATH, "1.2.3") //
                .post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains("sbomVersion is only supported for CLI integration");
  }

  @Test
  public void testEvaluateWithPolling_rejectsSbomVersion_forProxyStage() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    HttpResponse response =
        makeRequest(IntegrationType.CLI, app.getPublicId(), ProxyStageType.ID,
            ClientScanType.SONATYPE_THIRD_PARTY, false, EVALUATE_PATH, "1.2.3") //
                .post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains("sbomVersion is not supported for the proxy stage");
  }

  @Test
  public void testEvaluateWithPolling_rejectsInvalidSbomVersion_returns400WithRule() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    HttpResponse response =
        makeRequest(IntegrationType.CLI, app.getPublicId(), ComplianceStageType.ID,
            ClientScanType.SONATYPE_THIRD_PARTY, false, EVALUATE_PATH, "v1<bad>") //
                .post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains("HTML metacharacters");
  }

  @Test
  public void testEvaluateWithPolling_omitsSbomVersion_isAlwaysAccepted() throws Exception {
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

    // evaluate policy without sbomVersion (null)
    HttpResponse response =
        makeRequest(IntegrationType.CLI, app.getPublicId(), BuildStageType.ID,
            ClientScanType.SONATYPE, false, EVALUATE_PATH, null) //
                .header(HdsClient.CLM_CLIENT_USER_AGENT_HEADER, testClientUserAgent) //
                .post();
    assertResponseStatus(200, response);

    PolicyEvaluationReceipt receipt = response.getBody(PolicyEvaluationReceipt.class);
    assertThat(receipt).isNotNull();
    assertThat(receipt.getStatusId()).isNotNull();
  }

  private VulnerabilitySignatureAnalysisDTO getVulnerabilitySignatureAnalysisDTO(Application app) throws Exception {
    return createTestAnalysisDTO(
        app.getId(),
        "scanId",
        createMavenCoordinates("tomcat", "tomcat-util", "5.5.23"),
        "CVE-2012-0022",
        lookup(InsightWork.class));
  }
}
