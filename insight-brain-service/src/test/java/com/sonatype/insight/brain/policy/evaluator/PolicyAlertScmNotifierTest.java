/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Provider;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.actions.ApiComponentChangeActionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.api.v2.service.ApiComponentRemediationService;
import com.sonatype.insight.brain.git.GitApiService;
import com.sonatype.insight.brain.git.GitClientFactory;
import com.sonatype.insight.brain.git.GitRepositoryInfo;
import com.sonatype.insight.brain.git.PullRequestFeatureCheck;
import com.sonatype.insight.brain.git.PullRequestTask;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.test.LogOutput;
import com.sonatype.nexus.scm.api.GitApiClient;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static com.sonatype.insight.brain.policy.evaluator.PolicyAlertScmNotifier.APP_ID_BRANCH_TRUNCATE_INDEX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class PolicyAlertScmNotifierTest
    extends AbstractComponentTest
{
  private static final String PUBLIC_ID = "abc123";

  private static final String NAME = "reponame";

  @Mock
  private PullRequestFeatureCheck pullRequestFeatureCheck;

  @Mock
  private ApiComponentRemediationService remediationService;

  @Mock
  private GitClientFactory gitClientFactory;

  @Mock
  private GitApiService gitApiService;

  @Mock
  private GitRepositoryInfo gitRepositoryInfo;

  @Mock
  private GitApiClient gitApiClient;

  @Mock
  private BaseUrl baseUrl;

  @Mock
  Provider<PullRequestTask> provider;

  @Mock
  PullRequestTask pullRequestTask;

  private PolicyAlertScmNotifier scmNotifier;

  private Application application;

  @Rule
  public LogOutput logOutput = new LogOutput(PolicyAlertScmNotifier.class);

  @Before
  public void setup() throws Exception {
    scmNotifier = new PolicyAlertScmNotifier(pullRequestFeatureCheck,
        remediationService, new PolicyAlertSourceCodeOrganizer(), gitClientFactory,
        gitApiService, baseUrl, provider);
    Organization organization = tempEntity.newOrganization();
    application = tempEntity.newApplication(NAME, PUBLIC_ID, organization.getId());

    // TODO remove when SCM notifier is enabled
    System.setProperty("enableScmNotification", "true");
  }

  // TODO remove when SCM notifier is enabled
  @Test
  public void test_featureIsDisabledByProperties() throws Exception {
    // given property flag is not present
    System.clearProperty("enableScmNotification");

    // when we send notifications
    scmNotifier.sendNotifications(application, "scanId", new Stage(Stage.ID_BUILD), buildPolicyNotifications());

    // then no interactions
    verifyNoInteractions(gitApiService, pullRequestFeatureCheck);
  }

  @Test
  public void test_featureIsDisabled() throws Exception {
    // given we have repository info for an application
    when(gitApiService.getGitRepositoryInfoForApplication(application.getId()))
        .thenReturn(gitRepositoryInfo);

    // and feature is disabled
    when(pullRequestFeatureCheck.isPullRequestFeatureSupported(
        application, gitRepositoryInfo)).thenReturn(false);

    // when we send notifications
    scmNotifier.sendNotifications(application, "scanId", new Stage("build"), buildPolicyNotifications());

    // then we see no calls to the PR engine
    assertThat(logOutput).atAnyLevel().doesNotContain("Invoke PR engine to construct a PR");
  }

  @Test
  public void test_formatNotSupported() throws Exception {
    // given we have repository info for an application
    when(gitApiService.getGitRepositoryInfoForApplication(application.getId())).thenReturn(gitRepositoryInfo);

    // and feature is enabled
    when(pullRequestFeatureCheck.isPullRequestFeatureSupported(application, gitRepositoryInfo)).thenReturn(true);

    // but the format is not supported

    // when we send policy notifications
    scmNotifier.sendNotifications(application, "scanId", new Stage("build"),
        buildPolicyNotifications(ComponentIdentifier.createNugetCoordinates("foo", "1.2.3")));

    // then we see a message logged that the format is not supported
    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
      assertThat(logOutput).atDebugLevel().contains(
          "Format 'nuget: {packageId=foo, version=1.2.3}' is not supported for automatic remediation");
    });

    // and PR engine didn't run
    assertThat(logOutput).atDebugLevel().doesNotContain("Invoke PR engine to construct a PR");
  }

  @Test
  public void test_noRemediationOptions() throws Exception {
    // given we have repository info for an application
    when(gitApiService.getGitRepositoryInfoForApplication(application.getId()))
        .thenReturn(gitRepositoryInfo);

    // and feature is enabled
    when(pullRequestFeatureCheck.isPullRequestFeatureSupported(
        application, gitRepositoryInfo)).thenReturn(true);

    // and there are no suggested remediations
    ApiComponentRemediationDTO emptyRemediationDTO = new ApiComponentRemediationDTO();
    when(remediationService.getSuggestedRemediationForComponent(
        any(ApiComponentDTOV2.class), eq(OwnerType.APPLICATION),
        eq(application.getId()), isNull())).thenReturn(emptyRemediationDTO);

    // when we send policy notifications
    scmNotifier.sendNotifications(application, "scanId", new Stage("build"), buildPolicyNotifications());

    // then we see a message logged that there are no remediations
    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
      assertThat(logOutput).atDebugLevel().contains(
          "No remediation options found for component [maven: {artifactId=Package1, groupId=groupid, version=1.2.3}]");
    });

    // and PR engine didn't run
    assertThat(logOutput).atDebugLevel().doesNotContain(
        "Invoke PR engine to construct a PR");
  }

  @Test
  public void test_branchAlreadyExists_Stop() throws Exception {
    // given we have repository info for an application
    when(gitApiService.getGitRepositoryInfoForApplication(application.getId()))
        .thenReturn(gitRepositoryInfo);

    // and feature is enabled
    when(pullRequestFeatureCheck.isPullRequestFeatureSupported(
        application, gitRepositoryInfo)).thenReturn(true);

    // and there are suggested remediations
    ApiComponentRemediationDTO remediationDTO = buildRemediationDTOWithSuggestion();
    when(remediationService.getSuggestedRemediationForComponent(
        any(ApiComponentDTOV2.class), eq(OwnerType.APPLICATION),
        eq(application.getId()), isNull())).thenReturn(remediationDTO);

    // and the branch already exists on the server
    when(gitClientFactory.create(gitRepositoryInfo)).thenReturn(gitApiClient);
    String truncatedAppId = application.getId().substring(0, APP_ID_BRANCH_TRUNCATE_INDEX);
    when(gitApiClient.isBranchOnServer(truncatedAppId + "/groupid/Package1/1.2.3-to-2.0.1")).thenReturn(true);

    // when we send notifications to our scm notifier
    scmNotifier.sendNotifications(application, "scanId", new Stage("build"), buildPolicyNotifications());

    // then we see a log that the branch already exists
    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
      assertThat(logOutput).atInfoLevel().contains(
          "Branch already exists on remote server for remediation [" + truncatedAppId +
              "/groupid/Package1/1.2.3-to-2.0.1]");
    });

    // and PR engine didn't run
    assertThat(logOutput).atAnyLevel().doesNotContain("Invoke PR engine");
  }

  @Test
  public void test_invokePREngine() throws Exception {
    // given we have repository info for an application
    when(gitApiService.getGitRepositoryInfoForApplication(application.getId()))
        .thenReturn(gitRepositoryInfo);

    // and feature is enabled
    when(pullRequestFeatureCheck.isPullRequestFeatureSupported(
        application, gitRepositoryInfo)).thenReturn(true);

    // and there are suggested remediations
    ApiComponentRemediationDTO remediationDTO = buildRemediationDTOWithSuggestion();
    when(remediationService.getSuggestedRemediationForComponent(
        any(ApiComponentDTOV2.class), eq(OwnerType.APPLICATION),
        eq(application.getId()), isNull())).thenReturn(remediationDTO);

    // and the branch doesn't already exist on the server
    when(gitClientFactory.create(gitRepositoryInfo)).thenReturn(gitApiClient);
    String truncatedAppId = application.getId().substring(0, APP_ID_BRANCH_TRUNCATE_INDEX);
    when(gitApiClient.isBranchOnServer(truncatedAppId + "/groupid/Package1/1.2.3-to-2.0.1")).thenReturn(false);

    String branchName = truncatedAppId + "/groupid/Package1/1.2.3-to-2.0.1";
    when(gitApiClient.isBranchOnServer(branchName)).thenReturn(false);
    when(baseUrl.getConfigured()).thenReturn("foo");
    when(provider.get()).thenReturn(pullRequestTask);
    CountDownLatch finished = new CountDownLatch(1);
    doAnswer(invocation -> {
      finished.countDown();
      return null;
    }).when(pullRequestTask).run();

    // when we send policy notifications
    scmNotifier.sendNotifications(application,"scanId", new Stage("build"),  buildPolicyNotifications());

    // then we see the PR engine run for the component
    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
      assertThat(logOutput).atInfoLevel().contains("Executing pull request task for [maven: {artifactId=Package1," +
          " groupId=groupid, version=1.2.3}] on application with id [" + application.getId() + "]");
    });

    ArgumentCaptor<PullRequestRemediationDetails> captor =
        ArgumentCaptor.forClass(PullRequestRemediationDetails.class);
    verify(pullRequestTask).init(captor.capture());
    await().atMost(1, TimeUnit.SECONDS).until( () -> finished.getCount() == 0);
    verify(pullRequestTask).run();
    assertThat(captor.getValue().getToBeRemediated())
        .isEqualTo(ComponentIdentifier.createMavenCoordinates("groupid", "Package1", "1.2.3"));
    assertThat(captor.getValue().getRemediatedVersion()).isEqualTo("2.0.1");
    assertThat(captor.getValue().getPullRequestBranchName()).isEqualTo(branchName);
    assertThat(captor.getValue().getApp()).isEqualTo(application);
  }

  private ApiComponentRemediationDTO buildRemediationDTOWithSuggestion() {
    ApiVersionChangeOptionDTO versionChangeOptionDTO = new ApiVersionChangeOptionDTO();
    versionChangeOptionDTO.setType(ApiVersionChangeOptionType.NEXT_NON_FAILING);
    ApiComponentChangeActionDTO changeActionDTO = new ApiComponentChangeActionDTO();
    ApiComponentDTOV2 componentDTOV2 = new ApiComponentDTOV2();
    changeActionDTO.setComponent(componentDTOV2);
    versionChangeOptionDTO.setData(changeActionDTO);
    // upgrade version
    componentDTOV2.componentIdentifier = ApiComponentIdentifierDTOV2
        .fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("groupid", "Package1", "2.0.1"));
    ApiComponentRemediationDTO remediationDTO = new ApiComponentRemediationDTO();
    remediationDTO.remediation.versionChanges = Arrays.asList(versionChangeOptionDTO);
    return remediationDTO;
  }

  private List<PolicyNotification> buildPolicyNotifications() {
    return buildPolicyNotifications(ComponentIdentifier.createMavenCoordinates("groupid", "Package1", "1.2.3"));
  }

  private List<PolicyNotification> buildPolicyNotifications(final ComponentIdentifier componentIdentifier) {
    PolicyFact policyFact1 = new PolicyFact("policyid-1", "policyname-1", 3);
    policyFact1.addComponentFact(new ComponentFact(componentIdentifier, randomString()));

    Notifications notifications = new Notifications(
        new UserNotification("foo@mail.com", "release")
    );
    PolicyNotification policyNotification1 = new PolicyNotification(policyFact1, notifications);
    return Arrays.asList(policyNotification1);
  }

  private String randomString() {
    return UUID.randomUUID().toString();
  }
}
