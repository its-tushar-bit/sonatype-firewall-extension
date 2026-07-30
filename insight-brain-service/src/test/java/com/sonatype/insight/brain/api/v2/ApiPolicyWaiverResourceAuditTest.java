/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
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
import com.sonatype.insight.brain.policy.ConstraintFactDTO;
import com.sonatype.insight.brain.report.ReportTestUtils;
import com.sonatype.insight.brain.service.AbstractAuditTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.api.v2.ApiPolicyWaiverResource.BY_POLICY_VIOLATION_ID_PATH;
import static com.sonatype.insight.brain.api.v2.ApiPolicyWaiverResource.BY_POLICY_WAIVER_ID_PATH;
import static com.sonatype.insight.brain.api.v2.ApiPolicyWaiverResource.OWNERS_PATH;
import static com.sonatype.insight.brain.api.v2.ApiPolicyWaiverResource.TRANSITIVE_VIOLATIONS_BY_SCAN_ID_PATH;
import static com.sonatype.insight.brain.api.v2.ApiPolicyWaiverResource.TRANSITIVE_VIOLATIONS_BY_STAGE_ID_PATH;
import static com.sonatype.insight.brain.model.repository.RepositoryContainer.REPOSITORY_CONTAINER_ID;
import static com.sonatype.insight.brain.report.ReportTestUtils.zipReportDir;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiPolicyWaiverResourceAuditTest
    extends AbstractAuditTest
{
  private PolicyWaiverDAO policyWaiverDAO;

  private Organization org;

  private Application app;

  private Policy policy;

  private PolicyEvaluation policyEvaluation;

  private PolicyViolation policyViolation;

  @Before
  public void setUpPolicyViolation() {
    policyWaiverDAO = lookup(PolicyWaiverDAO.class);

    org = tempEntity.newOrganization();
    app = tempEntity.newApplication(org.getId());
    policy = tempEntity.newPolicy();

    policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan1");
    policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
  }

  @Test
  public void testDeletePolicyWaiver_Application() throws Exception {
    ConditionFact conditionFact = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID, 0, "foo", "bar");
    conditionFact.setTriggerJson("{\"conditionIndex\":1,\"trigger\":{\"refId\":\"1234\",\"severity\":5.7}}");
    ConstraintFact constraintFact = new ConstraintFact("id", "name", LogicalOperator.AND.toString());
    constraintFact.addConditionFact(conditionFact);

    PolicyWaiver policyWaiver =
        tempEntity.newWaiver("0b", policy.getId(), app.getId(), Collections.singletonList(constraintFact));

    restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getId(), policyWaiver.getId())
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_WAIVER, null);
    assertPolicyWaiverData(auditDTO, policyWaiver);
    assertApplicationData(auditDTO, app);
  }

  @Test
  public void testDeletePolicyWaiver_Application_NullHashCode_NullConstraintFacts() throws Exception {
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), app.getId());

    restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getId(), policyWaiver.getId())
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_WAIVER, null);
    assertPolicyWaiverData(auditDTO, policyWaiver);
    assertApplicationData(auditDTO, app);
  }

  @Test
  public void testDeletePolicyWaiver_Application_Unauthorized() throws Exception {
    restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getId(), "policy-waiver-id")
        .with(unauthorizedUser())
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_WAIVER, "unauthorized");
    assertApplicationData(auditDTO, app);
  }

  @Test
  public void testDeletePolicyWaiver_Organization() throws Exception {
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), org.getId());

    restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.ORGANIZATION, org.getId(), policyWaiver.getId())
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_WAIVER, null);
    assertPolicyWaiverData(auditDTO, policyWaiver);
    assertOrganizationData(auditDTO, org);
  }

  @Test
  public void testDeletePolicyWaiver_Organization_Unauthorized() throws Exception {
    restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.ORGANIZATION, org.getId(), "policy-waiver-id")
        .with(unauthorizedUser())
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_WAIVER, "unauthorized");
    assertOrganizationData(auditDTO, org);
  }

  @Test
  public void testDeletePolicyWaiver_Repository() throws Exception {
    Repository repository = tempEntity.newRepository();
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), repository.getId());

    restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.REPOSITORY, repository.getId(), policyWaiver.getId())
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_WAIVER, null);
    assertPolicyWaiverData(auditDTO, policyWaiver);
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  public void testDeletePolicyWaiver_Repository_Unauthorized() throws Exception {
    Repository repository = tempEntity.newRepository();

    restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.REPOSITORY, repository.getId(), "policy-waiver-id")
        .with(unauthorizedUser())
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_WAIVER, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  public void testDeletePolicyWaiver_RepositoryContainer() throws Exception {
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), REPOSITORY_CONTAINER_ID);

    restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID, policyWaiver.getId())
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_WAIVER, null);
    assertPolicyWaiverData(auditDTO, policyWaiver);
    assertRepositoryContainerData(auditDTO);
  }

  @Test
  public void testDeletePolicyWaiver_RepositoryContainer_Unauthorized() throws Exception {
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
  public void testGetPolicyWaivers_Application() throws Exception {
    ConditionFact conditionFact = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID, 0, "foo", "bar");
    conditionFact.setTriggerJson("{\"conditionIndex\":1,\"trigger\":{\"refId\":\"1234\",\"severity\":5.7}}");
    ConstraintFact constraintFact = new ConstraintFact("id", "name", LogicalOperator.AND.toString());
    constraintFact.addConditionFact(conditionFact);

    PolicyWaiver policyWaiver =
        tempEntity.newWaiver("0b", policy.getId(), app.getId(), Collections.singletonList(constraintFact));

    restRequest().path(OWNERS_PATH).parameter(OwnerType.APPLICATION, app.getId()).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_WAIVER, null);
    assertPolicyWaiverData(auditDTO, policyWaiver);
    assertApplicationData(auditDTO, app);
  }

  @Test
  public void testGetPolicyWaivers_Application_Unauthorized() throws Exception {
    restRequest().path(OWNERS_PATH).parameter(OwnerType.APPLICATION, app.getId()).with(unauthorizedUser()).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_WAIVER, "unauthorized");
    assertApplicationData(auditDTO, app);
  }

  @Test
  public void testGetPolicyWaivers_Organization() throws Exception {
    ConditionFact conditionFact = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID, 0, "foo", "bar");
    conditionFact.setTriggerJson("{\"conditionIndex\":1,\"trigger\":{\"refId\":\"1234\",\"severity\":5.7}}");
    ConstraintFact constraintFact = new ConstraintFact("id", "name", LogicalOperator.AND.toString());
    constraintFact.addConditionFact(conditionFact);

    PolicyWaiver policyWaiver =
        tempEntity.newWaiver("0b", policy.getId(), org.getId(), Collections.singletonList(constraintFact));

    restRequest().path(OWNERS_PATH).parameter(OwnerType.ORGANIZATION, org.getId()).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_WAIVER, null);
    assertPolicyWaiverData(auditDTO, policyWaiver);
    assertOrganizationData(auditDTO, org);
  }

  @Test
  public void testGetPolicyWaivers_Organization_Unauthorized() throws Exception {
    restRequest().path(OWNERS_PATH).parameter(OwnerType.ORGANIZATION, org.getId()).with(unauthorizedUser()).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_WAIVER, "unauthorized");
    assertOrganizationData(auditDTO, org);
  }

  @Test
  public void testGetPolicyWaivers_Repository() throws Exception {
    Repository repository = tempEntity.newRepository();
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), repository.getId());

    restRequest().path(OWNERS_PATH).parameter(OwnerType.REPOSITORY, repository.getId()).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_WAIVER, null);
    assertPolicyWaiverData(auditDTO, policyWaiver);
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  public void testGetPolicyWaivers_Repository_Unauthorized() throws Exception {
    Repository repository = tempEntity.newRepository();
    restRequest().path(OWNERS_PATH).parameter(OwnerType.REPOSITORY, repository.getId()).with(unauthorizedUser()).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_WAIVER, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  public void testGetPolicyWaivers_RepositoryContainer() throws Exception {
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
  public void testGetPolicyWaivers_RepositoryContainer_Unauthorized() throws Exception {
    restRequest().path(OWNERS_PATH)
        .parameter(OwnerType.REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID)
        .with(unauthorizedUser())
        .get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_WAIVER, "unauthorized");
    assertRepositoryContainerData(auditDTO);
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_Application() throws Exception {
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
  public void testAddPolicyWaiverByPolicyViolationId_Organization() throws Exception {
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
  public void testAddPolicyWaiverByPolicyViolationId_RootOrganization() throws Exception {
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
  public void testAddPolicyWaiverByPolicyViolationId_Application_Unauthorized() throws Exception {
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
  public void testAddPolicyWaiverByPolicyViolationId_Organization_Unauthorized() throws Exception {
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
  public void testAddPolicyWaiverByPolicyViolationId_RootOrganization_Unauthorized() throws Exception {
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
  public void testAddWaiverToTransitivePolicyViolationsByAppScanComponent() throws Exception {
    ReportTestUtils.createReportFile(app.getId(), policyEvaluation.getScanId(),
        zipReportDir("/ApiPolicyWaiverResourceAuditTest/report", tempDir),
        getCLMServer().getInstance(InsightWork.class));

    ComponentIdentifier direct = ComponentIdentifier.createMavenCoordinates("g", "direct", "v", "", "e");
    ComponentIdentifier transitive = ComponentIdentifier.createMavenCoordinates("g", "transitive", "v", "", "e");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy, transitive, "hash2");

    ReportHelper.createPolicyThreats(
        getCLMServer().getInstance(InsightWork.class),
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
  public void testAddWaiverToTransitivePolicyViolationsByOwnerStageComponent() throws Exception {
    ReportTestUtils.createReportFile(app.getId(), policyEvaluation.getScanId(),
        zipReportDir("/ApiPolicyWaiverResourceAuditTest/report", tempDir),
        getCLMServer().getInstance(InsightWork.class));
    ComponentIdentifier direct = ComponentIdentifier.createMavenCoordinates("g", "direct", "v", "", "e");
    ComponentIdentifier transitive = ComponentIdentifier.createMavenCoordinates("g", "transitive", "v", "", "e");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy, transitive, "hash2");
    ReportHelper.createPolicyThreats(
        getCLMServer().getInstance(InsightWork.class),
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
  public void testAddPolicyWaiverByPolicyViolationId_Repository() throws Exception {
    Repository repository = tempEntity.newRepository();
    policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());
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
  public void testAddPolicyWaiverByPolicyViolationId_Repository_Unauthorized() throws Exception {
    Repository repository = tempEntity.newRepository();
    policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());
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
  public void testAddPolicyWaiverByPolicyViolationId_RepositoryContainer() throws Exception {
    Repository repository = tempEntity.newRepository();
    policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());
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
  public void testAddPolicyWaiverByPolicyViolationId_RepositoryContainer_Unauthorized() throws Exception {
    Repository repository = tempEntity.newRepository();
    policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());
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

  @Override
  public HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.POLICY_WAIVER_PATH);
  }

  @Test
  public void testGetPolicyWaiver_Application() throws Exception {
    ConditionFact conditionFact = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID, 0, "foo", "bar");
    conditionFact.setTriggerJson("{\"conditionIndex\":1,\"trigger\":{\"refId\":\"1234\",\"severity\":5.7}}");
    ConstraintFact constraintFact = new ConstraintFact("id", "name", LogicalOperator.AND.toString());
    constraintFact.addConditionFact(conditionFact);

    PolicyWaiver policyWaiver =
        tempEntity.newWaiver("0b", policy.getId(), app.getId(), Collections.singletonList(constraintFact));

    restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getId(), policyWaiver.getId())
        .get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_WAIVER, null);
    assertPolicyWaiverData(auditDTO, policyWaiver);
    assertApplicationData(auditDTO, app);
  }

  @Test
  public void testGetPolicyWaiver_Application_Unauthorized() throws Exception {
    restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getId(), "policyWaiverHash")
        .with(unauthorizedUser())
        .get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_WAIVER, "unauthorized");
    assertApplicationData(auditDTO, app);
  }

  @Test
  public void testGetPolicyWaiver_Organization() throws Exception {
    ConditionFact conditionFact = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID, 0, "foo", "bar");
    conditionFact.setTriggerJson("{\"conditionIndex\":1,\"trigger\":{\"refId\":\"1234\",\"severity\":5.7}}");
    ConstraintFact constraintFact = new ConstraintFact("id", "name", LogicalOperator.AND.toString());
    constraintFact.addConditionFact(conditionFact);

    PolicyWaiver policyWaiver =
        tempEntity.newWaiver("0b", policy.getId(), org.getId(), Collections.singletonList(constraintFact));

    restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.ORGANIZATION, org.getId(), policyWaiver.getId())
        .get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_WAIVER, null);
    assertPolicyWaiverData(auditDTO, policyWaiver);
    assertOrganizationData(auditDTO, org);
  }

  @Test
  public void testGetPolicyWaiver_Organization_Unauthorized() throws Exception {
    restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.ORGANIZATION, org.getId(), "policyWaiverHash")
        .with(unauthorizedUser())
        .get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_WAIVER, "unauthorized");
    assertOrganizationData(auditDTO, org);
  }

  @Test
  public void testGetPolicyWaiver_Repository() throws Exception {
    Repository repository = tempEntity.newRepository();
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), repository.getId());

    restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.REPOSITORY, repository.getId(), policyWaiver.getId())
        .get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_WAIVER, null);
    assertPolicyWaiverData(auditDTO, policyWaiver);
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  public void testGetPolicyWaiver_Repository_Unauthorized() throws Exception {
    Repository repository = tempEntity.newRepository();

    restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.REPOSITORY, repository.getId(), "policyWaiverHash")
        .with(unauthorizedUser())
        .get();

    restRequest().path(OWNERS_PATH).parameter(OwnerType.REPOSITORY, repository.getId()).with(unauthorizedUser()).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_WAIVER, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  public void testGetPolicyWaiver_RepositoryContainer() throws Exception {
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), REPOSITORY_CONTAINER_ID);

    restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID, policyWaiver.getId())
        .get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_WAIVER, null);
    assertPolicyWaiverData(auditDTO, policyWaiver);
    assertRepositoryContainerData(auditDTO);
  }

  @Test
  public void testGetPolicyWaiver_RepositoryContainer_Unauthorized() throws Exception {
    restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID, "policyWaiverHash")
        .with(unauthorizedUser())
        .get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_WAIVER, "unauthorized");
    assertRepositoryContainerData(auditDTO);
  }

  @Test
  public void testUpdatePolicyWaiver_Application() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(application.getId());
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), application.getId());
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
  public void testUpdatePolicyWaiver_Application_Unauthorized() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(application.getId());
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), application.getId());
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
  public void testUpdatePolicyWaiver_Organization() throws Exception {
    Organization organization = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(organization.getId());
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), organization.getId());
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
  public void testUpdatePolicyWaiver_Organization_Unauthorized() throws Exception {
    Organization organization = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(organization.getId());
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), organization.getId());
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
  public void testUpdatePolicyWaiver_Repository() throws Exception {
    Repository repository = tempEntity.newRepository();
    Policy policy = tempEntity.newPolicy(repository.getId());
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), repository.getId());
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
  public void testUpdatePolicyWaiver_Repository_Unauthorized() throws Exception {
    Repository repository = tempEntity.newRepository();
    Policy policy = tempEntity.newPolicy(repository.getId());
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), repository.getId());
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
  public void testUpdatePolicyWaiver_RepositoryManager() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Policy policy = tempEntity.newPolicy(repositoryManager.getId());
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), repositoryManager.getId());
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
  public void testUpdatePolicyWaiver_RepositoryManager_Unauthorized() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Policy policy = tempEntity.newPolicy(repositoryManager.getId());
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), repositoryManager.getId());
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
  public void testUpdatePolicyWaiver_RepositoryContainer() throws Exception {
    Policy policy = tempEntity.newPolicy(REPOSITORY_CONTAINER_ID);
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), REPOSITORY_CONTAINER_ID);
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
  public void testUpdatePolicyWaiver_RepositoryContainer_Unauthorized() throws Exception {
    Policy policy = tempEntity.newPolicy(REPOSITORY_CONTAINER_ID);
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), REPOSITORY_CONTAINER_ID);
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
}
