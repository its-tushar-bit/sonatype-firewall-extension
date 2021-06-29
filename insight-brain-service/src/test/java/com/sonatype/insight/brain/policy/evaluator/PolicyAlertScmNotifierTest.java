/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.git.PullRequestCommentingRemediationService;
import com.sonatype.insight.brain.git.PullRequestFeatureCheck;
import com.sonatype.insight.brain.git.PullRequestRemediationService;
import com.sonatype.insight.brain.git.RemediationBranchNamePrefixGenerator;
import com.sonatype.insight.brain.git.RemediationVersionDTO;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.test.LogOutput;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
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
  private PullRequestCommentingRemediationService mockPullRequestCommentingRemediationService;

  @Mock
  private GitRepositoryInfo gitRepositoryInfo;

  @Mock
  private BaseUrl baseUrl;

  @Mock
  PullRequestRemediationService mockPullRequestRemediationService;

  @Mock
  SourceControlEventPublisher mockSourceControlEventPublisher;

  @Mock
  SourceControlUtils sourceControlUtils;

  private PolicyAlertScmNotifier scmNotifier;

  private Application application;

  @Rule
  public LogOutput logOutput = new LogOutput(PolicyAlertScmNotifier.class);

  @Before
  public void setup() {
    scmNotifier =
        new PolicyAlertScmNotifier(pullRequestFeatureCheck, mockPullRequestCommentingRemediationService,
            new PolicyAlertSourceCodeOrganizer(), baseUrl, sourceControlUtils, mockPullRequestRemediationService,
            mockSourceControlEventPublisher);
    Organization organization = tempEntity.newOrganization();
    application = tempEntity.newApplication(NAME, PUBLIC_ID, organization.getId());
  }

  @Test
  public void test_featureIsDisabled() {
    // given we have repository info for an application
    when(sourceControlUtils.getGitRepositoryInfoForApplication(application.getId()))
        .thenReturn(gitRepositoryInfo);

    // and feature is disabled
    when(pullRequestFeatureCheck.isPullRequestFeatureSupported(application, gitRepositoryInfo)).thenReturn(false);

    // when we send notifications
    scmNotifier.sendNotifications(application, "scanId", new Stage("build"), buildPolicyNotifications());

    // then we see no interaction with the source control event service
    verifyNoInteractions(mockSourceControlEventPublisher);
  }

  @Test
  public void test_developStageNotSupported() {
    // given we have repository info for an application
    when(sourceControlUtils.getGitRepositoryInfoForApplication(application.getId()))
        .thenReturn(gitRepositoryInfo);

    // when we send notifications
    scmNotifier.sendNotifications(application, "scanId", new Stage(Stage.ID_DEVELOP), buildPolicyNotifications());

    // then we see no calls to the PR engine
    assertThat(logOutput).atDebugLevel().contains(
        "Ignoring Pull Request notification for the 'develop' stage for application 'abc123' and scan 'scanId'");
    verifyNoInteractions(mockSourceControlEventPublisher);
  }

  @Test
  public void test_formatNotSupported() {
    // given we have repository info for an application
    when(sourceControlUtils.getGitRepositoryInfoForApplication(application.getId())).thenReturn(gitRepositoryInfo);

    // and feature is enabled
    when(pullRequestFeatureCheck.isPullRequestFeatureSupported(application, gitRepositoryInfo)).thenReturn(true);

    // and a component with an unsupported format
    ComponentIdentifier componentWithUnsupportedFormat = ComponentIdentifier.createNugetCoordinates("foo", "1.2.3");

    // when we send policy notifications
    scmNotifier.sendNotifications(application, "scanId", new Stage("build"),
        buildPolicyNotifications(componentWithUnsupportedFormat));

    // then we see a message logged that the format is not supported
    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
      assertThat(logOutput).atDebugLevel().contains(
          "Format 'nuget: {packageId=foo, version=1.2.3}' is not supported for automatic remediation");
    });

    // and the source control event service didn't publish an event
    verify(mockSourceControlEventPublisher, never()).publishEvent(any());
  }

  @Test
  public void test_noRemediationOptions() {
    // given we have repository info for an application
    when(sourceControlUtils.getGitRepositoryInfoForApplication(application.getId()))
        .thenReturn(gitRepositoryInfo);

    // and feature is enabled
    when(pullRequestFeatureCheck.isPullRequestFeatureSupported(
        application, gitRepositoryInfo)).thenReturn(true);

    // and there are no suggested remediations
    when(mockPullRequestCommentingRemediationService.getRemediationVersion(
        any(), eq(application.getId()))).thenReturn(Optional.empty());

    when(mockPullRequestRemediationService.isFormatSupportedForPullRequestRemediation(any())).thenReturn(true);

    // when we send policy notifications
    scmNotifier.sendNotifications(application, "scanId", new Stage("build"), buildPolicyNotifications());

    // then we see a message logged that there are no remediations
    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
      assertThat(logOutput).atDebugLevel().contains(
          "No remediation options found for component [maven: {artifactId=Package1, groupId=groupid, version=1.2.3}]");
    });

    // and the source control event service didn't have an event published to it
    verify(mockSourceControlEventPublisher, never()).publishEvent(any());
  }

  @Test
  public void test_remediationEventForBranchAlreadyExists() throws Exception {
    // given we have repository info for an application
    GitRepositoryInfo githubRepositoryInfo =
        new GitRepositoryInfo(null, null, null, SourceControlProvider.GITHUB, null, true, true);
    when(sourceControlUtils.getGitRepositoryInfoForApplication(application.getId()))
        .thenReturn(githubRepositoryInfo);

    // and feature is enabled
    when(pullRequestFeatureCheck.isPullRequestFeatureSupported(
        application, githubRepositoryInfo)).thenReturn(true);

    // and there are suggested remediations
    Optional<RemediationVersionDTO> remediationVersionOptional = Optional.of(new RemediationVersionDTO("2.0.1",
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS));
    when(mockPullRequestCommentingRemediationService.getRemediationVersion(
        any(), eq(application.getId()))).thenReturn(remediationVersionOptional);

    when(mockPullRequestRemediationService.isFormatSupportedForPullRequestRemediation(any())).thenReturn(true);

    String branchPrefix = new RemediationBranchNamePrefixGenerator().generatePrefixForApplication(application.getId());
    String branchName = branchPrefix + "/groupid/Package1/1.2.3-to-2.0.1";

    // and the branch already exists in the event table
    CountDownLatch finished = new CountDownLatch(1);
    doAnswer(invocation -> {
      finished.countDown();
      return true;
    }).when(mockSourceControlEventPublisher)
        .doesRemediationEventExistForBranch(eq(application.getId()), eq(branchName));

    // when we send policy notifications
    scmNotifier.sendNotifications(application, "scanId", new Stage("build"), buildPolicyNotifications());

    // then we DO NOT see an event created for a remediation PR
    assertThat(finished.await(5, TimeUnit.SECONDS)).isTrue();
    verify(mockSourceControlEventPublisher, never()).publishEvent(any());
  }

  @Test
  public void test_pullRequestEventCreated() throws Exception {
    // given we have repository info for an application
    GitRepositoryInfo githubRepositoryInfo =
        new GitRepositoryInfo(null, null, null, SourceControlProvider.GITHUB, null, true, true);
    when(sourceControlUtils.getGitRepositoryInfoForApplication(application.getId()))
        .thenReturn(githubRepositoryInfo);

    // and feature is enabled
    when(pullRequestFeatureCheck.isPullRequestFeatureSupported(
        application, githubRepositoryInfo)).thenReturn(true);

    // and there are suggested remediations
    Optional<RemediationVersionDTO> remediationVersionOptional = Optional.of(new RemediationVersionDTO("2.0.1",
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS));
    when(mockPullRequestCommentingRemediationService.getRemediationVersion(
        any(), eq(application.getId()))).thenReturn(remediationVersionOptional);

    when(mockPullRequestRemediationService.isFormatSupportedForPullRequestRemediation(any())).thenReturn(true);

    when(baseUrl.getConfigured()).thenReturn("foo");
    String branchPrefix = new RemediationBranchNamePrefixGenerator().generatePrefixForApplication(application.getId());
    final String branchName = branchPrefix + "/groupid/Package1/1.2.3-to-2.0.1";

    // and the branch does not yet exist in the event table
    when(mockSourceControlEventPublisher.doesRemediationEventExistForBranch(eq(application.getId()), eq(branchName)))
        .thenReturn(false);

    CountDownLatch finished = new CountDownLatch(1);
    doAnswer(invocation -> {
      finished.countDown();
      return null;
    }).when(mockSourceControlEventPublisher).publishEvent(any());

    // when we send policy notifications
    scmNotifier.sendNotifications(application, "scanId", new Stage("build"), buildPolicyNotifications());

    // then we see an event created for a remediation PR
    assertThat(finished.await(5, TimeUnit.SECONDS)).isTrue();

    verify(mockSourceControlEventPublisher).publishEvent(argThat(event -> {
      assertThat(event.getComponentIdentifier())
          .isEqualTo(ComponentIdentifier.createMavenCoordinates("groupid", "Package1", "1.2.3"));
      assertThat(event.getRemediationVersion()).isEqualTo("2.0.1");
      assertThat(event.getBranchName()).isEqualTo(branchName);
      assertThat(event.getApplicationId()).isEqualTo(application.getId());
      assertThat(event.getEventPriority()).isEqualTo(SourceControlEvent.EVENT_PRIORITY_LOWER);
      return true;
    }));
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
