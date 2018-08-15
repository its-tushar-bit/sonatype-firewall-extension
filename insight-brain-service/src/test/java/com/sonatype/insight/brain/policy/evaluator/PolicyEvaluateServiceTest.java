/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.mail.Message;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
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
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.InvalidStageException;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.conditions.AgeInDaysConditionType;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseStatusConditionType;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityStatusConditionType;
import com.sonatype.insight.brain.model.policy.notifications.JiraNotification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.report.MockReportDownloader;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportDownloader;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.mock_javamail.Mailbox;
import org.mockito.ArgumentCaptor;

import static com.sonatype.insight.brain.Assert.assertNotifications;
import static com.sonatype.insight.brain.policy.evaluator.AbstractPolicyEvaluationTest.assertContainsPolicyAlert;
import static com.sonatype.insight.brain.policy.evaluator.AbstractPolicyEvaluationTest.assertNotContainsPolicyAlert;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
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

  private final ComponentIdentifier GERONIMO_TOMCAT_1_0 = ComponentIdentifier.createMavenCoordinates("geronimo",
      "geronimo-tomcat", "1.0");

  private final ComponentIdentifier COMMONS_DBCP_1_4 = ComponentIdentifier.createMavenCoordinates("commons-dbcp",
      "commons-dbcp", "1.4");

  private final ComponentIdentifier TOMCAT_UTIL_5_5_23 = ComponentIdentifier.createMavenCoordinates("tomcat",
      "tomcat-util", "5.5.23");

  private static final ComponentIdentifier COMMONS_POOL_ID = ComponentIdentifier.createMavenCoordinates("commons-pool",
      "commons-pool", "1.4");

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

  @Test
  public void testEvaluate_MultipleMatchesForSameGAV() throws Exception {
    Constraint constraintLicense = new Constraint(null /* constraintId */, "Constraint License", LogicalOperator.AND);
    Condition condition1 = new Condition(LicenseConditionType.ID, "is", "UNSPECIFIED");
    constraintLicense.addCondition(condition1);
    Constraint constraintSV = new Constraint(null /* constraintId */, "Constraint SV", LogicalOperator.AND);
    Condition condition2 = new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0");
    constraintSV.addCondition(condition2);

    Policy policy = new Policy(null /* policyId */, "Policy 1");
    policy.setThreatLevel(5);
    policy.addConstraint(constraintLicense);
    policy.addConstraint(constraintSV);
    policy.setAction(Stage.ID_BUILD, Action.ID_FAIL);
    policy.setOwnerId(app.getId());
    tempEntity.newPolicy(policy);
    constraintLicense = policy.getConstraints().get(0);
    constraintSV = policy.getConstraints().get(1);

    Stage stage = new Stage(Stage.ID_BUILD);

    String scanId = simulateReportIsAvailable("MultipleMatchesForSameGAV/report");
    
    PolicyEvaluationResult policyEvaluationResult = policyEvaluateService.evaluate(app.getPublicId(), scanId, stage);
    
    assertEquals(3, policyEvaluationResult.getAffectedComponentCount());
    assertEquals(0, policyEvaluationResult.getCriticalComponentCount());
    assertEquals(3, policyEvaluationResult.getSevereComponentCount());
    assertEquals(0, policyEvaluationResult.getModerateComponentCount());
    List<PolicyAlert> policyAlerts = policyEvaluationResult.getAlerts();
    assertEquals(9, policyAlerts.size());
    for (PolicyAlert policyAlert : policyAlerts) {
      AbstractPolicyEvaluationTest.assertFactCounts(1, 1, policyAlert);
    }
    Component expectedComponentExact = ComponentFactory.forGav("tomcat", "tomcat-util", "5.0.28", MatchState.EXACT);
    expectedComponentExact.setHash("3102cdd0edd5a05afe00");
    Component expectedComponentSimilar1 = ComponentFactory
        .forGav("tomcat", "tomcat-util", "5.0.28", MatchState.SIMILAR);
    expectedComponentSimilar1.setHash("d29a75f9056e0b040f09");
    Component expectedComponentSimilar2 = ComponentFactory
        .forGav("tomcat", "tomcat-util", "5.0.28", MatchState.SIMILAR);
    expectedComponentSimilar2.setHash("707df42012875442b9df");
    assertContainsPolicyAlert(expectedComponentExact, policy, constraintLicense, Action.ID_FAIL,
        LicenseConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(expectedComponentExact, policy, constraintSV, Action.ID_FAIL,
        SecurityVulnerabilitySeverityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(expectedComponentSimilar1, policy, constraintLicense, Action.ID_FAIL,
        LicenseConditionType.ID, policyAlerts);
    // Verify that the SVs are associated with components by hash, not by component identifier.
    // If SVs were associated with components by component identifier, this component would have a policy violation for
    // an SV because it has the same identifier as expectedComponentExact, which has a violation for an SV.
    assertNotContainsPolicyAlert(expectedComponentSimilar1, policy, constraintSV, Action.ID_FAIL,
        SecurityVulnerabilitySeverityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(expectedComponentSimilar2, policy, constraintLicense, Action.ID_FAIL,
        LicenseConditionType.ID, policyAlerts);
    // Verify that the SVs are associated with components by hash, not by component identifier.
    // If SVs were associated with components by component identifier, this component would have a policy violation for
    // an SV because it has the same identifier as expectedComponentExact, which has a violation for an SV.
    assertNotContainsPolicyAlert(expectedComponentSimilar2, policy, constraintSV, Action.ID_FAIL,
        SecurityVulnerabilitySeverityConditionType.ID, policyAlerts);
  }

  @Test
  public void testEvaluate_ManuallyIdentifiedComponent() throws Exception {
    Constraint constraint = new Constraint(null /* constraintId */, "Constraint 1", LogicalOperator.AND);
    constraint.addCondition(new Condition(MatchStateConditionType.ID, "is", "exact"));
    constraint.addCondition(new Condition(AgeInDaysConditionType.ID, "younger than", "30"));

    Policy policy = new Policy(null /* policyId */, "Policy 1");
    policy.setThreatLevel(5);
    policy.addConstraint(constraint);
    policy.setAction(Stage.ID_BUILD, Action.ID_FAIL);
    policy.setOwnerId(app.getId());
    tempEntity.newPolicy(policy);
    constraint = policy.getConstraints().get(0);

    Stage stage = new Stage(Stage.ID_BUILD);

    String hash = "5801a1a27a36f88e2089";
    String groupId = "G";
    String artifactId = "A";
    String version = "V";
    tempEntity.newClaimedComponent(hash, ComponentIdentifier.createMavenCoordinates(groupId, artifactId, version));

    String scanId = simulateReportIsAvailable("ManuallyIdentifiedComponent/report.zip");
    
    PolicyEvaluationResult policyEvaluationResult = policyEvaluateService.evaluate(app.getPublicId(), scanId, stage);

    assertEquals(1, policyEvaluationResult.getAffectedComponentCount());
    assertEquals(0, policyEvaluationResult.getCriticalComponentCount());
    assertEquals(1, policyEvaluationResult.getSevereComponentCount());
    assertEquals(0, policyEvaluationResult.getModerateComponentCount());
    List<PolicyAlert> policyAlerts = policyEvaluationResult.getAlerts();
    assertEquals(1, policyAlerts.size());
    AbstractPolicyEvaluationTest.assertFactCounts(1, 1, policyAlerts.get(0));
    Component expectedComponentExact = ComponentFactory.forGav(groupId, artifactId, version, MatchState.EXACT);
    expectedComponentExact.setHash(hash);
    assertContainsPolicyAlert(expectedComponentExact, policy, constraint, Action.ID_FAIL, MatchStateConditionType.ID,
        policyAlerts);
  }

  @Test
  public void testEvaluate_Label_DefinedAtAppLevel() throws Exception {
    testEvaluate_Label(false, false);
  }

  @Test
  public void testEvaluate_Label_DefinedAtOrgLevel_AppliedAtOrgLevel() throws Exception {
    testEvaluate_Label(true, true);
  }

  @Test
  public void testEvaluate_Label_DefinedAtOrgLevel_AppliedAtAppLevel() throws Exception {
    testEvaluate_Label(true, false);
  }

  private void testEvaluate_Label(boolean orgLabel, boolean orgComponentLabel) throws Exception {
    String hash = "1249e25aebb15358bedd";
    Label label = tempEntity.newLabel(orgLabel ? app.getOrganizationId() : app.getId(), "red");
    tempEntity.newComponentLabel(orgComponentLabel ? app.getOrganizationId() : app.getId(), label.getId(), hash);

    Constraint constraint = new Constraint(null /* constraintId */, "Constraint 1", LogicalOperator.AND);
    constraint.addCondition(new Condition(LabelConditionType.ID, "is", label.getId()));
    Policy policy = new Policy(null /* policyId */, "Policy 1");
    policy.setThreatLevel(5);
    policy.addConstraint(constraint);
    policy.setAction(Stage.ID_BUILD, Action.ID_FAIL);
    policy.setOwnerId(app.getId());
    tempEntity.newPolicy(policy);
    constraint = policy.getConstraints().get(0);

    Stage stage = new Stage(Stage.ID_BUILD);

    String groupId = "tomcat";
    String artifactId = "tomcat-util";
    String version = "5.5.23";

    String scanId = simulateReportIsAvailable("report.zip");

    PolicyEvaluationResult policyEvaluationResult = policyEvaluateService.evaluate(app.getPublicId(), scanId, stage);

    assertThat(policyEvaluationResult.getAffectedComponentCount(), is(1));
    assertThat(policyEvaluationResult.getCriticalComponentCount(), is(0));
    assertThat(policyEvaluationResult.getSevereComponentCount(), is(1));
    assertThat(policyEvaluationResult.getModerateComponentCount(), is(0));
    List<PolicyAlert> policyAlerts = policyEvaluationResult.getAlerts();
    assertThat(policyAlerts.size(), is(1));
    AbstractPolicyEvaluationTest.assertFactCounts(1, 1, policyAlerts.get(0));
    Component expectedComponentExact = ComponentFactory.forGav(groupId, artifactId, version, MatchState.EXACT);
    expectedComponentExact.setHash(hash);
    assertContainsPolicyAlert(expectedComponentExact, policy, constraint, Action.ID_FAIL, LabelConditionType.ID,
        policyAlerts);
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
  public void testEvaluate_MultiLicense() throws Exception {
    Constraint constraint = new Constraint(null /* constraintId */, "Constraint 1", LogicalOperator.AND);
    Condition condition = new Condition(LicenseConditionType.ID, "is", "GPL-2.0");
    constraint.addCondition(condition);

    Policy policy = new Policy(null /* policyId */, "Policy 1");
    policy.setThreatLevel(5);
    policy.addConstraint(constraint);
    policy.setAction(Stage.ID_BUILD, Action.ID_FAIL);
    policy.setOwnerId(app.getId());
    tempEntity.newPolicy(policy);
    constraint = policy.getConstraints().get(0);

    Stage stage = new Stage(Stage.ID_BUILD);

    String scanId = simulateReportIsAvailable("report.zip");

    PolicyEvaluationResult policyEvaluationResult = policyEvaluateService.evaluate(app.getPublicId(), scanId, stage);

    assertEquals(3, policyEvaluationResult.getAffectedComponentCount());
    assertEquals(0, policyEvaluationResult.getCriticalComponentCount());
    assertEquals(3, policyEvaluationResult.getSevereComponentCount());
    assertEquals(0, policyEvaluationResult.getModerateComponentCount());
    List<PolicyAlert> policyAlerts = policyEvaluationResult.getAlerts();
    assertEquals(3, policyAlerts.size());
    for (PolicyAlert policyAlert : policyAlerts) {
      AbstractPolicyEvaluationTest.assertFactCounts(1, 1, policyAlert);
    }
    Component expectedComponent = ComponentFactory.forGav("org.webjars", "select2", "3.2", MatchState.EXACT);
    expectedComponent.setHash("f2e35e4a21f07d25710f");
    assertContainsPolicyAlert(expectedComponent, policy, constraint, Action.ID_FAIL, LicenseConditionType.ID,
        policyAlerts);
  }

  @Test
  public void testEvaluate_LicenseOverride() throws Exception {
    Constraint constraint1 = new Constraint(null /* constraintId */, "Constraint 1", LogicalOperator.AND);
    Condition condition1 = new Condition(LicenseConditionType.ID, "is", "ZPL-2.0");
    constraint1.addCondition(condition1);
    Constraint constraint2 = new Constraint(null /* constraintId */, "Constraint 2", LogicalOperator.AND);
    Condition condition2 = new Condition(LicenseStatusConditionType.ID, "is", "OVERRIDDEN");
    constraint2.addCondition(condition2);

    Policy policy = new Policy(null /* policyId */, "Policy 1");
    policy.setThreatLevel(5);
    policy.addConstraint(constraint1);
    policy.addConstraint(constraint2);
    policy.setAction(Stage.ID_BUILD, Action.ID_FAIL);
    policy.setOwnerId(app.getId());
    tempEntity.newPolicy(policy);
    constraint1 = policy.getConstraints().get(0);
    constraint2 = policy.getConstraints().get(1);

    Stage stage = new Stage(Stage.ID_BUILD);

    // Override the license at org level
    tempEntity.newLicenseOverride(app.getOrganizationId(), COMMONS_POOL_ID, LicenseOverrideStatus.OVERRIDDEN,
        "ZPL-2.0", " My comment");

    String scanId = simulateReportIsAvailable("report.zip");

    // Evaluate policy
    PolicyEvaluationResult policyEvaluationResult = policyEvaluateService.evaluate(app.getPublicId(), scanId, stage);

    assertEquals(1, policyEvaluationResult.getAffectedComponentCount());
    assertEquals(0, policyEvaluationResult.getCriticalComponentCount());
    assertEquals(1, policyEvaluationResult.getSevereComponentCount());
    assertEquals(0, policyEvaluationResult.getModerateComponentCount());
    List<PolicyAlert> policyAlerts = policyEvaluationResult.getAlerts();
    assertEquals(2, policyAlerts.size());
    for (PolicyAlert policyAlert : policyAlerts) {
      AbstractPolicyEvaluationTest.assertFactCounts(1, 1, policyAlert);
    }
    Component expectedComponent = ComponentFactory.forGav("commons-pool", "commons-pool", "1.4", MatchState.EXACT);
    expectedComponent.setHash("1a667c9d419dc4f185c9");
    assertContainsPolicyAlert(expectedComponent, policy, constraint1, Action.ID_FAIL, LicenseConditionType.ID,
        policyAlerts);
    assertContainsPolicyAlert(expectedComponent, policy, constraint2, Action.ID_FAIL, LicenseStatusConditionType.ID,
        policyAlerts);

    // Override the license at app level. This must supersede the override at org level, so the policy should not
    // trigger any alerts.
    tempEntity.newLicenseOverride(app.getId(), COMMONS_POOL_ID, LicenseOverrideStatus.ACKNOWLEDGED,
        (String) null /* licenseId */, " My comment");

    scanId = simulateReportIsAvailable("report.zip");

    // Evaluate policy
    policyEvaluationResult = policyEvaluateService.evaluate(app.getPublicId(), scanId, stage);

    assertEquals(0, policyEvaluationResult.getAffectedComponentCount());
    assertEquals(0, policyEvaluationResult.getCriticalComponentCount());
    assertEquals(0, policyEvaluationResult.getSevereComponentCount());
    assertEquals(0, policyEvaluationResult.getModerateComponentCount());
    policyAlerts = policyEvaluationResult.getAlerts();
    assertEquals(0, policyAlerts.size());
  }

  @Test
  public void testEvaluate_SecurityVulnerabilityOverride() throws Exception {
    Constraint constraint = new Constraint(null /* constraintId */, "Constraint name", LogicalOperator.AND);
    Condition condition = new Condition(SecurityVulnerabilityStatusConditionType.ID, "is", "CONFIRMED");
    constraint.addCondition(condition);

    Policy policy = new Policy(null /* policyId */, "Policy name");
    policy.setThreatLevel(5);
    policy.addConstraint(constraint);
    policy.setAction(Stage.ID_BUILD, Action.ID_FAIL);
    policy.setOwnerId(app.getId());
    tempEntity.newPolicy(policy);
    constraint = policy.getConstraints().get(0);

    Stage stage = new Stage(Stage.ID_BUILD);

    // Override the security vulnerability
    tempEntity.newSecurityVulnerabilityOverride(app.getId(), "494308fc2d433720c778" /* hash */, "cve", "CVE-2009-1524",
        SecurityVulnerabilityOverrideStatus.CONFIRMED, " My comment");

    String scanId = simulateReportIsAvailable("report.zip");

    // Evaluate policy
    PolicyEvaluationResult policyEvaluationResult = policyEvaluateService.evaluate(app.getPublicId(), scanId, stage);

    assertThat(policyEvaluationResult.getAffectedComponentCount(), is(1));
    assertThat(policyEvaluationResult.getCriticalComponentCount(), is(0));
    assertThat(policyEvaluationResult.getSevereComponentCount(), is(1));
    assertThat(policyEvaluationResult.getModerateComponentCount(), is(0));
    List<PolicyAlert> policyAlerts = policyEvaluationResult.getAlerts();
    assertThat(policyAlerts, hasSize(1));
    AbstractPolicyEvaluationTest.assertFactCounts(1, 1, policyAlerts.get(0));
    Component expectedComponent = ComponentFactory.forGav("org.mortbay.jetty", "jetty", "6.1.15", MatchState.EXACT);
    expectedComponent.setHash("494308fc2d433720c778");
    assertContainsPolicyAlert(expectedComponent, policy, constraint, Action.ID_FAIL,
        SecurityVulnerabilityStatusConditionType.ID, policyAlerts);
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
  public void testEvaluate_ErrorReport() throws Exception {
    String scanId = simulateReportIsAvailable("empty_report.zip");

    try {
      policyEvaluateService.evaluate(app.getPublicId(), scanId, new Stage(Stage.ID_BUILD));
      fail("Expected exception");
    }
    catch (BadRequestException expected) {
      assertThat(expected.getMessage(),
          is("Unable to evaluate policy, the scan " + scanId + " could not be processed."));
    }

    PolicyEvaluation eval = new PolicyEvaluationDAO().getLastByApplicationIdAndStageId(app.getId(), Stage.ID_BUILD);
    assertNull(eval);
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

  @Test
  public void testEvaluate_MissingReport() throws Exception {
    try {
      policyEvaluateService.evaluate(app.getPublicId(), "scanId", new Stage(Stage.ID_BUILD));
      fail("Expected exception");
    }
    catch (NotFoundException expected) {
      assertThat(expected.getMessage(), is("Could not download the report for scan ID scanId"));
    }

    PolicyEvaluation eval = new PolicyEvaluationDAO().getLastByApplicationIdAndStageId(app.getId(), Stage.ID_BUILD);
    assertNull(eval);
  }

  private List<PolicyViolation> sort(List<PolicyViolation> policyViolations) {
    List<PolicyViolation> result = new ArrayList<>(policyViolations);
    Collections.sort(result, PolicyViolationComparator.COMPARATOR);
    return result;
  }

  @Test
  public void testEvaluate_OneStage() throws Exception {
    Constraint constraint1 = new Constraint("C1", "constraint 1", LogicalOperator.OR);
    Condition condition1 = new Condition(CoordinatesConditionType.ID, "match", "maven:tomcat:tomcat-util:5.5.23");
    constraint1.addCondition(condition1);
    Condition condition2 = new Condition(CoordinatesConditionType.ID, "match", "maven:commons-pool:commons-pool:1.4");
    constraint1.addCondition(condition2);
    Policy policy = new Policy("P1", "policy");
    policy.setThreatLevel(8);
    policy.addConstraint(constraint1);
    policy.setOwnerId(app.getId());
    tempEntity.newPolicy(policy);

    Stage stage = new Stage(Stage.ID_BUILD);

    String scanId = simulateReportIsAvailable("report.zip");

    // Evaluate policy
    policyEvaluateService.evaluate(app.getPublicId(), scanId, stage);
    PolicyEvaluation policyEvaluation1 = new PolicyEvaluationDAO().getLastByApplicationIdAndStageId(app.getId(),
        stage.getStageTypeId());
    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();
    List<PolicyViolation> policyViolations1 = policyViolationDAO.getActiveByApplicationIdAndStageId(app.getId(),
        stage.getStageTypeId());
    assertThat(policyViolations1, hasSize(2));
    policyViolations1 = sort(policyViolations1);
    assertThat(policyViolations1.get(0).getComponentIdentifier().get(ComponentIdentifier.MAVEN_GROUP_ID), is("tomcat"));
    assertThat(policyViolations1.get(0).getOpenTime(), is(policyEvaluation1.getTime()));
    assertThat(policyViolations1.get(1).getComponentIdentifier().get(ComponentIdentifier.MAVEN_GROUP_ID),
        is("commons-pool"));
    assertThat(policyViolations1.get(1).getOpenTime(), is(policyEvaluation1.getTime()));

    // Change one of the policy conditions and re-evaluate the policy.
    // This should cause a policy violation to be cleared and a new policy violation to appear.
    policy.getConstraints().get(0).getConditions().get(0).setValue("maven:commons-dbcp:commons-dbcp:1.4");
    policyDAO.update(policy);
    // Evaluate policy again for the same scan
    policyEvaluateService.evaluate(app.getPublicId(), scanId, stage);
    PolicyEvaluation policyEvaluation2 = new PolicyEvaluationDAO().getLastByApplicationIdAndStageId(app.getId(),
        stage.getStageTypeId());
    assertThat(policyEvaluation1.getId(), is(not(policyEvaluation2.getId())));
    List<PolicyViolation> policyViolations2 = policyViolationDAO.getActiveByApplicationIdAndStageId(app.getId(),
        stage.getStageTypeId());
    assertThat(policyViolations2, hasSize(2));
    policyViolations2 = sort(policyViolations2);
    assertThat(policyViolations2.get(0).getComponentIdentifier().get(ComponentIdentifier.MAVEN_GROUP_ID),
        is("commons-pool"));
    assertThat(policyViolations2.get(0).getOpenTime(), is(policyEvaluation1.getTime()));
    assertThat(policyViolations2.get(1).getComponentIdentifier().get(ComponentIdentifier.MAVEN_GROUP_ID),
        is("commons-dbcp"));
    assertThat(policyViolations2.get(1).getOpenTime(), is(policyEvaluation2.getTime()));
  }

  @Test
  public void testEvaluate_TwoStages() throws Exception {
    Constraint constraint1 = new Constraint("C1", "constraint 1", LogicalOperator.OR);
    Condition condition1 = new Condition(CoordinatesConditionType.ID, "match", "maven:commons-pool:commons-pool:1.4");
    constraint1.addCondition(condition1);
    Policy policy = new Policy("P1", "policy");
    policy.setThreatLevel(8);
    policy.addConstraint(constraint1);
    policy.setOwnerId(app.getId());
    tempEntity.newPolicy(policy);

    // Evaluate policy for the Build stage
    String scanBuildId = simulateReportIsAvailable("report.zip");
    policyEvaluateService.evaluate(app.getPublicId(), scanBuildId, new Stage(Stage.ID_BUILD));
    PolicyEvaluation policyEvaluationBuild = new PolicyEvaluationDAO().getLastByApplicationIdAndStageId(app.getId(),
        Stage.ID_BUILD);
    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();
    List<PolicyViolation> policyViolationsBuild = policyViolationDAO.getActiveByApplicationIdAndStageId(app.getId(),
        Stage.ID_BUILD);
    assertThat(policyViolationsBuild, hasSize(1));
    assertThat(policyViolationsBuild.get(0).getComponentIdentifier().get(ComponentIdentifier.MAVEN_GROUP_ID),
        is("commons-pool"));
    assertThat(policyViolationsBuild.get(0).getOpenTime(), is(policyEvaluationBuild.getTime()));

    // Evaluate policy for the Release stage
    String scanReleaseId = simulateReportIsAvailable("report.zip");
    policyEvaluateService.evaluate(app.getPublicId(), scanReleaseId, new Stage(Stage.ID_RELEASE));
    PolicyEvaluation policyEvaluationRelease = new PolicyEvaluationDAO().getLastByApplicationIdAndStageId(app.getId(),
        Stage.ID_RELEASE);
    List<PolicyViolation> policyViolationsRelease = policyViolationDAO.getActiveByApplicationIdAndStageId(app.getId(),
        Stage.ID_RELEASE);
    assertThat(policyViolationsRelease, hasSize(1));
    assertThat(policyViolationsRelease.get(0).getComponentIdentifier().get(ComponentIdentifier.MAVEN_GROUP_ID),
        is("commons-pool"));
    assertThat(policyViolationsRelease.get(0).getOpenTime(), is(policyEvaluationRelease.getTime()));

    policyViolationsBuild = policyViolationDAO.getActiveByApplicationIdAndStageId(app.getId(), Stage.ID_BUILD);
    assertThat(policyViolationsBuild, hasSize(1));
    assertThat(policyViolationsBuild.get(0).getOpenTime(), is(policyEvaluationBuild.getTime()));
  }

  @Test
  public void testEvaluate_PersistApplicationComponents() throws Exception {
    Stage stage1 = new Stage(Stage.ID_BUILD);
    Stage stage2 = new Stage(Stage.ID_RELEASE);

    // Evaluate policy
    ApplicationComponentDAO appComponentDAO = new ApplicationComponentDAO();
    assertThat(appComponentDAO.getByApplicationIdAndStageTypeId(app.getId(), stage1.getStageTypeId()), is(empty()));
    String scanId1 = simulateReportIsAvailable("PersistApplicationComponents/report1.zip");
    policyEvaluateService.evaluate(app.getPublicId(), scanId1, stage1);
    List<ApplicationComponent> appComponents1 = appComponentDAO.getByApplicationIdAndStageTypeId(app.getId(),
        stage1.getStageTypeId());
    assertThat(appComponents1, hasSize(1));
    ApplicationComponent appComponent1 = appComponents1.get(0);
    PolicyEvaluationDAO policyEvaluationDAO = new PolicyEvaluationDAO();
    PolicyEvaluation policyEvaluation1 = policyEvaluationDAO.getLastByApplicationIdAndStageId(app.getId(),
        stage1.getStageTypeId());
    assertApplicationComponent(COMMONS_DBCP_1_4, policyEvaluation1.getTime(), appComponent1);

    // Evaluate policy for a different stage. It should not touch the app<->component assocs for the first stage.
    assertThat(appComponentDAO.getByApplicationIdAndStageTypeId(app.getId(), stage2.getStageTypeId()), is(empty()));
    String scanId2 = simulateReportIsAvailable("PersistApplicationComponents/report2.zip");
    policyEvaluateService.evaluate(app.getPublicId(), scanId2, stage2);
    List<ApplicationComponent> appComponents2 = appComponentDAO.getByApplicationIdAndStageTypeId(app.getId(),
        stage2.getStageTypeId());
    assertThat(appComponents2, hasSize(1));
    ApplicationComponent appComponent2 = appComponents2.get(0);
    PolicyEvaluation policyEvaluation2 = policyEvaluationDAO.getLastByApplicationIdAndStageId(app.getId(),
        stage2.getStageTypeId());
    assertApplicationComponent(GERONIMO_TOMCAT_1_0, policyEvaluation2.getTime(), appComponent2);
    appComponents1 = appComponentDAO.getByApplicationIdAndStageTypeId(app.getId(), stage1.getStageTypeId());
    assertThat(appComponents1, hasSize(1));
    assertApplicationComponent(COMMONS_DBCP_1_4, policyEvaluation1.getTime(), appComponents1.get(0));
    assertThat(appComponents1.get(0).getId(), is(appComponent1.getId()));

    // Evaluate again for the first stage. It should replace the app<->component assocs for the first stage and it
    // should not touch the app<->component assocs for the second stage.
    String scanId3 = simulateReportIsAvailable("PersistApplicationComponents/report3.zip");
    policyEvaluateService.evaluate(app.getPublicId(), scanId3, stage1);
    List<ApplicationComponent> appComponents3 = appComponentDAO.getByApplicationIdAndStageTypeId(app.getId(),
        stage1.getStageTypeId());
    assertThat(appComponents3, hasSize(1));
    ApplicationComponent appComponent3 = appComponents3.get(0);
    policyEvaluation1 = policyEvaluationDAO.getLastByApplicationIdAndStageId(app.getId(), stage1.getStageTypeId());
    assertApplicationComponent(TOMCAT_UTIL_5_5_23, policyEvaluation1.getTime(), appComponent3);
    appComponents2 = appComponentDAO.getByApplicationIdAndStageTypeId(app.getId(), stage2.getStageTypeId());
    assertThat(appComponents2, hasSize(1));
    assertApplicationComponent(GERONIMO_TOMCAT_1_0, policyEvaluation2.getTime(), appComponents2.get(0));
    assertThat(appComponents2.get(0).getId(), is(appComponent2.getId()));
  }

  @Test
  public void testEvaluate_ReEvaluateObsoleteScan() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);

    // Evaluate policy for scanId1
    String scanId1 = simulateReportIsAvailable("report.zip");
    policyEvaluateService.evaluate(app.getPublicId(), scanId1, stage);
    assertPolicyEvaluation(app.getId(), scanId1, false /* isReevaluation */);

    // Make sure we don't have two evaluations at exactly the same time
    Thread.sleep(1);

    // Evaluate policy for scanId2
    String scanId2 = simulateReportIsAvailable("report.zip");
    policyEvaluateService.evaluate(app.getPublicId(), scanId2, stage);
    assertPolicyEvaluation(app.getId(), scanId2, false /* isReevaluation */);

    // Evaluate policy again for scanId1
    policyEvaluateService.evaluate(app.getPublicId(), scanId1, stage);
    assertPolicyEvaluation(app.getId(), scanId1, true /* isReevaluation */, true /* isForObsoleteScan */);
  }

  private void assertApplicationComponent(ComponentIdentifier componentIdentifier,
                                          Date time,
                                          ApplicationComponent actual)
  {
    assertThat(actual.getComponentIdentifier(), is(componentIdentifier));
    assertThat(actual.getTime(), is(time));
  }

  @Test
  public void testEvaluate_InvalidStage() throws Exception {
    try {
      policyEvaluateService.evaluate(app.getPublicId(), "scanid", new Stage("foobar"));
      fail("Expected exception");
    }
    catch (InvalidStageException expected) {
      assertThat(expected.getMessage(), is("Invalid stage id=foobar"));
    }
  }

  @Test
  public void testEvaluate_WaivedPolicyViolations() throws Exception {
    // Create a policy
    Constraint constraint = new Constraint(null /* constraintId */, "Constraint 1", LogicalOperator.AND);
    Condition condition = new Condition(LicenseConditionType.ID, "is", "GPL-2.0");
    constraint.addCondition(condition);
    Policy policy = new Policy(null /* policyId */, "Policy 1");
    policy.setThreatLevel(5);
    policy.addConstraint(constraint);
    policy.setAction(Stage.ID_BUILD, Action.ID_FAIL);
    policy.setOwnerId(app.getId());
    tempEntity.newPolicy(policy);

    String componentHash = "f2e35e4a21f07d25710f";
    PolicyWaiver policyWaiver = tempEntity.newWaiver(componentHash, policy.getId(), app.getId(), "Waiver comment here");

    String scanId = simulateReportIsAvailable("report.zip");

    // Evaluate the policy
    Stage stage = new Stage(Stage.ID_BUILD);
    policyEvaluateService.evaluate(app.getPublicId(), scanId, stage);

    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();
    List<PolicyViolation> policyViolations = policyViolationDAO.getUnfixedByApplicationIdAndStageId(app.getId(),
        stage.getStageTypeId());
    assertThat(policyViolations, hasSize(3));
    for (PolicyViolation policyViolation : policyViolations) {
      if (componentHash.equals(policyViolation.getHash())) {
        assertThat(policyViolation.isWaived(), is(true));
        assertThat(policyViolation.getPolicyWaiverId(), is(policyWaiver.getId()));
        assertThat(policyViolation.getPolicyWaiverComment(), is(policyWaiver.getComment()));
      }
      else {
        assertThat(policyViolation.isWaived(), is(false));
      }
    }
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
