/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sourcecontrol;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.sourcecontrol.SourceControlPullRequestServiceTest.DEFAULT_REMEDIATION_VERSION;
import static com.sonatype.insight.brain.sourcecontrol.SourceControlPullRequestServiceTest.DEFAULT_VERSION;
import static com.sonatype.insight.brain.sourcecontrol.SourceControlPullRequestServiceTest.setupComponentVersionInfoDTO;
import static org.mockito.Mockito.lenient;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.PullRequestSubmissionDTO;
import com.sonatype.insight.brain.hds.ComponentInfoService;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.DependencyType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.brain.telemetry.NonBreakingRecommendationTelemetryStats.SourceEndpoint;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.github.dto.GithubUser;
import jakarta.inject.Inject;
import java.io.IOException;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

public class SourceControlPullRequestServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Rule
  public WireMockRule gitService = new WireMockRule(wireMockConfig().dynamicPort());

  @Mock
  private ComponentInfoService mockComponentInfoService;

  @Inject
  private SourceControlPullRequestService service;

  @Before
  public void setup() throws PlexusCipherException {
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("group", "artifact", DEFAULT_VERSION);
    setBaseUrl("http://localhost:1122");

    GithubUser githubUser = new GithubUser();
    githubUser.setGlobalId("userId");
    gitService.stubFor(get("/api/v3/user").withHeader("Authorization", matching("token token"))
        .willReturn(aResponse().withStatus(200).withBody(JsonUtils.format(githubUser))));
    gitService.stubFor(get(urlPathMatching("/api/v3/repos/[^/]+/[^/]+"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody("{ \"private\": true }")));

    lenient().when(mockComponentInfoService.getComponentVersionInfoNoAuth(OwnerType.APPLICATION, app.getPublicId(),
        componentIdentifier, "build", "Sonatype", "scanId", DependencyType.DIRECT,
        SourceEndpoint.MANUAL_PULL_REQUEST,
        true)).thenReturn(setupComponentVersionInfoDTO());

    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
    final SourceControl sourceControl = new SourceControl();
    sourceControl.setOwnerId(app.getId());
    sourceControl.setRepositoryUrl(gitService.baseUrl() + "/org/proj");
    sourceControl.setManualPullRequestsEnabled(true);
    sourceControl.setToken(new DefaultPlexusCipher().encrypt("token", "CMMDwoV"));
    tempEntity.newSourceControl(sourceControl);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetPullRequestStatus_Unauthenticated() {
    service.getPullRequestStatus("pullRequestId");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetPullRequestStatus_Unauthorized() {
    login();
    service.getPullRequestStatus(tempEntity.newSourceControlEvaluationEvent(app).getId());
  }

  @Test(expected = NotFoundException.class)
  public void testGetPullRequestStatus_Authorized() {
    grantReadPermission(app.getId());
    service.getPullRequestStatus(tempEntity.newSourceControlEvaluationEvent(app).getId());
  }

  @Test
  public void testCreateManualRemediationPullRequest_Authorized() throws IOException {
    tempEntity.newPolicyEvaluation(app.getId(), "build", "scanId");
    grantPermission(app.getId(), Permission.CREATE_PULL_REQUESTS);
    PullRequestSubmissionDTO submissionDTO = new PullRequestSubmissionDTO(
        app.getId(),
        "scanId",
        ComponentIdentifier.createMavenCoordinates(
            "group",
            "artifact",
            DEFAULT_VERSION),
        DEFAULT_REMEDIATION_VERSION,
        "Sonatype",
        true);
    service.createPullRequest(submissionDTO);
  }

  @Test(expected = UnauthorizedException.class)
  public void testCreateManualRemediationPullRequest_Unauthorized() throws IOException {
    login();
    PullRequestSubmissionDTO submissionDTO = new PullRequestSubmissionDTO(
        app.getId(),
        "scanId",
        ComponentIdentifier.createMavenCoordinates(
            "group",
            "artifact",
            DEFAULT_VERSION),
        DEFAULT_REMEDIATION_VERSION,
        "Sonatype",
        true);
    service.createPullRequest(submissionDTO);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testCreateManualRemediationPullRequest_Unauthenticated() throws IOException {
    PullRequestSubmissionDTO submissionDTO = new PullRequestSubmissionDTO(
        app.getId(),
        "scanId",
        ComponentIdentifier.createMavenCoordinates(
            "group",
            "artifact",
            DEFAULT_VERSION),
        DEFAULT_REMEDIATION_VERSION,
        "Sonatype",
        true);
    service.createPullRequest(submissionDTO);
  }
}
