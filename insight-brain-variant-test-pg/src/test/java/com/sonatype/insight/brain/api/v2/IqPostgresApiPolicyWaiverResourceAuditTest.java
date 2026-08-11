/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiWaiverOptionsDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.policy.ConstraintFactDTO;
import com.sonatype.insight.brain.report.ReportTestUtils;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.brain.variant.IqPostgresTest;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.test.LogOutput;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.api.v2.ApiPolicyWaiverResource.BY_POLICY_VIOLATION_ID_PATH;
import static com.sonatype.insight.brain.api.v2.ApiPolicyWaiverResource.BY_POLICY_WAIVER_ID_PATH;
import static com.sonatype.insight.brain.api.v2.ApiPolicyWaiverResource.OWNERS_PATH;
import static com.sonatype.insight.brain.api.v2.ApiPolicyWaiverResource.TRANSITIVE_VIOLATIONS_BY_SCAN_ID_PATH;
import static com.sonatype.insight.brain.api.v2.ApiPolicyWaiverResource.TRANSITIVE_VIOLATIONS_BY_STAGE_ID_PATH;
import static com.sonatype.insight.brain.model.repository.RepositoryContainer.REPOSITORY_CONTAINER_ID;
import static java.util.stream.Collectors.toCollection;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * IQ Server on PostgreSQL — port of the legacy {@code ApiPolicyWaiverResourceAuditTest} to the
 * reused-server variant pattern. No base class: audit-log capture/assertion helpers
 * ({@code AuditTestSupport} in the legacy base chain) are inlined here since {@link IqTestContext}
 * does not expose audit-log support. Kept in the original resource's package because
 * {@code BY_POLICY_VIOLATION_ID_PATH}/{@code BY_POLICY_WAIVER_ID_PATH}/{@code OWNERS_PATH}/
 * {@code TRANSITIVE_VIOLATIONS_BY_SCAN_ID_PATH}/{@code TRANSITIVE_VIOLATIONS_BY_STAGE_ID_PATH} are
 * package-private constants on {@link ApiPolicyWaiverResource}.
 */
@IqPostgresTest
class IqPostgresApiPolicyWaiverResourceAuditTest
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

  private PolicyEvaluation policyEvaluation;

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

    policyEvaluation = ctx.tempEntity().newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan1");
    policyViolation = ctx.tempEntity().newPolicyViolation(policyEvaluation, policy);
  }

  @AfterEach
  void tearDown() {
    logOutput.tearDown();
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.POLICY_WAIVER_PATH);
  }

  @Test
  void testDeletePolicyWaiver_Application() throws Exception {
    ConditionFact conditionFact = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID, 0, "foo", "bar");
    conditionFact.setTriggerJson("{\"conditionIndex\":1,\"trigger\":{\"refId\":\"1234\",\"severity\":5.7}}");
    ConstraintFact constraintFact = new ConstraintFact("id", "name", LogicalOperator.AND.toString());
    constraintFact.addConditionFact(conditionFact);

    PolicyWaiver policyWaiver =
        ctx.tempEntity().newWaiver("0b", policy.getId(), app.getId(), Collections.singletonList(constraintFact));

    restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getId(), policyWaiver.getId())
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_WAIVER, null);
    assertPolicyWaiverData(auditDTO, policyWaiver);
    assertApplicationData(auditDTO, app);
  }

  @Test
  void testDeletePolicyWaiver_Application_NullHashCode_NullConstraintFacts() throws Exception {
    PolicyWaiver policyWaiver = ctx.tempEntity().newWaiver(policy.getId(), app.getId());

    restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getId(), policyWaiver.getId())
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_WAIVER, null);
    assertPolicyWaiverData(auditDTO, policyWaiver);
    assertApplicationData(auditDTO, app);
  }

  @Test
  void testDeletePolicyWaiver_Application_Unauthorized() throws Exception {
    restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getId(), "policy-waiver-id")
        .with(unauthorizedUser())
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_WAIVER, "unauthorized");
    assertApplicationData(auditDTO, app);
  }

  @Test
  void testDeletePolicyWaiver_Organization() throws Exception {
    PolicyWaiver policyWaiver = ctx.tempEntity().newWaiver(policy.getId(), org.getId());

    restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.ORGANIZATION, org.getId(), policyWaiver.getId())
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_WAIVER, null);
    assertPolicyWaiverData(auditDTO, policyWaiver);
    assertOrganizationData(auditDTO, org);
  }

  @Test
  void testDeletePolicyWaiver_Organization_Unauthorized() throws Exception {
    restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.ORGANIZATION, org.getId(), "policy-waiver-id")
        .with(unauthorizedUser())
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_WAIVER, "unauthorized");
    assertOrganizationData(auditDTO, org);
  }

  @Test
  void testDeletePolicyWaiver_Repository() throws Exception {
    Repository repository = ctx.tempEntity().newRepository();
    PolicyWaiver policyWaiver = ctx.tempEntity().newWaiver(policy.getId(), repository.getId());

    restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.REPOSITORY, repository.getId(), policyWaiver.getId())
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_WAIVER, null);
    assertPolicyWaiverData(auditDTO, policyWaiver);
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  void testDeletePolicyWaiver_Repository_Unauthorized() throws Exception {
    Repository repository = ctx.tempEntity().newRepository();

    restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.REPOSITORY, repository.getId(), "policy-waiver-id")
        .with(unauthorizedUser())
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_WAIVER, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  void testDeletePolicyWaiver_RepositoryContainer() throws Exception {
    PolicyWaiver policyWaiver = ctx.tempEntity().newWaiver(policy.getId(), REPOSITORY_CONTAINER_ID);

    restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID, policyWaiver.getId())
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_WAIVER, null);
    assertPolicyWaiverData(auditDTO, policyWaiver);
    assertRepositoryContainerData(auditDTO);
  }

  @Test
  void testDeletePolicyWaiver_RepositoryContainer_Unauthorized() throws Exception {
    // Container-image waivers are stored under the container-image APPLICATION owner, not
    // REPOSITORY_CONTAINER_ID. Seed one so the delete re-routes to an app-scoped auth check
    // (rather than 404'ing on lookup before the auth check fires).
    PolicyWaiver policyWaiver = new PolicyWaiver(policy.getId(), app.getId(), "comment");
    policyWaiver.setForContainerImage(true);
    policyWaiverDAO.insert(policyWaiver);

    restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID, policyWaiver.getId())
        .with(unauthorizedUser())
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_WAIVER, "unauthorized");
    assertRepositoryContainerData(auditDTO);
  }

  @Test
  void testGetPolicyWaivers_Application() throws Exception {
    ConditionFact conditionFact = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID, 0, "foo", "bar");
    conditionFact.setTriggerJson("{\"conditionIndex\":1,\"trigger\":{\"refId\":\"1234\",\"severity\":5.7}}");
    ConstraintFact constraintFact = new ConstraintFact("id", "name", LogicalOperator.AND.toString());
    constraintFact.addConditionFact(conditionFact);

    PolicyWaiver policyWaiver =
        ctx.tempEntity().newWaiver("0b", policy.getId(), app.getId(), Collections.singletonList(constraintFact));

    restRequest().path(OWNERS_PATH).parameter(OwnerType.APPLICATION, app.getId()).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_WAIVER, null);
    assertPolicyWaiverData(auditDTO, policyWaiver);
    assertApplicationData(auditDTO, app);
  }

  @Test
  void testGetPolicyWaivers_Application_Unauthorized() throws Exception {
    restRequest().path(OWNERS_PATH).parameter(OwnerType.APPLICATION, app.getId()).with(unauthorizedUser()).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_WAIVER, "unauthorized");
    assertApplicationData(auditDTO, app);
  }

  @Test
  void testGetPolicyWaivers_Organization() throws Exception {
    ConditionFact conditionFact = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID, 0, "foo", "bar");
    conditionFact.setTriggerJson("{\"conditionIndex\":1,\"trigger\":{\"refId\":\"1234\",\"severity\":5.7}}");
    ConstraintFact constraintFact = new ConstraintFact("id", "name", LogicalOperator.AND.toString());
    constraintFact.addConditionFact(conditionFact);

    PolicyWaiver policyWaiver =
        ctx.tempEntity().newWaiver("0b", policy.getId(), org.getId(), Collections.singletonList(constraintFact));

    restRequest().path(OWNERS_PATH).parameter(OwnerType.ORGANIZATION, org.getId()).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_WAIVER, null);
    assertPolicyWaiverData(auditDTO, policyWaiver);
    assertOrganizationData(auditDTO, org);
  }

  @Test
  void testGetPolicyWaivers_Organization_Unauthorized() throws Exception {
    restRequest().path(OWNERS_PATH).parameter(OwnerType.ORGANIZATION, org.getId()).with(unauthorizedUser()).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_WAIVER, "unauthorized");
    assertOrganizationData(auditDTO, org);
  }

  @Test
  void testGetPolicyWaivers_Repository() throws Exception {
    Repository repository = ctx.tempEntity().newRepository();
    PolicyWaiver policyWaiver = ctx.tempEntity().newWaiver(policy.getId(), repository.getId());

    restRequest().path(OWNERS_PATH).parameter(OwnerType.REPOSITORY, repository.getId()).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_WAIVER, null);
    assertPolicyWaiverData(auditDTO, policyWaiver);
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  void testGetPolicyWaivers_Repository_Unauthorized() throws Exception {
    Repository repository = ctx.tempEntity().newRepository();
    restRequest().path(OWNERS_PATH).parameter(OwnerType.REPOSITORY, repository.getId()).with(unauthorizedUser()).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_WAIVER, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  void testGetPolicyWaivers_RepositoryContainer() throws Exception {
    // Container-image waivers are stored under the container-image APPLICATION owner (not
    // REPOSITORY_CONTAINER_ID) and flagged is_for_container_image=true. The Firewall
    // Containers → Existing Waivers list surfaces them via the virtual REPOSITORY_CONTAINER_ID.
    PolicyWaiver policyWaiver = new PolicyWaiver(policy.getId(), app.getId(), "comment");
    policyWaiver.setForContainerImage(true);
    policyWaiverDAO.insert(policyWaiver);

    restRequest().path(OWNERS_PATH)
        .parameter(OwnerType.REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID, policyWaiver.getId())
        .get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_WAIVER, null);
    assertPolicyWaiverData(auditDTO, policyWaiver);
    assertRepositoryContainerData(auditDTO);
  }

  @Test
  void testGetPolicyWaivers_RepositoryContainer_Unauthorized() throws Exception {
    restRequest().path(OWNERS_PATH)
        .parameter(OwnerType.REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID)
        .with(unauthorizedUser())
        .get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_WAIVER, "unauthorized");
    assertRepositoryContainerData(auditDTO);
  }

  @Test
  void testAddPolicyWaiverByPolicyViolationId_Application() throws Exception {
    ApiWaiverOptionsDTO waiverOptionsDTO = new ApiWaiverOptionsDTO();
    waiverOptionsDTO.comment = "waiver comment";
    restRequest().path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getId(), policyViolation.getId())
        .body(waiverOptionsDTO, MediaType.APPLICATION_JSON)
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_WAIVER, null);
    assertPolicyWaiverData(auditDTO);
    assertApplicationData(auditDTO, app);
  }

  @Test
  void testAddPolicyWaiverByPolicyViolationId_Organization() throws Exception {
    ApiWaiverOptionsDTO waiverOptionsDTO = new ApiWaiverOptionsDTO();
    waiverOptionsDTO.comment = "waiver comment";
    restRequest().path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.ORGANIZATION, org.getId(), policyViolation.getId())
        .body(waiverOptionsDTO, MediaType.APPLICATION_JSON)
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_WAIVER, null);
    assertPolicyWaiverData(auditDTO);
    assertOrganizationData(auditDTO, org);
  }

  @Test
  void testAddPolicyWaiverByPolicyViolationId_RootOrganization() throws Exception {
    ApiWaiverOptionsDTO waiverOptionsDTO = new ApiWaiverOptionsDTO();
    waiverOptionsDTO.comment = "waiver comment";
    restRequest().path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID, policyViolation.getId())
        .body(waiverOptionsDTO, MediaType.APPLICATION_JSON)
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_WAIVER, null);
    assertPolicyWaiverData(auditDTO);
    assertOrganizationData(auditDTO, Organization.ROOT_ORGANIZATION_ID, "Root Organization");
  }

  @Test
  void testAddPolicyWaiverByPolicyViolationId_Application_Unauthorized() throws Exception {
    ApiWaiverOptionsDTO waiverOptionsDTO = new ApiWaiverOptionsDTO();
    waiverOptionsDTO.comment = "waiver comment";
    restRequest().path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getId(), policyViolation.getId())
        .with(unauthorizedUser())
        .body(waiverOptionsDTO, MediaType.APPLICATION_JSON)
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_WAIVER, "unauthorized");
    assertApplicationData(auditDTO, app);
  }

  @Test
  void testAddPolicyWaiverByPolicyViolationId_Organization_Unauthorized() throws Exception {
    ApiWaiverOptionsDTO waiverOptionsDTO = new ApiWaiverOptionsDTO();
    waiverOptionsDTO.comment = "waiver comment";
    restRequest().path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.ORGANIZATION, org.getId(), policyViolation.getId())
        .with(unauthorizedUser())
        .body(waiverOptionsDTO, MediaType.APPLICATION_JSON)
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_WAIVER, "unauthorized");
    assertOrganizationData(auditDTO, org);
  }

  @Test
  void testAddPolicyWaiverByPolicyViolationId_RootOrganization_Unauthorized() throws Exception {
    ApiWaiverOptionsDTO waiverOptionsDTO = new ApiWaiverOptionsDTO();
    waiverOptionsDTO.comment = "waiver comment";
    restRequest().path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID, policyViolation.getId())
        .with(unauthorizedUser())
        .body(waiverOptionsDTO, MediaType.APPLICATION_JSON)
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_WAIVER, "unauthorized");
    assertOrganizationData(auditDTO, Organization.ROOT_ORGANIZATION_ID, "Root Organization");
  }

  @Test
  @SuppressWarnings("unchecked")
  void testAddWaiverToTransitivePolicyViolationsByAppScanComponent() throws Exception {
    ReportTestUtils.createReportFile(app.getId(), policyEvaluation.getScanId(),
        ReportTestUtils.zipReportDir("/ApiPolicyWaiverResourceAuditTest/report", ctx.tempFolder()),
        ctx.lookup(InsightWork.class));

    ComponentIdentifier direct = ComponentIdentifier.createMavenCoordinates("g", "direct", "v", "", "e");
    ComponentIdentifier transitive = ComponentIdentifier.createMavenCoordinates("g", "transitive", "v", "", "e");
    PolicyViolation policyViolation =
        ctx.tempEntity().newPolicyViolation(policyEvaluation, policy, transitive, "hash2");

    ReportHelper.createPolicyThreats(
        ctx.lookup(InsightWork.class),
        app.getId(),
        policyEvaluation.getScanId(),
        Collections.singletonList(policyViolation));

    ApiWaiverOptionsDTO waiverOptionsDTO = new ApiWaiverOptionsDTO();
    waiverOptionsDTO.comment = "waiver comment";
    waiverOptionsDTO.expiryTime = new Date(System.currentTimeMillis() + 10000);
    restRequest().path(TRANSITIVE_VIOLATIONS_BY_SCAN_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getPublicId(), policyEvaluation.getScanId())
        .query("hash", "hash1")
        .body(waiverOptionsDTO, MediaType.APPLICATION_JSON)
        .post();

    PolicyWaiver policyWaiver = policyWaiverDAO.getByPolicyId(policy.getId()).get(0);
    AuditDTO waiverAuditDTO = assertAuditLog(AuditEvent.CREATE_WAIVER, null);
    assertCustomData(waiverAuditDTO, "policyWaiverId", policyWaiver.getId());
    assertCustomData(waiverAuditDTO, "policyId", policy.getId());
    assertCustomData(waiverAuditDTO, "policyName", policy.getName());
    assertCustomData(waiverAuditDTO, "comment", waiverOptionsDTO.comment);
    assertCustomObject(waiverAuditDTO, "policyConstraints",
        policyWaiver.getConstraintFacts().stream().map(ConstraintFactDTO::new).collect(Collectors.toList()));

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_TRANSITIVE_POLICY_VIOLATIONS_WAIVER, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "scanId", policyEvaluation.getScanId());
    assertCustomData(auditDTO, "comment", waiverOptionsDTO.comment);
    assertCustomData(auditDTO, "expiryTime", waiverOptionsDTO.expiryTime.getTime());
    assertCustomData(auditDTO, "componentHash", "hash1");

    Map<String, ?> auditedComponentIdentifierMap = (Map<String, ?>) auditDTO.data.get("componentIdentifier");
    ComponentIdentifier auditedComponentIdentifier =
        new ComponentIdentifier(auditedComponentIdentifierMap.get("format").toString(),
            (Map<String, String>) auditedComponentIdentifierMap.get("coordinates"));

    assertThat(auditedComponentIdentifier).isEqualTo(direct);
  }

  @Test
  void testAddWaiverToTransitivePolicyViolationsByOwnerStageComponent() throws Exception {
    ReportTestUtils.createReportFile(app.getId(), policyEvaluation.getScanId(),
        ReportTestUtils.zipReportDir("/ApiPolicyWaiverResourceAuditTest/report", ctx.tempFolder()),
        ctx.lookup(InsightWork.class));
    ComponentIdentifier direct = ComponentIdentifier.createMavenCoordinates("g", "direct", "v", "", "e");
    ComponentIdentifier transitive = ComponentIdentifier.createMavenCoordinates("g", "transitive", "v", "", "e");
    PolicyViolation policyViolation =
        ctx.tempEntity().newPolicyViolation(policyEvaluation, policy, transitive, "hash2");
    ReportHelper.createPolicyThreats(
        ctx.lookup(InsightWork.class),
        app.getId(),
        policyEvaluation.getScanId(),
        Collections.singletonList(policyViolation));
    ApiWaiverOptionsDTO waiverOptionsDTO = new ApiWaiverOptionsDTO();
    waiverOptionsDTO.comment = "waiver comment";
    waiverOptionsDTO.expiryTime = DateUtils.addDays(new Date(), 1);

    restRequest().path(TRANSITIVE_VIOLATIONS_BY_STAGE_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getPublicId(), BuildStageType.ID)
        .query("hash", "hash1")
        .body(waiverOptionsDTO, MediaType.APPLICATION_JSON)
        .post();

    PolicyWaiver policyWaiver = policyWaiverDAO.getByPolicyId(policy.getId()).get(0);
    AuditDTO waiverAuditDTO = assertAuditLog(AuditEvent.CREATE_WAIVER, null);
    assertCustomData(waiverAuditDTO, "policyWaiverId", policyWaiver.getId());
    assertCustomData(waiverAuditDTO, "policyId", policy.getId());
    assertCustomData(waiverAuditDTO, "policyName", policy.getName());
    assertCustomData(waiverAuditDTO, "comment", waiverOptionsDTO.comment);
    assertCustomObject(waiverAuditDTO, "policyConstraints",
        policyWaiver.getConstraintFacts().stream().map(ConstraintFactDTO::new).collect(Collectors.toList()));
    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_TRANSITIVE_POLICY_VIOLATIONS_WAIVER, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "stageId", BuildStageType.ID);
    assertCustomData(auditDTO, "comment", waiverOptionsDTO.comment);
    assertCustomData(auditDTO, "expiryTime", waiverOptionsDTO.expiryTime.getTime());
    assertCustomData(auditDTO, "componentHash", "hash1");
    Map<String, ?> auditedComponentIdentifierMap = (Map<String, ?>) auditDTO.data.get("componentIdentifier");
    ComponentIdentifier auditedComponentIdentifier =
        new ComponentIdentifier(auditedComponentIdentifierMap.get("format").toString(),
            (Map<String, String>) auditedComponentIdentifierMap.get("coordinates"));
    assertThat(auditedComponentIdentifier).isEqualTo(direct);
  }

  @Test
  void testAddPolicyWaiverByPolicyViolationId_Repository() throws Exception {
    Repository repository = ctx.tempEntity().newRepository();
    policy = ctx.tempEntity().newPolicy(Organization.ROOT_ORGANIZATION_ID);
    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        ctx.tempEntity().newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());
    ApiWaiverOptionsDTO waiverOptionsDTO = new ApiWaiverOptionsDTO();
    waiverOptionsDTO.comment = "waiver comment";

    restRequest().path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.REPOSITORY, repository.getId(), proxyRepositoryPolicyViolation.getId())
        .body(waiverOptionsDTO, MediaType.APPLICATION_JSON)
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_WAIVER, null);
    assertPolicyWaiverData(auditDTO);
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  void testAddPolicyWaiverByPolicyViolationId_Repository_Unauthorized() throws Exception {
    Repository repository = ctx.tempEntity().newRepository();
    policy = ctx.tempEntity().newPolicy(Organization.ROOT_ORGANIZATION_ID);
    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        ctx.tempEntity().newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());
    ApiWaiverOptionsDTO waiverOptionsDTO = new ApiWaiverOptionsDTO();
    waiverOptionsDTO.comment = "waiver comment";
    restRequest().path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.REPOSITORY, repository.getId(), proxyRepositoryPolicyViolation.getId())
        .with(unauthorizedUser())
        .body(waiverOptionsDTO, MediaType.APPLICATION_JSON)
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_WAIVER, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  void testAddPolicyWaiverByPolicyViolationId_RepositoryContainer() throws Exception {
    Repository repository = ctx.tempEntity().newRepository();
    policy = ctx.tempEntity().newPolicy(Organization.ROOT_ORGANIZATION_ID);
    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        ctx.tempEntity().newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());
    ApiWaiverOptionsDTO waiverOptionsDTO = new ApiWaiverOptionsDTO();
    waiverOptionsDTO.comment = "waiver comment";

    restRequest()
        .path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.REPOSITORY_CONTAINER,
            RepositoryContainer.REPOSITORY_CONTAINER_ID, proxyRepositoryPolicyViolation.getId())
        .body(waiverOptionsDTO, MediaType.APPLICATION_JSON)
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_WAIVER, null);
    assertPolicyWaiverData(auditDTO);
    assertRepositoryContainerData(auditDTO);
  }

  @Test
  void testAddPolicyWaiverByPolicyViolationId_RepositoryContainer_Unauthorized() throws Exception {
    Repository repository = ctx.tempEntity().newRepository();
    policy = ctx.tempEntity().newPolicy(Organization.ROOT_ORGANIZATION_ID);
    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        ctx.tempEntity().newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());
    ApiWaiverOptionsDTO waiverOptionsDTO = new ApiWaiverOptionsDTO();
    waiverOptionsDTO.comment = "waiver comment";
    restRequest().path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID,
            proxyRepositoryPolicyViolation.getId())
        .with(unauthorizedUser())
        .body(waiverOptionsDTO, MediaType.APPLICATION_JSON)
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_WAIVER, "unauthorized");
    assertRepositoryContainerData(auditDTO);
  }

  private void assertPolicyWaiverData(AuditDTO auditDTO) {
    String policyWaiverId = (String) auditDTO.data.get("policyWaiverId");
    PolicyWaiver policyWaiver = policyWaiverDAO.getByIdNotNull(policyWaiverId);
    assertPolicyWaiverData(auditDTO, policyWaiver);
  }

  private void assertPolicyWaiverData(AuditDTO auditDTO, PolicyWaiver policyWaiver) {
    assertCustomData(auditDTO, "policyId", policyWaiver.getPolicyId());
    assertCustomData(auditDTO, "policyName", getPolicyDAO().getById(policyWaiver.getPolicyId()).getName());
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

  @Test
  void testGetPolicyWaiver_Application() throws Exception {
    ConditionFact conditionFact = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID, 0, "foo", "bar");
    conditionFact.setTriggerJson("{\"conditionIndex\":1,\"trigger\":{\"refId\":\"1234\",\"severity\":5.7}}");
    ConstraintFact constraintFact = new ConstraintFact("id", "name", LogicalOperator.AND.toString());
    constraintFact.addConditionFact(conditionFact);

    PolicyWaiver policyWaiver =
        ctx.tempEntity().newWaiver("0b", policy.getId(), app.getId(), Collections.singletonList(constraintFact));

    restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getId(), policyWaiver.getId())
        .get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_WAIVER, null);
    assertPolicyWaiverData(auditDTO, policyWaiver);
    assertApplicationData(auditDTO, app);
  }

  @Test
  void testGetPolicyWaiver_Application_Unauthorized() throws Exception {
    restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getId(), "policyWaiverHash")
        .with(unauthorizedUser())
        .get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_WAIVER, "unauthorized");
    assertApplicationData(auditDTO, app);
  }

  @Test
  void testGetPolicyWaiver_Organization() throws Exception {
    ConditionFact conditionFact = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID, 0, "foo", "bar");
    conditionFact.setTriggerJson("{\"conditionIndex\":1,\"trigger\":{\"refId\":\"1234\",\"severity\":5.7}}");
    ConstraintFact constraintFact = new ConstraintFact("id", "name", LogicalOperator.AND.toString());
    constraintFact.addConditionFact(conditionFact);

    PolicyWaiver policyWaiver =
        ctx.tempEntity().newWaiver("0b", policy.getId(), org.getId(), Collections.singletonList(constraintFact));

    restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.ORGANIZATION, org.getId(), policyWaiver.getId())
        .get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_WAIVER, null);
    assertPolicyWaiverData(auditDTO, policyWaiver);
    assertOrganizationData(auditDTO, org);
  }

  @Test
  void testGetPolicyWaiver_Organization_Unauthorized() throws Exception {
    restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.ORGANIZATION, org.getId(), "policyWaiverHash")
        .with(unauthorizedUser())
        .get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_WAIVER, "unauthorized");
    assertOrganizationData(auditDTO, org);
  }

  @Test
  void testGetPolicyWaiver_Repository() throws Exception {
    Repository repository = ctx.tempEntity().newRepository();
    PolicyWaiver policyWaiver = ctx.tempEntity().newWaiver(policy.getId(), repository.getId());

    restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.REPOSITORY, repository.getId(), policyWaiver.getId())
        .get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_WAIVER, null);
    assertPolicyWaiverData(auditDTO, policyWaiver);
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  void testGetPolicyWaiver_Repository_Unauthorized() throws Exception {
    Repository repository = ctx.tempEntity().newRepository();

    restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.REPOSITORY, repository.getId(), "policyWaiverHash")
        .with(unauthorizedUser())
        .get();

    restRequest().path(OWNERS_PATH).parameter(OwnerType.REPOSITORY, repository.getId()).with(unauthorizedUser()).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_WAIVER, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  void testGetPolicyWaiver_RepositoryContainer() throws Exception {
    PolicyWaiver policyWaiver = ctx.tempEntity().newWaiver(policy.getId(), REPOSITORY_CONTAINER_ID);

    restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID, policyWaiver.getId())
        .get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_WAIVER, null);
    assertPolicyWaiverData(auditDTO, policyWaiver);
    assertRepositoryContainerData(auditDTO);
  }

  @Test
  void testGetPolicyWaiver_RepositoryContainer_Unauthorized() throws Exception {
    restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID, "policyWaiverHash")
        .with(unauthorizedUser())
        .get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_WAIVER, "unauthorized");
    assertRepositoryContainerData(auditDTO);
  }

  @Test
  void testUpdatePolicyWaiver_Application() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();
    Policy policy = ctx.tempEntity().newPolicy(application.getId());
    PolicyWaiver policyWaiver = ctx.tempEntity().newWaiver(policy.getId(), application.getId());
    ApiWaiverOptionsDTO dto = new ApiWaiverOptionsDTO(policyWaiver);
    dto.comment = "new comment";

    restRequest()
        .path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, application.getId(), policyWaiver.getId())
        .body(dto, MediaType.APPLICATION_JSON)
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_WAIVER, null);
    assertPolicyWaiverData(auditDTO, policyWaiverDAO.getById(policyWaiver.getId()));
    assertApplicationData(auditDTO, application);
  }

  @Test
  void testUpdatePolicyWaiver_Application_Unauthorized() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();
    Policy policy = ctx.tempEntity().newPolicy(application.getId());
    PolicyWaiver policyWaiver = ctx.tempEntity().newWaiver(policy.getId(), application.getId());
    ApiWaiverOptionsDTO dto = new ApiWaiverOptionsDTO(policyWaiver);
    dto.comment = "new comment";

    restRequest()
        .path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, application.getId(), policyWaiver.getId())
        .body(dto, MediaType.APPLICATION_JSON)
        .with(unauthorizedUser())
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_WAIVER, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  @Test
  void testUpdatePolicyWaiver_Organization() throws Exception {
    Organization organization = ctx.tempEntity().newOrganization();
    Policy policy = ctx.tempEntity().newPolicy(organization.getId());
    PolicyWaiver policyWaiver = ctx.tempEntity().newWaiver(policy.getId(), organization.getId());
    ApiWaiverOptionsDTO dto = new ApiWaiverOptionsDTO(policyWaiver);
    dto.comment = "new comment";

    restRequest()
        .path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.ORGANIZATION, organization.getId(), policyWaiver.getId())
        .body(dto, MediaType.APPLICATION_JSON)
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_WAIVER, null);
    assertPolicyWaiverData(auditDTO, policyWaiverDAO.getById(policyWaiver.getId()));
    assertOrganizationData(auditDTO, organization);
  }

  @Test
  void testUpdatePolicyWaiver_Organization_Unauthorized() throws Exception {
    Organization organization = ctx.tempEntity().newOrganization();
    Policy policy = ctx.tempEntity().newPolicy(organization.getId());
    PolicyWaiver policyWaiver = ctx.tempEntity().newWaiver(policy.getId(), organization.getId());
    ApiWaiverOptionsDTO dto = new ApiWaiverOptionsDTO(policyWaiver);
    dto.comment = "new comment";

    restRequest()
        .path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.ORGANIZATION, organization.getId(), policyWaiver.getId())
        .body(dto, MediaType.APPLICATION_JSON)
        .with(unauthorizedUser())
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_WAIVER, "unauthorized");
    assertOrganizationData(auditDTO, organization);
  }

  @Test
  void testUpdatePolicyWaiver_Repository() throws Exception {
    Repository repository = ctx.tempEntity().newRepository();
    Policy policy = ctx.tempEntity().newPolicy(repository.getId());
    PolicyWaiver policyWaiver = ctx.tempEntity().newWaiver(policy.getId(), repository.getId());
    ApiWaiverOptionsDTO dto = new ApiWaiverOptionsDTO(policyWaiver);
    dto.comment = "new comment";

    restRequest()
        .path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.REPOSITORY, repository.getId(), policyWaiver.getId())
        .body(dto, MediaType.APPLICATION_JSON)
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_WAIVER, null);
    assertPolicyWaiverData(auditDTO, policyWaiverDAO.getById(policyWaiver.getId()));
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  void testUpdatePolicyWaiver_Repository_Unauthorized() throws Exception {
    Repository repository = ctx.tempEntity().newRepository();
    Policy policy = ctx.tempEntity().newPolicy(repository.getId());
    PolicyWaiver policyWaiver = ctx.tempEntity().newWaiver(policy.getId(), repository.getId());
    ApiWaiverOptionsDTO dto = new ApiWaiverOptionsDTO(policyWaiver);
    dto.comment = "new comment";

    restRequest()
        .path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.REPOSITORY, repository.getId(), policyWaiver.getId())
        .body(dto, MediaType.APPLICATION_JSON)
        .with(unauthorizedUser())
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_WAIVER, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  void testUpdatePolicyWaiver_RepositoryManager() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Policy policy = ctx.tempEntity().newPolicy(repositoryManager.getId());
    PolicyWaiver policyWaiver = ctx.tempEntity().newWaiver(policy.getId(), repositoryManager.getId());
    ApiWaiverOptionsDTO dto = new ApiWaiverOptionsDTO(policyWaiver);
    dto.comment = "new comment";

    restRequest()
        .path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId(), policyWaiver.getId())
        .body(dto, MediaType.APPLICATION_JSON)
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_WAIVER, null);
    assertPolicyWaiverData(auditDTO, policyWaiverDAO.getById(policyWaiver.getId()));
    assertRepositoryManagerData(auditDTO, repositoryManager);
  }

  @Test
  void testUpdatePolicyWaiver_RepositoryManager_Unauthorized() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Policy policy = ctx.tempEntity().newPolicy(repositoryManager.getId());
    PolicyWaiver policyWaiver = ctx.tempEntity().newWaiver(policy.getId(), repositoryManager.getId());
    ApiWaiverOptionsDTO dto = new ApiWaiverOptionsDTO(policyWaiver);
    dto.comment = "new comment";

    restRequest()
        .path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId(), policyWaiver.getId())
        .body(dto, MediaType.APPLICATION_JSON)
        .with(unauthorizedUser())
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_WAIVER, "unauthorized");
    assertRepositoryManagerData(auditDTO, repositoryManager);
  }

  @Test
  void testUpdatePolicyWaiver_RepositoryContainer() throws Exception {
    Policy policy = ctx.tempEntity().newPolicy(REPOSITORY_CONTAINER_ID);
    PolicyWaiver policyWaiver = ctx.tempEntity().newWaiver(policy.getId(), REPOSITORY_CONTAINER_ID);
    ApiWaiverOptionsDTO dto = new ApiWaiverOptionsDTO(policyWaiver);
    dto.comment = "new comment";

    restRequest()
        .path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID, policyWaiver.getId())
        .body(dto, MediaType.APPLICATION_JSON)
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_WAIVER, null);
    assertPolicyWaiverData(auditDTO, policyWaiverDAO.getById(policyWaiver.getId()));
    assertRepositoryContainerData(auditDTO);
  }

  @Test
  void testUpdatePolicyWaiver_RepositoryContainer_Unauthorized() throws Exception {
    Policy policy = ctx.tempEntity().newPolicy(REPOSITORY_CONTAINER_ID);
    PolicyWaiver policyWaiver = ctx.tempEntity().newWaiver(policy.getId(), REPOSITORY_CONTAINER_ID);
    ApiWaiverOptionsDTO dto = new ApiWaiverOptionsDTO(policyWaiver);
    dto.comment = "new comment";

    restRequest()
        .path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID, policyWaiver.getId())
        .body(dto, MediaType.APPLICATION_JSON)
        .with(unauthorizedUser())
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_WAIVER, "unauthorized");
    assertRepositoryContainerData(auditDTO);
  }

  // --- inlined AuditTestSupport / AbstractAuditTest helpers (not exposed by IqTestContext) --------

  private Consumer<HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUserEntity);
  }

  private PolicyDAO getPolicyDAO() {
    return ctx.lookup(PolicyDAO.class);
  }

  private List<AuditDTO> awaitLogEntries(AuditEvent auditEvent, int count) {
    await("Expect audit event " + auditEvent).atMost(10, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(getLogEntries(auditEvent)).hasSizeGreaterThanOrEqualTo(count));
    return getLogEntries(auditEvent);
  }

  private List<AuditDTO> getLogEntries(AuditEvent auditEvent) {
    return logOutput.getInfoMessages(AuditRecorder.toLoggerName(auditEvent.getDomain()))
        .stream()
        .map(IqPostgresApiPolicyWaiverResourceAuditTest::parseAuditLog)
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
      assertCustomData(auditDTO, key, ((java.util.Collection<?>) pojo).stream()
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
    assertOrganizationData(auditDTO, organization.getId(), organization.getName());
  }

  private void assertOrganizationData(AuditDTO auditDTO, String organizationId, String organizationName) {
    assertCustomData(auditDTO, "organizationId", organizationId);
    assertCustomData(auditDTO, "organizationName", organizationName);
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

  private void assertRepositoryManagerData(AuditDTO auditDTO, RepositoryManager repositoryManager) {
    assertCustomData(auditDTO, "repositoryManagerId", repositoryManager.getId());
    assertCustomData(auditDTO, "repositoryManagerInstanceId", repositoryManager.getInstanceId());
    assertCustomData(auditDTO, "repositoryManagerName", repositoryManager.getName());
  }

  private void assertRepositoryContainerData(AuditDTO auditDTO) {
    assertThat(auditDTO.data).containsEntry("scope", "all-repositories");
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
