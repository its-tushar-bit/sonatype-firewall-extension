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
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.ProxyRepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.policy.ConstraintFactDTO;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.test.LogOutput;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.stream.Collectors.toCollection;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.awaitility.Awaitility.await;

/**
 * IQ Server on PostgreSQL — port of the legacy
 * {@code ApiComponentReleaseQuarantineResourceAuditTest} to the reused-server variant pattern. No
 * base class: audit-log capture/assertion helpers ({@code AuditTestSupport} in the legacy base
 * chain) are inlined here since {@link IqTestContext} does not expose audit-log support.
 */
@IqPostgresTest
class IqPostgresApiComponentReleaseQuarantineResourceAuditTest
{
  private static final ObjectMapper JSON = new ObjectMapper();

  private static final String COMPONENT_HASH = "hash";

  private static final String PATHNAME = "pathname";

  private static final PackageUrlIdentifier PACKAGE_URL_IDENTIFIER =
      new PackageUrlIdentifier("pkg:maven/g1/a1@v1?type=e1");

  private static final String REPO_MAN_INSTANCE_ID = "repoManagerInstanceId";

  private static final String REPO_PUBLIC_ID = "repoPublicId";

  // Injected by IqPostgresServerExtension: the extension owns the shared, reused server.
  private IqTestContext ctx;

  private final TestLogOutput logOutput = new TestLogOutput(AuditRecorder.BASE_LOGGER_NAME);

  private PolicyWaiverDAO policyWaiverDAO;

  private ProxyRepositoryPolicyViolationDAO proxyRepositoryPolicyViolationDAO;

  private User unauthorizedUserEntity;

  @BeforeEach
  void setUp() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUserEntity = ctx.tempEntity().newUser();

    policyWaiverDAO = ctx.lookup(PolicyWaiverDAO.class);
    proxyRepositoryPolicyViolationDAO = ctx.lookup(ProxyRepositoryPolicyViolationDAO.class);
  }

  @AfterEach
  void tearDown() {
    logOutput.tearDown();
  }

  @Test
  void testReleaseQuarantineWithoutReEval() throws Exception {
    Date quarantineTime = new Date();
    Repository repository = ctx.tempEntity().newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "maven2");

    ProxyRepositoryComponent proxyRepositoryComponent = ctx.tempEntity()
        .newRepositoryComponent(repository.getId(), MatchState.EXACT, PATHNAME, COMPONENT_HASH,
            PACKAGE_URL_IDENTIFIER.ensureCompleteIdentifier(), quarantineTime, quarantineTime);

    Policy policy1 = ctx.tempEntity().newPolicy(repository.getParentOwnerId());
    Policy policy2 = ctx.tempEntity().newPolicy(repository.getParentOwnerId());

    ProxyRepositoryPolicyViolation repositoryPolicyViolation1 =
        createRepositoryPolicyViolation(proxyRepositoryComponent, false, 10, policy1, Action.ID_FAIL);

    ProxyRepositoryPolicyViolation repositoryPolicyViolation2 =
        createRepositoryPolicyViolation(proxyRepositoryComponent, false, 10, policy2, Action.ID_FAIL);

    releaseQuarantineRequest(proxyRepositoryComponent.getId()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.RELEASE_QUARANTINE, null);
    assertRepositoryData(auditDTO, repository);
    assertUnquarantineData(auditDTO, proxyRepositoryComponent);

    PolicyWaiver policyWaiver1 = getSavedPolicyWaiver(repositoryPolicyViolation1.getId());
    PolicyWaiver policyWaiver2 = getSavedPolicyWaiver(repositoryPolicyViolation2.getId());

    List<AuditDTO> waiverAuditDTOs = assertAuditLogs(AuditEvent.CREATE_WAIVER, 2, null);
    assertPolicyWaiverData(waiverAuditDTOs.get(0), policy1, policyWaiver1, repository);
    assertPolicyWaiverData(waiverAuditDTOs.get(1), policy2, policyWaiver2, repository);
  }

  private PolicyWaiver getSavedPolicyWaiver(String repositoryPolicyViolationId) {
    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        proxyRepositoryPolicyViolationDAO.getById(repositoryPolicyViolationId);
    return policyWaiverDAO.getByIdNotNull(proxyRepositoryPolicyViolation.getPolicyWaiverId());
  }

  private ProxyRepositoryPolicyViolation createRepositoryPolicyViolation(
      final ProxyRepositoryComponent proxyRepositoryComponent,
      final boolean waived,
      final int threatLevel,
      final Policy policy,
      final String action)
  {
    return ctx.tempEntity()
        .newRepositoryPolicyViolation(proxyRepositoryComponent.getRepositoryId(), threatLevel,
            proxyRepositoryComponent.getPathname(), waived, action, policy.getId(), policy.getName(),
            proxyRepositoryComponent.getComponentIdentifier());
  }

  @Test
  void testReleaseQuarantineWithoutReEval_NoWaivers() {
    assertThatExceptionOfType(ConditionTimeoutException.class).isThrownBy(() -> {
      Date quarantineTime = new Date();
      Repository repository = ctx.tempEntity().newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "maven2");

      ProxyRepositoryComponent proxyRepositoryComponent = ctx.tempEntity()
          .newRepositoryComponent(repository.getId(), MatchState.EXACT, PATHNAME, COMPONENT_HASH,
              PACKAGE_URL_IDENTIFIER.ensureCompleteIdentifier(), quarantineTime, quarantineTime);

      releaseQuarantineRequest(proxyRepositoryComponent.getId()).post();

      AuditDTO auditDTO = assertAuditLog(AuditEvent.RELEASE_QUARANTINE, null);
      assertRepositoryData(auditDTO, repository);
      assertUnquarantineData(auditDTO, proxyRepositoryComponent);

      // make sure there is no waiver auditing attempted
      assertAuditLog(AuditEvent.CREATE_WAIVER, null);
    });
  }

  @Test
  void testReleaseQuarantineWithoutReEval_Unauthorized() throws Exception {
    Date quarantineTime = new Date();
    Repository repository = ctx.tempEntity().newRepository();
    ProxyRepositoryComponent proxyRepositoryComponent = ctx.tempEntity()
        .newRepositoryComponent(repository.getId(), MatchState.EXACT, PATHNAME, COMPONENT_HASH,
            PACKAGE_URL_IDENTIFIER.ensureCompleteIdentifier(), quarantineTime, quarantineTime);

    releaseQuarantineRequest(proxyRepositoryComponent.getId()).with(unauthorizedUser()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.RELEASE_QUARANTINE, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }

  private HttpRequest releaseQuarantineRequest(String quarantineId) {
    return ctx.restRequest()
        .path(PublicApiPaths.COMPONENT_QUARANTINE_RELEASE_PATH_V2)
        .parameter(quarantineId)
        .body("waiver comment", MediaType.TEXT_PLAIN);
  }

  private void assertUnquarantineData(AuditDTO auditDTO, ProxyRepositoryComponent proxyRepositoryComponent) {
    assertCustomData(auditDTO, "componentHash", proxyRepositoryComponent.getHash());
    assertCustomData(auditDTO, "componentPathname", proxyRepositoryComponent.getPathname());
  }

  private void assertPolicyWaiverData(
      AuditDTO auditDTO,
      Policy policy,
      PolicyWaiver policyWaiver,
      Repository repository)
  {
    assertRepositoryData(auditDTO, repository);
    assertCustomData(auditDTO, "policyId", policy.getId());
    assertCustomData(auditDTO, "policyName", policy.getName());
    assertCustomData(auditDTO, "policyWaiverId", policyWaiver.getId());
    assertCustomData(auditDTO, "comment", policyWaiver.getComment());
    assertCustomData(auditDTO, "componentHash", policyWaiver.getHash());
    if (policyWaiver.getConstraintFacts() == null) {
      assertCustomData(auditDTO, "policyConstraints", null);
    }
    else {
      assertCustomObject(auditDTO, "policyConstraints",
          policyWaiver.getConstraintFacts().stream().map(ConstraintFactDTO::new).collect(Collectors.toList()));
    }
  }

  // --- inlined AuditTestSupport / AbstractAuditTest helpers (not exposed by IqTestContext) --------

  private Consumer<HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUserEntity);
  }

  private List<AuditDTO> awaitLogEntries(AuditEvent auditEvent, int count) {
    await("Expect audit event " + auditEvent).atMost(10, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(getLogEntries(auditEvent)).hasSizeGreaterThanOrEqualTo(count));
    return getLogEntries(auditEvent);
  }

  private List<AuditDTO> getLogEntries(AuditEvent auditEvent) {
    return logOutput.getInfoMessages(AuditRecorder.toLoggerName(auditEvent.getDomain()))
        .stream()
        .map(IqPostgresApiComponentReleaseQuarantineResourceAuditTest::parseAuditLog)
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
    String username = "unauthorized".equals(error) ? unauthorizedUserEntity.getUsername() : User.ADMIN_USERNAME;
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

  private void assertCustomObject(AuditDTO auditDTO, String key, Object pojo) {
    if (pojo instanceof java.util.Collection<?>) {
      assertCustomData(auditDTO, key,
          ((java.util.Collection<?>) pojo).stream()
              .map(element -> JSON.convertValue(element, Map.class))
              .collect(Collectors.toList()));
    }
    else {
      assertCustomData(auditDTO, key, JSON.convertValue(pojo, Map.class));
    }
  }

  private void assertRepositoryData(AuditDTO auditDTO, Repository repository) {
    assertCustomData(auditDTO, "repositoryId", repository.getId());
    assertCustomData(auditDTO, "repositoryPublicId", repository.getPublicId());
    assertCustomData(auditDTO, "format", repository.getFormat());
    assertCustomData(auditDTO, "type", repository.getRepositoryType().name());
    assertCustomData(auditDTO, "auditEnabled", repository.isAuditEnabled());
    assertCustomData(auditDTO, "quarantineEnabled", repository.isQuarantineEnabled());
    assertCustomData(auditDTO, "policyCompliantComponentSelectionEnabled",
        repository.isPolicyCompliantComponentSelectionEnabled());
    assertCustomData(auditDTO, "namespaceConfusionProtectionEnabled",
        repository.isNamespaceConfusionProtectionEnabled());
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
