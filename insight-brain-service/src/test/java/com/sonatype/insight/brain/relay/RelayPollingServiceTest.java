/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.relay;

import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppDAO;
import com.sonatype.insight.brain.git.PullRequestPollingScheduler;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.model.relay.RelayConfiguration;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.relay.dto.RelayAckResponse;
import com.sonatype.insight.brain.relay.dto.RelayEvent;
import com.sonatype.insight.brain.relay.dto.RelayEventsResponse;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.ScmNodeProcessor;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;

import com.sonatype.insight.error.exception.BadGatewayException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RelayPollingServiceTest
{
  @Mock
  private RelayClient relayClient;

  @Mock
  private RelayRegistrationService relayRegistrationService;

  @Mock
  private GitHubAppDAO gitHubAppDAO;

  @Mock
  private GitHubAppRelayLinker gitHubAppRelayLinker;

  @Mock
  private RelayEventMapper relayEventMapper;

  @Mock
  private SourceControlEventPublisher sourceControlEventPublisher;

  @Mock
  private PullRequestPollingScheduler pullRequestPollingScheduler;

  @Mock
  private PasswordHandler passwordHandler;

  @Mock
  private ShutdownHandler shutdownHandler;

  @Mock
  private ScmNodeProcessor scmNodeProcessor;

  private RelayPollingService service;

  @Before
  public void before() {
    // Default: gitHubAppDAO returns nothing for the relay-link retry pre-flight in most
    // tests. Tests that exercise the retry loop override this stub locally.
    lenient().when(gitHubAppDAO.getActiveByRelayLinkState(org.mockito.ArgumentMatchers.anySet()))
        .thenReturn(java.util.Collections.emptyList());
    // Default: 3 failures triggers fallback (matches the production constant).
    service = new RelayPollingService(relayClient, relayRegistrationService, gitHubAppDAO, gitHubAppRelayLinker,
        relayEventMapper, sourceControlEventPublisher, pullRequestPollingScheduler, passwordHandler, shutdownHandler,
        scmNodeProcessor, 0, 60, 50, 3, 1);
    service.disableSchedulingForTesting = true;

    lenient().when(passwordHandler.decryptPassword(anyString())).thenAnswer(inv -> "plain-" + inv.getArgument(0));
    lenient().when(pullRequestPollingScheduler.setSuppressed(anyBoolean())).thenReturn(true);
  }

  @Test
  public void pollOnce_featureGateClosed_isNoop() {
    when(relayRegistrationService.isFeatureGateOpen()).thenReturn(false);

    service.pollOnce();

    verify(relayClient, never()).pollEvents(anyString(), anyInt());
    verify(sourceControlEventPublisher, never()).publishEventBypassingFeatureGate(any());
  }

  @Test
  public void pollOnce_featureGateClosed_keepsLegacyPollingActive() {
    when(relayRegistrationService.isFeatureGateOpen()).thenReturn(false);

    service.pollOnce();

    verify(pullRequestPollingScheduler).setSuppressed(false);
  }

  @Test
  public void pollOnce_noConfiguration_triggersRegistrationAndDefers() {
    when(relayRegistrationService.isFeatureGateOpen()).thenReturn(true);
    when(relayRegistrationService.getConfiguration()).thenReturn(null);

    service.pollOnce();

    verify(relayRegistrationService).registerOnDemand();
    verify(relayClient, never()).pollEvents(anyString(), anyInt());
    // Successful pre-flight registration is intentionally NOT a relay-health signal:
    // neither recordSuccess() nor recordFailure() runs, so legacy suppression must be
    // left untouched in either direction. Asserts both halves of that contract so a
    // future refactor that adds recordSuccess() here cannot silently start suppressing
    // legacy polling before any real relay poll has succeeded.
    verify(pullRequestPollingScheduler, never()).setSuppressed(anyBoolean());
  }

  @Test
  public void pollOnce_noConfiguration_registrationFailure_engagesFallbackAfterThreshold() {
    when(relayRegistrationService.isFeatureGateOpen()).thenReturn(true);
    when(relayRegistrationService.getConfiguration()).thenReturn(null);
    org.mockito.Mockito.doThrow(new RuntimeException("relay unreachable"))
        .when(relayRegistrationService)
        .registerOnDemand();

    service.pollOnce();
    service.pollOnce();
    service.pollOnce();

    // 3 consecutive pre-flight failures → fallback (legacy SCM polling) engages.
    verify(pullRequestPollingScheduler, atLeastOnce()).setSuppressed(false);
  }

  @Test
  public void runPollCycleWithErrorBoundary_swallowsThrowableAndRecordsFailure() {
    // CRITICAL contract: any uncaught throwable from pollOnce must NOT escape the wrapper.
    // ScheduledThreadPoolExecutor.scheduleAtFixedRate cancels the recurring schedule
    // PERMANENTLY on any uncaught throwable — a single Error from a downstream library
    // (LinkageError, NoSuchMethodError, AssertionError, OOM) would silently kill polling
    // for the JVM's entire lifetime. The wrapper must catch Throwable, not RuntimeException,
    // and must call recordFailure() so legacy SCM polling can still re-engage after the
    // threshold while the underlying issue is diagnosed.
    //
    // Drive the throw via a downstream collaborator: when feature gate is open and config
    // is non-null but apiKey decryption returns blank, pollOnce reaches the recordFailure
    // path through a different branch — we want the WRAPPER itself to catch, so we drive
    // the throw through pollEvents (mocked to throw an Error rather than a RuntimeException).
    primeRegistration();
    org.mockito.Mockito.doThrow(new AssertionError("downstream library exploded"))
        .when(relayClient)
        .pollEvents(anyString(), anyInt());

    // Must NOT throw: the whole point of the wrapper.
    service.runPollCycleWithErrorBoundary();
    service.runPollCycleWithErrorBoundary();
    service.runPollCycleWithErrorBoundary();

    // After threshold, legacy polling re-engages — same contract as other failure paths.
    verify(pullRequestPollingScheduler, atLeastOnce()).setSuppressed(false);
  }

  @Test
  public void runPollCycleWithErrorBoundary_swallowsErrorEvenOnFirstCycle() {
    // A single Throwable on the very first cycle must NOT propagate; if it did, the
    // ScheduledFuture would be cancelled and polling would die forever (the bug this catch
    // exists to prevent).
    primeRegistration();
    org.mockito.Mockito.doThrow(new LinkageError("classloader surprise"))
        .when(relayClient)
        .pollEvents(anyString(), anyInt());

    service.runPollCycleWithErrorBoundary();
    // No assertion needed beyond "didn't throw"; if the catch were RuntimeException-only the
    // LinkageError would escape and JUnit would report the test as errored.
  }

  @Test
  public void pollOnce_noConfiguration_401_isHandledAndCountsAsFailure() {
    // Stuck-state recovery: relay holds a customer record under this license but IQ has
    // no credential to recover (operator-deleted local row, restored-from-backup mismatch,
    // etc.). The 401 branch must NOT crash the cycle and MUST recordFailure() so the
    // legacy SCM polling fallback eventually engages — otherwise users get neither relay
    // events nor legacy PR scans.
    when(relayRegistrationService.isFeatureGateOpen()).thenReturn(true);
    when(relayRegistrationService.getConfiguration()).thenReturn(null);
    org.mockito.Mockito.doThrow(new jakarta.ws.rs.NotAuthorizedException(
        jakarta.ws.rs.core.Response.status(401).build(),
        "license already in use"))
        .when(relayRegistrationService)
        .registerOnDemand();

    service.pollOnce();
    service.pollOnce();
    service.pollOnce();

    // Same fallback contract as the generic-failure test: after threshold, legacy polling resumes.
    verify(pullRequestPollingScheduler, atLeastOnce()).setSuppressed(false);
  }

  @Test
  public void pollOnce_noConfiguration_401_warnsOnceThenDebug() {
    // Anti-spam contract: the operator-actionable WARN is emitted exactly once per stuck-state
    // entry (consecutiveFailures==0). Subsequent cycles that keep hitting the same 401 log at
    // DEBUG so the loop is still observable at trace level without flooding production logs at
    // ~1440 lines/day per stuck tenant. Without this gate, the WARN is unbounded —
    // ensureFallbackPolling(true) only un-suppresses legacy polling, it does NOT cancel the
    // relay polling future, so the cycle keeps firing forever.
    Logger logger = (Logger) LoggerFactory.getLogger(RelayPollingService.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    try {
      when(relayRegistrationService.isFeatureGateOpen()).thenReturn(true);
      when(relayRegistrationService.getConfiguration()).thenReturn(null);
      org.mockito.Mockito.doThrow(new jakarta.ws.rs.NotAuthorizedException(
          jakarta.ws.rs.core.Response.status(401).build(),
          "license already in use"))
          .when(relayRegistrationService)
          .registerOnDemand();

      service.pollOnce();
      service.pollOnce();
      service.pollOnce();
      service.pollOnce();
      service.pollOnce();

      long warnCount = appender.list.stream()
          .filter(e -> e.getLevel() == Level.WARN)
          .filter(e -> e.getFormattedMessage().contains("Pre-flight relay registration rejected (HTTP 401)"))
          .count();
      long debugCount = appender.list.stream()
          .filter(e -> e.getLevel() == Level.DEBUG)
          .filter(e -> e.getFormattedMessage().contains("still rejected (HTTP 401)"))
          .count();

      assertThat(warnCount).as("401 WARN should be emitted exactly once across 5 stuck cycles").isEqualTo(1);
      assertThat(debugCount).as("subsequent stuck cycles should DEBUG, not WARN").isEqualTo(4);
    }
    finally {
      logger.detachAppender(appender);
      appender.stop();
    }
  }

  @Test
  public void pollOnce_drainsEventsPublishesAndAcks() {
    primeRegistration();
    RelayEvent event1 = inboundEvent("e-1", "rh-1");
    RelayEvent event2 = inboundEvent("e-2", "rh-2");
    when(relayClient.pollEvents("plain-encrypted-key", 50))
        .thenReturn(new RelayEventsResponse(List.of(event1, event2)));
    SourceControlEvent mapped1 = new SourceControlEvent().setApplicationId("app-1").forDiscoveredPullRequest();
    SourceControlEvent mapped2 = new SourceControlEvent().setApplicationId("app-2").forUpdatedPullRequest();
    when(relayEventMapper.map(eq(event1), any(), any(), any())).thenReturn(List.of(mapped1));
    when(relayEventMapper.map(eq(event2), any(), any(), any())).thenReturn(List.of(mapped2));
    when(relayClient.ack(anyString(), anyList())).thenReturn(new RelayAckResponse());

    service.pollOnce();

    verify(sourceControlEventPublisher).publishEventBypassingFeatureGate(mapped1);
    verify(sourceControlEventPublisher).publishEventBypassingFeatureGate(mapped2);
    ArgumentCaptor<List<String>> handles = ArgumentCaptor.forClass(List.class);
    verify(relayClient).ack(eq("plain-encrypted-key"), handles.capture());
    assertThat(handles.getValue()).containsExactly("rh-1", "rh-2");
  }

  @Test
  public void pollOnce_githubAppMode_forwardsGithubAppAuthenticationTypeToMapper() {
    // primeRegistration leaves webhookUrl blank → GitHub App mode.
    primeRegistration();
    RelayEvent event = inboundEvent("e-1", "rh-1");
    when(relayClient.pollEvents("plain-encrypted-key", 50))
        .thenReturn(new RelayEventsResponse(List.of(event)));
    when(relayEventMapper.map(eq(event), any(), any(), eq("GITHUB_APP"))).thenReturn(Collections.emptyList());
    when(relayClient.ack(anyString(), anyList())).thenReturn(new RelayAckResponse());

    service.pollOnce();

    verify(relayEventMapper).map(eq(event), any(), any(), eq("GITHUB_APP"));
  }

  @Test
  public void pollOnce_patMode_forwardsPatAuthenticationTypeToMapper() {
    when(relayRegistrationService.isFeatureGateOpen()).thenReturn(true);
    RelayConfiguration cfg = new RelayConfiguration();
    cfg.setApiKey("encrypted-key");
    cfg.setCustomerId("cust-1");
    cfg.setWebhookUrl("https://relay.example.com/webhook/abc/github");
    when(relayRegistrationService.getConfiguration()).thenReturn(cfg);
    RelayEvent event = inboundEvent("e-1", "rh-1");
    when(relayClient.pollEvents("plain-encrypted-key", 50))
        .thenReturn(new RelayEventsResponse(List.of(event)));
    when(relayEventMapper.map(eq(event), any(), any(), eq("PAT"))).thenReturn(Collections.emptyList());
    when(relayClient.ack(anyString(), anyList())).thenReturn(new RelayAckResponse());

    service.pollOnce();

    verify(relayEventMapper).map(eq(event), any(), any(), eq("PAT"));
  }

  @Test
  public void pollOnce_firstSuccess_suppressesLegacyPollingFromColdStart() {
    primeRegistration();
    when(relayClient.pollEvents("plain-encrypted-key", 50))
        .thenReturn(new RelayEventsResponse(Collections.emptyList()));

    service.pollOnce();

    verify(pullRequestPollingScheduler).setSuppressed(true);
  }

  @Test
  public void pollOnce_respectsConfiguredMaxEvents() {
    service = new RelayPollingService(relayClient, relayRegistrationService, gitHubAppDAO, gitHubAppRelayLinker,
        relayEventMapper, sourceControlEventPublisher, pullRequestPollingScheduler, passwordHandler, shutdownHandler,
        scmNodeProcessor, 0, 60, 7, 3, 1);
    service.disableSchedulingForTesting = true;
    primeRegistration();
    when(relayClient.pollEvents("plain-encrypted-key", 7)).thenReturn(new RelayEventsResponse(Collections.emptyList()));

    service.pollOnce();

    verify(relayClient).pollEvents("plain-encrypted-key", 7);
  }

  @Test
  public void pollOnce_emptyResponse_skipsAck() {
    primeRegistration();
    when(relayClient.pollEvents(anyString(), anyInt()))
        .thenReturn(new RelayEventsResponse(Collections.emptyList()));

    service.pollOnce();

    verify(relayClient, never()).ack(anyString(), anyList());
  }

  @Test
  public void pollOnce_publishingExceptionStillAcksHandle() {
    primeRegistration();
    RelayEvent event = inboundEvent("e-1", "rh-1");
    when(relayClient.pollEvents(anyString(), anyInt())).thenReturn(new RelayEventsResponse(List.of(event)));
    when(relayEventMapper.map(eq(event), any(), any(), any()))
        .thenReturn(List.of(new SourceControlEvent().setApplicationId("app-1").forDiscoveredPullRequest()));
    org.mockito.Mockito.doThrow(new RuntimeException("boom"))
        .when(sourceControlEventPublisher)
        .publishEventBypassingFeatureGate(any());
    when(relayClient.ack(anyString(), anyList())).thenReturn(new RelayAckResponse());

    service.pollOnce();

    verify(relayClient).ack(eq("plain-encrypted-key"), eq(List.of("rh-1")));
  }

  @Test
  public void pollOnce_relayFailureBelowThreshold_doesNotActivateFallback() {
    primeRegistration();
    when(relayClient.pollEvents(anyString(), anyInt())).thenThrow(new BadGatewayException("relay down"));

    service.pollOnce();
    service.pollOnce();

    // 2 failures < threshold (3): legacy polling must be left completely untouched in
    // either direction. setSuppressed(false) would prematurely activate fallback;
    // setSuppressed(true) would silently disable legacy polling while the relay is
    // failing. Asserting anyBoolean() catches both regressions.
    verify(pullRequestPollingScheduler, never()).setSuppressed(anyBoolean());
  }

  @Test
  public void pollOnce_consecutiveFailuresActivateFallback() {
    primeRegistration();
    when(relayClient.pollEvents(anyString(), anyInt())).thenThrow(new BadGatewayException("relay down"));

    service.pollOnce();
    service.pollOnce();
    service.pollOnce();

    verify(pullRequestPollingScheduler, atLeastOnce()).setSuppressed(false);
  }

  @Test
  public void pollOnce_recoveryAfterFallback_resumesRelayPolling() {
    primeRegistration();
    when(relayClient.pollEvents(anyString(), anyInt()))
        .thenThrow(new BadGatewayException("down"))
        .thenThrow(new BadGatewayException("down"))
        .thenThrow(new BadGatewayException("down"))
        .thenReturn(new RelayEventsResponse(Collections.emptyList()));

    service.pollOnce();
    service.pollOnce();
    service.pollOnce();
    // fallback activated by now
    service.pollOnce();

    verify(pullRequestPollingScheduler, atLeastOnce()).setSuppressed(false);
    verify(pullRequestPollingScheduler, atLeastOnce()).setSuppressed(true);
  }

  @Test
  public void pollOnce_ackFailureCountsAsFailureForFallback() {
    primeRegistration();
    RelayEvent event = inboundEvent("e-1", "rh-1");
    when(relayClient.pollEvents(anyString(), anyInt()))
        .thenReturn(new RelayEventsResponse(List.of(event)));
    when(relayEventMapper.map(any(), any(), any(), any())).thenReturn(Collections.emptyList());
    when(relayClient.ack(anyString(), anyList())).thenThrow(new BadGatewayException("ack failed"));

    service.pollOnce();
    service.pollOnce();
    service.pollOnce();

    verify(pullRequestPollingScheduler, atLeastOnce()).setSuppressed(false);
  }

  @Test
  public void pollOnce_partialAckFailures_areLoggedAndPollerContinues() {
    primeRegistration();
    RelayEvent event = inboundEvent("e-1", "rh-1");
    when(relayClient.pollEvents(anyString(), anyInt()))
        .thenReturn(new RelayEventsResponse(List.of(event)));
    when(relayEventMapper.map(any(), any(), any(), any())).thenReturn(Collections.emptyList());
    RelayAckResponse partial = new RelayAckResponse();
    partial.setAcknowledged(Collections.emptyList());
    partial.setFailed(List.of("rh-1"));
    when(relayClient.ack(anyString(), anyList())).thenReturn(partial);

    service.pollOnce();

    // No exception; success path still recorded.
    verify(relayClient).ack(anyString(), anyList());
  }

  @Test
  public void register_skipsWhenScmNodeShouldNotRun() {
    when(scmNodeProcessor.shouldRun()).thenReturn(false);

    service.register();

    verify(relayClient, never()).pollEvents(anyString(), anyInt());
  }

  @Test
  public void deregister_whenScmNodeShouldNotRun_stillRestoresLegacySuppression() {
    when(scmNodeProcessor.shouldRun()).thenReturn(false);

    service.deregister();

    // Non-SCM nodes never started a poller, so stopPolling() is skipped — but if a prior
    // poll cycle (when shouldRun() was still true) left the tenant in suppressed=true,
    // deregister() is the last chance to clear it. Always restoring on deregister keeps
    // legacy SCM polling from being silently disabled across a node-role change.
    verify(pullRequestPollingScheduler).setSuppressed(false);
    verify(pullRequestPollingScheduler, never()).setSuppressed(true);
  }

  @Test
  public void pollOnce_drainsMultiplePagesUntilQueueEmpty() {
    // Allow up to 3 drain iterations.
    service = new RelayPollingService(relayClient, relayRegistrationService, gitHubAppDAO, gitHubAppRelayLinker,
        relayEventMapper, sourceControlEventPublisher, pullRequestPollingScheduler, passwordHandler, shutdownHandler,
        scmNodeProcessor, 0, 60, 2, 3, 3);
    service.disableSchedulingForTesting = true;
    primeRegistration();

    RelayEvent e1 = inboundEvent("e-1", "rh-1");
    RelayEvent e2 = inboundEvent("e-2", "rh-2");
    RelayEvent e3 = inboundEvent("e-3", "rh-3");
    when(relayClient.pollEvents("plain-encrypted-key", 2))
        .thenReturn(new RelayEventsResponse(List.of(e1, e2))) // full page
        .thenReturn(new RelayEventsResponse(List.of(e3))) // partial → stop
        .thenReturn(new RelayEventsResponse(Collections.emptyList()));
    when(relayEventMapper.map(any(), any(), any(), any())).thenReturn(Collections.emptyList());
    when(relayClient.ack(anyString(), anyList())).thenReturn(new RelayAckResponse());

    service.pollOnce();

    verify(relayClient, atLeast(2)).pollEvents("plain-encrypted-key", 2);
    // Two ack calls (one per page).
    verify(relayClient, atLeast(2)).ack(eq("plain-encrypted-key"), anyList());
  }

  @Test
  public void pollOnce_acksInChunksOf100Handles() {
    primeRegistration();
    java.util.List<RelayEvent> manyEvents = new java.util.ArrayList<>();
    for (int i = 0; i < 150; i++) {
      manyEvents.add(inboundEvent("e-" + i, "rh-" + i));
    }
    when(relayClient.pollEvents(anyString(), anyInt())).thenReturn(new RelayEventsResponse(manyEvents));
    when(relayEventMapper.map(any(), any(), any(), any())).thenReturn(Collections.emptyList());
    when(relayClient.ack(anyString(), anyList())).thenReturn(new RelayAckResponse());

    service.pollOnce();

    ArgumentCaptor<List<String>> handles = ArgumentCaptor.forClass(List.class);
    verify(relayClient, atLeast(2)).ack(eq("plain-encrypted-key"), handles.capture());
    for (List<String> chunk : handles.getAllValues()) {
      assertThat(chunk.size()).isLessThanOrEqualTo(RelayPollingService.ACK_BATCH_LIMIT);
    }
  }

  @Test
  public void pollOnce_decryptionFailure_recordsFailure() {
    primeRegistration();
    when(passwordHandler.decryptPassword(anyString())).thenThrow(new RuntimeException("key material lost"));

    service.pollOnce();
    service.pollOnce();
    service.pollOnce();

    // 3 consecutive failures → fallback engages.
    verify(pullRequestPollingScheduler, atLeastOnce()).setSuppressed(false);
  }

  @Test
  public void deregister_restoresLegacyPolling() {
    when(scmNodeProcessor.shouldRun()).thenReturn(true);

    service.deregister();

    // Tenant left in suppressed=true at deregister time would otherwise lose discovery on
    // re-register until the next relay poll tick.
    verify(pullRequestPollingScheduler).setSuppressed(false);
  }

  @Test
  public void pollOnce_retryLoop_invokesLinkerForUnregisteredAndError() {
    primeRegistration();
    when(relayClient.pollEvents(anyString(), anyInt()))
        .thenReturn(new RelayEventsResponse(Collections.emptyList()));
    com.sonatype.insight.brain.model.githubapp.GitHubApp unreg = appWithState("app-1", 100L,
        com.sonatype.insight.brain.model.githubapp.RelayLinkState.UNREGISTERED, 0);
    com.sonatype.insight.brain.model.githubapp.GitHubApp errored = appWithState("app-2", 200L,
        com.sonatype.insight.brain.model.githubapp.RelayLinkState.ERROR, 3);
    when(gitHubAppDAO.getActiveByRelayLinkState(java.util.Set.of(
        com.sonatype.insight.brain.model.githubapp.RelayLinkState.UNREGISTERED,
        com.sonatype.insight.brain.model.githubapp.RelayLinkState.ERROR)))
            .thenReturn(java.util.List.of(unreg, errored));

    service.pollOnce();

    verify(gitHubAppRelayLinker).tryRegister(unreg);
    verify(gitHubAppRelayLinker).tryRegister(errored);
  }

  @Test
  public void pollOnce_retryLoop_respectsBudget() {
    primeRegistration();
    when(relayClient.pollEvents(anyString(), anyInt()))
        .thenReturn(new RelayEventsResponse(Collections.emptyList()));
    java.util.List<com.sonatype.insight.brain.model.githubapp.GitHubApp> tooMany = new java.util.ArrayList<>();
    for (int i = 0; i < RelayPollingService.MAX_RELAY_LINK_RETRIES_PER_CYCLE + 5; i++) {
      tooMany.add(appWithState("app-" + i, 100L + i,
          com.sonatype.insight.brain.model.githubapp.RelayLinkState.UNREGISTERED, 0));
    }
    when(gitHubAppDAO.getActiveByRelayLinkState(any())).thenReturn(tooMany);

    service.pollOnce();

    // Bounded so a backlog cannot starve event polling on a tenant with many Apps.
    verify(gitHubAppRelayLinker, org.mockito.Mockito.times(RelayPollingService.MAX_RELAY_LINK_RETRIES_PER_CYCLE))
        .tryRegister(any());
  }

  @Test
  public void pollOnce_retryLoop_daoFailureIsLoggedAndPollContinues() {
    primeRegistration();
    when(gitHubAppDAO.getActiveByRelayLinkState(any())).thenThrow(new RuntimeException("db down"));
    when(relayClient.pollEvents(anyString(), anyInt()))
        .thenReturn(new RelayEventsResponse(Collections.emptyList()));

    service.pollOnce();

    // The poll itself still happened; the retry-loop DAO failure does not abort the cycle.
    verify(relayClient).pollEvents(anyString(), anyInt());
    verify(gitHubAppRelayLinker, never()).tryRegister(any());
  }

  @Test
  public void pollOnce_retryLoop_linkerExceptionDoesNotStopOtherRetries() {
    primeRegistration();
    when(relayClient.pollEvents(anyString(), anyInt()))
        .thenReturn(new RelayEventsResponse(Collections.emptyList()));
    com.sonatype.insight.brain.model.githubapp.GitHubApp first = appWithState("app-1", 100L,
        com.sonatype.insight.brain.model.githubapp.RelayLinkState.ERROR, 1);
    com.sonatype.insight.brain.model.githubapp.GitHubApp second = appWithState("app-2", 200L,
        com.sonatype.insight.brain.model.githubapp.RelayLinkState.ERROR, 1);
    when(gitHubAppDAO.getActiveByRelayLinkState(any())).thenReturn(java.util.List.of(first, second));
    when(gitHubAppRelayLinker.tryRegister(first)).thenThrow(new RuntimeException("unexpected"));

    service.pollOnce();

    // Defensive catch in retry loop ensures one bad row does not block the rest.
    verify(gitHubAppRelayLinker).tryRegister(second);
  }

  private static com.sonatype.insight.brain.model.githubapp.GitHubApp appWithState(
      String id,
      long installationId,
      String state,
      int attempts)
  {
    com.sonatype.insight.brain.model.githubapp.GitHubApp app =
        new com.sonatype.insight.brain.model.githubapp.GitHubApp();
    app.setId(id);
    app.setInstallationId(installationId);
    app.setRelayLinkState(state);
    app.setRelayLinkAttempts(attempts);
    app.setActive(true);
    return app;
  }

  private void primeRegistration() {
    when(relayRegistrationService.isFeatureGateOpen()).thenReturn(true);
    RelayConfiguration cfg = new RelayConfiguration();
    cfg.setApiKey("encrypted-key");
    cfg.setCustomerId("cust-1");
    when(relayRegistrationService.getConfiguration()).thenReturn(cfg);
  }

  private static RelayEvent inboundEvent(String eventId, String receiptHandle) {
    RelayEvent event = new RelayEvent();
    event.setEventId(eventId);
    event.setProvider("github");
    event.setEventType(RelayEvent.TYPE_PULL_REQUEST_OPENED);
    event.setRepositoryUrl("https://github.com/o/r");
    event.setReceiptHandle(receiptHandle);
    return event;
  }

}
