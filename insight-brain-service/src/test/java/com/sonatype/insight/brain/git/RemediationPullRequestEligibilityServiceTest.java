/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import jakarta.inject.Inject;

import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.sourcecontrol.PullRequestState;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.test.LogOutput;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.github.dto.GithubUser;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.hc.core5.http.HttpHeaders;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.testing.AbstractBaseIntegrationTest.testProductLicense;
import static org.assertj.core.api.Assertions.assertThat;

public class RemediationPullRequestEligibilityServiceTest
    extends AbstractComponentTest
{
  @Rule
  public LogOutput remediationPullRequestEligibilityServiceLogOutput =
      new LogOutput(RemediationPullRequestEligibilityService.class);

  @Rule
  public LogOutput pullRequestFeatureCheckLogOutput = new LogOutput(PullRequestFeatureCheck.class);

  @Rule
  public WireMockRule gitService = new WireMockRule(wireMockConfig().dynamicPort());

  @Inject
  private RemediationPullRequestEligibilityService eligibilityService;

  private Application application;

  private ComponentIdentifier mavenComponent;

  private Stage stage;

  @Before
  public void setup() {
    application = tempEntity.newApplicationWithParent("app");
    mavenComponent = ComponentIdentifier.createMavenCoordinates("group", "artifact", "1.0.0");
    stage = new Stage("build");

    testProductLicense.setMissingFeatures(LicensedFeature.AUTOMATION);
    tempEntity.newPolicyEvaluation(application.getId(), stage.getStageTypeId(), "scanId");
    GithubUser githubUser = new GithubUser();
    githubUser.setGlobalId("userId");
    gitService.stubFor(get("/api/v3/user").withHeader("Authorization", matching("token token"))
        .willReturn(aResponse().withStatus(200).withBody(JsonUtils.format(githubUser))));
    gitService.stubFor(get(urlPathMatching("/api/v3/repos/[^/]+/[^/]+"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody("{ \"private\": true }")));
  }

  @Test
  public void testIsEligibleForAutoPullRequest_success() throws PlexusCipherException {
    setupSourceControl(false);
    boolean result = eligibilityService.isEligibleForAutoPullRequest(
        application, stage, mavenComponent, false, true, null);
    assertThat(result).isTrue();
  }

  @Test
  public void testIsEligibleForAutoPullRequest_unsupportedStage() {
    Stage developStage = new Stage(Stage.ID_DEVELOP);
    boolean result = eligibilityService.isEligibleForAutoPullRequest(
        application, developStage, mavenComponent, false, true, null);
    assertThat(result).isFalse();
    assertThat(remediationPullRequestEligibilityServiceLogOutput).atDebugLevel()
        .contains("Pull Request not supported for the stage 'develop'");
  }

  @Test
  public void testIsEligibleForAutoPullRequest_unsupportedFormat() {
    ComponentIdentifier pypiComponent =
        ComponentIdentifier.createPypiCoordinates("PyYAML", "3.11", "win-amd64-py2.7", "exe");
    boolean result = eligibilityService.isEligibleForAutoPullRequest(
        application, stage, pypiComponent, false, true, null);
    assertThat(result).isFalse();
    assertThat(remediationPullRequestEligibilityServiceLogOutput).atDebugLevel()
        .contains("Format 'pypi' is not supported for remediation");
  }

  @Test
  public void testIsEligibleForAutoPullRequest_innerSource_success() throws PlexusCipherException {
    setupSourceControl(true);
    boolean result = eligibilityService.isEligibleForAutoPullRequest(
        application, stage, mavenComponent, true, true, null);
    assertThat(result).isTrue();
  }

  @Test
  public void testIsEligibleForAutoPullRequest_innerSource_unsupportedStage() {
    Stage developStage = new Stage(Stage.ID_DEVELOP);
    boolean result = eligibilityService.isEligibleForAutoPullRequest(
        application, developStage, mavenComponent, true, true, null);
    assertThat(result).isFalse();
    assertThat(remediationPullRequestEligibilityServiceLogOutput).atDebugLevel()
        .contains("Pull Request not supported for the stage 'develop'");
  }

  @Test
  public void testIsEligibleForAutoPullRequest_innerSource_unsupportedFormat() {
    ComponentIdentifier pypiComponent =
        ComponentIdentifier.createPypiCoordinates("PyYAML", "3.11", "win-amd64-py2.7", "exe");
    boolean result = eligibilityService.isEligibleForAutoPullRequest(
        application, stage, pypiComponent, true, true, null);
    assertThat(result).isFalse();
    assertThat(remediationPullRequestEligibilityServiceLogOutput).atDebugLevel()
        .contains("Format 'pypi' is not supported for remediation");
  }

  @Test
  public void testIsEligibleForAutoPullRequest_innerSource_innerSourceUpdatesDisabled() throws PlexusCipherException {
    setupSourceControl(false);
    boolean result =
        eligibilityService.isEligibleForAutoPullRequest(application, stage, mavenComponent, true, true, null);
    assertThat(result).isFalse();
    assertThat(pullRequestFeatureCheckLogOutput).atDebugLevel()
        .contains("InnerSource Pull Requests have been explicitly disabled");
  }

  @Test
  public void testIsEligibleForAutoPullRequest_nonDirectDependency() {
    boolean result = eligibilityService.isEligibleForAutoPullRequest(
        application, stage, mavenComponent, false, false, null);
    assertThat(result).isFalse();
    assertThat(remediationPullRequestEligibilityServiceLogOutput)
        .atDebugLevel()
        .contains("Component '" + mavenComponent + "' is not a direct dependency.");
  }

  // manual pr section
  @Test
  public void testIsEligibleForManualPullRequest_success() throws PlexusCipherException {
    setupSourceControl(false);
    boolean result = eligibilityService.isEligibleForManualPullRequest(
        application, stage, mavenComponent, true, null);
    assertThat(result).isTrue();
  }

  @Test
  public void testIsEligibleForManualPullRequest_unsupportedFormat() {
    ComponentIdentifier pypiComponent =
        ComponentIdentifier.createPypiCoordinates("PyYAML", "3.11", "win-amd64-py2.7", "exe");
    boolean result = eligibilityService.isEligibleForManualPullRequest(
        application, stage, pypiComponent, true, null);
    assertThat(result).isFalse();
    assertThat(remediationPullRequestEligibilityServiceLogOutput).atDebugLevel()
        .contains("Format 'pypi' is not supported for remediation");
  }

  @Test
  public void testIsEligibleForManualPullRequest_unsupportedStage() {
    boolean result = eligibilityService.isEligibleForManualPullRequest(
        application, new Stage(Stage.ID_DEVELOP), mavenComponent, true, null);
    assertThat(result).isFalse();
    assertThat(remediationPullRequestEligibilityServiceLogOutput).atDebugLevel()
        .contains("Pull Request not supported for the stage 'develop'");
  }

  @Test
  public void testIsEligibleForManualPullRequest_nonDirectDependency() {
    boolean result = eligibilityService.isEligibleForManualPullRequest(
        application, stage, mavenComponent, false, null);
    assertThat(result).isFalse();
    assertThat(remediationPullRequestEligibilityServiceLogOutput)
        .atDebugLevel()
        .contains("Component '" + mavenComponent + "' is not a direct dependency.");
  }

  @Test
  public void testIsRemediationWaitingOrDone() {
    var policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), stage.getStageTypeId(), "scanId");

    tempEntity.newSourceControlEvent(application, policyEvaluation, "b1",
        SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT, SourceControlEvent.EVENT_STATUS_NEW);
    tempEntity.newSourceControlEvent(application, policyEvaluation, "b2",
        SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT, SourceControlEvent.EVENT_STATUS_IN_PROGRESS);
    tempEntity.newSourceControlEvent(application, policyEvaluation, "b3",
        SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT, SourceControlEvent.EVENT_STATUS_COMPLETE);
    tempEntity.newSourceControlEvent(application, policyEvaluation, "b4",
        SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT, SourceControlEvent.EVENT_STATUS_ERROR);

    tempEntity.newSourceControlEvent(application, policyEvaluation, "b5",
        SourceControlEvent.MANUAL_REMEDIATION_PULL_REQUEST_EVENT, SourceControlEvent.EVENT_STATUS_NEW);
    tempEntity.newSourceControlEvent(application, policyEvaluation, "b6",
        SourceControlEvent.MANUAL_REMEDIATION_PULL_REQUEST_EVENT, SourceControlEvent.EVENT_STATUS_IN_PROGRESS);
    tempEntity.newSourceControlEvent(application, policyEvaluation, "b7",
        SourceControlEvent.MANUAL_REMEDIATION_PULL_REQUEST_EVENT, SourceControlEvent.EVENT_STATUS_COMPLETE);
    tempEntity.newSourceControlEvent(application, policyEvaluation, "b8",
        SourceControlEvent.MANUAL_REMEDIATION_PULL_REQUEST_EVENT, SourceControlEvent.EVENT_STATUS_ERROR);

    // Note SourceControlEvent.EVENT_STATUS_PARTIALLY_COMPLETE is not possible for a remediation event
    // (i.e. either the PR was created or it wasn't)

    assertThat(eligibilityService.isRemediationWaitingOrDone(application.getId(), "b1")).isTrue();
    assertThat(eligibilityService.isRemediationWaitingOrDone(application.getId(), "b2")).isTrue();
    assertThat(eligibilityService.isRemediationWaitingOrDone(application.getId(), "b3")).isTrue();
    assertThat(eligibilityService.isRemediationWaitingOrDone(application.getId(), "b4")).isFalse();
    assertThat(eligibilityService.isRemediationWaitingOrDone(application.getId(), "b5")).isTrue();
    assertThat(eligibilityService.isRemediationWaitingOrDone(application.getId(), "b6")).isTrue();
    assertThat(eligibilityService.isRemediationWaitingOrDone(application.getId(), "b7")).isTrue();
    assertThat(eligibilityService.isRemediationWaitingOrDone(application.getId(), "b8")).isFalse();
  }

  @Test
  public void testIsRemediationWaitingOrDone_completedEvent_mergedPr_allowsCreatePr() throws PlexusCipherException {
    setupSourceControl(false);
    var policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), stage.getStageTypeId(), "scanId2");
    tempEntity.newSourceControlEvent(application, policyEvaluation, "branch-merged",
        SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT, SourceControlEvent.EVENT_STATUS_COMPLETE);

    String repoUrl = SourceControl.normalizeRepositoryUrl(gitService.baseUrl() + "/org/proj");
    Date now = new Date();
    tempEntity.newSourceControlPullRequest(repoUrl, 2, "head", "base", "branch-merged", "main",
        now, now, now, PullRequestState.MERGED);

    assertThat(eligibilityService.isRemediationWaitingOrDone(application.getId(), "branch-merged")).isFalse();
  }

  @Test
  public void testIsRemediationWaitingOrDone_completedEvent_closedPr_allowsCreatePr() throws PlexusCipherException {
    setupSourceControl(false);
    var policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), stage.getStageTypeId(), "scanId3");
    tempEntity.newSourceControlEvent(application, policyEvaluation, "branch-closed",
        SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT, SourceControlEvent.EVENT_STATUS_COMPLETE);

    String repoUrl = SourceControl.normalizeRepositoryUrl(gitService.baseUrl() + "/org/proj");
    Date now = new Date();
    tempEntity.newSourceControlPullRequest(repoUrl, 2, "head", "base", "branch-closed", "main",
        now, now, now, PullRequestState.CLOSED);

    assertThat(eligibilityService.isRemediationWaitingOrDone(application.getId(), "branch-closed")).isFalse();
  }

  @Test
  public void testIsRemediationWaitingOrDone_completedEvent_openPr_remainsBlocking() throws PlexusCipherException {
    setupSourceControl(false);
    var policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), stage.getStageTypeId(), "scanId4");
    tempEntity.newSourceControlEvent(application, policyEvaluation, "branch-open",
        SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT, SourceControlEvent.EVENT_STATUS_COMPLETE);

    String repoUrl = SourceControl.normalizeRepositoryUrl(gitService.baseUrl() + "/org/proj");
    Date now = new Date();
    tempEntity.newSourceControlPullRequest(repoUrl, 2, "head", "base", "branch-open", "main",
        now, now, now, PullRequestState.OPEN);

    assertThat(eligibilityService.isRemediationWaitingOrDone(application.getId(), "branch-open")).isTrue();
  }

  @Test
  public void testIsRemediationWaitingOrDone_completedEvent_autoClosedPr_allowsCreatePr() throws PlexusCipherException {
    setupSourceControl(false);
    var policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), stage.getStageTypeId(), "scanId5");
    tempEntity.newSourceControlEvent(application, policyEvaluation, "branch-autoclosed",
        SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT, SourceControlEvent.EVENT_STATUS_COMPLETE);

    String repoUrl = SourceControl.normalizeRepositoryUrl(gitService.baseUrl() + "/org/proj");
    Date now = new Date();
    tempEntity.newSourceControlPullRequest(repoUrl, 2, "head", "base", "branch-autoclosed", "main",
        now, now, now, PullRequestState.AUTO_CLOSED);

    assertThat(eligibilityService.isRemediationWaitingOrDone(application.getId(), "branch-autoclosed")).isFalse();
  }

  @Test
  public void testIsRemediationWaitingOrDone_completedEvent_missingPr_allowsCreatePr() throws PlexusCipherException {
    setupSourceControl(false);
    var policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), stage.getStageTypeId(), "scanId6");
    tempEntity.newSourceControlEvent(application, policyEvaluation, "branch-missing",
        SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT, SourceControlEvent.EVENT_STATUS_COMPLETE);

    String repoUrl = SourceControl.normalizeRepositoryUrl(gitService.baseUrl() + "/org/proj");
    Date now = new Date();
    tempEntity.newSourceControlPullRequest(repoUrl, 2, "head", "base", "branch-missing", "main",
        now, now, now, PullRequestState.MISSING);

    assertThat(eligibilityService.isRemediationWaitingOrDone(application.getId(), "branch-missing")).isFalse();
  }

  @Test
  public void testIsRemediationWaitingOrDone_completedEvent_lockedPr_remainsBlocking() throws PlexusCipherException {
    setupSourceControl(false);
    var policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), stage.getStageTypeId(), "scanId7");
    tempEntity.newSourceControlEvent(application, policyEvaluation, "branch-locked",
        SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT, SourceControlEvent.EVENT_STATUS_COMPLETE);

    String repoUrl = SourceControl.normalizeRepositoryUrl(gitService.baseUrl() + "/org/proj");
    Date now = new Date();
    tempEntity.newSourceControlPullRequest(repoUrl, 2, "head", "base", "branch-locked", "main",
        now, now, now, PullRequestState.LOCKED);

    assertThat(eligibilityService.isRemediationWaitingOrDone(application.getId(), "branch-locked")).isTrue();
  }

  @Test
  public void testIsRemediationWaitingOrDone_completedEvent_noPrRow_remainsBlocking() {
    var policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), stage.getStageTypeId(), "scanId8");
    tempEntity.newSourceControlEvent(application, policyEvaluation, "branch-no-pr",
        SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT, SourceControlEvent.EVENT_STATUS_COMPLETE);

    assertThat(eligibilityService.isRemediationWaitingOrDone(application.getId(), "branch-no-pr")).isTrue();
  }

  @Test
  public void testIsRemediationWaitingOrDone_noEvents() {
    String branchName = "branchName";
    assertThat(eligibilityService.isRemediationWaitingOrDone(application.getId(), branchName)).isFalse();
  }

  @Test
  public void testIsRemediationWaitingOrDone_otherTypes() {
    String branchName = "branchName";
    var policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), stage.getStageTypeId(), "scanId");
    for (var eventType : SourceControlEvent.EVENT_TYPES) {
      if (SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT.equals(eventType) ||
          SourceControlEvent.MANUAL_REMEDIATION_PULL_REQUEST_EVENT.equals(eventType))
      {
        continue;
      }
      tempEntity.newSourceControlEvent(application, policyEvaluation, branchName,
          eventType, SourceControlEvent.EVENT_STATUS_COMPLETE);
    }

    boolean result = eligibilityService.isRemediationWaitingOrDone(application.getId(), branchName);
    assertThat(result).isFalse();
  }

  @Test
  public void testIsEligibleForAutoPullRequest_nonDefaultBranch() throws PlexusCipherException {
    setupSourceControl(false);
    boolean result = eligibilityService.isEligibleForAutoPullRequest(
        application, stage, mavenComponent, false, true, "feature/my-branch");
    assertThat(result).isFalse();
    assertThat(remediationPullRequestEligibilityServiceLogOutput).atDebugLevel()
        .contains("scanned branch 'feature/my-branch' differs from default branch");
  }

  @Test
  public void testIsEligibleForAutoPullRequest_nullBranch_allowed() throws PlexusCipherException {
    setupSourceControl(false);
    boolean result = eligibilityService.isEligibleForAutoPullRequest(
        application, stage, mavenComponent, false, true, null);
    assertThat(result).isTrue();
  }

  @Test
  public void testIsEligibleForAutoPullRequest_emptyBranch_allowed() throws PlexusCipherException {
    setupSourceControl(false);
    boolean result = eligibilityService.isEligibleForAutoPullRequest(
        application, stage, mavenComponent, false, true, "");
    assertThat(result).isTrue();
  }

  @Test
  public void testIsEligibleForAutoPullRequest_matchingBranch_allowed() throws PlexusCipherException {
    setupSourceControl(false);
    boolean result = eligibilityService.isEligibleForAutoPullRequest(
        application, stage, mavenComponent, false, true, "master");
    assertThat(result).isTrue();
  }

  @Test
  public void testIsEligibleForManualPullRequest_nonDefaultBranch() throws PlexusCipherException {
    setupSourceControl(false);
    boolean result = eligibilityService.isEligibleForManualPullRequest(
        application, stage, mavenComponent, true, "feature/other");
    assertThat(result).isFalse();
    assertThat(remediationPullRequestEligibilityServiceLogOutput).atDebugLevel()
        .contains("scanned branch 'feature/other' differs from default branch");
  }

  @Test
  public void testIsEligibleForManualPullRequest_nullBranch_allowed() throws PlexusCipherException {
    setupSourceControl(false);
    boolean result = eligibilityService.isEligibleForManualPullRequest(
        application, stage, mavenComponent, true, null);
    assertThat(result).isTrue();
  }

  @Test
  public void testIsScannedBranchNonDefault_nullBranch() {
    assertThat(RemediationPullRequestEligibilityService.isScannedBranchNonDefault(null, "main")).isFalse();
  }

  @Test
  public void testIsScannedBranchNonDefault_emptyBranch() {
    assertThat(RemediationPullRequestEligibilityService.isScannedBranchNonDefault("", "main")).isFalse();
  }

  @Test
  public void testIsScannedBranchNonDefault_matchingBranch() {
    assertThat(RemediationPullRequestEligibilityService.isScannedBranchNonDefault("main", "main")).isFalse();
  }

  @Test
  public void testIsScannedBranchNonDefault_differentBranch() {
    assertThat(RemediationPullRequestEligibilityService.isScannedBranchNonDefault("feature/x", "main")).isTrue();
  }

  @Test
  public void testIsScannedBranchNonDefault_refsHeadsPrefix_scanned() {
    assertThat(RemediationPullRequestEligibilityService.isScannedBranchNonDefault("refs/heads/main", "main"))
        .isFalse();
  }

  @Test
  public void testIsScannedBranchNonDefault_refsHeadsPrefix_default() {
    assertThat(RemediationPullRequestEligibilityService.isScannedBranchNonDefault("main", "refs/heads/main"))
        .isFalse();
  }

  @Test
  public void testIsScannedBranchNonDefault_refsHeadsPrefix_both() {
    assertThat(RemediationPullRequestEligibilityService.isScannedBranchNonDefault(
        "refs/heads/main", "refs/heads/main")).isFalse();
  }

  @Test
  public void testIsScannedBranchNonDefault_refsHeadsPrefix_different() {
    assertThat(RemediationPullRequestEligibilityService.isScannedBranchNonDefault(
        "refs/heads/feature", "refs/heads/main")).isTrue();
  }

  @Test
  public void testIsScannedBranchNonDefault_caseSensitive() {
    assertThat(RemediationPullRequestEligibilityService.isScannedBranchNonDefault("Main", "main")).isTrue();
  }

  @Test
  public void testIsScannedBranchNonDefault_blankBranch() {
    assertThat(RemediationPullRequestEligibilityService.isScannedBranchNonDefault("  ", "main")).isFalse();
  }

  @Test
  public void testIsScannedBranchNonDefault_nullDefault() {
    assertThat(RemediationPullRequestEligibilityService.isScannedBranchNonDefault("feature", null)).isFalse();
  }

  @Test
  public void testIsScannedBranchNonDefault_blankDefault() {
    assertThat(RemediationPullRequestEligibilityService.isScannedBranchNonDefault("feature", "  ")).isFalse();
  }

  private void setupSourceControl(boolean innerSourceAutomatedUpdatesEnabled) throws PlexusCipherException {
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
    final SourceControl sourceControl = new SourceControl();
    sourceControl.setOwnerId(application.getId());
    sourceControl.setRepositoryUrl(gitService.baseUrl() + "/org/proj");
    sourceControl.setManualPullRequestsEnabled(true);
    sourceControl.setToken(new DefaultPlexusCipher().encrypt("token", "CMMDwoV"));
    sourceControl.setInnerSourceAutomatedUpdatesEnabled(innerSourceAutomatedUpdatesEnabled);
    tempEntity.newSourceControl(sourceControl);
  }
}
