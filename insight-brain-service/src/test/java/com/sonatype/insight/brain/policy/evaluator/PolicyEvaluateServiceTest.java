/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import jakarta.inject.Inject;
import jakarta.mail.Message;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.BadRequestException;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.container.image.ContainerImageTelemetryMetrics;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationReceipt;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationStatus;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.clm.dto.model.signature.VulnerabilitySignatureAnalysisDTO;
import com.sonatype.insight.brain.PolicyEvaluationHelper;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.configuration.MailConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PersistedPolicyEvaluationPollingResultDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.hds.ScanHandler;
import com.sonatype.insight.brain.integration.IntegrationType;
import com.sonatype.insight.brain.jira.JiraClient;
import com.sonatype.insight.brain.jira.JiraClientFactory;
import com.sonatype.insight.brain.jira.JiraField;
import com.sonatype.insight.brain.jira.JiraIssueCreateRequest;
import com.sonatype.insight.brain.jira.JiraIssueCreateRequest.JiraIssueCreateResponse;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.InvalidStageException;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.PersistedPolicyEvaluationPollingResult;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.notifications.JiraNotification;
import com.sonatype.insight.brain.model.policy.notifications.Notification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.organization.ApplicationContactLoader;
import com.sonatype.insight.brain.organization.ContactDTO;
import com.sonatype.insight.brain.policy.componentanalysis.ComponentAnalysisService;
import com.sonatype.insight.brain.policy.componentanalysis.ComponentAnalysisServiceTest;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.report.ApplicationReport;
import com.sonatype.insight.brain.report.MockReportDownloader;
import com.sonatype.insight.brain.report.ReportDownloader;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.scan.ScanContext;
import com.sonatype.insight.brain.scan.ScanResult;
import com.sonatype.insight.brain.scan.datastore.FileScanEntity;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.UserDirectory;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.shutdown.ShutdownPriority;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.utils.ScanHelper;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.error.exception.PaymentRequiredException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.scan.application.ScannerDriver;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.test.MailboxTestUtil;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.stubbing.answers.CallsRealMethods;
import org.mockito.invocation.InvocationOnMock;

import static com.sonatype.clm.dto.model.component.ComponentIdentifier.createMavenCoordinates;
import static com.sonatype.clm.dto.model.policy.PolicyEvaluationSubStatus.COMPONENT_ANALYSIS_COMPLETE;
import static com.sonatype.clm.dto.model.policy.PolicyEvaluationSubStatus.COMPONENT_ANALYSIS_PENDING;
import static com.sonatype.clm.dto.model.policy.PolicyEvaluationSubStatus.POLICY_EVALUATION_COMPLETE;
import static com.sonatype.clm.dto.model.policy.Stage.ID_BUILD;
import static com.sonatype.clm.dto.model.policy.Stage.ID_COMPLIANCE;
import static com.sonatype.clm.dto.model.policy.Stage.ID_PROXY;
import static com.sonatype.insight.brain.Assert.assertNotifications;
import static com.sonatype.insight.brain.hds.HdsClient.CLM_CLIENT_USER_AGENT_HEADER;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.POLICY_THREATS;
import static com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetryCreator.POLICY_VIOLATION_TELEMETRY;
import static com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetryCreator.REPOSITORY_COMPONENT_TELEMETRY;
import static com.sonatype.insight.brain.utils.VulnerabilitySignatureAnalysisDTOHelper.createTestAnalysisDTO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class PolicyEvaluateServiceTest
    extends AbstractComponentTest
{
  @Inject
  private PolicyEvaluateService policyEvaluateService;

  @Inject
  private TestProductLicenseManager productLicenseManager;

  @Inject
  private UserDirectory userDirectory;

  @Inject
  private PolicyDAO policyDAO;

  @Inject
  private ApplicationComponentDAO appComponentDAO;

  @Inject
  private MailConfigurationDAO mailConfigurationDAO;

  @Inject
  private PolicyEvaluationDAO policyEvaluationDAO;

  @Inject
  private PolicyViolationDAO policyViolationDAO;

  @Inject
  private ApplicationDAO applicationDAO;

  @Inject
  private OrganizationDAO organizationDAO;

  @Inject
  private RepositoryDAO repositoryDAO;

  @Inject
  private PolicyEvaluationHelper policyEvaluationHelper;

  @Inject
  private TestProductLicense testProductLicense;

  @Inject
  private ReportService reportService;

  @Inject
  private InsightWork insightWork;

  @Inject
  private PersistedPolicyEvaluationPollingResultDAO persistedPolicyEvaluationPollingResultDAO;

  @Inject
  private ComponentAnalysisService componentAnalysisService;

  private Application app;

  private JiraClientFactory mockJiraClientFactory;

  private MockReportDownloader mockReportDownloader;

  private ScanHandler mockScanHandler;

  private TelemetrySender mockTelemetrySender;

  @Mock
  private ShutdownHandler mockShutdownHandler;

  @Override
  public void configure(Binder binder) {
    mockReportDownloader = new MockReportDownloader(tempDir);
    binder.bind(ReportDownloader.class).toInstance(mockReportDownloader.getMock());
    mockJiraClientFactory = mock(JiraClientFactory.class);
    binder.bind(JiraClientFactory.class).toInstance(mockJiraClientFactory);
    mockTelemetrySender = mock(TelemetrySender.class);
    binder.bind(TelemetrySender.class).toInstance(mockTelemetrySender);
    mockScanHandler = mock(ScanHandler.class);
    binder.bind(ScanHandler.class).toInstance(mockScanHandler);
    binder.bind(TaskScheduler.class).toInstance(mock(TaskScheduler.class));
    binder.bind(ShutdownHandler.class).toInstance(mockShutdownHandler);
    super.configure(binder);
  }

  @Before
  public void before() {
    app = tempEntity.newApplicationWithParent();

    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setHostname("127.0.0.1");
    mailConfiguration.setPort(587);
    mailConfiguration.setSystemEmail("NexusIQServer@localhost");
    mailConfigurationDAO.set(mailConfiguration);

    mockReportDownloader.setInsightWork(insightWork);

    Mockito.reset(mockScanHandler);
  }

  @Test
  public void testPolicyEvaluationPolling_ComplianceStageValidation() throws IOException {
    assertThatNoException().isThrownBy(() -> policyEvaluateService.evaluateWithPolling(
        IntegrationType.CLI, app.getPublicId(), ClientScanType.SONATYPE, null, new Stage(ID_BUILD)));

    FileScanEntity fileScanEntity = new FileScanEntity(new File("parent/test-file.xml").toPath(), app.getId());
    when(mockScanHandler.createTempScanFile(any(HttpServletRequest.class), any(Application.class)))
        .thenReturn(fileScanEntity);
    HttpServletRequest mockedReq = mock(HttpServletRequest.class);
    assertThatNoException().isThrownBy(() -> policyEvaluateService.evaluateWithPolling(
        IntegrationType.CLI, app.getPublicId(), ClientScanType.SONATYPE, mockedReq, new Stage(ID_COMPLIANCE)));

    assertThatNoException().isThrownBy(() -> policyEvaluateService.evaluateWithPolling(
        "statusId", app, ClientScanType.SONATYPE, new Stage(ID_COMPLIANCE), ScanTriggerType.SBOM_API,
        mock(ScanEntity.class),
        "thirdPartyScanType", "clientUserAgent", "clientInstanceId"));
  }

  @Test
  public void testDefaultPolicyEvaluateService_AddsExecutorToShutdownHandler() {
    verify(mockShutdownHandler).add(policyEvaluateService.getExecutor(), ShutdownPriority.POLICY_EVALUATIONS);
  }

  private void assertPolicyEvaluation(
      String applicationId,
      String scanId,
      ScanTriggerType scanTriggerType,
      boolean isReevaluation)
  {
    assertPolicyEvaluation(applicationId, scanId, scanTriggerType, isReevaluation,
        false /* isForObsoleteScan */);
  }

  private void assertPolicyEvaluation(
      String applicationId,
      String scanId,
      ScanTriggerType scanTriggerType,
      boolean isReevaluation,
      boolean isForObsoleteScan)
  {
    PolicyEvaluation policyEvaluation = policyEvaluationDAO
        .getLastByApplicationIdAndScanId(applicationId, scanId);
    assertThat(policyEvaluation.getScanTriggerType()).isEqualTo(scanTriggerType);
    assertThat(policyEvaluation.isReevaluation()).isEqualTo(isReevaluation);
    assertThat(policyEvaluation.isForObsoleteScan()).isEqualTo(isForObsoleteScan);
  }

  @Test
  @Category(SlowTest.class)
  public void testEvaluate() throws Exception {
    setBaseUrl("http://localhost");
    createJiraConfiguration(null);
    JiraClient mockJiraClient = mock(JiraClient.class);
    when(mockJiraClientFactory.create(any())).thenReturn(mockJiraClient);
    JiraIssueCreateResponse createResponse = new JiraIssueCreateResponse();
    when(mockJiraClient.createIssue(any(JiraIssueCreateRequest.class), anyBoolean())).thenReturn(createResponse);

    String mailA = "manager@example.com";
    String mailB = "john.doe@example.com";

    final Policy policy1 = tempEntity.newPolicy(app, 8, LogicalOperator.AND,
        new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    addNotificationsToPolicy(policy1, Stage.ID_BUILD, //
        new UserNotification(mailA, Stage.ID_BUILD),
        new UserNotification(mailB, Stage.ID_BUILD),
        new JiraNotification("projectKey1", 1, Stage.ID_BUILD));

    // same conditions, but lower threat-level => analysis should show highest threat-level
    final Policy policy2 = tempEntity.newPolicy(app, 3, LogicalOperator.AND,
        new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    addNotificationsToPolicy(policy2, Stage.ID_RELEASE, //
        new UserNotification("Mark.MyWords@example.com", Stage.ID_RELEASE),
        new JiraNotification("projectKey2", 2, Stage.ID_RELEASE));

    final Stage stage = new Stage(Stage.ID_BUILD);

    String scanId = simulateReportIsAvailable();
    ScanHelper.createDummyScanFile(lookup(InsightWork.class), app.getId(), scanId);

    assertThat(appComponentDAO.getByApplicationIdAndStageTypeId(app.getId(), stage.getStageTypeId())).isEmpty();

    // evaluate policy
    PolicyEvaluationResult policyEvaluationResult =
        policyEvaluateService.evaluate(app.getPublicId(), scanId, stage, ScanTriggerType.CLI);
    assertEvaluate(scanId, stage, ScanTriggerType.CLI, policyEvaluationResult, policy1, mockJiraClient,
        appComponentDAO, mailA, mailB);
  }

  @Test
  public void testEvaluate_ChecksTheScanBelongsToApplication() throws Exception {
    String scanId = simulateReportIsAvailable();
    ScanHelper.createDummyScanFile(lookup(InsightWork.class), app.getId(), scanId);

    // Verify that policy evaluation is successful for the app owning the scan.
    Stage stage = new Stage(Stage.ID_BUILD);
    policyEvaluateService.evaluate(app.getPublicId(), scanId, stage, ScanTriggerType.CLI);

    // Verify that policy evaluation fails for a different app (doesn't own the scan).
    Application app1 = tempEntity.newApplicationWithParent();
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> policyEvaluateService.evaluate(app1.getPublicId(), scanId, stage, ScanTriggerType.CLI))
        .withMessage("Cannot find scan with ID " + scanId);
  }

  @Test
  @Category(SlowTest.class)
  public void testEvaluate_PolicyThreatLevelCounts() throws Exception {
    Policy policy = tempEntity.newPolicy(app, 1, LogicalOperator.AND,
        new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));

    final Stage stage = new Stage(Stage.ID_BUILD);

    InsightWork insightWork = lookup(InsightWork.class);
    String scanId = simulateReportIsAvailable();
    ScanHelper.createDummyScanFile(insightWork, app.getId(), scanId);

    PolicyEvaluationResult policyEvaluationResult =
        policyEvaluateService.evaluate(app.getPublicId(), scanId, stage, ScanTriggerType.CLI);

    // Threat Level 1 Should not show up in any counts
    assertThat(policyEvaluationResult.getAffectedComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getCriticalComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getSevereComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getModerateComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getCriticalPolicyViolationCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getSeverePolicyViolationCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getModeratePolicyViolationCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getLegacyViolationCount()).isEqualTo(0);

    policy.setThreatLevel(2);
    policyDAO.update(policy);
    scanId = simulateReportIsAvailable();
    ScanHelper.createDummyScanFile(insightWork, app.getId(), scanId);

    // Threat Level 2 should show up as moderate
    policyEvaluationResult =
        policyEvaluateService.evaluate(app.getPublicId(), scanId, stage, ScanTriggerType.CLI);
    assertThat(policyEvaluationResult.getAffectedComponentCount()).isEqualTo(7);
    assertThat(policyEvaluationResult.getCriticalComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getSevereComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getModerateComponentCount()).isEqualTo(7);
    assertThat(policyEvaluationResult.getCriticalPolicyViolationCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getSeverePolicyViolationCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getModeratePolicyViolationCount()).isEqualTo(36);
    assertThat(policyEvaluationResult.getLegacyViolationCount()).isEqualTo(0);

    policy.setThreatLevel(4);
    policyDAO.update(policy);
    scanId = simulateReportIsAvailable();
    ScanHelper.createDummyScanFile(insightWork, app.getId(), scanId);

    // Threat Level 4 should show up as severe
    policyEvaluationResult =
        policyEvaluateService.evaluate(app.getPublicId(), scanId, stage, ScanTriggerType.CLI);
    assertThat(policyEvaluationResult.getAffectedComponentCount()).isEqualTo(7);
    assertThat(policyEvaluationResult.getCriticalComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getSevereComponentCount()).isEqualTo(7);
    assertThat(policyEvaluationResult.getModerateComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getCriticalPolicyViolationCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getSeverePolicyViolationCount()).isEqualTo(36);
    assertThat(policyEvaluationResult.getModeratePolicyViolationCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getLegacyViolationCount()).isEqualTo(0);

    policy.setThreatLevel(8);
    policyDAO.update(policy);
    scanId = simulateReportIsAvailable();
    ScanHelper.createDummyScanFile(insightWork, app.getId(), scanId);

    // Threat Level 8 should show up as severe
    policyEvaluationResult =
        policyEvaluateService.evaluate(app.getPublicId(), scanId, stage, ScanTriggerType.CLI);
    assertThat(policyEvaluationResult.getAffectedComponentCount()).isEqualTo(7);
    assertThat(policyEvaluationResult.getCriticalComponentCount()).isEqualTo(7);
    assertThat(policyEvaluationResult.getSevereComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getModerateComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getCriticalPolicyViolationCount()).isEqualTo(36);
    assertThat(policyEvaluationResult.getSeverePolicyViolationCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getModeratePolicyViolationCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getLegacyViolationCount()).isEqualTo(0);

    // One legacy violation
    PolicyViolation policyViolation = policyViolationDAO
        .getActiveByApplicationIdAndStageId(app.getId(), stage.getStageTypeId()).get(0);
    policyViolationDAO.loadConstraintFacts(Collections.singletonList(policyViolation));
    policyViolation.setLegacyViolationTime(new Date());
    policyViolationDAO.update(policyViolation);
    scanId = simulateReportIsAvailable();
    ScanHelper.createDummyScanFile(insightWork, app.getId(), scanId);

    policyEvaluationResult =
        policyEvaluateService.evaluate(app.getPublicId(), scanId, stage, ScanTriggerType.CLI);
    assertThat(policyEvaluationResult.getAffectedComponentCount()).isEqualTo(7);
    assertThat(policyEvaluationResult.getCriticalComponentCount()).isEqualTo(7);
    assertThat(policyEvaluationResult.getSevereComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getModerateComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getCriticalPolicyViolationCount()).isEqualTo(35);
    assertThat(policyEvaluationResult.getSeverePolicyViolationCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getModeratePolicyViolationCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getLegacyViolationCount()).isEqualTo(1);
  }

  @Test
  @Category(SlowTest.class)
  public void testEvaluate_NotificationEmailModel() throws Exception {
    tempEntity.newPolicy(app, 8, LogicalOperator.AND,
        new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "5"));
    tempEntity.newPolicy(app, 4, LogicalOperator.AND,
        new Condition(CoordinatesConditionType.ID, "match", "maven:tomcat"));
    tempEntity.newPolicy(app, 3, LogicalOperator.AND,
        new Condition(CoordinatesConditionType.ID, "match", "maven:org.*"));
    tempEntity.newPolicy(app, 0, LogicalOperator.AND,
        new Condition(SecurityVulnerabilitySeverityConditionType.ID, "<", "5"));

    final Stage stage = new Stage(Stage.ID_BUILD);

    String scanId = simulateReportIsAvailable();
    ScanHelper.createDummyScanFile(lookup(InsightWork.class), app.getId(), scanId);

    PolicyEvaluationResult policyEvaluationResult =
        policyEvaluateService.evaluate(app.getPublicId(), scanId, stage, ScanTriggerType.CLI);

    List<PolicyFact> policyFacts = new ArrayList<>();
    for (PolicyAlert policyAlert : policyEvaluationResult.getAlerts()) {
      policyFacts.add(policyAlert.getTrigger());
    }

    app.setContactInternalName(User.ADMIN_USERNAME);
    applicationDAO.update(app);

    PolicyAlertEmailer emailer = lookup(PolicyAlertEmailer.class);

    String serverUrl = "http://localhost/";
    setBaseUrl(serverUrl);
    ContactDTO appContact =
        ApplicationContactLoader.getInstance(userDirectory).getContact(app.getContactInternalName());
    Map<String, Object> baseModel =
        emailer.createPolicyMailModel("https://cdn.sonatype.com/", app, StageTypes.getById(stage.getStageTypeId()),
            policyFacts);
    Map<String, Object> model =
        emailer.createPolicyMailModel(app, appContact, scanId, 8, baseModel);
    assertThat(model.get("policyFacts")).isEqualTo(policyFacts);
    assertThat(model.get("cdnUrl")).isEqualTo("https://cdn.sonatype.com/");
    assertThat(model.get("detailedReportUrl"))
        .isEqualTo(serverUrl + UserInterfaceLinksHelper.getReportUrl(app.getPublicId(), scanId));
    assertThat(model.get("policyThreatRedCount")).isEqualTo(18);
    assertThat(model.get("policyThreatOrangeCount")).isEqualTo(3);
    assertThat(model.get("policyThreatYellowCount")).isEqualTo(13);
    assertThat(model.get("policyThreatBlueCount")).isEqualTo(18);
    assertThat(model.get("policyThreatStage")).isEqualTo("Build");
    assertThat(model.get("policyThreatApp")).isEqualTo(app.getPublicId());
    assertThat(model.get("applicationContactName")).isEqualTo("Admin BuiltIn");
    assertThat(model.get("applicationContactEmail")).isEqualTo("admin@localhost");
    assertThat(model.get("policyThreatTime")).isNotNull();
    assertThat(model.get("ownerIdLabel")).isEqualTo("APP ID");
    assertThat(model.get("legacyViolationCount")).isEqualTo(8);
  }

  @Test
  @Category(SlowTest.class)
  public void testEvaluate_ReEvaluateNotifications() throws Exception {
    Policy policy = tempEntity.newPolicy(app, 8, LogicalOperator.AND,
        new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    addNotificationsToPolicy(policy, Stage.ID_BUILD, new UserNotification("manager@test.corp", Stage.ID_BUILD));

    Stage stage = new Stage(Stage.ID_BUILD);

    setBaseUrl("http://localhost");

    List<Message> notifications = MailboxTestUtil.get("manager@test.corp");
    notifications.clear();

    // Evaluate policy
    String scanId = simulateReportIsAvailable();

    ScanHelper.createDummyScanFile(lookup(InsightWork.class), app.getId(), scanId);
    PolicyEvaluationResult policyEvaluationResult =
        policyEvaluateService.evaluate(app.getPublicId(), scanId, stage, ScanTriggerType.CLI);

    List<PolicyAlert> policyAlerts = policyEvaluationResult.getAlerts();
    assertThat(policyAlerts).hasSize(36);
    assertPolicyEvaluation(app.getId(), scanId, ScanTriggerType.CLI, false /* isReevaluation */);

    // Notification message should have been sent
    assertNotifications(notifications, 1, 5000);
    notifications.clear();

    // Change the policy name
    policy.setName(policy.getName() + "Updated");
    policyDAO.update(policy);

    // Evaluate policy again for the same scan
    policyEvaluationResult =
        policyEvaluateService.evaluate(app.getPublicId(), scanId, stage, ScanTriggerType.CLI);
    policyAlerts = policyEvaluationResult.getAlerts();
    assertThat(policyAlerts).hasSize(36);
    assertPolicyEvaluation(app.getId(), scanId, ScanTriggerType.CLI, true /* isReevaluation */);

    // Notification message should not have been sent since this is a re-evaluation
    assertNotifications(notifications, 0, 5000);
  }

  @Test
  @Category(SlowTest.class)
  public void testEvaluateWithPolling_CI() throws Exception {
    testEvaluateWithPolling(LicensedFeature.CI_INTEGRATION, IntegrationType.CI,
        ScanTriggerType.CONTINUOUS_INTEGRATION);
  }

  @Test
  @Category(SlowTest.class)
  public void testEvaluateWithPolling_CLI() throws Exception {
    testEvaluateWithPolling(LicensedFeature.CLI_INTEGRATION, IntegrationType.CLI, ScanTriggerType.CLI);
  }

  @Test
  @Category(SlowTest.class)
  public void testEvaluateWithPolling_RepoManager() throws Exception {
    testEvaluateWithPolling(LicensedFeature.RM_STAGING_INTEGRATION, IntegrationType.RM,
        ScanTriggerType.REPOSITORY_MANAGER);
  }

  private void testEvaluateWithPolling(
      LicensedFeature requiredFeature,
      IntegrationType integrationType,
      ScanTriggerType scanTriggerType)
      throws Exception
  {
    productLicenseManager.setFeatures(requiredFeature, LicensedFeature.NOTIFICATIONS);
    setBaseUrl("http://localhost");
    createJiraConfiguration(null);
    JiraClient mockJiraClient = mock(JiraClient.class);
    when(mockJiraClientFactory.create(any())).thenReturn(mockJiraClient);
    JiraIssueCreateResponse createResponse = new JiraIssueCreateResponse();
    when(mockJiraClient.createIssue(any(JiraIssueCreateRequest.class), anyBoolean())).thenReturn(createResponse);

    String mailA = "managerWithPolling@example.com";
    String mailB = "john.doeWithPolling@example.com";

    final Policy policy1 = tempEntity.newPolicy(app, 8, LogicalOperator.AND,
        new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    addNotificationsToPolicy(policy1, Stage.ID_BUILD, //
        new UserNotification(mailA, Stage.ID_BUILD),
        new UserNotification(mailB, Stage.ID_BUILD),
        new JiraNotification("projectKey1", 1, Stage.ID_BUILD));

    // same conditions, but lower threat-level => analysis should show highest threat-level
    final Policy policy2 = tempEntity.newPolicy(app, 3, LogicalOperator.AND,
        new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    addNotificationsToPolicy(policy2, Stage.ID_RELEASE, //
        new UserNotification("Mark.MyWords@example.com", Stage.ID_RELEASE),
        new JiraNotification("projectKey2", 2, Stage.ID_RELEASE));

    final Stage stage = new Stage(Stage.ID_BUILD);

    String scanId = simulateReportIsAvailable();

    ScanHelper.createDummyScanFile(lookup(InsightWork.class), app.getId(), scanId);

    assertThat(appComponentDAO.getByApplicationIdAndStageTypeId(app.getId(), stage.getStageTypeId())).isEmpty();

    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(scanId);

    String testClientUserAgent = "testClientUserAgent";
    HttpServletRequest mockHttpServletRequest = mock(HttpServletRequest.class);
    when(mockHttpServletRequest.getHeader(HdsClient.CLM_CLIENT_USER_AGENT_HEADER))
        .thenReturn(testClientUserAgent);

    when(mockScanHandler.createTempScanFile(eq(mockHttpServletRequest), any(Application.class)))
        .thenReturn(mock(ScanEntity.class));
    when(mockScanHandler.handle(any(ScanHandler.ScanRequest.class)))
        .thenReturn(scanReceipt);

    // evaluate policy
    PolicyEvaluationReceipt policyEvaluationReceipt = policyEvaluateService.evaluateWithPolling(integrationType,
        app.getPublicId(), ClientScanType.SONATYPE, mockHttpServletRequest, stage);

    PolicyEvaluationPollingResult policyEvaluationPollingResult = policyEvaluationHelper
        .awaitEvaluationCompleted(app.getId(), policyEvaluationReceipt.getStatusId());

    PolicyEvaluationResult policyEvaluationResult = policyEvaluationPollingResult.getResult();
    assertThat(policyEvaluationPollingResult.getStatus()).isEqualTo(PolicyEvaluationStatus.COMPLETED);
    assertThat(policyEvaluationPollingResult.getReason()).isNull();
    assertThat(policyEvaluationPollingResult.getResult()).isNotNull();
    assertThat(policyEvaluationPollingResult.getScanReceipt()).usingRecursiveComparison().isEqualTo(scanReceipt);

    assertEvaluate(scanId, stage, scanTriggerType, policyEvaluationResult, policy1, mockJiraClient,
        appComponentDAO, mailA, mailB);
  }

  @Test
  @Category(SlowTest.class)
  public void testEvaluateWithPolling_sendThirdPartyScanUsageTelemetry() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);

    String scanId = simulateReportIsAvailable();

    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(scanId);
    when(mockScanHandler.createTempScanFile(any(HttpServletRequest.class), any(Application.class)))
        .thenReturn(mock(ScanEntity.class));
    when(mockScanHandler.handle(any(ScanHandler.ScanRequest.class)))
        .thenReturn(scanReceipt);

    HttpServletRequest req = mock(HttpServletRequest.class);
    when(req.getHeader(HdsClient.CLM_CLIENT_USER_AGENT_HEADER)).thenReturn("userAgent");

    PolicyEvaluationReceipt policyEvaluationReceipt = policyEvaluateService
        .evaluateWithPolling(IntegrationType.CLI, app.getPublicId(), ClientScanType.SONATYPE_THIRD_PARTY, req, stage);

    policyEvaluationHelper.awaitEvaluationCompleted(app.getId(), policyEvaluationReceipt.getStatusId());

    ArgumentCaptor<ScanHandler.ScanRequest> scanRequestCaptor = ArgumentCaptor.forClass(ScanHandler.ScanRequest.class);
    verify(mockScanHandler).handle(scanRequestCaptor.capture());
    TelemetryData telemetryData = scanRequestCaptor.getValue().getThirdPartyScanTelemetryData();
    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.THIRD_PARTY_SCAN_USAGE);

    Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put("application_id", HdsClientAnalytics.obfuscate(app.getId()));
    expectedAttributes.put("real_application_id", app.getId());
    expectedAttributes.put("stage_id", stage.getStageTypeId());
    expectedAttributes.put("source", IntegrationType.CLI.toString());
    expectedAttributes.put("scan_type", ScanTriggerType.CLI.toString());
    expectedAttributes.put("user_agent", "userAgent");
    assertThat(telemetryData.getAttributes()).isEqualTo(expectedAttributes);
  }

  @Test
  public void testEvaluateWithPolling_Pending_StatusId() throws Exception {
    setBaseUrl("http://localhost");

    final Stage stage = new Stage(Stage.ID_BUILD);

    assertThat(appComponentDAO.getByApplicationIdAndStageTypeId(app.getId(), stage.getStageTypeId())).isEmpty();

    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId("scanId");

    when(mockScanHandler.createTempScanFile(eq(null), any(Application.class))).thenReturn(mock(ScanEntity.class));

    when(mockScanHandler.handle(any(ScanHandler.ScanRequest.class)))
        .thenReturn(scanReceipt);

    // using the spy to put a delay into the real service so we make sure the Polling Result does not
    // reach COMPLETED before we have a chance to see it PENDING with a scan receipt
    PolicyEvaluateService spyService = spy(policyEvaluateService);

    doAnswer(new CallsRealMethods()
    {
      private static final long serialVersionUID = 453256790682974127L;

      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        Thread.sleep(100);
        return super.answer(invocation);
      }
    }).when(spyService).evaluate(any(Application.class), anyString(), any(Stage.class),
        any(ScanTriggerType.class), eq(null), eq(null), eq(ClientScanType.SONATYPE));

    // evaluate policy
    PolicyEvaluationReceipt policyEvaluationReceipt =
        spyService.evaluateWithPolling(IntegrationType.CLI, app.getPublicId(), ClientScanType.SONATYPE, null, stage);

    PolicyEvaluationPollingResultDTO policyEvaluationPollingResult =
        waitForResult(app.getPublicId(), policyEvaluationReceipt.getStatusId(),
            p -> p.status == PolicyEvaluationStatus.PENDING && p.scanReceipt != null);

    assertThat(policyEvaluationPollingResult.status).isEqualTo(PolicyEvaluationStatus.PENDING);
    assertThat(policyEvaluationPollingResult.reason).isNull();
    assertThat(policyEvaluationPollingResult.result).isNull();
    assertThat(policyEvaluationPollingResult.subStatus).isNull();
    assertThat(policyEvaluationPollingResult.scanReceipt).usingRecursiveComparison().isEqualTo(scanReceipt);
  }

  private PolicyEvaluationPollingResultDTO waitForResult(
      String appId,
      String scanId,
      Function<PolicyEvaluationPollingResultDTO, Boolean> readyTest) throws Exception
  {
    long endTime = System.currentTimeMillis() + 50000;
    PolicyEvaluationPollingResultDTO result;
    while (System.currentTimeMillis() < endTime) {
      result = policyEvaluateService.pollEvaluationResult(appId, scanId);
      if (readyTest.apply(result)) {
        return result;
      }
      Thread.sleep(50);
    }
    throw new RuntimeException("Evaluation did not complete within the expected 20 seconds to get the polling result.");
  }

  @Test
  public void testEvaluateWithPolling_PollEvaluationResult_Pending() throws Exception {
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scanid");
    Application app = tempEntity.newApplicationWithParent();

    CountDownLatch countDownLatch = new CountDownLatch(1);
    lenient().doAnswer(invocation -> {
      countDownLatch.await(1, TimeUnit.MINUTES);
      return null;
    }).when(mockScanHandler).handle(any(ScanHandler.ScanRequest.class));

    PolicyEvaluationReceipt receipt = policyEvaluateService.evaluateWithPolling(IntegrationType.CLI, app.getPublicId(),
        ClientScanType.SONATYPE, null, new Stage(Stage.ID_BUILD));

    PolicyEvaluationPollingResultDTO policyEvaluationPollingResult =
        policyEvaluateService.pollEvaluationResult(app.getPublicId(), receipt.getStatusId());
    countDownLatch.countDown();
    assertThat(policyEvaluationPollingResult).isNotNull();
    assertThat(policyEvaluationPollingResult.status).isEqualTo(PolicyEvaluationStatus.PENDING);
    assertThat(policyEvaluationPollingResult.reason).isNull();
    assertThat(policyEvaluationPollingResult.result).isNull();
    assertThat(policyEvaluationPollingResult.subStatus).isNull();
    assertThat(policyEvaluationPollingResult.scanReceipt).isNull();
    assertThat(policyEvaluationPollingResult.nextPollingIntervalInSeconds).isEqualTo(5);
  }

  @Test
  public void testEvaluateWithPolling_PollEvaluationResult_ContainerImage_Success_CLI() throws Exception {
    Organization organization = tempEntity.newOrganization();
    organization.setRelatedRepositoryId("repositoryId");
    organizationDAO.update(organization);
    Application app = tempEntity.newApplication("app", organization.getId());

    tempEntity.newPolicyEvaluation(app.getId(), ID_PROXY, "scanid");

    CountDownLatch countDownLatch = new CountDownLatch(1);
    lenient().doAnswer(invocation -> {
      countDownLatch.await(1, TimeUnit.MINUTES);
      return null;
    }).when(mockScanHandler).handle(any(ScanHandler.ScanRequest.class));

    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

    PolicyEvaluationReceipt receipt = policyEvaluateService.evaluateWithPolling(IntegrationType.CLI, app.getPublicId(),
        ClientScanType.SONATYPE, null, new Stage(ID_PROXY));

    PolicyEvaluationPollingResultDTO policyEvaluationPollingResult =
        policyEvaluateService.pollEvaluationResult(app.getPublicId(), receipt.getStatusId());
    countDownLatch.countDown();
    assertThat(policyEvaluationPollingResult).isNotNull();
    assertThat(policyEvaluationPollingResult.status).isEqualTo(PolicyEvaluationStatus.PENDING);
    assertThat(policyEvaluationPollingResult.reason).isNull();
    assertThat(policyEvaluationPollingResult.result).isNull();
    assertThat(policyEvaluationPollingResult.subStatus).isNull();
    assertThat(policyEvaluationPollingResult.scanReceipt).isNull();
    assertThat(policyEvaluationPollingResult.nextPollingIntervalInSeconds).isEqualTo(5);
  }

  @Test
  public void testEvaluateWithPolling_ContainerImage_FeatureFlagDisabled() {
    productLicenseManager.setFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION, LicensedFeature.CLI_INTEGRATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(false);

    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> policyEvaluateService.evaluateWithPolling(IntegrationType.CLI, app.getPublicId(),
            ClientScanType.SONATYPE, null, new Stage(ID_PROXY)))
        .withMessage("Application evaluation using the proxy stage is not supported by your license.");
  }

  @Test
  public void testEvaluateWithPolling_ContainerImage_MissingLicenseFeature() {
    productLicenseManager.setFeatures(LicensedFeature.CLI_INTEGRATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> policyEvaluateService.evaluateWithPolling(IntegrationType.CLI, app.getPublicId(),
            ClientScanType.SONATYPE, null, new Stage(ID_PROXY)))
        .withMessage("Application evaluation using the proxy stage is not supported by your license.");
  }

  @Test
  public void testEvaluateWithPolling_ContainerImage_NotRepositoryRelated() {
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> policyEvaluateService.evaluateWithPolling(IntegrationType.CLI, app.getPublicId(),
            ClientScanType.SONATYPE, null, new Stage(ID_PROXY)))
        .withMessage("Cannot evaluate a non-container image application with a proxy stage.");
  }

  @Test
  public void testPollEvaluationResult_ContainerImage_MissingLicenseFeature() {
    Organization organization = tempEntity.newOrganization();
    organization.setRelatedRepositoryId("repositoryId");
    organizationDAO.update(organization);
    Application app = tempEntity.newApplication("app", organization.getId());
    productLicenseManager.setFeatures();
    String statusId = TemporaryEntity.uuid();
    insertPersistedPolicyEvaluationPollingResult(statusId, app.getId());
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> policyEvaluateService.pollEvaluationResult(app.getPublicId(), statusId))
        .withMessage("Your IQ Server license does not enable this feature.");
  }

  @Test
  public void testPollEvaluationResult_ContainerImage_FeatureFlagDisabled() {
    Organization organization = tempEntity.newOrganization();
    organization.setRelatedRepositoryId("repositoryId");
    organizationDAO.update(organization);
    Application app = tempEntity.newApplication("app", organization.getId());

    String statusId = TemporaryEntity.uuid();
    insertPersistedPolicyEvaluationPollingResult(statusId, app.getId());
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(false);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> policyEvaluateService.pollEvaluationResult(app.getPublicId(), statusId))
        .withMessage("Your IQ Server license does not enable this feature.");
  }

  private void insertPersistedPolicyEvaluationPollingResult(String statusId, String appId) {
    PolicyEvaluationPollingResult policyEvaluationPollingResult = new PolicyEvaluationPollingResult();
    policyEvaluationPollingResult.setReason("reason");
    PersistedPolicyEvaluationPollingResult expected =
        new PersistedPolicyEvaluationPollingResult(appId, statusId, policyEvaluationPollingResult);
    persistedPolicyEvaluationPollingResultDAO.insert(expected);
  }

  @Test
  public void testCreatePolicyEvaluationPollingResultDTO_ShouldHandleResultSubStatus() {
    PolicyEvaluationPollingResult result = new PolicyEvaluationPollingResult();
    result.setSubStatus(COMPONENT_ANALYSIS_PENDING);

    PersistedPolicyEvaluationPollingResult persistedResult = new PersistedPolicyEvaluationPollingResult("", "", result);

    PolicyEvaluationPollingResultDTO resultDTO =
        policyEvaluateService.toPolicyEvaluationPollingResultDTO(persistedResult);
    assertThat(resultDTO.subStatus).isEqualTo(result.getSubStatus());
  }

  @Test
  public void testEvaluateWithPolling_PollEvaluationResult_Failure() throws Exception {
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scanid");

    Application app = tempEntity.newApplicationWithParent();

    when(mockScanHandler.createTempScanFile(eq(null), any(Application.class))).thenReturn(mock(ScanEntity.class));
    doThrow(new IOException("HDS Upload Scan Failure!!!"))
        .when(mockScanHandler)
        .handle(any(ScanHandler.ScanRequest.class));

    PolicyEvaluationReceipt receipt = policyEvaluateService
        .evaluateWithPolling(IntegrationType.CLI, app.getPublicId(), ClientScanType.SONATYPE, null,
            new Stage(Stage.ID_BUILD));

    PolicyEvaluationPollingResult policyEvaluationPollingResult =
        policyEvaluationHelper.awaitEvaluationFailed(app.getId(), receipt.getStatusId());

    assertThat(policyEvaluationPollingResult).isNotNull();
    assertThat(policyEvaluationPollingResult.getStatus()).isEqualTo(PolicyEvaluationStatus.FAILED);
    assertThat(policyEvaluationPollingResult.getReason()).startsWith("Internal Server Error");
    assertThat(policyEvaluationPollingResult.getResult()).isNull();
    assertThat(policyEvaluationPollingResult.getScanReceipt()).isNull();
    assertThat(policyEvaluationPollingResult.getNextPollingIntervalInSeconds()).isEqualTo(5);
  }

  @Test
  @Category(SlowTest.class)
  public void testEvaluateWithPolling_PollEvaluationResult_Success() throws Exception {
    String scanId = simulateReportIsAvailable();
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, scanId);

    Application app = tempEntity.newApplicationWithParent();

    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(scanId);

    when(mockScanHandler.createTempScanFile(eq(null), any(Application.class))).thenReturn(mock(ScanEntity.class));
    when(mockScanHandler.handle(any(ScanHandler.ScanRequest.class)))
        .thenReturn(scanReceipt);

    PolicyEvaluationReceipt receipt = policyEvaluateService
        .evaluateWithPolling(IntegrationType.CLI, app.getPublicId(), ClientScanType.SONATYPE, null,
            new Stage(Stage.ID_BUILD));

    PolicyEvaluationPollingResult policyEvaluationPollingResult =
        policyEvaluationHelper.awaitEvaluationCompleted(app.getId(), receipt.getStatusId());

    assertThat(policyEvaluationPollingResult).isNotNull();
    assertThat(policyEvaluationPollingResult.getStatus()).isEqualTo(PolicyEvaluationStatus.COMPLETED);
    assertThat(policyEvaluationPollingResult.getReason()).isNull();
    assertThat(policyEvaluationPollingResult.getResult()).isNotNull();
    assertThat(policyEvaluationPollingResult.getScanReceipt()).usingRecursiveComparison().isEqualTo(scanReceipt);
  }

  @Test
  public void testEvaluateWithPolling_InvalidStage() {
    assertThatExceptionOfType(InvalidStageException.class)
        .isThrownBy(() -> policyEvaluateService.evaluateWithPolling(IntegrationType.CLI, app.getPublicId(),
            ClientScanType.SONATYPE, null, new Stage("invalidStage")))
        .withMessage("Invalid stage id=invalidStage");
  }

  @Test
  public void testEvaluateWithPolling_StageNotLicensed() {
    productLicenseManager.setStageTypes(StageTypes.RELEASE);

    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> policyEvaluateService.evaluateWithPolling(IntegrationType.CLI, app.getPublicId(),
            ClientScanType.SONATYPE, null, new Stage("build")))
        .withMessage("Stage 'build' is not supported by your license.");
  }

  @Test
  public void testEvaluateWithPolling_FailsWithoutFeature() {
    productLicenseManager.setFeatures();

    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> policyEvaluateService.evaluateWithPolling(IntegrationType.CI, app.getPublicId(),
            ClientScanType.SONATYPE, null, new Stage(Stage.ID_BUILD)))
        .withMessage("Your IQ Server license does not enable this feature.");

    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> policyEvaluateService.evaluateWithPolling(IntegrationType.CLI, app.getPublicId(),
            ClientScanType.SONATYPE, null, new Stage(Stage.ID_BUILD)))
        .withMessage("Your IQ Server license does not enable this feature.");

    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> policyEvaluateService.evaluateWithPolling(IntegrationType.RM, app.getPublicId(),
            ClientScanType.SONATYPE, null, new Stage(Stage.ID_BUILD)))
        .withMessage("Your IQ Server license does not enable this feature.");
  }

  @Test
  @Category(SlowTest.class)
  public void testEvaluateWithPolling_AppPublicIdCaseInsensitive() throws Exception {
    Application app = tempEntity.newApplicationWithParent("THE-public-ID");
    ScanReceipt scanReceipt = new ScanReceipt();
    String scanId = simulateReportIsAvailable();
    scanReceipt.setScanId(scanId);

    when(mockScanHandler.createTempScanFile(eq(null), any(Application.class))).thenReturn(mock(ScanEntity.class));
    when(mockScanHandler.handle(any(ScanHandler.ScanRequest.class)))
        .thenReturn(scanReceipt);

    PolicyEvaluationReceipt receipt = policyEvaluateService.evaluateWithPolling(IntegrationType.CLI,
        app.getPublicId().toLowerCase(Locale.ENGLISH), ClientScanType.SONATYPE, null, new Stage(Stage.ID_BUILD));

    PolicyEvaluationPollingResult policyEvaluationPollingResult =
        policyEvaluationHelper.awaitEvaluationCompleted(app.getId(), receipt.getStatusId());

    assertThat(policyEvaluationPollingResult).isNotNull();
    assertThat(policyEvaluationPollingResult.getStatus()).isEqualTo(PolicyEvaluationStatus.COMPLETED);
    assertThat(policyEvaluationPollingResult.getReason()).isNull();
    assertThat(policyEvaluationPollingResult.getResult()).isNotNull();
    assertThat(policyEvaluationPollingResult.getScanReceipt()).usingRecursiveComparison().isEqualTo(scanReceipt);
  }

  /**
   * Simulates that a report (based on the specified resource) exists.
   *
   * @return A generated scan ID that can be used in subsequent calls to evaluate policies.
   */
  private String simulateReportIsAvailable() {
    return mockReportDownloader.mockDownloadReport("/" + getClass().getSimpleName() + "/report");
  }

  /**
   * Use the simulation of a {@link ComponentAnalysisServiceTest} report.
   *
   * @return A generated scan ID that can be used in subsequent calls to evaluate policies.
   */
  private String simulateComponentAnalysisReportIsAvailable() {
    return mockReportDownloader.mockDownloadReport(
        "/" + ComponentAnalysisServiceTest.class.getSimpleName() + "/report");
  }

  private void addNotificationsToPolicy(Policy policy, String stageId, Notification... notifications) {
    Arrays.stream(notifications).forEach(policy.getNotifications()::add);
    policy.getActions().clear();
    policy.setAction(stageId, Action.ID_FAIL);
    policyDAO.update(policy);
  }

  private void assertEvaluate(
      String scanId,
      Stage stage,
      ScanTriggerType scanTriggerType,
      PolicyEvaluationResult policyEvaluationResult,
      Policy policy1,
      JiraClient mockJiraClient,
      ApplicationComponentDAO appComponentDAO,
      String mailboxA,
      String mailboxB) throws Exception
  {
    final List<Message> messagesA = MailboxTestUtil.get(mailboxA);
    final List<Message> messagesB = MailboxTestUtil.get(mailboxB);

    assertThat(policyEvaluationResult.getAffectedComponentCount()).isEqualTo(7);
    assertThat(policyEvaluationResult.getCriticalComponentCount()).isEqualTo(7);
    assertThat(policyEvaluationResult.getSevereComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getModerateComponentCount()).isEqualTo(0);
    List<PolicyAlert> policyAlerts = policyEvaluationResult.getAlerts();
    assertThat(policyAlerts).hasSize(72);
    for (PolicyAlert policyAlert : policyAlerts) {
      AbstractPolicyEvaluationTest.assertFactCounts(1, 1, policyAlert);
    }
    assertPolicyEvaluation(app.getId(), scanId, scanTriggerType, false /* isReevaluation */);
    for (PolicyViolation policyViolation : policyViolationDAO.getActiveByApplicationIdAndStageId(app.getId(),
        stage.getStageTypeId())) {
      if (policyViolation.getPolicyId().equals(policy1.getId())) {
        assertThat(policyViolation.getActionTypeId()).isEqualTo(Action.ID_FAIL);
      }
      else {
        assertThat(policyViolation.getActionTypeId()).isNull();
      }
    }

    // check the calculated policy threat
    ApplicationReport applicationReport = reportService.getReport(app.getId(), scanId);
    ReportEntry policyThreatsReportEntry = applicationReport.getEntry(POLICY_THREATS.getName());
    final JsonNode policyThreats = JsonUtils.parse(policyThreatsReportEntry.buf).get("aaData");
    assertThat(policyThreats).isNotEmpty();
    assertThat(policyThreats.get(0).get("policyThreatLevel").asInt()).isEqualTo(8);

    // check components are associated with the application and stage
    assertThat(appComponentDAO.getByApplicationIdAndStageTypeId(app.getId(), stage.getStageTypeId())).hasSize(28);

    // notification message should also have been sent
    assertNotifications(messagesA, 1, 5000);
    assertThat(messagesA.get(0).getSubject()).contains("Policy");
    assertNotifications(messagesB, 1, 5000);
    assertThat(messagesB.get(0).getSubject()).contains("Policy");

    ArgumentCaptor<JiraIssueCreateRequest> createRequestArgumentCaptor = ArgumentCaptor
        .forClass(JiraIssueCreateRequest.class);
    verify(mockJiraClient, timeout(5000)).createIssue(createRequestArgumentCaptor.capture(), anyBoolean());
    JiraIssueCreateRequest jiraIssueCreateRequest = createRequestArgumentCaptor.getValue();
    assertThat(jiraIssueCreateRequest.getFields()).hasSize(4);
    Map<String, String> projectMeta = jiraIssueCreateRequest.getField(JiraField.PROJECT);
    assertThat(projectMeta).containsEntry("key", "projectKey1");

    messagesA.clear();
    messagesB.clear();

    reset(mockJiraClient);

    // evaluate policy again
    policyEvaluationResult =
        policyEvaluateService.evaluate(app.getPublicId(), scanId, stage, scanTriggerType);
    policyAlerts = policyEvaluationResult.getAlerts();
    assertThat(policyAlerts).hasSize(72);
    for (PolicyAlert policyAlert : policyAlerts) {
      AbstractPolicyEvaluationTest.assertFactCounts(1, 1, policyAlert);
    }
    assertPolicyEvaluation(app.getId(), scanId, scanTriggerType, true /* isReevaluation */);
    for (PolicyViolation policyViolation : policyViolationDAO.getActiveByApplicationIdAndStageId(app.getId(),
        stage.getStageTypeId())) {
      if (policyViolation.getPolicyId().equals(policy1.getId())) {
        assertThat(policyViolation.getActionTypeId()).isEqualTo(Action.ID_FAIL);
      }
      else {
        assertThat(policyViolation.getActionTypeId()).isNull();
      }
    }

    // notification message should not have been sent since the results are the same
    assertNotifications(messagesA, 0, 5000);
    assertNotifications(messagesB, 0, 1000);

    verify(mockJiraClient, times(0)).createIssue(any(JiraIssueCreateRequest.class), anyBoolean());

    messagesA.clear();
    messagesB.clear();
  }

  @Test
  public void testEvaluateSynchronousNoAuth() throws Exception {
    setBaseUrl("http://localhost");

    String mail = "userSynchronous@example.com";

    Stage stage = new Stage(Stage.ID_BUILD);
    Policy policy = tempEntity.newPolicy(app, 8, LogicalOperator.AND,
        new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    addNotificationsToPolicy(policy, stage.getStageTypeId(), new UserNotification(mail, stage.getStageTypeId()));

    String scanId = simulateReportIsAvailable();
    ScanEntity scanEntity =
        new FileScanEntity(ScanHelper.createDummyScanFile(lookup(InsightWork.class), app.getId(), scanId).toPath());
    assertThat(appComponentDAO.getByApplicationIdAndStageTypeId(app.getId(), stage.getStageTypeId())).isEmpty();

    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(scanId);
    when(mockScanHandler.handle(any(ScanHandler.ScanRequest.class))).thenReturn(scanReceipt);

    // evaluate policy
    String testClientUserAgent = "testClientUserAgent";
    ScanTriggerType scanTriggerType = ScanTriggerType.CLI;
    PolicyEvaluation policyEvaluation = policyEvaluateService.evaluateSynchronousNoAuth(app, ClientScanType.SONATYPE,
        scanEntity, stage, scanTriggerType, testClientUserAgent);

    assertThat(policyEvaluation.getApplicationId()).isEqualTo(app.getId());
    assertThat(policyEvaluation.getScanId()).isEqualTo(scanId);
    assertThat(policyEvaluation.getStageTypeId()).isEqualTo(stage.getStageTypeId());
    assertThat(policyEvaluation.getScanTriggerType()).isEqualTo(scanTriggerType);
    assertThat(policyEvaluation.isForMonitoring()).isFalse();
    assertThat(policyEvaluation.isReevaluation()).isFalse();
    assertThat(policyEvaluation.isForObsoleteScan()).isFalse();

    List<PolicyViolation> policyViolations =
        policyViolationDAO.getActiveByApplicationIdAndStageId(app.getId(), stage.getStageTypeId());
    assertThat(policyViolations).hasSize(36);
    for (PolicyViolation policyViolation : policyViolations) {
      assertThat(policyViolation.getPolicyId()).isEqualTo(policy.getId());
      assertThat(policyViolation.getActionTypeId()).isEqualTo(Action.ID_FAIL);
    }

    // check components are associated with the application and stage
    assertThat(appComponentDAO.getByApplicationIdAndStageTypeId(app.getId(), stage.getStageTypeId())).hasSize(28);

    // notification message should also have been sent
    List<Message> notifications = MailboxTestUtil.get(mail);
    assertNotifications(notifications, 1, 5000);
    assertThat(notifications.get(0).getSubject()).contains("Policy");
  }

  @Test
  public void testEvaluateWithPolling_CLI_SbomManager_ComplianceStage_MaxSbomLimitReached() throws IOException {
    testProductLicense.setMaxSbom(0);
    when(mockScanHandler.createTempScanFile(any(HttpServletRequest.class), any(Application.class)))
        .thenReturn(mock(ScanEntity.class));
    HttpServletRequest mockedReq = mock(HttpServletRequest.class);

    assertThatExceptionOfType(PaymentRequiredException.class)
        .isThrownBy(() -> policyEvaluateService.evaluateWithPolling(
            IntegrationType.CLI, app.getPublicId(), ClientScanType.SONATYPE, mockedReq, new Stage(ID_COMPLIANCE)))
        .withMessage("You have exceeded the licensed limit of 0 sboms.");
  }

  @Test
  @Category(SlowTest.class)
  public void testEvaluateWithPolling_WithReachableVulnerability_And_CompletedComponentAnalysis() throws Exception {
    PolicyEvaluationReceipt componentAnalyzeEvaluationReceipt = analyzeComponentsWithPolling();

    PersistedPolicyEvaluationPollingResult componentAnalyzePollingResult = persistedPolicyEvaluationPollingResultDAO
        .getByApplicationIdAndStatusId(
            app.getId(),
            componentAnalyzeEvaluationReceipt.getStatusId()
        );

    assertThat(componentAnalyzeEvaluationReceipt.getStatusId()).isEqualTo(componentAnalyzePollingResult.getStatusId());

    PolicyEvaluationPollingResult policyEvaluationPollingResult =
        componentAnalyzePollingResult.getPolicyEvaluationPollingResult();
    assertThat(policyEvaluationPollingResult.getSubStatus()).isEqualTo(COMPONENT_ANALYSIS_COMPLETE);

    ComponentIdentifier componentIdentifier = createMavenCoordinates("tomcat", "tomcat-util", "5.5.23");
    String vulnerabilityIdentifier = "CVE-2012-0022";

    VulnerabilitySignatureAnalysisDTO analysisDTO = createTestAnalysisDTO(
        app.getId(),
        policyEvaluationPollingResult.getScanReceipt().getScanId(),
        componentIdentifier,
        vulnerabilityIdentifier,
        lookup(InsightWork.class)
    );

    PolicyEvaluationReceipt receipt = policyEvaluateService.evaluateWithPolling(
        IntegrationType.CLI,
        app.getPublicId(),
        ClientScanType.SONATYPE,
        null,
        new Stage(Stage.ID_BUILD),
        componentAnalyzePollingResult.getStatusId(),
        analysisDTO
    );

    PolicyEvaluationPollingResult pollingResult = policyEvaluationHelper
        .awaitEvaluationCompleted(app.getId(), receipt.getStatusId());

    assertThat(pollingResult).isNotNull();
    assertThat(pollingResult.getStatus()).isEqualTo(PolicyEvaluationStatus.COMPLETED);
    assertThat(pollingResult.getSubStatus()).isEqualTo(POLICY_EVALUATION_COMPLETE);
    assertThat(pollingResult.getReason()).isNull();
    assertThat(pollingResult.getScanReceipt()).usingRecursiveComparison()
        .isEqualTo(policyEvaluationPollingResult.getScanReceipt());

    PolicyEvaluationResult result = pollingResult.getResult();
    assertThat(result).isNotNull();
    assertThat(result.getAlerts()).isEmpty();
  }

  @Test
  @Category(SlowTest.class)
  public void testEvaluateWithPolling_WithoutReachableVulnerability_And_CompletedComponentAnalysis() throws Exception {
    PolicyEvaluationReceipt componentAnalyzeEvaluationReceipt = analyzeComponentsWithPolling();

    PersistedPolicyEvaluationPollingResult componentAnalyzePollingResult = persistedPolicyEvaluationPollingResultDAO
        .getByApplicationIdAndStatusId(
            app.getId(),
            componentAnalyzeEvaluationReceipt.getStatusId()
        );

    assertThat(componentAnalyzeEvaluationReceipt.getStatusId()).isEqualTo(componentAnalyzePollingResult.getStatusId());

    PolicyEvaluationPollingResult policyEvaluationPollingResult =
        componentAnalyzePollingResult.getPolicyEvaluationPollingResult();
    assertThat(policyEvaluationPollingResult.getSubStatus()).isEqualTo(COMPONENT_ANALYSIS_COMPLETE);

    PolicyEvaluationReceipt receipt = policyEvaluateService.evaluateWithPolling(
        IntegrationType.CLI,
        app.getPublicId(),
        ClientScanType.SONATYPE,
        null,
        new Stage(Stage.ID_BUILD),
        componentAnalyzePollingResult.getStatusId(),
        null
    );

    PolicyEvaluationPollingResult pollingResult = policyEvaluationHelper
        .awaitEvaluationCompleted(app.getId(), receipt.getStatusId());

    assertThat(pollingResult).isNotNull();
    assertThat(pollingResult.getStatus()).isEqualTo(PolicyEvaluationStatus.COMPLETED);
    assertThat(pollingResult.getSubStatus()).isEqualTo(POLICY_EVALUATION_COMPLETE);
    assertThat(pollingResult.getReason()).isNull();
    assertThat(pollingResult.getScanReceipt()).usingRecursiveComparison()
        .isEqualTo(policyEvaluationPollingResult.getScanReceipt());

    PolicyEvaluationResult result = pollingResult.getResult();
    assertThat(result).isNotNull();
    assertThat(result.getAlerts()).isEmpty();
  }

  @Test
  public void testEvaluateWithPolling_ProxyStage_ContainerImage() throws Exception {
    PolicyEvaluationReceipt componentAnalyzeEvaluationReceipt = analyzeComponentsWithPolling();
    productLicenseManager.setFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
    PersistedPolicyEvaluationPollingResult componentAnalyzePollingResult = persistedPolicyEvaluationPollingResultDAO
        .getByApplicationIdAndStatusId(
            app.getId(),
            componentAnalyzeEvaluationReceipt.getStatusId()
        );

    assertThat(componentAnalyzeEvaluationReceipt.getStatusId()).isEqualTo(componentAnalyzePollingResult.getStatusId());

    PolicyEvaluationPollingResult policyEvaluationPollingResult =
        componentAnalyzePollingResult.getPolicyEvaluationPollingResult();
    assertThat(policyEvaluationPollingResult.getSubStatus()).isEqualTo(COMPONENT_ANALYSIS_COMPLETE);

    PolicyEvaluationReceipt receipt = policyEvaluateService.evaluateWithPolling(
        IntegrationType.CLI,
        app.getPublicId(),
        ClientScanType.SONATYPE,
        null,
        new Stage(Stage.ID_PROXY),
        componentAnalyzePollingResult.getStatusId(),
        null
    );

    PolicyEvaluationPollingResult pollingResult = policyEvaluationHelper
        .awaitEvaluationCompleted(app.getId(), receipt.getStatusId());

    assertThat(pollingResult).isNotNull();
    assertThat(pollingResult.getStatus()).isEqualTo(PolicyEvaluationStatus.COMPLETED);
    assertThat(pollingResult.getSubStatus()).isEqualTo(POLICY_EVALUATION_COMPLETE);
    assertThat(pollingResult.getReason()).isNull();
    assertThat(pollingResult.getScanReceipt()).usingRecursiveComparison()
        .isEqualTo(policyEvaluationPollingResult.getScanReceipt());

    PolicyEvaluationResult result = pollingResult.getResult();
    assertThat(result).isNotNull();
    assertThat(result.getAlerts()).isEmpty();
  }

  @Test
  public void testEvaluateWithPolling_WithReachableVulnerability_And_ComponentAnalysisNotFound() throws Exception {
    VulnerabilitySignatureAnalysisDTO analysisDTO = createTestAnalysisDTO(
        app.getId(),
        "scanId",
        createMavenCoordinates("tomcat", "tomcat-util", "5.5.23"),
        "CVE-2012-0022",
        lookup(InsightWork.class)
    );

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() ->
            policyEvaluateService.evaluateWithPolling(
                IntegrationType.CLI,
                app.getPublicId(),
                ClientScanType.SONATYPE,
                null,
                new Stage(Stage.ID_BUILD),
                "componentAnalysisStatusId",
                analysisDTO
            ))
        .withMessage("Component Analysis not found for Application ID: "
            + app.getPublicId() + " and Status ID: componentAnalysisStatusId");
  }

  @Test
  public void testEvaluateWithPolling_WithReachableVulnerability_And_ComponentAnalysisNotCompleted() throws Exception {
    PolicyEvaluationPollingResult policyEvaluationPollingResult = new PolicyEvaluationPollingResult();
    policyEvaluationPollingResult.setSubStatus(COMPONENT_ANALYSIS_PENDING);

    persistedPolicyEvaluationPollingResultDAO.insert(
        new PersistedPolicyEvaluationPollingResult(
            app.getId(),
            "componentAnalysisStatusId",
            policyEvaluationPollingResult
        )
    );

    VulnerabilitySignatureAnalysisDTO analysisDTO = createTestAnalysisDTO(
        app.getId(),
        "scanId",
        createMavenCoordinates("tomcat", "tomcat-util", "5.5.23"),
        "CVE-2012-0022",
        lookup(InsightWork.class)
    );

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() ->
            policyEvaluateService.evaluateWithPolling(
                IntegrationType.CLI,
                app.getPublicId(),
                ClientScanType.SONATYPE,
                null,
                new Stage(Stage.ID_BUILD),
                "componentAnalysisStatusId",
                analysisDTO
            ))
        .withMessage("Component analysis has not completed for public application id: " + app.getPublicId()
            + " and status ID: componentAnalysisStatusId "
            + "The current status is null and the current sub status is COMPONENT_ANALYSIS_PENDING");
  }

  @Test
  public void test_evaluateWithPolling_SendRepositoryComponentTelemetryDataForContainer_CLI() throws Exception {
    // Arrange
    productLicenseManager.setFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

    String scanId = simulateReportIsAvailable();
    Stage stage = new Stage(Stage.ID_PROXY);
    Repository repository = tempEntity.newRepository();
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(scanId);
    repository.setRepositoryType(RepositoryType.proxy);
    repository.setFormat("docker");
    repositoryDAO.update(repository);
    Organization organization = tempEntity.newOrganization();
    organization.setRelatedRepositoryId(repository.getId());
    organizationDAO.update(organization);
    Application app = tempEntity.newApplication("app", organization.getId());
    tempEntity.newPolicyEvaluation(app.getId(), stage.getStageTypeId(), scanId);

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);

    when(mockScanHandler.createTempScanFile(eq(null), any(Application.class))).thenReturn(mock(ScanEntity.class));
    doReturn(scanReceipt)
        .when(mockScanHandler)
        .handle(any(ScanHandler.ScanRequest.class));
    doNothing()
        .when(mockTelemetrySender)
        .send(telemetryDataArgumentCaptor.capture());

    // Act
    PolicyEvaluationReceipt receipt =
        policyEvaluateService.evaluateWithPolling(IntegrationType.CLI, app.getPublicId(),
            ClientScanType.SONATYPE_THIRD_PARTY, null, stage);
    PolicyEvaluationPollingResult pollingResult = policyEvaluationHelper
        .awaitEvaluationCompleted(app.getId(), receipt.getStatusId());

    List<TelemetryData> telemetryDataValues = telemetryDataArgumentCaptor.getAllValues();
    TelemetryData telemetryDataForContainer = telemetryDataValues.stream()
        .filter(telemetryData -> telemetryData.getPurpose().equals(TelemetryPurpose.REPOSITORY_COMPONENT))
        .findFirst()
        .orElse(null);

    // Assert
    verify(mockTelemetrySender, atLeastOnce()).send(any(TelemetryData.class));

    assertThat(pollingResult).isNotNull();
    assertThat(pollingResult.getStatus()).isEqualTo(PolicyEvaluationStatus.COMPLETED);

    assertThat(telemetryDataValues).isNotEmpty();
    assertThat(telemetryDataForContainer).isNotNull();
    assertThat(telemetryDataForContainer.getPurpose()).isEqualTo(TelemetryPurpose.REPOSITORY_COMPONENT);
    assertThat(telemetryDataForContainer.getAttributes())
        .containsKey(REPOSITORY_COMPONENT_TELEMETRY)
        .containsKey(POLICY_VIOLATION_TELEMETRY);
  }

  @Test
  public void test_evaluateWithPolling_SendRepositoryComponentTelemetryDataForContainer_Api() throws Exception {
    // Arrange
    productLicenseManager.setFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

    String scanId = simulateReportIsAvailable();
    Stage stage = new Stage(Stage.ID_PROXY);
    Repository repository = tempEntity.newRepository();
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(scanId);
    repository.setRepositoryType(RepositoryType.proxy);
    repository.setFormat("docker");
    repositoryDAO.update(repository);
    Organization organization = tempEntity.newOrganization();
    organization.setRelatedRepositoryId(repository.getId());
    organizationDAO.update(organization);
    Application app = tempEntity.newApplication("app", organization.getId());
    tempEntity.newPolicyEvaluation(app.getId(), stage.getStageTypeId(), scanId);

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);

    doReturn(scanReceipt)
        .when(mockScanHandler)
        .handle(any());
    doNothing()
        .when(mockTelemetrySender)
        .send(telemetryDataArgumentCaptor.capture());

    // Act
    ScanResult scanResult = new ScanResult();
    scanResult.setScanEntity(mock(ScanEntity.class));

    String scanRequestId = UUID.randomUUID().toString().replace("-", "");

    ContainerImageTelemetryMetrics containerImageTelemetryMetrics = new ContainerImageTelemetryMetrics();
    containerImageTelemetryMetrics.setBaseOs("test-os:1234");
    containerImageTelemetryMetrics.setComponentsCount(10L);
    containerImageTelemetryMetrics.setManifestMediaType("test-manifest-media-type");
    containerImageTelemetryMetrics.setScanDurationMilliseconds(10_000L);

    ScanContext scanContext = new ScanContext.Builder()
        .containerImageTelemetryMetrics(containerImageTelemetryMetrics)
        .containerImageSbomSpecification(SbomSpecification.CYCLONEDX)
        .build();

    policyEvaluateService.evaluateWithPolling(scanRequestId, app, ClientScanType.SONATYPE_THIRD_PARTY,
        new Stage(Stage.ID_PROXY), ScanTriggerType.SONATYPE_CONTAINER_IMAGE_SCANNER_API, scanResult.getScanEntity(),
        ScannerDriver.THIRD_PARTY_API.getValue(), null, null, scanContext);

    PolicyEvaluationPollingResult pollingResult = policyEvaluationHelper
        .awaitEvaluationCompleted(app.getId(), scanRequestId);

    List<TelemetryData> telemetryDataValues = telemetryDataArgumentCaptor.getAllValues();
    TelemetryData telemetryDataForContainerComponents = telemetryDataValues.stream()
        .filter(telemetryData -> telemetryData.getPurpose().equals(TelemetryPurpose.REPOSITORY_COMPONENT))
        .findFirst()
        .orElse(null);

    TelemetryData telemetryDataForContainerEvaluation = telemetryDataValues.stream()
        .filter(
            telemetryData -> telemetryData.getPurpose().equals(TelemetryPurpose.FIREWALL_CONTAINER_IMAGE_EVALUATION))
        .findFirst()
        .orElse(null);

    // Assert
    verify(mockTelemetrySender, atLeastOnce()).send(any(TelemetryData.class));

    assertThat(pollingResult).isNotNull();
    assertThat(pollingResult.getStatus()).isEqualTo(PolicyEvaluationStatus.COMPLETED);

    assertThat(telemetryDataValues).isNotEmpty();

    assertThat(telemetryDataForContainerComponents).isNotNull();
    assertThat(telemetryDataForContainerComponents.getPurpose()).isEqualTo(TelemetryPurpose.REPOSITORY_COMPONENT);
    assertThat(telemetryDataForContainerComponents.getAttributes())
        .containsKey(REPOSITORY_COMPONENT_TELEMETRY)
        .containsKey(POLICY_VIOLATION_TELEMETRY);

    assertThat(telemetryDataForContainerEvaluation).isNotNull();
    assertThat(telemetryDataForContainerEvaluation.getPurpose()).isEqualTo(
        TelemetryPurpose.FIREWALL_CONTAINER_IMAGE_EVALUATION);
    assertThat(telemetryDataForContainerEvaluation.getAttributes()).isNotNull();

    ContainerImageTelemetryMetrics telemetryMetricsFromAttributes =
        (ContainerImageTelemetryMetrics) telemetryDataForContainerEvaluation.getAttributes()
            .get("container_image_metrics");
    assertThat(telemetryMetricsFromAttributes).isNotNull();
    assertThat(telemetryMetricsFromAttributes.getBaseOs()).isEqualTo(containerImageTelemetryMetrics.getBaseOs());
    assertThat(telemetryMetricsFromAttributes.getComponentsCount()).isEqualTo(
        containerImageTelemetryMetrics.getComponentsCount());
    assertThat(telemetryMetricsFromAttributes.getManifestMediaType()).isEqualTo(
        containerImageTelemetryMetrics.getManifestMediaType());
    assertThat(telemetryMetricsFromAttributes.getScanDurationMilliseconds()).isEqualTo(
        containerImageTelemetryMetrics.getScanDurationMilliseconds());
    assertThat(telemetryMetricsFromAttributes.getPolicyEvaluationDurationMilliseconds()).isGreaterThan(0L);
  }

  private PolicyEvaluationReceipt analyzeComponentsWithPolling() throws IOException {
    HttpServletRequest httpRequest = mock(HttpServletRequest.class);

    FileScanEntity fileScanEntity = new FileScanEntity(new File("test-file.xml").toPath(), app.getId());
    doReturn(fileScanEntity)
        .when(mockScanHandler)
        .createTempScanFile(any(HttpServletRequest.class), any(Application.class));
    doReturn("test-client-user-agent")
        .when(httpRequest)
        .getHeader(CLM_CLIENT_USER_AGENT_HEADER);

    String scanId = simulateComponentAnalysisReportIsAvailable();
    ScanHelper.createDummyScanFile(lookup(InsightWork.class), app.getId(), scanId);
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(scanId);
    doReturn(scanReceipt)
        .when(mockScanHandler)
        .handle(any(ScanHandler.ScanRequest.class));

    PolicyEvaluationReceipt receipt = componentAnalysisService.analyzeComponentsWithPolling(
        IntegrationType.CLI, app.getPublicId(), ClientScanType.SONATYPE, httpRequest, new Stage(Stage.ID_BUILD));

    PersistedPolicyEvaluationPollingResult pollingResult = persistedPolicyEvaluationPollingResultDAO
        .getByApplicationIdAndStatusId(app.getId(), receipt.getStatusId());

    PolicyEvaluationPollingResult policyEvaluationPollingResult = pollingResult.getPolicyEvaluationPollingResult();
    assertThat(policyEvaluationPollingResult.getSubStatus()).isEqualTo(COMPONENT_ANALYSIS_PENDING);

    policyEvaluationHelper.awaitComponentAnalysisCompleted(app.getId(), receipt.getStatusId());

    return receipt;
  }
}
