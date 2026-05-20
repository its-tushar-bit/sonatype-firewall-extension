/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sourcecontrol;

import java.util.HashMap;
import java.util.List;

import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.PullRequestSubmissionDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.git.pullrequestcreationservice.PullRequestSubmissionResultDTO;
import com.sonatype.insight.brain.hds.VersionScoringService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.sourcecontrol.PullRequestSource;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.service.AbstractAuditTest;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;
import com.sonatype.nexus.scm.SourceControlProvider;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.nexus.scm.github.dto.GithubUser;
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

public class SourceControlPullRequestResourceAuditTest
    extends AbstractAuditTest
{
  @Rule
  public WireMockRule gitService = new WireMockRule(wireMockConfig().dynamicPort());

  @Before
  public void stubGithubApi() {
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
  public void testInitiatePullRequestAuditEmittedOnSuccessfulManualSubmission() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newPolicy(application.getId(), "Policy Name", 10);
    tempEntity.newPolicyEvaluation(application.getId(), "build", "scanId");

    mockComponentDetails(createComponentDetailsList());
    setupSourceControl(application);

    ComponentIdentifier mavenComponentIdentifier =
        ComponentIdentifier.createMavenCoordinates("ch.qos.logback", "logback-classic", "1.3.14", "", "jar");
    PullRequestSubmissionDTO submission = new PullRequestSubmissionDTO(
        application.getId(), "scanId", mavenComponentIdentifier, "1.3.16", "Sonatype", true);

    HttpResponse response = restRequest()
        .path(SourceControlPullRequestResource.RESOURCE_PATH)
        .body(submission)
        .post();

    assertResponseStatus(200, response);
    PullRequestSubmissionResultDTO result = response.getBody(PullRequestSubmissionResultDTO.class);
    assertThat(result.id()).isNotNull();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.INITIATE_PULL_REQUEST, null);
    assertCustomData(auditDTO, "sourceControlEventId", result.id());
    assertCustomData(auditDTO, "requestMode", PullRequestSource.MANUAL.name());
    assertCustomData(auditDTO, "provider", SourceControlProvider.GITHUB.name());
    assertCustomData(auditDTO, "scanId", "scanId");
    assertCustomData(auditDTO, "stageId", "build");
    assertApplicationData(auditDTO, application);
  }

  @Test
  public void testTraceJoinKey_AuditRowAndPersistedEventShareTheSameId() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newPolicy(application.getId(), "Policy Name", 10);
    tempEntity.newPolicyEvaluation(application.getId(), "build", "scanId");

    mockComponentDetails(createComponentDetailsList());
    setupSourceControl(application);

    ComponentIdentifier mavenComponentIdentifier =
        ComponentIdentifier.createMavenCoordinates("ch.qos.logback", "logback-classic", "1.3.14", "", "jar");
    PullRequestSubmissionDTO submission = new PullRequestSubmissionDTO(
        application.getId(), "scanId", mavenComponentIdentifier, "1.3.16", "Sonatype", true);

    HttpResponse response = restRequest()
        .path(SourceControlPullRequestResource.RESOURCE_PATH)
        .body(submission)
        .post();

    assertResponseStatus(200, response);
    PullRequestSubmissionResultDTO result = response.getBody(PullRequestSubmissionResultDTO.class);
    String sourceControlEventId = result.id();

    // 1. The audit row carries the same sourceControlEventId as the response (request edge).
    AuditDTO auditDTO = assertAuditLog(AuditEvent.INITIATE_PULL_REQUEST, null);
    assertCustomData(auditDTO, "sourceControlEventId", sourceControlEventId);

    // 2. The persisted source_control_event row exists at that id (workflow record durable).
    SourceControlEvent persistedEvent = lookup(SourceControlEventDAO.class).getById(sourceControlEventId);
    assertThat(persistedEvent).isNotNull();
    assertThat(persistedEvent.getApplicationId()).isEqualTo(application.getId());
    assertThat(persistedEvent.getScanId()).isEqualTo("scanId");
    assertThat(persistedEvent.getStageTypeId()).isEqualTo("build");

    // 3. The execution-time trace fields are absent on the just-persisted row — they get filled in by
    // PullRequestTask.run() at execution time, NOT at request time. This proves Decision D3:
    // auth context is recorded from the strategy actually used, not at queue time.
    assertThat(persistedEvent.getOutcome()).isNull();
    assertThat(persistedEvent.getAuthenticationType()).isNull();
  }

  private void mockComponentDetails(final ComponentDetailsList componentDetailsList) {
    hdsRespondWith(componentDetailsList).atUri("rest/ci/componentDetails/list");
    mockGetDependencies(new ComponentDependenciesDTO(new HashMap<>(), new HashMap<>()));
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
    ComponentDetailsList detailsList = new ComponentDetailsList();
    detailsList.setList(List.of(details1, details2, details3));
    return detailsList;
  }

  private ComponentDetails createComponentDetailsForSecurityViolation(ComponentIdentifier componentIdentifier) {
    ComponentDetails componentDetails = createComponentDetailsForNoViolation(componentIdentifier);
    componentDetails.setLicenseThreatLevel(5);
    componentDetails.setSecurityVulnerabilities(List.of(new SecurityVulnerability("ref", "source", 5f)));
    return componentDetails;
  }

  private ComponentDetails createComponentDetailsForNoViolation(ComponentIdentifier componentIdentifier) {
    ComponentDetails componentDetails = new ComponentDetails();
    componentDetails.setComponentIdentifier(componentIdentifier);
    return componentDetails;
  }

  private void setupSourceControl(final Application application) throws PlexusCipherException {
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
    SourceControl sourceControl = new SourceControl();
    sourceControl.setOwnerId(application.getId());
    sourceControl.setRepositoryUrl(gitService.baseUrl() + "/org/proj");
    sourceControl.setManualPullRequestsEnabled(true);
    sourceControl.setToken(new DefaultPlexusCipher().encrypt("token", "CMMDwoV"));
    tempEntity.newSourceControl(sourceControl);
  }
}
