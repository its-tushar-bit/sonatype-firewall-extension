/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.git.PullRequestCommentingRemediationService;
import com.sonatype.insight.brain.git.RemediationPullRequestEligibilityService;
import com.sonatype.insight.brain.git.RemediationVersionDTO;
import com.sonatype.insight.brain.git.pullrequestcreationservice.AutomatedPullRequestCreationService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.shutdown.ShutdownPriority;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.test.LogOutput;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.github.dto.GithubUser;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.inject.Binder;
import org.apache.hc.core5.http.HttpHeaders;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PolicyAlertScmNotifierTest
    extends AbstractComponentTest
{
  @Inject
  private PolicyAlertScmNotifier scmNotifier;

  @Inject
  private SourceControlEventDAO sourceControlEventDAO;

  @Mock
  private PullRequestCommentingRemediationService mockPullRequestCommentingRemediationService;

  @Mock
  private ReportComponentService mockReportComponentService;

  @Mock
  private ShutdownHandler mockShutdownHandler;

  @Rule
  public LogOutput logOutput = new LogOutput(AutomatedPullRequestCreationService.class,
      RemediationPullRequestEligibilityService.class);

  @Rule
  public WireMockRule gitService = new WireMockRule(wireMockConfig().dynamicPort());

  @Captor
  private ArgumentCaptor<Thread> threadArgumentCaptor;

  private Application application;

  @Override
  public void configure(Binder binder) {
    binder.bind(PullRequestCommentingRemediationService.class).toInstance(mockPullRequestCommentingRemediationService);
    binder.bind(ShutdownHandler.class).toInstance(mockShutdownHandler);
    binder.bind(ReportComponentService.class).toInstance(mockReportComponentService);
    super.configure(binder);
  }

  @Before
  public void setup() throws PlexusCipherException {
    application = tempEntity.newApplicationWithParent("appId");
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
    sourceControl.setRemediationPullRequestsEnabled(true);
    sourceControl.setToken(new DefaultPlexusCipher().encrypt("token", "CMMDwoV"));
    tempEntity.newSourceControl(sourceControl);
  }

  @Test
  public void testSendNotification_noRemediationOptions() throws InterruptedException {
    when(mockPullRequestCommentingRemediationService.getRemediationVersion(any(), eq(application.getId())))
        .thenReturn(Optional.empty());

    scmNotifier.sendNotifications(application, "scanId", new Stage("build"), buildPolicyNotification(), null);

    assertShutDownEventAndJoin(1000);

    assertThat(logOutput).atDebugLevel()
        .contains(
            "No remediation options found for component [maven: {artifactId=Package1, groupId=groupid, version=1.2.3}]");
  }

  @Test
  public void testSendNotification_remediationOptionsAvailable() throws InterruptedException {
    RemediationVersionDTO remediationVersion = new RemediationVersionDTO("2.0.1",
        ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES, 0);
    when(mockPullRequestCommentingRemediationService.getRemediationVersion(any(), eq(application.getId())))
        .thenReturn(Optional.of(remediationVersion));

    scmNotifier.sendNotifications(application, "scanId", new Stage("build"), buildPolicyNotification(), null);

    assertShutDownEventAndJoin(1000);

    // verify that the event did create
    List<SourceControlEvent> all = sourceControlEventDAO.getAll();
    assertThat(all).hasSize(1);
    SourceControlEvent event = all.get(0);
    assertThat(event.getRemediationVersion()).isEqualTo("2.0.1");
    assertThat(event.getApplicationId()).isEqualTo(application.getId());
    assertThat(event.getComponentIdentifier()).isEqualByComparingTo(
        ComponentIdentifier.createMavenCoordinates("groupid", "Package1", "1.2.3"));
    assertThat(event.getStageTypeId()).isEqualTo("build");
    assertThat(event.getEventType()).isEqualTo(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
  }

  @Test
  public void testSendNotification_multipleComponentsWithRemediation() throws InterruptedException {
    ComponentIdentifier component1 = ComponentIdentifier.createMavenCoordinates("group1", "package1", "1.0.0");
    ComponentIdentifier component2 = ComponentIdentifier.createMavenCoordinates("group2", "package2", "2.0.0");

    List<PolicyNotification> notifications = List.of(
        buildPolicyNotification(component1),
        buildPolicyNotification(component2));

    RemediationVersionDTO remediationVersionDTO1 =
        new RemediationVersionDTO("1.1.0", ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES);
    RemediationVersionDTO remediationVersionDTO2 =
        new RemediationVersionDTO("2.1.0", ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES);

    when(mockPullRequestCommentingRemediationService.getRemediationVersion(eq(component1), eq(application.getId())))
        .thenReturn(Optional.of(remediationVersionDTO1));
    when(mockPullRequestCommentingRemediationService.getRemediationVersion(eq(component2), eq(application.getId())))
        .thenReturn(Optional.of(remediationVersionDTO2));

    scmNotifier.sendNotifications(application, "scanId", new Stage("build"), notifications, null);

    assertShutDownEventAndJoin(5000);

    // Verify that the events were created for both components
    List<SourceControlEvent> all = sourceControlEventDAO.getAll();
    assertThat(all).hasSize(2);
    SourceControlEvent event1 = all.get(0);
    assertThat(event1.getRemediationVersion()).isEqualTo("1.1.0");
    assertThat(event1.getApplicationId()).isEqualTo(application.getId());
    assertThat(event1.getComponentIdentifier()).isEqualByComparingTo(component1);
    assertThat(event1.getStageTypeId()).isEqualTo("build");
    assertThat(event1.getEventType()).isEqualTo(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
    SourceControlEvent event2 = all.get(1);
    assertThat(event2.getRemediationVersion()).isEqualTo("2.1.0");
    assertThat(event2.getApplicationId()).isEqualTo(application.getId());
    assertThat(event2.getComponentIdentifier()).isEqualByComparingTo(component2);
    assertThat(event2.getStageTypeId()).isEqualTo("build");
    assertThat(event2.getEventType()).isEqualTo(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
  }

  @Test
  public void testSendNotification_withDirectDependency() throws InterruptedException, IOException {
    ComponentIdentifier directDependency = ComponentIdentifier.createMavenCoordinates("group1", "package1", "1.0.0");
    List<PolicyNotification> notifications = List.of(buildPolicyNotification(directDependency));

    // Direct Dependency
    mockDependencyType(directDependency, true);

    RemediationVersionDTO remediationVersionDTO =
        new RemediationVersionDTO("1.1.0", ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES);
    when(mockPullRequestCommentingRemediationService.getRemediationVersion(eq(directDependency),
        eq(application.getId())))
            .thenReturn(Optional.of(remediationVersionDTO));

    scmNotifier.sendNotifications(application, "scanId", new Stage("build"), notifications, null);

    assertShutDownEventAndJoin(1000);

    // Verify remediation is created for the direct dependency
    List<SourceControlEvent> all = sourceControlEventDAO.getAll();
    assertThat(all).hasSize(1);
    SourceControlEvent event = all.get(0);
    assertThat(event.getRemediationVersion()).isEqualTo("1.1.0");
    assertThat(event.getApplicationId()).isEqualTo(application.getId());
    assertThat(event.getComponentIdentifier()).isEqualByComparingTo(directDependency);
    assertThat(event.getStageTypeId()).isEqualTo("build");
    assertThat(event.getEventType()).isEqualTo(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
  }

  @Test
  public void testSendNotification_withTransitiveDependency() throws InterruptedException, IOException {
    ComponentIdentifier transitiveDependency =
        ComponentIdentifier.createMavenCoordinates("group2", "package2", "2.0.0");
    List<PolicyNotification> notifications = List.of(buildPolicyNotification(transitiveDependency));

    // Transitive Dependency
    mockDependencyType(transitiveDependency, false);

    scmNotifier.sendNotifications(application, "scanId", new Stage("build"), notifications, null);

    assertShutDownEventAndJoin(1000);

    // Verify no remediation is created for the transitive dependency
    List<SourceControlEvent> all = sourceControlEventDAO.getAll();
    assertThat(all).isEmpty();
  }

  @Test
  public void testSendNotification_withMissingDependencyType() throws InterruptedException, IOException {
    ComponentIdentifier missingDependencyType =
        ComponentIdentifier.createMavenCoordinates("group3", "package3", "3.0.0");
    List<PolicyNotification> notifications = List.of(buildPolicyNotification(missingDependencyType));

    // Missing dependency type
    mockDependencyType(missingDependencyType, null);

    RemediationVersionDTO remediationVersionDTO =
        new RemediationVersionDTO("3.1.0", ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES);
    when(mockPullRequestCommentingRemediationService.getRemediationVersion(eq(missingDependencyType),
        eq(application.getId())))
            .thenReturn(Optional.of(remediationVersionDTO));

    scmNotifier.sendNotifications(application, "scanId", new Stage("build"), notifications, null);

    assertShutDownEventAndJoin(1000);

    // Verify remediation is created when dependency type is missing
    List<SourceControlEvent> all = sourceControlEventDAO.getAll();
    assertThat(all).hasSize(1);
    SourceControlEvent event = all.get(0);
    assertThat(event.getRemediationVersion()).isEqualTo("3.1.0");
    assertThat(event.getApplicationId()).isEqualTo(application.getId());
    assertThat(event.getComponentIdentifier()).isEqualByComparingTo(missingDependencyType);
    assertThat(event.getStageTypeId()).isEqualTo("build");
    assertThat(event.getEventType()).isEqualTo(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
  }

  @Test
  public void testSendNotification_withFetchDependencyTypesException() throws InterruptedException, IOException {
    ComponentIdentifier component = ComponentIdentifier.createMavenCoordinates("group4", "package4", "4.0.0");
    List<PolicyNotification> notifications = List.of(buildPolicyNotification(component));

    // Exception when fetching the dependency types
    mockDependencyTypeThrowsException();

    RemediationVersionDTO remediationVersionDTO =
        new RemediationVersionDTO("4.1.0", ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES);
    when(mockPullRequestCommentingRemediationService.getRemediationVersion(eq(component), eq(application.getId())))
        .thenReturn(Optional.of(remediationVersionDTO));

    scmNotifier.sendNotifications(application, "scanId", new Stage("build"), notifications, null);

    assertShutDownEventAndJoin(1000);

    // Verify remediation is created when report fetch fails
    List<SourceControlEvent> all = sourceControlEventDAO.getAll();
    assertThat(all).hasSize(1);
    SourceControlEvent event = all.get(0);
    assertThat(event.getRemediationVersion()).isEqualTo("4.1.0");
    assertThat(event.getApplicationId()).isEqualTo(application.getId());
    assertThat(event.getComponentIdentifier()).isEqualByComparingTo(component);
    assertThat(event.getStageTypeId()).isEqualTo("build");
    assertThat(event.getEventType()).isEqualTo(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
  }

  private void mockDependencyType(
      ComponentIdentifier componentIdentifier,
      Boolean isDirectDependency) throws IOException
  {
    if (isDirectDependency == null) {
      when(mockReportComponentService.fetchReportAndComponents(eq(application), any(), any()))
          .thenReturn(null);
      return;
    }

    Component component = new Component(componentIdentifier);
    component.setDirectDependency(isDirectDependency);
    ReportComponentData reportComponentData = new ReportComponentData(null, List.of(component));

    when(mockReportComponentService.fetchReportAndComponents(eq(application), any(), any()))
        .thenReturn(reportComponentData);
  }

  private void mockDependencyTypeThrowsException() throws IOException {
    when(mockReportComponentService.fetchReportAndComponents(eq(application), any(), any()))
        .thenThrow(new IOException("Simulated fetch error"));
  }

  private void assertShutDownEventAndJoin(final int millis) throws InterruptedException {
    verify(mockShutdownHandler).add(threadArgumentCaptor.capture(), eq(ShutdownPriority.NOTIFICATIONS));
    assertThat(threadArgumentCaptor.getValue().getName()).startsWith("PolicyAlertScmNotifierForScan");
    threadArgumentCaptor.getValue().join(millis);
  }

  @Test
  public void testSendNotification_formatIsNotSupported() throws Exception {
    ComponentIdentifier unsupportedComponent =
        ComponentIdentifier.createContainerCoordinates("groupid", "Package1", "1.2.3");

    List<PolicyNotification> notifications = List.of(buildPolicyNotification(unsupportedComponent));

    scmNotifier.sendNotifications(application, "scanId", new Stage("build"), notifications, null);

    List<SourceControlEvent> all = sourceControlEventDAO.getAll();
    assertThat(all).isEmpty();

    assertShutDownEventAndJoin(1000);
    assertThat(logOutput).atDebugLevel()
        .contains(
            "Component '" + unsupportedComponent + "' in application '" + application.getPublicId() +
                "' is not eligible for automated PR");
  }

  @Test
  public void testSendNotification_nonDefaultBranch_shouldNotCreatePR() throws Exception {
    List<PolicyNotification> notifications = buildPolicyNotification();

    scmNotifier.sendNotifications(application, "scanId", new Stage("build"), notifications, "feature/clm-37819");

    assertShutDownEventAndJoin(1000);

    List<SourceControlEvent> all = sourceControlEventDAO.getAll();
    assertThat(all).isEmpty();
    assertThat(logOutput).atDebugLevel()
        .contains("scanned branch 'feature/clm-37819' differs from default branch");
  }

  private List<PolicyNotification> buildPolicyNotification() {
    return List.of(buildPolicyNotification(ComponentIdentifier.createMavenCoordinates("groupid", "Package1", "1.2.3")));
  }

  private PolicyNotification buildPolicyNotification(final ComponentIdentifier componentIdentifier) {
    PolicyFact policyFact1 = new PolicyFact("policyid-1", "policyname-1", 3);
    policyFact1.addComponentFact(new ComponentFact(componentIdentifier, randomString()));

    Notifications notifications = new Notifications(
        new UserNotification("foo@mail.com", "release"));
    return new PolicyNotification(policyFact1, notifications);
  }

  private String randomString() {
    return UUID.randomUUID().toString();
  }
}
