/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PersistedPolicyEvaluationPollingResultDAO;
import com.sonatype.insight.brain.hds.ScanHandler;
import com.sonatype.insight.brain.integration.IntegrationType;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.PersistedPolicyEvaluationPollingResult;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.report.MockReportDownloader;
import com.sonatype.insight.brain.report.ReportDataStore;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ScanHelper;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.scan.model.ClientScanType;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@ComponentH2Test
public class PolicyEvaluateServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private PolicyEvaluateService policyEvaluateService;

  @Inject
  private InsightWork insightWork;

  @Mock
  private ScanHandler mockScanHandler;

  private MockReportDownloader mockReportDownloader;

  private PersistedPolicyEvaluationPollingResultDAO persistedPolicyEvaluationPollingResultDAO;

  private OrganizationDAO organizationDAO;

  @BeforeEach
  public void setup() {
    mockReportDownloader = new MockReportDownloader(tempDir);
    mockReportDownloader.setInsightWork(insightWork);
    applyBeanFieldOverride(ReportDataStore.class, "reportDownloader", mockReportDownloader.getMock());
    applyBeanFieldOverride(PolicyEvaluateService.class, "scanHandler", mockScanHandler);
    persistedPolicyEvaluationPollingResultDAO = daoFactory.createPersistedPolicyEvaluationPollingResultDAO();
    organizationDAO = daoFactory.createOrganizationDAO();
  }

  @Test
  public void testEvaluate_Unauthenticated() throws Exception {
    assertThrows(UnauthenticatedException.class, () -> policyEvaluateService.evaluate(app.getPublicId(), "scanId",
        new Stage(BuildStageType.ID), ScanTriggerType.CLI));
  }

  @Test
  public void testEvaluate_Authorized() throws Exception {
    grantPermission(app.getId(), Permission.EVALUATE_APPLICATION);

    String scanId = mockReportDownloader.mockDownloadReport("/PolicyEvaluateServiceTest/report");
    ScanHelper.createDummyScanFile(lookup(InsightWork.class), app.getId(), scanId);

    policyEvaluateService.evaluate(app.getPublicId(), scanId, new Stage(BuildStageType.ID),
        ScanTriggerType.CLI);
  }

  @Test
  public void testEvaluate_Unauthorized() throws Exception {
    login();
    assertThrows(UnauthorizedException.class, () -> policyEvaluateService.evaluate(app.getPublicId(), "scanId",
        new Stage(BuildStageType.ID), ScanTriggerType.CLI));
  }

  @Test
  public void testEvaluateWithPolling_Unauthenticated() throws Exception {
    assertThrows(UnauthenticatedException.class,
        () -> policyEvaluateService.evaluateWithPolling(IntegrationType.CLI, app.getPublicId(),
            ClientScanType.SONATYPE, null, new Stage(BuildStageType.ID)));
  }

  @Test
  public void testEvaluateWithPolling_Authorized() throws Exception {
    grantPermission(app.getId(), Permission.EVALUATE_APPLICATION);
    stubSuccessfulPollingScan(app);
    policyEvaluateService.evaluateWithPolling(IntegrationType.CLI, app.getPublicId(), ClientScanType.SONATYPE, null,
        new Stage(BuildStageType.ID));
  }

  @Test
  public void testEvaluateWithPolling_ProxyStage_Authorized() throws Exception {
    Organization organization = tempEntity.newOrganizationWithRepositoryManager("org");
    Application application = tempEntity.newApplication("app", organization.getId());
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
    grantPermission(application.getId(), Permission.EVALUATE_COMPONENT);
    stubSuccessfulPollingScan(application);
    policyEvaluateService.evaluateWithPolling(IntegrationType.CLI, application.getPublicId(), ClientScanType.SONATYPE,
        null, new Stage(ProxyStageType.ID));
  }

  @Test
  public void testEvaluateWithPolling_ProxyStage_Unauthorized() throws Exception {
    Organization organization = tempEntity.newOrganizationWithRepositoryManager("org");
    Application application = tempEntity.newApplication("app", organization.getId());
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
    grantPermission(application.getId(), Permission.EVALUATE_APPLICATION);
    assertThrows(UnauthorizedException.class,
        () -> policyEvaluateService.evaluateWithPolling(IntegrationType.CLI, application.getPublicId(),
            ClientScanType.SONATYPE, null, new Stage(ProxyStageType.ID)));
  }

  @Test
  public void testEvaluateWithPolling_ProxyStage_BadRequest() throws Exception {
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
    grantPermission(app.getId(), Permission.EVALUATE_APPLICATION);
    assertThrows(BadRequestException.class,
        () -> policyEvaluateService.evaluateWithPolling(IntegrationType.CLI, app.getPublicId(),
            ClientScanType.SONATYPE, null, new Stage(ProxyStageType.ID)));
  }

  @Test
  public void testEvaluateWithPolling_Unauthorized() throws Exception {
    login();
    assertThrows(UnauthorizedException.class,
        () -> policyEvaluateService.evaluateWithPolling(IntegrationType.CLI, app.getPublicId(),
            ClientScanType.SONATYPE, null, new Stage(BuildStageType.ID)));
  }

  @Test
  public void testEvaluateWithPollingByStatusId_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> policyEvaluateService.evaluateWithPolling(IntegrationType.CLI, app.getPublicId(),
            ClientScanType.SONATYPE, null, new Stage(BuildStageType.ID), "statusId", null));
  }

  @Test
  public void testEvaluateWithPollingByStatusId_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> policyEvaluateService.evaluateWithPolling(IntegrationType.CLI, app.getPublicId(),
            ClientScanType.SONATYPE, null, new Stage(BuildStageType.ID), "statusId", null));
  }

  @Test
  public void testPollEvaluationResult_Authorized() {
    grantPermission(app.getId(), Permission.EVALUATE_APPLICATION);
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> policyEvaluateService.pollEvaluationResult(app.getPublicId(), "statusId"))
        .withMessage("Policy evaluation status with id %s for public application id %s was not found.", "statusId",
            app.getPublicId());
  }

  @Test
  public void testPollEvaluationResult_Unauthorized() {
    login();
    String statusId = TemporaryEntity.uuid();
    grantEvaluateComponentPermission(app.getId());
    insertPersistedPolicyEvaluationPollingResult(statusId, app.getId());
    assertThrows(UnauthorizedException.class,
        () -> policyEvaluateService.pollEvaluationResult(app.getPublicId(), statusId));
  }

  @Test
  public void testPollEvaluationResultContainerImage_Unauthenticated() {
    Organization organization = tempEntity.newOrganization();
    organization.setRelatedRepositoryId("repositoryId");
    organizationDAO.update(organization);
    Application application = tempEntity.newApplication("app", organization.getId());

    String statusId = TemporaryEntity.uuid();
    insertPersistedPolicyEvaluationPollingResult(statusId, application.getId());
    assertThrows(UnauthenticatedException.class,
        () -> policyEvaluateService.pollEvaluationResult(application.getPublicId(), statusId));
  }

  @Test
  public void testPollEvaluationResult_Unauthenticated() {
    String statusId = TemporaryEntity.uuid();
    insertPersistedPolicyEvaluationPollingResult(statusId, app.getId());
    assertThrows(UnauthenticatedException.class,
        () -> policyEvaluateService.pollEvaluationResult(app.getPublicId(), statusId));
  }

  @Test
  public void testPollEvaluationResultContainerImage_Unauthorized() {
    Organization organization = tempEntity.newOrganization();
    organization.setRelatedRepositoryId("repositoryId");
    organizationDAO.update(organization);
    Application application = tempEntity.newApplication("app", organization.getId());
    grantAddApplicationPermission(application.getId());
    login();
    String statusId = TemporaryEntity.uuid();
    insertPersistedPolicyEvaluationPollingResult(statusId, application.getId());
    assertThrows(UnauthorizedException.class,
        () -> policyEvaluateService.pollEvaluationResult(application.getPublicId(), statusId));
  }

  @Test
  public void testPollEvaluationResultContainerImage_Authorized() {
    Organization organization = tempEntity.newOrganization();
    organization.setRelatedRepositoryId("repositoryId");
    organizationDAO.update(organization);
    Application application = tempEntity.newApplication("app", organization.getId());
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
    grantEvaluateComponentPermission(application.getId());
    login();
    String statusId = TemporaryEntity.uuid();
    insertPersistedPolicyEvaluationPollingResult(statusId, application.getId());
    policyEvaluateService.pollEvaluationResult(application.getPublicId(), statusId);
  }

  @Test
  public void testEvaluateWithPollingContainerImage_Unauthorized() throws Exception {
    Organization organization = tempEntity.newOrganization();
    organization.setRelatedRepositoryId("repositoryId");
    organizationDAO.update(organization);
    Application application = tempEntity.newApplication("app", organization.getId());
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
    login();
    grantAddApplicationPermission(application.getId());
    assertThrows(UnauthorizedException.class,
        () -> policyEvaluateService.evaluateWithPolling(IntegrationType.CLI, application.getPublicId(),
            ClientScanType.SONATYPE, null, new Stage(ProxyStageType.ID)));
  }

  @Test
  public void testEvaluateWithPollingContainerImage_Unauthenticated() throws Exception {
    Organization organization = tempEntity.newOrganization();
    organization.setRelatedRepositoryId("repositoryId");
    organizationDAO.update(organization);
    Application application = tempEntity.newApplication("app", organization.getId());
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
    assertThrows(UnauthenticatedException.class,
        () -> policyEvaluateService.evaluateWithPolling(IntegrationType.CLI, application.getPublicId(),
            ClientScanType.SONATYPE, null, new Stage(ProxyStageType.ID)));
  }

  @Test
  public void testEvaluateWithPollingContainerImage_Authorized() throws Exception {
    Organization organization = tempEntity.newOrganization();
    organization.setRelatedRepositoryId("repositoryId");
    organizationDAO.update(organization);
    Application application = tempEntity.newApplication("app", organization.getId());
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
    grantEvaluateComponentPermission(application.getId());
    login();
    stubSuccessfulPollingScan(application);
    policyEvaluateService.evaluateWithPolling(IntegrationType.CLI, application.getPublicId(), ClientScanType.SONATYPE,
        null, new Stage(ProxyStageType.ID));
  }

  private void insertPersistedPolicyEvaluationPollingResult(String statusId, String appId) {
    PolicyEvaluationPollingResult policyEvaluationPollingResult = new PolicyEvaluationPollingResult();
    policyEvaluationPollingResult.setReason("reason");
    PersistedPolicyEvaluationPollingResult expected =
        new PersistedPolicyEvaluationPollingResult(appId, statusId, policyEvaluationPollingResult);
    persistedPolicyEvaluationPollingResultDAO.insert(expected);
  }

  private void stubSuccessfulPollingScan(Application application) throws Exception {
    String scanId = mockReportDownloader.mockDownloadReport("/PolicyEvaluateServiceTest/report");
    ScanHelper.createDummyScanFile(insightWork, application.getId(), scanId);

    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(scanId);

    when(mockScanHandler.createTempScanFile(any(), any(Application.class))).thenReturn(mock(ScanEntity.class));
    when(mockScanHandler.handle(any(ScanHandler.ScanRequest.class))).thenReturn(scanReceipt);
  }
}
