/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.relay;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.githubapp.GitHubApp;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.relay.dto.RelayEvent;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RelayEventMapperTest
{
  private static final String REPO_URL = "https://github.com/org/repo";

  @Mock
  private ApplicationDAO applicationDAO;

  @Mock
  private SourceControlDAO sourceControlDAO;

  @Mock
  private SourceControlUtils sourceControlUtils;

  @Mock
  private GitHubAppDAO gitHubAppDAO;

  private RelayEventMapper mapper;

  @BeforeEach
  public void before() {
    mapper = new RelayEventMapper(applicationDAO, sourceControlDAO, sourceControlUtils, gitHubAppDAO);
    lenient().when(sourceControlUtils.getScmUserIdForApplication("app-1")).thenReturn("user-1");
    lenient().when(sourceControlUtils.getScmUserIdForApplication("app-2")).thenReturn("user-2");
  }

  @Test
  public void map_pullRequestOpened_buildsDiscoveredEvent() {
    when(applicationDAO.getByRepositoryUrl(REPO_URL)).thenReturn(List.of(application("app-1")));

    List<SourceControlEvent> events = mapper.map(relayEvent(RelayEvent.TYPE_PULL_REQUEST_OPENED, prPayload(42)));

    assertThat(events).hasSize(1);
    SourceControlEvent event = events.get(0);
    assertThat(event.getEventType()).isEqualTo(SourceControlEvent.DISCOVERED_PULL_REQUEST_EVENT);
    assertThat(event.getApplicationId()).isEqualTo("app-1");
    assertThat(event.getPullRequestNumber()).isEqualTo(42);
    assertThat(event.getBranchName()).isEqualTo("feature");
    assertThat(event.getBaseBranchName()).isEqualTo("main");
    assertThat(event.getCommitHash()).isEqualTo("abc123");
    assertThat(event.getInitiator()).isEqualTo(RelayEventMapper.INITIATOR_RELAY);
    assertThat(event.getScmUsername()).isEqualTo("user-1");
  }

  @Test
  public void map_pullRequestUpdated_buildsUpdatedEvent() {
    when(applicationDAO.getByRepositoryUrl(REPO_URL)).thenReturn(List.of(application("app-1")));

    List<SourceControlEvent> events = mapper.map(relayEvent(RelayEvent.TYPE_PULL_REQUEST_UPDATED, prPayload(7)));

    assertThat(events).hasSize(1);
    assertThat(events.get(0).getEventType()).isEqualTo(SourceControlEvent.UPDATED_PULL_REQUEST_EVENT);
  }

  @Test
  public void map_pullRequestClosed_buildsStateUpdateEvent() {
    when(applicationDAO.getByRepositoryUrl(REPO_URL)).thenReturn(List.of(application("app-1")));

    List<SourceControlEvent> events = mapper.map(relayEvent(RelayEvent.TYPE_PULL_REQUEST_CLOSED, prPayload(7)));

    assertThat(events).hasSize(1);
    assertThat(events.get(0).getEventType()).isEqualTo(SourceControlEvent.PR_STATE_UPDATE_EVENT);
  }

  @Test
  public void map_pullRequestOpened_fansOutAcrossApplications() {
    when(applicationDAO.getByRepositoryUrl(REPO_URL))
        .thenReturn(List.of(application("app-1"), application("app-2")));

    List<SourceControlEvent> events = mapper.map(relayEvent(RelayEvent.TYPE_PULL_REQUEST_OPENED, prPayload(11)));

    assertThat(events).extracting(SourceControlEvent::getApplicationId).containsExactly("app-1", "app-2");
    assertThat(events).extracting(SourceControlEvent::getPullRequestNumber).containsOnly(11);
  }

  @Test
  public void map_unknownRepository_returnsEmpty() {
    when(applicationDAO.getByRepositoryUrl(REPO_URL)).thenReturn(Collections.emptyList());

    assertThat(mapper.map(relayEvent(RelayEvent.TYPE_PULL_REQUEST_OPENED, prPayload(1)))).isEmpty();
  }

  @Test
  public void map_pullRequestMissingNumber_returnsEmpty() {
    when(applicationDAO.getByRepositoryUrl(REPO_URL)).thenReturn(List.of(application("app-1")));
    Map<String, Object> payload = new HashMap<>();
    payload.put("sourceBranch", "feature");

    assertThat(mapper.map(relayEvent(RelayEvent.TYPE_PULL_REQUEST_OPENED, payload))).isEmpty();
  }

  @Test
  public void map_pushOnDefaultBranch_buildsSourceControlEvaluation() {
    when(applicationDAO.getByRepositoryUrl(REPO_URL)).thenReturn(List.of(application("app-1")));
    when(sourceControlDAO.getByRepositoryUrl(REPO_URL)).thenReturn(List.of(sourceControl("app-1", "main")));

    List<SourceControlEvent> events = mapper.map(relayEvent(RelayEvent.TYPE_PUSH, pushPayload("refs/heads/main")));

    assertThat(events).hasSize(1);
    SourceControlEvent event = events.get(0);
    assertThat(event.getEventType()).isEqualTo(SourceControlEvent.SOURCE_CONTROL_EVALUATION_EVENT);
    assertThat(event.getBranchName()).isEqualTo("main");
    assertThat(event.getCommitHash()).isEqualTo("after-sha");
    assertThat(event.getBaseCommitHash()).isEqualTo("before-sha");
    assertThat(event.getScanTriggerType())
        .isEqualTo(ScanTriggerType.SOURCE_CONTROL_INTERNAL_DEFAULT_BRANCH_MONITORING);
  }

  @Test
  public void map_pushWithMissingShas_buildsEventWithNullCommitHashes() {
    when(applicationDAO.getByRepositoryUrl(REPO_URL)).thenReturn(List.of(application("app-1")));
    when(sourceControlDAO.getByRepositoryUrl(REPO_URL)).thenReturn(List.of(sourceControl("app-1", "main")));
    Map<String, Object> payload = pushPayload("refs/heads/main");
    payload.remove("after");
    payload.remove("before");

    List<SourceControlEvent> events = mapper.map(relayEvent(RelayEvent.TYPE_PUSH, payload));

    assertThat(events).hasSize(1);
    SourceControlEvent event = events.get(0);
    assertThat(event.getCommitHash()).isNull();
    assertThat(event.getBaseCommitHash()).isNull();
    // Branch + scan trigger still resolve; null shas are tolerated downstream by the
    // SourceControlEvent model and event DAO insert.
    assertThat(event.getBranchName()).isEqualTo("main");
  }

  @Test
  public void map_pushOnNonDefaultBranch_returnsEmpty() {
    when(applicationDAO.getByRepositoryUrl(REPO_URL)).thenReturn(List.of(application("app-1")));
    when(sourceControlDAO.getByRepositoryUrl(REPO_URL)).thenReturn(List.of(sourceControl("app-1", "main")));

    List<SourceControlEvent> events =
        mapper.map(relayEvent(RelayEvent.TYPE_PUSH, pushPayload("refs/heads/feature")));

    assertThat(events).isEmpty();
  }

  @Test
  public void map_pushAcceptsBareBranchName() {
    when(applicationDAO.getByRepositoryUrl(REPO_URL)).thenReturn(List.of(application("app-1")));
    when(sourceControlDAO.getByRepositoryUrl(REPO_URL)).thenReturn(List.of(sourceControl("app-1", "main")));

    // Bitbucket style: ref does not have refs/heads/ prefix
    List<SourceControlEvent> events = mapper.map(relayEvent(RelayEvent.TYPE_PUSH, pushPayload("main")));

    assertThat(events).hasSize(1);
  }

  @Test
  public void map_pushFanout_filtersPerApplicationDefaultBranch() {
    when(applicationDAO.getByRepositoryUrl(REPO_URL))
        .thenReturn(List.of(application("app-1"), application("app-2")));
    when(sourceControlDAO.getByRepositoryUrl(REPO_URL))
        .thenReturn(List.of(sourceControl("app-1", "main"), sourceControl("app-2", "develop")));

    List<SourceControlEvent> events = mapper.map(relayEvent(RelayEvent.TYPE_PUSH, pushPayload("main")));

    assertThat(events).extracting(SourceControlEvent::getApplicationId).containsExactly("app-1");
  }

  @Test
  public void map_pushOrgLevelSourceControl_resolvesViaOrganizationId() {
    // SourceControl is owned by the organization, not the application. The mapper must
    // fall back to the application's organizationId when the per-app key misses.
    Application app = application("app-1");
    app.setOrganizationId("org-1");
    when(applicationDAO.getByRepositoryUrl(REPO_URL)).thenReturn(List.of(app));
    when(sourceControlDAO.getByRepositoryUrl(REPO_URL))
        .thenReturn(List.of(sourceControl("org-1", "main")));

    List<SourceControlEvent> events = mapper.map(relayEvent(RelayEvent.TYPE_PUSH, pushPayload("refs/heads/main")));

    assertThat(events).hasSize(1);
    assertThat(events.get(0).getApplicationId()).isEqualTo("app-1");
    assertThat(events.get(0).getBranchName()).isEqualTo("main");
  }

  @Test
  public void map_pushAppLevelOverridesOrgLevel_whenBothPresent() {
    // App-level config takes precedence over org-level (matches inheritance semantics).
    Application app = application("app-1");
    app.setOrganizationId("org-1");
    when(applicationDAO.getByRepositoryUrl(REPO_URL)).thenReturn(List.of(app));
    when(sourceControlDAO.getByRepositoryUrl(REPO_URL))
        .thenReturn(List.of(sourceControl("app-1", "main"), sourceControl("org-1", "develop")));

    // Push to "main" — should match the app-level baseBranch and produce an event.
    List<SourceControlEvent> events = mapper.map(relayEvent(RelayEvent.TYPE_PUSH, pushPayload("refs/heads/main")));

    assertThat(events).hasSize(1);
  }

  @Test
  public void map_unknownEventType_returnsEmpty() {
    when(applicationDAO.getByRepositoryUrl(REPO_URL)).thenReturn(List.of(application("app-1")));
    RelayEvent event = relayEvent("ping", new HashMap<>());

    assertThat(mapper.map(event)).isEmpty();
  }

  @Test
  public void map_nullEvent_returnsEmpty() {
    assertThat(mapper.map(null)).isEmpty();
  }

  @Test
  public void map_blankRepositoryUrl_returnsEmpty() {
    RelayEvent event = relayEvent(RelayEvent.TYPE_PULL_REQUEST_OPENED, prPayload(1));
    event.setRepositoryUrl("");
    assertThat(mapper.map(event)).isEmpty();
  }

  @Test
  public void map_scmUserResolutionFailure_isSwallowed() {
    when(applicationDAO.getByRepositoryUrl(REPO_URL)).thenReturn(List.of(application("app-1")));
    when(sourceControlUtils.getScmUserIdForApplication("app-1"))
        .thenThrow(new RuntimeException("scm config missing"));

    List<SourceControlEvent> events = mapper.map(relayEvent(RelayEvent.TYPE_PULL_REQUEST_OPENED, prPayload(1)));

    assertThat(events).hasSize(1);
    assertThat(events.get(0).getScmUsername()).isNull();
  }

  @Test
  public void map_pullRequestOpened_appliesAuthenticationTypeAndInstallationId() {
    when(applicationDAO.getByRepositoryUrl(REPO_URL)).thenReturn(List.of(application("app-1")));
    Map<String, Object> payload = prPayload(99);
    payload.put("installationId", 12345L);

    List<SourceControlEvent> events = mapper.map(
        relayEvent(RelayEvent.TYPE_PULL_REQUEST_OPENED, payload),
        new HashMap<>(),
        new HashMap<>(),
        "GITHUB_APP");

    assertThat(events).hasSize(1);
    assertThat(events.get(0).getAuthenticationType()).isEqualTo("GITHUB_APP");
    assertThat(events.get(0).getInstallationId()).isEqualTo("12345");
  }

  @Test
  public void map_push_appliesAuthenticationTypeAndInstallationId() {
    when(applicationDAO.getByRepositoryUrl(REPO_URL)).thenReturn(List.of(application("app-1")));
    when(sourceControlDAO.getByRepositoryUrl(REPO_URL)).thenReturn(List.of(sourceControl("app-1", "main")));
    Map<String, Object> payload = pushPayload("refs/heads/main");
    payload.put("installationId", 7777L);

    List<SourceControlEvent> events = mapper.map(
        relayEvent(RelayEvent.TYPE_PUSH, payload),
        new HashMap<>(),
        new HashMap<>(),
        "GITHUB_APP");

    assertThat(events).hasSize(1);
    assertThat(events.get(0).getAuthenticationType()).isEqualTo("GITHUB_APP");
    assertThat(events.get(0).getInstallationId()).isEqualTo("7777");
  }

  @Test
  public void map_patMode_setsAuthenticationTypeWithoutInstallationId() {
    when(applicationDAO.getByRepositoryUrl(REPO_URL)).thenReturn(List.of(application("app-1")));
    // PAT-mode payloads from non-GitHub providers (or PAT-mode GitHub before the App is wired)
    // never include the installation block.
    List<SourceControlEvent> events = mapper.map(
        relayEvent(RelayEvent.TYPE_PULL_REQUEST_OPENED, prPayload(1)),
        new HashMap<>(),
        new HashMap<>(),
        "PAT");

    assertThat(events).hasSize(1);
    assertThat(events.get(0).getAuthenticationType()).isEqualTo("PAT");
    assertThat(events.get(0).getInstallationId()).isNull();
  }

  @Test
  public void map_installationIdAsString_isAccepted() {
    // Defensive: relays may serialize numeric ids as strings depending on JSON shape.
    when(applicationDAO.getByRepositoryUrl(REPO_URL)).thenReturn(List.of(application("app-1")));
    Map<String, Object> payload = prPayload(1);
    payload.put("installationId", "42");

    List<SourceControlEvent> events = mapper.map(
        relayEvent(RelayEvent.TYPE_PULL_REQUEST_OPENED, payload),
        new HashMap<>(),
        new HashMap<>(),
        "GITHUB_APP");

    assertThat(events).hasSize(1);
    assertThat(events.get(0).getInstallationId()).isEqualTo("42");
  }

  @Test
  public void map_malformedInstallationBlock_isIgnored() {
    when(applicationDAO.getByRepositoryUrl(REPO_URL)).thenReturn(List.of(application("app-1")));
    Map<String, Object> payload = prPayload(1);
    // Payload is present but missing the installationId field — must not blow up,
    // just leave installationId null.
    payload.put("installation", Map.of("slug", "some-app"));

    List<SourceControlEvent> events = mapper.map(
        relayEvent(RelayEvent.TYPE_PULL_REQUEST_OPENED, payload),
        new HashMap<>(),
        new HashMap<>(),
        "GITHUB_APP");

    assertThat(events).hasSize(1);
    assertThat(events.get(0).getInstallationId()).isNull();
  }

  @Test
  public void map_resolvesGithubAppIdFromInstallationId() {
    when(applicationDAO.getByRepositoryUrl(REPO_URL)).thenReturn(List.of(application("app-1")));
    GitHubApp app = new GitHubApp();
    app.setId("github-app-uuid-1");
    app.setInstallationId(54321L);
    when(gitHubAppDAO.getActiveByInstallationId(54321L)).thenReturn(app);

    Map<String, Object> payload = prPayload(1);
    payload.put("installationId", 54321L);
    List<SourceControlEvent> events = mapper.map(
        relayEvent(RelayEvent.TYPE_PULL_REQUEST_OPENED, payload),
        new HashMap<>(),
        new HashMap<>(),
        "GITHUB_APP");

    assertThat(events).hasSize(1);
    assertThat(events.get(0).getInstallationId()).isEqualTo("54321");
    assertThat(events.get(0).getGithubAppId()).isEqualTo("github-app-uuid-1");
  }

  @Test
  public void map_unknownInstallationId_leavesGithubAppIdNull() {
    // A relay-side delivery for an installation that no longer has a local github_app row
    // (e.g. App was deleted but a queued event arrived afterwards). The mapper still produces
    // the event — just without the App correlation.
    when(applicationDAO.getByRepositoryUrl(REPO_URL)).thenReturn(List.of(application("app-1")));
    when(gitHubAppDAO.getActiveByInstallationId(99999L)).thenReturn(null);

    Map<String, Object> payload = prPayload(1);
    payload.put("installationId", 99999L);
    List<SourceControlEvent> events = mapper.map(
        relayEvent(RelayEvent.TYPE_PULL_REQUEST_OPENED, payload),
        new HashMap<>(),
        new HashMap<>(),
        "GITHUB_APP");

    assertThat(events).hasSize(1);
    assertThat(events.get(0).getInstallationId()).isEqualTo("99999");
    assertThat(events.get(0).getGithubAppId()).isNull();
  }

  @Test
  public void map_patMode_doesNotLookUpGithubApp() {
    // PAT events have no installation id; the DAO should never be called.
    when(applicationDAO.getByRepositoryUrl(REPO_URL)).thenReturn(List.of(application("app-1")));

    mapper.map(
        relayEvent(RelayEvent.TYPE_PULL_REQUEST_OPENED, prPayload(1)),
        new HashMap<>(),
        new HashMap<>(),
        "PAT");

    org.mockito.Mockito.verifyNoInteractions(gitHubAppDAO);
  }

  @Test
  public void map_githubAppDaoFailure_swallowsAndLeavesGithubAppIdNull() {
    when(applicationDAO.getByRepositoryUrl(REPO_URL)).thenReturn(List.of(application("app-1")));
    when(gitHubAppDAO.getActiveByInstallationId(11111L))
        .thenThrow(new RuntimeException("db transient"));

    Map<String, Object> payload = prPayload(1);
    payload.put("installationId", 11111L);
    List<SourceControlEvent> events = mapper.map(
        relayEvent(RelayEvent.TYPE_PULL_REQUEST_OPENED, payload),
        new HashMap<>(),
        new HashMap<>(),
        "GITHUB_APP");

    assertThat(events).hasSize(1);
    assertThat(events.get(0).getInstallationId()).isEqualTo("11111");
    assertThat(events.get(0).getGithubAppId()).isNull();
  }

  private static Application application(String id) {
    Application app = new Application();
    app.setId(id);
    return app;
  }

  private static SourceControl sourceControl(String ownerId, String baseBranch) {
    SourceControl sc = new SourceControl();
    sc.setOwnerId(ownerId);
    sc.setRepositoryUrl(REPO_URL);
    sc.setBaseBranch(baseBranch);
    return sc;
  }

  private static RelayEvent relayEvent(String type, Map<String, Object> payload) {
    RelayEvent event = new RelayEvent();
    event.setEventId("e-" + type);
    event.setProvider("github");
    event.setEventType(type);
    event.setRepositoryUrl(REPO_URL);
    event.setTimestamp("2026-01-01T00:00:00Z");
    event.setReceiptHandle("rh-1");
    event.setPayload(payload);
    return event;
  }

  private static Map<String, Object> prPayload(int number) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("number", number);
    payload.put("sourceBranch", "feature");
    payload.put("targetBranch", "main");
    payload.put("sourceSha", "abc123");
    return payload;
  }

  private static Map<String, Object> pushPayload(String ref) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("ref", ref);
    payload.put("after", "after-sha");
    payload.put("before", "before-sha");
    return payload;
  }
}
