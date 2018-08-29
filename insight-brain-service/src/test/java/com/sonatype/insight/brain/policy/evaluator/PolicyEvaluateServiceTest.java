/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.mail.Message;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.jira.JiraClient;
import com.sonatype.insight.brain.jira.JiraClientFactory;
import com.sonatype.insight.brain.jira.JiraConfig;
import com.sonatype.insight.brain.jira.JiraField;
import com.sonatype.insight.brain.jira.JiraIssueCreateRequest;
import com.sonatype.insight.brain.jira.JiraIssueCreateRequest.JiraIssueCreateResponse;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.notifications.JiraNotification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.report.MockReportDownloader;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportDownloader;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.mock_javamail.Mailbox;
import org.mockito.ArgumentCaptor;

import static com.sonatype.insight.brain.Assert.assertNotifications;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PolicyEvaluateServiceTest
    extends AbstractComponentTest
{
  @Inject
  private PolicyEvaluateService policyEvaluateService;

  private PolicyDAO policyDAO = new PolicyDAO();

  private Application app;

  private JiraClientFactory mockJiraClientFactory;

  private MockReportDownloader mockReportDownloader;

  @Override
  public void configure(Binder binder) {
    mockReportDownloader = new MockReportDownloader();
    binder.bind(ReportDownloader.class).toInstance(mockReportDownloader.getMock());
    mockJiraClientFactory = mock(JiraClientFactory.class);
    binder.bind(JiraClientFactory.class).toInstance(mockJiraClientFactory);
    binder.bind(TelemetrySender.class).toInstance(mock(TelemetrySender.class));

    super.configure(binder);
  }

  @Before
  public void before() throws Exception {
    app = tempEntity.newApplicationWithParent();
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
    assertEquals(isReevaluation, policyEvaluation.isReevaluation());
    assertEquals(isForObsoleteScan, policyEvaluation.isForObsoleteScan());
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

    final Constraint constraint1 = new Constraint("C1", "constraint 1", LogicalOperator.AND);
    final Condition condition1 = new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0");
    constraint1.addCondition(condition1);
    final Policy policy1 = new Policy("P1", "policy1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.getNotifications().add(new UserNotification("manager@example.com", Stage.ID_BUILD));
    policy1.getNotifications().add(new UserNotification("john.doe@example.com", Stage.ID_BUILD));
    policy1.getNotifications().add(new JiraNotification("projectKey1", 1, Stage.ID_BUILD));
    policy1.setAction(Stage.ID_BUILD, Action.ID_FAIL);
    policy1.setOwnerId(app.getId());
    tempEntity.newPolicy(policy1);

    final Constraint constraint2 = new Constraint("C2", "constraint 2", LogicalOperator.AND);
    final Condition condition2 = new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0");
    constraint2.addCondition(condition2);
    // same conditions, but lower threat-level => analysis should show highest threat-level
    final Policy policy2 = new Policy("P2", "policy2");
    policy2.setThreatLevel(3);
    policy2.addConstraint(constraint2);
    policy2.setOwnerId(app.getId());
    policy2.getNotifications().add(new UserNotification("Mark.MyWords@example.com", Stage.ID_RELEASE));
    policy2.getNotifications().add(new JiraNotification("projectKey2", 2, Stage.ID_RELEASE));
    policy2.setAction(Stage.ID_RELEASE, Action.ID_FAIL);
    tempEntity.newPolicy(policy2);

    final Stage stage = new Stage(Stage.ID_BUILD);

    String scanId = simulateReportIsAvailable("report.zip");

    final List<Message> messagesA = Mailbox.get("manager@example.com");
    final List<Message> messagesB = Mailbox.get("john.doe@example.com");

    messagesA.clear();
    messagesB.clear();

    ApplicationComponentDAO appComponentDAO = new ApplicationComponentDAO();
    assertThat(appComponentDAO.getByApplicationIdAndStageTypeId(app.getId(), stage.getStageTypeId()), is(empty()));

    // evaluate policy
    PolicyEvaluationResult policyEvaluationResult = policyEvaluateService.evaluate(app.getPublicId(), scanId, stage);
    assertEquals(7, policyEvaluationResult.getAffectedComponentCount());
    assertEquals(7, policyEvaluationResult.getCriticalComponentCount());
    assertEquals(0, policyEvaluationResult.getSevereComponentCount());
    assertEquals(0, policyEvaluationResult.getModerateComponentCount());
    List<PolicyAlert> policyAlerts = policyEvaluationResult.getAlerts();
    assertEquals(72, policyAlerts.size());
    for (PolicyAlert policyAlert : policyAlerts) {
      AbstractPolicyEvaluationTest.assertFactCounts(1, 1, policyAlert);
    }
    assertPolicyEvaluation(app.getId(), scanId, false /* isReevaluation */);
    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();
    for (PolicyViolation policyViolation : policyViolationDAO.getActiveByApplicationIdAndStageId(app.getId(),
        stage.getStageTypeId())) {
      if (policyViolation.getPolicyId().equals(policy1.getId())) {
        assertThat(policyViolation.getActionTypeId(), is(Action.ID_FAIL));
      }
      else {
        assertThat(policyViolation.getActionTypeId(), is(nullValue()));
      }
    }

    // check the calculated policy threat
    InsightWork insightWork = lookup(InsightWork.class);
    File reportFile = insightWork.getReportFile(app.getId(), scanId);
    ReportEntry policyThreatsReportEntry = Report.getEntry(reportFile, ScanPolicyEvaluator.POLICY_THREATS_FILENAME);
    final JsonNode policyThreats = JsonUtils.parse(policyThreatsReportEntry.buf).get("aaData");
    assertTrue(policyThreats.size() > 0);
    assertEquals(8, policyThreats.get(0).get("policyThreatLevel").asInt());

    // check components are associated with the application and stage
    assertThat(appComponentDAO.getByApplicationIdAndStageTypeId(app.getId(), stage.getStageTypeId()), hasSize(28));

    // notification message should also have been sent
    assertNotifications(messagesA, 1, 5000);
    assertTrue(messagesA.get(0).getSubject().contains("Policy"));
    assertNotifications(messagesB, 1, 5000);
    assertTrue(messagesB.get(0).getSubject().contains("Policy"));

    ArgumentCaptor<JiraIssueCreateRequest> createRequestArgumentCaptor = ArgumentCaptor
        .forClass(JiraIssueCreateRequest.class);
    verify(mockJiraClient, timeout(5000)).createIssue(createRequestArgumentCaptor.capture());
    JiraIssueCreateRequest jiraIssueCreateRequest = createRequestArgumentCaptor.getValue();
    assertThat(jiraIssueCreateRequest.getFields().size(), is(4));
    Map<String, String> projectMeta = jiraIssueCreateRequest.getField(JiraField.PROJECT);
    assertThat(projectMeta.get("key"), is("projectKey1"));

    messagesA.clear();
    messagesB.clear();

    reset(mockJiraClient);

    // evaluate policy again
    policyEvaluationResult = policyEvaluateService.evaluate(app.getPublicId(), scanId, stage);
    policyAlerts = policyEvaluationResult.getAlerts();
    assertEquals(72, policyAlerts.size());
    for (PolicyAlert policyAlert : policyAlerts) {
      AbstractPolicyEvaluationTest.assertFactCounts(1, 1, policyAlert);
    }
    assertPolicyEvaluation(app.getId(), scanId, true /* isReevaluation */);
    for (PolicyViolation policyViolation : policyViolationDAO.getActiveByApplicationIdAndStageId(app.getId(),
        stage.getStageTypeId())) {
      if (policyViolation.getPolicyId().equals(policy1.getId())) {
        assertThat(policyViolation.getActionTypeId(), is(Action.ID_FAIL));
      }
      else {
        assertThat(policyViolation.getActionTypeId(), is(nullValue()));
      }
    }

    // notification message should not have been sent since the results are the same
    assertNotifications(messagesA, 0, 5000);
    assertNotifications(messagesB, 0, 1000);

    verify(mockJiraClient, times(0)).createIssue(any(JiraIssueCreateRequest.class));
  }

  @Test
  public void testEvaluate_PolicyThreatLevelCounts() throws Exception {
    final Constraint constraint = new Constraint("C1", "PolicyThreatCountResourceTest constraint 1",
        LogicalOperator.AND);
    final Condition condition = new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0");
    constraint.addCondition(condition);
    Policy policy = new Policy("P1", "PolicyThreatCountResourceTest policy1");
    policy.setThreatLevel(1);
    policy.addConstraint(constraint);
    policy.setOwnerId(app.getId());
    tempEntity.newPolicy(policy);

    final Stage stage = new Stage(Stage.ID_BUILD);

    String scanId = simulateReportIsAvailable("report.zip");

    PolicyEvaluationResult policyEvaluationResult = policyEvaluateService.evaluate(app.getPublicId(), scanId, stage);

    // Threat Level 1 Should not show up in any counts
    assertEquals(0, policyEvaluationResult.getAffectedComponentCount());
    assertEquals(0, policyEvaluationResult.getCriticalComponentCount());
    assertEquals(0, policyEvaluationResult.getSevereComponentCount());
    assertEquals(0, policyEvaluationResult.getModerateComponentCount());
    assertEquals(0, policyEvaluationResult.getCriticalPolicyViolationCount());
    assertEquals(0, policyEvaluationResult.getSeverePolicyViolationCount());
    assertEquals(0, policyEvaluationResult.getModeratePolicyViolationCount());
    assertEquals(0, policyEvaluationResult.getGrandfatheredPolicyViolationCount());

    policy.setThreatLevel(2);
    policyDAO.update(policy);
    scanId = simulateReportIsAvailable("report.zip");

    // Threat Level 2 should show up as moderate
    policyEvaluationResult = policyEvaluateService.evaluate(app.getPublicId(), scanId, stage);
    assertEquals(7, policyEvaluationResult.getAffectedComponentCount());
    assertEquals(0, policyEvaluationResult.getCriticalComponentCount());
    assertEquals(0, policyEvaluationResult.getSevereComponentCount());
    assertEquals(7, policyEvaluationResult.getModerateComponentCount());
    assertEquals(0, policyEvaluationResult.getCriticalPolicyViolationCount());
    assertEquals(0, policyEvaluationResult.getSeverePolicyViolationCount());
    assertEquals(36, policyEvaluationResult.getModeratePolicyViolationCount());
    assertEquals(0, policyEvaluationResult.getGrandfatheredPolicyViolationCount());

    policy.setThreatLevel(4);
    policyDAO.update(policy);
    scanId = simulateReportIsAvailable("report.zip");

    // Threat Level 4 should show up as severe
    policyEvaluationResult = policyEvaluateService.evaluate(app.getPublicId(), scanId, stage);
    assertEquals(7, policyEvaluationResult.getAffectedComponentCount());
    assertEquals(0, policyEvaluationResult.getCriticalComponentCount());
    assertEquals(7, policyEvaluationResult.getSevereComponentCount());
    assertEquals(0, policyEvaluationResult.getModerateComponentCount());
    assertEquals(0, policyEvaluationResult.getCriticalPolicyViolationCount());
    assertEquals(36, policyEvaluationResult.getSeverePolicyViolationCount());
    assertEquals(0, policyEvaluationResult.getModeratePolicyViolationCount());
    assertEquals(0, policyEvaluationResult.getGrandfatheredPolicyViolationCount());

    policy.setThreatLevel(8);
    policyDAO.update(policy);
    scanId = simulateReportIsAvailable("report.zip");

    // Threat Level 8 should show up as severe
    policyEvaluationResult = policyEvaluateService.evaluate(app.getPublicId(), scanId, stage);
    assertEquals(7, policyEvaluationResult.getAffectedComponentCount());
    assertEquals(7, policyEvaluationResult.getCriticalComponentCount());
    assertEquals(0, policyEvaluationResult.getSevereComponentCount());
    assertEquals(0, policyEvaluationResult.getModerateComponentCount());
    assertEquals(36, policyEvaluationResult.getCriticalPolicyViolationCount());
    assertEquals(0, policyEvaluationResult.getSeverePolicyViolationCount());
    assertEquals(0, policyEvaluationResult.getModeratePolicyViolationCount());
    assertEquals(0, policyEvaluationResult.getGrandfatheredPolicyViolationCount());

    // Grandfather one violation
    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();
    PolicyViolation policyViolation = policyViolationDAO
        .getActiveByApplicationIdAndStageId(app.getId(), stage.getStageTypeId()).get(0);
    policyViolation.setGrandfatherTime(new Date());
    policyViolationDAO.update(policyViolation);
    scanId = simulateReportIsAvailable("report.zip");
    policyEvaluationResult = policyEvaluateService.evaluate(app.getPublicId(), scanId, stage);
    assertEquals(7, policyEvaluationResult.getAffectedComponentCount());
    assertEquals(7, policyEvaluationResult.getCriticalComponentCount());
    assertEquals(0, policyEvaluationResult.getSevereComponentCount());
    assertEquals(0, policyEvaluationResult.getModerateComponentCount());
    assertEquals(35, policyEvaluationResult.getCriticalPolicyViolationCount());
    assertEquals(0, policyEvaluationResult.getSeverePolicyViolationCount());
    assertEquals(0, policyEvaluationResult.getModeratePolicyViolationCount());
    assertEquals(1, policyEvaluationResult.getGrandfatheredPolicyViolationCount());
  }

  @Test
  public void testEvaluate_NotificationEmailModel() throws Exception {
    final Constraint constraint1 = new Constraint("C1", "constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "5"));
    final Policy policy1 = new Policy("P1", "policy1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.setOwnerId(app.getId());
    tempEntity.newPolicy(policy1);

    final Constraint constraint2 = new Constraint("C2", "constraint 2", LogicalOperator.AND);
    constraint2.addCondition(new Condition(CoordinatesConditionType.ID, "match", "maven:tomcat"));
    final Policy policy2 = new Policy("P2", "policy2");
    policy2.setThreatLevel(4);
    policy2.addConstraint(constraint2);
    policy2.setOwnerId(app.getId());
    tempEntity.newPolicy(policy2);

    final Constraint constraint3 = new Constraint("C3", "constraint 3", LogicalOperator.AND);
    constraint3.addCondition(new Condition(CoordinatesConditionType.ID, "match", "maven:org.*"));
    final Policy policy3 = new Policy("P3", "policy3");
    policy3.setThreatLevel(3);
    policy3.addConstraint(constraint3);
    policy3.setOwnerId(app.getId());
    tempEntity.newPolicy(policy3);

    final Constraint constraint4 = new Constraint("C4", "constraint 1", LogicalOperator.AND);
    constraint4.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, "<", "5"));
    final Policy policy4 = new Policy("P4", "policy4");
    policy4.setThreatLevel(0);
    policy4.addConstraint(constraint4);
    policy4.setOwnerId(app.getId());
    tempEntity.newPolicy(policy4);

    final Stage stage = new Stage(Stage.ID_BUILD);

    String scanId = simulateReportIsAvailable("report.zip");

    PolicyEvaluationResult policyEvaluationResult = policyEvaluateService.evaluate(app.getPublicId(), scanId, stage);

    List<PolicyFact> policyFacts = new ArrayList<>();
    for (PolicyAlert policyAlert : policyEvaluationResult.getAlerts()) {
      policyFacts.add(policyAlert.getTrigger());
    }

    app.setContactInternalName(User.ADMIN_USERNAME);
    new ApplicationDAO().update(app);

    PolicyAlertEmailer emailer = lookup(PolicyAlertEmailer.class);

    String serverUrl = "http://localhost/";
    Map<String, Object> model = emailer.createPolicyMailModel(serverUrl, app, scanId, stage, policyFacts, 8);
    assertEquals(policyFacts, model.get("policyFacts"));
    assertEquals("http://cdn.sonatype.com/", model.get("cdnUrl"));
    assertEquals(serverUrl + UserInterfaceLinksResource.getReportUrl(app.getPublicId(), scanId),
        model.get("detailedReportUrl"));
    assertEquals(18, model.get("policyThreatRedCount"));
    assertEquals(3, model.get("policyThreatOrangeCount"));
    assertEquals(13, model.get("policyThreatYellowCount"));
    assertEquals(18, model.get("policyThreatBlueCount"));
    assertEquals("Build", model.get("policyThreatStage"));
    assertEquals(app.getPublicId(), model.get("policyThreatApp"));
    assertEquals("Admin BuiltIn", model.get("applicationContactName"));
    assertEquals("admin@localhost", model.get("applicationContactEmail"));
    assertNotNull(model.get("policyThreatTime"));
    assertEquals("APP ID", model.get("ownerIdLabel"));
    assertEquals(8, model.get("grandfatheredPolicyViolationCount"));
  }

  @Test
  public void testEvaluate_ReEvaluateNotifications() throws Exception {
    Constraint constraint = new Constraint("C1", "constraint 1", LogicalOperator.AND);
    Condition condition = new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0");
    constraint.addCondition(condition);
    Policy policy = new Policy("P1", "policy1");
    policy.setThreatLevel(8);
    policy.addConstraint(constraint);
    policy.getNotifications().add(new UserNotification("manager@test.corp", Stage.ID_BUILD));
    policy.setOwnerId(app.getId());
    tempEntity.newPolicy(policy);

    Stage stage = new Stage(Stage.ID_BUILD);

    InsightConfig insightConfig = lookup(InsightConfig.class);
    insightConfig.setBaseUrl("http://localhost");

    List<Message> notifications = Mailbox.get("manager@test.corp");
    notifications.clear();

    // Evaluate policy
    String scanId = simulateReportIsAvailable("report.zip");
    PolicyEvaluationResult policyEvaluationResult = policyEvaluateService.evaluate(app.getPublicId(), scanId, stage);

    List<PolicyAlert> policyAlerts = policyEvaluationResult.getAlerts();
    assertEquals(36, policyAlerts.size());
    assertPolicyEvaluation(app.getId(), scanId, false /* isReevaluation */);

    // Notification message should have been sent
    assertNotifications(notifications, 1, 5000);
    notifications.clear();

    // Change the policy name
    policy.setName(policy.getName() + "Updated");
    policyDAO.update(policy);

    // Evaluate policy again for the same scan
    policyEvaluationResult = policyEvaluateService.evaluate(app.getPublicId(), scanId, stage);
    assertEquals(36, policyAlerts.size());
    assertPolicyEvaluation(app.getId(), scanId, true /* isReevaluation */);

    // Notification message should not have been sent since this is a re-evaluation
    assertNotifications(notifications, 0, 5000);
  }

  /**
   * Simulates that a report (based on the specified resource) exists.
   * 
   * @param reportResourceName can be a report.zip file or a directory that will be zipped up into a report.
   * 
   * @return A generated scan ID that can be used in subsequent calls to evaluate policies.
   */
  private String simulateReportIsAvailable(String reportResourceName) {
    return mockReportDownloader.mockDownloadReport("/PolicyEvaluateServiceTest/" + reportResourceName);
  }
}
