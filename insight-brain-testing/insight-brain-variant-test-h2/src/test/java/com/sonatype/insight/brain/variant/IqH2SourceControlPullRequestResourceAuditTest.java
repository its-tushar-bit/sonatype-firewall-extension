/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

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
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.git.pullrequestcreationservice.PullRequestSubmissionResultDTO;
import com.sonatype.insight.brain.hds.VersionScoringService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.sourcecontrol.PullRequestSource;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.brain.sourcecontrol.SourceControlPullRequestResource;
import com.sonatype.insight.brain.testsupport.wiremock.ReusableWireMockExtension;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;
import com.sonatype.insight.test.LogOutput;
import com.sonatype.nexus.scm.SourceControlProvider;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.nexus.scm.github.dto.GithubUser;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.sonatype.insight.brain.hds.VersionScoringService.HDS_BULK_SCORE_VERSIONING_PATH;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kept in the original resource package because it exercises {@link SourceControlPullRequestResource}. The mock git
 * service is a reuse-safe, dynamic-port WireMock extension registered as a {@code static} field: one server per class,
 * with stubs and request journal reset between tests, so it runs cleanly inside the reused IQ server cohort.
 */
@IqH2Test
class IqH2SourceControlPullRequestResourceAuditTest
    implements AuditTestSupport
{
  // Injected by IqH2ServerExtension: the extension owns the shared, reused server.
  private IqTestContext ctx;

  private User unauthorizedUser;

  private final TestLogOutput logOutput =
      new TestLogOutput(com.sonatype.insight.brain.audit.AuditRecorder.BASE_LOGGER_NAME);

  @RegisterExtension
  static ReusableWireMockExtension gitService = new ReusableWireMockExtension();

  @BeforeEach
  void before() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();

    stubGithubApi();
  }

  @AfterEach
  void after() {
    logOutput.tearDown();
  }

  @Override
  public LogOutput getLogOutput() {
    return logOutput;
  }

  @Override
  public String getUnauthorizedUsername() {
    return unauthorizedUser.getUsername();
  }

  @Override
  public PolicyDAO getPolicyDAO() {
    return ctx.lookup(PolicyDAO.class);
  }

  private void stubGithubApi() {
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
  void testInitiatePullRequestAuditEmittedOnSuccessfulManualSubmission() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();
    ctx.tempEntity().newPolicy(application.getId(), "Policy Name", 10);
    ctx.tempEntity().newPolicyEvaluation(application.getId(), "build", "scanId");

    mockComponentDetails(createComponentDetailsList());
    setupSourceControl(application);

    ComponentIdentifier mavenComponentIdentifier =
        ComponentIdentifier.createMavenCoordinates("ch.qos.logback", "logback-classic", "1.3.14", "", "jar");
    PullRequestSubmissionDTO submission = new PullRequestSubmissionDTO(
        application.getId(), "scanId", mavenComponentIdentifier, "1.3.16", "Sonatype", true);

    HttpResponse response = ctx.restRequest()
        .path(SourceControlPullRequestResource.RESOURCE_PATH)
        .body(submission)
        .post();

    ctx.assertResponseStatus(200, response);
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
  void testTraceJoinKey_AuditRowAndPersistedEventShareTheSameId() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();
    ctx.tempEntity().newPolicy(application.getId(), "Policy Name", 10);
    ctx.tempEntity().newPolicyEvaluation(application.getId(), "build", "scanId");

    mockComponentDetails(createComponentDetailsList());
    setupSourceControl(application);

    ComponentIdentifier mavenComponentIdentifier =
        ComponentIdentifier.createMavenCoordinates("ch.qos.logback", "logback-classic", "1.3.14", "", "jar");
    PullRequestSubmissionDTO submission = new PullRequestSubmissionDTO(
        application.getId(), "scanId", mavenComponentIdentifier, "1.3.16", "Sonatype", true);

    HttpResponse response = ctx.restRequest()
        .path(SourceControlPullRequestResource.RESOURCE_PATH)
        .body(submission)
        .post();

    ctx.assertResponseStatus(200, response);
    PullRequestSubmissionResultDTO result = response.getBody(PullRequestSubmissionResultDTO.class);
    String sourceControlEventId = result.id();

    // 1. The audit row carries the same sourceControlEventId as the response (request edge).
    AuditDTO auditDTO = assertAuditLog(AuditEvent.INITIATE_PULL_REQUEST, null);
    assertCustomData(auditDTO, "sourceControlEventId", sourceControlEventId);

    // 2. The persisted source_control_event row exists at that id (workflow record durable).
    SourceControlEvent persistedEvent = ctx.lookup(SourceControlEventDAO.class).getById(sourceControlEventId);
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
    ctx.hdsRespondWith(componentDetailsList).atUri("rest/ci/componentDetails/list");
    mockGetDependencies(new ComponentDependenciesDTO(new HashMap<>(), new HashMap<>()));
    ctx.hdsRespondWith(new VersionScoringService[]{}).atUri(HDS_BULK_SCORE_VERSIONING_PATH);
  }

  private void mockGetDependencies(final ComponentDependenciesDTO dto) {
    ctx.hdsRespondWith(dto).atUri("rest/component/dependencies");
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
    ctx.tempEntity().newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
    SourceControl sourceControl = new SourceControl();
    sourceControl.setOwnerId(application.getId());
    sourceControl.setRepositoryUrl(gitService.baseUrl() + "/org/proj");
    sourceControl.setManualPullRequestsEnabled(true);
    sourceControl.setToken(new DefaultPlexusCipher().encrypt("token", "CMMDwoV"));
    ctx.tempEntity().newSourceControl(sourceControl);
  }

  private static final class TestLogOutput
      extends LogOutput
  {
    TestLogOutput(String... loggerNames) {
      super(loggerNames);
    }

    void tearDown() {
      after();
    }
  }
}
