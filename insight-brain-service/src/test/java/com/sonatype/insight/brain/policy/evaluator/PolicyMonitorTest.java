/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import static com.sonatype.insight.brain.Assert.assertNotifications;
import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.POLICY_ALERTS;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.THIRD_PARTY_BOM_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.THIRD_PARTY_LICENSE_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.THIRD_PARTY_SECURITY_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.MailConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.notifications.Notification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.policy.PolicyResource;
import com.sonatype.insight.brain.policy.evaluator.queue.EvaluationQueueConfig;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceService;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.test.MailboxTestUtil;
import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;
import com.sonatype.insight.brain.webhook.ApplicationEvaluationEvent;
import com.sonatype.insight.brain.webhook.TestEventHandler;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.test.LogOutput;
import jakarta.mail.Message;
import jakarta.ws.rs.InternalServerErrorException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

@Category(SlowTest.class)
public class PolicyMonitorTest
    extends AbstractBrainServiceIntegrationTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(PolicyMonitor.class);

  private PolicyMonitor policyMonitor;

  private InsightWork insightWork;

  private AsyncEventBus asyncEventBus;

  private TestEventHandler<ApplicationEvaluationEvent> handler;

  private PolicyEvaluationDAO policyEvaluationDAO;

  private PolicyViolationDAO policyViolationDAO;

  private OwnerDAO ownerDAO;

  private static final TelemetrySender mockTelemetrySender = mock(TelemetrySender.class);

  private static final ShutdownHandler mockShutdownHandler = mock(ShutdownHandler.class);

  private static final MailConfiguration testMailConfiguration = createTestMailConfiguration();

  private static MailConfiguration createTestMailConfiguration() {
    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setHostname("127.0.0.1");
    mailConfiguration.setPort(587);
    mailConfiguration.setSystemEmail("NexusIQServer@localhost");
    return mailConfiguration;
  }

  @Before
  public void setup() {
    setBaseUrl("http://clm.sonatype.com/test");
    insightWork = getCLMServer().getInstance(InsightWork.class);
    policyMonitor = getCLMServer().getInstance(PolicyMonitor.class);
    overrideField(policyMonitor, "shutdownHandler", mockShutdownHandler);
    overrideField(policyMonitor, "telemetrySender", mockTelemetrySender);
    asyncEventBus = getCLMServer().getInstance(AsyncEventBus.class);
    policyEvaluationDAO = getCLMServer().getInstance(PolicyEvaluationDAO.class);
    policyViolationDAO = getCLMServer().getInstance(PolicyViolationDAO.class);
    ownerDAO = getCLMServer().getInstance(OwnerDAO.class);

    MailConfigurationDAO mailConfigurationDAO = getCLMServer().getInstance(MailConfigurationDAO.class);
    mailConfigurationDAO.set(testMailConfiguration);
  }

  @After
  public void cleanup() {
    if (handler != null) {
      asyncEventBus.unregister(handler);
    }
    Mockito.reset(mockTelemetrySender, mockShutdownHandler);
  }

  @Test
  public void testApplicationNotMonitoredWhenUnlicensed() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication("MonitoredApp", org.getId());
    Stage stage = new Stage(ReleaseStageType.ID);

    tempEntity.newPolicyMonitoring(app.getId(), stage.getStageTypeId());

    String scanId1 = "PolicyMonitorTest_scanId";

    createScanFile(app, scanId1);

    // Simulate that the report is available
    mockScanReceiptAndReport(scanId1);

    evaluatePolicy(app.getPublicId(), scanId1, stage);

    setMissingFeature(LicensedFeature.POLICY_MONITORING);

    Collection<StageType> stageTypes = StageTypes.getAll();

    Map<StageType, Date> lastRun = new HashMap<>();
    for (StageType stageType : stageTypes) {
      PolicyEvaluation eval = policyEvaluationDAO.getLastByApplicationIdAndStageId(app.getId(), stageType.getId());
      lastRun.put(stageType, eval == null ? null : eval.getTime());
    }

    String scanId2 = "PolicyMonitorTest_scanId2";
    mockScanReceiptAndReport(scanId2);
    policyMonitor.run();

    // There should be no new policy evaluations
    for (StageType stageType : stageTypes) {
      PolicyEvaluation eval = policyEvaluationDAO.getLastByApplicationIdAndStageId(app.getId(), stageType.getId());
      Date val = lastRun.get(stageType);
      if (val == null) {
        assertThat(eval).isNull();
      }
      else {
        assertThat(eval).isNotNull();
        assertThat(eval.getTime()).isEqualTo(val);
      }
    }
    assertShutdownHandler();
  }

  @Test
  public void testApplicationNotMonitored() {
    Organization org = tempEntity.newOrganization();
    // Create a monitored app only because the policy monitoring exits fast if nothing is monitored.
    Application monitoredApp = tempEntity.newApplication("MonitoredApp", org.getId());
    tempEntity.newPolicyMonitoring(monitoredApp.getId(), ReleaseStageType.ID);

    Application notMonitoredApp = tempEntity.newApplication("NotMonitoredApp", org.getId());
    // Seed policy evaluations for all stages. These should be the last evaluations after we run the policy monitoring,
    // i.e. no re-evaluations happened.
    Map<StageType, PolicyEvaluation> policyEvaluations = new LinkedHashMap<>();
    for (StageType stageType : StageTypes.getAll()) {
      PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(notMonitoredApp.getId(), stageType.getId(),
          "fakeScanId" + stageType.getId());
      policyEvaluations.put(stageType, policyEvaluation);
    }

    policyMonitor.run();

    // There should be no new policy evaluations
    for (StageType stageType : StageTypes.getAll()) {
      assertThat(
          policyEvaluationDAO.getLastByApplicationIdAndStageId(notMonitoredApp.getId(), stageType.getId()).getTime())
              .isEqualTo(policyEvaluations.get(stageType).getTime());
    }
    assertShutdownHandler();
  }

  @Test
  public void testApplicationMonitored_NoScan() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication("MonitoredApp", org.getId());
    tempEntity.newPolicyMonitoring(app.getId(), ReleaseStageType.ID);

    policyMonitor.run();

    // There should be no policy evaluations
    for (StageType stageType : StageTypes.getAll()) {
      assertThat(policyEvaluationDAO.getLastByApplicationIdAndStageId(app.getId(), stageType.getId())).isNull();
    }
    assertShutdownHandler();
  }

  @Test
  public void testApplicationMonitored() throws Exception {
    testMonitored(OwnerType.APPLICATION);
  }

  @Test
  public void testOrganizationMonitored() throws Exception {
    testMonitored(OwnerType.ORGANIZATION);
  }

  @Test
  public void testRootOrganizationMonitored() throws Exception {
    testMonitored(OwnerType.GLOBAL);
  }

  @Test
  public void testRun_NoShiroSubjectEmitsApplicationEvaluationEvent() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication("MonitoredApp", org.getId());
    Stage stage = new Stage(ReleaseStageType.ID);

    tempEntity.newPolicyMonitoring(app.getId(), stage.getStageTypeId());

    String scanId = "PolicyMonitorTest_scanId";
    createScanFile(app, scanId);

    // Simulate that the report is available
    mockScanReceiptAndReport(scanId);

    evaluatePolicy(app.getPublicId(), scanId, stage);

    handler = new TestEventHandler<>(new CountDownLatch(1), ApplicationEvaluationEvent.class);
    asyncEventBus.register(handler);

    scanId = "PolicyMonitorTest_scanId1";
    mockScanReceiptAndReport(scanId);
    policyMonitor.run();

    assertThat(handler.getLatch().await(1, TimeUnit.SECONDS)).isTrue();
    ApplicationEvaluationEvent event = handler.getEvent();
    assertThat(event).isNotNull();
    // When no authenticated user is present, initiator should be either:
    // - "system" (no SecurityManager in ThreadContext - production Quartz threads)
    // - "anonymous" (SecurityManager present but no authenticated principal - integration test environment)
    // Both indicate no real user initiated this action, which is the intent of this test.
    assertThat(event.initiator).isIn(CurrentUser.SYSTEM, CurrentUser.ANONYMOUS);

    assertShutdownHandler();
  }

  @Test
  public void testPolicyMonitorThreads() {
    Application application = tempEntity.newApplicationWithParent();
    String scanId = "scanId";
    createScanFile(application, scanId);
    mockScanReceiptAndReport(scanId);
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(Organization.ROOT_ORGANIZATION_ID, BuildStageType.ID);
    tempEntity.newPolicyMonitoring(policyMonitoring);
    doThrow(new RuntimeException("Something went wrong")).when(mockTelemetrySender).send(any(TelemetryData.class));

    PolicyMonitor policyMonitor = getCLMServer().getInstance(PolicyMonitor.class);
    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(policyMonitor::run)
        .withMessageContaining("Something went wrong");
    assertThat(policyMonitor.getExecutorService().isShutdown()).isTrue();
  }

  private void testMonitored(OwnerType monitorOwnerType) throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication("MonitoredApp", org.getId());
    Owner parentOrg = ownerDAO.getParentOwner(org);

    Stage stage = new Stage(ReleaseStageType.ID);

    PolicyMonitoring policyMonitoring;
    switch (monitorOwnerType) {
      case APPLICATION:
        policyMonitoring = new PolicyMonitoring(app.getId(), stage.getStageTypeId());
        break;
      case ORGANIZATION:
        policyMonitoring = new PolicyMonitoring(org.getId(), stage.getStageTypeId());
        break;
      case GLOBAL:
        policyMonitoring = new PolicyMonitoring(parentOrg.getId(), stage.getStageTypeId());
        break;
      default:
        throw new IllegalArgumentException("Unknown OwnerType " + monitorOwnerType);
    }
    tempEntity.newPolicyMonitoring(policyMonitoring);

    String scanId1 = "PolicyMonitorTest_scanId1";

    String notifyEmail = "developer@sonatype.com";
    String monitorNotifyEmail1 = "monitor1@sonatype.com";
    String monitorNotifyEmail2 = "monitor2@sonatype.com";
    String monitorNotifyEmail3 = "monitor3@sonatype.com";
    Policy policy1 = createPolicy(app.getId(), "Policy1", stage, notifyEmail, monitorNotifyEmail1);
    Policy policy2 = createPolicy(org.getId(), "Policy2", stage, notifyEmail, monitorNotifyEmail2);
    Policy policy3 = createPolicy(app.getId(), "Policy3", stage, notifyEmail, null /* monitorNotifyEmail */);
    Policy policy4 = createPolicy(parentOrg.getId(), "Policy4", stage, notifyEmail, monitorNotifyEmail3);

    File scanFile1 = createScanFile(app, scanId1);

    // Simulate that the report is available
    mockScanReceiptAndReport(scanId1);

    // Prepare to receive email notifications
    List<Message> notificationsDeveloper = MailboxTestUtil.get(notifyEmail);
    List<Message> notificationsMonitor1 = MailboxTestUtil.get(monitorNotifyEmail1);
    List<Message> notificationsMonitor2 = MailboxTestUtil.get(monitorNotifyEmail2);
    List<Message> notificationsMonitor3 = MailboxTestUtil.get(monitorNotifyEmail3);
    notificationsDeveloper.clear();
    notificationsMonitor1.clear();
    notificationsMonitor2.clear();
    notificationsMonitor3.clear();

    // Evaluate the policy. Only the developer should receive a notification.
    evaluatePolicy(app.getPublicId(), scanId1, stage);
    assertNotifications(notificationsMonitor1, 0, 5000);
    assertNotifications(notificationsMonitor2, 0, 0);
    assertNotifications(notificationsMonitor3, 0, 0);
    assertNotifications(notificationsDeveloper, 1, 0);
    notificationsDeveloper.clear();
    PolicyEvaluation policyEvaluation1 = policyEvaluationDAO.getLastByApplicationIdAndStageId(app.getId(),
        stage.getStageTypeId());
    for (PolicyViolation policyViolation : policyViolationDAO.getActiveByApplicationIdAndStageId(app.getId(),
        stage.getStageTypeId()))
    {
      assertThat(policyViolation.getActionTypeId()).isEqualTo(Action.ID_FAIL);
    }
    assertThat(scanFile1).isFile();

    // Run the policy monitor. There should be a new policy evaluation, but no notifications because nothing changed.
    String scanId2 = "PolicyMonitorTest_scanId2";
    mockScanReceiptAndReport(scanId2);
    policyMonitor.run();
    PolicyEvaluation policyEvaluation2 = policyEvaluationDAO.getLastByApplicationIdAndStageId(app.getId(),
        stage.getStageTypeId());
    assertThat(policyEvaluation2.getId()).isNotEqualTo(policyEvaluation1.getId());
    assertThat(policyEvaluation2.getScanId()).isEqualTo(scanId2);
    assertThat(policyEvaluation2.getTime()).isAfter(policyEvaluation1.getTime());
    for (PolicyViolation policyViolation : policyViolationDAO.getActiveByApplicationIdAndStageId(app.getId(),
        stage.getStageTypeId()))
    {
      assertThat(policyViolation.getActionTypeId()).isNull();
    }
    assertNotifications(notificationsDeveloper, 0, 5000);
    assertNotifications(notificationsMonitor1, 0, 0);
    assertNotifications(notificationsMonitor2, 0, 0);
    assertNotifications(notificationsMonitor3, 0, 0);
    assertThat(scanFile1).doesNotExist();
    File scanFile2 = insightWork.getScanFile(app.getId(), scanId2);
    assertThat(scanFile2.exists()).isTrue();

    // Modify policy3 and run the monitor again. There should be a new policy evaluation, but no notifications
    // because policy3 does not have notifications for monitoring.
    policy3.setThreatLevel(policy3.getThreatLevel() - 1);
    updatePolicy(OwnerType.APPLICATION, app.getPublicId(), policy3);
    String scanId3 = "PolicyMonitorTest_scanId3";
    mockScanReceiptAndReport(scanId3);
    policyMonitor.run();
    PolicyEvaluation policyEvaluation3 = policyEvaluationDAO.getLastByApplicationIdAndStageId(app.getId(),
        stage.getStageTypeId());
    assertThat(policyEvaluation3.getId()).isNotEqualTo(policyEvaluation2.getId());
    assertThat(policyEvaluation3.getScanId()).isEqualTo(scanId3);
    assertThat(policyEvaluation3.getTime()).isAfter(policyEvaluation2.getTime());
    for (PolicyViolation policyViolation : policyViolationDAO.getActiveByApplicationIdAndStageId(app.getId(),
        stage.getStageTypeId()))
    {
      assertThat(policyViolation.getActionTypeId()).isNull();
    }
    assertNotifications(notificationsDeveloper, 0, 5000);
    assertNotifications(notificationsMonitor1, 0, 0);
    assertNotifications(notificationsMonitor2, 0, 0);
    assertNotifications(notificationsMonitor3, 0, 0);
    assertThat(scanFile2).doesNotExist();
    File scanFile3 = insightWork.getScanFile(app.getId(), scanId3);
    assertThat(scanFile3.exists()).isTrue();

    // Modify policy1 and run the monitor again. Only the first monitor email should receive a notification.
    policy1.setThreatLevel(policy1.getThreatLevel() - 1);
    updatePolicy(OwnerType.APPLICATION, app.getPublicId(), policy1);
    String scanId4 = "PolicyMonitorTest_scanId4";
    mockScanReceiptAndReport(scanId4);
    policyMonitor.run();
    PolicyEvaluation policyEvaluation4 = policyEvaluationDAO.getLastByApplicationIdAndStageId(app.getId(),
        stage.getStageTypeId());
    assertThat(policyEvaluation4.getId()).isNotEqualTo(policyEvaluation3.getId());
    assertThat(policyEvaluation4.getScanId()).isEqualTo(scanId4);
    assertThat(policyEvaluation4.getTime()).isAfter(policyEvaluation3.getTime());
    for (PolicyViolation policyViolation : policyViolationDAO.getActiveByApplicationIdAndStageId(app.getId(),
        stage.getStageTypeId()))
    {
      assertThat(policyViolation.getActionTypeId()).isNull();
    }
    assertNotifications(notificationsDeveloper, 0, 5000);
    assertNotifications(notificationsMonitor2, 0, 0);
    assertNotifications(notificationsMonitor3, 0, 0);
    assertNotifications(notificationsMonitor1, 1, 0);
    notificationsMonitor1.clear();
    assertThat(scanFile3).doesNotExist();
    File scanFile4 = insightWork.getScanFile(app.getId(), scanId4);
    assertThat(scanFile4.exists()).isTrue();

    // Modify policy2 and run the monitor again. Only the second monitor email should receive a notification.
    policy2.setThreatLevel(policy2.getThreatLevel() - 1);
    updatePolicy(OwnerType.ORGANIZATION, org.getId(), policy2);
    String scanId5 = "PolicyMonitorTest_scanId5";
    mockScanReceiptAndReport(scanId5);
    policyMonitor.run();
    PolicyEvaluation policyEvaluation5 = policyEvaluationDAO.getLastByApplicationIdAndStageId(app.getId(),
        stage.getStageTypeId());
    assertThat(policyEvaluation5.getId()).isNotEqualTo(policyEvaluation4.getId());
    assertThat(policyEvaluation5.getScanId()).isEqualTo(scanId5);
    assertThat(policyEvaluation5.getTime()).isAfter(policyEvaluation4.getTime());
    for (PolicyViolation policyViolation : policyViolationDAO.getActiveByApplicationIdAndStageId(app.getId(),
        stage.getStageTypeId()))
    {
      assertThat(policyViolation.getActionTypeId()).isNull();
    }
    assertNotifications(notificationsDeveloper, 0, 5000);
    assertNotifications(notificationsMonitor1, 0, 0);
    assertNotifications(notificationsMonitor2, 1, 0);
    assertNotifications(notificationsMonitor3, 0, 0);
    notificationsMonitor2.clear();
    assertThat(scanFile4).doesNotExist();
    File scanFile5 = insightWork.getScanFile(app.getId(), scanId5);
    assertThat(scanFile5.exists()).isTrue();

    // Modify policy4 and run the monitor again. Only the forth monitor email should receive a notification
    policy4.setThreatLevel(policy4.getThreatLevel() - 1);
    updatePolicy(OwnerType.ORGANIZATION, parentOrg.getId(), policy4);
    String scanId6 = "PolicyMonitorTest_scanId6";
    mockScanReceiptAndReport(scanId6);
    policyMonitor.run();
    PolicyEvaluation policyEvaluation6 = policyEvaluationDAO.getLastByApplicationIdAndStageId(app.getId(),
        stage.getStageTypeId());
    assertThat(policyEvaluation6.getId()).isNotEqualTo(policyEvaluation5.getId());
    assertThat(policyEvaluation6.getScanId()).isEqualTo(scanId6);
    assertThat(policyEvaluation6.getTime()).isAfter(policyEvaluation5.getTime());
    for (PolicyViolation policyViolation : policyViolationDAO.getActiveByApplicationIdAndStageId(app.getId(),
        stage.getStageTypeId()))
    {
      assertThat(policyViolation.getActionTypeId()).isNull();
    }
    assertNotifications(notificationsDeveloper, 0, 5000);
    assertNotifications(notificationsMonitor1, 0, 0);
    assertNotifications(notificationsMonitor2, 0, 0);
    assertNotifications(notificationsMonitor3, 1, 0);
    notificationsMonitor3.clear();
    assertThat(scanFile5).doesNotExist();
    File scanFile6 = insightWork.getScanFile(app.getId(), scanId6);
    assertThat(scanFile6.exists()).isTrue();

    assertShutdownHandler();
  }

  private Policy createPolicy(
      String ownerId,
      String policyName,
      Stage stage,
      String notifyEmail,
      String monitorNotifyEmail)
  {
    Policy policy = new Policy(null /* id */, policyName);
    policy.setOwnerId(ownerId);
    policy.setThreatLevel(8);
    Constraint constraint = new Constraint(null /* id */, "Constraint", LogicalOperator.AND);
    Condition condition = new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0");
    constraint.addCondition(condition);
    policy.addConstraint(constraint);
    policy.setAction(stage.getStageTypeId(), FailActionType.ID);
    policy.getNotifications().add(new UserNotification(notifyEmail, stage.getStageTypeId()));
    if (monitorNotifyEmail != null) {
      policy.getNotifications().add(new UserNotification(monitorNotifyEmail, Notification.CONTINUOUS_MONITORING));
    }

    return tempEntity.newPolicy(policy);
  }

  private PolicyEvaluationResult evaluatePolicy(String applicationPublicId, String scanId, Stage stage) {
    HttpResponse response;
    try {
      response = restRequest().path(PolicyEvaluateResource.RESOURCE_PATH)
          .query("scanId", scanId)
          .parameter(applicationPublicId)
          .body(stage)
          .post();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
    assertResponseStatus(200, response);
    PolicyEvaluationResult policyEval = response.getBody(PolicyEvaluationResult.class);
    assertThat(policyEval).isNotNull();
    return policyEval;
  }

  private Policy updatePolicy(OwnerType ownerType, String ownerId, Policy policy) throws Exception {
    HttpResponse response = restRequest().path(PolicyResource.RESOURCE_PATH)
        .parameter(ownerType, ownerId)
        .body(policy)
        .put();
    assertResponseStatus(200, response);
    return response.getBody(Policy.class);
  }

  @Test
  public void testEvaluate_ScanFileDoesNotExist() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication("MonitoredApp", org.getId());
    Stage stage = new Stage(ReleaseStageType.ID);
    PolicyMonitoring policyMonitoring = tempEntity.newPolicyMonitoring(app.getId(), stage.getStageTypeId());

    String scanId = "PolicyMonitorTest_scanId";

    // Simulate that the report is available and evaluate policies
    createScanFile(app.getId(), scanId);
    mockScanReceiptAndReport(scanId);
    evaluatePolicy(app.getPublicId(), scanId, stage);

    // The scan file does not exist, which will cause an IOException in the policy monitoring.
    File scanFile = insightWork.getScanFile(app.getId(), scanId);
    scanFile.delete();
    assertThatExceptionOfType(IOException.class)
        .isThrownBy(() -> policyMonitor.evaluate(app, policyMonitoring))
        .withMessageContaining(scanFile.getName());
  }

  @Test
  public void testEvaluate_LatestScanFileReplacedDuringMonitoring() throws Exception {
    Organization org = tempEntity.newOrganization();
    final Application app = tempEntity.newApplication("MonitoredApp", org.getId());
    final Stage stage = new Stage(ReleaseStageType.ID);
    PolicyMonitoring policyMonitoring = tempEntity.newPolicyMonitoring(app.getId(), stage.getStageTypeId());

    final String scanId1 = "PolicyMonitorTest_scanId1";
    final String scanId2 = "PolicyMonitorTest_scanId2";
    final String scanId3 = "PolicyMonitorTest_scanId3";

    createScanFile(app, scanId1, "test1");
    mockScanReceiptAndReport(scanId1);
    evaluatePolicy(app.getPublicId(), scanId1, stage);

    // Exercise the retry branch in PolicyMonitor.cloneScanFile: the copy of scanFile1 must fail AFTER a
    // newer evaluation (scanId2) has entered the DAO, so that the retry picks up the new scan id and
    // succeeds. Spy on ScanPersistenceService so the first copyScanFile call can register the newer
    // evaluation as a side effect and then throw, simulating the race the old SecurityManager-based
    // test drove.
    ScanPersistenceService realScanPersistenceService =
        getCLMServer().getInstance(ScanPersistenceService.class);
    ScanPersistenceService spyScanPersistenceService = Mockito.spy(realScanPersistenceService);

    Field scanPersistenceServiceField = PolicyMonitor.class.getDeclaredField("scanPersistenceService");
    scanPersistenceServiceField.setAccessible(true);
    Object originalScanPersistenceService = scanPersistenceServiceField.get(policyMonitor);
    scanPersistenceServiceField.set(policyMonitor, spyScanPersistenceService);
    try {
      Mockito.doAnswer(invocation -> {
        createScanFile(app, scanId2, "test2");
        mockScanReceiptAndReport(scanId2);
        evaluatePolicy(app.getPublicId(), scanId2, stage);
        mockScanReceiptAndReport(scanId3);

        // Let cloneScanFile's retry use the real implementation so it actually copies scanFile2.
        Mockito.doCallRealMethod().when(spyScanPersistenceService).copyScanFile(any(), any());
        throw new IOException("Simulated read failure for scan " + scanId1);
      }).when(spyScanPersistenceService).copyScanFile(any(), any());

      policyMonitor.evaluate(app, policyMonitoring);
    }
    finally {
      scanPersistenceServiceField.set(policyMonitor, originalScanPersistenceService);
    }

    // Verify the retry recovered: last evaluation is for monitoring, uses the new scanId3, and the
    // uploaded scan file carries the content from scanFile2 (not scanFile1).
    PolicyEvaluation policyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndStageId(app.getId(),
        stage.getStageTypeId());
    assertThat(policyEvaluation.isForMonitoring()).isTrue();
    assertThat(policyEvaluation.getScanId()).isEqualTo(scanId3);
    File scanFile3 = insightWork.getScanFile(app.getId(), scanId3);
    assertThat(scanFile3).usingCharset(StandardCharsets.UTF_8).hasContent("test2");
  }

  @Test
  public void testEvaluate_PolicyAlertsFilePresent() throws Exception {
    Organization org = tempEntity.newOrganization();
    final Application app = tempEntity.newApplication("MonitoredApp", org.getId());
    final Stage stage = new Stage(ReleaseStageType.ID);
    PolicyMonitoring policyMonitoring = tempEntity.newPolicyMonitoring(app.getId(), stage.getStageTypeId());

    String scanId = "PolicyMonitorTest_scanId";
    createScanFile(app, scanId, "test");

    // Simulate that the report is available and evaluate policies
    mockScanReceiptAndReport(scanId);
    evaluatePolicy(app.getPublicId(), scanId, stage);

    // mock hds response for policyMonitor.evaluate triggering a new evaluation
    String newScanId = scanId + "1";
    mockScanReceiptAndReport(newScanId);

    policyMonitor.evaluate(app, policyMonitoring);

    File reportFile = insightWork.getReportFile(app.getId(), newScanId);
    assertThat(reportFile).isFile();
    File reportCacheDir = new File(reportFile.getParentFile(), "report.cache");
    assertThat(reportCacheDir).isDirectory();
    File policyAlertsFile = new File(reportCacheDir, POLICY_ALERTS.getName());
    assertThat(policyAlertsFile).isFile();
  }

  @Test
  public void testEvaluate_TempScanFileIsDeletedWhenHdsUploadFails() {
    Application app = tempEntity.newApplicationWithParent();
    Stage stage = new Stage(ReleaseStageType.ID);
    PolicyMonitoring policyMonitoring = tempEntity.newPolicyMonitoring(app.getId(), stage.getStageTypeId());

    String scanId = "scanId";
    File scanFile = createScanFile(app, scanId, "test");

    // Simulate that the report is available and evaluate policies
    mockScanReceiptAndReport(scanId);
    evaluatePolicy(app.getPublicId(), scanId, stage);

    // Mock an HDS error on scan upload and evaluate policies again.
    hdsRespondWith("HDS Error").andStatus(500).atUri("rest/application/analysis");
    assertThatExceptionOfType(InternalServerErrorException.class)
        .isThrownBy(() -> policyMonitor.evaluate(app, policyMonitoring));

    // Verify there are no temp scan files left behind.
    File[] scanFiles = insightWork.getScanDir(app.getId()).listFiles();
    assertThat(scanFiles).containsExactly(scanFile);
  }

  @Test
  public void testApplicationMonitored_SbomManagerComplianceStage_EvaluationQueueEnabled() throws Exception {
    setEvaluationQueueConfig(true);
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication("MonitoredApp", org.getId());
    Stage stage = new Stage(ComplianceStageType.ID);
    tempEntity.newPolicyMonitoring(app.getId(), stage.getStageTypeId());

    policyMonitor.run();

    assertThat(logOutput).atInfoLevel()
        .doesNotContain("SBOM Manager Policy Monitoring is enabled for application '" +
            app.getName() + "' and stage '" + stage.getStageTypeId() + "'");
  }

  @Test
  public void testApplicationMonitored_SbomManagerComplianceStage_EvaluationQueueDisabled() throws Exception {
    setEvaluationQueueConfig(false);
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication("MonitoredApp", org.getId());
    Stage stage = new Stage(ComplianceStageType.ID);

    // Simulate first scan
    String scanId1 = "PolicyMonitorTest_scanId";
    tempEntity.newPolicyEvaluation(app.getId(), stage.getStageTypeId(), scanId1);

    File scanZip = createScanFileZip(app, scanId1, "scan/scan-third-party.xml");
    createReportFile(app.getId(), scanId1, "/PolicyMonitorTest/report-third-party");

    tempEntity.newPolicyMonitoring(app.getId(), stage.getStageTypeId());

    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), ACTIVE, "xyz");
    tempEntity.newThirdPartyScan(scanId1, scanId1, thirdPartyFile, scanZip.getName());

    String newScanId = "PolicyMonitorTest_scanId2";
    mockScanReceiptAndReport(newScanId);
    policyMonitor.run();
    assertThat(logOutput).atInfoLevel()
        .contains("SBOM Manager Policy Monitoring is enabled for application '" +
            app.getName() + "' and stage '" + stage.getStageTypeId() + "'");

    File reportFile = insightWork.getReportFile(app.getId(), newScanId);
    assertThat(reportFile).isFile();
    File parentDir = new File(reportFile.getParentFile() + "/additional.files");

    assertThirdPartyFile(parentDir, THIRD_PARTY_BOM_JSON.getName());
    assertThirdPartyFile(parentDir, THIRD_PARTY_LICENSE_JSON.getName());
    assertThirdPartyFile(parentDir, THIRD_PARTY_SECURITY_JSON.getName());

    assertShutdownHandler();
  }

  @Test
  public void testApplicationMonitored_Both_SbomManagerComplianceStage_LifecycleReleaseStage() throws Exception {
    setEvaluationQueueConfig(false);
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication("app", org.getId());
    Stage complianceStage = new Stage(ComplianceStageType.ID);
    Stage releaseStage = new Stage(ReleaseStageType.ID);
    tempEntity.newPolicyMonitoring(app.getId(), complianceStage.getStageTypeId());
    tempEntity.newPolicyMonitoring(app.getId(), releaseStage.getStageTypeId());

    String scanId1 = "PolicyMonitorTest_scanId";
    tempEntity.newPolicyEvaluation(app.getId(), complianceStage.getStageTypeId(), scanId1);
    File scanZip = createScanFileZip(app, scanId1, "scan/scan-third-party.xml");
    createReportFile(app.getId(), scanId1, "/PolicyMonitorTest/report-third-party");
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), ACTIVE, "xyz");
    tempEntity.newThirdPartyScan(scanId1, scanId1, thirdPartyFile, scanZip.getName());

    String newScanId = "PolicyMonitorTest_scanId2";
    mockScanReceiptAndReport(newScanId);
    policyMonitor.run();

    assertThat(logOutput).atInfoLevel()
        .contains("SBOM Manager Policy Monitoring is enabled for application '" +
            app.getName() + "' and stage '" + complianceStage.getStageTypeId() + "'");
    assertThat(logOutput).atInfoLevel()
        .contains("Policy monitoring is enabled for application '" +
            app.getName() + "' and stage '" + releaseStage.getStageTypeId() + "'");
    assertThat(logOutput).atDebugLevel()
        .contains("SBOM Manager Policy Monitoring evaluated for application '" +
            app.getName() + "'");
    assertThat(logOutput).atInfoLevel().contains("Finished policy monitoring");

    assertShutdownHandler();
  }

  @Test
  public void testApplicationMonitored_SbomManagerComplianceStage_NoMonitorableSbomVersion() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication("MonitoredApp", org.getId());
    Stage stage = new Stage(ComplianceStageType.ID);

    // Simulate first scan
    String scanId1 = "PolicyMonitorTest_scanId";
    tempEntity.newPolicyEvaluation(app.getId(), stage.getStageTypeId(), scanId1);

    File scanZip = createScanFileZip(app, scanId1, "scan/scan-third-party.xml");
    createReportFile(app.getId(), scanId1, "/PolicyMonitorTest/report-third-party");

    tempEntity.newPolicyMonitoring(app.getId(), stage.getStageTypeId());

    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(scanId1, scanId1, thirdPartyFile, scanZip.getName());

    String newScanId = "PolicyMonitorTest_scanId2";
    mockScanReceiptAndReport(newScanId);
    policyMonitor.run();

    File reportFile = insightWork.getReportFile(app.getId(), newScanId);
    assertThat(reportFile).doesNotExist();

    assertShutdownHandler();
  }

  @Test
  public void testApplicationMonitored_SbomManagerComplianceStage_NoFilteredScanFile() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication("MonitoredApp", org.getId());
    Stage stage = new Stage(ComplianceStageType.ID);

    // Simulate first scan
    String scanId1 = "PolicyMonitorTest_scanId";
    tempEntity.newPolicyEvaluation(app.getId(), stage.getStageTypeId(), scanId1);

    createScanFileZip(app, scanId1, "scan/scan-third-party.xml");
    createReportFile(app.getId(), scanId1, "/PolicyMonitorTest/report-third-party");

    tempEntity.newPolicyMonitoring(app.getId(), stage.getStageTypeId());

    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), ACTIVE, "xyz");

    String newScanId = "PolicyMonitorTest_scanId2";
    mockScanReceiptAndReport(newScanId);
    policyMonitor.run();

    File reportFile = insightWork.getReportFile(app.getId(), newScanId);
    assertThat(reportFile).doesNotExist();

    assertShutdownHandler();
  }

  @Test
  public void testApplicationMonitored_SbomManagerComplianceStage_MissingFilteredScanFile() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication("MonitoredApp", org.getId());
    Stage stage = new Stage(ComplianceStageType.ID);

    // Simulate first scan
    String scanId1 = "PolicyMonitorTest_scanId";
    tempEntity.newPolicyEvaluation(app.getId(), stage.getStageTypeId(), scanId1);

    createScanFileZip(app, scanId1, "scan/scan-third-party.xml");
    createReportFile(app.getId(), scanId1, "/PolicyMonitorTest/report-third-party");

    tempEntity.newPolicyMonitoring(app.getId(), stage.getStageTypeId());

    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), ACTIVE, "xyz");
    tempEntity.newThirdPartyScan(scanId1, scanId1, thirdPartyFile, "scan/deleted.gz");

    String newScanId = "PolicyMonitorTest_scanId2";
    mockScanReceiptAndReport(newScanId);
    policyMonitor.run();

    File reportFile = insightWork.getReportFile(app.getId(), newScanId);
    assertThat(reportFile).doesNotExist();

    assertShutdownHandler();
  }

  @Test
  public void testApplicationMonitored_ThirdPartyScan() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication("MonitoredApp", org.getId());
    Stage stage = new Stage(ReleaseStageType.ID);

    // Simulate first scan
    String scanId1 = "PolicyMonitorTest_scanId";
    tempEntity.newPolicyEvaluation(app.getId(), stage.getStageTypeId(), scanId1);

    createScanFileZip(app, scanId1, "scan/scan-third-party.xml");
    createReportFile(app.getId(), scanId1, "/PolicyMonitorTest/report-third-party");

    tempEntity.newPolicyMonitoring(app.getId(), stage.getStageTypeId());

    String newScanId = "PolicyMonitorTest_scanId2";
    mockScanReceiptAndReport(newScanId);
    policyMonitor.run();

    File reportFile = insightWork.getReportFile(app.getId(), newScanId);
    assertThat(reportFile).isFile();
    File parentDir = new File(reportFile.getParentFile() + "/additional.files");

    assertThirdPartyFile(parentDir, THIRD_PARTY_BOM_JSON.getName());
    assertThirdPartyFile(parentDir, THIRD_PARTY_LICENSE_JSON.getName());
    assertThirdPartyFile(parentDir, THIRD_PARTY_SECURITY_JSON.getName());

    assertShutdownHandler();
  }

  @Test
  public void testApplicationNotMonitored_RootOrgAndProxyStage() {
    tempEntity.newPolicyMonitoring(Organization.ROOT_ORGANIZATION_ID, ProxyStageType.ID);
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication("app", org.getId());

    String scanId1 = "PolicyMonitorTest_scanId";
    createScanFile(app, scanId1);

    tempEntity.newPolicyEvaluation(app.getId(), ProxyStageType.ID, scanId1);

    // second scan file should not appear if there has not been an evaluation
    String scanId2 = "PolicyMonitorTest_scanId2";
    mockScanReceiptAndReport(scanId2);

    policyMonitor.run();

    // If the second scan file does not exist, then evaluation was not attempted and the scan file was not uploaded.
    // Invalid stage type exception would be still be thrown, but is caught and logged so can't be verified here.
    assertThat(insightWork.getScanFile(app.getId(), scanId2).exists()).isFalse();

    assertShutdownHandler();
  }

  private File createScanFileZip(Application app, String scanId, final String fileName) throws Exception {
    URL resource = getClass().getResource("/PolicyMonitorTest/" + fileName);
    File scanXml = new File(resource.toURI());

    File scanFile = insightWork.getScanFile(app.getId(), scanId);
    Files.createDirectories(scanFile.getParentFile().toPath());

    try (GZIPOutputStream gzipStream = new GZIPOutputStream(Files.newOutputStream(scanFile.toPath()))) {
      FileUtils.copyFile(scanXml, gzipStream);
    }
    return scanFile;
  }

  private void assertThirdPartyFile(File parentDir, String fileName) {
    File thirdPartyDataFile = new File(parentDir, fileName);
    assertThat(thirdPartyDataFile).exists();
  }

  private File createScanFile(Application app, String scanId) {
    return createScanFile(app, scanId, "test");
  }

  private File createScanFile(Application app, String scanId, String fileContent) {
    File scanFile = insightWork.getScanFile(app.getId(), scanId);

    try {
      Files.createDirectories(scanFile.getParentFile().toPath());
      Files.write(scanFile.toPath(), fileContent.getBytes(StandardCharsets.UTF_8));
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }

    return scanFile;
  }

  private void mockScanReceiptAndReport(String scanId) {
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(scanId);
    scanReceipt.setTimeToReport(1L);
    mockScanReceipt(scanReceipt);
    mockReport(scanId, "/" + getClass().getSimpleName() + "/report");
  }

  private void overrideField(Object target, String fieldName, Object value) {
    Class<?> type = target.getClass();
    while (type != null) {
      try {
        Field field = type.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
        return;
      }
      catch (NoSuchFieldException e) {
        type = type.getSuperclass();
      }
      catch (IllegalAccessException e) {
        throw new RuntimeException("Failed to override field '" + fieldName + "'", e);
      }
    }
    throw new IllegalArgumentException("Could not find field '" + fieldName + "' on " + target.getClass());
  }

  private void assertShutdownHandler() {
    verify(mockShutdownHandler).add(policyMonitor.getExecutorService());
    verify(mockShutdownHandler).remove(policyMonitor.getExecutorService());
  }

  private void setEvaluationQueueConfig(final boolean enabled) {
    EvaluationQueueConfig evaluationQueueConfig = EvaluationQueueConfig.builder().enabled(enabled).build();
    ApiConfigurationService apiConfigurationService = getCLMServer().getInstance(ApiConfigurationService.class);
    apiConfigurationService.setConfigurationNoAuthz(SystemConfigurationProperty.EVALUATION_QUEUE_CONFIG,
        JsonUtils.convertValue(evaluationQueueConfig, Map.class));
  }
}
