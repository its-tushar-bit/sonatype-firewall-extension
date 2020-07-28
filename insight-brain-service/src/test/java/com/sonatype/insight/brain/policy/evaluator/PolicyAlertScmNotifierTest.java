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
import com.sonatype.insight.brain.git.PullRequestFeatureCheck;
import com.sonatype.insight.brain.git.PullRequestRemediationService;
import com.sonatype.insight.brain.git.event.SourceControlEventService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
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

import static com.sonatype.insight.brain.policy.evaluator.PolicyAlertScmNotifier.APP_ID_BRANCH_TRUNCATE_INDEX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
  private ApiComponentRemediationService remediationService;

  @Mock
  private GitRepositoryInfo gitRepositoryInfo;

  @Mock
  private BaseUrl baseUrl;

  @Mock
  PullRequestRemediationService mockPullRequestRemediationService;

  @Mock
  SourceControlEventService mockSourceControlEventService;

  @Mock
  SourceControlUtils sourceControlUtils;

  private PolicyAlertScmNotifier scmNotifier;

  private Application application;

  @Rule
  public LogOutput logOutput = new LogOutput(PolicyAlertScmNotifier.class);

  @Before
  public void setup() {
    scmNotifier =
        new PolicyAlertScmNotifier(pullRequestFeatureCheck, remediationService, new PolicyAlertSourceCodeOrganizer(),
            baseUrl, sourceControlUtils, mockPullRequestRemediationService, mockSourceControlEventService);
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
    verifyNoInteractions(mockSourceControlEventService);
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
    verifyNoInteractions(mockSourceControlEventService);
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
    verify(mockSourceControlEventService, never()).publishEvent(any());
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
    ApiComponentRemediationDTO emptyRemediationDTO = new ApiComponentRemediationDTO();
    when(remediationService.getSuggestedRemediationForComponentNoAuth(
        any(ApiComponentDTOV2.class), eq(OwnerType.APPLICATION),
        eq(application.getId()), isNull(), isNull(), isNull())).thenReturn(emptyRemediationDTO);

    when(mockPullRequestRemediationService.isFormatSupportedForPullRequestRemediation(any())).thenReturn(true);

    // when we send policy notifications
    scmNotifier.sendNotifications(application, "scanId", new Stage("build"), buildPolicyNotifications());

    // then we see a message logged that there are no remediations
    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
      assertThat(logOutput).atDebugLevel().contains(
          "No remediation options found for component [maven: {artifactId=Package1, groupId=groupid, version=1.2.3}]");
    });

    // and the source control event service didn't have an event published to it
    verify(mockSourceControlEventService, never()).publishEvent(any());
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
    ApiComponentRemediationDTO remediationDTO = buildRemediationDTOWithSuggestion();
    when(remediationService.getSuggestedRemediationForComponentNoAuth(
        any(ApiComponentDTOV2.class), eq(OwnerType.APPLICATION),
        eq(application.getId()), isNull(), isNull(), isNull())).thenReturn(remediationDTO);

    when(mockPullRequestRemediationService.isFormatSupportedForPullRequestRemediation(any())).thenReturn(true);

    String truncatedAppId = application.getId().substring(0, APP_ID_BRANCH_TRUNCATE_INDEX);
    String branchName = truncatedAppId + "/groupid/Package1/1.2.3-to-2.0.1";

    // and the branch already exists in the event table
    CountDownLatch finished = new CountDownLatch(1);
    doAnswer(invocation -> {
      finished.countDown();
      return true;
    }).when(mockSourceControlEventService).doesRemediationEventExistForBranch(eq(application.getId()), eq(branchName));

    // when we send policy notifications
    scmNotifier.sendNotifications(application, "scanId", new Stage("build"), buildPolicyNotifications());

    // then we DO NOT see an event created for a remediation PR
    assertThat(finished.await(5, TimeUnit.SECONDS)).isTrue();
    verify(mockSourceControlEventService, never()).publishEvent(any());
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
    ApiComponentRemediationDTO remediationDTO = buildRemediationDTOWithSuggestion();
    when(remediationService.getSuggestedRemediationForComponentNoAuth(
        any(ApiComponentDTOV2.class), eq(OwnerType.APPLICATION),
        eq(application.getId()), isNull(), isNull(), isNull())).thenReturn(remediationDTO);

    when(mockPullRequestRemediationService.isFormatSupportedForPullRequestRemediation(any())).thenReturn(true);

    when(baseUrl.getConfigured()).thenReturn("foo");
    String truncatedAppId = application.getId().substring(0, APP_ID_BRANCH_TRUNCATE_INDEX);
    final String branchName = truncatedAppId + "/groupid/Package1/1.2.3-to-2.0.1";

    // and the branch does not yet exist in the event table
    when(mockSourceControlEventService.doesRemediationEventExistForBranch(eq(application.getId()), eq(branchName)))
        .thenReturn(false);

    CountDownLatch finished = new CountDownLatch(1);
    doAnswer(invocation -> {
      finished.countDown();
      return null;
    }).when(mockSourceControlEventService).publishEvent(any());

    // when we send policy notifications
    scmNotifier.sendNotifications(application, "scanId", new Stage("build"), buildPolicyNotifications());

    // then we see an event created for a remediation PR
    assertThat(finished.await(5, TimeUnit.SECONDS)).isTrue();

    verify(mockSourceControlEventService).publishEvent(argThat(event -> {
      assertThat(event.getComponentIdentifier())
          .isEqualTo(ComponentIdentifier.createMavenCoordinates("groupid", "Package1", "1.2.3"));
      assertThat(event.getRemediationVersion()).isEqualTo("2.0.1");
      assertThat(event.getBranchName()).isEqualTo(branchName);
      assertThat(event.getApplicationId()).isEqualTo(application.getId());
      return true;
    }));
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
