/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.jira;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.notifications.JiraNotification;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.model.policy.notifications.RoleNotification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.policy.evaluator.PolicyNotificationUtil;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.insight.test.LogOutput;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JiraPolicyAlertNotifierTest
    extends AbstractComponentTest
{
  private static final int NOTIFICATION_WAIT_TIMEOUT = 5000; // millisecs

  @Rule
  public LogOutput logOutput = new LogOutput(JiraPolicyAlertNotifier.class);

  @Inject
  private JiraPolicyAlertNotifier jiraPolicyAlertNotifier;

  @Inject
  private InsightConfig config;

  @Inject
  private CLMLicenseManager clmLicenseManager;

  @Inject
  private TestProductLicenseManager productLicenseManager;

  @Mock
  private JiraClient jiraClient;

  @Mock
  private JiraService jiraService;

  @Override
  public void configure(Binder binder) {
    lenient().when(jiraService.client()).thenReturn(jiraClient);
    lenient().when(jiraService.isEnabled()).thenReturn(true);

    binder.bind(JiraService.class).toInstance(jiraService);
    super.configure(binder);
  }

  @Before
  public void before() {
    config.setBaseUrl("http://localhost");
    config.setJiraConfig(new JiraConfig());
  }

  @Test
  public void test_sendNotifications() throws IOException {
    Application application = tempEntity.newApplicationWithParent("app");

    final String projectKey = "projectKey";
    final Long issueTypeId = 1L;

    Stage stage = new Stage(Stage.ID_BUILD, "BUILD");
    String scanId = "scan-id";
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(application.getId(), stage.getStageTypeId(), scanId);
    Policy policy = tempEntity.newPolicy(application);
    policy.getNotifications().add(new JiraNotification(projectKey, issueTypeId, evaluation.getStageTypeId()));
    new PolicyDAO().update(policy);

    List<PolicyViolation> policyViolations = new ArrayList<>();
    policyViolations.add(tempEntity.newPolicyViolation(evaluation, policy));
    List<PolicyNotification> policyNotifications = PolicyNotificationUtil
        .createPolicyNotifications(policyViolations, evaluation.getStageTypeId(), evaluation.isForMonitoring());

    jiraPolicyAlertNotifier.sendNotifications(application, scanId, stage, policyNotifications);

    ArgumentCaptor<JiraIssueCreateRequest> createRequestArgumentCaptor = ArgumentCaptor
        .forClass(JiraIssueCreateRequest.class);
    verify(jiraClient, timeout(NOTIFICATION_WAIT_TIMEOUT)).createIssue(createRequestArgumentCaptor.capture());

    JiraIssueCreateRequest jiraIssueCreateRequest = createRequestArgumentCaptor.getValue();
    assertThat(jiraIssueCreateRequest.getFields()).hasSize(4);
    Map<String, String> projectMeta = jiraIssueCreateRequest.getField(JiraField.PROJECT);
    assertThat(projectMeta).containsEntry("key", projectKey);
    Map<String, Long> issueMeta = jiraIssueCreateRequest.getField(JiraField.ISSUETYPE);
    assertThat(issueMeta).containsEntry("id", issueTypeId);
    String summary = jiraIssueCreateRequest.getField(JiraField.SUMMARY);
    assertThat(summary).isEqualTo("Nexus IQ: Application " + application.getName() + "; BUILD stage; 1 Policy alerts");
  }

  @Test
  public void test_sendNotifications_NonMaven() throws IOException {
    Application application = tempEntity.newApplicationWithParent("app");

    final String projectKey = "projectKey";
    final Long issueTypeId = 1L;

    Stage stage = new Stage(Stage.ID_BUILD, "BUILD");
    String scanId = "scan-id";
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(application.getId(), stage.getStageTypeId(), scanId);
    Policy policy = tempEntity.newPolicy(application);
    policy.getNotifications().add(new JiraNotification(projectKey, issueTypeId, evaluation.getStageTypeId()));
    new PolicyDAO().update(policy);

    ComponentIdentifier identifier = ComponentIdentifier.createAnameCoordinates("jquery", "", "3.0.0");
    List<PolicyViolation> policyViolations = new ArrayList<>();
    policyViolations.add(tempEntity.newPolicyViolation(evaluation, policy, identifier, "abcd"));
    List<PolicyNotification> policyNotifications = PolicyNotificationUtil.createPolicyNotifications(policyViolations,
        evaluation.getStageTypeId(), evaluation.isForMonitoring());

    jiraPolicyAlertNotifier.sendNotifications(application, scanId, stage, policyNotifications);

    ArgumentCaptor<JiraIssueCreateRequest> createRequestArgumentCaptor = ArgumentCaptor
        .forClass(JiraIssueCreateRequest.class);
    verify(jiraClient, timeout(NOTIFICATION_WAIT_TIMEOUT)).createIssue(createRequestArgumentCaptor.capture());

    JiraIssueCreateRequest jiraIssueCreateRequest = createRequestArgumentCaptor.getValue();
    assertThat((String) jiraIssueCreateRequest.getField("description"))
        .contains(ComponentDisplayNameUtil.fromIdentifier(identifier).toString());
  }

  @Test
  public void test_sendNotifications_Error() throws IOException {
    Application application = tempEntity.newApplicationWithParent("app");

    final String projectKey = "projectKey";
    final Long issueTypeId = 1L;

    Stage stage = new Stage(Stage.ID_BUILD, "BUILD");
    String scanId = "scan-id";
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(application.getId(), stage.getStageTypeId(), scanId);
    Policy policy = tempEntity.newPolicy(application);
    policy.getNotifications().add(new JiraNotification(projectKey, issueTypeId, evaluation.getStageTypeId()));
    new PolicyDAO().update(policy);

    List<PolicyViolation> policyViolations = new ArrayList<>();
    policyViolations.add(tempEntity.newPolicyViolation(evaluation, policy));
    List<PolicyNotification> policyNotifications = PolicyNotificationUtil
        .createPolicyNotifications(policyViolations, evaluation.getStageTypeId(), evaluation.isForMonitoring());

    Exception ex = new RuntimeException();
    doThrow(ex).when(jiraClient).createIssue(any(JiraIssueCreateRequest.class));
    jiraPolicyAlertNotifier.sendNotifications(application, scanId, stage, policyNotifications);

    await().atMost(NOTIFICATION_WAIT_TIMEOUT, TimeUnit.MILLISECONDS).untilAsserted(() -> {
      assertThat(logOutput).atErrorLevel()
          .contains("Failed to create JIRA notification for JIRA project key " + projectKey + " and JIRA issue type id "
              + issueTypeId + ". Failed for application " + application.getPublicId() + " and scan " + scanId
              + " in stage " + stage.getStageTypeId(), ex);
    });
  }

  @Test
  public void test_sendNotifications_EmailRoleNotificationsOnly() {
    Application application = tempEntity.newApplicationWithParent("app");
    Role role = tempEntity.newRole(false /* global */, Permission.READ);

    Stage stage = new Stage(Stage.ID_BUILD, "BUILD");
    String scanId = "scan-id";
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(application.getId(), stage.getStageTypeId(), scanId);
    Policy policy = tempEntity.newPolicy(application);
    policy.getNotifications().add(new RoleNotification(role.getId(), stage.getStageTypeId()));
    policy.getNotifications().add(new UserNotification("email@sonatype.com", stage.getStageTypeId()));
    new PolicyDAO().update(policy);

    List<PolicyViolation> policyViolations = new ArrayList<>();
    policyViolations.add(tempEntity.newPolicyViolation(evaluation, policy));
    List<PolicyNotification> policyNotifications = PolicyNotificationUtil
        .createPolicyNotifications(policyViolations, evaluation.getStageTypeId(), evaluation.isForMonitoring());

    jiraPolicyAlertNotifier.sendNotifications(application, scanId, stage, policyNotifications);

    await().atMost(NOTIFICATION_WAIT_TIMEOUT, TimeUnit.MILLISECONDS).untilAsserted(() -> {
      assertThat(logOutput).atDebugLevel()
          .contains("Not sending JIRA notifications for application " + application.getPublicId() + " and scan "
              + evaluation.getScanId() + " in stage " + evaluation.getStageTypeId()
              + ", no JIRA projects configured for any violated policy");
    });
  }

  @Test
  public void test_sendNotifications_NotEnabled() throws IOException {
    when(jiraService.isEnabled()).thenReturn(false);

    jiraPolicyAlertNotifier.sendNotifications(new Application(), "", new Stage(), Collections.emptyList());

    assertThat(logOutput).atDebugLevel().contains("JIRA integration is not enabled; skipping issue creation");

    verify(jiraClient, timeout(NOTIFICATION_WAIT_TIMEOUT).times(0)).createIssue(any(JiraIssueCreateRequest.class));
  }

  @Test
  public void testCreatePolicyMailModel_BaseUrlNotConfigured() {
    config.setBaseUrl(null);

    Application app = tempEntity.newApplicationWithParent();

    assertThatThrownBy(() -> {
      jiraPolicyAlertNotifier.createPolicyMailModel(app, "scanId", new Stage(Stage.ID_BUILD), null, null);
    }).isInstanceOf(IllegalStateException.class).hasMessage(BaseUrl.ERR_MSG_BASE_URL_NOT_CONFIGURED);
  }

  @Test
  public void testSendNotifications_Foundation() throws Exception {
    productLicenseManager.setProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);
    clmLicenseManager.installLicense(null);

    Application application = tempEntity.newApplicationWithParent("app");

    final String projectKey = "projectKey";
    final Long issueTypeId = 1L;

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = "scan-id";
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(application.getId(), stage.getStageTypeId(), scanId);
    Policy policy = tempEntity.newPolicy(application);
    policy.getNotifications().add(new JiraNotification(projectKey, issueTypeId, evaluation.getStageTypeId()));
    new PolicyDAO().update(policy);

    List<PolicyViolation> policyViolations = new ArrayList<>();
    policyViolations.add(tempEntity.newPolicyViolation(evaluation, policy));
    List<PolicyNotification> policyNotifications = PolicyNotificationUtil
        .createPolicyNotifications(policyViolations, evaluation.getStageTypeId(), evaluation.isForMonitoring());

    jiraPolicyAlertNotifier.sendNotifications(application, scanId, stage, policyNotifications);

    verify(jiraClient, timeout(NOTIFICATION_WAIT_TIMEOUT).times(0)).createIssue(any());
  }

  @Test
  public void testSendNotifications_FoundationAndFirewall() throws Exception {
    productLicenseManager.setProducts(ProductLicenseDetails.PRODUCT_FOUNDATION, ProductLicenseDetails.PRODUCT_FIREWALL);
    clmLicenseManager.installLicense(null);

    Application application = tempEntity.newApplicationWithParent("app");

    final String projectKey = "projectKey";
    final Long issueTypeId = 1L;

    Stage stage = new Stage(Stage.ID_BUILD, "BUILD");
    String scanId = "scan-id";
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(application.getId(), stage.getStageTypeId(), scanId);
    Policy policy = tempEntity.newPolicy(application);
    policy.getNotifications().add(new JiraNotification(projectKey, issueTypeId, evaluation.getStageTypeId()));
    new PolicyDAO().update(policy);

    List<PolicyViolation> policyViolations = new ArrayList<>();
    policyViolations.add(tempEntity.newPolicyViolation(evaluation, policy));
    List<PolicyNotification> policyNotifications = PolicyNotificationUtil
        .createPolicyNotifications(policyViolations, evaluation.getStageTypeId(), evaluation.isForMonitoring());

    jiraPolicyAlertNotifier.sendNotifications(application, scanId, stage, policyNotifications);

    verify(jiraClient, timeout(NOTIFICATION_WAIT_TIMEOUT).times(0)).createIssue(any());
  }
}
