/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sourcecontrol;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.PullRequestSubmissionDTO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.git.pullrequestcreationservice.PullRequestSubmissionResultDTO;
import com.sonatype.insight.brain.hds.VersionScoringService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.github.dto.GithubUser;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.http.HttpHeaders;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.sonatype.insight.brain.hds.VersionScoringService.HDS_BULK_SCORE_VERSIONING_PATH;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class SourceControlPullRequestResourceTest
    extends AbstractResourceTest
{
  @Rule
  public WireMockRule gitService = new WireMockRule(wireMockConfig().dynamicPort());

  @Before
  public void before() {
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
  public void testGetPullRequestStatus_Pending() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    SourceControlEvent event = createRemediationEvent(application);

    HttpResponse response = restRequest()
        .path(SourceControlPullRequestResource.RESOURCE_PATH, SourceControlPullRequestResource.STATUS_PATH)
        .parameter(event.getId())
        .get();

    assertResponseStatus(200, response);
    JsonNode dto = new ObjectMapper().readTree(response.getBodyText());
    assertThat(dto).isNotNull();
    assertThat(dto.path("status").asText()).isEqualTo("PULL_REQUEST_CREATION_PENDING");
  }

  @Test
  public void testGetPullRequestStatus_Success() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    SourceControlEvent event = createRemediationEvent(application);
    event.setEventStatus(SourceControlEvent.EVENT_STATUS_COMPLETE);
    event.setEventStatusDetails("https://github.com/sonatype/insight-brain/pull/13397");
    event.setPullRequestNumber(13397);
    lookup(SourceControlEventDAO.class).update(event);

    HttpResponse response = restRequest()
        .path(SourceControlPullRequestResource.RESOURCE_PATH, SourceControlPullRequestResource.STATUS_PATH)
        .parameter(event.getId())
        .get();

    assertResponseStatus(200, response);
    JsonNode dto = new ObjectMapper().readTree(response.getBodyText());
    assertThat(dto).isNotNull();
    assertThat(dto.path("status").asText()).isEqualTo("PULL_REQUEST");
    assertThat(dto.path("url").asText()).isEqualTo(event.getEventStatusDetails());
  }

  @Test
  public void testGetPullRequestStatus_Failure() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    SourceControlEvent event = createRemediationEvent(application);
    event.setEventStatus(SourceControlEvent.EVENT_STATUS_ERROR);
    event.setEventStatusDetails("Some error");
    lookup(SourceControlEventDAO.class).update(event);

    HttpResponse response = restRequest()
        .path(SourceControlPullRequestResource.RESOURCE_PATH, SourceControlPullRequestResource.STATUS_PATH)
        .parameter(event.getId())
        .get();

    assertResponseStatus(200, response);
    JsonNode dto = new ObjectMapper().readTree(response.getBodyText());
    assertThat(dto).isNotNull();
    assertThat(dto.path("status").asText()).isEqualTo("PULL_REQUEST_CREATION_FAILED");
    assertThat(dto.path("reason").asText()).isEqualTo(event.getEventStatusDetails());
  }

  @Test
  public void testCreatePullRequest_Success() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newPolicy(application.getId(), "Policy Name", 10);
    tempEntity.newPolicyEvaluation(application.getId(), "build", "scanId");

    mockComponentDetails(createComponentDetailsList());
    setupSourceControl(application);
    ComponentIdentifier mavenComponentIdentifier =
        ComponentIdentifier.createMavenCoordinates("ch.qos.logback", "logback-classic", "1.3.14", "", "jar");
    PullRequestSubmissionDTO submission =
        new PullRequestSubmissionDTO(application.getId(), "scanId", mavenComponentIdentifier, "1.3.16", "Sonatype",
            true);

    HttpResponse response = restRequest()
        .path(SourceControlPullRequestResource.RESOURCE_PATH)
        .body(submission)
        .post();

    assertResponseStatus(200, response);
    PullRequestSubmissionResultDTO dto = response.getBody(PullRequestSubmissionResultDTO.class);
    assertThat(dto).isNotNull();
    assertThat(dto.id()).isNotNull();
  }

  @Test
  public void testCreatePullRequest_Failure_NoApplicableVersionChange() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newPolicy(application.getId(), "Policy Name", 10);
    tempEntity.newPolicyEvaluation(application.getId(), "build", "scanId");
    ComponentIdentifier mavenComponentIdentifier =
        ComponentIdentifier.createMavenCoordinates("ch.qos.logback", "logback-classic", "1.3.14", "", "jar");

    // only same version available
    ComponentDetails details = createComponentDetailsForSecurityViolation(mavenComponentIdentifier);
    ComponentDetailsList componentDetailsList = new ComponentDetailsList();
    componentDetailsList.setList(Collections.singletonList(details));

    mockComponentDetails(componentDetailsList);

    setupSourceControl(application);
    PullRequestSubmissionDTO submission =
        new PullRequestSubmissionDTO(application.getId(), "scanId", mavenComponentIdentifier, "1.3.16", "Sonatype",
            true);

    HttpResponse response = restRequest()
        .path(SourceControlPullRequestResource.RESOURCE_PATH)
        .body(submission)
        .post();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains(
        "Target version 1.3.16 does not match the applicable version change 1.3.14 for component " +
            ComponentDisplayNameUtil.fromIdentifier(mavenComponentIdentifier));
  }

  @Test
  public void testCreatePullRequest_Failure_TargetMismatched() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newPolicy(application.getId(), "Policy Name", 10);
    tempEntity.newPolicyEvaluation(application.getId(), "build", "scanId");

    mockComponentDetails(createComponentDetailsList());
    setupSourceControl(application);
    ComponentIdentifier mavenComponentIdentifier =
        ComponentIdentifier.createMavenCoordinates("ch.qos.logback", "logback-classic", "1.3.14", "", "jar");

    // request a targetVersion not in the applicable version change list
    PullRequestSubmissionDTO submission =
        new PullRequestSubmissionDTO(application.getId(), "scanId", mavenComponentIdentifier, "1.3.15", "Sonatype",
            true);

    HttpResponse response = restRequest()
        .path(SourceControlPullRequestResource.RESOURCE_PATH)
        .body(submission)
        .post();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains(
        "Target version " + submission.targetVersion() + " does not match the applicable version change");
  }

  @Test
  public void testCreatePullRequest_Failure_InvalidScanId() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newPolicy(application.getId(), "Policy Name", 10);

    // Using a scan ID that doesn't exist
    ComponentIdentifier mavenComponentIdentifier =
        ComponentIdentifier.createMavenCoordinates("ch.qos.logback", "logback-classic", "1.3.14", "", "jar");
    PullRequestSubmissionDTO submission =
        new PullRequestSubmissionDTO(application.getId(), "nonExistentScanId", mavenComponentIdentifier, "1.3.16",
            "Sonatype", true);

    HttpResponse response = restRequest()
        .path(SourceControlPullRequestResource.RESOURCE_PATH)
        .body(submission)
        .post();

    assertResponseStatus(404, response);
  }

  @Test
  public void testCreatePullRequest_Failure_NoSourceControlConfigured() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newPolicy(application.getId(), "Policy Name", 10);
    tempEntity.newPolicyEvaluation(application.getId(), "build", "scanId");

    // No source control configuration
    ComponentIdentifier mavenComponentIdentifier =
        ComponentIdentifier.createMavenCoordinates("ch.qos.logback", "logback-classic", "1.3.14", "", "jar");
    PullRequestSubmissionDTO submission =
        new PullRequestSubmissionDTO(application.getId(), "scanId", mavenComponentIdentifier, "1.3.16", "Sonatype",
            true);

    HttpResponse response = restRequest()
        .path(SourceControlPullRequestResource.RESOURCE_PATH)
        .body(submission)
        .post();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains(
        "Manual pull request creation is not eligible for application " + application.getPublicId());
  }

  @Test
  public void testCreatePullRequest_Failure_NonDirectDependency() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newPolicy(application.getId(), "Policy Name", 10);
    tempEntity.newPolicyEvaluation(application.getId(), "build", "scanId");

    mockComponentDetails(createComponentDetailsList());
    setupSourceControl(application);
    ComponentIdentifier mavenComponentIdentifier =
        ComponentIdentifier.createMavenCoordinates("ch.qos.logback", "logback-classic", "1.3.14", "", "jar");
    PullRequestSubmissionDTO submission =
        new PullRequestSubmissionDTO(application.getId(), "scanId", mavenComponentIdentifier, "1.3.16", "Sonatype",
            false);

    HttpResponse response = restRequest()
        .path(SourceControlPullRequestResource.RESOURCE_PATH)
        .body(submission)
        .post();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains(
        "Manual pull request creation is not eligible for application " + application.getPublicId());
  }

  private SourceControlEvent createRemediationEvent(final Application app) {
    SourceControlEvent event = new SourceControlEvent().forRemediationPullRequest().setApplicationId(app.getId());
    lookup(SourceControlEventDAO.class).insert(event);
    return event;
  }

  private void mockComponentDetails(final ComponentDetailsList componentDetailsList) {
    hdsRespondWith(componentDetailsList).atUri("rest/ci/componentDetails/list");
    mockGetDependencies(new ComponentDependenciesDTO(new HashMap<>(), new HashMap<>()));
    mockVersionScoring();
  }

  private void mockVersionScoring() {
    hdsRespondWith(new VersionScoringService[]{}).atUri(HDS_BULK_SCORE_VERSIONING_PATH);
  }

  private ComponentDetailsList createComponentDetailsList() {
    final ComponentIdentifier currentComponent =
        ComponentIdentifier.createMavenCoordinates("ch.qos.logback", "logback-classic", "1.3.14", "", "jar");
    final ComponentIdentifier newerVersionComponent1 =
        ComponentIdentifier.createMavenCoordinates("ch.qos.logback", "logback-classic", "1.3.15", "", "jar");
    final ComponentIdentifier newerVersionComponent2 =
        ComponentIdentifier.createMavenCoordinates("ch.qos.logback", "logback-classic", "1.3.16", "", "jar");

    ComponentDetails details1 = createComponentDetailsForSecurityViolation(currentComponent);
    ComponentDetails details2 = createComponentDetailsForSecurityViolation(newerVersionComponent1);
    ComponentDetails details3 = createComponentDetailsForNoViolation(newerVersionComponent2);
    List<ComponentDetails> list = List.of(details1, details2, details3);
    ComponentDetailsList detailsList = new ComponentDetailsList();
    detailsList.setList(list);
    return detailsList;
  }

  private ComponentDetails createComponentDetailsForSecurityViolation(ComponentIdentifier componentIdentifier) {
    ComponentDetails componentDetails = createComponentDetailsForNoViolation(componentIdentifier);
    componentDetails.setLicenseThreatLevel(5);
    componentDetails
        .setSecurityVulnerabilities(List.of(new SecurityVulnerability("ref", "source", 5f)));
    return componentDetails;
  }

  private ComponentDetails createComponentDetailsForNoViolation(ComponentIdentifier componentIdentifier) {
    ComponentDetails componentDetails = new ComponentDetails();
    componentDetails.setComponentIdentifier(componentIdentifier);
    return componentDetails;
  }

  private void setupSourceControl(final Application application) throws PlexusCipherException {
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
    final SourceControl sourceControl = new SourceControl();
    sourceControl.setOwnerId(application.getId());
    sourceControl.setRepositoryUrl(gitService.baseUrl() + "/org/proj");
    sourceControl.setManualPullRequestsEnabled(true);
    sourceControl.setToken(new DefaultPlexusCipher().encrypt("token", "CMMDwoV"));
    tempEntity.newSourceControl(sourceControl);
  }
}
