/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.policy.componentanalysis;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import com.google.inject.Binder;
import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationReceipt;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationStatus;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationSubStatus;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.PolicyEvaluationHelper;
import com.sonatype.insight.brain.dataaccess.policy.PersistedPolicyEvaluationPollingResultDAO;
import com.sonatype.insight.brain.hds.ScanHandler;
import com.sonatype.insight.brain.integration.IntegrationType;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.InvalidStageException;
import com.sonatype.insight.brain.model.policy.PersistedPolicyEvaluationPollingResult;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.report.MockReportDownloader;
import com.sonatype.insight.brain.report.ReportDownloader;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ScanHelper;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.telemetry.model.TelemetryData;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.sonatype.insight.brain.hds.HdsClient.CLM_CLIENT_USER_AGENT_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;

public class ComponentAnalysisServiceTest
    extends AbstractComponentTest
{
  private static final IntegrationType INTEGRATION_TYPE = IntegrationType.CLI;

  private static final Stage STAGE = new Stage(Stage.ID_BUILD);

  @Inject
  private ComponentAnalysisService componentAnalysisService;

  @Inject
  private PolicyEvaluationHelper policyEvaluationHelper;

  @Inject
  private PersistedPolicyEvaluationPollingResultDAO persistedPolicyEvaluationPollingResultDAO;

  @Inject
  private ProductLicense productLicense;

  @Mock
  private HttpServletRequest httpRequest;

  @Mock
  private ScanHandler scanHandler;

  @Mock
  private StageTypeService stageTypeService;

  private MockReportDownloader mockReportDownloader;

  private Application app;

  @Override
  public void configure(Binder binder) {
    SystemConfigurationPropertyFeature.NEW_SCAN_PROCESS.setEnabled(true);
    binder.bind(HttpServletRequest.class).toInstance(httpRequest);
    binder.bind(ScanHandler.class).toInstance(scanHandler);
    binder.bind(StageTypeService.class).toInstance(stageTypeService);
    mockReportDownloader = new MockReportDownloader();
    binder.bind(ReportDownloader.class).toInstance(mockReportDownloader.getMock());
    super.configure(binder);
  }

  @Before
  public void before() {
    app = tempEntity.newApplicationWithParent();
    lenient().doReturn(StageTypes.getAll())
        .when(stageTypeService)
        .getLicensedStageTypes();
  }

  @Test
  public void testAnalyzeComponentsWithPolling_UnsupportedStage() {
    final Stage unsupportedStage = new Stage(Stage.ID_COMPLIANCE);
    assertThatThrownBy(() -> componentAnalysisService.analyzeComponentsWithPolling(INTEGRATION_TYPE, "appId",
        ClientScanType.SONATYPE, httpRequest, unsupportedStage))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Compliance scans are not supported for component analysis. " +
            "Please use the policy evaluation endpoint.");
  }

  @Test
  public void testAnalyzeComponentsWithPolling_InvalidStage() {
    final String invalidStage = "invalid-stage";
    assertThatThrownBy(() -> componentAnalysisService.analyzeComponentsWithPolling(INTEGRATION_TYPE, "appId",
        ClientScanType.SONATYPE, httpRequest, new Stage(invalidStage)))
        .isInstanceOf(InvalidStageException.class)
        .hasMessage("Invalid stage id=" + invalidStage);
  }

  @Test
  public void testAnalyzeComponentsWithPolling_UnlicensedStage() {
    doReturn(Set.of())
        .when(stageTypeService)
        .getLicensedStageTypes();
    final Stage unlicensedStage = new Stage(Stage.ID_BUILD);
    assertThatThrownBy(() -> componentAnalysisService.analyzeComponentsWithPolling(INTEGRATION_TYPE, "appId",
        ClientScanType.SONATYPE, httpRequest, unlicensedStage))
        .isInstanceOf(InvalidLicenseException.class)
        .hasMessage("Stage '" + unlicensedStage.getStageTypeId() + "' is not supported by your license.");
  }

  @Test
  public void testAnalyzeComponentsWithPolling_NewScanProcessDisabled() {
    SystemConfigurationPropertyFeature.NEW_SCAN_PROCESS.setEnabled(false);
    assertThatThrownBy(() -> componentAnalysisService.analyzeComponentsWithPolling(INTEGRATION_TYPE, "appId",
        ClientScanType.SONATYPE, httpRequest, STAGE))
        .isInstanceOf(UnauthorizedException.class)
        .hasMessage(SystemConfigurationPropertyFeature.NEW_SCAN_PROCESS.getId() + " feature is disabled.");
  }

  @Test
  public void testAnalyzeComponentsWithPolling_DoesNotContainIntegrationTypeLicenseFeature() {
    productLicense.clear();
    assertThatThrownBy(() -> componentAnalysisService.analyzeComponentsWithPolling(INTEGRATION_TYPE, "appId",
        ClientScanType.SONATYPE, httpRequest, STAGE))
        .isInstanceOf(InvalidLicenseException.class)
        .hasMessage("Your IQ Server license does not enable this feature.");
  }

  @Test
  public void testAnalyzeComponentsWithPolling_Success() throws Exception {
    final File file = new File("test-file.xml");
    doReturn(file)
        .when(scanHandler)
        .createTempScanFile(any(HttpServletRequest.class), any(Application.class));
    doReturn("test-client-user-agent")
        .when(httpRequest)
        .getHeader(CLM_CLIENT_USER_AGENT_HEADER);

    final String scanId = simulateReportIsAvailable();
    ScanHelper.createDummyScanFile(lookup(InsightWork.class), app.getId(), scanId);
    final ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(scanId);
    doReturn(scanReceipt)
        .when(scanHandler)
        .handle(any(File.class), any(Application.class), any(ClientScanType.class), any(TelemetryData.class),
            anyString(), anyString(), anyString(), eq(null));

    final PolicyEvaluationReceipt receipt = componentAnalysisService.analyzeComponentsWithPolling(INTEGRATION_TYPE,
        app.getPublicId(), ClientScanType.SONATYPE, httpRequest, STAGE);

    PersistedPolicyEvaluationPollingResult pollingResult = persistedPolicyEvaluationPollingResultDAO
        .getByApplicationIdAndStatusId(app.getId(), receipt.getStatusId());
    PolicyEvaluationPollingResult policyEvaluationPollingResult =
        pollingResult.getPolicyEvaluationPollingResult();
    assertThat(policyEvaluationPollingResult.getSubStatus())
        .isEqualTo(PolicyEvaluationSubStatus.COMPONENT_ANALYSIS_PENDING);

    policyEvaluationHelper.awaitComponentAnalysisCompleted(app.getId(), receipt.getStatusId());

    pollingResult = persistedPolicyEvaluationPollingResultDAO.getByApplicationIdAndStatusId(app.getId(),
        receipt.getStatusId());
    assertThat(receipt.getStatusId()).isEqualTo(pollingResult.getStatusId());
    assertThat(pollingResult.getApplicationId()).isEqualTo(app.getId());
    policyEvaluationPollingResult = pollingResult.getPolicyEvaluationPollingResult();
    assertThat(policyEvaluationPollingResult).isNotNull();
    assertThat(policyEvaluationPollingResult.getStatus()).isEqualTo(PolicyEvaluationStatus.PENDING);
    assertThat(policyEvaluationPollingResult.getSubStatus())
        .isEqualTo(PolicyEvaluationSubStatus.COMPONENT_ANALYSIS_COMPLETE);
  }

  @Test
  public void testAnalyzeComponentsWithPolling_Failure() throws Exception {
    doThrow(IOException.class)
        .when(scanHandler)
        .handle(any(File.class), any(Application.class), any(ClientScanType.class), any(TelemetryData.class),
            anyString(), anyString(), anyString(), eq(null));

    final PolicyEvaluationReceipt receipt = componentAnalysisService.analyzeComponentsWithPolling(INTEGRATION_TYPE,
        app.getPublicId(), ClientScanType.SONATYPE, httpRequest, STAGE);

    PersistedPolicyEvaluationPollingResult pollingResult = persistedPolicyEvaluationPollingResultDAO
        .getByApplicationIdAndStatusId(app.getId(), receipt.getStatusId());
    PolicyEvaluationPollingResult policyEvaluationPollingResult =
        pollingResult.getPolicyEvaluationPollingResult();
    assertThat(policyEvaluationPollingResult.getSubStatus())
        .isEqualTo(PolicyEvaluationSubStatus.COMPONENT_ANALYSIS_PENDING);

    policyEvaluationHelper.awaitComponentAnalysisFailed(app.getId(), receipt.getStatusId());

    pollingResult = persistedPolicyEvaluationPollingResultDAO.getByApplicationIdAndStatusId(app.getId(),
        receipt.getStatusId());
    assertThat(receipt.getStatusId()).isEqualTo(pollingResult.getStatusId());
    assertThat(pollingResult.getApplicationId()).isEqualTo(app.getId());
    policyEvaluationPollingResult = pollingResult.getPolicyEvaluationPollingResult();
    assertThat(policyEvaluationPollingResult).isNotNull();
    assertThat(policyEvaluationPollingResult.getReason()).startsWith("Internal Server Error");
    assertThat(policyEvaluationPollingResult.getStatus()).isEqualTo(PolicyEvaluationStatus.FAILED);
    assertThat(policyEvaluationPollingResult.getSubStatus())
        .isEqualTo(PolicyEvaluationSubStatus.COMPONENT_ANALYSIS_PENDING);
  }

  private String simulateReportIsAvailable() {
    return mockReportDownloader.mockDownloadReport("/" + getClass().getSimpleName() + "/report");
  }
}
