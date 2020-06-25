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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.inject.Inject;
import javax.mail.Message;
import javax.servlet.http.HttpServletRequest;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationReceipt;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationStatus;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.MailConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.hds.ScanHandler;
import com.sonatype.insight.brain.integration.IntegrationType;
import com.sonatype.insight.brain.jira.JiraClient;
import com.sonatype.insight.brain.jira.JiraClientFactory;
import com.sonatype.insight.brain.jira.JiraConfig;
import com.sonatype.insight.brain.jira.JiraField;
import com.sonatype.insight.brain.jira.JiraIssueCreateRequest;
import com.sonatype.insight.brain.jira.JiraIssueCreateRequest.JiraIssueCreateResponse;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.InvalidStageException;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.notifications.JiraNotification;
import com.sonatype.insight.brain.model.policy.notifications.Notification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.report.MockReportDownloader;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportDownloader;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.mock_javamail.Mailbox;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.internal.stubbing.answers.CallsRealMethods;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static com.sonatype.insight.brain.Assert.assertNotifications;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
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

  private PolicyDAO policyDAO = new PolicyDAO();

  private Application app;

  private JiraClientFactory mockJiraClientFactory;

  private MockReportDownloader mockReportDownloader;

  @Mock
  private ScanHandler mockScanHandler;

  private TelemetrySender mockTelemetrySender;

  @Override
  public void configure(Binder binder) {
    mockReportDownloader = new MockReportDownloader();
    binder.bind(ReportDownloader.class).toInstance(mockReportDownloader.getMock());
    mockJiraClientFactory = mock(JiraClientFactory.class);
    binder.bind(JiraClientFactory.class).toInstance(mockJiraClientFactory);
    mockTelemetrySender = mock(TelemetrySender.class);
    binder.bind(TelemetrySender.class).toInstance(mockTelemetrySender);
    mockScanHandler = mock(ScanHandler.class);
    binder.bind(ScanHandler.class).toInstance(mockScanHandler);

    super.configure(binder);
  }

  @Before
  public void before() throws Exception {
    app = tempEntity.newApplicationWithParent();

    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setHostname("127.0.0.1");
    mailConfiguration.setPort(587);
    mailConfiguration.setSystemEmail("NexusIQServer@localhost");
    new MailConfigurationDAO().set(mailConfiguration);
  }

  private void assertPolicyEvaluation(String applicationId, String scanId, boolean isReevaluation) {
    assertPolicyEvaluation(applicationId, scanId, isReevaluation, false /* isForObsoleteScan */);
  }

  private void assertPolicyEvaluation(String applicationId,
                                      String scanId,
                                      boolean isReevaluation,
                                      boolean isForObsoleteScan)
  {
    PolicyEvaluation policyEvaluation = new PolicyEvaluationDAO()
        .getLastByApplicationIdAndScanId(applicationId, scanId);
    assertThat(policyEvaluation.isReevaluation()).isEqualTo(isReevaluation);
    assertThat(policyEvaluation.isForObsoleteScan()).isEqualTo(isForObsoleteScan);
  }

  @Test
  public void testEvaluate() throws Exception {
    InsightConfig insightConfig = lookup(InsightConfig.class);
    insightConfig.setBaseUrl("http://localhost");
    insightConfig.setJiraConfig(new JiraConfig());
    JiraClient mockJiraClient = mock(JiraClient.class);
    when(mockJiraClientFactory.create()).thenReturn(mockJiraClient);
    JiraIssueCreateResponse createResponse = new JiraIssueCreateResponse();
    when(mockJiraClient.createIssue(any(JiraIssueCreateRequest.class))).thenReturn(createResponse);

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

    ApplicationComponentDAO appComponentDAO = new ApplicationComponentDAO();
    assertThat(appComponentDAO.getByApplicationIdAndStageTypeId(app.getId(), stage.getStageTypeId())).isEmpty();

    // evaluate policy
    PolicyEvaluationResult policyEvaluationResult = policyEvaluateService.evaluate(app.getPublicId(), scanId, stage);
    assertEvaluate(scanId, stage, policyEvaluationResult, policy1, mockJiraClient, appComponentDAO, mailA, mailB);
  }

  @Test
  public void testEvaluate_PolicyThreatLevelCounts() throws Exception {
    Policy policy = tempEntity.newPolicy(app, 1, LogicalOperator.AND,
        new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));

    final Stage stage = new Stage(Stage.ID_BUILD);

    String scanId = simulateReportIsAvailable();

    PolicyEvaluationResult policyEvaluationResult = policyEvaluateService.evaluate(app.getPublicId(), scanId, stage);

    // Threat Level 1 Should not show up in any counts
    assertThat(policyEvaluationResult.getAffectedComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getCriticalComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getSevereComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getModerateComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getCriticalPolicyViolationCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getSeverePolicyViolationCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getModeratePolicyViolationCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getGrandfatheredPolicyViolationCount()).isEqualTo(0);

    policy.setThreatLevel(2);
    policyDAO.update(policy);
    scanId = simulateReportIsAvailable();

    // Threat Level 2 should show up as moderate
    policyEvaluationResult = policyEvaluateService.evaluate(app.getPublicId(), scanId, stage);
    assertThat(policyEvaluationResult.getAffectedComponentCount()).isEqualTo(7);
    assertThat(policyEvaluationResult.getCriticalComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getSevereComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getModerateComponentCount()).isEqualTo(7);
    assertThat(policyEvaluationResult.getCriticalPolicyViolationCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getSeverePolicyViolationCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getModeratePolicyViolationCount()).isEqualTo(36);
    assertThat(policyEvaluationResult.getGrandfatheredPolicyViolationCount()).isEqualTo(0);

    policy.setThreatLevel(4);
    policyDAO.update(policy);
    scanId = simulateReportIsAvailable();

    // Threat Level 4 should show up as severe
    policyEvaluationResult = policyEvaluateService.evaluate(app.getPublicId(), scanId, stage);
    assertThat(policyEvaluationResult.getAffectedComponentCount()).isEqualTo(7);
    assertThat(policyEvaluationResult.getCriticalComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getSevereComponentCount()).isEqualTo(7);
    assertThat(policyEvaluationResult.getModerateComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getCriticalPolicyViolationCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getSeverePolicyViolationCount()).isEqualTo(36);
    assertThat(policyEvaluationResult.getModeratePolicyViolationCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getGrandfatheredPolicyViolationCount()).isEqualTo(0);

    policy.setThreatLevel(8);
    policyDAO.update(policy);
    scanId = simulateReportIsAvailable();

    // Threat Level 8 should show up as severe
    policyEvaluationResult = policyEvaluateService.evaluate(app.getPublicId(), scanId, stage);
    assertThat(policyEvaluationResult.getAffectedComponentCount()).isEqualTo(7);
    assertThat(policyEvaluationResult.getCriticalComponentCount()).isEqualTo(7);
    assertThat(policyEvaluationResult.getSevereComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getModerateComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getCriticalPolicyViolationCount()).isEqualTo(36);
    assertThat(policyEvaluationResult.getSeverePolicyViolationCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getModeratePolicyViolationCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getGrandfatheredPolicyViolationCount()).isEqualTo(0);

    // Grandfather one violation
    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();
    PolicyViolation policyViolation = policyViolationDAO
        .getActiveByApplicationIdAndStageId(app.getId(), stage.getStageTypeId()).get(0);
    policyViolation.setGrandfatherTime(new Date());
    policyViolationDAO.update(policyViolation);
    scanId = simulateReportIsAvailable();
    policyEvaluationResult = policyEvaluateService.evaluate(app.getPublicId(), scanId, stage);
    assertThat(policyEvaluationResult.getAffectedComponentCount()).isEqualTo(7);
    assertThat(policyEvaluationResult.getCriticalComponentCount()).isEqualTo(7);
    assertThat(policyEvaluationResult.getSevereComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getModerateComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getCriticalPolicyViolationCount()).isEqualTo(35);
    assertThat(policyEvaluationResult.getSeverePolicyViolationCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getModeratePolicyViolationCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getGrandfatheredPolicyViolationCount()).isEqualTo(1);
  }

  @Test
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

    PolicyEvaluationResult policyEvaluationResult = policyEvaluateService.evaluate(app.getPublicId(), scanId, stage);

    List<PolicyFact> policyFacts = new ArrayList<>();
    for (PolicyAlert policyAlert : policyEvaluationResult.getAlerts()) {
      policyFacts.add(policyAlert.getTrigger());
    }

    app.setContactInternalName(User.ADMIN_USERNAME);
    new ApplicationDAO().update(app);

    PolicyAlertEmailer emailer = lookup(PolicyAlertEmailer.class);

    String serverUrl = "http://localhost/";
    lookup(InsightConfig.class).setBaseUrl(serverUrl);
    Map<String, Object> model = emailer.createPolicyMailModel(app, scanId, StageTypes.BUILD, policyFacts, 8);
    assertThat(model.get("policyFacts")).isEqualTo(policyFacts);
    assertThat(model.get("cdnUrl")).isEqualTo("https://cdn.sonatype.com/");
    assertThat(model.get("detailedReportUrl"))
        .isEqualTo(serverUrl + UserInterfaceLinksResource.getReportUrl(app.getPublicId(), scanId));
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
    assertThat(model.get("grandfatheredPolicyViolationCount")).isEqualTo(8);
  }

  @Test
  public void testEvaluate_ReEvaluateNotifications() throws Exception {
    Policy policy = tempEntity.newPolicy(app, 8, LogicalOperator.AND,
        new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    addNotificationsToPolicy(policy, Stage.ID_BUILD, new UserNotification("manager@test.corp", Stage.ID_BUILD));

    Stage stage = new Stage(Stage.ID_BUILD);

    InsightConfig insightConfig = lookup(InsightConfig.class);
    insightConfig.setBaseUrl("http://localhost");

    List<Message> notifications = Mailbox.get("manager@test.corp");
    notifications.clear();

    // Evaluate policy
    String scanId = simulateReportIsAvailable();
    PolicyEvaluationResult policyEvaluationResult = policyEvaluateService.evaluate(app.getPublicId(), scanId, stage);

    List<PolicyAlert> policyAlerts = policyEvaluationResult.getAlerts();
    assertThat(policyAlerts).hasSize(36);
    assertPolicyEvaluation(app.getId(), scanId, false /* isReevaluation */);

    // Notification message should have been sent
    assertNotifications(notifications, 1, 5000);
    notifications.clear();

    // Change the policy name
    policy.setName(policy.getName() + "Updated");
    policyDAO.update(policy);

    // Evaluate policy again for the same scan
    policyEvaluationResult = policyEvaluateService.evaluate(app.getPublicId(), scanId, stage);
    policyAlerts = policyEvaluationResult.getAlerts();
    assertThat(policyAlerts).hasSize(36);
    assertPolicyEvaluation(app.getId(), scanId, true /* isReevaluation */);

    // Notification message should not have been sent since this is a re-evaluation
    assertNotifications(notifications, 0, 5000);
  }

  @Test
  public void testEvaluateWithPolling_CI() throws Exception {
    testEvaluateWithPolling(LicensedFeature.CI_INTEGRATION, IntegrationType.CI);
  }

  @Test
  public void testEvaluateWithPolling_CLI() throws Exception {
    testEvaluateWithPolling(LicensedFeature.CLI_INTEGRATION, IntegrationType.CLI);
  }

  @Test
  public void testEvaluateWithPolling_RepoManager() throws Exception {
    testEvaluateWithPolling(LicensedFeature.RM_STAGING_INTEGRATION, IntegrationType.RM);
  }

  private void testEvaluateWithPolling(
      LicensedFeature requiredFeature,
      IntegrationType integrationType)
      throws Exception
  {
    productLicenseManager.setFeatures(requiredFeature, LicensedFeature.NOTIFICATIONS);
    InsightConfig insightConfig = lookup(InsightConfig.class);
    insightConfig.setBaseUrl("http://localhost");
    insightConfig.setJiraConfig(new JiraConfig());
    JiraClient mockJiraClient = mock(JiraClient.class);
    when(mockJiraClientFactory.create()).thenReturn(mockJiraClient);
    JiraIssueCreateResponse createResponse = new JiraIssueCreateResponse();
    when(mockJiraClient.createIssue(any(JiraIssueCreateRequest.class))).thenReturn(createResponse);

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

    ApplicationComponentDAO appComponentDAO = new ApplicationComponentDAO();
    assertThat(appComponentDAO.getByApplicationIdAndStageTypeId(app.getId(), stage.getStageTypeId())).isEmpty();

    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(scanId);

    when(mockScanHandler.createTempScanFile(eq(null), any(Application.class), eq(ClientScanType.SONATYPE)))
        .thenReturn(mock(File.class));
    when(mockScanHandler
        .handle(any(File.class), any(Application.class), eq(ClientScanType.SONATYPE), any(TelemetryData.class),
            anyString()))
        .thenReturn(scanReceipt);

    // evaluate policy
    PolicyEvaluationReceipt policyEvaluationReceipt = policyEvaluateService
        .evaluateWithPolling(integrationType, app.getPublicId(), ClientScanType.SONATYPE, null, stage);

    PolicyEvaluationPollingResult policyEvaluationPollingResult =
        waitForResult(app.getPublicId(), policyEvaluationReceipt.getStatusId(),
            p ->  p.getStatus().equals(PolicyEvaluationStatus.COMPLETED));

    PolicyEvaluationResult policyEvaluationResult = policyEvaluationPollingResult.getResult();
    assertThat(policyEvaluationPollingResult.getStatus()).isEqualTo(PolicyEvaluationStatus.COMPLETED);
    assertThat(policyEvaluationPollingResult.getReason()).isNull();
    assertThat(policyEvaluationPollingResult.getResult()).isNotNull();
    assertThat(policyEvaluationPollingResult.getScanReceipt()).isEqualTo(scanReceipt);

    assertEvaluate(scanId, stage, policyEvaluationResult, policy1, mockJiraClient, appComponentDAO, mailA, mailB);
  }

  @Test
  public void testEvaluateWithPolling_sendThirdPartyScanUsageTelemetry() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);

    String scanId = simulateReportIsAvailable();

    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(scanId);
    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    when(mockScanHandler.createTempScanFile(any(HttpServletRequest.class), any(Application.class),
        eq(ClientScanType.SONATYPE_THIRD_PARTY))).thenReturn(mock(File.class));
    when(mockScanHandler.handle(any(File.class), any(Application.class), eq(ClientScanType.SONATYPE_THIRD_PARTY),
        telemetryDataArgumentCaptor.capture(), anyString()))
        .thenReturn(scanReceipt);

    HttpServletRequest req = mock(HttpServletRequest.class);
    when(req.getHeader(HdsClient.CLM_CLIENT_USER_AGENT_HEADER)).thenReturn("userAgent");

    PolicyEvaluationReceipt policyEvaluationReceipt = policyEvaluateService
        .evaluateWithPolling(IntegrationType.CLI, app.getPublicId(), ClientScanType.SONATYPE_THIRD_PARTY, req, stage);

    waitForResult(app.getPublicId(), policyEvaluationReceipt.getStatusId(),
        p -> p.getStatus().equals(PolicyEvaluationStatus.COMPLETED));

    TelemetryData telemetryData = telemetryDataArgumentCaptor.getAllValues().get(0);
    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.THIRD_PARTY_SCAN_USAGE);

    Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put("application_id", app.getPublicId());
    expectedAttributes.put("stage_id", stage.getStageTypeId());
    expectedAttributes.put("source", IntegrationType.CLI.toString());
    expectedAttributes.put("user_agent", "userAgent");
    assertThat(telemetryData.getAttributes()).isEqualTo(expectedAttributes);
  }

  @Test
  public void testEvaluateWithPolling_Pending_StatusId() throws Exception {
    InsightConfig insightConfig = lookup(InsightConfig.class);
    insightConfig.setBaseUrl("http://localhost");

    final Stage stage = new Stage(Stage.ID_BUILD);

    final String scanId = "scanId1";

    ApplicationComponentDAO appComponentDAO = new ApplicationComponentDAO();
    assertThat(appComponentDAO.getByApplicationIdAndStageTypeId(app.getId(), stage.getStageTypeId())).isEmpty();

    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(scanId);

    when(mockScanHandler.createTempScanFile(eq(null), any(Application.class), eq(ClientScanType.SONATYPE)))
        .thenReturn(mock(File.class));

    when(mockScanHandler
        .handle(any(File.class), any(Application.class), eq(ClientScanType.SONATYPE), any(TelemetryData.class),
            anyString()))
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
    }).when(spyService).evaluate(any(Application.class), anyString(), any(Stage.class));

    // evaluate policy
    PolicyEvaluationReceipt policyEvaluationReceipt =
        spyService.evaluateWithPolling(IntegrationType.CLI, app.getPublicId(), ClientScanType.SONATYPE, null, stage);

    PolicyEvaluationPollingResult policyEvaluationPollingResult =
        waitForResult(app.getPublicId(), policyEvaluationReceipt.getStatusId(),
            p -> p.getStatus().equals(PolicyEvaluationStatus.PENDING) && p.getScanReceipt() != null);

    assertThat(policyEvaluationPollingResult.getStatus()).isEqualTo(PolicyEvaluationStatus.PENDING);
    assertThat(policyEvaluationPollingResult.getReason()).isNull();
    assertThat(policyEvaluationPollingResult.getResult()).isNull();
    assertThat(policyEvaluationPollingResult.getScanReceipt()).isEqualTo(scanReceipt);
  }

  private PolicyEvaluationPollingResult waitForResult(String appId, String scanId,
                                                      Function<PolicyEvaluationPollingResult, Boolean> readyTest)
      throws Exception
  {
    long endTime = System.currentTimeMillis() + 20000;
    PolicyEvaluationPollingResult result;
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

    PolicyEvaluateService policyEvaluationServiceSpy = spy(policyEvaluateService);

    CountDownLatch countDownLatch = new CountDownLatch(1);
    lenient().doAnswer((Answer<PolicyEvaluationResult>) invocationOnSpy -> {
      countDownLatch.await(1, TimeUnit.MINUTES);
      return null;
    }).when(policyEvaluationServiceSpy).evaluate(any(Application.class), any(String.class), any(Stage.class));

    Application app = tempEntity.newApplicationWithParent();
    PolicyEvaluationReceipt receipt = policyEvaluationServiceSpy
        .evaluateWithPolling(IntegrationType.CLI, app.getPublicId(), ClientScanType.SONATYPE, null,
            new Stage(Stage.ID_BUILD));

    PolicyEvaluationPollingResult policyEvaluationPollingResult =
        policyEvaluateService.pollEvaluationResult(app.getPublicId(), receipt.getStatusId());
    assertThat(policyEvaluationPollingResult).isNotNull();
    assertThat(policyEvaluationPollingResult.getStatus()).isEqualTo(PolicyEvaluationStatus.PENDING);
    assertThat(policyEvaluationPollingResult.getReason()).isNull();
    assertThat(policyEvaluationPollingResult.getResult()).isNull();
    assertThat(policyEvaluationPollingResult.getScanReceipt()).isNull();
    assertThat(policyEvaluationPollingResult.getNextPollingIntervalInSeconds()).isEqualTo(5);
  }

  @Test
  public void testEvaluateWithPolling_PollEvaluationResult_Failure() throws Exception {
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scanid");

    Application app = tempEntity.newApplicationWithParent();

    doThrow(new IOException("HDS Upload Scan Failure!!!"))
        .when(mockScanHandler)
        .handle(any(File.class), any(Application.class), any(ClientScanType.class), any(TelemetryData.class),
            anyString());

    PolicyEvaluationReceipt receipt = policyEvaluateService
        .evaluateWithPolling(IntegrationType.CLI, app.getPublicId(), ClientScanType.SONATYPE, null,
            new Stage(Stage.ID_BUILD));

    PolicyEvaluationPollingResult policyEvaluationPollingResult =
        waitForResult(app.getPublicId(), receipt.getStatusId(),
            p ->  !p.getStatus().equals(PolicyEvaluationStatus.PENDING));

    assertThat(policyEvaluationPollingResult).isNotNull();
    assertThat(policyEvaluationPollingResult.getStatus()).isEqualTo(PolicyEvaluationStatus.FAILED);
    assertThat(policyEvaluationPollingResult.getReason()).startsWith("Internal Server Error");
    assertThat(policyEvaluationPollingResult.getResult()).isNull();
    assertThat(policyEvaluationPollingResult.getScanReceipt()).isNull();
    assertThat(policyEvaluationPollingResult.getNextPollingIntervalInSeconds()).isEqualTo(5);
  }

  @Test
  public void testEvaluateWithPolling_PollEvaluationResult_Success() throws Exception {
    String scanId = simulateReportIsAvailable();
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, scanId);

    Application app = tempEntity.newApplicationWithParent();

    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(scanId);

    when(mockScanHandler.createTempScanFile(eq(null), any(Application.class), eq(ClientScanType.SONATYPE)))
        .thenReturn(mock(File.class));
    when(mockScanHandler
        .handle(any(File.class), any(Application.class), eq(ClientScanType.SONATYPE), any(TelemetryData.class),
            anyString()))
        .thenReturn(scanReceipt);

    PolicyEvaluationReceipt receipt = policyEvaluateService
        .evaluateWithPolling(IntegrationType.CLI, app.getPublicId(), ClientScanType.SONATYPE, null,
            new Stage(Stage.ID_BUILD));

    PolicyEvaluationPollingResult policyEvaluationPollingResult =
        waitForResult(app.getPublicId(), receipt.getStatusId(),
            p -> p.getStatus().equals(PolicyEvaluationStatus.COMPLETED));

    assertThat(policyEvaluationPollingResult).isNotNull();
    assertThat(policyEvaluationPollingResult.getStatus()).isEqualTo(PolicyEvaluationStatus.COMPLETED);
    assertThat(policyEvaluationPollingResult.getReason()).isNull();
    assertThat(policyEvaluationPollingResult.getResult()).isNotNull();
    assertThat(policyEvaluationPollingResult.getScanReceipt()).isEqualTo(scanReceipt);
  }

  @Test
  public void testEvaluateWithPolling_InvalidStage() {
    assertThatExceptionOfType(InvalidStageException.class).isThrownBy(() -> {
      policyEvaluateService.evaluateWithPolling(IntegrationType.CLI, app.getPublicId(), ClientScanType.SONATYPE, null,
          new Stage("invalidStage"));
    }).withMessage("Invalid stage id=invalidStage");
  }

  @Test
  public void testEvaluateWithPolling_StageNotLicensed() {
    productLicenseManager.setStageTypes(StageTypes.RELEASE);

    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() -> {
      policyEvaluateService.evaluateWithPolling(IntegrationType.CLI, app.getPublicId(), ClientScanType.SONATYPE, null,
          new Stage("build"));
    }).withMessage("Stage 'build' is not supported by your license.");
  }

  @Test
  public void testEvaluateWithPolling_FailsWithoutFeature() {
    productLicenseManager.setFeatures();

    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() -> {
      policyEvaluateService.evaluateWithPolling(IntegrationType.CI, app.getPublicId(), ClientScanType.SONATYPE, null,
          new Stage(Stage.ID_BUILD));
    }).withMessage("Your IQ Server license does not enable this feature.");

    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() -> {
      policyEvaluateService.evaluateWithPolling(IntegrationType.CLI, app.getPublicId(), ClientScanType.SONATYPE, null,
          new Stage(Stage.ID_BUILD));
    }).withMessage("Your IQ Server license does not enable this feature.");

    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() -> {
      policyEvaluateService.evaluateWithPolling(IntegrationType.RM, app.getPublicId(), ClientScanType.SONATYPE, null,
          new Stage(Stage.ID_BUILD));
    }).withMessage("Your IQ Server license does not enable this feature.");
  }

  @Test
  public void testEvaluateWithPolling_AppPublicIdCaseInsensitive() throws Exception {
    Application app = tempEntity.newApplicationWithParent("THE-public-ID");
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(simulateReportIsAvailable());

    when(mockScanHandler.createTempScanFile(eq(null), any(Application.class), eq(ClientScanType.SONATYPE)))
        .thenReturn(mock(File.class));
    when(mockScanHandler
        .handle(any(File.class), any(Application.class), eq(ClientScanType.SONATYPE), any(TelemetryData.class),
            anyString()))
        .thenReturn(scanReceipt);

    PolicyEvaluationReceipt receipt = policyEvaluateService.evaluateWithPolling(IntegrationType.CLI,
        app.getPublicId().toLowerCase(Locale.ENGLISH), ClientScanType.SONATYPE, null, new Stage(Stage.ID_BUILD));

    PolicyEvaluationPollingResult policyEvaluationPollingResult =
        waitForResult(app.getPublicId().toUpperCase(Locale.ENGLISH), receipt.getStatusId(),
            p -> !p.getStatus().equals(PolicyEvaluationStatus.PENDING));

    assertThat(policyEvaluationPollingResult).isNotNull();
    assertThat(policyEvaluationPollingResult.getStatus()).isEqualTo(PolicyEvaluationStatus.COMPLETED);
    assertThat(policyEvaluationPollingResult.getReason()).isNull();
    assertThat(policyEvaluationPollingResult.getResult()).isNotNull();
    assertThat(policyEvaluationPollingResult.getScanReceipt()).isEqualTo(scanReceipt);
  }

  /**
   * Simulates that a report (based on the specified resource) exists.
   *
   * @return A generated scan ID that can be used in subsequent calls to evaluate policies.
   */
  private String simulateReportIsAvailable() {
    return mockReportDownloader.mockDownloadReport("/" + getClass().getSimpleName() + "/report");
  }

  private void addNotificationsToPolicy(Policy policy, String stageId, Notification... notifications) {
    Arrays.stream(notifications).forEach(policy.getNotifications()::add);
    policy.getActions().clear();
    policy.setAction(stageId, Action.ID_FAIL);
    policyDAO.update(policy);
  }

  private void assertEvaluate(String scanId,
                              Stage stage,
                              PolicyEvaluationResult policyEvaluationResult,
                              Policy policy1,
                              JiraClient mockJiraClient,
                              ApplicationComponentDAO appComponentDAO,
                              String mailboxA,
                              String mailboxB) throws Exception
  {
    final List<Message> messagesA = Mailbox.get(mailboxA);
    final List<Message> messagesB = Mailbox.get(mailboxB);

    assertThat(policyEvaluationResult.getAffectedComponentCount()).isEqualTo(7);
    assertThat(policyEvaluationResult.getCriticalComponentCount()).isEqualTo(7);
    assertThat(policyEvaluationResult.getSevereComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getModerateComponentCount()).isEqualTo(0);
    List<PolicyAlert> policyAlerts = policyEvaluationResult.getAlerts();
    assertThat(policyAlerts).hasSize(72);
    for (PolicyAlert policyAlert : policyAlerts) {
      AbstractPolicyEvaluationTest.assertFactCounts(1, 1, policyAlert);
    }
    assertPolicyEvaluation(app.getId(), scanId, false /* isReevaluation */);
    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();
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
    InsightWork insightWork = lookup(InsightWork.class);
    File reportFile = insightWork.getReportFile(app.getId(), scanId);
    ReportEntry policyThreatsReportEntry = Report.getEntry(reportFile, ScanPolicyEvaluator.POLICY_THREATS_FILENAME);
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
    verify(mockJiraClient, timeout(5000)).createIssue(createRequestArgumentCaptor.capture());
    JiraIssueCreateRequest jiraIssueCreateRequest = createRequestArgumentCaptor.getValue();
    assertThat(jiraIssueCreateRequest.getFields()).hasSize(4);
    Map<String, String> projectMeta = jiraIssueCreateRequest.getField(JiraField.PROJECT);
    assertThat(projectMeta).containsEntry("key", "projectKey1");

    messagesA.clear();
    messagesB.clear();

    reset(mockJiraClient);

    // evaluate policy again
    policyEvaluationResult = policyEvaluateService.evaluate(app.getPublicId(), scanId, stage);
    policyAlerts = policyEvaluationResult.getAlerts();
    assertThat(policyAlerts).hasSize(72);
    for (PolicyAlert policyAlert : policyAlerts) {
      AbstractPolicyEvaluationTest.assertFactCounts(1, 1, policyAlert);
    }
    assertPolicyEvaluation(app.getId(), scanId, true /* isReevaluation */);
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

    verify(mockJiraClient, times(0)).createIssue(any(JiraIssueCreateRequest.class));

    messagesA.clear();
    messagesB.clear();
  }
}
