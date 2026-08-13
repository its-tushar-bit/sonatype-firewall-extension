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

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiComponentsInQuarantineReportingResource;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.test.LogOutput;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.stream.Collectors.toCollection;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * IQ Server on PostgreSQL — port of the legacy {@code ApiComponentsInQuarantineReportingResourceAuditTest} to the
 * reused-server variant pattern. No base class: audit-log capture/assertion helpers ({@code AuditTestSupport} in
 * the legacy base chain) are inlined here since {@link IqTestContext} does not expose audit-log support.
 */
@IqPostgresTest
class IqPostgresApiComponentsInQuarantineReportingResourceAuditTest
{
  private static final ObjectMapper JSON = new ObjectMapper();

  // Injected by IqPostgresServerExtension: the extension owns the shared, reused server.
  private IqTestContext ctx;

  private final TestLogOutput logOutput = new TestLogOutput(AuditRecorder.BASE_LOGGER_NAME);

  private Repository repo1;

  private Repository repo2;

  @BeforeEach
  void setUp() {
    logOutput.before();
    logOutput.clear();

    repo1 = ctx.tempEntity().newRepository("rm1", "r1", "maven2");
    repo2 = ctx.tempEntity().newRepository("rm2", "r2", "maven3");
  }

  @AfterEach
  void tearDown() {
    logOutput.tearDown();
  }

  private HttpRequest restRequest() {
    return ctx.restRequest()
        .path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2 + ApiComponentsInQuarantineReportingResource.PATH);
  }

  @Test
  void testGetComponentsInQuarantine_WithNoComponents() throws Exception {
    restRequest().get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_QUARANTINED_COMPONENTS, null);
    assertCustomData(auditDTO, "numberOfQuarantinedComponents", 0);
  }

  @Test
  void testGetComponentsInQuarantine_WithQuarantinedComponents() throws Exception {
    createRepositoryComponent(repo1, "pathname1", true, false);
    createRepositoryComponent(repo2, "pathname2", true, false);

    restRequest().get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_QUARANTINED_COMPONENTS, null);
    assertCustomData(auditDTO, "numberOfQuarantinedComponents", 2);
  }

  @Test
  void testGetComponentsInQuarantine_WithQuarantinedAndNonQuarantinedComponents() throws Exception {
    // quarantined components
    createRepositoryComponent(repo1, "pathname1", true, false);
    createRepositoryComponent(repo1, "pathname2", true, false);
    createRepositoryComponent(repo2, "pathname3", true, false);

    // non-quarantined components
    createRepositoryComponent(repo1, "pathname4", false, false);
    createRepositoryComponent(repo2, "pathname5", false, false);
    createRepositoryComponent(repo2, "pathname6", false, false);

    // component released from quarantine
    createRepositoryComponent(repo2, "pathname7", true, true);

    restRequest().get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_QUARANTINED_COMPONENTS, null);
    assertCustomData(auditDTO, "numberOfQuarantinedComponents", 3);
  }

  @Test
  void testGetComponentsInQuarantine_WithNoQuarantinedComponents() throws Exception {
    createRepositoryComponent(repo1, "pathname1", false, false);
    createRepositoryComponent(repo2, "pathname2", false, false);

    restRequest().get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_QUARANTINED_COMPONENTS, null);
    assertCustomData(auditDTO, "numberOfQuarantinedComponents", 0);
  }

  private void createRepositoryComponent(
      Repository repo,
      String pathname,
      boolean isQuarantined,
      boolean isReleasedFromQuarantine)
  {
    ProxyRepositoryComponent proxyRepositoryComponent = new ProxyRepositoryComponent();
    proxyRepositoryComponent.setRepositoryId(repo.getId());
    proxyRepositoryComponent.setPathname(pathname);
    proxyRepositoryComponent.setTime(new Date());
    proxyRepositoryComponent.setHash("hash");
    proxyRepositoryComponent.setComponentIdentifier(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"));
    proxyRepositoryComponent.setMatchStateId(MatchState.EXACT.getId());
    proxyRepositoryComponent.setIdentificationSourceId(IdentificationSource.SONATYPE.getId());
    proxyRepositoryComponent.setLastEvaluationTime(new Date());
    if (isQuarantined) {
      proxyRepositoryComponent.setQuarantineTime(new Date());
    }
    if (isReleasedFromQuarantine) {
      proxyRepositoryComponent.setUnquarantineTimeForManualRelease(new Date());
    }
    ctx.tempEntity().newRepositoryComponent(proxyRepositoryComponent);
  }

  // --- inlined AuditTestSupport / AbstractAuditTest helpers (not exposed by IqTestContext) --------

  private List<AuditDTO> awaitLogEntries(AuditEvent auditEvent, int count) {
    await("Expect audit event " + auditEvent).atMost(10, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(getLogEntries(auditEvent)).hasSizeGreaterThanOrEqualTo(count));
    return getLogEntries(auditEvent);
  }

  private List<AuditDTO> getLogEntries(AuditEvent auditEvent) {
    return logOutput.getInfoMessages(AuditRecorder.toLoggerName(auditEvent.getDomain()))
        .stream()
        .map(IqPostgresApiComponentsInQuarantineReportingResourceAuditTest::parseAuditLog)
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

  private AuditDTO assertAuditLog(AuditEvent auditEvent, String error) {
    return assertAuditLogs(auditEvent, 1, error).get(0);
  }

  private List<AuditDTO> assertAuditLogs(AuditEvent auditEvent, int number, String error) {
    List<AuditDTO> auditDTOs = awaitLogEntries(auditEvent, number);
    auditDTOs.forEach(auditDTO -> assertStandardData(auditDTO, auditEvent, error));
    return auditDTOs;
  }

  private void assertStandardData(AuditDTO auditDTO, AuditEvent auditEvent, String error) {
    String username = User.ADMIN_USERNAME;
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

  private void assertCustomData(AuditDTO auditDTO, String key, Object value) {
    if (value == null) {
      assertThat(auditDTO.data).doesNotContainKey(key);
    }
    else {
      assertThat(auditDTO.data).containsEntry(key, value);
    }
  }

  /** Exposes {@link LogOutput#after()} (protected, {@code ExternalResource}) for {@code @AfterEach} teardown. */
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
