/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

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
import com.sonatype.insight.brain.report.ReportDownloader;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.utils.ScanHelper;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.scan.model.ClientScanType;

import com.google.inject.Binder;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

public class PolicyEvaluateServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private PolicyEvaluateService policyEvaluateService;

  @Inject
  private InsightWork insightWork;

  private MockReportDownloader mockReportDownloader;

  private PersistedPolicyEvaluationPollingResultDAO persistedPolicyEvaluationPollingResultDAO;

  private OrganizationDAO organizationDAO;

  @Before
  public void setup() {
    mockReportDownloader.setInsightWork(insightWork);
    persistedPolicyEvaluationPollingResultDAO = daoFactory.createPersistedPolicyEvaluationPollingResultDAO();
    organizationDAO = daoFactory.createOrganizationDAO();
  }

  @Override
  public void configure(Binder binder) {
    mockReportDownloader = new MockReportDownloader(tempDir);
    binder.bind(ReportDownloader.class).toInstance(mockReportDownloader.getMock());
    binder.bind(TelemetrySender.class).toInstance(mock(TelemetrySender.class));
    binder.bind(ScanHandler.class).toInstance(mock(ScanHandler.class));

    super.configure(binder);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testEvaluate_Unauthenticated() throws Exception {
    policyEvaluateService.evaluate(app.getPublicId(), "scanId", new Stage(BuildStageType.ID),
        ScanTriggerType.CLI);
  }

  @Test
  public void testEvaluate_Authorized() throws Exception {
    grantPermission(app.getId(), Permission.EVALUATE_APPLICATION);

    String scanId = mockReportDownloader.mockDownloadReport("/PolicyEvaluateServiceTest/report");
    ScanHelper.createDummyScanFile(lookup(InsightWork.class), app.getId(), scanId);

    policyEvaluateService.evaluate(app.getPublicId(), scanId, new Stage(BuildStageType.ID),
        ScanTriggerType.CLI);
  }

  @Test(expected = UnauthorizedException.class)
  public void testEvaluate_Unauthorized() throws Exception {
    login();
    policyEvaluateService.evaluate(app.getPublicId(), "scanId", new Stage(BuildStageType.ID),
        ScanTriggerType.CLI);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testEvaluateWithPolling_Unauthenticated() throws Exception {
    policyEvaluateService.evaluateWithPolling(IntegrationType.CLI, app.getPublicId(), ClientScanType.SONATYPE, null,
        new Stage(BuildStageType.ID));
  }

  @Test
  public void testEvaluateWithPolling_Authorized() throws Exception {
    grantPermission(app.getId(), Permission.EVALUATE_APPLICATION);
    policyEvaluateService.evaluateWithPolling(IntegrationType.CLI, app.getPublicId(), ClientScanType.SONATYPE, null,
        new Stage(BuildStageType.ID));
  }

  @Test
  public void testEvaluateWithPolling_ProxyStage_Authorized() throws Exception {
    Organization organization = tempEntity.newOrganizationWithRepositoryManager("org");
    Application application = tempEntity.newApplication("app", organization.getId());
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
    grantPermission(application.getId(), Permission.EVALUATE_COMPONENT);
    policyEvaluateService.evaluateWithPolling(IntegrationType.CLI, application.getPublicId(), ClientScanType.SONATYPE,
        null, new Stage(ProxyStageType.ID));
  }

  @Test(expected = UnauthorizedException.class)
  public void testEvaluateWithPolling_ProxyStage_Unauthorized() throws Exception {
    Organization organization = tempEntity.newOrganizationWithRepositoryManager("org");
    Application application = tempEntity.newApplication("app", organization.getId());
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
    grantPermission(application.getId(), Permission.EVALUATE_APPLICATION);
    policyEvaluateService.evaluateWithPolling(IntegrationType.CLI, application.getPublicId(), ClientScanType.SONATYPE,
        null, new Stage(ProxyStageType.ID));
  }

  @Test(expected = BadRequestException.class)
  public void testEvaluateWithPolling_ProxyStage_BadRequest() throws Exception {
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
    grantPermission(app.getId(), Permission.EVALUATE_APPLICATION);
    policyEvaluateService.evaluateWithPolling(IntegrationType.CLI, app.getPublicId(), ClientScanType.SONATYPE, null,
        new Stage(ProxyStageType.ID));
  }

  @Test(expected = UnauthorizedException.class)
  public void testEvaluateWithPolling_Unauthorized() throws Exception {
    login();
    policyEvaluateService.evaluateWithPolling(IntegrationType.CLI, app.getPublicId(), ClientScanType.SONATYPE, null,
        new Stage(BuildStageType.ID));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testEvaluateWithPollingByStatusId_Unauthenticated() {
    policyEvaluateService.evaluateWithPolling(IntegrationType.CLI, app.getPublicId(), ClientScanType.SONATYPE, null,
        new Stage(BuildStageType.ID), "statusId", null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testEvaluateWithPollingByStatusId_Unauthorized() {
    login();
    policyEvaluateService.evaluateWithPolling(IntegrationType.CLI, app.getPublicId(), ClientScanType.SONATYPE, null,
        new Stage(BuildStageType.ID), "statusId", null);
  }

  @Test
  public void testPollEvaluationResult_Authorized() {
    grantPermission(app.getId(), Permission.EVALUATE_APPLICATION);
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> policyEvaluateService.pollEvaluationResult(app.getPublicId(), "statusId"))
        .withMessage("Policy evaluation status with id %s for public application id %s was not found.", "statusId",
            app.getPublicId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testPollEvaluationResult_Unauthorized() {
    login();
    String statusId = TemporaryEntity.uuid();
    grantEvaluateComponentPermission(app.getId());
    insertPersistedPolicyEvaluationPollingResult(statusId, app.getId());
    policyEvaluateService.pollEvaluationResult(app.getPublicId(), statusId);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testPollEvaluationResultContainerImage_Unauthenticated() {
    Organization organization = tempEntity.newOrganization();
    organization.setRelatedRepositoryId("repositoryId");
    organizationDAO.update(organization);
    Application application = tempEntity.newApplication("app", organization.getId());

    String statusId = TemporaryEntity.uuid();
    insertPersistedPolicyEvaluationPollingResult(statusId, application.getId());
    policyEvaluateService.pollEvaluationResult(application.getPublicId(), statusId);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testPollEvaluationResult_Unauthenticated() {
    String statusId = TemporaryEntity.uuid();
    insertPersistedPolicyEvaluationPollingResult(statusId, app.getId());
    policyEvaluateService.pollEvaluationResult(app.getPublicId(), statusId);
  }

  @Test(expected = UnauthorizedException.class)
  public void testPollEvaluationResultContainerImage_Unauthorized() {
    Organization organization = tempEntity.newOrganization();
    organization.setRelatedRepositoryId("repositoryId");
    organizationDAO.update(organization);
    Application application = tempEntity.newApplication("app", organization.getId());
    grantAddApplicationPermission(application.getId());
    login();
    String statusId = TemporaryEntity.uuid();
    insertPersistedPolicyEvaluationPollingResult(statusId, application.getId());
    policyEvaluateService.pollEvaluationResult(application.getPublicId(), statusId);
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

  @Test(expected = UnauthorizedException.class)
  public void testEvaluateWithPollingContainerImage_Unauthorized() throws Exception {
    Organization organization = tempEntity.newOrganization();
    organization.setRelatedRepositoryId("repositoryId");
    organizationDAO.update(organization);
    Application application = tempEntity.newApplication("app", organization.getId());
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
    login();
    grantAddApplicationPermission(application.getId());
    policyEvaluateService.evaluateWithPolling(IntegrationType.CLI, application.getPublicId(), ClientScanType.SONATYPE,
        null, new Stage(ProxyStageType.ID));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testEvaluateWithPollingContainerImage_Unauthenticated() throws Exception {
    Organization organization = tempEntity.newOrganization();
    organization.setRelatedRepositoryId("repositoryId");
    organizationDAO.update(organization);
    Application application = tempEntity.newApplication("app", organization.getId());
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
    policyEvaluateService.evaluateWithPolling(IntegrationType.CLI, application.getPublicId(), ClientScanType.SONATYPE,
        null, new Stage(ProxyStageType.ID));
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
}
