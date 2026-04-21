/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.pullrequestcreationservice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.git.RemediationVersionDTO;
import com.sonatype.insight.brain.metrics.ScmOperationMetrics;
import com.sonatype.insight.brain.git.utils.PullRequestBranchNameGenerator;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.policy.evaluator.PolicyNotificationUtil;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.test.LogOutput;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.github.dto.GithubUser;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.hc.core5.http.HttpHeaders;
import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.sonatype.insight.brain.metrics.ScmPrIneligibleReason.NOT_ELIGIBLE;
import static com.sonatype.insight.brain.metrics.ScmPrIneligibleReason.NOT_GOLDEN_VERSION;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class AutomatedPullRequestCreationServiceTest
    extends AbstractComponentTest
{
  private static final String DEFAULT_SCAN_ID = "scan-id";

  private static final String DEFAULT_VERSION = "1.0.0";

  private static final String DEFAULT_REMEDIATION_VERSION = "2.0.0";

  @Rule
  public WireMockRule gitService = new WireMockRule(wireMockConfig().dynamicPort());

  @Mock
  private ScmOperationMetrics mockScmOperationMetrics;

  @Inject
  private PullRequestBranchNameGenerator branchNameGenerator;

  @Inject
  private PolicyNotificationUtil policyNotificationUtil;

  @Inject
  private AutomatedPullRequestCreationService automatedPrService;

  @Inject
  private SourceControlDAO sourceControlDAO;

  @Inject
  private SourceControlEventDAO sourceControlEventDAO;

  @Rule
  public LogOutput logOutput = new LogOutput(AutomatedPullRequestCreationService.class);

  private Application application;

  private ComponentIdentifier mavenComponent;

  private Stage stage;

  @Override
  public void configure(final Binder binder) {
    binder.bind(ScmOperationMetrics.class).toInstance(mockScmOperationMetrics);
    super.configure(binder);
  }

  @Before
  public void setup() throws PlexusCipherException {
    application = tempEntity.newApplicationWithParent("appId");
    mavenComponent = ComponentIdentifier.createMavenCoordinates("group", "artifact", DEFAULT_VERSION);
    stage = new Stage(Stage.ID_BUILD);
    setBaseUrl("http://localhost:1122");

    GithubUser githubUser = new GithubUser();
    githubUser.setGlobalId("userId");
    gitService.stubFor(get("/api/v3/user").withHeader("Authorization", matching("token token"))
        .willReturn(aResponse().withStatus(200).withBody(JsonUtils.format(githubUser))));
    gitService.stubFor(get(urlPathMatching("/api/v3/repos/[^/]+/[^/]+"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody("{ \"private\": true }")));

    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
    final SourceControl sourceControl = new SourceControl();
    sourceControl.setOwnerId(application.getId());
    sourceControl.setRepositoryUrl(gitService.baseUrl() + "/org/proj");
    sourceControl.setManualPullRequestsEnabled(true);
    sourceControl.setToken(new DefaultPlexusCipher().encrypt("token", "CMMDwoV"));
    tempEntity.newSourceControl(sourceControl);
  }

  @Test
  public void testCreateAutomatedRemediationPullRequest_goldenPR() throws IOException {
    String branchName = branchNameGenerator.getBranchName(application, mavenComponent, DEFAULT_REMEDIATION_VERSION);
    RemediationVersionDTO remediationVersionDTO = new RemediationVersionDTO(DEFAULT_REMEDIATION_VERSION,
        ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES, 0);
    List<PolicyNotification> notifications = createPolicyNotifications();

    // Golden PR with non-breaking with dependencies remediation type
    automatedPrService.createAutomatedRemediationPullRequest(
        application,
        DEFAULT_SCAN_ID,
        stage,
        mavenComponent,
        () -> Optional.of(remediationVersionDTO),
        notifications,
        true);

    List<SourceControlEvent> eventList = sourceControlEventDAO.getAllByApplicationId(application.getId());
    assertThat(eventList).hasSize(1);

    SourceControlEvent event = eventList.get(0);
    assertThat(event.getRemediationVersion()).isEqualTo(DEFAULT_REMEDIATION_VERSION);
    assertThat(event.getComponentIdentifier()).isEqualTo(mavenComponent);
    assertThat(event.getApplicationId()).isEqualTo(application.getId());
    assertThat(event.getBranchName()).isEqualTo(branchName);
    assertThat(event.getInitiator()).isEqualTo("policy alert");
    assertThat(event.getEventType()).isEqualTo(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
    verifyNoInteractions(mockScmOperationMetrics);
  }

  @Test
  public void testCreateAutomatedRemediationPullRequest_withNonDirectDependency_shouldNotCreatePR() throws IOException {
    String version = "1.5.0";
    RemediationVersionDTO remediationVersionDTO =
        new RemediationVersionDTO(version, ApiVersionChangeOptionType.NEXT_NON_FAILING, 0);
    List<PolicyNotification> notifications = createPolicyNotifications();

    // non-direct dependency — should be filtered out
    automatedPrService.createAutomatedRemediationPullRequest(
        application,
        DEFAULT_SCAN_ID,
        stage,
        mavenComponent,
        () -> Optional.of(remediationVersionDTO),
        notifications,
        false);

    List<SourceControlEvent> eventList = sourceControlEventDAO.getAll();
    assertThat(eventList).isEmpty();
    assertThat(logOutput)
        .atDebugLevel()
        .contains(String.format("Component '%s' in application '%s' is not eligible for automated PR", mavenComponent,
            application.getPublicId()));
    verify(mockScmOperationMetrics).recordPrCreationIneligible(NOT_ELIGIBLE);
  }

  @Test
  public void testCreateAutomatedRemediationPullRequest_withNonGoldenType_shouldNotCreatePR() throws IOException {
    String version = "1.5.0";
    RemediationVersionDTO remediationVersionDTO =
        new RemediationVersionDTO(version, ApiVersionChangeOptionType.NEXT_NON_FAILING, 0);
    List<PolicyNotification> notifications = createPolicyNotifications();

    automatedPrService.createAutomatedRemediationPullRequest(
        application,
        DEFAULT_SCAN_ID,
        stage,
        mavenComponent,
        () -> Optional.of(remediationVersionDTO),
        notifications,
        true);

    List<SourceControlEvent> eventList = sourceControlEventDAO.getAll();
    assertThat(eventList).isEmpty();
    assertThat(logOutput).atDebugLevel()
        .contains("Suppressing non-golden automated PR");
    verify(mockScmOperationMetrics).recordPrCreationIneligible(NOT_GOLDEN_VERSION);
  }

  @Test
  public void testCreateAutomatedRemediationPullRequest_nonGoldenWithOptIn_shouldCreatePR() throws IOException {
    String branchName = branchNameGenerator.getBranchName(application, mavenComponent, DEFAULT_REMEDIATION_VERSION);
    RemediationVersionDTO remediationVersionDTO =
        new RemediationVersionDTO(DEFAULT_REMEDIATION_VERSION, ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS, 0);
    List<PolicyNotification> notifications = createPolicyNotifications();

    enableNonGoldenPullRequests();

    automatedPrService.createAutomatedRemediationPullRequest(
        application,
        DEFAULT_SCAN_ID,
        stage,
        mavenComponent,
        () -> Optional.of(remediationVersionDTO),
        notifications,
        true);

    List<SourceControlEvent> eventList = sourceControlEventDAO.getAll();
    assertThat(eventList).hasSize(1);
    SourceControlEvent event = eventList.get(0);
    assertThat(event.getScanId()).isEqualTo(DEFAULT_SCAN_ID);
    assertThat(event.getRemediationVersion()).isEqualTo(DEFAULT_REMEDIATION_VERSION);
    assertThat(event.getComponentIdentifier()).isEqualTo(mavenComponent);
    assertThat(event.getApplicationId()).isEqualTo(application.getId());
    assertThat(event.getBranchName()).isEqualTo(branchName);
    assertThat(event.getInitiator()).isEqualTo("policy alert");
    assertThat(event.getEventType()).isEqualTo(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
    assertThat(logOutput).atDebugLevel().contains("non-golden PRs explicitly enabled");
    verifyNoInteractions(mockScmOperationMetrics);
  }

  @Test
  public void testCreateAutomatedRemediationPullRequest_npmNonGolden_shouldSuppressByDefault() throws IOException {
    ComponentIdentifier npmComponent = ComponentIdentifier.createNpmCoordinates("lodash", DEFAULT_VERSION);
    RemediationVersionDTO remediationVersionDTO =
        new RemediationVersionDTO("4.18.0", ApiVersionChangeOptionType.NEXT_NON_FAILING, 0);
    List<PolicyNotification> notifications = createPolicyNotifications();

    automatedPrService.createAutomatedRemediationPullRequest(
        application,
        DEFAULT_SCAN_ID,
        stage,
        npmComponent,
        () -> Optional.of(remediationVersionDTO),
        notifications,
        true);

    List<SourceControlEvent> eventList = sourceControlEventDAO.getAll();
    assertThat(eventList).isEmpty();
    assertThat(logOutput).atDebugLevel().contains("Suppressing non-golden automated PR");
    verify(mockScmOperationMetrics).recordPrCreationIneligible(NOT_GOLDEN_VERSION);
  }

  @Test
  public void testCreateAutomatedRemediationPullRequest_npmNonGoldenWithOptIn_shouldCreatePR() throws IOException {
    ComponentIdentifier npmComponent = ComponentIdentifier.createNpmCoordinates("lodash", DEFAULT_VERSION);
    RemediationVersionDTO remediationVersionDTO =
        new RemediationVersionDTO("4.18.0", ApiVersionChangeOptionType.NEXT_NON_FAILING, 0);
    List<PolicyNotification> notifications = createPolicyNotifications();

    enableNonGoldenPullRequests();

    automatedPrService.createAutomatedRemediationPullRequest(
        application,
        DEFAULT_SCAN_ID,
        stage,
        npmComponent,
        () -> Optional.of(remediationVersionDTO),
        notifications,
        true);

    List<SourceControlEvent> eventList = sourceControlEventDAO.getAll();
    assertThat(eventList).hasSize(1);
    assertThat(logOutput).atDebugLevel().contains("non-golden PRs explicitly enabled");
    verifyNoInteractions(mockScmOperationMetrics);
  }

  private void enableNonGoldenPullRequests() {
    SourceControl appSourceControl = sourceControlDAO.getByOwnerId(application.getId());
    appSourceControl.setNonGoldenPullRequestsEnabled(true);
    sourceControlDAO.updateWithoutValidation(appSourceControl);
  }

  private List<PolicyNotification> createPolicyNotifications() {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(
        application.getId(), stage.getStageTypeId(), DEFAULT_SCAN_ID);
    Policy policy = tempEntity.newPolicy(application);

    List<PolicyViolation> policyViolations = new ArrayList<>();
    policyViolations.add(tempEntity.newPolicyViolation(evaluation, policy, mavenComponent, "abcd"));

    return policyNotificationUtil.createPolicyNotifications(
        application,
        policyViolations,
        evaluation.getStageTypeId(),
        evaluation.isForMonitoring());
  }
}
