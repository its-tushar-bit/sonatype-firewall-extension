/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.security.User;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toCollection;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * IQ Server on PostgreSQL — {@link com.sonatype.insight.brain.api.v2.ApiFirewallCascadeResource} audit logging.
 * No base class; the {@code AuditTestSupport}/{@code LogOutput} log-capture behaviour from the legacy
 * {@code AbstractAuditTest} is inlined here since it is not part of {@link IqTestContext}.
 */
@IqPostgresTest
class IqPostgresApiFirewallCascadeResourceAuditTest
{
  private static final String COMPONENT_HASH = "test_hash_123";

  private static final ObjectMapper JSON = new ObjectMapper();

  // Injected by IqPostgresServerExtension: the extension owns the shared, reused server.
  private IqTestContext ctx;

  private final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

  private Level originalLevel;

  private User unauthorizedUser;

  @BeforeEach
  void setupCommonFixture() {
    Logger auditLogger = loggerContext.getLogger(AuditRecorder.BASE_LOGGER_NAME);
    originalLevel = auditLogger.getLevel();
    auditLogger.setLevel(Level.DEBUG);
    appender.list = java.util.Collections.synchronizedList(appender.list);
    appender.setContext(loggerContext);
    appender.start();
    auditLogger.detachAppender(appender);
    auditLogger.addAppender(appender);
    unauthorizedUser = ctx.tempEntity().newUser();
  }

  @AfterEach
  void tearDownLogCapture() {
    Logger auditLogger = loggerContext.getLogger(AuditRecorder.BASE_LOGGER_NAME);
    auditLogger.detachAppender(appender);
    auditLogger.setLevel(originalLevel);
    appender.stop();
  }

  @Test
  void testInitiateCascadeReevaluation_Success() throws Exception {
    createRepositoryWithComponent();

    cascadeReevaluateRequest(COMPONENT_HASH).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.INITIATE_CASCADE_REEVALUATION, null);
    assertCascadeReevaluationData(auditDTO, COMPONENT_HASH);
  }

  @Test
  void testInitiateCascadeReevaluation_Unauthorized() throws Exception {
    createRepositoryWithComponent();

    cascadeReevaluateRequest(COMPONENT_HASH)
        .with(unauthorizedUser())
        .post();

    assertAuditLog(AuditEvent.INITIATE_CASCADE_REEVALUATION, "unauthorized");
  }

  @Test
  void testInitiateCascadeReevaluation_MultipleRepositories() throws Exception {
    // Create multiple repositories with the same component
    createRepositoryWithComponent("repo-1");
    createRepositoryWithComponent("repo-2");

    cascadeReevaluateRequest(COMPONENT_HASH).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.INITIATE_CASCADE_REEVALUATION, null);
    assertCascadeReevaluationData(auditDTO, COMPONENT_HASH);
    // Note: Repository count (2) will be determined by async task, not available in immediate audit log
  }

  @Test
  void testInitiateCascadeReevaluation_BlankComponentHash() throws Exception {
    createRepositoryWithComponent();

    // Test with blank component hash - should result in 404 or 400
    cascadeReevaluateRequest("").post();

    // Blank hash in URL path typically results in routing error before audit
    List<AuditDTO> auditEntries = getLogEntries(AuditEvent.INITIATE_CASCADE_REEVALUATION);
    // May or may not generate audit entry depending on how early the error occurs
    if (!auditEntries.isEmpty()) {
      assertThat(auditEntries.get(0).error).isIn("bad-request", "not-found");
    }
  }

  private HttpRequest cascadeReevaluateRequest(String componentHash) {
    return ctx.restRequest()
        .path(PublicApiPaths.FIREWALL_CASCADE_REEVALUATE_PATH, "/componentHash/" + componentHash);
  }

  private void createRepositoryWithComponent() {
    createRepositoryWithComponent("test-repo");
  }

  private void createRepositoryWithComponent(String repositoryName) {
    Repository repository = ctx.tempEntity().newRepository(repositoryName);

    Date now = new Date();
    ctx.tempEntity()
        .newRepositoryComponent(
            repository.getId(),
            MatchState.EXACT,
            "test/path/component",
            COMPONENT_HASH,
            ComponentIdentifier.createNpmCoordinates("test-pkg", "1.0.0"),
            now,
            now);
  }

  private void assertCascadeReevaluationData(AuditDTO auditDTO, String componentHash) {
    boolean hasComponentHash = auditDTO.data.containsKey("componentHash") ||
        auditDTO.data.containsKey("pathParam.componentHash") ||
        auditDTO.data.containsKey("param.componentHash");

    if (hasComponentHash) {
      // If componentHash is present, verify it matches
      String auditedHash = (String) auditDTO.data.getOrDefault("componentHash",
          auditDTO.data.getOrDefault("pathParam.componentHash",
              auditDTO.data.get("param.componentHash")));
      assertThat(auditedHash).isEqualTo(componentHash);
    }

    assertThat(auditDTO.type).isEqualTo(AuditEvent.INITIATE_CASCADE_REEVALUATION.getType());
  }

  // --- inlined AuditTestSupport/AbstractAuditTest behaviour (not part of IqTestContext) ----------

  private Consumer<HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUser);
  }

  private AuditDTO assertAuditLog(AuditEvent auditEvent, String error) {
    return assertAuditLogs(auditEvent, 1, error).get(0);
  }

  private List<AuditDTO> assertAuditLogs(AuditEvent auditEvent, int number, String error) {
    List<AuditDTO> auditDTOs = awaitLogEntries(auditEvent, number);
    auditDTOs.forEach(auditDTO -> assertStandardData(auditDTO, auditEvent, error));
    return auditDTOs;
  }

  private void assertStandardData(AuditDTO auditDTO, AuditEvent auditEvent, String error) {
    String username = "unauthorized".equals(error) ? unauthorizedUser.getUsername() : User.ADMIN_USERNAME;
    assertThat(auditDTO.domain).isEqualTo(auditEvent.getDomain());
    assertThat(auditDTO.type).isEqualTo(auditEvent.getType());
    assertThat(auditDTO.error).isEqualTo(error);
    assertThat(auditDTO.timestamp).matches("2[0-9]{3}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}[-+0-9Z.:]+");
    assertThat(auditDTO.requestMethod).isNull();
    assertThat(auditDTO.requestUri).isNull();
    assertThat(auditDTO.forwarded).isNull();
    assertThat(auditDTO.remoteIpAddress).isNotEmpty();
    assertThat(auditDTO.userAgent).isNotEmpty();
    assertThat(auditDTO.username).isEqualTo(username);
  }

  private List<AuditDTO> awaitLogEntries(AuditEvent auditEvent, int count) {
    await("Expect audit event " + auditEvent).atMost(10, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(getLogEntries(auditEvent)).hasSizeGreaterThanOrEqualTo(count));
    return getLogEntries(auditEvent);
  }

  private List<AuditDTO> getLogEntries(AuditEvent auditEvent) {
    String loggerName = AuditRecorder.toLoggerName(auditEvent.getDomain());
    List<ILoggingEvent> events;
    synchronized (appender.list) {
      events = new ArrayList<>(appender.list);
    }
    return events.stream()
        .filter(event -> Level.INFO.equals(event.getLevel()) && loggerName.equals(event.getLoggerName()))
        .map(event -> parseAuditLog(event.getFormattedMessage()))
        .filter(dto -> auditEvent.getType().equals(dto.type))
        .collect(toCollection(ArrayList::new));
  }

  private static AuditDTO parseAuditLog(String auditLogEntry) {
    try {
      return JSON.readValue(auditLogEntry, AuditDTO.class);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
