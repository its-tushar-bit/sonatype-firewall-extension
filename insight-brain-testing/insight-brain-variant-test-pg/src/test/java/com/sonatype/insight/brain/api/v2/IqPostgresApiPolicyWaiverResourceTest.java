/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.UnaryOperator;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiBulkWaiversDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRequestPolicyWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiWaiverOptionsDTO;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiComponentPolicyWaiversDTO;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.AbstractPolicyViolation;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverReason;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.brain.variant.IqPostgresTest;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.json.store.JsonUtils;

import org.apache.commons.lang.time.DateUtils;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.api.v2.ApiPolicyWaiverResource.BY_POLICY_VIOLATION_ID_PATH;
import static com.sonatype.insight.brain.api.v2.ApiPolicyWaiverResource.BY_POLICY_WAIVER_ID_PATH;
import static com.sonatype.insight.brain.api.v2.ApiPolicyWaiverResource.OWNERS_PATH;
import static com.sonatype.insight.brain.api.v2.ApiPolicyWaiverResource.TRANSITIVE_VIOLATIONS_BY_SCAN_ID_PATH;
import static com.sonatype.insight.brain.api.v2.ApiPolicyWaiverResource.TRANSITIVE_VIOLATIONS_BY_STAGE_ID_PATH;
import static com.sonatype.insight.brain.api.v2.service.ApiPolicyWaiverService.MAX_BULK_WAIVER_VIOLATIONS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_COMPONENTS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
import static com.sonatype.insight.brain.model.repository.RepositoryContainer.REPOSITORY_CONTAINER_ID;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@IqPostgresTest
class IqPostgresApiPolicyWaiverResourceTest
{
  // Injected by IqPostgresServerExtension: the extension owns the shared, reused server.
  private IqTestContext ctx;

  private RepositoryManagerDAO repositoryManagerDAO;

  private PolicyWaiverDAO policyWaiverDAO;

  private PolicyViolationDAO policyViolationDAO;

  @BeforeEach
  void setUp() {
    repositoryManagerDAO = ctx.lookup(RepositoryManagerDAO.class);
    policyWaiverDAO = ctx.lookup(PolicyWaiverDAO.class);
    policyViolationDAO = ctx.lookup(PolicyViolationDAO.class);
  }

  @Test
  void testDeletePolicyWaiver() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();
    Policy policy = ctx.tempEntity().newPolicy(application);
    PolicyWaiver policyWaiver = ctx.tempEntity().newWaiver(policy.getId(), application.getId());

    HttpResponse response = restRequest()
        .path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, application.getId(), policyWaiver.getId())
        .delete();

    ctx.assertResponseStatus(204, response);
    assertThat(policyWaiverDAO.getById(policyWaiver.getId())).isNull();
  }

  @Test
  void testGetPolicyWaivers_Application() throws Exception {
    Instant now = Instant.now();
    Date today = Date.from(now);
    Date aWeekFromNow = Date.from(now.plus(7, ChronoUnit.DAYS));
    Application application = ctx.tempEntity().newApplicationWithParent();
    Policy policy = ctx.tempEntity().newPolicy(application);

    TriggerReference triggerReference = new TriggerReference(TriggerReference.Type.SECURITY_VULNERABILITY_REFID,
        "vulnerability-1");
    ConditionFact conditionFact = new ConditionFact("condition type id", 0, "summary", "reason", triggerReference);
    ConstraintFact constraintFact = new ConstraintFact("constraint id", "constraint name", "operator", conditionFact);
    PolicyWaiver policyWaiver = ctx.tempEntity()
        .newWaiver("hash", policy.getId(), application.getId(),
            singletonList(constraintFact), EXACT_COMPONENT, "a comment", today, aWeekFromNow);

    HttpResponse response = restRequest().path(OWNERS_PATH).parameter(OwnerType.APPLICATION, application.getId()).get();

    ctx.assertResponseStatus(200, response);

    List<ApiPolicyWaiverDTO> policyWaiverDtoList = Arrays.asList(response.getBody(ApiPolicyWaiverDTO[].class));
    assertThat(policyWaiverDtoList).hasSize(1);

    ApiPolicyWaiverDTO apiPolicyWaiverDTO = policyWaiverDtoList.get(0);
    assertThat(apiPolicyWaiverDTO.policyWaiverId).isEqualTo(policyWaiver.getId());
    assertThat(apiPolicyWaiverDTO.comment).isEqualTo(policyWaiver.getComment());
    assertThat(apiPolicyWaiverDTO.createTime).isEqualTo(policyWaiver.getCreateTime());
    assertThat(apiPolicyWaiverDTO.hash).isEqualTo("hash");
    assertThat(apiPolicyWaiverDTO.policyId).isEqualTo(policyWaiver.getPolicyId());
    assertThat(apiPolicyWaiverDTO.comment).isEqualTo("a comment");
    assertThat(apiPolicyWaiverDTO.scopeOwnerId).isEqualTo(application.getId());
    assertThat(apiPolicyWaiverDTO.scopeOwnerName).isEqualTo(application.getName());
    assertThat(apiPolicyWaiverDTO.scopeOwnerType).isEqualTo(OwnerType.APPLICATION.toString());
    assertThat(apiPolicyWaiverDTO.expiryTime).isEqualTo(aWeekFromNow);
    assertThat(apiPolicyWaiverDTO.vulnerabilityId).isEqualTo("vulnerability-1");
    assertThat(apiPolicyWaiverDTO.creatorId).isNotNull();
    assertThat(apiPolicyWaiverDTO.creatorId).isEqualTo("testuser");
    assertThat(apiPolicyWaiverDTO.creatorName).isEqualTo("Test User");
  }

  @Test
  void testGetPolicyWaivers_Organization() throws Exception {
    Instant now = Instant.now();
    Date today = Date.from(now);
    Date aWeekFromNow = Date.from(now.plus(7, ChronoUnit.DAYS));

    Organization organization = ctx.tempEntity().newOrganization();
    Policy policy = ctx.tempEntity().newPolicy(organization);
    PolicyWaiver policyWaiver = ctx.tempEntity()
        .newWaiver("hash", policy.getId(), organization.getId(),
            null, EXACT_COMPONENT, "a comment in org waiver", today, aWeekFromNow);

    HttpResponse response =
        restRequest().path(OWNERS_PATH).parameter(OwnerType.ORGANIZATION, organization.getId()).get();

    ctx.assertResponseStatus(200, response);

    List<ApiPolicyWaiverDTO> policyWaiverDtoList = Arrays.asList(response.getBody(ApiPolicyWaiverDTO[].class));
    assertThat(policyWaiverDtoList).hasSize(1);

    ApiPolicyWaiverDTO apiPolicyWaiverDTO = policyWaiverDtoList.get(0);
    assertThat(apiPolicyWaiverDTO.policyWaiverId).isEqualTo(policyWaiver.getId());
    assertThat(apiPolicyWaiverDTO.comment).isEqualTo("a comment in org waiver");
    assertThat(apiPolicyWaiverDTO.createTime).isEqualTo(policyWaiver.getCreateTime());
    assertThat(apiPolicyWaiverDTO.hash).isEqualTo("hash");
    assertThat(apiPolicyWaiverDTO.policyId).isEqualTo(policyWaiver.getPolicyId());
    assertThat(apiPolicyWaiverDTO.scopeOwnerId).isEqualTo(organization.getId());
    assertThat(apiPolicyWaiverDTO.scopeOwnerName).isEqualTo(organization.getName());
    assertThat(apiPolicyWaiverDTO.scopeOwnerType).isEqualTo(OwnerType.ORGANIZATION.toString());
    assertThat(apiPolicyWaiverDTO.expiryTime).isEqualTo(aWeekFromNow);
  }

  @Test
  void testGetPolicyWaivers_Repository() throws Exception {
    Instant now = Instant.now();
    Date today = Date.from(now);
    Date aWeekFromNow = Date.from(now.plus(7, ChronoUnit.DAYS));

    Repository repository = ctx.tempEntity().newRepository();
    Policy policy = ctx.tempEntity().newPolicy();
    PolicyWaiver policyWaiver = ctx.tempEntity()
        .newWaiver("hash", policy.getId(), repository.getId(),
            null, EXACT_COMPONENT, "comment", today, aWeekFromNow);

    HttpResponse response = restRequest().path(OWNERS_PATH).parameter(OwnerType.REPOSITORY, repository.getId()).get();

    ctx.assertResponseStatus(200, response);

    List<ApiPolicyWaiverDTO> policyWaiverDtoList = Arrays.asList(response.getBody(ApiPolicyWaiverDTO[].class));
    assertThat(policyWaiverDtoList).hasSize(1);

    ApiPolicyWaiverDTO apiPolicyWaiverDTO = policyWaiverDtoList.get(0);
    assertThat(apiPolicyWaiverDTO.policyWaiverId).isEqualTo(policyWaiver.getId());
    assertThat(apiPolicyWaiverDTO.comment).isEqualTo("comment");
    assertThat(apiPolicyWaiverDTO.createTime).isEqualTo(policyWaiver.getCreateTime());
    assertThat(apiPolicyWaiverDTO.hash).isEqualTo(policyWaiver.getHash());
    assertThat(apiPolicyWaiverDTO.policyId).isEqualTo(policyWaiver.getPolicyId());
    assertThat(apiPolicyWaiverDTO.scopeOwnerId).isEqualTo(repository.getId());
    assertThat(apiPolicyWaiverDTO.scopeOwnerName).isEqualTo(repository.getName());
    assertThat(apiPolicyWaiverDTO.scopeOwnerType).isEqualTo(OwnerType.REPOSITORY.toString());
    assertThat(apiPolicyWaiverDTO.expiryTime).isEqualTo(aWeekFromNow);
  }

  @Test
  void testGetPolicyWaivers_RepositoryManager() throws Exception {
    Instant now = Instant.now();
    Date today = Date.from(now);
    Date aWeekFromNow = Date.from(now.plus(7, ChronoUnit.DAYS));

    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Policy policy = ctx.tempEntity().newPolicy();
    PolicyWaiver policyWaiver = ctx.tempEntity()
        .newWaiver("hash", policy.getId(), repositoryManager.getId(), null,
            EXACT_COMPONENT, "comment", today, aWeekFromNow);

    HttpResponse response =
        restRequest().path(OWNERS_PATH).parameter(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId()).get();

    ctx.assertResponseStatus(200, response);

    List<ApiPolicyWaiverDTO> policyWaiverDtoList = Arrays.asList(response.getBody(ApiPolicyWaiverDTO[].class));
    assertThat(policyWaiverDtoList).hasSize(1);

    ApiPolicyWaiverDTO apiPolicyWaiverDTO = policyWaiverDtoList.get(0);
    assertThat(apiPolicyWaiverDTO.policyWaiverId).isEqualTo(policyWaiver.getId());
    assertThat(apiPolicyWaiverDTO.comment).isEqualTo("comment");
    assertThat(apiPolicyWaiverDTO.createTime).isEqualTo(policyWaiver.getCreateTime());
    assertThat(apiPolicyWaiverDTO.hash).isEqualTo(policyWaiver.getHash());
    assertThat(apiPolicyWaiverDTO.policyId).isEqualTo(policyWaiver.getPolicyId());
    assertThat(apiPolicyWaiverDTO.scopeOwnerId).isEqualTo(repositoryManager.getId());
    assertThat(apiPolicyWaiverDTO.scopeOwnerName).isEqualTo(repositoryManager.getName());
    assertThat(apiPolicyWaiverDTO.scopeOwnerType).isEqualTo(OwnerType.REPOSITORY_MANAGER.toString());
    assertThat(apiPolicyWaiverDTO.expiryTime).isEqualTo(aWeekFromNow);
  }

  @Test
  void testGetPolicyWaivers_RepositoryContainer() throws Exception {
    Instant now = Instant.now();
    Date aWeekFromNow = Date.from(now.plus(7, ChronoUnit.DAYS));

    // Container-image waivers are stored under the container-image APPLICATION id (not
    // REPOSITORY_CONTAINER_ID) and flagged is_for_container_image=true. The Firewall Containers →
    // Existing Waivers view (addressed via REPOSITORY_CONTAINER_ID) filters directly on that flag.
    Application containerApp = ctx.tempEntity().newApplicationWithParent();
    Policy policy = ctx.tempEntity().newPolicy();
    PolicyWaiver waiverToInsert = new PolicyWaiver("hash", policy.getId(), containerApp.getId(), "comment");
    waiverToInsert.setForContainerImage(true);
    waiverToInsert.setExpiryTime(aWeekFromNow);
    PolicyWaiver policyWaiver = ctx.tempEntity().newWaiver(waiverToInsert);

    HttpResponse response =
        restRequest().path(OWNERS_PATH).parameter(OwnerType.REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID).get();

    ctx.assertResponseStatus(200, response);

    List<ApiPolicyWaiverDTO> policyWaiverDtoList = Arrays.asList(response.getBody(ApiPolicyWaiverDTO[].class));
    assertThat(policyWaiverDtoList).hasSize(1);

    ApiPolicyWaiverDTO apiPolicyWaiverDTO = policyWaiverDtoList.get(0);
    assertThat(apiPolicyWaiverDTO.policyWaiverId).isEqualTo(policyWaiver.getId());
    assertThat(apiPolicyWaiverDTO.comment).isEqualTo(policyWaiver.getComment());
    assertThat(apiPolicyWaiverDTO.createTime).isEqualTo(policyWaiver.getCreateTime());
    assertThat(apiPolicyWaiverDTO.hash).isEqualTo(policyWaiver.getHash());
    assertThat(apiPolicyWaiverDTO.policyId).isEqualTo(policyWaiver.getPolicyId());
    // scopeOwner is re-resolved to the container-image application per buildPolicyWaiverDTOsPerOwner,
    // not the REPOSITORY_CONTAINER owner the caller addressed the list through.
    assertThat(apiPolicyWaiverDTO.scopeOwnerId).isEqualTo(containerApp.getId());
    assertThat(apiPolicyWaiverDTO.scopeOwnerName).isEqualTo(containerApp.getName());
    assertThat(apiPolicyWaiverDTO.scopeOwnerType).isEqualTo(OwnerType.APPLICATION.toString());
    assertThat(apiPolicyWaiverDTO.expiryTime).isEqualTo(aWeekFromNow);
  }

  @Test
  void testAddPolicyWaiverByPolicyViolationId_Application() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    Policy policy = ctx.tempEntity().newPolicy(app);

    PolicyEvaluation policyEvaluation =
        ctx.tempEntity().newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    PolicyViolation policyViolation = ctx.tempEntity()
        .newPolicyViolation(policyEvaluation, policy, "g1", "a1", "v1", "h1", "r1");

    ApiWaiverOptionsDTO waiverOptionsDTO = new ApiWaiverOptionsDTO();
    waiverOptionsDTO.comment = "waiver comment";
    HttpResponse response = restRequest().path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getId(), policyViolation.getId())
        .body(waiverOptionsDTO, MediaType.APPLICATION_JSON)
        .post();

    ctx.assertResponseStatus(204, response);
    assertNonExpiringPolicyWaiver(app.getId(), policy, policyViolation, "waiver comment", policyViolation.getHash());
  }

  @Test
  void testAddPolicyWaiverByPolicyViolationId_Organization() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();
    Application app = ctx.tempEntity().newApplication(org.getId());
    Policy policy = ctx.tempEntity().newPolicy(app);

    PolicyEvaluation policyEvaluation =
        ctx.tempEntity().newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    PolicyViolation policyViolation = ctx.tempEntity()
        .newPolicyViolation(policyEvaluation, policy, "g1", "a1", "v1", "h1", "r1");

    ApiWaiverOptionsDTO waiverOptionsDTO = new ApiWaiverOptionsDTO();
    waiverOptionsDTO.comment = "waiver comment";
    HttpResponse response = restRequest().path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.ORGANIZATION, org.getId(), policyViolation.getId())
        .body(waiverOptionsDTO, MediaType.APPLICATION_JSON)
        .post();

    ctx.assertResponseStatus(204, response);
    assertNonExpiringPolicyWaiver(org.getId(), policy, policyViolation, "waiver comment", policyViolation.getHash());
  }

  @Test
  void testAddPolicyWaiverByPolicyViolationId_RootOrganization() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    Policy policy = ctx.tempEntity().newPolicy(app);

    PolicyEvaluation policyEvaluation =
        ctx.tempEntity().newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    PolicyViolation policyViolation = ctx.tempEntity()
        .newPolicyViolation(policyEvaluation, policy, "g1", "a1", "v1", "h1", "r1");

    ApiWaiverOptionsDTO waiverOptionsDTO = new ApiWaiverOptionsDTO();
    waiverOptionsDTO.comment = "waiver comment";
    HttpResponse response = restRequest().path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID, policyViolation.getId())
        .body(waiverOptionsDTO, MediaType.APPLICATION_JSON)
        .post();

    ctx.assertResponseStatus(204, response);
    assertNonExpiringPolicyWaiver(Organization.ROOT_ORGANIZATION_ID, policy, policyViolation, "waiver comment",
        policyViolation.getHash());
  }

  @Test
  void testAddPolicyWaiverByPolicyViolationId_ApplyToAllComponents() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    Policy policy = ctx.tempEntity().newPolicy(app);

    PolicyEvaluation policyEvaluation =
        ctx.tempEntity().newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    PolicyViolation policyViolation = ctx.tempEntity()
        .newPolicyViolation(policyEvaluation, policy, "g1", "a1", "v1", "h1", "r1");

    ApiWaiverOptionsDTO waiverOptionsDTO = new ApiWaiverOptionsDTO();
    waiverOptionsDTO.comment = "waiver comment";
    waiverOptionsDTO.applyToAllComponents = true;
    HttpResponse response = restRequest().path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getId(), policyViolation.getId())
        .body(waiverOptionsDTO, MediaType.APPLICATION_JSON)
        .post();

    ctx.assertResponseStatus(204, response);
    assertNonExpiringPolicyWaiver(app.getId(), policy, policyViolation, "waiver comment", null);
  }

  @Test
  void testAddPolicyWaiverByPolicyViolationId_NoRequestBody() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    Policy policy = ctx.tempEntity().newPolicy(app);

    PolicyEvaluation policyEvaluation =
        ctx.tempEntity().newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    PolicyViolation policyViolation = ctx.tempEntity()
        .newPolicyViolation(policyEvaluation, policy, "g1", "a1", "v1", "h1", "r1");

    HttpResponse response = restRequest().path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getId(), policyViolation.getId())
        .post();

    ctx.assertResponseStatus(204, response);
    assertNonExpiringPolicyWaiver(app.getId(), policy, policyViolation, null, policyViolation.getHash());
  }

  @Test
  void testAddPolicyWaiverByPolicyViolationId_ExpiresInFuture() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    Policy policy = ctx.tempEntity().newPolicy(app);

    PolicyEvaluation policyEvaluation =
        ctx.tempEntity().newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    PolicyViolation policyViolation = ctx.tempEntity()
        .newPolicyViolation(policyEvaluation, policy, "g1", "a1", "v1", "h1", "r1");

    ApiWaiverOptionsDTO waiverOptionsDTO = new ApiWaiverOptionsDTO();
    waiverOptionsDTO.comment = "waiver comment";
    Date expiryTime = DateTime.now().plusDays(7).toDate();
    waiverOptionsDTO.expiryTime = expiryTime;
    HttpResponse response = restRequest().path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getId(), policyViolation.getId())
        .body(waiverOptionsDTO, MediaType.APPLICATION_JSON)
        .post();

    ctx.assertResponseStatus(204, response);
    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getActiveByOwnerId(app.getId());
    assertThat(policyWaivers).isNotEmpty().hasSize(1);
    assertPolicyWaiver(app.getId(), policy, policyViolation, policyWaivers.get(0), "waiver comment",
        policyViolation.getHash(), expiryTime);
  }

  @Test
  void testAddPolicyWaiverByPolicyViolationId_Expired() {
    Application app = ctx.tempEntity().newApplicationWithParent();
    Policy policy = ctx.tempEntity().newPolicy(app);

    PolicyEvaluation policyEvaluation =
        ctx.tempEntity().newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    PolicyViolation policyViolation = ctx.tempEntity()
        .newPolicyViolation(policyEvaluation, policy, "g1", "a1", "v1", "h1", "r1");

    /*
     * Directly insert into db with expiry date in the past.
     * The api will now not allow creating waiver with expiry date in the past.
     */

    policyViolation = policyViolationDAO.getById(policyViolation.getId());
    policyViolationDAO.loadConstraintFacts(Collections.singletonList(policyViolation));
    Date expiryTime = DateTime.now().minusDays(1).toDate();
    String waiverComment = "some comment";
    PolicyWaiver policyWaiver = new PolicyWaiver("h1", policyViolation.getPolicyId(), app.getId(), waiverComment);
    policyWaiver.setConstraintFactsJson(policyViolation.getConstraintFactsJson());
    policyWaiver.setExpiryTime(expiryTime);

    policyWaiverDAO.insert(policyWaiver);

    // should not return a policy as the one existing is expired
    List<PolicyWaiver> activePolicyWaivers = policyWaiverDAO.getActiveByOwnerId(app.getId());
    assertThat(activePolicyWaivers).isEmpty();

    // getByOwnerId should still return the expired policy
    List<PolicyWaiver> allPolicyWaivers = policyWaiverDAO.getByOwnerId(app.getId());
    assertThat(allPolicyWaivers).hasSize(1);
    assertPolicyWaiver(app.getId(), policy, policyViolation, allPolicyWaivers.get(0), waiverComment,
        policyViolation.getHash(), expiryTime);
  }

  @Test
  void testAddWaiverToTransitivePolicyViolationsByAppScanComponent_ByComponentIdentifier() throws Exception {
    testAddWaiverToTransitivePolicyViolationsByAppScanComponent(request -> request.query("componentIdentifier",
        ComponentIdentifier.createMavenCoordinates("g", "direct", "v", "", "e")));
  }

  @Test
  void testAddWaiverToTransitivePolicyViolationsByAppScanComponent_ByPackageUrl() throws Exception {
    testAddWaiverToTransitivePolicyViolationsByAppScanComponent(
        request -> request.query("packageUrl", "pkg:maven/g/direct@v?type=e"));
  }

  @Test
  void testAddWaiverToTransitivePolicyViolationsByAppScanComponent_ByHash() throws Exception {
    testAddWaiverToTransitivePolicyViolationsByAppScanComponent(request -> request.query("hash", "hash1"));
  }

  void testAddWaiverToTransitivePolicyViolationsByAppScanComponent(
      UnaryOperator<HttpRequest> operator) throws Exception
  {
    Application app = ctx.tempEntity().newApplicationWithParent();
    PolicyEvaluation policyEvaluation = ctx.tempEntity().newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId");
    Policy policy = ctx.tempEntity().newPolicy(app);

    ComponentIdentifier componentIdentifierTransitive =
        ComponentIdentifier.createMavenCoordinates("g", "transitive", "v", "", "e");
    PolicyViolation policyViolationTransitive =
        ctx.tempEntity().newPolicyViolation(policyEvaluation, policy, componentIdentifierTransitive, "hash2");

    ctx.createReportFile(app.getId(), policyEvaluation.getScanId(), "/ApiPolicyWaiverResourceTest/report");

    ReportHelper.createPolicyThreats(
        ctx.insightWork(),
        app.getId(),
        policyEvaluation.getScanId(),
        Collections.singletonList(policyViolationTransitive));

    ApiWaiverOptionsDTO waiverOptionsDTO = new ApiWaiverOptionsDTO();
    waiverOptionsDTO.comment = "waiver comment";
    waiverOptionsDTO.expiryTime = new Date(System.currentTimeMillis() + 1000);

    HttpRequest request = restRequest()
        .path(TRANSITIVE_VIOLATIONS_BY_SCAN_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getPublicId(), policyEvaluation.getScanId())
        .body(waiverOptionsDTO, MediaType.APPLICATION_JSON);

    HttpResponse response = operator.apply(request).post();

    ctx.assertResponseStatus(204, response);

    List<PolicyWaiver> allPolicyWaivers = policyWaiverDAO.getByOwnerId(app.getId());
    assertThat(allPolicyWaivers).hasSize(1);
    assertPolicyWaiver(app.getId(), policy, policyViolationTransitive, allPolicyWaivers.get(0),
        waiverOptionsDTO.comment, policyViolationTransitive.getHash(), waiverOptionsDTO.expiryTime);
  }

  @Test
  void testAddWaiverToTransitivePolicyViolationsByOwnerStageComponent_ByComponentIdentifier() throws Exception {
    testAddWaiverToTransitivePolicyViolationsByOwnerStageComponent(request -> request.query("componentIdentifier",
        ComponentIdentifier.createMavenCoordinates("g", "direct", "v", "", "e")));
  }

  @Test
  void testAddWaiverToTransitivePolicyViolationsByOwnerStageComponent_ByPackageUrl() throws Exception {
    testAddWaiverToTransitivePolicyViolationsByOwnerStageComponent(
        request -> request.query("packageUrl", "pkg:maven/g/direct@v?type=e"));
  }

  @Test
  void testAddWaiverToTransitivePolicyViolationsByOwnerStageComponent_ByHash() throws Exception {
    testAddWaiverToTransitivePolicyViolationsByOwnerStageComponent(request -> request.query("hash", "hash1"));
  }

  void testAddWaiverToTransitivePolicyViolationsByOwnerStageComponent(
      UnaryOperator<HttpRequest> operator) throws Exception
  {
    Application app = ctx.tempEntity().newApplicationWithParent();
    PolicyEvaluation policyEvaluation = ctx.tempEntity().newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId");
    Policy policy = ctx.tempEntity().newPolicy(app);
    ComponentIdentifier componentIdentifierTransitive =
        ComponentIdentifier.createMavenCoordinates("g", "transitive", "v", "", "e");
    PolicyViolation policyViolationTransitive =
        ctx.tempEntity().newPolicyViolation(policyEvaluation, policy, componentIdentifierTransitive, "hash2");
    ctx.createReportFile(app.getId(), policyEvaluation.getScanId(), "/ApiPolicyWaiverResourceTest/report");
    ReportHelper.createPolicyThreats(
        ctx.insightWork(),
        app.getId(),
        policyEvaluation.getScanId(),
        Collections.singletonList(policyViolationTransitive));
    ApiWaiverOptionsDTO waiverOptionsDTO = new ApiWaiverOptionsDTO();
    waiverOptionsDTO.comment = "waiver comment";
    waiverOptionsDTO.expiryTime = new Date(System.currentTimeMillis() + 1000);

    HttpRequest request = restRequest()
        .path(TRANSITIVE_VIOLATIONS_BY_STAGE_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getPublicId(), BuildStageType.ID)
        .body(waiverOptionsDTO, MediaType.APPLICATION_JSON);

    HttpResponse response = operator.apply(request).post();
    ctx.assertResponseStatus(204, response);
    List<PolicyWaiver> allPolicyWaivers = policyWaiverDAO.getByOwnerId(app.getId());
    assertThat(allPolicyWaivers).hasSize(1);
    assertPolicyWaiver(app.getId(), policy, policyViolationTransitive, allPolicyWaivers.get(0),
        waiverOptionsDTO.comment, policyViolationTransitive.getHash(), waiverOptionsDTO.expiryTime);
  }

  @Test
  void testGetTransitivePolicyWaiversByAppScanComponent_ByComponentIdentifier() throws Exception {
    testGetTransitivePolicyWaiversByAppScanComponent(request -> request.query("componentIdentifier",
        ComponentIdentifier.createMavenCoordinates("g", "direct", "v", "", "e")));
  }

  @Test
  void testGetTransitivePolicyWaiversByAppScanComponent_ByPackageUrl() throws Exception {
    testGetTransitivePolicyWaiversByAppScanComponent(
        request -> request.query("packageUrl", "pkg:maven/g/direct@v?type=e"));
  }

  @Test
  void testGetTransitivePolicyWaiversByAppScanComponent_ByHash() throws Exception {
    testGetTransitivePolicyWaiversByAppScanComponent(request -> request.query("hash", "hash1"));
  }

  @Test
  void testAddPolicyWaiverByPolicyViolationId_Repository() throws Exception {
    Repository repository = ctx.tempEntity().newRepository();
    Policy policy = ctx.tempEntity().newPolicy(Organization.ROOT_ORGANIZATION_ID);

    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        ctx.tempEntity().newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    ApiWaiverOptionsDTO waiverOptionsDTO = new ApiWaiverOptionsDTO();
    waiverOptionsDTO.comment = "waiver comment";
    HttpResponse response = restRequest().path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.REPOSITORY, repository.getId(), proxyRepositoryPolicyViolation.getId())
        .body(waiverOptionsDTO, MediaType.APPLICATION_JSON)
        .post();

    ctx.assertResponseStatus(204, response);
    assertNonExpiringPolicyWaiver(repository.getId(), policy, proxyRepositoryPolicyViolation, "waiver comment",
        proxyRepositoryPolicyViolation.getHash());
  }

  @Test
  void testAddPolicyWaiverByPolicyViolationId_RepositoryManager() throws Exception {
    Repository repository = ctx.tempEntity().newRepository();
    RepositoryManager repositoryManager = repositoryManagerDAO.getById(repository.getRepositoryManagerId());
    Policy policy = ctx.tempEntity().newPolicy(Organization.ROOT_ORGANIZATION_ID);

    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        ctx.tempEntity().newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    ApiWaiverOptionsDTO waiverOptionsDTO = new ApiWaiverOptionsDTO();
    waiverOptionsDTO.comment = "waiver comment";
    HttpResponse response = restRequest().path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId(), proxyRepositoryPolicyViolation.getId())
        .body(waiverOptionsDTO, MediaType.APPLICATION_JSON)
        .post();

    ctx.assertResponseStatus(204, response);
    assertNonExpiringPolicyWaiver(repositoryManager.getId(), policy, proxyRepositoryPolicyViolation, "waiver comment",
        proxyRepositoryPolicyViolation.getHash());
  }

  @Test
  void testAddPolicyWaiverByPolicyViolationId_RepositoryContainer() throws Exception {
    Repository repository = ctx.tempEntity().newRepository();
    Policy policy = ctx.tempEntity().newPolicy(Organization.ROOT_ORGANIZATION_ID);

    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        ctx.tempEntity().newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    ApiWaiverOptionsDTO waiverOptionsDTO = new ApiWaiverOptionsDTO();
    waiverOptionsDTO.comment = "waiver comment";
    HttpResponse response = restRequest()
        .path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.REPOSITORY_CONTAINER,
            RepositoryContainer.REPOSITORY_CONTAINER_ID, proxyRepositoryPolicyViolation.getId())
        .body(waiverOptionsDTO, MediaType.APPLICATION_JSON)
        .post();

    ctx.assertResponseStatus(204, response);
    assertNonExpiringPolicyWaiver(RepositoryContainer.REPOSITORY_CONTAINER_ID, policy, proxyRepositoryPolicyViolation,
        "waiver comment", proxyRepositoryPolicyViolation.getHash());
  }

  private void testGetTransitivePolicyWaiversByAppScanComponent(UnaryOperator<HttpRequest> operator) throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    PolicyEvaluation policyEvaluation = ctx.tempEntity().newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId");
    ctx.createReportFile(app.getId(), policyEvaluation.getScanId(), "/ApiPolicyWaiverResourceTest/report");
    Policy policy = ctx.tempEntity().newPolicy();
    PolicyWaiver policyWaiver =
        ctx.tempEntity().newWaiver("hash2", policy.getId(), app.getId(), null, EXACT_COMPONENT, null);

    HttpRequest request = restRequest()
        .path(TRANSITIVE_VIOLATIONS_BY_SCAN_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getPublicId(), policyEvaluation.getScanId());

    HttpResponse response = operator.apply(request).get();
    ctx.assertResponseStatus(200, response);
    ApiComponentPolicyWaiversDTO result = response.getBody(ApiComponentPolicyWaiversDTO.class);
    assertThat(result).isNotNull();
    assertThat(result.componentPolicyWaivers).isNotNull();
    assertThat(result.componentPolicyWaivers).extracting(componentPolicyWaiver -> componentPolicyWaiver.policyWaiverId)
        .containsExactly(policyWaiver.getId());
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.POLICY_WAIVER_PATH);
  }

  private void assertNonExpiringPolicyWaiver(
      String ownerId,
      Policy policy,
      AbstractPolicyViolation abstractPolicyViolation,
      String comment,
      String hash)
  {
    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getActiveByOwnerId(ownerId);
    assertThat(policyWaivers).isNotEmpty().hasSize(1);
    assertPolicyWaiver(ownerId, policy, abstractPolicyViolation, policyWaivers.get(0), comment, hash, null);
  }

  private void assertPolicyWaiver(
      String ownerId,
      Policy policy,
      AbstractPolicyViolation abstractPolicyViolation,
      PolicyWaiver policyWaiver,
      String comment,
      String hash,
      Date expiryTime)
  {
    assertThat(policyWaiver).isNotNull();
    assertThat(policyWaiver.getId()).isNotNull();
    assertThat(policyWaiver.getOwnerId()).isEqualTo(ownerId);
    assertThat(policyWaiver.getHash()).isEqualTo(hash);
    assertThat(policyWaiver.getComment()).isEqualTo(comment);
    assertThat(policyWaiver.getPolicyId()).isEqualTo(policy.getId());
    assertThat(policyWaiver.getCreateTime()).isNotNull();
    assertThat(policyWaiver.getExpiryTime()).isEqualTo(expiryTime);
    assertThat(policyWaiver.getConstraintFactsJson()).isEqualTo(abstractPolicyViolation.getConstraintFactsJson());
  }

  @Test
  void testGetPolicyWaiver_Application() throws Exception {
    Instant now = Instant.now();
    Date today = Date.from(now);
    Date aWeekFromNow = Date.from(now.plus(7, ChronoUnit.DAYS));
    Application application = ctx.tempEntity().newApplicationWithParent();
    Policy policy = ctx.tempEntity().newPolicy(application);

    TriggerReference triggerReference = new TriggerReference(TriggerReference.Type.SECURITY_VULNERABILITY_REFID,
        "vulnerability-1");
    ConditionFact conditionFact = new ConditionFact("condition type id", 0, "summary", "reason", triggerReference);
    ConstraintFact constraintFact = new ConstraintFact("constraint id", "constraint name", "operator", conditionFact);
    PolicyWaiver policyWaiver = ctx.tempEntity()
        .newWaiver("hash", policy.getId(), application.getId(),
            singletonList(constraintFact), EXACT_COMPONENT, "a comment", today, aWeekFromNow);

    HttpResponse response = restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, application.getId(), policyWaiver.getId())
        .get();

    ctx.assertResponseStatus(200, response);

    ApiPolicyWaiverDTO apiPolicyWaiverDTO = response.getBody(ApiPolicyWaiverDTO.class);
    assertThat(apiPolicyWaiverDTO.policyWaiverId).isEqualTo(policyWaiver.getId());
    assertThat(apiPolicyWaiverDTO.comment).isEqualTo(policyWaiver.getComment());
    assertThat(apiPolicyWaiverDTO.createTime).isEqualTo(policyWaiver.getCreateTime());
    assertThat(apiPolicyWaiverDTO.hash).isEqualTo("hash");
    assertThat(apiPolicyWaiverDTO.policyId).isEqualTo(policyWaiver.getPolicyId());
    assertThat(apiPolicyWaiverDTO.comment).isEqualTo("a comment");
    assertThat(apiPolicyWaiverDTO.scopeOwnerId).isEqualTo(application.getId());
    assertThat(apiPolicyWaiverDTO.scopeOwnerName).isEqualTo(application.getName());
    assertThat(apiPolicyWaiverDTO.scopeOwnerType).isEqualTo(OwnerType.APPLICATION.toString());
    assertThat(apiPolicyWaiverDTO.expiryTime).isEqualTo(aWeekFromNow);
    assertThat(apiPolicyWaiverDTO.vulnerabilityId).isEqualTo("vulnerability-1");
    assertThat(apiPolicyWaiverDTO.threatLevel).isEqualTo(policy.getThreatLevel());
    assertThat(apiPolicyWaiverDTO.creatorId).isNotNull();
    assertThat(apiPolicyWaiverDTO.creatorId).isEqualTo("testuser");
    assertThat(apiPolicyWaiverDTO.creatorName).isEqualTo("Test User");
    assertThat(apiPolicyWaiverDTO.constraintFactsJson).isEqualTo(
        JsonUtils.writeUnformatted(singletonList(constraintFact)));
  }

  @Test
  void testGetPolicyWaiver_Organization() throws Exception {
    Instant now = Instant.now();
    Date today = Date.from(now);
    Date aWeekFromNow = Date.from(now.plus(7, ChronoUnit.DAYS));

    Organization organization = ctx.tempEntity().newOrganization();
    Policy policy = ctx.tempEntity().newPolicy(organization);
    PolicyWaiver policyWaiver = ctx.tempEntity()
        .newWaiver("hash", policy.getId(), organization.getId(),
            null, EXACT_COMPONENT, "a comment in org waiver", today, aWeekFromNow);

    HttpResponse response = restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.ORGANIZATION, organization.getId(), policyWaiver.getId())
        .get();

    ctx.assertResponseStatus(200, response);

    ApiPolicyWaiverDTO apiPolicyWaiverDTO = response.getBody(ApiPolicyWaiverDTO.class);

    assertThat(apiPolicyWaiverDTO.policyWaiverId).isEqualTo(policyWaiver.getId());
    assertThat(apiPolicyWaiverDTO.comment).isEqualTo("a comment in org waiver");
    assertThat(apiPolicyWaiverDTO.createTime).isEqualTo(policyWaiver.getCreateTime());
    assertThat(apiPolicyWaiverDTO.hash).isEqualTo("hash");
    assertThat(apiPolicyWaiverDTO.policyId).isEqualTo(policyWaiver.getPolicyId());
    assertThat(apiPolicyWaiverDTO.scopeOwnerId).isEqualTo(organization.getId());
    assertThat(apiPolicyWaiverDTO.scopeOwnerName).isEqualTo(organization.getName());
    assertThat(apiPolicyWaiverDTO.scopeOwnerType).isEqualTo(OwnerType.ORGANIZATION.toString());
    assertThat(apiPolicyWaiverDTO.expiryTime).isEqualTo(aWeekFromNow);
    assertThat(apiPolicyWaiverDTO.threatLevel).isEqualTo(policy.getThreatLevel());
  }

  @Test
  void testGetPolicyWaiver_Repository() throws Exception {
    Instant now = Instant.now();
    Date today = Date.from(now);
    Date aWeekFromNow = Date.from(now.plus(7, ChronoUnit.DAYS));

    Repository repository = ctx.tempEntity().newRepository();
    Policy policy = ctx.tempEntity().newPolicy();
    PolicyWaiver policyWaiver = ctx.tempEntity()
        .newWaiver("hash", policy.getId(), repository.getId(),
            null, EXACT_COMPONENT, "comment", today, aWeekFromNow);

    HttpResponse response = restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.REPOSITORY, repository.getId(), policyWaiver.getId())
        .get();

    ctx.assertResponseStatus(200, response);

    ApiPolicyWaiverDTO apiPolicyWaiverDTO = response.getBody(ApiPolicyWaiverDTO.class);

    assertThat(apiPolicyWaiverDTO.policyWaiverId).isEqualTo(policyWaiver.getId());
    assertThat(apiPolicyWaiverDTO.comment).isEqualTo("comment");
    assertThat(apiPolicyWaiverDTO.createTime).isEqualTo(policyWaiver.getCreateTime());
    assertThat(apiPolicyWaiverDTO.hash).isEqualTo(policyWaiver.getHash());
    assertThat(apiPolicyWaiverDTO.policyId).isEqualTo(policyWaiver.getPolicyId());
    assertThat(apiPolicyWaiverDTO.scopeOwnerId).isEqualTo(repository.getId());
    assertThat(apiPolicyWaiverDTO.scopeOwnerName).isEqualTo(repository.getName());
    assertThat(apiPolicyWaiverDTO.scopeOwnerType).isEqualTo(OwnerType.REPOSITORY.toString());
    assertThat(apiPolicyWaiverDTO.expiryTime).isEqualTo(aWeekFromNow);
    assertThat(apiPolicyWaiverDTO.threatLevel).isEqualTo(policy.getThreatLevel());
  }

  @Test
  void testGetPolicyWaiver_RepositoryManager() throws Exception {
    Instant now = Instant.now();
    Date today = Date.from(now);
    Date aWeekFromNow = Date.from(now.plus(7, ChronoUnit.DAYS));

    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Policy policy = ctx.tempEntity().newPolicy();
    PolicyWaiver policyWaiver = ctx.tempEntity()
        .newWaiver("hash", policy.getId(), repositoryManager.getId(), null,
            EXACT_COMPONENT, "comment", today, aWeekFromNow);

    HttpResponse response = restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId(), policyWaiver.getId())
        .get();

    ctx.assertResponseStatus(200, response);

    ApiPolicyWaiverDTO apiPolicyWaiverDTO = response.getBody(ApiPolicyWaiverDTO.class);

    assertThat(apiPolicyWaiverDTO.policyWaiverId).isEqualTo(policyWaiver.getId());
    assertThat(apiPolicyWaiverDTO.comment).isEqualTo("comment");
    assertThat(apiPolicyWaiverDTO.createTime).isEqualTo(policyWaiver.getCreateTime());
    assertThat(apiPolicyWaiverDTO.hash).isEqualTo(policyWaiver.getHash());
    assertThat(apiPolicyWaiverDTO.policyId).isEqualTo(policyWaiver.getPolicyId());
    assertThat(apiPolicyWaiverDTO.scopeOwnerId).isEqualTo(repositoryManager.getId());
    assertThat(apiPolicyWaiverDTO.scopeOwnerName).isEqualTo(repositoryManager.getName());
    assertThat(apiPolicyWaiverDTO.scopeOwnerType).isEqualTo(OwnerType.REPOSITORY_MANAGER.toString());
    assertThat(apiPolicyWaiverDTO.expiryTime).isEqualTo(aWeekFromNow);
    assertThat(apiPolicyWaiverDTO.threatLevel).isEqualTo(policy.getThreatLevel());
  }

  @Test
  void testGetPolicyWaiver_RepositoryContainer() throws Exception {
    Instant now = Instant.now();
    Date today = Date.from(now);
    Date aWeekFromNow = Date.from(now.plus(7, ChronoUnit.DAYS));

    Policy policy = ctx.tempEntity().newPolicy();
    PolicyWaiver policyWaiver = ctx.tempEntity()
        .newWaiver("hash", policy.getId(), REPOSITORY_CONTAINER_ID,
            null, EXACT_COMPONENT, "comment", today, aWeekFromNow);

    HttpResponse response = restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID, policyWaiver.getId())
        .get();

    ctx.assertResponseStatus(200, response);

    ApiPolicyWaiverDTO apiPolicyWaiverDTO = response.getBody(ApiPolicyWaiverDTO.class);

    assertThat(apiPolicyWaiverDTO.policyWaiverId).isEqualTo(policyWaiver.getId());
    assertThat(apiPolicyWaiverDTO.comment).isEqualTo(policyWaiver.getComment());
    assertThat(apiPolicyWaiverDTO.createTime).isEqualTo(policyWaiver.getCreateTime());
    assertThat(apiPolicyWaiverDTO.hash).isEqualTo(policyWaiver.getHash());
    assertThat(apiPolicyWaiverDTO.policyId).isEqualTo(policyWaiver.getPolicyId());
    assertThat(apiPolicyWaiverDTO.scopeOwnerId).isEqualTo(REPOSITORY_CONTAINER_ID);
    assertThat(apiPolicyWaiverDTO.scopeOwnerName).isEqualTo("Repository Managers");
    assertThat(apiPolicyWaiverDTO.scopeOwnerType).isEqualTo("all_repositories");
    assertThat(apiPolicyWaiverDTO.expiryTime).isEqualTo(aWeekFromNow);
    assertThat(apiPolicyWaiverDTO.threatLevel).isEqualTo(policy.getThreatLevel());
  }

  /**
   * @deprecated Deprecated because the tested method is deprecated.
   */
  @Deprecated(since = "1.192")
  @Test
  void testRequestWaiver_applicationPolicyViolation() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    Policy policy = ctx.tempEntity().newPolicy(app);
    PolicyEvaluation policyEvaluation =
        ctx.tempEntity().newPolicyEvaluation(app.getId(), StageTypes.BUILD.getName(), "scanId");
    PolicyViolation policyViolation = ctx.tempEntity().newPolicyViolation(policyEvaluation, policy);

    ApiRequestPolicyWaiverDTO dto = new ApiRequestPolicyWaiverDTO();
    dto.addWaiverLink = "addWaiverLink";
    dto.policyViolationLink = "policyViolationLink";
    dto.comment = "comment";
    dto.reasonId = "9b704ef5bc064fc29d7fe08a251ee9a6";
    HttpResponse post = restRequest()
        .path(ApiPolicyWaiverResource.REQUEST_WAIVER_BY_POLICY_VIOLATION_ID_PATH)
        .parameter(policyViolation.getId())
        .body(dto, MediaType.APPLICATION_JSON)
        .post();
    assertThat(post.getStatusCode()).isEqualTo(HttpServletResponse.SC_NO_CONTENT);
  }

  /**
   * @deprecated Deprecated because the tested method is deprecated.
   */
  @Deprecated(since = "1.192")
  @Test
  void testRequestWaiver_unknownOrRepositoryPolicyViolation() throws Exception {
    ApiRequestPolicyWaiverDTO dto = new ApiRequestPolicyWaiverDTO();
    dto.addWaiverLink = "addWaiverLink";
    dto.policyViolationLink = "policyViolationLink";

    HttpResponse post = restRequest()
        .path(ApiPolicyWaiverResource.REQUEST_WAIVER_BY_POLICY_VIOLATION_ID_PATH)
        .parameter("InvalidPolicyViolationId")
        .body(dto, MediaType.APPLICATION_JSON)
        .post();
    assertThat(post.getStatusCode()).isEqualTo(HttpServletResponse.SC_NOT_FOUND);
    assertThat(post.getBodyText()).isEqualTo("Could not find associated policy violation");

    Repository repository = ctx.tempEntity().newRepository();
    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        ctx.tempEntity().newRepositoryPolicyViolation(repository.getId(), Date.from(Instant.now()));
    post = restRequest()
        .path(ApiPolicyWaiverResource.REQUEST_WAIVER_BY_POLICY_VIOLATION_ID_PATH)
        .parameter(proxyRepositoryPolicyViolation.getId())
        .body(dto, MediaType.APPLICATION_JSON)
        .post();
    assertThat(post.getStatusCode()).isEqualTo(HttpServletResponse.SC_NOT_FOUND);
    assertThat(post.getBodyText()).isEqualTo("Could not find associated policy violation");
  }

  @Test
  void testUpdatePolicyWaiver() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();
    Policy policy = ctx.tempEntity().newPolicy(application);
    PolicyWaiverReason policyWaiverReason1 = ctx.tempEntity().newWaiverReason("type1", "reason1");
    String hash = "hash";
    Date expiry = DateUtils.addDays(new Date(), 1);
    PolicyWaiver policyWaiver =
        ctx.tempEntity().newWaiver(hash, policy.getId(), application.getId(), "comment1", expiry);
    policyWaiver.setExpireWhenRemediationAvailable(false);
    policyWaiver.setWaiverReasonId(policyWaiverReason1.getId());
    policyWaiverDAO.update(policyWaiver);

    ApiWaiverOptionsDTO dto = new ApiWaiverOptionsDTO();
    dto.comment = "comment2";
    dto.expiryTime = DateUtils.addDays(expiry, 1);
    PolicyWaiverReason policyWaiverReason2 = ctx.tempEntity().newWaiverReason("type1", "reason2");
    dto.waiverReasonId = policyWaiverReason2.getId();
    dto.expireWhenRemediationAvailable = true;
    dto.matcherStrategy = EXACT_COMPONENT;

    HttpResponse response = restRequest()
        .path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, application.getId(), policyWaiver.getId())
        .body(dto, MediaType.APPLICATION_JSON)
        .put();

    ctx.assertResponseStatus(204, response);
    assertThat(new ApiWaiverOptionsDTO(policyWaiverDAO.getById(policyWaiver.getId())))
        .usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
        .isEqualTo(dto);
  }

  @Test
  void testAddBulkPolicyWaivers_Application_Success() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();
    Policy policy1 = ctx.tempEntity().newPolicy(application);
    Policy policy2 = ctx.tempEntity().newPolicy(application);

    PolicyEvaluation policyEvaluation = ctx.tempEntity().newPolicyEvaluation(application.getId(), "develop", "scan1");
    PolicyViolation violation1 = ctx.tempEntity()
        .newPolicyViolation(policyEvaluation, policy1,
            "g1", "a1", "v1", "hash1", "reason");
    PolicyViolation violation2 = ctx.tempEntity()
        .newPolicyViolation(policyEvaluation, policy2,
            "g2", "a2", "v2", "hash2", "reason");

    ApiWaiverOptionsDTO waiverOptions = new ApiWaiverOptionsDTO();
    waiverOptions.comment = "Bulk waiver via API";
    waiverOptions.matcherStrategy = EXACT_COMPONENT;
    ApiBulkWaiversDTO bulkWaiversDTO = new ApiBulkWaiversDTO(
        Arrays.asList(violation1.getId(), violation2.getId()),
        waiverOptions);

    HttpResponse response = restRequest()
        .path(OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, application.getPublicId())
        .body(bulkWaiversDTO, MediaType.APPLICATION_JSON)
        .post();

    ctx.assertResponseStatus(204, response);

    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(application.getId());
    assertThat(waivers)
        .hasSize(2)
        .allSatisfy(waiver -> {
          assertThat(waiver.getComment()).isEqualTo("Bulk waiver via API");
          assertThat(waiver.getComponentMatchStrategy()).isEqualTo(EXACT_COMPONENT);
        });
  }

  @Test
  void testAddBulkPolicyWaivers_Organization_Success() throws Exception {
    Organization organization = ctx.tempEntity().newOrganization();
    Application application = ctx.tempEntity().newApplicationWithParent(organization);
    Policy policy = ctx.tempEntity().newPolicy(application);

    PolicyEvaluation policyEvaluation = ctx.tempEntity().newPolicyEvaluation(application.getId(), "develop", "scan1");
    PolicyViolation violation = ctx.tempEntity()
        .newPolicyViolation(policyEvaluation, policy,
            "g1", "a1", "v1", "hash1", "reason");

    ApiWaiverOptionsDTO waiverOptions = new ApiWaiverOptionsDTO();
    waiverOptions.comment = "Org bulk waiver";
    waiverOptions.matcherStrategy =
        com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_VERSIONS;
    ApiBulkWaiversDTO bulkWaiversDTO = new ApiBulkWaiversDTO(
        singletonList(violation.getId()),
        waiverOptions);

    HttpResponse response = restRequest()
        .path(OWNERS_PATH)
        .parameter(OwnerType.ORGANIZATION, organization.getId())
        .body(bulkWaiversDTO, MediaType.APPLICATION_JSON)
        .post();

    ctx.assertResponseStatus(204, response);

    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(organization.getId());
    assertThat(waivers).hasSize(1);
    assertThat(waivers.get(0).getComment()).isEqualTo("Org bulk waiver");
  }

  @Test
  void testAddBulkPolicyWaivers_Repository_Success() throws Exception {
    Repository repository = ctx.tempEntity().newRepository();
    Policy policy = ctx.tempEntity().newPolicy(Organization.ROOT_ORGANIZATION_ID);

    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        ctx.tempEntity().newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    ApiWaiverOptionsDTO waiverOptions = new ApiWaiverOptionsDTO();
    waiverOptions.comment = "Repo bulk waiver";
    waiverOptions.matcherStrategy = EXACT_COMPONENT;
    ApiBulkWaiversDTO bulkWaiversDTO = new ApiBulkWaiversDTO(
        singletonList(proxyRepositoryPolicyViolation.getId()),
        waiverOptions);

    HttpResponse response = restRequest()
        .path(OWNERS_PATH)
        .parameter(OwnerType.REPOSITORY, repository.getId())
        .body(bulkWaiversDTO, MediaType.APPLICATION_JSON)
        .post();

    ctx.assertResponseStatus(204, response);

    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(repository.getId());
    assertThat(waivers).hasSize(1);
    assertThat(waivers.get(0).getComment()).isEqualTo("Repo bulk waiver");
  }

  @Test
  void testAddBulkPolicyWaivers_RepositoryContainer_Success() throws Exception {
    Repository repository = ctx.tempEntity().newRepository();
    Policy policy = ctx.tempEntity().newPolicy(Organization.ROOT_ORGANIZATION_ID);

    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        ctx.tempEntity().newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    ApiWaiverOptionsDTO waiverOptions = new ApiWaiverOptionsDTO();
    waiverOptions.comment = "Container bulk waiver";
    waiverOptions.matcherStrategy = EXACT_COMPONENT;
    ApiBulkWaiversDTO bulkWaiversDTO = new ApiBulkWaiversDTO(
        singletonList(proxyRepositoryPolicyViolation.getId()),
        waiverOptions);

    HttpResponse response = restRequest()
        .path(OWNERS_PATH)
        .parameter(OwnerType.REPOSITORY_CONTAINER,
            RepositoryContainer.REPOSITORY_CONTAINER_ID, proxyRepositoryPolicyViolation.getId())
        .body(bulkWaiversDTO, MediaType.APPLICATION_JSON)
        .post();

    ctx.assertResponseStatus(204, response);

    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    assertThat(waivers).hasSize(1);
    assertThat(waivers.get(0).getComment()).isEqualTo("Container bulk waiver");
  }

  @Test
  void testAddBulkPolicyWaivers_RepositoryManager_Success() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager);
    Policy policy = ctx.tempEntity().newPolicy(Organization.ROOT_ORGANIZATION_ID);
    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        ctx.tempEntity().newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    ApiWaiverOptionsDTO waiverOptions = new ApiWaiverOptionsDTO();
    waiverOptions.comment = "Repository Manager bulk waiver";
    waiverOptions.matcherStrategy = EXACT_COMPONENT;
    ApiBulkWaiversDTO bulkWaiversDTO = new ApiBulkWaiversDTO(
        singletonList(proxyRepositoryPolicyViolation.getId()),
        waiverOptions);

    HttpResponse response = restRequest()
        .path(OWNERS_PATH)
        .parameter(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId())
        .body(bulkWaiversDTO, MediaType.APPLICATION_JSON)
        .post();

    ctx.assertResponseStatus(204, response);

    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(repositoryManager.getId());
    assertThat(waivers).hasSize(1);
    assertThat(waivers.get(0).getComment()).isEqualTo("Repository Manager bulk waiver");
    assertThat(waivers.get(0).getComponentMatchStrategy()).isEqualTo(EXACT_COMPONENT);
  }

  @Test
  void testAddBulkPolicyWaivers_NullRequest() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();

    HttpResponse response = restRequest()
        .path(OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, application.getPublicId())
        .body(null, MediaType.APPLICATION_JSON)
        .post();

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains("Waivers request cannot be null");
  }

  @Test
  void testAddBulkPolicyWaivers_EmptyViolationIds() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();

    ApiWaiverOptionsDTO waiverOptions = new ApiWaiverOptionsDTO();
    waiverOptions.matcherStrategy = EXACT_COMPONENT;
    ApiBulkWaiversDTO bulkWaiversDTO = new ApiBulkWaiversDTO(
        Collections.emptyList(),
        waiverOptions);

    HttpResponse response = restRequest()
        .path(OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, application.getPublicId())
        .body(bulkWaiversDTO, MediaType.APPLICATION_JSON)
        .post();

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains("Violation IDs list cannot be null or empty");
  }

  @Test
  void testAddBulkPolicyWaivers_TooManyViolations() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();

    List<String> tooManyViolations = new ArrayList<>();
    for (int i = 0; i < 1001; i++) {
      tooManyViolations.add("violation-" + i);
    }

    ApiWaiverOptionsDTO waiverOptions = new ApiWaiverOptionsDTO();
    waiverOptions.matcherStrategy = EXACT_COMPONENT;
    ApiBulkWaiversDTO bulkWaiversDTO = new ApiBulkWaiversDTO(
        tooManyViolations,
        waiverOptions);

    HttpResponse response = restRequest()
        .path(OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, application.getPublicId())
        .body(bulkWaiversDTO, MediaType.APPLICATION_JSON)
        .post();

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains("Maximum "
        + MAX_BULK_WAIVER_VIOLATIONS + " violations allowed per waiver request");
  }

  @Test
  void testAddBulkPolicyWaivers_InvalidMatcherStrategy() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();
    Policy policy = ctx.tempEntity().newPolicy(application);

    PolicyEvaluation policyEvaluation = ctx.tempEntity().newPolicyEvaluation(application.getId(), "develop", "scan1");
    PolicyViolation violation = ctx.tempEntity()
        .newPolicyViolation(policyEvaluation, policy,
            "g1", "a1", "v1", "hash1", "reason");

    ApiWaiverOptionsDTO waiverOptions = new ApiWaiverOptionsDTO();
    waiverOptions.matcherStrategy = ALL_COMPONENTS;
    ApiBulkWaiversDTO bulkWaiversDTO = new ApiBulkWaiversDTO(
        singletonList(violation.getId()),
        waiverOptions);

    HttpResponse response = restRequest()
        .path(OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, application.getPublicId())
        .body(bulkWaiversDTO, MediaType.APPLICATION_JSON)
        .post();

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains("Only EXACT_COMPONENT and ALL_VERSIONS matcher strategies " +
        "are supported for bulk waivers");
  }

  @Test
  void testAddBulkPolicyWaivers_NoValidViolations() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();

    ApiWaiverOptionsDTO waiverOptions = new ApiWaiverOptionsDTO();
    waiverOptions.matcherStrategy = EXACT_COMPONENT;
    ApiBulkWaiversDTO bulkWaiversDTO = new ApiBulkWaiversDTO(
        Arrays.asList("invalid-1", "invalid-2"),
        waiverOptions);

    HttpResponse response = restRequest()
        .path(OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, application.getPublicId())
        .body(bulkWaiversDTO, MediaType.APPLICATION_JSON)
        .post();

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains("Error processing policy violation with ID: invalid-1");
  }

  @Test
  void testAddBulkPolicyWaivers_ThrowsErrorsOnInvalidViolationIds() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();
    Policy policy = ctx.tempEntity().newPolicy(application);

    PolicyEvaluation policyEvaluation = ctx.tempEntity().newPolicyEvaluation(application.getId(), "develop", "scan1");
    PolicyViolation violation = ctx.tempEntity()
        .newPolicyViolation(policyEvaluation, policy,
            "g1", "a1", "v1", "hash1", "reason");

    ApiWaiverOptionsDTO waiverOptions = new ApiWaiverOptionsDTO();
    waiverOptions.comment = "Partial success test";
    waiverOptions.matcherStrategy = EXACT_COMPONENT;
    ApiBulkWaiversDTO bulkWaiversDTO = new ApiBulkWaiversDTO(
        Arrays.asList(violation.getId(), "invalid-id-123", "another-invalid-id"),
        waiverOptions);

    HttpResponse response = restRequest()
        .path(OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, application.getPublicId())
        .body(bulkWaiversDTO, MediaType.APPLICATION_JSON)
        .post();

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText())
        .satisfiesAnyOf(
            bodyText -> assertThat(bodyText).contains("Error processing policy violation with ID: invalid-id-123"),
            bodyText -> assertThat(bodyText).contains("Error processing policy violation with ID: another-invalid-id"));

    // Should create no waivers when invalid valid violation IDs are present
    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(application.getId());
    assertThat(waivers).hasSize(0);
  }

  @Test
  void testAddBulkPolicyWaivers_WithExpiryTime() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();
    Policy policy = ctx.tempEntity().newPolicy(application);

    PolicyEvaluation policyEvaluation = ctx.tempEntity().newPolicyEvaluation(application.getId(), "develop", "scan1");
    PolicyViolation violation = ctx.tempEntity()
        .newPolicyViolation(policyEvaluation, policy,
            "g1", "a1", "v1", "hash1", "reason");

    Date futureDate = DateUtils.addDays(new Date(), 7);

    ApiWaiverOptionsDTO waiverOptions = new ApiWaiverOptionsDTO();
    waiverOptions.comment = "Expiring waiver";
    waiverOptions.matcherStrategy = EXACT_COMPONENT;
    waiverOptions.expiryTime = futureDate;
    ApiBulkWaiversDTO bulkWaiversDTO = new ApiBulkWaiversDTO(
        singletonList(violation.getId()),
        waiverOptions);

    HttpResponse response = restRequest()
        .path(OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, application.getPublicId())
        .body(bulkWaiversDTO, MediaType.APPLICATION_JSON)
        .post();

    ctx.assertResponseStatus(204, response);

    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(application.getId());
    assertThat(waivers).hasSize(1);
    assertThat(waivers.get(0).getExpiryTime()).isEqualTo(futureDate);
  }

  @Test
  void testAddBulkPolicyWaivers_ExpiryTimeInPast() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();
    Policy policy = ctx.tempEntity().newPolicy(application);

    PolicyEvaluation policyEvaluation = ctx.tempEntity().newPolicyEvaluation(application.getId(), "develop", "scan1");
    PolicyViolation violation = ctx.tempEntity()
        .newPolicyViolation(policyEvaluation, policy,
            "g1", "a1", "v1", "hash1", "reason");

    Date pastDate = DateUtils.addDays(new Date(), -1);

    ApiWaiverOptionsDTO waiverOptions = new ApiWaiverOptionsDTO();
    waiverOptions.comment = "Invalid expiry";
    waiverOptions.matcherStrategy = EXACT_COMPONENT;
    waiverOptions.expiryTime = pastDate;
    ApiBulkWaiversDTO bulkWaiversDTO = new ApiBulkWaiversDTO(
        singletonList(violation.getId()),
        waiverOptions);

    HttpResponse response = restRequest()
        .path(OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, application.getPublicId())
        .body(bulkWaiversDTO, MediaType.APPLICATION_JSON)
        .post();

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains("Expiration date must be in the future");
  }
}
