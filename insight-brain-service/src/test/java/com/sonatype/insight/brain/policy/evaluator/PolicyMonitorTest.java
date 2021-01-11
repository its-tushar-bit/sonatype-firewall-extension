/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.Permission;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

import javax.mail.Message;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.HygieneRating;
import com.sonatype.clm.dto.model.component.IntegrityRating;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.MailConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.actions.WarnActionType;
import com.sonatype.insight.brain.model.policy.conditions.HygieneRatingConditionType;
import com.sonatype.insight.brain.model.policy.conditions.IntegrityRatingConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.notifications.Notification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.policy.PolicyResource;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.brain.service.AbstractBrainServiceTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO;
import com.sonatype.insight.brain.webhook.ApplicationEvaluationEvent;
import com.sonatype.insight.brain.webhook.TestEventHandler;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.license.model.LicensedFeature;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.mock_javamail.Mailbox;

import static com.sonatype.insight.brain.Assert.assertNotifications;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class PolicyMonitorTest
    extends AbstractBrainServiceTest
{
  private PolicyMonitor policyMonitor;

  private InsightWork insightWork;

  private InsightConfig insightConfig;

  private String savedBaseUrl;

  private AsyncEventBus asyncEventBus;

  private TestEventHandler<ApplicationEvaluationEvent> handler;

  @Before
  public void setup() {
    insightConfig = getCLMServer().getInstance(InsightConfig.class);
    savedBaseUrl = insightConfig.getBaseUrl();
    insightConfig.setBaseUrl("http://clm.sonatype.com/test");
    insightWork = getCLMServer().getInstance(InsightWork.class);
    policyMonitor = getCLMServer().getInstance(PolicyMonitor.class);
    asyncEventBus = getCLMServer().getInstance(AsyncEventBus.class);

    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setHostname("127.0.0.1");
    mailConfiguration.setPort(587);
    mailConfiguration.setSystemEmail("NexusIQServer@localhost");
    new MailConfigurationDAO().set(mailConfiguration);
  }

  @After
  public void cleanup() {
    insightConfig.setBaseUrl(savedBaseUrl);

    if (handler != null) {
      asyncEventBus.unregister(handler);
    }
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
    PolicyEvaluationDAO policyEvaluationDAO = new PolicyEvaluationDAO();
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
  }

  @Test
  public void testApplicationNotMonitored() throws Exception {
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
    PolicyEvaluationDAO policyEvaluationDAO = new PolicyEvaluationDAO();
    for (StageType stageType : StageTypes.getAll()) {
      assertThat(
          policyEvaluationDAO.getLastByApplicationIdAndStageId(notMonitoredApp.getId(), stageType.getId()).getTime())
              .isEqualTo(policyEvaluations.get(stageType).getTime());
    }
  }

  @Test
  public void testApplicationMonitored_NoScan() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication("MonitoredApp", org.getId());
    tempEntity.newPolicyMonitoring(app.getId(), ReleaseStageType.ID);

    policyMonitor.run();

    // There should be no policy evaluations
    PolicyEvaluationDAO policyEvaluationDAO = new PolicyEvaluationDAO();
    for (StageType stageType : StageTypes.getAll()) {
      assertThat(policyEvaluationDAO.getLastByApplicationIdAndStageId(app.getId(), stageType.getId())).isNull();
    }
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

    handler = new TestEventHandler<>(new CountDownLatch(1));
    asyncEventBus.register(handler);

    scanId = "PolicyMonitorTest_scanId1";
    mockScanReceiptAndReport(scanId);
    policyMonitor.run();

    assertThat(handler.getLatch().await(1, TimeUnit.SECONDS)).isTrue();
    ApplicationEvaluationEvent event = handler.getEvent();
    assertThat(event).isNotNull();
    assertThat(event.initiator).isEqualTo("system");
  }

  private void testMonitored(OwnerType monitorOwnerType) throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication("MonitoredApp", org.getId());
    Owner parentOrg = new OwnerDAO().getParentOwner(org);

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
    List<Message> notificationsDeveloper = Mailbox.get(notifyEmail);
    List<Message> notificationsMonitor1 = Mailbox.get(monitorNotifyEmail1);
    List<Message> notificationsMonitor2 = Mailbox.get(monitorNotifyEmail2);
    List<Message> notificationsMonitor3 = Mailbox.get(monitorNotifyEmail3);
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
    PolicyEvaluationDAO policyEvaluationDAO = new PolicyEvaluationDAO();
    PolicyEvaluation policyEvaluation1 = policyEvaluationDAO.getLastByApplicationIdAndStageId(app.getId(),
        stage.getStageTypeId());
    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();
    for (PolicyViolation policyViolation : policyViolationDAO.getActiveByApplicationIdAndStageId(app.getId(),
        stage.getStageTypeId())) {
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
        stage.getStageTypeId())) {
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
        stage.getStageTypeId())) {
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
        stage.getStageTypeId())) {
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
        stage.getStageTypeId())) {
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
        stage.getStageTypeId())) {
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
  }

  private Policy createPolicy(String ownerId,
                              String policyName,
                              Stage stage,
                              String notifyEmail,
                              String monitorNotifyEmail) throws Exception
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

  private Policy createPolicy(
      String policyName,
      Stage stage,
      String action,
      Constraint constraint)
  {
    Policy policy = new Policy(null /* id */, policyName);
    policy.setOwnerId(Organization.ROOT_ORGANIZATION_ID);
    policy.setThreatLevel(8);
    policy.addConstraint(constraint);
    policy.setAction(stage.getStageTypeId(), action);

    return tempEntity.newPolicy(policy);
  }

  private PolicyEvaluationResult evaluatePolicy(String applicationPublicId, String scanId, Stage stage) {
    HttpResponse response;
    try {
      response = restRequest().path(PolicyEvaluateResource.RESOURCE_PATH).query("scanId", scanId)
          .parameter(applicationPublicId).body(stage).post();
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
    HttpResponse response = restRequest().path(PolicyResource.RESOURCE_PATH).parameter(ownerType, ownerId).body(policy)
        .put();
    assertResponseStatus(200, response);
    return response.getBody(Policy.class);
  }

  @Test
  public void testEvaluate_ScanFileDoesNotExist() throws Exception {
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
    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
      policyMonitor.evaluate(app, policyMonitoring);
    }).withMessageContaining(scanFile.getName());
  }

  @Test
  public void testEvaluate_LatestScanFileReplacedDuringMonitoring() throws Exception {
    Organization org = tempEntity.newOrganization();
    final Application app = tempEntity.newApplication("MonitoredApp", org.getId());
    final Stage stage = new Stage(ReleaseStageType.ID);
    PolicyMonitoring policyMonitoring = tempEntity.newPolicyMonitoring(app.getId(), stage.getStageTypeId());

    String scanId1 = "PolicyMonitorTest_scanId1";
    final File scanFile1 = createScanFile(app, scanId1, "test1");

    // Simulate that the report is available and evaluate policies
    mockScanReceiptAndReport(scanId1);
    evaluatePolicy(app.getPublicId(), scanId1, stage);

    final String scanId2 = "PolicyMonitorTest_scanId2";
    final String scanId3 = "PolicyMonitorTest_scanId3";

    // Simulate a race condition between monitoring and policy evaluation:
    // Plug in a SecurityManager that triggers a new policy evaluation with a new scan file when the first scan file is
    // accessed and denies access to the first scan file. This causes the {@link PolicyMonitor} to retry the monitoring
    // policy evaluation with the new scan file (if there wasn't a new scan file, the monitoring would fail).
    SecurityManager originalSecurityManager = System.getSecurityManager();
    try {
      System.setSecurityManager(new SecurityManager()
      {
        private boolean enabled = true;

        @Override
        public void checkRead(String file) {
          if (enabled && file.contains(scanFile1.getName())) {
            enabled = false;
            createScanFile(app, scanId2, "test2");
            mockScanReceiptAndReport(scanId2);
            evaluatePolicy(app.getPublicId(), scanId2, stage);

            // Prepare the HDS mock server to reply with scanId3 for the next uploaded scan.
            mockScanReceiptAndReport(scanId3);

            throw new SecurityException("Read denied for " + file);
          }
        }

        @Override
        public void checkPermission(Permission perm) {
        }
      });

      policyMonitor.evaluate(app, policyMonitoring);
    }
    finally {
      System.setSecurityManager(originalSecurityManager);
    }

    // Verify that the latest policy evaluation is for monitoring and it used the second scan file.
    PolicyEvaluation policyEvaluation = new PolicyEvaluationDAO().getLastByApplicationIdAndStageId(app.getId(),
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
    File reportCacheDir = new File(reportFile.getParentFile(), Report.CACHE_DIRECTORY_NAME);
    assertThat(reportCacheDir).isDirectory();
    File policyAlertsFile = new File(reportCacheDir, ScanPolicyEvaluator.POLICY_ALERTS_FILENAME);
    assertThat(policyAlertsFile).isFile();
  }

  @Test
  public void testEvaluate_TempScanFileIsDeletedWhenHdsUploadFails() throws Exception {
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
    assertThatExceptionOfType(BadGatewayException.class)
        .isThrownBy(() -> policyMonitor.evaluate(app, policyMonitoring));

    // Verify there are no temp scan files left behind.
    File[] scanFiles = insightWork.getScanDir(app.getId()).listFiles();
    assertThat(scanFiles).containsExactly(scanFile);
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
    File parentDir = new File(reportFile.getParentFile() + "/report.cache");

    assertThirdPartyFile(parentDir, ThirdPartyComponentDAO.THIRD_PARTY_BOM_JSON_FILENAME);
    assertThirdPartyFile(parentDir, ThirdPartyComponentDAO.THIRD_PARTY_LICENSE_JSON_FILENAME);
    assertThirdPartyFile(parentDir, ThirdPartyComponentDAO.THIRD_PARTY_SECURITY_JSON_FILENAME);
  }

  @Test
  public void testRepositoryMonitored_MonitoringNotEnabled() {
    Condition condition = new Condition(IntegrityRatingConditionType.ID, "is", "2");
    Constraint constraint = new Constraint("c1", "constraint1", LogicalOperator.OR);
    constraint.addCondition(condition);
    Policy policy = createPolicy("policy", new Stage(ProxyStageType.ID), FailActionType.ID, constraint);
    Repository repository = tempEntity.newRepository();
    RepositoryComponent component = tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "pathname1",
        "hash1", ComponentIdentifier.createMavenCoordinates("g", "a1", "v"), true);
    assertThat(component.isQuarantined()).isTrue();

    createPolicyViolation(policy, component, FailActionType.ID);

    mockFirewallResponse(getFirewallHdsResponse(component, "hash1", new IntegrityRating(0, "Normal")));

    policyMonitor.run();

    assertThat(new RepositoryComponentDAO().getById(component.getId()).isQuarantined()).isTrue();
    assertThat(new RepositoryPolicyViolationDAO().getByRepositoryId(repository.getId())).hasSize(1);
  }

  @Test
  public void testRepositoryMonitored_IntegrityRatingNotChanged() {
    Condition condition = new Condition(IntegrityRatingConditionType.ID, "is", "2");
    Constraint constraint = new Constraint("c1", "constraint1", LogicalOperator.OR);
    constraint.addCondition(condition);
    Policy policy = createPolicy("policy", new Stage(ProxyStageType.ID), FailActionType.ID, constraint);

    Repository repository = tempEntity.newRepository();
    RepositoryComponent component = tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "pathname1",
        "hash1", ComponentIdentifier.createMavenCoordinates("g", "a1", "v"), true);
    assertThat(component.isQuarantined()).isTrue();

    createPolicyViolation(policy, component, FailActionType.ID);
    tempEntity.newPolicyMonitoring(repository.getId(), ProxyStageType.ID);

    mockFirewallResponse(getFirewallHdsResponse(component, "hash1", new IntegrityRating(2, "Pending")));

    policyMonitor.run();

    assertThat(new RepositoryComponentDAO().getById(component.getId()).isQuarantined()).isTrue();
    assertThat(new RepositoryPolicyViolationDAO().getByRepositoryId(repository.getId())).hasSize(1);
  }

  @Test
  public void testRepositoryMonitored_IntegrityRatingChanged() {
    Condition condition = new Condition(IntegrityRatingConditionType.ID, "is", "2");
    Constraint constraint = new Constraint("c1", "constraint1", LogicalOperator.OR);
    constraint.addCondition(condition);
    Policy policy = createPolicy("policy", new Stage(ProxyStageType.ID), FailActionType.ID, constraint);

    Repository repository = tempEntity.newRepository();
    RepositoryComponent component = tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "pathname1",
        "hash1", ComponentIdentifier.createMavenCoordinates("g", "a1", "v"), true);
    assertThat(component.isQuarantined()).isTrue();

    createPolicyViolation(policy, component, FailActionType.ID);
    tempEntity.newPolicyMonitoring(repository.getId(), ProxyStageType.ID);

    mockFirewallResponse(getFirewallHdsResponse(component, "hash1", new IntegrityRating(0, "Normal")));

    policyMonitor.run();

    assertThat(new RepositoryComponentDAO().getById(component.getId()).isQuarantined()).isFalse();
    assertThat(new RepositoryPolicyViolationDAO().getByRepositoryId(repository.getId())).isEmpty();
  }

  @Test
  public void testRepositoryMonitored_IntegrityRatingChangedWithOtherFailViolation() {
    Constraint constraint = new Constraint("c1", "constraint1", LogicalOperator.OR);
    Condition condition = new Condition(IntegrityRatingConditionType.ID, "is", "2");
    constraint.addCondition(condition);
    Condition condition2 = new Condition(HygieneRatingConditionType.ID, "is", "4");
    constraint.addCondition(condition2);
    Policy policy = createPolicy("policy", new Stage(ProxyStageType.ID), FailActionType.ID, constraint);

    Repository repository = tempEntity.newRepository();
    RepositoryComponent component = tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "pathname1",
        "hash1", ComponentIdentifier.createMavenCoordinates("g", "a1", "v"), true);
    assertThat(component.isQuarantined()).isTrue();

    createPolicyViolation(policy, component, FailActionType.ID);
    tempEntity.newPolicyMonitoring(repository.getId(), ProxyStageType.ID);

    mockFirewallResponse(getFirewallHdsResponse(component, "hash1", new IntegrityRating(0, "Normal")));

    policyMonitor.run();

    assertThat(new RepositoryComponentDAO().getById(component.getId()).isQuarantined()).isTrue();
    assertThat(new RepositoryPolicyViolationDAO().getByRepositoryId(repository.getId())).hasSize(1);
  }

  @Test
  public void testRepositoryMonitored_IntegrityRatingChangedWithOtherNonFailViolation() {
    Constraint constraint1 = new Constraint("c1", "constraint1", LogicalOperator.OR);
    Condition condition1 = new Condition(IntegrityRatingConditionType.ID, "is", "2");
    constraint1.addCondition(condition1);
    Policy policy1 = createPolicy("policy1", new Stage(ProxyStageType.ID), FailActionType.ID, constraint1);

    Constraint constraint2 = new Constraint("c1", "constraint1", LogicalOperator.OR);
    Condition condition2 = new Condition(HygieneRatingConditionType.ID, "is", "4");
    constraint2.addCondition(condition2);
    Policy policy2 = createPolicy("policy2", new Stage(ProxyStageType.ID), WarnActionType.ID, constraint2);

    Repository repository = tempEntity.newRepository();
    RepositoryComponent component = tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "pathname1",
        "hash1", ComponentIdentifier.createMavenCoordinates("g", "a1", "v"), true);
    assertThat(component.isQuarantined()).isTrue();

    createPolicyViolation(policy1, component, FailActionType.ID);
    createPolicyViolation(policy2, component, WarnActionType.ID);
    tempEntity.newPolicyMonitoring(repository.getId(), ProxyStageType.ID);

    mockFirewallResponse(getFirewallHdsResponse(component, "hash1", new IntegrityRating(0, "Normal")));

    policyMonitor.run();

    assertThat(new RepositoryComponentDAO().getById(component.getId()).isQuarantined()).isFalse();
    assertThat(new RepositoryPolicyViolationDAO().getByRepositoryId(repository.getId())).hasSize(1);
  }

  @Test
  public void testRepositoryMonitored_IntegrityRatingViolationNotQuarantined() {
    Condition condition = new Condition(IntegrityRatingConditionType.ID, "is", "2");
    Constraint constraint = new Constraint("c1", "constraint1", LogicalOperator.OR);
    constraint.addCondition(condition);
    Policy policy = createPolicy("policy", new Stage(ProxyStageType.ID), FailActionType.ID, constraint);

    Repository repository = tempEntity.newRepository();
    RepositoryComponent component = tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "pathname1",
        "hash1", ComponentIdentifier.createMavenCoordinates("g", "a1", "v"), false);
    assertThat(component.isQuarantined()).isFalse();

    createPolicyViolation(policy, component, FailActionType.ID);
    tempEntity.newPolicyMonitoring(repository.getId(), ProxyStageType.ID);

    // if the component gets re-evaluated, it will quarantined due matching policy condition
    mockFirewallResponse(getFirewallHdsResponse(component, "hash1", new IntegrityRating(2, "Pending")));

    policyMonitor.run();

    assertThat(new RepositoryComponentDAO().getById(component.getId()).isQuarantined()).isFalse();
    assertThat(new RepositoryPolicyViolationDAO().getByRepositoryId(repository.getId())).hasSize(1);
  }

  @Test
  public void testRepositoryMonitored_IntegrityRatingViolationQuarantinedBeyondMaxDays() {
    Condition condition = new Condition(IntegrityRatingConditionType.ID, "is", "2");
    Constraint constraint = new Constraint("c1", "constraint1", LogicalOperator.OR);
    constraint.addCondition(condition);
    Policy policy = createPolicy("policy", new Stage(ProxyStageType.ID), FailActionType.ID, constraint);

    Repository repository = tempEntity.newRepository();
    Date quarantineTime = Date.from(
        LocalDateTime.now().minusDays(PolicyMonitor.MAX_DAYS_FOR_UPDATED_INTEGRITY_RATING + 1)
            .atZone(ZoneId.systemDefault()).toInstant());
    RepositoryComponent component = tempEntity
        .newRepositoryComponent(repository.getId(), MatchState.EXACT, "pathname1", "hash1",
            ComponentIdentifier.createMavenCoordinates("g", "a1", "v"), new Date(), quarantineTime);
    assertThat(component.isQuarantined()).isTrue();

    createPolicyViolation(policy, component, FailActionType.ID);
    tempEntity.newPolicyMonitoring(repository.getId(), ProxyStageType.ID);

    // if the component gets re-evaluated, it will unquarantined due not matching policy condition
    mockFirewallResponse(getFirewallHdsResponse(component, "hash1", new IntegrityRating(0, "Normal")));

    policyMonitor.run();

    assertThat(new RepositoryComponentDAO().getById(component.getId()).isQuarantined()).isTrue();
    assertThat(new RepositoryPolicyViolationDAO().getByRepositoryId(repository.getId())).hasSize(1);
  }

  private ComponentEvaluationDataList getFirewallHdsResponse(
      final RepositoryComponent component,
      final String hash,
      final IntegrityRating integrityRating)
  {
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    ComponentEvaluationData componentEvaluationData = new ComponentEvaluationData();
    componentEvaluationData.hash = hash;
    componentEvaluationData.componentIdentifier = component.getComponentIdentifier();
    componentEvaluationData.matchState = MatchState.EXACT.getId();
    componentEvaluationData.declaredLicenses = new HashSet<>();
    componentEvaluationData.observedLicenses = new HashSet<>();
    componentEvaluationData.securityVulnerabilities = new ArrayList<>();
    componentEvaluationData.securityVulnerabilities.add(new SecurityVulnerability("refid", "source", 10F));
    componentEvaluationData.integrityRating = integrityRating;
    componentEvaluationData.hygieneRating = new HygieneRating(4, "Laggard");
    hdsResult.components.add(componentEvaluationData);
    return hdsResult;
  }

  private RepositoryPolicyViolation createPolicyViolation(
      Policy policy,
      RepositoryComponent component,
      String action)
  {
    RepositoryPolicyViolation policyViolation = new RepositoryPolicyViolation();
    policyViolation.setRepositoryId(component.getRepositoryId());
    policyViolation.setPathname(component.getPathname());
    policyViolation.setTime(new Date());
    policyViolation.setHash(component.getHash());
    policyViolation.setComponentIdentifier(component.getComponentIdentifier());
    policyViolation.setPolicyId(policy.getId());
    policyViolation.setPolicyName(policy.getName());
    policyViolation.setThreatLevel(policy.getThreatLevel());
    policyViolation.setThreatCategory(policy.getThreatCategory());
    policyViolation.setConstraintFacts(createConstraintFacts(policy));
    policyViolation.setActionTypeId(action);
    return tempEntity.newRepositoryPolicyViolation(policyViolation);
  }

  private List<ConstraintFact> createConstraintFacts(Policy policy) {
    List<ConstraintFact> constraintFacts = new ArrayList();
    for (Constraint constraint : policy.getConstraints()) {
      for (Condition condition : constraint.getConditions()) {
        ConstraintFact constraintFact = new ConstraintFact(constraint.getId(), constraint.getName(),
            constraint.getOperator().toString());
        constraintFact.addConditionFact(new ConditionFact(condition.getConditionTypeId(), 0, "", "random for condition "
            + condition.getConditionTypeId()));
        constraintFacts.add(constraintFact);
      }
    }

    return constraintFacts;
  }

  private File createScanFileZip(Application app, String scanId, final String fileName) throws Exception {
    URL resource = getClass().getResource("/PolicyMonitorTest/" + fileName);
    File scanXml = new File(resource.toURI());

    File scanFile = insightWork.getScanFile(app.getId(), scanId);
    Files.createDirectories(scanFile.getParentFile().toPath());

    try (GZIPOutputStream gzipStream = new GZIPOutputStream(new FileOutputStream(scanFile))) {
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

  protected void mockFirewallResponse(ComponentEvaluationDataList hdsResult) {
    hdsRespondWith(hdsResult).atUri(RepositoryPolicyEvaluator.HDS_COMPONENT_DETAILS_PATH);
  }
}
