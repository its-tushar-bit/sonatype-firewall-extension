/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.features.FeaturesService;
import com.sonatype.insight.brain.git.PullRequestCommentingRemediationService;
import com.sonatype.insight.brain.git.PullRequestRemediationService;
import com.sonatype.insight.brain.git.RemediationBranchNamePrefixGenerator;
import com.sonatype.insight.brain.git.RemediationPullRequestFeatureCheck;
import com.sonatype.insight.brain.git.RemediationVersionDTO;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.shutdown.ShutdownPriority;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.test.LogOutput;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class PolicyAlertScmNotifierTest
    extends AbstractComponentTest
{
  private static final String PUBLIC_ID = "abc123";

  private static final String NAME = "reponame";

  @Mock
  private RemediationPullRequestFeatureCheck remediationPullRequestFeatureCheck;

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

  @Mock
  ShutdownHandler mockShutdownHandler;

  @Mock
  private FeaturesService featuresService;

  @Inject
  StageTypeService stageTypeService;

  private PolicyAlertScmNotifier scmNotifier;

  private Application application;

  @Rule
  public LogOutput logOutput = new LogOutput(PolicyAlertScmNotifier.class);

  @Inject
  private OrganizationDAO organizationDAO;

  @Captor
  private ArgumentCaptor<Thread> threadArgumentCaptor;

  @Before
  public void setup() {
    scmNotifier =
        new PolicyAlertScmNotifier(remediationPullRequestFeatureCheck, mockPullRequestCommentingRemediationService,
            new PolicyAlertSourceCodeOrganizer(), baseUrl, sourceControlUtils, mockPullRequestRemediationService,
            mockSourceControlEventPublisher, organizationDAO, mockShutdownHandler, featuresService, stageTypeService);
    Organization organization = tempEntity.newOrganization();
    application = tempEntity.newApplication(NAME, PUBLIC_ID, organization.getId());
  }

  @Test
  public void test_featureIsDisabled() {
    // given we have repository info for an application
    when(sourceControlUtils.getGitRepositoryInfoForApplication(application.getId()))
        .thenReturn(gitRepositoryInfo);

    // and feature is disabled
    when(remediationPullRequestFeatureCheck.isPullRequestFeatureSupported(application, gitRepositoryInfo))
        .thenReturn(false);

    // when we send notifications
    scmNotifier.sendNotifications(application, "scanId", new Stage("build"), buildPolicyNotification());

    // then we see no interaction with the source control event service
    verifyNoInteractions(mockSourceControlEventPublisher);
  }

  @Test
  public void test_developStageNotSupported() {
    // given we have repository info for an application
    when(sourceControlUtils.getGitRepositoryInfoForApplication(application.getId()))
        .thenReturn(gitRepositoryInfo);

    // when we send notifications
    scmNotifier.sendNotifications(application, "scanId", new Stage(Stage.ID_DEVELOP), buildPolicyNotification());

    // then we see no calls to the PR engine
    assertThat(logOutput).atDebugLevel().contains(
        "Ignoring Pull Request notification for the stage 'develop' for application 'abc123' and scan 'scanId'");
    verifyNoInteractions(mockSourceControlEventPublisher);
  }

  @Test
  public void test_complianceStageNotSupported() {
    // given we have repository info for an application
    when(sourceControlUtils.getGitRepositoryInfoForApplication(application.getId()))
            .thenReturn(gitRepositoryInfo);

    // when we send notifications
    scmNotifier.sendNotifications(application, "scanId", new Stage(Stage.ID_COMPLIANCE), buildPolicyNotification());

    // then we see no calls to the PR engine
    assertThat(logOutput).atDebugLevel().contains(
            "Ignoring Pull Request notification for the stage 'compliance' for application 'abc123' and scan 'scanId'");
    verifyNoInteractions(mockSourceControlEventPublisher);
  }

  @Test
  public void test_formatNotSupported() {
    // given we have repository info for an application
    when(sourceControlUtils.getGitRepositoryInfoForApplication(application.getId())).thenReturn(gitRepositoryInfo);

    // and feature is enabled
    when(remediationPullRequestFeatureCheck.isPullRequestFeatureSupported(application, gitRepositoryInfo))
        .thenReturn(true);

    // and a component with an unsupported format
    ComponentIdentifier componentWithUnsupportedFormat = ComponentIdentifier.createNugetCoordinates("foo", "1.2.3");

    // when we send policy notifications
    scmNotifier.sendNotifications(application, "scanId", new Stage("build"),
        List.of(buildPolicyNotification(componentWithUnsupportedFormat)));

    // then we see a message logged that the format is not supported
    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> assertThat(logOutput).atDebugLevel().contains(
        "Format 'nuget: {packageId=foo, version=1.2.3}' is not supported for automatic remediation"));

    // and the source control event service didn't publish an event
    verify(mockSourceControlEventPublisher, never()).publishEvent(any());
  }

  @Test
  public void test_noRemediationOptions() {
    // given we have repository info for an application
    when(sourceControlUtils.getGitRepositoryInfoForApplication(application.getId()))
        .thenReturn(gitRepositoryInfo);

    // and feature is enabled
    when(remediationPullRequestFeatureCheck.isPullRequestFeatureSupported(
        application, gitRepositoryInfo)).thenReturn(true);

    // and there are no suggested remediations
    when(mockPullRequestCommentingRemediationService.getRemediationVersion(
        any(), eq(application.getId()))).thenReturn(Optional.empty());

    when(mockPullRequestRemediationService.isFormatSupportedForPullRequestRemediation(any())).thenReturn(true);

    // when we send policy notifications
    scmNotifier.sendNotifications(application, "scanId", new Stage("build"), buildPolicyNotification());

    // then we see a message logged that there are no remediations
    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> assertThat(logOutput).atDebugLevel().contains(
        "No remediation options found for component [maven: {artifactId=Package1, groupId=groupid, version=1.2.3}]"));

    // and the source control event service didn't have an event published to it
    verify(mockSourceControlEventPublisher, never()).publishEvent(any());
    verify(mockShutdownHandler).add(threadArgumentCaptor.capture(), eq(ShutdownPriority.NOTIFICATIONS));
    assertThat(threadArgumentCaptor.getValue().getName()).startsWith("PolicyAlertScmNotifierForScan");
  }

  @Test
  public void test_remediationEventForBranchAlreadyExists() throws Exception {
    // given we have repository info for an application
    GitRepositoryInfo githubRepositoryInfo = new GitRepositoryInfo(null, null, null, null, SourceControlProvider.GITHUB,
        null, true, true, true, true, false, null);
    when(sourceControlUtils.getGitRepositoryInfoForApplication(application.getId()))
        .thenReturn(githubRepositoryInfo);

    // and feature is enabled
    when(remediationPullRequestFeatureCheck.isPullRequestFeatureSupported(
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
    scmNotifier.sendNotifications(application, "scanId", new Stage("build"), buildPolicyNotification());

    // then we DO NOT see an event created for a remediation PR
    assertThat(finished.await(5, TimeUnit.SECONDS)).isTrue();
    verify(mockSourceControlEventPublisher, never()).publishEvent(any());
  }

  @Test
  public void test_pullRequestEventCreated() throws Exception {
    // given we have repository info for an application
    GitRepositoryInfo githubRepositoryInfo = new GitRepositoryInfo(null, null, null, null, SourceControlProvider.GITHUB,
        null, true, true, true, true, false, null);
    when(sourceControlUtils.getGitRepositoryInfoForApplication(application.getId()))
        .thenReturn(githubRepositoryInfo);

    // and feature is enabled
    when(remediationPullRequestFeatureCheck.isPullRequestFeatureSupported(
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
    scmNotifier.sendNotifications(application, "scanId", new Stage("build"), buildPolicyNotification());

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

  @Test
  public void test_pullRequestEventCreated_NonBreakingWithDependenciesRemediationOnly() throws Exception {
    // given we have repository info for an application
    GitRepositoryInfo githubRepositoryInfo = new GitRepositoryInfo(null, null, null, null, SourceControlProvider.GITHUB,
        null, true, true, true, true, false, null);
    when(sourceControlUtils.getGitRepositoryInfoForApplication(application.getId()))
        .thenReturn(githubRepositoryInfo);

    // and the auto PR and suggest non-breaking features are enabled
    when(remediationPullRequestFeatureCheck.isPullRequestFeatureSupported(
        application, githubRepositoryInfo)).thenReturn(true);
    when(featuresService.getFeatures()).thenReturn(Set.of(
        SystemConfigurationPropertyFeature.DEVELOPER_SUGGEST_NON_BREAKING_VERSION));

    // and there are suggested remediations
    Optional<RemediationVersionDTO> remediationVersion1 = Optional.of(new RemediationVersionDTO("1.3.1",
        ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES));
    Optional<RemediationVersionDTO> remediationVersion2 = Optional.of(new RemediationVersionDTO("2.4.1",
        ApiVersionChangeOptionType.NEXT_NON_FAILING));
    Optional<RemediationVersionDTO> remediationVersion3 = Optional.of(new RemediationVersionDTO("5.7.1",
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS));
    when(mockPullRequestCommentingRemediationService.getRemediationVersion(
        any(), eq(application.getId())))
        .thenReturn(remediationVersion1)
        .thenReturn(remediationVersion2)
        .thenReturn(remediationVersion3)
        .thenReturn(Optional.empty());

    when(mockPullRequestRemediationService.isFormatSupportedForPullRequestRemediation(any())).thenReturn(true);

    when(baseUrl.getConfigured()).thenReturn("foo");
    String branchPrefix = new RemediationBranchNamePrefixGenerator().generatePrefixForApplication(application.getId());
    final String branchName1 = branchPrefix + "/group-1/package-1/1.2.3-to-1.3.1";
    final String branchName2 = branchPrefix + "/package-3/5.6.7-to-5.7.1";

    // and the branches do not yet exist in the event table
    when(mockSourceControlEventPublisher.doesRemediationEventExistForBranch(eq(application.getId()),
        or(eq(branchName1), eq(branchName2))))
        .thenReturn(false);

    CountDownLatch finished = new CountDownLatch(1);
    doAnswer(invocation -> {
      finished.countDown();
      return null;
    }).when(mockSourceControlEventPublisher).publishEvent(any());

    // when we send policy notifications where:
    // package-1 is a maven component with a non-breaking with dependencies remediation (eligible for Golden PR)
    // package-2 is a maven component with a non-failing remediation (not eligible for either Golden or regular PR)
    // package-3 is a golang component with a non-violating remediation (eligible for regular PR)
    // package-4 has no remediation (no PR)
    final ComponentIdentifier component1 = ComponentIdentifier.createMavenCoordinates("group-1", "package-1", "1.2.3");
    final ComponentIdentifier component2 = ComponentIdentifier.createMavenCoordinates("group-2", "package-2", "2.3.4");
    final ComponentIdentifier component3 = ComponentIdentifier.createGolangCoordinates("package-3", "5.6.7");
    final ComponentIdentifier component4 = ComponentIdentifier.createMavenCoordinates("group-4", "package-4", "8.9.10");
    final List<PolicyNotification> policyNotifications =
        List.of(buildPolicyNotification(component1),
            buildPolicyNotification(component2),
            buildPolicyNotification(component3),
            buildPolicyNotification(component4));
    scmNotifier.sendNotifications(application, "scanId", new Stage("build"), policyNotifications);

    // then we see an event created for a remediation PR
    assertThat(finished.await(15, TimeUnit.SECONDS)).isTrue();

    final ArgumentCaptor<SourceControlEvent> eventsCaptor = ArgumentCaptor.forClass(SourceControlEvent.class);
    final List<SourceControlEvent> events = new ArrayList<>();
    await().atMost(5, TimeUnit.SECONDS).until(() -> {
      verify(mockSourceControlEventPublisher, times(2)).publishEvent(eventsCaptor.capture());
      events.addAll(eventsCaptor.getAllValues());
      return eventsCaptor.getAllValues().size() == 2;
    });

    final SourceControlEvent event1 = events.get(0);
    assertThat(event1.getComponentIdentifier())
        .isEqualTo(component1);
    assertThat(event1.getRemediationVersion()).isEqualTo("1.3.1");
    assertThat(event1.getBranchName()).isEqualTo(branchName1);
    assertThat(event1.getApplicationId()).isEqualTo(application.getId());
    assertThat(event1.getEventPriority()).isEqualTo(SourceControlEvent.EVENT_PRIORITY_LOWER);

    final SourceControlEvent event2 = events.get(1);
    assertThat(event2.getComponentIdentifier())
        .isEqualTo(component3);
    assertThat(event2.getRemediationVersion()).isEqualTo("5.7.1");
    assertThat(event2.getBranchName()).isEqualTo(branchName2);
    assertThat(event2.getApplicationId()).isEqualTo(application.getId());
    assertThat(event2.getEventPriority()).isEqualTo(SourceControlEvent.EVENT_PRIORITY_LOWER);
  }

  private List<PolicyNotification> buildPolicyNotification() {
    return List.of(buildPolicyNotification(ComponentIdentifier.createMavenCoordinates("groupid", "Package1", "1.2.3")));
  }

  private PolicyNotification buildPolicyNotification(final ComponentIdentifier componentIdentifier) {
    PolicyFact policyFact1 = new PolicyFact("policyid-1", "policyname-1", 3);
    policyFact1.addComponentFact(new ComponentFact(componentIdentifier, randomString()));

    Notifications notifications = new Notifications(
        new UserNotification("foo@mail.com", "release")
    );
    return new PolicyNotification(policyFact1, notifications);
  }

  private String randomString() {
    return UUID.randomUUID().toString();
  }
}
