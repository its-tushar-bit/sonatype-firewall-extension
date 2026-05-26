/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.jira;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.jira.JiraIssueCreateRequest.JiraIssueCreateResponse;
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
import com.sonatype.insight.brain.organization.ApplicationContactLoader;
import com.sonatype.insight.brain.policy.evaluator.PolicyNotificationUtil;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.security.UserDirectory;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.shutdown.ShutdownPriority;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.test.LogOutput;
import jakarta.inject.Inject;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

public class JiraPolicyAlertNotifierTest
    extends AbstractComponentTest
{
  private static final Duration NOTIFICATION_WAIT_TIMEOUT = Duration.ofMillis(5000);

  @Rule
  public LogOutput logOutput = new LogOutput(JiraPolicyAlertNotifier.class);

  @Inject
  private JiraPolicyAlertNotifier jiraPolicyAlertNotifier;

  @Inject
  private TestProductLicense testProductLicense;

  @Inject
  private UserDirectory userDirectory;

  @Inject
  private PolicyNotificationUtil policyNotificationUtil;

  @Inject
  private PolicyDAO policyDAO;

  @Mock
  private JiraClient jiraClient;

  @Mock
  private JiraService jiraService;

  @Mock
  private ShutdownHandler mockShutdownHandler;

  @Captor
  private ArgumentCaptor<Thread> threadArgumentCaptor;

  @Before
  public void before() throws Exception {
    setBaseUrl("http://localhost");

    jiraPolicyAlertNotifier = new JiraPolicyAlertNotifier(userDirectory, jiraService, lookup(BaseUrl.class),
        lookup(AuditRecorder.class), testProductLicense, mockShutdownHandler);

    JiraIssueCreateResponse createResponse = mock(JiraIssueCreateResponse.class);
    lenient().when(jiraService.getConfiguration())
        .thenReturn(new com.sonatype.insight.brain.model.jira.JiraConfiguration());
    lenient().when(jiraService.client(any())).thenReturn(jiraClient);
    lenient().when(jiraClient.createIssue(any(JiraIssueCreateRequest.class), anyBoolean())).thenReturn(createResponse);
    lenient().when(createResponse.getKey()).thenReturn("IQ-1");
  }

  @Test
  public void test_sendNotifications() throws IOException {
    Application application = tempEntity.newApplicationWithParent("app");

    final String projectKey = "projectKey";
    final long issueTypeId = 1L;

    Stage stage = new Stage(Stage.ID_BUILD, "BUILD");
    String scanId = "scan-id";
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(application.getId(), stage.getStageTypeId(), scanId);
    Policy policy = tempEntity.newPolicy(application);
    policy.getNotifications().add(new JiraNotification(projectKey, issueTypeId, evaluation.getStageTypeId()));
    policyDAO.update(policy);

    List<PolicyViolation> policyViolations = new ArrayList<>();
    policyViolations.add(tempEntity.newPolicyViolation(evaluation, policy));
    List<PolicyNotification> policyNotifications = policyNotificationUtil
        .createPolicyNotifications(application, policyViolations, evaluation.getStageTypeId(),
            evaluation.isForMonitoring());

    jiraPolicyAlertNotifier.sendNotifications(application, scanId, stage, policyNotifications);

    ArgumentCaptor<JiraIssueCreateRequest> createRequestArgumentCaptor = ArgumentCaptor
        .forClass(JiraIssueCreateRequest.class);
    verify(jiraClient, timeout(NOTIFICATION_WAIT_TIMEOUT.toMillis()))
        .createIssue(createRequestArgumentCaptor.capture(), anyBoolean());

    JiraIssueCreateRequest jiraIssueCreateRequest = createRequestArgumentCaptor.getValue();
    assertThat(jiraIssueCreateRequest.getFields()).hasSize(4);
    Map<String, String> projectMeta = jiraIssueCreateRequest.getField(JiraField.PROJECT);
    assertThat(projectMeta).containsEntry("key", projectKey);
    Map<String, Long> issueMeta = jiraIssueCreateRequest.getField(JiraField.ISSUETYPE);
    assertThat(issueMeta).containsEntry("id", issueTypeId);
    String summary = jiraIssueCreateRequest.getField(JiraField.SUMMARY);
    assertThat(summary).isEqualTo("Nexus IQ: Application " + application.getName() + "; BUILD stage; 1 Policy alerts");
    verify(mockShutdownHandler).add(threadArgumentCaptor.capture(), eq(ShutdownPriority.NOTIFICATIONS));
    assertThat(threadArgumentCaptor.getValue().getName()).startsWith("PolicyAlertJIRANotifierForScan");
  }

  @Test
  public void test_sendNotifications_NonMaven() throws IOException {
    Application application = tempEntity.newApplicationWithParent("app");

    final String projectKey = "projectKey";
    final long issueTypeId = 1L;

    Stage stage = new Stage(Stage.ID_BUILD, "BUILD");
    String scanId = "scan-id";
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(application.getId(), stage.getStageTypeId(), scanId);
    Policy policy = tempEntity.newPolicy(application);
    policy.getNotifications().add(new JiraNotification(projectKey, issueTypeId, evaluation.getStageTypeId()));
    policyDAO.update(policy);

    ComponentIdentifier identifier = ComponentIdentifier.createAnameCoordinates("jquery", "", "3.0.0");
    List<PolicyViolation> policyViolations = new ArrayList<>();
    policyViolations.add(tempEntity.newPolicyViolation(evaluation, policy, identifier, "abcd"));
    List<PolicyNotification> policyNotifications =
        policyNotificationUtil.createPolicyNotifications(application, policyViolations, evaluation.getStageTypeId(),
            evaluation.isForMonitoring());

    jiraPolicyAlertNotifier.sendNotifications(application, scanId, stage, policyNotifications);

    ArgumentCaptor<JiraIssueCreateRequest> createRequestArgumentCaptor = ArgumentCaptor
        .forClass(JiraIssueCreateRequest.class);
    verify(jiraClient, timeout(NOTIFICATION_WAIT_TIMEOUT.toMillis()))
        .createIssue(createRequestArgumentCaptor.capture(), anyBoolean());

    JiraIssueCreateRequest jiraIssueCreateRequest = createRequestArgumentCaptor.getValue();
    assertThat(jiraIssueCreateRequest.getField("description").toString())
        .contains(ComponentDisplayNameUtil.fromIdentifier(identifier).toString());
  }

  @Test
  public void test_sendNotifications_Error() throws IOException {
    Application application = tempEntity.newApplicationWithParent("app");

    final String projectKey = "projectKey";
    final long issueTypeId = 1L;

    Stage stage = new Stage(Stage.ID_BUILD, "BUILD");
    String scanId = "scan-id";
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(application.getId(), stage.getStageTypeId(), scanId);
    Policy policy = tempEntity.newPolicy(application);
    policy.getNotifications().add(new JiraNotification(projectKey, issueTypeId, evaluation.getStageTypeId()));
    policyDAO.update(policy);

    List<PolicyViolation> policyViolations = new ArrayList<>();
    policyViolations.add(tempEntity.newPolicyViolation(evaluation, policy));
    List<PolicyNotification> policyNotifications = policyNotificationUtil
        .createPolicyNotifications(application, policyViolations, evaluation.getStageTypeId(),
            evaluation.isForMonitoring());

    Exception ex = new RuntimeException();
    doThrow(ex).when(jiraClient).createIssue(any(JiraIssueCreateRequest.class), anyBoolean());
    jiraPolicyAlertNotifier.sendNotifications(application, scanId, stage, policyNotifications);

    await().atMost(NOTIFICATION_WAIT_TIMEOUT)
        .untilAsserted(() -> assertThat(logOutput).atErrorLevel()
            .contains(
                "Failed to create notification for Internal JIRA project key " + projectKey + " and issue type id "
                    + issueTypeId + ". Failed for application " + application.getPublicId() + " and scan " + scanId
                    + " in stage " + stage.getStageTypeId() + ".",
                ex));
  }

  @Test
  public void test_sendNotifications_EmailRoleNotificationsOnly() {
    Application application = tempEntity.newApplicationWithParent("app");
    Role role = tempEntity.newRole(false /* global */, Permission.READ);

    Stage stage = new Stage(Stage.ID_BUILD, "BUILD");
    String scanId = "scan-id";
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(application.getId(), stage.getStageTypeId(), scanId);
    Policy policy = tempEntity.newPolicy(application);
    policy.getNotifications().add(new RoleNotification(role.getId(), role.getName(), stage.getStageTypeId()));
    policy.getNotifications().add(new UserNotification("email@sonatype.com", stage.getStageTypeId()));
    policyDAO.update(policy);

    List<PolicyViolation> policyViolations = new ArrayList<>();
    policyViolations.add(tempEntity.newPolicyViolation(evaluation, policy));
    List<PolicyNotification> policyNotifications =
        policyNotificationUtil.createPolicyNotifications(application, policyViolations, evaluation.getStageTypeId(),
            evaluation.isForMonitoring());

    jiraPolicyAlertNotifier.sendNotifications(application, scanId, stage, policyNotifications);

    await().atMost(NOTIFICATION_WAIT_TIMEOUT)
        .untilAsserted(() -> assertThat(logOutput).atDebugLevel()
            .contains(
                "Not sending Internal JIRA notifications for application " + application.getPublicId() + " and scan "
                    + evaluation.getScanId() + " in stage " + evaluation.getStageTypeId()
                    + ", no JIRA projects configured for any violated policy."));
  }

  @Test
  public void test_sendNotifications_NotEnabled() throws IOException {
    when(jiraService.getConfiguration()).thenReturn(null);

    jiraPolicyAlertNotifier.sendNotifications(new Application(), "", new Stage(), Collections.emptyList());

    assertThat(logOutput).atDebugLevel().contains("Internal JIRA integration is not enabled; skipping issue creation");

    verify(jiraClient, timeout(NOTIFICATION_WAIT_TIMEOUT.toMillis()).times(0))
        .createIssue(any(JiraIssueCreateRequest.class), anyBoolean());
  }

  @Test
  public void testCreatePolicyMailModel_BaseUrlNotConfigured() {
    setBaseUrl(null);

    Application app = tempEntity.newApplicationWithParent();

    assertThatThrownBy(() -> jiraPolicyAlertNotifier.createPolicyMailModel(app,
        ApplicationContactLoader.getInstance(userDirectory).getContact(app.getContactInternalName()), "scanId",
        new Stage(Stage.ID_BUILD), null, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(BaseUrl.ERR_MSG_BASE_URL_NOT_CONFIGURED);
  }

  @Test
  public void testSendNotifications_MissingLicenseFeature() throws Exception {
    testProductLicense.setMissingFeatures(LicensedFeature.NOTIFICATIONS);

    Application application = tempEntity.newApplicationWithParent("app");

    final String projectKey = "projectKey";
    final long issueTypeId = 1L;

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = "scan-id";
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(application.getId(), stage.getStageTypeId(), scanId);
    Policy policy = tempEntity.newPolicy(application);
    policy.getNotifications().add(new JiraNotification(projectKey, issueTypeId, evaluation.getStageTypeId()));
    policyDAO.update(policy);

    List<PolicyViolation> policyViolations = new ArrayList<>();
    policyViolations.add(tempEntity.newPolicyViolation(evaluation, policy));
    List<PolicyNotification> policyNotifications = policyNotificationUtil
        .createPolicyNotifications(application, policyViolations, evaluation.getStageTypeId(),
            evaluation.isForMonitoring());

    jiraPolicyAlertNotifier.sendNotifications(application, scanId, stage, policyNotifications);

    verify(jiraClient, timeout(NOTIFICATION_WAIT_TIMEOUT.toMillis()).times(0)).createIssue(any(), anyBoolean());
  }
}
