/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.actions.ApiComponentChangeActionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.service.ApiComponentRemediationService;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.git.GitClientFactory;
import com.sonatype.insight.brain.git.PullRequestFeatureCheck;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractComponentAuditTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.SourceControlConfig;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.nexus.git.utils.api.GitException;
import com.sonatype.nexus.iq.manager.PullRequestCommand;
import com.sonatype.nexus.iq.manager.PullRequestExecutor;
import com.sonatype.nexus.iq.manager.PullRequestResult;
import com.sonatype.nexus.scm.api.GitApiClient;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PolicyAlertScmNotifierAuditTest
    extends AbstractComponentAuditTest
{
  private static final String SCAN_ID = "scanId";

  private static final String STAGE_ID = BuildStageType.ID;

  @Mock
  private PullRequestFeatureCheck pullRequestFeatureCheck;

  @Mock
  private ApiComponentRemediationService remediationService;

  @Mock
  private GitClientFactory gitClientFactory;

  @Mock
  private GitApiClient gitApiClient;

  @Mock
  private SourceControlUtils sourceControlUtils;

  @Mock
  private PullRequestExecutor pullRequestExecutor;

  @Inject
  private PolicyAlertScmNotifier policyAlertScmNotifier;

  @Inject
  private InsightConfig config;

  private Application application;

  @Override
  public void configure(Binder binder) {
    binder.bind(PullRequestFeatureCheck.class).toInstance(pullRequestFeatureCheck);
    binder.bind(ApiComponentRemediationService.class).toInstance(remediationService);
    binder.bind(GitClientFactory.class).toInstance(gitClientFactory);
    binder.bind(GitApiClient.class).toInstance(gitApiClient);
    binder.bind(SourceControlUtils.class).toInstance(sourceControlUtils);
    binder.bind(PullRequestExecutor.class).toInstance(pullRequestExecutor);

    super.configure(binder);
  }

  @Before
  public void before() throws IOException {
    application = tempEntity.newApplicationWithParent();

    config.setBaseUrl("http://localhost");

    givenSourceControlIsEnabled();
    givenRemediationIsAvailable();
    givenGitServicesAvailable();
    givenGitRepositoryInfoAvailable();
  }

  @Test
  public void testSendNotifications() throws IOException, GitException {
    // given a pull request can execute successfully for a notification
    final PullRequestResult result = new PullRequestResult();
    result.setPullRequestUrl("my url");
    when(pullRequestExecutor.isSupportedFormat(any())).thenReturn(true);
    when(pullRequestExecutor.execute(any(PullRequestCommand.class))).thenReturn(result);

    // and some notifications to send
    final List<PolicyNotification> policyNotifications = createPolicyNotifications();

    // when we send the notifications to the scm notifier
    policyAlertScmNotifier.sendNotifications(application, SCAN_ID, new Stage(STAGE_ID), policyNotifications);

    // then we see an audit created with the pullRequestUrl
    final List<AuditDTO> auditDTOs = awaitLogEntries(AuditEvent.CREATE_PULL_REQUEST, 1);
    assertApplicationPolicyNotificationAuditData(auditDTOs.get(0), "my url",
        getComponentIdentifier(policyNotifications));
  }

  private ComponentIdentifier getComponentIdentifier(final List<PolicyNotification> policyNotifications) {
    return policyNotifications.get(0).getPolicyFact().getComponentFacts().get(0).getComponentIdentifier();
  }

  @Test
  public void testSendNotifications_GitException_No_Audit() throws Exception {
    // given a pull request will fail for a notification
    when(pullRequestExecutor.isSupportedFormat(any())).thenReturn(true);
    when(pullRequestExecutor.execute(any())).thenThrow(new GitException("oh no!"));

    // and some notifications to send
    final List<PolicyNotification> policyNotifications = createPolicyNotifications();

    // when we send the notifications to the scm notifier
    policyAlertScmNotifier.sendNotifications(application, SCAN_ID, new Stage(STAGE_ID), policyNotifications);

    // then we see executor was run to trigger our failure
    verify(pullRequestExecutor, timeout(Duration.ofSeconds(1).toMillis())).execute(any());

    // and no audits events were created
    awaitLogEntries(AuditEvent.CREATE_PULL_REQUEST, 0);
  }

  private List<PolicyNotification> createPolicyNotifications() {
    // create an evaluation in datastore
    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, SCAN_ID);

    final Policy policy = tempEntity.newPolicy(application);
    new PolicyDAO().update(policy);

    final List<PolicyNotification> policyNotifications = new ArrayList<>();
    policyNotifications.add(createPolicyNotification(policy, "hash1"));
    policyNotifications.add(createPolicyNotification(policy, "hash2"));
    policyNotifications.add(createPolicyNotification(policy, "hash3"));

    return policyNotifications;
  }

  private PolicyNotification createPolicyNotification(final Policy policy, final String hash) {
    final PolicyFact policyFact = new PolicyFact(policy.getId(), policy.getName(), policy.getThreatLevel());
    final ApplicationComponent component = tempEntity
        .newApplicationComponent(application.getId(), STAGE_ID, hash, MatchState.EXACT, false);
    policyFact.addComponentFact(new ComponentFact(component.getComponentIdentifier(), component.getHash()));
    return new PolicyNotification(policyFact, policy.getNotifications());
  }

  private void assertApplicationPolicyNotificationAuditData(
      final AuditDTO auditDTO,
      final String pullRequestUrl,
      final ComponentIdentifier componentIdentifier)
  {
    assertStandardData(auditDTO, AuditEvent.CREATE_PULL_REQUEST, null, SYSTEM_USER);
    assertApplicationData(auditDTO, application);
    assertCustomData(auditDTO, "scanId", SCAN_ID);
    assertCustomData(auditDTO, "stageId", STAGE_ID);
    assertCustomData(auditDTO, "pullRequestUrl", pullRequestUrl);
    assertCustomObject(auditDTO, "componentIdentifier", componentIdentifier);
  }

  private void givenGitServicesAvailable() throws IOException {
    when(gitClientFactory.createApiClient(any())).thenReturn(gitApiClient);
    when(gitApiClient.isBranchOnServer(any())).thenReturn(false);
  }

  private void givenGitRepositoryInfoAvailable() {
    final GitRepositoryInfo gitRepoInfo = new GitRepositoryInfo(
        "url", "token", null, "master", false, false);
    when(sourceControlUtils.getGitRepositoryInfoForApplication(any())).thenReturn(gitRepoInfo);
  }

  private void givenRemediationIsAvailable() {
    final Map<String, String> coordinates = new HashMap<>();
    coordinates.put("version", "shiny-new");

    final ApiComponentIdentifierDTOV2 componentIdentifier = new ApiComponentIdentifierDTOV2();
    componentIdentifier.setCoordinates(coordinates);

    final ApiComponentDTOV2 component = new ApiComponentDTOV2();
    component.componentIdentifier = componentIdentifier;

    final ApiComponentChangeActionDTO action = new ApiComponentChangeActionDTO();
    action.setComponent(component);

    final ApiVersionChangeOptionDTO apiVersionChange = new ApiVersionChangeOptionDTO();
    apiVersionChange.setData(action);

    final ApiComponentRemediationDTO remediation = new ApiComponentRemediationDTO();
    remediation.remediation.versionChanges.add(apiVersionChange);

    when(remediationService.getSuggestedRemediationForComponentNoAuth(
        any(), any(), any(), any(), eq(null), eq(null))).thenReturn(remediation);
  }

  private void givenSourceControlIsEnabled() throws IOException {
    final SourceControlConfig sourceControlConfig = new SourceControlConfig();
    sourceControlConfig.setCloneDirectory("clone_dir");
    config.setSourceControl(sourceControlConfig);

    when(pullRequestFeatureCheck.isPullRequestFeatureSupported(eq(application), any()))
        .thenReturn(true);
  }
}
