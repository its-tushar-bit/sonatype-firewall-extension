/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.relay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.clm.dto.model.policy.Stage;
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

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Translates relay events into {@link SourceControlEvent} instances. One inbound relay event
 * fans out to one event per application bound to its repository URL; events with no matching
 * application or with payloads we cannot interpret are skipped (the caller still acks them so
 * the relay does not redeliver indefinitely).
 */
@Named
@Singleton
public class RelayEventMapper
{
  private static final Logger log = LoggerFactory.getLogger(RelayEventMapper.class);

  static final String INITIATOR_RELAY = "relay";

  private static final String PAYLOAD_NUMBER = "number";

  private static final String PAYLOAD_SOURCE_BRANCH = "sourceBranch";

  private static final String PAYLOAD_TARGET_BRANCH = "targetBranch";

  private static final String PAYLOAD_SOURCE_SHA = "sourceSha";

  private static final String PAYLOAD_REF = "ref";

  private static final String PAYLOAD_AFTER = "after";

  private static final String PAYLOAD_BEFORE = "before";

  private static final String PAYLOAD_INSTALLATION_ID = "installationId";

  private static final String REFS_HEADS_PREFIX = "refs/heads/";

  private final ApplicationDAO applicationDAO;

  private final SourceControlDAO sourceControlDAO;

  private final SourceControlUtils sourceControlUtils;

  private final GitHubAppDAO gitHubAppDAO;

  @Inject
  public RelayEventMapper(
      ApplicationDAO applicationDAO,
      SourceControlDAO sourceControlDAO,
      SourceControlUtils sourceControlUtils,
      GitHubAppDAO gitHubAppDAO)
  {
    this.applicationDAO = applicationDAO;
    this.sourceControlDAO = sourceControlDAO;
    this.sourceControlUtils = sourceControlUtils;
    this.gitHubAppDAO = gitHubAppDAO;
  }

  /**
   * Maps a single relay event to zero or more {@link SourceControlEvent}s. Returns an empty list
   * for unknown event types, push events on non-default branches, missing payload fields, or
   * repository URLs that no application is bound to.
   *
   * <p>
   * Each call queries the DB for applications (and source-controls for push events) by
   * repository URL. Callers processing a batch of events should prefer
   * {@link #map(RelayEvent, Map, Map, String)} with shared caches so repeated URLs are looked up
   * once and a per-cycle authentication type pre-derived from the relay configuration.
   */
  public List<SourceControlEvent> map(RelayEvent relayEvent) {
    return map(relayEvent, new HashMap<>(), new HashMap<>(), null);
  }

  /**
   * Batch-friendly variant of {@link #map(RelayEvent)}. The two maps are populated lazily on
   * first repository-URL hit and reused across subsequent events sharing the same URL, which
   * eliminates the N+1 DAO calls when a polling cycle returns multiple events on one repo.
   *
   * <p>
   * {@code authenticationType} is the
   * {@link com.sonatype.insight.brain.model.sourcecontrol.SourceControl.AuthenticationType}
   * name ("PAT" / "GITHUB_APP") derived once per cycle from the relay configuration; it is
   * applied to every produced event so support-zip and telemetry can attribute events to a
   * mode without re-querying the configuration. {@code null} is permitted (mapper is also
   * called from contexts without a configuration in scope, e.g. unit tests).
   */
  public List<SourceControlEvent> map(
      RelayEvent relayEvent,
      Map<String, List<Application>> applicationsByRepoUrl,
      Map<String, List<SourceControl>> sourceControlsByRepoUrl,
      String authenticationType)
  {
    if (relayEvent == null || StringUtils.isBlank(relayEvent.getRepositoryUrl())
        || StringUtils.isBlank(relayEvent.getEventType()))
    {
      log.debug("Skipping malformed relay event: {}", relayEvent);
      return Collections.emptyList();
    }

    // Cache by the normalized URL so two events on the same physical repo arriving with
    // different surface forms (e.g. https vs http, trailing slash, mixed case) hit the cache
    // on the second event instead of issuing a duplicate DAO call. The DAO normalizes
    // internally; we mirror that here to keep the per-cycle cache as effective as the
    // Javadoc claims.
    String normalizedRepoUrl = SourceControl.normalizeRepositoryUrl(relayEvent.getRepositoryUrl());
    List<Application> applications =
        applicationsByRepoUrl.computeIfAbsent(normalizedRepoUrl, key -> applicationDAO.getByRepositoryUrl(key));
    if (applications.isEmpty()) {
      log.debug("No applications bound to repositoryUrl='{}' for relay eventId={}; skipping",
          relayEvent.getRepositoryUrl(), relayEvent.getEventId());
      return Collections.emptyList();
    }

    Map<String, Object> payload = relayEvent.getPayload() != null ? relayEvent.getPayload() : Collections.emptyMap();
    String installationId = extractInstallationId(payload);
    String gitHubAppId = resolveGitHubAppId(installationId);

    switch (relayEvent.getEventType()) {
      case RelayEvent.TYPE_PULL_REQUEST_OPENED:
        return mapPullRequest(applications, payload, EventBuilder.OPENED, authenticationType, installationId,
            gitHubAppId);
      case RelayEvent.TYPE_PULL_REQUEST_UPDATED:
        return mapPullRequest(applications, payload, EventBuilder.UPDATED, authenticationType, installationId,
            gitHubAppId);
      case RelayEvent.TYPE_PULL_REQUEST_CLOSED:
        return mapPullRequest(applications, payload, EventBuilder.CLOSED, authenticationType, installationId,
            gitHubAppId);
      case RelayEvent.TYPE_PUSH:
        return mapPush(relayEvent, applications, payload, sourceControlsByRepoUrl, authenticationType, installationId,
            gitHubAppId);
      default:
        log.debug("Unknown relay event type '{}' for eventId={}; skipping",
            relayEvent.getEventType(), relayEvent.getEventId());
        return Collections.emptyList();
    }
  }

  private List<SourceControlEvent> mapPullRequest(
      List<Application> applications,
      Map<String, Object> payload,
      EventBuilder builder,
      String authenticationType,
      String installationId,
      String gitHubAppId)
  {
    Integer prNumber = readInt(payload, PAYLOAD_NUMBER);
    if (prNumber == null) {
      log.warn("Pull request relay event missing 'number' field; skipping");
      return Collections.emptyList();
    }
    String headBranch = readString(payload, PAYLOAD_SOURCE_BRANCH);
    String baseBranch = readString(payload, PAYLOAD_TARGET_BRANCH);
    String headSha = readString(payload, PAYLOAD_SOURCE_SHA);

    List<SourceControlEvent> events = new ArrayList<>(applications.size());
    for (Application application : applications) {
      SourceControlEvent event = new SourceControlEvent()
          .setApplicationId(application.getId())
          .setPullRequestNumber(prNumber)
          .setBranchName(headBranch)
          .setBaseBranchName(baseBranch)
          .setCommitHash(headSha)
          .setInitiator(INITIATOR_RELAY)
          .setAuthenticationType(authenticationType)
          .setInstallationId(installationId)
          .setGithubAppId(gitHubAppId)
          .setScmUsername(getScmUsernameSafely(application.getId()));
      builder.apply(event);
      events.add(event);
    }
    return events;
  }

  private List<SourceControlEvent> mapPush(
      RelayEvent relayEvent,
      List<Application> applications,
      Map<String, Object> payload,
      Map<String, List<SourceControl>> sourceControlsByRepoUrl,
      String authenticationType,
      String installationId,
      String gitHubAppId)
  {
    String pushedBranch = stripRefsHeads(readString(payload, PAYLOAD_REF));
    String headSha = readString(payload, PAYLOAD_AFTER);
    String baseSha = readString(payload, PAYLOAD_BEFORE);
    if (StringUtils.isBlank(pushedBranch)) {
      log.debug("Push event missing ref for eventId={}; skipping", relayEvent.getEventId());
      return Collections.emptyList();
    }

    String normalizedRepoUrl = SourceControl.normalizeRepositoryUrl(relayEvent.getRepositoryUrl());
    List<SourceControl> sourceControls =
        sourceControlsByRepoUrl.computeIfAbsent(normalizedRepoUrl, key -> sourceControlDAO.getByRepositoryUrl(key));
    // SourceControl rows for a repository URL can be owned by either the application itself
    // (app-level SCM config) or the parent organization (org-level config inherited by all
    // applications in that org). Index by ownerId; the per-application lookup below tries
    // application id first, then falls back to the organization id.
    Map<String, String> baseBranchByOwnerId = new HashMap<>();
    for (SourceControl sc : sourceControls) {
      baseBranchByOwnerId.put(sc.getOwnerId(), sc.getBaseBranch());
    }

    List<SourceControlEvent> events = new ArrayList<>();
    for (Application application : applications) {
      String baseBranch = baseBranchByOwnerId.get(application.getId());
      if (StringUtils.isBlank(baseBranch) && application.getOrganizationId() != null) {
        baseBranch = baseBranchByOwnerId.get(application.getOrganizationId());
      }
      if (StringUtils.isBlank(baseBranch) || !pushedBranch.equalsIgnoreCase(baseBranch)) {
        log.debug("Push to non-default branch '{}' for application '{}' (default='{}'); skipping",
            pushedBranch, application.getId(), baseBranch);
        continue;
      }
      SourceControlEvent event = new SourceControlEvent()
          .forSourceControlEvaluation()
          .setApplicationId(application.getId())
          .setBranchName(pushedBranch)
          .setCommitHash(headSha)
          .setBaseCommitHash(baseSha)
          .setStageTypeId(Stage.ID_SOURCE)
          .setScanTriggerType(ScanTriggerType.SOURCE_CONTROL_INTERNAL_DEFAULT_BRANCH_MONITORING)
          .setInitiator(INITIATOR_RELAY)
          .setAuthenticationType(authenticationType)
          .setInstallationId(installationId)
          .setGithubAppId(gitHubAppId)
          .setScmUsername(getScmUsernameSafely(application.getId()));
      events.add(event);
    }
    return events;
  }

  /**
   * Extracts the GitHub App installation id from the relay payload. The relay forwards it as
   * a top-level {@code installationId} numeric on App-mode deliveries; PAT-mode deliveries
   * (and non-GitHub providers) omit it. The id is rendered as a string to match the
   * {@code source_control_event.installation_id varchar(64)} column shape used by the legacy
   * polling path ({@code PullRequestTask}).
   */
  private static String extractInstallationId(Map<String, Object> payload) {
    Object id = payload.get(PAYLOAD_INSTALLATION_ID);
    if (id instanceof Number) {
      return Long.toString(((Number) id).longValue());
    }
    if (id instanceof String && !((String) id).isBlank()) {
      return (String) id;
    }
    return null;
  }

  /**
   * Resolves the local {@code github_app.id} (UUID) corresponding to the supplied installation
   * id. Returns {@code null} for PAT-mode events (no installation id), unparseable ids, or
   * installation ids the local DB doesn't know about (a stale event for a deleted App, or
   * a delayed delivery from another tenant in test environments). DAO failures are
   * swallowed and logged at debug — mapping should not fail because of a metadata lookup
   * miss.
   */
  private String resolveGitHubAppId(String installationId) {
    if (installationId == null) {
      return null;
    }
    long parsed;
    try {
      parsed = Long.parseLong(installationId);
    }
    catch (NumberFormatException e) {
      return null;
    }
    try {
      GitHubApp app = gitHubAppDAO.getActiveByInstallationId(parsed);
      return app == null ? null : app.getId();
    }
    catch (RuntimeException e) {
      log.debug("Could not resolve GitHub App id for installation {}: {}", installationId, e.getMessage());
      return null;
    }
  }

  private String getScmUsernameSafely(String applicationId) {
    try {
      return sourceControlUtils.getScmUserIdForApplication(applicationId);
    }
    catch (RuntimeException e) {
      // SCM username is best-effort: SourceControlEventPublisher will lazily attempt the same
      // lookup at publish time, and the load balancer tolerates a null synchronization key.
      log.debug("Unable to resolve SCM username for application '{}': {}", applicationId, e.getMessage());
      return null;
    }
  }

  private static Integer readInt(Map<String, Object> payload, String key) {
    Object value = payload.get(key);
    if (value instanceof Number) {
      long longValue = ((Number) value).longValue();
      if (longValue > Integer.MAX_VALUE || longValue < Integer.MIN_VALUE) {
        // Defensive: GitHub PR numbers are well below INT_MAX today, but providers that
        // expose 64-bit identifiers (some self-hosted SCMs) could overflow. Skip rather
        // than silently truncate.
        log.warn("Relay event field '{}' value {} exceeds int range; treating as null", key, longValue);
        return null;
      }
      return (int) longValue;
    }
    if (value instanceof String && StringUtils.isNumeric((String) value)) {
      try {
        long longValue = Long.parseLong((String) value);
        if (longValue > Integer.MAX_VALUE || longValue < Integer.MIN_VALUE) {
          log.warn("Relay event field '{}' value {} exceeds int range; treating as null", key, longValue);
          return null;
        }
        return (int) longValue;
      }
      catch (NumberFormatException e) {
        log.warn("Relay event field '{}' value '{}' is not a valid integer; treating as null", key, value);
        return null;
      }
    }
    return null;
  }

  private static String readString(Map<String, Object> payload, String key) {
    Object value = payload.get(key);
    return value instanceof String ? (String) value : null;
  }

  private static String stripRefsHeads(String ref) {
    if (ref == null) {
      return null;
    }
    return ref.startsWith(REFS_HEADS_PREFIX) ? ref.substring(REFS_HEADS_PREFIX.length()) : ref;
  }

  /** Each PR variant uses the same builder shape; only the event type differs. */
  private enum EventBuilder
  {
    OPENED
    {
      @Override
      void apply(SourceControlEvent event) {
        event.forDiscoveredPullRequest();
      }
    },
    UPDATED
    {
      @Override
      void apply(SourceControlEvent event) {
        event.forUpdatedPullRequest();
      }
    },
    CLOSED
    {
      @Override
      void apply(SourceControlEvent event) {
        event.forPullRequestStateUpdate();
      }
    };

    abstract void apply(SourceControlEvent event);
  }
}
