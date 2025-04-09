/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.pullrequestcreationservice;

import java.io.IOException;
import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.hds.ComponentInfoService;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.DependencyType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.brain.telemetry.NonBreakingRecommendationTelemetryStats.SourceEndpoint;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.github.dto.GithubUser;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.inject.Binder;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.sonatype.insight.brain.git.pullrequestcreationservice.ManualPullRequestCreationServiceTest.setupComponentVersionInfoDTO;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.mockito.Mockito.lenient;

public class ManualPullRequestCreationServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  private static final String SCAN_ID = "scan-id";

  private static final String TARGET_VERSION = "2.0.0";

  private static final String IDENTIFICATION_SOURCE = "Sonatype";

  @Rule
  public WireMockRule gitService = new WireMockRule(wireMockConfig().dynamicPort());

  @Mock
  private ComponentInfoService mockComponentInfoService;

  @Inject
  private ManualPullRequestCreationService manualPullRequestCreationService;

  private ComponentIdentifier componentIdentifier;

  @Override
  public void configure(Binder binder) {
    binder.bind(ComponentInfoService.class).toInstance(mockComponentInfoService);
    super.configure(binder);
  }

  @Before
  public void setup() throws PlexusCipherException {
    componentIdentifier = ComponentIdentifier.createMavenCoordinates("group", "artifact", "1.0.0");
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
        componentIdentifier, "build", "Sonatype", SCAN_ID, DependencyType.DIRECT,
        SourceEndpoint.MANUAL_PULL_REQUEST,
        true)).thenReturn(setupComponentVersionInfoDTO());

    //set up source control configuration
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
    final SourceControl sourceControl = new SourceControl();
    sourceControl.setOwnerId(app.getId());
    sourceControl.setRepositoryUrl(gitService.baseUrl() + "/org/proj");
    sourceControl.setManualPullRequestsEnabled(true);
    sourceControl.setToken(new DefaultPlexusCipher().encrypt("token", "CMMDwoV"));
    tempEntity.newSourceControl(sourceControl);
  }

  @Test
  public void testCreateManualRemediationPullRequest_Authorized() throws IOException {
    tempEntity.newPolicyEvaluation(app.getId(), "build", SCAN_ID);
    grantPermission(app.getId(), Permission.CREATE_PULL_REQUESTS);
    manualPullRequestCreationService.createManualRemediationPullRequest(
        app.getId(),
        SCAN_ID,
        componentIdentifier,
        TARGET_VERSION,
        IDENTIFICATION_SOURCE
    );
  }

  @Test(expected = UnauthorizedException.class)
  public void testCreateManualRemediationPullRequest_Unauthorized() throws IOException {
    login();
    manualPullRequestCreationService.createManualRemediationPullRequest(
        app.getId(),
        SCAN_ID,
        componentIdentifier,
        TARGET_VERSION,
        IDENTIFICATION_SOURCE
    );
  }

  @Test(expected = UnauthenticatedException.class)
  public void testCreateManualRemediationPullRequest_Unauthenticated() throws IOException {
    manualPullRequestCreationService.createManualRemediationPullRequest(
        app.getId(),
        SCAN_ID,
        componentIdentifier,
        TARGET_VERSION,
        IDENTIFICATION_SOURCE
    );
  }
}
