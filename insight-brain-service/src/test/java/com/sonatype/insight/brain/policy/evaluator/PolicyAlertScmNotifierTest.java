/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.PolicyFact;
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
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.test.LogOutput;
import com.sonatype.nexus.scm.api.GitApiClient;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class PolicyAlertScmNotifierTest
    extends AbstractComponentTest
{
  private static final String PUBLIC_ID = "abc123";

  private static final String NAME = "reponame";

  private static final String ORGANIZATION_ID = "sonatype";

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

  private PolicyAlertScmNotifier scmNotifier;

  private Application application;

  @Rule
  public LogOutput logOutput = new LogOutput(PolicyAlertScmNotifier.class);

  @Before
  public void setup() throws Exception {
    scmNotifier = new PolicyAlertScmNotifier(pullRequestFeatureCheck,
        remediationService, new PolicyAlertSourceCodeOrganizer(), gitClientFactory,
        gitApiService);
    application = new Application(PUBLIC_ID, NAME, ORGANIZATION_ID);
    application.setId("app-id");

    // TODO remove when SCM notifier is enabled
    System.setProperty("enableScmNotification", "true");
  }

  // TODO remove when SCM notifier is enabled
  @Test
  public void test_featureIsDisabledByProperties() throws Exception {
    // given property flag is not present
    System.clearProperty("enableScmNotification");

    // when we send notifications
    scmNotifier.sendNotifications(application, buildPolicyNotifications());

    // then no interactions
    verifyZeroInteractions(gitApiService, pullRequestFeatureCheck);
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
    scmNotifier.sendNotifications(application, buildPolicyNotifications());

    // then we see no calls to the PR engine
    assertThat(logOutput).atAnyLevel().doesNotContain("Invoke PR engine to construct a PR");
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
    scmNotifier.sendNotifications(application, buildPolicyNotifications());

    // then we see a message logged that there are no remediations
    assertThat(logOutput).atDebugLevel().contains(
        "No remediation options found for component [nuget: {packageId=Package1, version=1.2.3}]");

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
    when(gitApiClient.isBranchOnServer("Package1/1.2.3-to-2.0.1")).thenReturn(true);

    // when we send notifications to our scm notifier
    scmNotifier.sendNotifications(application, buildPolicyNotifications());

    // then we see a log that the branch already exists
    assertThat(logOutput).atDebugLevel().contains(
        "Branch already exists for remediation [Package1/1.2.3-to-2.0.1]");

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
    when(gitApiClient.isBranchOnServer("Package1/1.2.3-to-2.0.1")).thenReturn(false);

    // when we send policy notifications
    scmNotifier.sendNotifications(application, buildPolicyNotifications());

    // then we see the PR engine run for the component
    assertThat(logOutput).atDebugLevel().contains(
        "Invoke PR engine to construct a PR for [nuget: {packageId=Package1, version=1.2.3}]");
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
        .fromComponentIdentifier(ComponentIdentifier.createNugetCoordinates("Package1", "2.0.1"));
    ApiComponentRemediationDTO remediationDTO = new ApiComponentRemediationDTO();
    remediationDTO.remediation.versionChanges = Arrays.asList(versionChangeOptionDTO);
    return remediationDTO;
  }

  private List<PolicyNotification> buildPolicyNotifications() {
    PolicyFact policyFact1 = new PolicyFact("policyid-1", "policyname-1", 3);
    policyFact1.addComponentFact(
        new ComponentFact(ComponentIdentifier.createNugetCoordinates("Package1", "1.2.3"), randomString()));

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
