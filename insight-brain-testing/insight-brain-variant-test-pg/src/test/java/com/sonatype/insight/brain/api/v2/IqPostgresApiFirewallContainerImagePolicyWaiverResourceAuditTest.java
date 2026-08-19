/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.v2.dto.containerimagewaiver.ApiContainerImageWaiverDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.policy.ConstraintFactDTO;
import com.sonatype.insight.brain.variant.IqPostgresTest;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.test.LogOutput;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.api.PublicApiPaths.FIREWALL_CONTAINER_IMAGE_RESOURCE_PATH;
import static com.sonatype.insight.brain.api.v2.ApiFirewallContainerImagePolicyWaiverResource.CONTAINER_IMAGE_ID;
import static com.sonatype.insight.brain.api.v2.ApiFirewallContainerImagePolicyWaiverResource.POLICY_WAIVER;
import static java.util.stream.Collectors.toCollection;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * IQ Server on PostgreSQL — port of the legacy {@code ApiFirewallContainerImagePolicyWaiverResourceAuditTest} to
 * the reused-server variant pattern. No base class: audit-log capture/assertion helpers ({@code AuditTestSupport}
 * in the legacy base chain) are inlined here since {@link IqTestContext} does not expose audit-log support. Kept
 * in the original resource's package because {@code CONTAINER_IMAGE_ID}/{@code POLICY_WAIVER} are package-private
 * constants on {@link ApiFirewallContainerImagePolicyWaiverResource}.
 */
@IqPostgresTest
class IqPostgresApiFirewallContainerImagePolicyWaiverResourceAuditTest
{
  private static final ObjectMapper JSON = new ObjectMapper();

  // Injected by IqPostgresServerExtension: the extension owns the shared, reused server.
  private IqTestContext ctx;

  private final TestLogOutput logOutput = new TestLogOutput(AuditRecorder.BASE_LOGGER_NAME);

  private PolicyWaiverDAO policyWaiverDAO;

  private RepositoryDAO repositoryDAO;

  private OrganizationDAO organizationDAO;

  private User unauthorizedUser;

  private Organization org;

  private Application app;

  private Policy policy;

  private PolicyEvaluation policyEvaluation;

  @BeforeEach
  void setUp() throws Exception {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();

    policyWaiverDAO = ctx.lookup(PolicyWaiverDAO.class);
    repositoryDAO = ctx.lookup(RepositoryDAO.class);
    organizationDAO = ctx.lookup(OrganizationDAO.class);

    org = ctx.tempEntity().newOrganization();
    app = ctx.tempEntity().newApplication(org.getId());
    policy = ctx.tempEntity().newPolicy();
    policyEvaluation = ctx.tempEntity().newPolicyEvaluation(app.getId(), ProxyStageType.ID, "scan1");
    ctx.tempEntity()
        .newPolicyViolation(policyEvaluation, policy, 5, PolicyThreatCategory.SECURITY,
            "g", "a", "v", "hash", FailActionType.ID);
    Repository repository = ctx.tempEntity()
        .newRepository(ctx.tempEntity().newRepositoryManager(), "docker-repo",
            RepositoryType.proxy, "docker");
    repository.setRelatedOrganizationId(org.getId());
    repositoryDAO.update(repository);
    org.setRelatedRepositoryId(repository.getId());
    organizationDAO.update(org);

    ctx.setFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
  }

  @AfterEach
  void tearDown() {
    try {
      logOutput.tearDown();
    }
    finally {
      SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(false);
    }
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(FIREWALL_CONTAINER_IMAGE_RESOURCE_PATH + CONTAINER_IMAGE_ID + POLICY_WAIVER);
  }

  private Consumer<HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUser);
  }

  @Test
  void testAddWaiver_Unauthorized() throws Exception {
    restRequest()
        .parameter(app.getId())
        .body(new ApiContainerImageWaiverDTO(), MediaType.APPLICATION_JSON)
        .with(unauthorizedUser())
        .post();

    assertAuditLog(AuditEvent.CREATE_CONTAINER_IMAGE_POLICY_VIOLATIONS_WAIVER, "unauthorized");
  }

  @Test
  void testAddWaiver() throws Exception {
    ApiContainerImageWaiverDTO waiverDTO = new ApiContainerImageWaiverDTO();
    waiverDTO.expiryTime = DateUtils.addDays(new Date(), 1);
    waiverDTO.comment = "Container image waiver comment";

    restRequest()
        .parameter(app.getId())
        .body(waiverDTO)
        .post();

    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getActiveByOwnerId(app.getId());
    assertThat(policyWaivers).hasSize(2);

    assertAuditLog(AuditEvent.CREATE_CONTAINER_IMAGE_POLICY_VIOLATIONS_WAIVER, null);
    List<AuditDTO> waiverAuditDTOs = assertAuditLogs(AuditEvent.CREATE_WAIVER, 2, null);

    policyWaivers = new ArrayList<>(policyWaivers);
    policyWaivers.sort(Comparator.comparing(PolicyWaiver::getHash, Comparator.nullsLast(Comparator.naturalOrder())));

    assertWaiverAuditDTO(waiverAuditDTOs.get(0), policyWaivers.get(0));
    assertWaiverAuditDTO(waiverAuditDTOs.get(1), policyWaivers.get(1));
  }

  @Test
  void testDeleteWaiversToContainerImage_Unauthorized() throws Exception {
    restRequest()
        .parameter(app.getId())
        .with(unauthorizedUser())
        .delete();

    assertAuditLog(AuditEvent.DELETE_CONTAINER_IMAGE_POLICY_VIOLATIONS_WAIVER, "unauthorized");
  }

  @Test
  void testDeleteWaiversToContainerImage_AuditEvent() throws Exception {
    policyEvaluation = ctx.tempEntity().newPolicyEvaluation(app.getId(), ProxyStageType.ID, "scan1");
    ctx.tempEntity().newPolicyViolation(policyEvaluation, policy);

    ApiContainerImageWaiverDTO containerWaiversDTO = new ApiContainerImageWaiverDTO();
    containerWaiversDTO.comment = "Container image waiver comment";
    containerWaiversDTO.waiverReasonId = ctx.tempEntity().newWaiverReason("type", "reasons").getId();
    containerWaiversDTO.expiryTime = DateUtils.addDays(new Date(), 1);

    restRequest()
        .parameter(app.getId())
        .body(containerWaiversDTO)
        .post();

    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getActiveByOwnerId(app.getId());
    assertThat(policyWaivers).hasSize(2);

    restRequest()
        .parameter(app.getId())
        .delete();

    AuditDTO resourceAudit = assertAuditLog(AuditEvent.DELETE_CONTAINER_IMAGE_POLICY_VIOLATIONS_WAIVER, null);
    assertThat(resourceAudit.domain).isEqualTo("governance.waiver.container");

    List<AuditDTO> auditDTOs = assertAuditLogs(AuditEvent.DELETE_WAIVER, 2, null);

    auditDTOs = auditDTOs.stream().filter(audit -> audit.data != null).toList();
    assertThat(
        auditDTOs.stream()
            .filter(auditDTO -> auditDTO.data.get("isForContainerImageComponent").equals(Boolean.TRUE))
            .toList()).hasSize(1);
    assertThat(auditDTOs.stream()
        .filter(auditDTO -> auditDTO.data.get("isForContainerImage").equals(Boolean.TRUE))
        .toList()).hasSize(1);
  }

  private void assertWaiverAuditDTO(AuditDTO waiverAuditDTO, PolicyWaiver policyWaiver) {
    assertCustomData(waiverAuditDTO, "policyWaiverId", policyWaiver.getId());
    assertCustomData(waiverAuditDTO, "policyId", policy.getId());
    assertCustomData(waiverAuditDTO, "policyName", policy.getName());
    assertCustomData(waiverAuditDTO, "componentHash", policyWaiver.getHash());
    assertCustomData(waiverAuditDTO, "expiryTime", policyWaiver.getExpiryTime().getTime());
    assertCustomData(waiverAuditDTO, "comment", policyWaiver.getComment());
    assertCustomData(waiverAuditDTO, "isForContainerImageComponent", policyWaiver.isForContainerImageComponent());
    assertCustomData(waiverAuditDTO, "isForContainerImage", policyWaiver.isForContainerImage());
    assertCustomObject(waiverAuditDTO, "policyConstraints",
        policyWaiver.getConstraintFacts().stream().map(ConstraintFactDTO::new).collect(Collectors.toList()));
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
        .map(IqPostgresApiFirewallContainerImagePolicyWaiverResourceAuditTest::parseAuditLog)
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
      assertCustomData(auditDTO, key, ((java.util.Collection<?>) pojo).stream()
          .map(element -> JSON.convertValue(element, java.util.Map.class))
          .collect(java.util.stream.Collectors.toList()));
    }
    else {
      assertCustomData(auditDTO, key, JSON.convertValue(pojo, java.util.Map.class));
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
