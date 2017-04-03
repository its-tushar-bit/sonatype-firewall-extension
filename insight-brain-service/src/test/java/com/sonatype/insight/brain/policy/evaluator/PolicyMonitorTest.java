/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.security.Permission;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.mail.Message;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.model.policy.notifications.Notification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.policy.PolicyResource;
import com.sonatype.insight.brain.service.AbstractBrainServiceTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.webhook.ApplicationEvaluationEvent;
import com.sonatype.insight.brain.webhook.TestEventHandler;

import org.codehaus.plexus.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.mock_javamail.Mailbox;

import static com.sonatype.insight.brain.Assert.assertNotifications;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

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
    insightConfig = getCLMServer().getInjector().getInstance(InsightConfig.class);
    savedBaseUrl = insightConfig.getBaseUrl();
    insightConfig.setBaseUrl("http://clm.sonatype.com/test");
    insightWork = getCLMServer().getInjector().getInstance(InsightWork.class);
    policyMonitor = getCLMServer().getInjector().getInstance(PolicyMonitor.class);
    asyncEventBus = getCLMServer().getInjector().getInstance(AsyncEventBus.class);
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

    String licenseFingerprint = "PolicyMonitorTest_LicenseFingerprint";
    setLicenseFingerprint(licenseFingerprint);
    String scanId = "PolicyMonitorTest_scanId";

    createScanFile(app, scanId);

    // Simulate that the report is available
    mockReport(scanId, "/PolicyMonitorTest/report.zip");

    evaluatePolicy(app.getPublicId(), scanId, stage);

    setLicenseProducts();

    Collection<StageType> stageTypes = StageTypes.getAll();

    Map<StageType, Date> lastRun = new HashMap<>();
    PolicyEvaluationDAO policyEvaluationDAO = new PolicyEvaluationDAO();
    for (StageType stageType : stageTypes) {
      PolicyEvaluation eval = policyEvaluationDAO.getLastByApplicationIdAndStageId(app.getId(), stageType.getId());
      lastRun.put(stageType, eval == null ? null : eval.getTime());
    }

    policyMonitor.run();

    // There should be no new policy evaluations
    for (StageType stageType : stageTypes) {
      PolicyEvaluation eval = policyEvaluationDAO.getLastByApplicationIdAndStageId(app.getId(), stageType.getId());
      Date val = lastRun.get(stageType);
      assertThat((val == null && eval == null) || (val != null && eval != null && val.equals(eval.getTime())), is(true));
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
      assertThat(policyEvaluationDAO.getLastByApplicationIdAndStageId(notMonitoredApp.getId(), stageType.getId())
          .getTime(), is(policyEvaluations.get(stageType).getTime()));
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
      assertThat(policyEvaluationDAO.getLastByApplicationIdAndStageId(app.getId(), stageType.getId()), is(nullValue()));
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
    mockReport(scanId, "/PolicyMonitorTest/report.zip");

    evaluatePolicy(app.getPublicId(), scanId, stage);

    handler = new TestEventHandler<>(new CountDownLatch(1));
    asyncEventBus.register(handler);

    policyMonitor.run();

    assertThat(handler.getLatch().await(1, TimeUnit.SECONDS), is(true));
    ApplicationEvaluationEvent event = handler.getEvent();
    assertThat(event, is(notNullValue()));
    assertThat(event.initiator, is("system"));
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

    String licenseFingerprint = "PolicyMonitorTest_LicenseFingerprint";
    setLicenseFingerprint(licenseFingerprint);
    String scanId = "PolicyMonitorTest_scanId";

    String notifyEmail = "developer@sonatype.com";
    String monitorNotifyEmail1 = "monitor1@sonatype.com";
    String monitorNotifyEmail2 = "monitor2@sonatype.com";
    String monitorNotifyEmail3 = "monitor3@sonatype.com";
    Policy policy1 = createPolicy(app.getId(), "Policy1", stage, notifyEmail, monitorNotifyEmail1);
    Policy policy2 = createPolicy(org.getId(), "Policy2", stage, notifyEmail, monitorNotifyEmail2);
    Policy policy3 = createPolicy(app.getId(), "Policy3", stage, notifyEmail, null /* monitorNotifyEmail */);
    Policy policy4 = createPolicy(parentOrg.getId(), "Policy4", stage, notifyEmail, monitorNotifyEmail3);

    File scanFile = createScanFile(app, scanId);

    // Simulate that the report is available
    mockReport(scanId, "/PolicyMonitorTest/report.zip");

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
    evaluatePolicy(app.getPublicId(), scanId, stage);
    assertNotifications(notificationsMonitor1, 0, 5000);
    assertNotifications(notificationsMonitor2, 0, 0);
    assertNotifications(notificationsMonitor3, 0, 0);
    assertNotifications(notificationsDeveloper, 1, 0);
    notificationsDeveloper.clear();
    PolicyEvaluationDAO policyEvaluationDAO = new PolicyEvaluationDAO();
    PolicyEvaluation policyEvaluation1 = policyEvaluationDAO.getLastByApplicationIdAndStageId(app.getId(),
        stage.getStageTypeId());
    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();
    for (PolicyViolation policyViolation : policyViolationDAO.getActiveByEvaluationId(policyEvaluation1.getId())) {
      assertThat(policyViolation.getActionTypeId(), is(Action.ID_FAIL));
      assertThat(policyViolation.getNotificationsString(), is(notifyEmail));
    }
    assertThat(scanFile.exists(), is(true));

    // Run the policy monitor. There should be a new policy evaluation, but no notifications because nothing changed.
    policyMonitor.run();
    PolicyEvaluation policyEvaluation2 = policyEvaluationDAO.getLastByApplicationIdAndStageId(app.getId(),
        stage.getStageTypeId());
    assertThat(policyEvaluation2.getId(), not(is(policyEvaluation1.getId())));
    assertThat(policyEvaluation2.getTime(), is(greaterThan(policyEvaluation1.getTime())));
    for (PolicyViolation policyViolation : policyViolationDAO.getActiveByEvaluationId(policyEvaluation2.getId())) {
      assertThat(policyViolation.getActionTypeId(), is(nullValue()));
      assertThat(policyViolation.getNotificationsString(), is(nullValue()));
    }
    assertNotifications(notificationsDeveloper, 0, 5000);
    assertNotifications(notificationsMonitor1, 0, 0);
    assertNotifications(notificationsMonitor2, 0, 0);
    assertNotifications(notificationsMonitor3, 0, 0);
    assertThat(scanFile.exists(), is(true));

    // Modify policy3 and run the monitor again. There should be a new policy evaluation, but no notifications
    // because policy3 does not have notifications for monitoring.
    policy3.setName(policy3.getName() + "Updated");
    updatePolicy(OwnerType.APPLICATION, app.getPublicId(), policy3);
    policyMonitor.run();
    PolicyEvaluation policyEvaluation3 = policyEvaluationDAO.getLastByApplicationIdAndStageId(app.getId(),
        stage.getStageTypeId());
    assertThat(policyEvaluation3.getId(), not(is(policyEvaluation2.getId())));
    assertThat(policyEvaluation3.getTime(), is(greaterThan(policyEvaluation2.getTime())));
    for (PolicyViolation policyViolation : policyViolationDAO.getActiveByEvaluationId(policyEvaluation3.getId())) {
      assertThat(policyViolation.getActionTypeId(), is(nullValue()));
      assertThat(policyViolation.getNotificationsString(), is(nullValue()));
    }
    assertNotifications(notificationsDeveloper, 0, 5000);
    assertNotifications(notificationsMonitor1, 0, 0);
    assertNotifications(notificationsMonitor2, 0, 0);
    assertNotifications(notificationsMonitor3, 0, 0);
    assertThat(scanFile.exists(), is(true));

    // Modify policy1 and run the monitor again. Only the first monitor email should receive a notification.
    policy1.setName(policy1.getName() + "Updated");
    updatePolicy(OwnerType.APPLICATION, app.getPublicId(), policy1);
    policyMonitor.run();
    PolicyEvaluation policyEvaluation4 = policyEvaluationDAO.getLastByApplicationIdAndStageId(app.getId(),
        stage.getStageTypeId());
    assertThat(policyEvaluation4.getId(), not(is(policyEvaluation3.getId())));
    assertThat(policyEvaluation4.getTime(), is(greaterThan(policyEvaluation3.getTime())));
    for (PolicyViolation policyViolation : policyViolationDAO.getActiveByEvaluationId(policyEvaluation4.getId())) {
      assertThat(policyViolation.getActionTypeId(), is(nullValue()));
      if (policyViolation.getPolicyId().equals(policy1.getId())) {
        assertThat(policyViolation.getNotificationsString(), is(monitorNotifyEmail1));
      }
      else {
        assertThat(policyViolation.getNotificationsString(), is(nullValue()));
      }
    }
    assertNotifications(notificationsDeveloper, 0, 5000);
    assertNotifications(notificationsMonitor2, 0, 0);
    assertNotifications(notificationsMonitor3, 0, 0);
    assertNotifications(notificationsMonitor1, 1, 0);
    notificationsMonitor1.clear();
    assertThat(scanFile.exists(), is(true));

    // Modify policy2 and run the monitor again. Only the second monitor email should receive a notification.
    policy2.setName(policy2.getName() + "Updated");
    updatePolicy(OwnerType.ORGANIZATION, org.getId(), policy2);
    policyMonitor.run();
    PolicyEvaluation policyEvaluation5 = policyEvaluationDAO.getLastByApplicationIdAndStageId(app.getId(),
        stage.getStageTypeId());
    assertThat(policyEvaluation5.getId(), not(is(policyEvaluation4.getId())));
    assertThat(policyEvaluation5.getTime(), is(greaterThan(policyEvaluation4.getTime())));
    for (PolicyViolation policyViolation : policyViolationDAO.getActiveByEvaluationId(policyEvaluation5.getId())) {
      assertThat(policyViolation.getActionTypeId(), is(nullValue()));
      if (policyViolation.getPolicyId().equals(policy2.getId())) {
        assertThat(policyViolation.getNotificationsString(), is(monitorNotifyEmail2));
      }
      else {
        assertThat(policyViolation.getNotificationsString(), is(nullValue()));
      }
    }
    assertNotifications(notificationsDeveloper, 0, 5000);
    assertNotifications(notificationsMonitor1, 0, 0);
    assertNotifications(notificationsMonitor2, 1, 0);
    assertNotifications(notificationsMonitor3, 0, 0);
    notificationsMonitor2.clear();
    assertThat(scanFile.exists(), is(true));

    // Modify policy4 and run the monitor again. Only the forth monitor email should receive a notification
    policy4.setName(policy4.getName() + "Updated");
    updatePolicy(OwnerType.ORGANIZATION, parentOrg.getId(), policy4);
    policyMonitor.run();
    PolicyEvaluation policyEvaluation6 = policyEvaluationDAO.getLastByApplicationIdAndStageId(app.getId(),
        stage.getStageTypeId());
    assertThat(policyEvaluation6.getId(), not(is(policyEvaluation5.getId())));
    assertThat(policyEvaluation6.getTime(), is(greaterThan(policyEvaluation5.getTime())));
    for (PolicyViolation policyViolation : policyViolationDAO.getActiveByEvaluationId(policyEvaluation6.getId())) {
      assertThat(policyViolation.getActionTypeId(), is(nullValue()));
      if (policyViolation.getPolicyId().equals(policy4.getId())) {
        assertThat(policyViolation.getNotificationsString(), is(monitorNotifyEmail3));
      }
      else {
        assertThat(policyViolation.getNotificationsString(), is(nullValue()));
      }
    }
    assertNotifications(notificationsDeveloper, 0, 5000);
    assertNotifications(notificationsMonitor1, 0, 0);
    assertNotifications(notificationsMonitor2, 0, 0);
    assertNotifications(notificationsMonitor3, 1, 0);
    notificationsMonitor3.clear();
    assertThat(scanFile.exists(), is(true));
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
    Condition condition = new Condition(SecurityVulnerabilityConditionType.ID, "present");
    constraint.addCondition(condition);
    policy.addConstraint(constraint);
    policy.setAction(stage.getStageTypeId(), FailActionType.ID);
    policy.getNotifications().add(new UserNotification(notifyEmail, stage.getStageTypeId()));
    if (monitorNotifyEmail != null) {
      policy.getNotifications().add(new UserNotification(monitorNotifyEmail, Notification.CONTINUOUS_MONITORING));
    }

    return tempEntity.newPolicy(policy);
  }

  private PolicyEvaluationResult evaluatePolicy(String applicationPublicId, String scanId, Stage stage)
  {
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
    assertThat(policyEval, is(notNullValue()));
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
    mockReport(scanId, "/PolicyMonitorTest/report.zip");
    evaluatePolicy(app.getPublicId(), scanId, stage);

    // The scan file does not exist, which will cause a FileNotFoundException in the policy monitoring.
    try {
      policyMonitor.evaluate(app, policyMonitoring);
      fail("Expected exception");
    }
    catch (FileNotFoundException expected) {
      File scanFile = insightWork.getScanFile(app.getId(), scanId);
      assertThat(expected.getMessage(), endsWith(scanFile.getName()));
    }
  }

  @Test
  public void testEvaluate_LatestScanFileReplacedDuringMonitoring() throws Exception {
    Organization org = tempEntity.newOrganization();
    final Application app = tempEntity.newApplication("MonitoredApp", org.getId());
    final Stage stage = new Stage(ReleaseStageType.ID);
    PolicyMonitoring policyMonitoring = tempEntity.newPolicyMonitoring(app.getId(), stage.getStageTypeId());

    String scanId1 = "PolicyMonitorTest_scanId1";
    final File scanFile1 = createScanFile(app, scanId1);

    // Simulate that the report is available and evaluate policies
    mockReport(scanId1, "/PolicyMonitorTest/report.zip");
    evaluatePolicy(app.getPublicId(), scanId1, stage);

    final String scanId2 = "PolicyMonitorTest_scanId2";

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
            createScanFile(app, scanId2);
            mockReport(scanId2, "/PolicyMonitorTest/report.zip");
            evaluatePolicy(app.getPublicId(), scanId2, stage);

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
    assertThat(policyEvaluation.isForMonitoring(), is(true));
    assertThat(policyEvaluation.getScanId(), is(scanId2));
  }

  private File createScanFile(Application app, String scanId) {
    File scanFile = insightWork.getScanFile(app.getId(), scanId);
    scanFile.delete();
    URL testScanFileUrl = getClass().getResource("/PolicyMonitorTest/scan.xml.gz");
    try {
      FileUtils.copyFile(new File(testScanFileUrl.getFile()), scanFile);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }

    return scanFile;
  }
}
