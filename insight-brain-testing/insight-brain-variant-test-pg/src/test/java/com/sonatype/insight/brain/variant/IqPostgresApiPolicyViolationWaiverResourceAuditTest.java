/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.policy.ConstraintFactDTO;
import com.sonatype.insight.test.LogOutput;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.stream.Collectors.toCollection;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * IQ Server on PostgreSQL — port of the legacy {@code ApiPolicyViolationWaiverResourceAuditTest} to
 * the reused-server variant pattern. No base class: audit-log capture/assertion helpers
 * ({@code AuditTestSupport} in the legacy base chain) are inlined here since {@link IqTestContext}
 * does not expose audit-log support.
 */
@IqPostgresTest
class IqPostgresApiPolicyViolationWaiverResourceAuditTest
{
  private static final ObjectMapper JSON = new ObjectMapper();

  // Injected by IqPostgresServerExtension: the extension owns the shared, reused server.
  private IqTestContext ctx;

  private final TestLogOutput logOutput = new TestLogOutput(AuditRecorder.BASE_LOGGER_NAME);

  private PolicyWaiverDAO policyWaiverDAO;

  private User unauthorizedUserEntity;

  private Organization org;

  private Application app;

  private Policy policy;

  private PolicyViolation policyViolation;

  @BeforeEach
  void setUp() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUserEntity = ctx.tempEntity().newUser();

    policyWaiverDAO = ctx.lookup(PolicyWaiverDAO.class);

    org = ctx.tempEntity().newOrganization();
    app = ctx.tempEntity().newApplication(org.getId());
    policy = ctx.tempEntity().newPolicy();

    PolicyEvaluation policyEvaluation =
        ctx.tempEntity().newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan1");
    policyViolation = ctx.tempEntity().newPolicyViolation(policyEvaluation, policy);
  }

  @AfterEach
  void tearDown() {
    logOutput.tearDown();
  }

  @Test
  void testAddPolicyWaiver_Application() throws Exception {
    restRequest(policyViolation.getId(), OwnerType.APPLICATION).body("waiver comment", MediaType.TEXT_PLAIN).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_WAIVER, null);
    assertPolicyWaiverData(auditDTO);
    assertApplicationData(auditDTO, app);
  }

  @Test
  void testAddPolicyWaiver_Organization() throws Exception {
    restRequest(policyViolation.getId(), OwnerType.ORGANIZATION).body("waiver comment", MediaType.TEXT_PLAIN).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_WAIVER, null);
    assertPolicyWaiverData(auditDTO);
    assertOrganizationData(auditDTO, org);
  }

  @Test
  void testAddPolicyWaiver_Application_Unauthorized() throws Exception {
    restRequest(policyViolation.getId(), OwnerType.APPLICATION).with(unauthorizedUser())
        .body("waiver comment", MediaType.TEXT_PLAIN)
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_WAIVER, "unauthorized");
    assertApplicationData(auditDTO, app);
  }

  @Test
  void testAddPolicyWaiver_Organization_Unauthorized() throws Exception {
    restRequest(policyViolation.getId(), OwnerType.ORGANIZATION).with(unauthorizedUser())
        .body("waiver comment", MediaType.TEXT_PLAIN)
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_WAIVER, "unauthorized");
    assertOrganizationData(auditDTO, org);
  }

  private void assertPolicyWaiverData(AuditDTO auditDTO) {
    PolicyWaiver policyWaiver = policyWaiverDAO.getByIdNotNull((String) auditDTO.data.get("policyWaiverId"));
    assertCustomData(auditDTO, "policyId", policyWaiver.getPolicyId());
    assertCustomData(auditDTO, "policyName", policy.getName());
    assertCustomData(auditDTO, "policyWaiverId", policyWaiver.getId());
    assertCustomData(auditDTO, "comment", policyWaiver.getComment());
    assertCustomData(auditDTO, "componentHash", policyWaiver.getHash());
    assertCustomObject(auditDTO, "policyConstraints",
        policyWaiver.getConstraintFacts().stream().map(ConstraintFactDTO::new).collect(Collectors.toList()));
  }

  private HttpRequest restRequest(String policyViolationId, OwnerType ownerType) {
    return ctx.restRequest()
        .path(PublicApiPaths.POLICY_VIOLATION_WAIVER_PATH)
        .parameter(policyViolationId, ownerType.toString());
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
        .map(IqPostgresApiPolicyViolationWaiverResourceAuditTest::parseAuditLog)
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

  private void assertApplicationData(AuditDTO auditDTO, Application application) {
    assertCustomData(auditDTO, "applicationId", application.getId());
    assertCustomData(auditDTO, "applicationPublicId", application.getPublicId());
    assertCustomData(auditDTO, "applicationName", application.getName());
  }

  private void assertOrganizationData(AuditDTO auditDTO, Organization organization) {
    assertCustomData(auditDTO, "organizationId", organization.getId());
    assertCustomData(auditDTO, "organizationName", organization.getName());
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
