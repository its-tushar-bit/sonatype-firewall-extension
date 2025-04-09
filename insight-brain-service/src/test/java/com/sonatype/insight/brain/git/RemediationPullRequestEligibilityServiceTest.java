/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
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
  public LogOutput logOutput = new LogOutput(RemediationPullRequestEligibilityService.class);

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
    setupSourceControl();
    boolean result = eligibilityService.isEligibleForAutoPullRequest(
        application, stage, mavenComponent);
    assertThat(result).isTrue();
  }

  @Test
  public void testIsEligibleForAutoPullRequest_unsupportedStage() {
    Stage developStage = new Stage(Stage.ID_DEVELOP);
    boolean result = eligibilityService.isEligibleForAutoPullRequest(
        application, developStage, mavenComponent);
    assertThat(result).isFalse();
    assertThat(logOutput).atDebugLevel().contains("Pull Request not supported for the stage 'develop'");
  }

  @Test
  public void testIsEligibleForAutoPullRequest_unsupportedFormat() {
    ComponentIdentifier pypiComponent =
        ComponentIdentifier.createPypiCoordinates("PyYAML", "3.11", "win-amd64-py2.7", "exe");
    boolean result = eligibilityService.isEligibleForAutoPullRequest(
        application, stage, pypiComponent);
    assertThat(result).isFalse();
    assertThat(logOutput).atDebugLevel().contains("Format 'pypi' is not supported for remediation");
  }

  //manual pr section
  @Test
  public void testIsEligibleForManualPullRequest_success() throws PlexusCipherException {
    setupSourceControl();
    boolean result = eligibilityService.isEligibleForManualPullRequest(
        application, stage, mavenComponent);
    assertThat(result).isTrue();
  }

  @Test
  public void testIsEligibleForManualPullRequest_unsupportedFormat() {
    ComponentIdentifier pypiComponent =
        ComponentIdentifier.createPypiCoordinates("PyYAML", "3.11", "win-amd64-py2.7", "exe");
    boolean result = eligibilityService.isEligibleForManualPullRequest(
        application, stage, pypiComponent);
    assertThat(result).isFalse();
    assertThat(logOutput).atDebugLevel().contains("Format 'pypi' is not supported for remediation");
  }

  @Test
  public void testIsEligibleForManualPullRequest_unsupportedStage() {
    boolean result = eligibilityService.isEligibleForManualPullRequest(
        application, new Stage(Stage.ID_DEVELOP), mavenComponent);
    assertThat(result).isFalse();
    assertThat(logOutput).atDebugLevel().contains("Pull Request not supported for the stage 'develop'");
  }

  private void setupSourceControl() throws PlexusCipherException {
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
    final SourceControl sourceControl = new SourceControl();
    sourceControl.setOwnerId(application.getId());
    sourceControl.setRepositoryUrl(gitService.baseUrl() + "/org/proj");
    sourceControl.setManualPullRequestsEnabled(true);
    sourceControl.setToken(new DefaultPlexusCipher().encrypt("token", "CMMDwoV"));
    tempEntity.newSourceControl(sourceControl);
  }
}
