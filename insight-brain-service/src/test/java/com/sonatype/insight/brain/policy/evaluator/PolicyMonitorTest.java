/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.mail.Message;

import com.sonatype.clm.dto.model.policy.NotifyAction;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.policy.PolicyResource;
import com.sonatype.insight.brain.service.AbstractLicenseTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.IdUtils;

import com.ning.http.client.Response;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.codehaus.plexus.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.mock_javamail.Mailbox;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class PolicyMonitorTest
    extends AbstractLicenseTest
{
  private PolicyMonitor policyMonitor;

  private InsightWork insightWork;

  private InsightConfig insightConfig;

  @Before
  public void setup() {
    insightConfig = brain.getInjector().getInstance(InsightConfig.class);
    insightConfig.setBaseUrl("http://clm.sonatype.com/test");
    insightWork = brain.getInjector().getInstance(InsightWork.class);
    policyMonitor = brain.getInjector().getInstance(PolicyMonitor.class);
  }
  
  @After
  public void cleanup() {
    getTestProductLicenseManager().resetProducts();
  }

  @Test
  public void testApplicationNotMonitoredWhenUnlicensed() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication("MonitoredApp", org.getId());

    Stage stage = new Stage(ReleaseStageType.ID);

    PolicyMonitoring policyMonitoring = new PolicyMonitoring(app.getId(), stage.getStageTypeId());
    new PolicyMonitoringDAO().insert(policyMonitoring);

    String licenseFingerprint = "PolicyMonitorTest_LicenseFingerprint";
    setLicenseFingerprint(licenseFingerprint);
    String scanId = "PolicyMonitorTest_scanId";
    File saasReportFile = getReportResponseFile(licenseFingerprint, scanId);
    saasReportFile.delete();
    File scanFile = insightWork.getScanFile(app.getId(), scanId);
    scanFile.delete();

    // Create the scan file
    URL testScanFileUrl = getClass().getResource("/PolicyMonitorTest/scan.xml.gz");
    FileUtils.copyFile(new File(testScanFileUrl.getFile()), scanFile);

    // Simulate that the report is available
    URL testReportFileUrl = getClass().getResource("/PolicyMonitorTest/report.zip");
    FileUtils.copyFile(new File(testReportFileUrl.getFile()), saasReportFile);

    evaluatePolicy(app.getPublicId(), scanId, stage);

    setLicenseProducts(new String[0]);

    Collection<StageType> stageTypes = StageTypes.getAll();

    Map<StageType, Date> lastRun = new HashMap<StageType, Date>();
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
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(monitoredApp.getId(), ReleaseStageType.ID);
    new PolicyMonitoringDAO().insert(policyMonitoring);

    Application notMonitoredApp = tempEntity.newApplication("NotMonitoredApp", org.getId());
    // Seed policy evaluations for all stages. These should be the last evaluations after we run the policy monitoring,
    // i.e. no re-evaluations happened.
    Map<StageType, PolicyEvaluation> policyEvaluations = new LinkedHashMap<>();
    for (StageType stageType : StageTypes.getAll()) {
      PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(notMonitoredApp.getId(), stageType.getId(),
          "fakeScanId");
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
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(app.getId(), ReleaseStageType.ID);
    new PolicyMonitoringDAO().insert(policyMonitoring);

    policyMonitor.run();

    // There should be no policy evaluations
    PolicyEvaluationDAO policyEvaluationDAO = new PolicyEvaluationDAO();
    for (StageType stageType : StageTypes.getAll()) {
      assertThat(policyEvaluationDAO.getLastByApplicationIdAndStageId(app.getId(), stageType.getId()), is(nullValue()));
    }
  }

  @Test
  public void testApplicationMonitored() throws Exception {
    testMonitored(IdUtils.TYPE_APPLICATION);
  }

  @Test
  public void testOrganizationMonitored() throws Exception {
    testMonitored(IdUtils.TYPE_ORGANIZATION);
  }

  private void testMonitored(String monitorOwnerType) throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication("MonitoredApp", org.getId());

    Stage stage = new Stage(ReleaseStageType.ID);

    PolicyMonitoring policyMonitoring;
    if (IdUtils.TYPE_APPLICATION.equals(monitorOwnerType)) {
      policyMonitoring = new PolicyMonitoring(app.getId(), stage.getStageTypeId());
    }
    else {
      policyMonitoring = new PolicyMonitoring(org.getId(), stage.getStageTypeId());
    }
    new PolicyMonitoringDAO().insert(policyMonitoring);

    String licenseFingerprint = "PolicyMonitorTest_LicenseFingerprint";
    setLicenseFingerprint(licenseFingerprint);
    String scanId = "PolicyMonitorTest_scanId";
    File saasReportFile = getReportResponseFile(licenseFingerprint, scanId);
    saasReportFile.delete();
    File scanFile = insightWork.getScanFile(app.getId(), scanId);
    scanFile.delete();

    String notifyEmail = "developer@sonatype.com";
    String monitorNotifyEmail1 = "monitor1@sonatype.com";
    String monitorNotifyEmail2 = "monitor2@sonatype.com";
    Policy policy1 = createPolicy(IdUtils.TYPE_APPLICATION, app.getPublicId(), "Policy1", stage, notifyEmail,
        monitorNotifyEmail1);
    Policy policy2 = createPolicy(IdUtils.TYPE_ORGANIZATION, org.getId(), "Policy2", stage, notifyEmail,
        monitorNotifyEmail2);
    Policy policy3 = createPolicy(IdUtils.TYPE_APPLICATION, app.getPublicId(), "Policy3", stage, notifyEmail, null /* monitorNotifyEmail */);

    // Create the scan file
    URL testScanFileUrl = getClass().getResource("/PolicyMonitorTest/scan.xml.gz");
    FileUtils.copyFile(new File(testScanFileUrl.getFile()), scanFile);

    // Simulate that the report is available
    URL testReportFileUrl = getClass().getResource("/PolicyMonitorTest/report.zip");
    FileUtils.copyFile(new File(testReportFileUrl.getFile()), saasReportFile);

    // Prepare to receive email notifications
    List<Message> notificationsDeveloper = Mailbox.get(notifyEmail);
    List<Message> notificationsMonitor1 = Mailbox.get(monitorNotifyEmail1);
    List<Message> notificationsMonitor2 = Mailbox.get(monitorNotifyEmail2);
    notificationsDeveloper.clear();
    notificationsMonitor1.clear();
    notificationsMonitor2.clear();

    // Evaluate the policy. Only the developer should receive a notification.
    evaluatePolicy(app.getPublicId(), scanId, stage);
    assertThat(notificationsDeveloper, hasSize(1));
    notificationsDeveloper.clear();
    assertThat(notificationsMonitor1, is(empty()));
    assertThat(notificationsMonitor2, is(empty()));
    PolicyEvaluationDAO policyEvaluationDAO = new PolicyEvaluationDAO();
    PolicyEvaluation policyEvaluation1 = policyEvaluationDAO.getLastByApplicationIdAndStageId(app.getId(),
        stage.getStageTypeId());

    // Run the policy monitor. There should be a new policy evaluation, but no notifications because nothing changed.
    policyMonitor.run();
    PolicyEvaluation policyEvaluation2 = policyEvaluationDAO.getLastByApplicationIdAndStageId(app.getId(),
        stage.getStageTypeId());
    assertThat(policyEvaluation2.getId(), not(is(policyEvaluation1.getId())));
    assertThat(policyEvaluation2.getTime(), is(greaterThan(policyEvaluation1.getTime())));
    assertThat(notificationsDeveloper, is(empty()));
    assertThat(notificationsMonitor1, is(empty()));
    assertThat(notificationsMonitor2, is(empty()));

    // Modify policy3 and run the monitor again. There should be a new policy evaluation, but no notifications
    // because policy3 does not have notifications for monitoring.
    policy3.setName(policy3.getName() + "Updated");
    updatePolicy(IdUtils.TYPE_APPLICATION, app.getPublicId(), policy3);
    policyMonitor.run();
    PolicyEvaluation policyEvaluation3 = policyEvaluationDAO.getLastByApplicationIdAndStageId(app.getId(),
        stage.getStageTypeId());
    assertThat(policyEvaluation3.getId(), not(is(policyEvaluation2.getId())));
    assertThat(policyEvaluation3.getTime(), is(greaterThan(policyEvaluation2.getTime())));
    assertThat(notificationsDeveloper, is(empty()));
    assertThat(notificationsMonitor1, is(empty()));
    assertThat(notificationsMonitor2, is(empty()));

    // Modify policy1 and run the monitor again. Only the first monitor email should receive a notification.
    policy1.setName(policy1.getName() + "Updated");
    updatePolicy(IdUtils.TYPE_APPLICATION, app.getPublicId(), policy1);
    policyMonitor.run();
    PolicyEvaluation policyEvaluation4 = policyEvaluationDAO.getLastByApplicationIdAndStageId(app.getId(),
        stage.getStageTypeId());
    assertThat(policyEvaluation4.getId(), not(is(policyEvaluation3.getId())));
    assertThat(policyEvaluation4.getTime(), is(greaterThan(policyEvaluation3.getTime())));
    assertThat(notificationsDeveloper, is(empty()));
    assertThat(notificationsMonitor1, hasSize(1));
    notificationsMonitor1.clear();
    assertThat(notificationsMonitor2, is(empty()));

    // Modify policy2 and run the monitor again. Only the second monitor email should receive a notification.
    policy2.setName(policy2.getName() + "Updated");
    updatePolicy(IdUtils.TYPE_ORGANIZATION, org.getId(), policy2);
    policyMonitor.run();
    PolicyEvaluation policyEvaluation5 = policyEvaluationDAO.getLastByApplicationIdAndStageId(app.getId(),
        stage.getStageTypeId());
    assertThat(policyEvaluation5.getId(), not(is(policyEvaluation4.getId())));
    assertThat(policyEvaluation5.getTime(), is(greaterThan(policyEvaluation4.getTime())));
    assertThat(notificationsDeveloper, is(empty()));
    assertThat(notificationsMonitor1, is(empty()));
    assertThat(notificationsMonitor2, hasSize(1));
    notificationsMonitor2.clear();
  }

  @Test
  public void testPreviousResultsCorrupted() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication("MonitoredApp", org.getId());

    Stage stage = new Stage(ReleaseStageType.ID);

    PolicyMonitoring policyMonitoring = new PolicyMonitoring(app.getId(), stage.getStageTypeId());
    new PolicyMonitoringDAO().insert(policyMonitoring);

    String licenseFingerprint = "PolicyMonitorTest_LicenseFingerprint";
    setLicenseFingerprint(licenseFingerprint);
    String scanId = "PolicyMonitorTest_scanId";
    File saasReportFile = getReportResponseFile(licenseFingerprint, scanId);
    saasReportFile.delete();
    File scanFile = insightWork.getScanFile(app.getId(), scanId);
    scanFile.delete();

    String notifyEmail = "developer@sonatype.com";
    String monitorNotifyEmail1 = "monitor1@sonatype.com";
    String monitorNotifyEmail2 = "monitor2@sonatype.com";
    Policy policy1 = createPolicy(IdUtils.TYPE_APPLICATION, app.getPublicId(), "Policy1", stage, notifyEmail,
        monitorNotifyEmail1);
    createPolicy(IdUtils.TYPE_ORGANIZATION, org.getId(), "Policy2", stage, notifyEmail, monitorNotifyEmail2);

    // Create the scan file
    URL testScanFileUrl = getClass().getResource("/PolicyMonitorTest/scan.xml.gz");
    FileUtils.copyFile(new File(testScanFileUrl.getFile()), scanFile);

    // Simulate that the report is available
    URL testReportFileUrl = getClass().getResource("/PolicyMonitorTest/report.zip");
    FileUtils.copyFile(new File(testReportFileUrl.getFile()), saasReportFile);

    // Prepare to receive email notifications
    List<Message> notificationsDeveloper = Mailbox.get(notifyEmail);
    List<Message> notificationsMonitor1 = Mailbox.get(monitorNotifyEmail1);
    List<Message> notificationsMonitor2 = Mailbox.get(monitorNotifyEmail2);
    notificationsDeveloper.clear();
    notificationsMonitor1.clear();
    notificationsMonitor2.clear();

    // Evaluate the policy. Only the developer should receive a notification.
    evaluatePolicy(app.getPublicId(), scanId, stage);
    assertThat(notificationsDeveloper, hasSize(1));
    notificationsDeveloper.clear();
    assertThat(notificationsMonitor1, is(empty()));
    assertThat(notificationsMonitor2, is(empty()));

    // Simulate that the first alerts are corrupted
    PolicyEvaluationDAO policyEvaluationDAO = new PolicyEvaluationDAO();
    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();
    PolicyEvaluation policyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndStageId(app.getId(),
        stage.getStageTypeId());
    PolicyViolation policyViolation = new PolicyViolation(policyEvaluation.getId(), policy1.getId(), policy1.getName(),
        policy1.getThreatLevel(), PolicyThreatCategory.OTHER, null, null, null, null, "invalid constraint facts json");
    policyViolationDAO.insert(policyViolation);

    // Run the policy monitor. There should be new notifications because the previous alerts could not be loaded.
    policyMonitor.run();
    assertThat(notificationsDeveloper, is(empty()));
    assertThat(notificationsMonitor1, hasSize(1));
    notificationsMonitor1.clear();
    assertThat(notificationsMonitor2, hasSize(1));
    notificationsMonitor2.clear();

    // Simulate that the previous alerts are corrupted
    policyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndStageId(app.getId(), stage.getStageTypeId());
    policyViolation = new PolicyViolation(policyEvaluation.getId(), policy1.getId(), policy1.getName(),
        policy1.getThreatLevel(), PolicyThreatCategory.OTHER, null, null, null, null, "invalid constraint facts json");
    policyViolationDAO.insert(policyViolation);

    // Run the policy monitor. There should be new notifications because the previous alerts could not be loaded.
    policyMonitor.run();
    assertThat(notificationsDeveloper, is(empty()));
    assertThat(notificationsMonitor1, hasSize(1));
    notificationsMonitor1.clear();
    assertThat(notificationsMonitor2, hasSize(1));
    notificationsMonitor2.clear();
  }

  private Policy createPolicy(String ownerType, String ownerId, String policyName, Stage stage, String notifyEmail,
      String monitorNotifyEmail) throws Exception
  {
    Policy policy = new Policy(null /* id */, policyName);
    policy.setThreatLevel(8);
    Constraint constraint = new Constraint(null /* id */, "Constraint", LogicalOperator.AND);
    Condition condition = new Condition(SecurityVulnerabilityConditionType.ID, "present");
    constraint.addCondition(condition);
    policy.addConstraint(constraint);
    NotifyAction notifyAction = new NotifyAction();
    notifyAction.setTarget(notifyEmail);
    policy.addAction(stage.getStageTypeId(), notifyAction);
    if (monitorNotifyEmail != null) {
      NotifyAction monitorNotifyAction = new NotifyAction();
      monitorNotifyAction.setTarget(monitorNotifyEmail);
      policy.addMonitorNotifyAction(monitorNotifyAction);
    }
    return addPolicy(ownerType, ownerId, policy);
  }

  private PolicyEvaluationResult evaluatePolicy(String applicationPublicId, String scanId, Stage stage)
      throws Exception
  {
    Response response = AuthedRestAccess.post(getRestUrl(PolicyEvaluateResource.SERVICE_PATH, applicationPublicId)
        + "?scanId=" + scanId, JsonHelpers.asJson(stage));
    assertResponseStatus(200, response);
    PolicyEvaluationResult policyEval = JsonHelpers.fromJson(response.getResponseBody(), PolicyEvaluationResult.class);
    assertThat(policyEval, is(notNullValue()));
    return policyEval;
  }

  private Policy addPolicy(String ownerType, String ownerId, Policy policy) throws Exception {
    Response response = AuthedRestAccess.post(getRestUrl(PolicyResource.SERVICE_PATH, ownerType, ownerId),
        JsonHelpers.asJson(policy));
    assertResponseStatus(200, response);
    return fromJson(response, Policy.class);
  }

  private Policy updatePolicy(String ownerType, String ownerId, Policy policy) throws Exception {
    Response response = AuthedRestAccess.put(getRestUrl(PolicyResource.SERVICE_PATH, ownerType, ownerId),
        JsonHelpers.asJson(policy));
    assertResponseStatus(200, response);
    return fromJson(response, Policy.class);
  }
}
