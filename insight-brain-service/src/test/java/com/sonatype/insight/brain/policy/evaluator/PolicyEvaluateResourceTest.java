/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.mail.Message;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.jira.JiraConfig;
import com.sonatype.insight.brain.jira.JiraField;
import com.sonatype.insight.brain.jira.JiraIssueCreateRequest;
import com.sonatype.insight.brain.jira.JiraIssueCreateRequest.JiraIssueCreateResponse;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
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
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.report.ReportResource;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Assert;
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
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PolicyEvaluateResourceTest
    extends AbstractResourceTest
{
  private final ComponentIdentifier GERONIMO_TOMCAT_1_0 = ComponentIdentifier.createMavenCoordinates("geronimo",
      "geronimo-tomcat", "1.0");

  private final ComponentIdentifier COMMONS_DBCP_1_4 = ComponentIdentifier.createMavenCoordinates("commons-dbcp",
      "commons-dbcp", "1.4");

  private final ComponentIdentifier TOMCAT_UTIL_5_5_23 = ComponentIdentifier.createMavenCoordinates("tomcat",
      "tomcat-util", "5.5.23");

  private static final ComponentIdentifier COMMONS_POOL_ID = ComponentIdentifier.createMavenCoordinates("commons-pool",
      "commons-pool", "1.4");

  private String applicationPublicId = "PolicyEvaluateResourceTestAppPublicId";

  private String licenseFingerprint = "PolicyEvaluateResourceTest_LicenseFingerprint";

  private PolicyDAO policyDAO = new PolicyDAO();

  private Application app;

  private HttpRequest evalRequest(String appId, String scanId, Stage stage) {
    return restRequest().path(PolicyEvaluateResource.RESOURCE_PATH).query("scanId", scanId).parameter(appId)
        .body(stage);
  }

  @Before
  public void before() throws Exception {
    Organization org = tempEntity.newOrganization();
    app = tempEntity.newApplication("appName", applicationPublicId, org.getId(), "admin");
    if (!isTestUsingManualServerInit()) {
      setLicenseFingerprint(licenseFingerprint);
    }
  }

  @Test
  public void testEvaluate_MultipleMatchesForSameGAV() throws Exception {
    String scanId = "testEvaluate_MultipleMatchesForSameGAV_ScanId";

    Constraint constraintLicense = new Constraint(null /* constraintId */, "Constraint License", LogicalOperator.AND);
    Condition condition1 = new Condition(LicenseConditionType.ID, "is", "UNSPECIFIED");
    constraintLicense.addCondition(condition1);
    Constraint constraintSV = new Constraint(null /* constraintId */, "Constraint SV", LogicalOperator.AND);
    Condition condition2 = new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0");
    constraintSV.addCondition(condition2);

    Policy policy1 = new Policy(null /* policyId */, "Policy 1");
    policy1.setThreatLevel(5);
    policy1.addConstraint(constraintLicense);
    policy1.addConstraint(constraintSV);
    policy1.setAction(Stage.ID_BUILD, Action.ID_FAIL);
    policy1.setOwnerId(app.getId());
    policyDAO.insert(policy1);
    constraintLicense = policy1.getConstraints().get(0);
    constraintSV = policy1.getConstraints().get(1);

    Stage stage = new Stage(Stage.ID_BUILD);

    // The report file is not available yet
    HttpResponse response = evalRequest(applicationPublicId, scanId, stage).post();
    assertResponseStatus(404, response);

    // Simulate that the report is available
    mockReport(scanId, "/PolicyEvaluateResourceTest/MultipleMatchesForSameGAV/report");
    response = evalRequest(applicationPublicId, scanId, stage).post();
    assertResponseStatus(200, response);
    PolicyEvaluationResult policyEval = response.getBody(PolicyEvaluationResult.class);
    Assert.assertNotNull(policyEval);
    Assert.assertEquals(3, policyEval.getAffectedComponentCount());
    Assert.assertEquals(0, policyEval.getCriticalComponentCount());
    Assert.assertEquals(3, policyEval.getSevereComponentCount());
    Assert.assertEquals(0, policyEval.getModerateComponentCount());
    List<PolicyAlert> policyAlerts = policyEval.getAlerts();
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(9, policyAlerts.size());
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
    assertContainsPolicyAlert(expectedComponentExact, policy1, constraintLicense, Action.ID_FAIL,
        LicenseConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(expectedComponentExact, policy1, constraintSV, Action.ID_FAIL,
        SecurityVulnerabilitySeverityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(expectedComponentSimilar1, policy1, constraintLicense, Action.ID_FAIL,
        LicenseConditionType.ID, policyAlerts);
    // Verify that the SVs are associated with components by hash, not by component identifier.
    // If SVs were associated with components by component identifier, this component would have a policy violation for
    // an SV because it has the same identifier as expectedComponentExact, which has a violation for an SV.
    assertNotContainsPolicyAlert(expectedComponentSimilar1, policy1, constraintSV, Action.ID_FAIL,
        SecurityVulnerabilitySeverityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(expectedComponentSimilar2, policy1, constraintLicense, Action.ID_FAIL,
        LicenseConditionType.ID, policyAlerts);
    // Verify that the SVs are associated with components by hash, not by component identifier.
    // If SVs were associated with components by component identifier, this component would have a policy violation for
    // an SV because it has the same identifier as expectedComponentExact, which has a violation for an SV.
    assertNotContainsPolicyAlert(expectedComponentSimilar2, policy1, constraintSV, Action.ID_FAIL,
        SecurityVulnerabilitySeverityConditionType.ID, policyAlerts);
  }

  @Test
  public void testEvaluate_ManuallyIdentifiedComponent() throws Exception {
    String scanId = "testEvaluate_ManuallyIdentifiedComponent_ScanId";

    Constraint constraint1 = new Constraint(null /* constraintId */, "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(MatchStateConditionType.ID, "is", "exact"));
    constraint1.addCondition(new Condition(AgeInDaysConditionType.ID, "younger than", "30"));

    Policy policy1 = new Policy(null /* policyId */, "Policy 1");
    policy1.setThreatLevel(5);
    policy1.addConstraint(constraint1);
    policy1.setAction(Stage.ID_BUILD, Action.ID_FAIL);
    policy1.setOwnerId(app.getId());
    policyDAO.insert(policy1);
    constraint1 = policy1.getConstraints().get(0);

    Stage stage = new Stage(Stage.ID_BUILD);

    String hash = "5801a1a27a36f88e2089";
    String groupId = "G";
    String artifactId = "A";
    String version = "V";
    HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier(hash,
        ComponentIdentifier.createMavenCoordinates(groupId, artifactId, version));
    hashComponentIdentifier.setCreateTime(new Date());
    HashComponentIdentifierDAO hashComponentIdentifierDAO = new HashComponentIdentifierDAO();
    hashComponentIdentifierDAO.insert(hashComponentIdentifier);
    // The report file is not available yet
    HttpResponse response = evalRequest(applicationPublicId, scanId, stage).post();
    assertResponseStatus(404, response);

    // Simulate that the report is available
    mockReport(scanId, "/PolicyEvaluateResourceTest/ManuallyIdentifiedComponent/report.zip");
    response = evalRequest(applicationPublicId, scanId, stage).post();
    hashComponentIdentifierDAO.delete(hashComponentIdentifier);
    assertResponseStatus(200, response);
    PolicyEvaluationResult policyEval = response.getBody(PolicyEvaluationResult.class);
    Assert.assertNotNull(policyEval);
    Assert.assertEquals(1, policyEval.getAffectedComponentCount());
    Assert.assertEquals(0, policyEval.getCriticalComponentCount());
    Assert.assertEquals(1, policyEval.getSevereComponentCount());
    Assert.assertEquals(0, policyEval.getModerateComponentCount());
    List<PolicyAlert> policyAlerts = policyEval.getAlerts();
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    AbstractPolicyEvaluationTest.assertFactCounts(1, 1, policyAlerts.get(0));
    Component expectedComponentExact = ComponentFactory.forGav(groupId, artifactId, version, MatchState.EXACT);
    expectedComponentExact.setHash(hash);
    assertContainsPolicyAlert(expectedComponentExact, policy1, constraint1, Action.ID_FAIL, MatchStateConditionType.ID,
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
    String scanId = "testEvaluate_Label_ScanId";
    Label label = tempEntity.newLabel(orgLabel ? app.getOrganizationId() : app.getId(), "red");
    tempEntity.newComponentLabel(orgComponentLabel ? app.getOrganizationId() : app.getId(), label.getId(), hash);

    Constraint constraint1 = new Constraint(null /* constraintId */, "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(LabelConditionType.ID, "is", label.getId()));
    Policy policy1 = new Policy(null /* policyId */, "Policy 1");
    policy1.setThreatLevel(5);
    policy1.addConstraint(constraint1);
    policy1.setAction(Stage.ID_BUILD, Action.ID_FAIL);
    policy1.setOwnerId(app.getId());
    policyDAO.insert(policy1);
    constraint1 = policy1.getConstraints().get(0);

    Stage stage = new Stage(Stage.ID_BUILD);

    String groupId = "tomcat";
    String artifactId = "tomcat-util";
    String version = "5.5.23";

    mockReport(scanId, "/PolicyEvaluateResourceTest/report.zip");
    HttpResponse response = evalRequest(applicationPublicId, scanId, stage).post();
    assertResponseStatus(200, response);
    PolicyEvaluationResult policyEval = response.getBody(PolicyEvaluationResult.class);
    assertThat(policyEval, is(notNullValue()));
    assertThat(policyEval.getAffectedComponentCount(), is(1));
    assertThat(policyEval.getCriticalComponentCount(), is(0));
    assertThat(policyEval.getSevereComponentCount(), is(1));
    assertThat(policyEval.getModerateComponentCount(), is(0));
    List<PolicyAlert> policyAlerts = policyEval.getAlerts();
    assertThat(policyAlerts, is(notNullValue()));
    assertThat(policyAlerts.size(), is(1));
    AbstractPolicyEvaluationTest.assertFactCounts(1, 1, policyAlerts.get(0));
    Component expectedComponentExact = ComponentFactory.forGav(groupId, artifactId, version, MatchState.EXACT);
    expectedComponentExact.setHash(hash);
    assertContainsPolicyAlert(expectedComponentExact, policy1, constraint1, Action.ID_FAIL, LabelConditionType.ID,
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
    Assert.assertNotNull(policyEvaluation);
    Assert.assertEquals(isReevaluation, policyEvaluation.isReevaluation());
    Assert.assertEquals(isForObsoleteScan, policyEvaluation.isForObsoleteScan());
  }

  @Test
  @ManualServerInit
  public void testEvaluate() throws Exception {
    initServer(new Configurator()
    {
      @Override
      public void configure(final InsightConfig config) {
        config.setJiraConfig(new JiraConfig());
      }
    });
    JiraIssueCreateResponse createResponse = new JiraIssueCreateResponse();
    when(mockJiraClient.createIssue(any(JiraIssueCreateRequest.class))).thenReturn(createResponse);

    setLicenseFingerprint(licenseFingerprint);

    final String scanId = "PolicyEvaluateResourceTest_ScanId";

    final Constraint constraint1 = new Constraint("C1", "PolicyEvaluateResourceTest constraint 1", LogicalOperator.AND);
    final Condition condition1 = new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0");
    constraint1.addCondition(condition1);
    final Policy policy1 = new Policy("P1", "PolicyEvaluateResourceTest policy1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.getNotifications().add(new UserNotification("manager@example.com", Stage.ID_BUILD));
    policy1.getNotifications().add(new UserNotification("john.doe@example.com", Stage.ID_BUILD));
    policy1.getNotifications().add(new JiraNotification("projectKey1", 1, Stage.ID_BUILD));
    policy1.setAction(Stage.ID_BUILD, Action.ID_FAIL);
    policy1.setOwnerId(app.getId());
    policyDAO.insert(policy1);

    final Constraint constraint2 = new Constraint("C2", "PolicyEvaluateResourceTest constraint 2", LogicalOperator.AND);
    final Condition condition2 = new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0");
    constraint2.addCondition(condition2);
    // same conditions, but lower threat-level => analysis should show highest threat-level
    final Policy policy2 = new Policy("P2", "PolicyEvaluateResourceTest policy2");
    policy2.setThreatLevel(3);
    policy2.addConstraint(constraint2);
    policy2.setOwnerId(app.getId());
    policy2.getNotifications().add(new UserNotification("Mark.MyWords@example.com", Stage.ID_RELEASE));
    policy2.getNotifications().add(new JiraNotification("projectKey2", 2, Stage.ID_RELEASE));
    policy2.setAction(Stage.ID_RELEASE, Action.ID_FAIL);
    policyDAO.insert(policy2);

    final Stage stage = new Stage(Stage.ID_BUILD);

    // Simulate that the report is available
    mockReport(scanId, "/PolicyEvaluateResourceTest/report.zip");

    final List<Message> messagesA = Mailbox.get("manager@example.com");
    final List<Message> messagesB = Mailbox.get("john.doe@example.com");

    messagesA.clear();
    messagesB.clear();

    ApplicationComponentDAO appComponentDAO = new ApplicationComponentDAO();
    assertThat(appComponentDAO.getByApplicationIdAndStageTypeId(app.getId(), stage.getStageTypeId()), is(empty()));

    // evaluate policy
    HttpResponse response = evalRequest(applicationPublicId, scanId, stage).post();
    assertResponseStatus(200, response);
    PolicyEvaluationResult policyEval = response.getBody(PolicyEvaluationResult.class);
    Assert.assertNotNull(policyEval);
    Assert.assertEquals(7, policyEval.getAffectedComponentCount());
    Assert.assertEquals(7, policyEval.getCriticalComponentCount());
    Assert.assertEquals(0, policyEval.getSevereComponentCount());
    Assert.assertEquals(0, policyEval.getModerateComponentCount());
    List<PolicyAlert> policyAlerts = policyEval.getAlerts();
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(72, policyAlerts.size());
    for (PolicyAlert policyAlert : policyAlerts) {
      AbstractPolicyEvaluationTest.assertFactCounts(1, 1, policyAlert);
    }
    PolicyEvaluationDAO policyEvaluationDAO = new PolicyEvaluationDAO();
    PolicyEvaluation policyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndScanId(app.getId(), scanId);
    assertThat(policyEvaluation, notNullValue());
    assertThat(policyEvaluation.isReevaluation(), is(false));
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
    response = restRequest()
        .path(ReportResource.RESOURCE_PATH, "browseReport", ScanPolicyEvaluator.POLICY_THREATS_FILENAME)
        .parameter(applicationPublicId, scanId).get();
    assertResponseStatus(200, response);
    final JsonNode policyThreats = JsonUtils.parse(response.getBodyText()).get("aaData");
    Assert.assertNotNull(policyThreats);
    Assert.assertTrue(policyThreats.size() > 0);
    Assert.assertEquals(8, policyThreats.get(0).get("policyThreatLevel").asInt());

    // check components are associated with the application and stage
    assertThat(appComponentDAO.getByApplicationIdAndStageTypeId(app.getId(), stage.getStageTypeId()), hasSize(28));

    // notification message should also have been sent
    assertNotifications(messagesA, 1, 5000);
    Assert.assertTrue(messagesA.get(0).getSubject().contains("Policy"));
    assertNotifications(messagesB, 1, 5000);
    Assert.assertTrue(messagesB.get(0).getSubject().contains("Policy"));

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
    response = evalRequest(applicationPublicId, scanId, stage).post();
    assertResponseStatus(200, response);
    policyEval = response.getBody(PolicyEvaluationResult.class);
    Assert.assertNotNull(policyEval);
    policyAlerts = policyEval.getAlerts();
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(72, policyAlerts.size());
    for (PolicyAlert policyAlert : policyAlerts) {
      AbstractPolicyEvaluationTest.assertFactCounts(1, 1, policyAlert);
    }
    policyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndScanId(app.getId(), scanId);
    assertThat(policyEvaluation, notNullValue());
    assertThat(policyEvaluation.isReevaluation(), is(true));
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
  public void testPolicyThreatLevelCounts() throws Exception {
    final String scanId = "PolicyThreatCountResourceTest_ScanId";

    final Constraint constraint = new Constraint("C1", "PolicyThreatCountResourceTest constraint 1",
        LogicalOperator.AND);
    final Condition condition = new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0");
    constraint.addCondition(condition);
    Policy policy = new Policy("P1", "PolicyThreatCountResourceTest policy1");
    policy.setThreatLevel(1);
    policy.addConstraint(constraint);
    policy.setOwnerId(app.getId());
    policyDAO.insert(policy);

    final Stage stage = new Stage(Stage.ID_BUILD);

    // The report file is not available yet
    HttpResponse response = evalRequest(applicationPublicId, scanId, stage).post();
    assertResponseStatus(404, response);

    // Simulate that the report is available
    mockReport(scanId, "/PolicyEvaluateResourceTest/report.zip");

    // Threat Level 1 Should not show up in any counts
    response = evalRequest(applicationPublicId, scanId, stage).post();
    assertResponseStatus(200, response);

    PolicyEvaluationResult policyEval = response.getBody(PolicyEvaluationResult.class);
    Assert.assertNotNull(policyEval);
    Assert.assertEquals(0, policyEval.getAffectedComponentCount());
    Assert.assertEquals(0, policyEval.getCriticalComponentCount());
    Assert.assertEquals(0, policyEval.getSevereComponentCount());
    Assert.assertEquals(0, policyEval.getModerateComponentCount());

    policy.setThreatLevel(2);
    policyDAO.update(policy);

    // Threat Level 2 should show up as moderate
    response = evalRequest(applicationPublicId, scanId, stage).post();
    assertResponseStatus(200, response);
    policyEval = response.getBody(PolicyEvaluationResult.class);
    Assert.assertNotNull(policyEval);
    Assert.assertEquals(7, policyEval.getAffectedComponentCount());
    Assert.assertEquals(0, policyEval.getCriticalComponentCount());
    Assert.assertEquals(0, policyEval.getSevereComponentCount());
    Assert.assertEquals(7, policyEval.getModerateComponentCount());

    policy.setThreatLevel(4);
    policyDAO.update(policy);

    // Threat Level 4 should show up as severe
    response = evalRequest(applicationPublicId, scanId, stage).post();
    assertResponseStatus(200, response);
    policyEval = response.getBody(PolicyEvaluationResult.class);
    Assert.assertNotNull(policyEval);
    Assert.assertEquals(7, policyEval.getAffectedComponentCount());
    Assert.assertEquals(0, policyEval.getCriticalComponentCount());
    Assert.assertEquals(7, policyEval.getSevereComponentCount());
    Assert.assertEquals(0, policyEval.getModerateComponentCount());

    policy.setThreatLevel(8);
    policyDAO.update(policy);

    // Threat Level 8 should show up as severe
    response = evalRequest(applicationPublicId, scanId, stage).post();
    assertResponseStatus(200, response);
    policyEval = response.getBody(PolicyEvaluationResult.class);
    Assert.assertNotNull(policyEval);
    Assert.assertEquals(7, policyEval.getAffectedComponentCount());
    Assert.assertEquals(7, policyEval.getCriticalComponentCount());
    Assert.assertEquals(0, policyEval.getSevereComponentCount());
    Assert.assertEquals(0, policyEval.getModerateComponentCount());
  }

  @Test
  public void testEvaluate_MultiLicense() throws Exception {
    String scanId = "testEvaluate_MultiLicense_ScanId";

    Constraint constraint1 = new Constraint(null /* constraintId */, "Constraint 1", LogicalOperator.AND);
    Condition condition1 = new Condition(LicenseConditionType.ID, "is", "GPL-2.0");
    constraint1.addCondition(condition1);

    Policy policy1 = new Policy(null /* policyId */, "Policy 1");
    policy1.setThreatLevel(5);
    policy1.addConstraint(constraint1);
    policy1.setAction(Stage.ID_BUILD, Action.ID_FAIL);
    policy1.setOwnerId(app.getId());
    policyDAO.insert(policy1);
    constraint1 = policy1.getConstraints().get(0);

    Stage stage = new Stage(Stage.ID_BUILD);

    // The report file is not available yet
    HttpResponse response = evalRequest(applicationPublicId, scanId, stage).post();
    assertResponseStatus(404, response);

    // Simulate that the report is available
    mockReport(scanId, "/PolicyEvaluateResourceTest/report.zip");
    response = evalRequest(applicationPublicId, scanId, stage).post();
    assertResponseStatus(200, response);
    PolicyEvaluationResult policyEval = response.getBody(PolicyEvaluationResult.class);
    Assert.assertNotNull(policyEval);
    Assert.assertEquals(3, policyEval.getAffectedComponentCount());
    Assert.assertEquals(0, policyEval.getCriticalComponentCount());
    Assert.assertEquals(3, policyEval.getSevereComponentCount());
    Assert.assertEquals(0, policyEval.getModerateComponentCount());
    List<PolicyAlert> policyAlerts = policyEval.getAlerts();
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(3, policyAlerts.size());
    for (PolicyAlert policyAlert : policyAlerts) {
      AbstractPolicyEvaluationTest.assertFactCounts(1, 1, policyAlert);
    }
    Component expectedComponent = ComponentFactory.forGav("org.webjars", "select2", "3.2", MatchState.EXACT);
    expectedComponent.setHash("f2e35e4a21f07d25710f");
    assertContainsPolicyAlert(expectedComponent, policy1, constraint1, Action.ID_FAIL, LicenseConditionType.ID,
        policyAlerts);
  }

  @Test
  public void testEvaluate_LicenseOverride() throws Exception {
    String scanId = "testEvaluate_LicenseOverride_DefinedAtOrgLevel";

    Constraint constraint1 = new Constraint(null /* constraintId */, "Constraint 1", LogicalOperator.AND);
    Condition condition1 = new Condition(LicenseConditionType.ID, "is", "ZPL-2.0");
    constraint1.addCondition(condition1);
    Constraint constraint2 = new Constraint(null /* constraintId */, "Constraint 2", LogicalOperator.AND);
    Condition condition2 = new Condition(LicenseStatusConditionType.ID, "is", "OVERRIDDEN");
    constraint2.addCondition(condition2);

    Policy policy1 = new Policy(null /* policyId */, "Policy 1");
    policy1.setThreatLevel(5);
    policy1.addConstraint(constraint1);
    policy1.addConstraint(constraint2);
    policy1.setAction(Stage.ID_BUILD, Action.ID_FAIL);
    policy1.setOwnerId(app.getId());
    policyDAO.insert(policy1);
    constraint1 = policy1.getConstraints().get(0);
    constraint2 = policy1.getConstraints().get(1);

    Stage stage = new Stage(Stage.ID_BUILD);

    // Simulate that the report is available
    mockReport(scanId, "/PolicyEvaluateResourceTest/report.zip");

    // Override the license at org level
    tempEntity.newLicenseOverride(app.getOrganizationId(), COMMONS_POOL_ID, LicenseOverrideStatus.OVERRIDDEN,
        "ZPL-2.0", " My comment");

    // Evaluate policy
    HttpResponse response = evalRequest(applicationPublicId, scanId, stage).post();
    assertResponseStatus(200, response);
    PolicyEvaluationResult policyEval = response.getBody(PolicyEvaluationResult.class);
    Assert.assertNotNull(policyEval);
    Assert.assertEquals(1, policyEval.getAffectedComponentCount());
    Assert.assertEquals(0, policyEval.getCriticalComponentCount());
    Assert.assertEquals(1, policyEval.getSevereComponentCount());
    Assert.assertEquals(0, policyEval.getModerateComponentCount());
    List<PolicyAlert> policyAlerts = policyEval.getAlerts();
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(2, policyAlerts.size());
    for (PolicyAlert policyAlert : policyAlerts) {
      AbstractPolicyEvaluationTest.assertFactCounts(1, 1, policyAlert);
    }
    Component expectedComponent = ComponentFactory.forGav("commons-pool", "commons-pool", "1.4", MatchState.EXACT);
    expectedComponent.setHash("1a667c9d419dc4f185c9");
    assertContainsPolicyAlert(expectedComponent, policy1, constraint1, Action.ID_FAIL, LicenseConditionType.ID,
        policyAlerts);
    assertContainsPolicyAlert(expectedComponent, policy1, constraint2, Action.ID_FAIL, LicenseStatusConditionType.ID,
        policyAlerts);

    // Override the license at app level. This must supersede the override at org level, so the policy should not
    // trigger any alerts.
    tempEntity.newLicenseOverride(app.getId(), COMMONS_POOL_ID, LicenseOverrideStatus.ACKNOWLEDGED,
        (String) null /* licenseId */, " My comment");

    // Evaluate policy
    response = evalRequest(applicationPublicId, scanId, stage).post();
    assertResponseStatus(200, response);
    policyEval = response.getBody(PolicyEvaluationResult.class);
    Assert.assertNotNull(policyEval);
    Assert.assertEquals(0, policyEval.getAffectedComponentCount());
    Assert.assertEquals(0, policyEval.getCriticalComponentCount());
    Assert.assertEquals(0, policyEval.getSevereComponentCount());
    Assert.assertEquals(0, policyEval.getModerateComponentCount());
    policyAlerts = policyEval.getAlerts();
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(0, policyAlerts.size());
  }

  @Test
  public void testEvaluate_SecurityVulnerabilityOverride() throws Exception {
    String scanId = "testEvaluate_SecurityVulnerabilityOverride";

    Constraint constraint = new Constraint(null /* constraintId */, "Constraint name", LogicalOperator.AND);
    Condition condition = new Condition(SecurityVulnerabilityStatusConditionType.ID, "is", "CONFIRMED");
    constraint.addCondition(condition);

    Policy policy = new Policy(null /* policyId */, "Policy name");
    policy.setThreatLevel(5);
    policy.addConstraint(constraint);
    policy.setAction(Stage.ID_BUILD, Action.ID_FAIL);
    policy.setOwnerId(app.getId());
    policyDAO.insert(policy);
    constraint = policy.getConstraints().get(0);

    Stage stage = new Stage(Stage.ID_BUILD);

    // Simulate that the report is available
    mockReport(scanId, "/PolicyEvaluateResourceTest/report.zip");

    // Override the security vulnerability
    tempEntity.newSecurityVulnerabilityOverride(app.getId(), "494308fc2d433720c778" /* hash */, "cve", "CVE-2009-1524",
        SecurityVulnerabilityOverrideStatus.CONFIRMED, " My comment");

    // Evaluate policy
    HttpResponse response = evalRequest(applicationPublicId, scanId, stage).post();
    assertResponseStatus(200, response);
    PolicyEvaluationResult policyEval = response.getBody(PolicyEvaluationResult.class);
    assertThat(policyEval.getAffectedComponentCount(), is(1));
    assertThat(policyEval.getCriticalComponentCount(), is(0));
    assertThat(policyEval.getSevereComponentCount(), is(1));
    assertThat(policyEval.getModerateComponentCount(), is(0));
    List<PolicyAlert> policyAlerts = policyEval.getAlerts();
    assertThat(policyAlerts, hasSize(1));
    AbstractPolicyEvaluationTest.assertFactCounts(1, 1, policyAlerts.get(0));
    Component expectedComponent = ComponentFactory.forGav("org.mortbay.jetty", "jetty", "6.1.15", MatchState.EXACT);
    expectedComponent.setHash("494308fc2d433720c778");
    assertContainsPolicyAlert(expectedComponent, policy, constraint, Action.ID_FAIL,
        SecurityVulnerabilityStatusConditionType.ID, policyAlerts);
  }

  @Test
  public void testNotificationEmailModel() throws Exception {
    final String scanId = "PolicyEvaluateResourceTest_ScanId";

    final Constraint constraint1 = new Constraint("C1", "PolicyEvaluateResourceTest constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "5"));
    final Policy policy1 = new Policy("P1", "PolicyEvaluateResourceTest policy1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.setOwnerId(app.getId());
    policyDAO.insert(policy1);

    final Constraint constraint2 = new Constraint("C2", "PolicyEvaluateResourceTest constraint 2", LogicalOperator.AND);
    constraint2.addCondition(new Condition(CoordinatesConditionType.ID, "match", "maven:tomcat"));
    final Policy policy2 = new Policy("P2", "PolicyEvaluateResourceTest policy2");
    policy2.setThreatLevel(4);
    policy2.addConstraint(constraint2);
    policy2.setOwnerId(app.getId());
    policyDAO.insert(policy2);

    final Constraint constraint3 = new Constraint("C3", "PolicyEvaluateResourceTest constraint 3", LogicalOperator.AND);
    constraint3.addCondition(new Condition(CoordinatesConditionType.ID, "match", "maven:org.*"));
    final Policy policy3 = new Policy("P3", "PolicyEvaluateResourceTest policy3");
    policy3.setThreatLevel(3);
    policy3.addConstraint(constraint3);
    policy3.setOwnerId(app.getId());
    policyDAO.insert(policy3);

    final Constraint constraint4 = new Constraint("C4", "PolicyEvaluateResourceTest constraint 1", LogicalOperator.AND);
    constraint4.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, "<", "5"));
    final Policy policy4 = new Policy("P4", "PolicyEvaluateResourceTest policy4");
    policy4.setThreatLevel(0);
    policy4.addConstraint(constraint4);
    policy4.setOwnerId(app.getId());
    policyDAO.insert(policy4);

    final Stage stage = new Stage(Stage.ID_BUILD);

    mockReport(scanId, "/PolicyEvaluateResourceTest/report.zip");

    String serverUrl = "http://localhost/";

    HttpResponse response = evalRequest(applicationPublicId, scanId, stage).post();
    assertResponseStatus(200, response);
    PolicyEvaluationResult policyEval = response.getBody(PolicyEvaluationResult.class);

    List<PolicyFact> policyFacts = new ArrayList<>();
    for (PolicyAlert policyAlert : policyEval.getAlerts()) {
      policyFacts.add(policyAlert.getTrigger());
    }

    PolicyAlertEmailer emailer = getCLMServer().getInjector().getInstance(PolicyAlertEmailer.class);

    Map<String, Object> model = emailer.createPolicyMailModel(serverUrl, app, scanId, stage, policyFacts);
    Assert.assertNotNull(model);
    Assert.assertEquals(policyFacts, model.get("policyFacts"));
    Assert.assertEquals("http://cdn.sonatype.com/", model.get("cdnUrl"));
    Assert.assertEquals(serverUrl + UserInterfaceLinksResource.getReportUrl(applicationPublicId, scanId),
        model.get("detailedReportUrl"));
    Assert.assertEquals(18, model.get("policyThreatRedCount"));
    Assert.assertEquals(3, model.get("policyThreatOrangeCount"));
    Assert.assertEquals(13, model.get("policyThreatYellowCount"));
    Assert.assertEquals(18, model.get("policyThreatBlueCount"));
    Assert.assertEquals("Build", model.get("policyThreatStage"));
    Assert.assertEquals(applicationPublicId, model.get("policyThreatApp"));
    Assert.assertEquals("Admin BuiltIn", model.get("applicationContactName"));
    Assert.assertEquals("admin@localhost", model.get("applicationContactEmail"));
    Assert.assertNotNull(model.get("policyThreatTime"));
    Assert.assertEquals("APP ID", model.get("ownerIdLabel"));
  }

  @Test
  public void testErrorReport() throws Exception {
    final String scanId = "PolicyEvaluateResourceTest_ScanId";

    mockReport(scanId, "/PolicyEvaluateResourceTest/empty_report.zip");

    HttpResponse response = evalRequest(applicationPublicId, scanId, new Stage(Stage.ID_BUILD)).post();
    assertResponseStatus(400, response);

    PolicyEvaluation eval = new PolicyEvaluationDAO().getLastByApplicationIdAndStageId(app.getId(), Stage.ID_BUILD);
    Assert.assertNull(eval);
  }

  @Test
  public void testReEvaluate_Notifications() throws Exception {
    String scanId = "testReEvaluation";

    Constraint constraint1 = new Constraint("C1", "PolicyEvaluateResourceTest constraint 1", LogicalOperator.AND);
    Condition condition1 = new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0");
    constraint1.addCondition(condition1);
    Policy policy1 = new Policy("P1", "PolicyEvaluateResourceTest policy1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.getNotifications().add(new UserNotification("manager@test.corp", Stage.ID_BUILD));
    policy1.setOwnerId(app.getId());
    policyDAO.insert(policy1);

    Stage stage = new Stage(Stage.ID_BUILD);

    // Simulate that the report is available
    mockReport(scanId, "/PolicyEvaluateResourceTest/report.zip");

    List<Message> notifications = Mailbox.get("manager@test.corp");
    notifications.clear();

    // Evaluate policy
    HttpResponse response = evalRequest(applicationPublicId, scanId, stage).post();
    assertResponseStatus(200, response);
    PolicyEvaluationResult policyEvaluationResult = response.getBody(PolicyEvaluationResult.class);
    Assert.assertNotNull(policyEvaluationResult);
    List<PolicyAlert> policyAlerts = policyEvaluationResult.getAlerts();
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(36, policyAlerts.size());
    assertPolicyEvaluation(app.getId(), scanId, false /* isReevaluation */);

    // Notification message should have been sent
    assertNotifications(notifications, 1, 5000);
    notifications.clear();

    // Change the policy name
    policy1.setName(policy1.getName() + "Updated");
    policyDAO.update(policy1);

    // Evaluate policy again for the same scan
    response = evalRequest(applicationPublicId, scanId, stage).post();
    assertResponseStatus(200, response);
    policyEvaluationResult = response.getBody(PolicyEvaluationResult.class);
    Assert.assertNotNull(policyEvaluationResult);
    Assert.assertEquals(36, policyAlerts.size());
    assertPolicyEvaluation(app.getId(), scanId, true /* isReevaluation */);

    // Notification message should not have been sent since this is a re-evaluation
    assertNotifications(notifications, 0, 5000);
  }

  @Test
  public void testEvaluate_NoPolicyEvalAuditEntryCreatedIfReportMissing() throws Exception {
    final String scanId = "PolicyEvaluateResourceTest_ScanId";

    setHdsResponseForURI("/rest/ci/report?scanId=" + scanId, "Internal Error", 500);
    HttpResponse response = evalRequest(applicationPublicId, scanId, new Stage(Stage.ID_BUILD)).post();
    assertResponseStatus(404, response);

    PolicyEvaluation eval = new PolicyEvaluationDAO().getLastByApplicationIdAndStageId(app.getId(), Stage.ID_BUILD);
    Assert.assertNull(eval);
  }

  private List<PolicyViolation> sort(List<PolicyViolation> policyViolations) {
    List<PolicyViolation> result = new ArrayList<>(policyViolations);
    Collections.sort(result, PolicyViolationComparator.COMPARATOR);
    return result;
  }

  @Test
  public void testEvaluate_FirstOccurrencePolicyViolations_OneStage() throws Exception {
    String scanId = "testEvaluateFirstOccurrencePolicyViolations";

    Constraint constraint1 = new Constraint("C1", "PolicyEvaluateResourceTest constraint 1", LogicalOperator.OR);
    Condition condition1 = new Condition(CoordinatesConditionType.ID, "match", "maven:tomcat:tomcat-util:5.5.23");
    constraint1.addCondition(condition1);
    Condition condition2 = new Condition(CoordinatesConditionType.ID, "match", "maven:commons-pool:commons-pool:1.4");
    constraint1.addCondition(condition2);
    Policy policy = new Policy("P1", "PolicyEvaluateResourceTest policy");
    policy.setThreatLevel(8);
    policy.addConstraint(constraint1);
    policy.setOwnerId(app.getId());
    policyDAO.insert(policy);

    Stage stage = new Stage(Stage.ID_BUILD);

    // Simulate that the report is available
    mockReport(scanId, "/PolicyEvaluateResourceTest/report.zip");

    // Evaluate policy
    HttpResponse response = evalRequest(applicationPublicId, scanId, stage).post();
    assertResponseStatus(200, response);
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
    response = evalRequest(applicationPublicId, scanId, stage).post();
    assertResponseStatus(200, response);
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
  public void testEvaluate_FirstOccurrencePolicyViolations_TwoStages() throws Exception {
    Constraint constraint1 = new Constraint("C1", "PolicyEvaluateResourceTest constraint 1", LogicalOperator.OR);
    Condition condition1 = new Condition(CoordinatesConditionType.ID, "match", "maven:commons-pool:commons-pool:1.4");
    constraint1.addCondition(condition1);
    Policy policy = new Policy("P1", "PolicyEvaluateResourceTest policy");
    policy.setThreatLevel(8);
    policy.addConstraint(constraint1);
    policy.setOwnerId(app.getId());
    policyDAO.insert(policy);

    // Evaluate policy for the Build stage
    String scanBuildId = "scanBuildId";
    mockReport(scanBuildId, "/PolicyEvaluateResourceTest/report.zip");
    HttpResponse response = evalRequest(applicationPublicId, scanBuildId, new Stage(Stage.ID_BUILD)).post();
    assertResponseStatus(200, response);
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
    String scanReleaseId = "scanReleaseId";
    mockReport(scanReleaseId, "/PolicyEvaluateResourceTest/report.zip");
    response = evalRequest(applicationPublicId, scanReleaseId, new Stage(Stage.ID_RELEASE)).post();
    assertResponseStatus(200, response);
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

    // Simulate that the report is available
    String scanId1 = "testEvaluatePersistApplicationComponents1";
    mockReport(scanId1, "/PolicyEvaluateResourceTest/PersistApplicationComponents/report1.zip");
    String scanId2 = "testEvaluatePersistApplicationComponents2";
    mockReport(scanId2, "/PolicyEvaluateResourceTest/PersistApplicationComponents/report2.zip");
    String scanId3 = "testEvaluatePersistApplicationComponents3";
    mockReport(scanId3, "/PolicyEvaluateResourceTest/PersistApplicationComponents/report3.zip");

    // Evaluate policy
    ApplicationComponentDAO appComponentDAO = new ApplicationComponentDAO();
    assertThat(appComponentDAO.getByApplicationIdAndStageTypeId(app.getId(), stage1.getStageTypeId()), is(empty()));
    HttpResponse response = evalRequest(applicationPublicId, scanId1, stage1).post();
    assertResponseStatus(200, response);
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
    response = evalRequest(applicationPublicId, scanId2, stage2).post();
    assertResponseStatus(200, response);
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
    response = evalRequest(applicationPublicId, scanId3, stage1).post();
    assertResponseStatus(200, response);
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
  public void testReEvaluate_ObsoleteScan() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);

    // Evaluate policy for scanId1
    String scanId1 = "scanId1";
    // Simulate that the report is available
    mockReport(scanId1, "/PolicyEvaluateResourceTest/report.zip");
    HttpResponse response = evalRequest(applicationPublicId, scanId1, stage).post();
    assertResponseStatus(200, response);
    PolicyEvaluationResult policyEvaluationResult = response.getBody(PolicyEvaluationResult.class);
    assertPolicyEvaluation(app.getId(), scanId1, false /* isReevaluation */);

    // Make sure we don't have two evaluations at exactly the same time
    Thread.sleep(1);

    // Evaluate policy for scanId2
    String scanId2 = "scanId2";
    // Simulate that the report is available
    mockReport(scanId2, "/PolicyEvaluateResourceTest/report.zip");
    response = evalRequest(applicationPublicId, scanId2, stage).post();
    assertResponseStatus(200, response);
    assertPolicyEvaluation(app.getId(), scanId2, false /* isReevaluation */);

    // Evaluate policy again for scanid1
    response = evalRequest(applicationPublicId, scanId1, stage).post();
    assertResponseStatus(200, response);
    policyEvaluationResult = response.getBody(PolicyEvaluationResult.class);
    Assert.assertNotNull(policyEvaluationResult);
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
  public void testInvalidStage() throws Exception {
    HttpResponse response = evalRequest(applicationPublicId, "scanid", new Stage("foobar")).post();
    assertResponseStatus(HttpStatus.BAD_REQUEST_400, response);
    assertEquals("Invalid stage id=foobar", response.getBodyText());
  }

  @Test
  public void testEvaluate_WaivedPolicyViolations() throws Exception {
    String scanId = "testEvaluate_WaivedPolicyViolations_ScanId";

    // Create a policy
    Constraint constraint = new Constraint(null /* constraintId */, "Constraint 1", LogicalOperator.AND);
    Condition condition = new Condition(LicenseConditionType.ID, "is", "GPL-2.0");
    constraint.addCondition(condition);
    Policy policy = new Policy(null /* policyId */, "Policy 1");
    policy.setThreatLevel(5);
    policy.addConstraint(constraint);
    policy.setAction(Stage.ID_BUILD, Action.ID_FAIL);
    policy.setOwnerId(app.getId());
    policyDAO.insert(policy);

    String componentHash = "f2e35e4a21f07d25710f";
    PolicyWaiver policyWaiver = tempEntity.newWaiver(componentHash, policy.getId(), app.getId(), "Waiver comment here");

    // Simulate that the report is available
    mockReport(scanId, "/PolicyEvaluateResourceTest/report.zip");

    // Evaluate the policy
    Stage stage = new Stage(Stage.ID_BUILD);
    HttpResponse response = evalRequest(applicationPublicId, scanId, stage).post();
    assertResponseStatus(200, response);

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
}
