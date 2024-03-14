/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.UnaryOperator;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRequestPolicyWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiWaiverOptionsDTO;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiComponentPolicyWaiversDTO;
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
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.report.ReportTestUtils;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.json.store.JsonUtils;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.api.v2.ApiPolicyWaiverResource.BY_POLICY_VIOLATION_ID_PATH;
import static com.sonatype.insight.brain.api.v2.ApiPolicyWaiverResource.BY_POLICY_WAIVER_ID_PATH;
import static com.sonatype.insight.brain.api.v2.ApiPolicyWaiverResource.OWNERS_PATH;
import static com.sonatype.insight.brain.api.v2.ApiPolicyWaiverResource.TRANSITIVE_VIOLATIONS_BY_SCAN_ID_PATH;
import static com.sonatype.insight.brain.api.v2.ApiPolicyWaiverResource.TRANSITIVE_VIOLATIONS_BY_STAGE_ID_PATH;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
import static com.sonatype.insight.brain.model.repository.RepositoryContainer.REPOSITORY_CONTAINER_ID;
import static com.sonatype.insight.brain.report.ReportTestUtils.zipReportDir;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiPolicyWaiverResourceTest
    extends AbstractResourceTest
{
  private RepositoryManagerDAO repositoryManagerDAO;

  private PolicyWaiverDAO policyWaiverDAO;

  private PolicyViolationDAO policyViolationDAO;

  @Before
  public void setUp() {
    repositoryManagerDAO = lookup(RepositoryManagerDAO.class);
    policyWaiverDAO = lookup(PolicyWaiverDAO.class);
    policyViolationDAO = lookup(PolicyViolationDAO.class);
  }

  @Test
  public void testDeletePolicyWaiver() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(application);
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), application.getId());

    HttpResponse response = restRequest()
        .path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, application.getId(), policyWaiver.getId())
        .delete();

    assertResponseStatus(204, response);
    assertThat(policyWaiverDAO.getById(policyWaiver.getId())).isNull();
  }

  @Test
  public void testGetPolicyWaivers_Application() throws Exception {
    Instant now = Instant.now();
    Date today = Date.from(now);
    Date aWeekFromNow = Date.from(now.plus(7, ChronoUnit.DAYS));
    Application application = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(application);

    TriggerReference triggerReference = new TriggerReference(TriggerReference.Type.SECURITY_VULNERABILITY_REFID,
        "vulnerability-1");
    ConditionFact conditionFact = new ConditionFact("condition type id", 0, "summary", "reason", triggerReference);
    ConstraintFact constraintFact = new ConstraintFact("constraint id", "constraint name", "operator", conditionFact);
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash", policy.getId(), application.getId(),
        singletonList(constraintFact), EXACT_COMPONENT, "a comment", today, aWeekFromNow);

    HttpResponse response = restRequest().path(OWNERS_PATH).parameter(OwnerType.APPLICATION, application.getId()).get();

    assertResponseStatus(200, response);

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
  public void testGetPolicyWaivers_Organization() throws Exception {
    Instant now = Instant.now();
    Date today = Date.from(now);
    Date aWeekFromNow = Date.from(now.plus(7, ChronoUnit.DAYS));

    Organization organization = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(organization);
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash", policy.getId(), organization.getId(),
        null, EXACT_COMPONENT, "a comment in org waiver", today, aWeekFromNow);

    HttpResponse response =
        restRequest().path(OWNERS_PATH).parameter(OwnerType.ORGANIZATION, organization.getId()).get();

    assertResponseStatus(200, response);

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
  public void testGetPolicyWaivers_Repository() throws Exception {
    Instant now = Instant.now();
    Date today = Date.from(now);
    Date aWeekFromNow = Date.from(now.plus(7, ChronoUnit.DAYS));

    Repository repository = tempEntity.newRepository();
    Policy policy = tempEntity.newPolicy();
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash", policy.getId(), repository.getId(),
        null, EXACT_COMPONENT, "comment", today, aWeekFromNow);

    HttpResponse response = restRequest().path(OWNERS_PATH).parameter(OwnerType.REPOSITORY, repository.getId()).get();

    assertResponseStatus(200, response);

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
  public void testGetPolicyWaivers_RepositoryManager() throws Exception {
    Instant now = Instant.now();
    Date today = Date.from(now);
    Date aWeekFromNow = Date.from(now.plus(7, ChronoUnit.DAYS));

    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Policy policy = tempEntity.newPolicy();
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash", policy.getId(), repositoryManager.getId(), null,
        EXACT_COMPONENT, "comment", today, aWeekFromNow);

    HttpResponse response =
        restRequest().path(OWNERS_PATH).parameter(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId()).get();

    assertResponseStatus(200, response);

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
  public void testGetPolicyWaivers_RepositoryContainer() throws Exception {
    Instant now = Instant.now();
    Date today = Date.from(now);
    Date aWeekFromNow = Date.from(now.plus(7, ChronoUnit.DAYS));

    Policy policy = tempEntity.newPolicy();
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash", policy.getId(), REPOSITORY_CONTAINER_ID,
        null, EXACT_COMPONENT, "comment", today, aWeekFromNow);

    HttpResponse response =
        restRequest().path(OWNERS_PATH).parameter(OwnerType.REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID).get();

    assertResponseStatus(200, response);

    List<ApiPolicyWaiverDTO> policyWaiverDtoList = Arrays.asList(response.getBody(ApiPolicyWaiverDTO[].class));
    assertThat(policyWaiverDtoList).hasSize(1);

    ApiPolicyWaiverDTO apiPolicyWaiverDTO = policyWaiverDtoList.get(0);
    assertThat(apiPolicyWaiverDTO.policyWaiverId).isEqualTo(policyWaiver.getId());
    assertThat(apiPolicyWaiverDTO.comment).isEqualTo(policyWaiver.getComment());
    assertThat(apiPolicyWaiverDTO.createTime).isEqualTo(policyWaiver.getCreateTime());
    assertThat(apiPolicyWaiverDTO.hash).isEqualTo(policyWaiver.getHash());
    assertThat(apiPolicyWaiverDTO.policyId).isEqualTo(policyWaiver.getPolicyId());
    assertThat(apiPolicyWaiverDTO.scopeOwnerId).isEqualTo(REPOSITORY_CONTAINER_ID);
    assertThat(apiPolicyWaiverDTO.scopeOwnerName).isEqualTo("Repository Managers");
    assertThat(apiPolicyWaiverDTO.scopeOwnerType).isEqualTo("all_repositories");
    assertThat(apiPolicyWaiverDTO.expiryTime).isEqualTo(aWeekFromNow);
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_Application() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    PolicyViolation policyViolation = tempEntity
        .newPolicyViolation(policyEvaluation, policy, "g1", "a1", "v1", "h1", "r1");

    ApiWaiverOptionsDTO waiverOptionsDTO = new ApiWaiverOptionsDTO();
    waiverOptionsDTO.comment = "waiver comment";
    HttpResponse response = restRequest().path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getId(), policyViolation.getId())
        .body(waiverOptionsDTO, MediaType.APPLICATION_JSON)
        .post();

    assertResponseStatus(204, response);
    assertNonExpiringPolicyWaiver(app.getId(), policy, policyViolation, "waiver comment", policyViolation.getHash());
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_Organization() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Policy policy = tempEntity.newPolicy(app);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    PolicyViolation policyViolation = tempEntity
        .newPolicyViolation(policyEvaluation, policy, "g1", "a1", "v1", "h1", "r1");

    ApiWaiverOptionsDTO waiverOptionsDTO = new ApiWaiverOptionsDTO();
    waiverOptionsDTO.comment = "waiver comment";
    HttpResponse response = restRequest().path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.ORGANIZATION, org.getId(), policyViolation.getId())
        .body(waiverOptionsDTO, MediaType.APPLICATION_JSON)
        .post();

    assertResponseStatus(204, response);
    assertNonExpiringPolicyWaiver(org.getId(), policy, policyViolation, "waiver comment", policyViolation.getHash());
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_RootOrganization() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    PolicyViolation policyViolation = tempEntity
        .newPolicyViolation(policyEvaluation, policy, "g1", "a1", "v1", "h1", "r1");

    ApiWaiverOptionsDTO waiverOptionsDTO = new ApiWaiverOptionsDTO();
    waiverOptionsDTO.comment = "waiver comment";
    HttpResponse response = restRequest().path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID, policyViolation.getId())
        .body(waiverOptionsDTO, MediaType.APPLICATION_JSON)
        .post();

    assertResponseStatus(204, response);
    assertNonExpiringPolicyWaiver(Organization.ROOT_ORGANIZATION_ID, policy, policyViolation, "waiver comment",
        policyViolation.getHash());
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_ApplyToAllComponents() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    PolicyViolation policyViolation = tempEntity
        .newPolicyViolation(policyEvaluation, policy, "g1", "a1", "v1", "h1", "r1");

    ApiWaiverOptionsDTO waiverOptionsDTO = new ApiWaiverOptionsDTO();
    waiverOptionsDTO.comment = "waiver comment";
    waiverOptionsDTO.applyToAllComponents = true;
    HttpResponse response = restRequest().path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getId(), policyViolation.getId())
        .body(waiverOptionsDTO, MediaType.APPLICATION_JSON)
        .post();

    assertResponseStatus(204, response);
    assertNonExpiringPolicyWaiver(app.getId(), policy, policyViolation, "waiver comment", null);
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_NoRequestBody() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    PolicyViolation policyViolation = tempEntity
        .newPolicyViolation(policyEvaluation, policy, "g1", "a1", "v1", "h1", "r1");

    HttpResponse response = restRequest().path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getId(), policyViolation.getId())
        .post();

    assertResponseStatus(204, response);
    assertNonExpiringPolicyWaiver(app.getId(), policy, policyViolation, null, policyViolation.getHash());
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_ExpiresInFuture() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    PolicyViolation policyViolation = tempEntity
        .newPolicyViolation(policyEvaluation, policy, "g1", "a1", "v1", "h1", "r1");

    ApiWaiverOptionsDTO waiverOptionsDTO = new ApiWaiverOptionsDTO();
    waiverOptionsDTO.comment = "waiver comment";
    Date expiryTime = DateTime.now().plusDays(7).toDate();
    waiverOptionsDTO.expiryTime = expiryTime;
    HttpResponse response = restRequest().path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getId(), policyViolation.getId())
        .body(waiverOptionsDTO, MediaType.APPLICATION_JSON)
        .post();

    assertResponseStatus(204, response);
    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getActiveByOwnerId(app.getId());
    assertThat(policyWaivers).isNotEmpty().hasSize(1);
    assertPolicyWaiver(app.getId(), policy, policyViolation, policyWaivers.get(0), "waiver comment",
        policyViolation.getHash(), expiryTime);
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_Expired() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    PolicyViolation policyViolation = tempEntity
        .newPolicyViolation(policyEvaluation, policy, "g1", "a1", "v1", "h1", "r1");

    /*
       Directly insert into db with expiry date in the past.
       The api will now not allow creating waiver with expiry date in the past.
     */

    AbstractPolicyViolation abstractPolicyViolation = policyViolationDAO.getById(policyViolation.getId());
    Date expiryTime = DateTime.now().minusDays(1).toDate();
    String waiverComment = "some comment";
    try (TransactionContext tx = policyWaiverDAO.createTransactionContext()) {
      tx.begin();

      PolicyWaiver policyWaiver =
          new PolicyWaiver("h1", abstractPolicyViolation.getPolicyId(), app.getId(), waiverComment);
      policyWaiver.setConstraintFactsJson(abstractPolicyViolation.getConstraintFactsJson());
      policyWaiver.setExpiryTime(expiryTime);

      policyWaiverDAO.insert(tx, policyWaiver);
      tx.commit();
    }

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
  public void testAddWaiverToTransitivePolicyViolationsByAppScanComponent_ByComponentIdentifier() throws Exception {
    testAddWaiverToTransitivePolicyViolationsByAppScanComponent(request -> request.query("componentIdentifier",
        ComponentIdentifier.createMavenCoordinates("g", "direct", "v", "", "e")));
  }

  @Test
  public void testAddWaiverToTransitivePolicyViolationsByAppScanComponent_ByPackageUrl() throws Exception {
    testAddWaiverToTransitivePolicyViolationsByAppScanComponent(
        request -> request.query("packageUrl", "pkg:maven/g/direct@v?type=e"));
  }

  @Test
  public void testAddWaiverToTransitivePolicyViolationsByAppScanComponent_ByHash() throws Exception {
    testAddWaiverToTransitivePolicyViolationsByAppScanComponent(request -> request.query("hash", "hash1"));
  }

  public void testAddWaiverToTransitivePolicyViolationsByAppScanComponent(
      UnaryOperator<HttpRequest> operator) throws Exception
  {
    Application app = tempEntity.newApplicationWithParent();
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId");
    Policy policy = tempEntity.newPolicy(app);

    ComponentIdentifier componentIdentifierTransitive =
        ComponentIdentifier.createMavenCoordinates("g", "transitive", "v", "", "e");
    PolicyViolation policyViolationTransitive =
        tempEntity.newPolicyViolation(policyEvaluation, policy, componentIdentifierTransitive, "hash2");

    ReportTestUtils.createReportFile(app.getId(), policyEvaluation.getScanId(),
        zipReportDir("/ApiPolicyWaiverResourceTest/report", tempDir),
        getCLMServer().getInstance(InsightWork.class));

    ReportTestUtils.createPolicyThreats(app.getId(), policyEvaluation.getScanId(),
        getCLMServer().getInstance(InsightWork.class), Collections.singletonList(policyViolationTransitive));

    ApiWaiverOptionsDTO waiverOptionsDTO = new ApiWaiverOptionsDTO();
    waiverOptionsDTO.comment = "waiver comment";
    waiverOptionsDTO.expiryTime = new Date(System.currentTimeMillis() + 1000);

    HttpRequest request = restRequest()
        .path(TRANSITIVE_VIOLATIONS_BY_SCAN_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getPublicId(), policyEvaluation.getScanId())
        .body(waiverOptionsDTO, MediaType.APPLICATION_JSON);

    HttpResponse response = operator.apply(request).post();

    assertResponseStatus(204, response);

    List<PolicyWaiver> allPolicyWaivers = policyWaiverDAO.getByOwnerId(app.getId());
    assertThat(allPolicyWaivers).hasSize(1);
    assertPolicyWaiver(app.getId(), policy, policyViolationTransitive, allPolicyWaivers.get(0),
        waiverOptionsDTO.comment, policyViolationTransitive.getHash(), waiverOptionsDTO.expiryTime);
  }

  @Test
  public void testAddWaiverToTransitivePolicyViolationsByOwnerStageComponent_ByComponentIdentifier() throws Exception {
    testAddWaiverToTransitivePolicyViolationsByOwnerStageComponent(request -> request.query("componentIdentifier",
        ComponentIdentifier.createMavenCoordinates("g", "direct", "v", "", "e")));
  }

  @Test
  public void testAddWaiverToTransitivePolicyViolationsByOwnerStageComponent_ByPackageUrl() throws Exception {
    testAddWaiverToTransitivePolicyViolationsByOwnerStageComponent(
        request -> request.query("packageUrl", "pkg:maven/g/direct@v?type=e"));
  }

  @Test
  public void testAddWaiverToTransitivePolicyViolationsByOwnerStageComponent_ByHash() throws Exception {
    testAddWaiverToTransitivePolicyViolationsByOwnerStageComponent(request -> request.query("hash", "hash1"));
  }

  public void testAddWaiverToTransitivePolicyViolationsByOwnerStageComponent(
      UnaryOperator<HttpRequest> operator) throws Exception
  {
    Application app = tempEntity.newApplicationWithParent();
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId");
    Policy policy = tempEntity.newPolicy(app);
    ComponentIdentifier componentIdentifierTransitive =
        ComponentIdentifier.createMavenCoordinates("g", "transitive", "v", "", "e");
    PolicyViolation policyViolationTransitive =
        tempEntity.newPolicyViolation(policyEvaluation, policy, componentIdentifierTransitive, "hash2");
    ReportTestUtils.createReportFile(app.getId(), policyEvaluation.getScanId(),
        zipReportDir("/ApiPolicyWaiverResourceTest/report", tempDir),
        getCLMServer().getInstance(InsightWork.class));
    ReportTestUtils.createPolicyThreats(app.getId(), policyEvaluation.getScanId(),
        getCLMServer().getInstance(InsightWork.class), Collections.singletonList(policyViolationTransitive));
    ApiWaiverOptionsDTO waiverOptionsDTO = new ApiWaiverOptionsDTO();
    waiverOptionsDTO.comment = "waiver comment";
    waiverOptionsDTO.expiryTime = new Date(System.currentTimeMillis() + 1000);

    HttpRequest request = restRequest()
        .path(TRANSITIVE_VIOLATIONS_BY_STAGE_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getPublicId(), BuildStageType.ID)
        .body(waiverOptionsDTO, MediaType.APPLICATION_JSON);

    HttpResponse response = operator.apply(request).post();
    assertResponseStatus(204, response);
    List<PolicyWaiver> allPolicyWaivers = policyWaiverDAO.getByOwnerId(app.getId());
    assertThat(allPolicyWaivers).hasSize(1);
    assertPolicyWaiver(app.getId(), policy, policyViolationTransitive, allPolicyWaivers.get(0),
        waiverOptionsDTO.comment, policyViolationTransitive.getHash(), waiverOptionsDTO.expiryTime);
  }

  @Test
  public void testGetTransitivePolicyWaiversByAppScanComponent_ByComponentIdentifier() throws Exception {
    testGetTransitivePolicyWaiversByAppScanComponent(request -> request.query("componentIdentifier",
        ComponentIdentifier.createMavenCoordinates("g", "direct", "v", "", "e")));
  }

  @Test
  public void testGetTransitivePolicyWaiversByAppScanComponent_ByPackageUrl() throws Exception {
    testGetTransitivePolicyWaiversByAppScanComponent(
        request -> request.query("packageUrl", "pkg:maven/g/direct@v?type=e"));
  }

  @Test
  public void testGetTransitivePolicyWaiversByAppScanComponent_ByHash() throws Exception {
    testGetTransitivePolicyWaiversByAppScanComponent(request -> request.query("hash", "hash1"));
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_Repository() throws Exception {
    Repository repository = tempEntity.newRepository();
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);

    RepositoryPolicyViolation repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    ApiWaiverOptionsDTO waiverOptionsDTO = new ApiWaiverOptionsDTO();
    waiverOptionsDTO.comment = "waiver comment";
    HttpResponse response = restRequest().path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.REPOSITORY, repository.getId(), repositoryPolicyViolation.getId())
        .body(waiverOptionsDTO, MediaType.APPLICATION_JSON).post();

    assertResponseStatus(204, response);
    assertNonExpiringPolicyWaiver(repository.getId(), policy, repositoryPolicyViolation, "waiver comment",
        repositoryPolicyViolation.getHash());
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_RepositoryManager() throws Exception {
    Repository repository = tempEntity.newRepository();
    RepositoryManager repositoryManager = repositoryManagerDAO.getById(repository.getRepositoryManagerId());
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);

    RepositoryPolicyViolation repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    ApiWaiverOptionsDTO waiverOptionsDTO = new ApiWaiverOptionsDTO();
    waiverOptionsDTO.comment = "waiver comment";
    HttpResponse response = restRequest().path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId(), repositoryPolicyViolation.getId())
        .body(waiverOptionsDTO, MediaType.APPLICATION_JSON).post();

    assertResponseStatus(204, response);
    assertNonExpiringPolicyWaiver(repositoryManager.getId(), policy, repositoryPolicyViolation, "waiver comment",
        repositoryPolicyViolation.getHash());
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_RepositoryContainer() throws Exception {
    Repository repository = tempEntity.newRepository();
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);

    RepositoryPolicyViolation repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    ApiWaiverOptionsDTO waiverOptionsDTO = new ApiWaiverOptionsDTO();
    waiverOptionsDTO.comment = "waiver comment";
    HttpResponse response = restRequest()
        .path(BY_POLICY_VIOLATION_ID_PATH).parameter(OwnerType.REPOSITORY_CONTAINER,
            RepositoryContainer.REPOSITORY_CONTAINER_ID, repositoryPolicyViolation.getId())
        .body(waiverOptionsDTO, MediaType.APPLICATION_JSON).post();

    assertResponseStatus(204, response);
    assertNonExpiringPolicyWaiver(RepositoryContainer.REPOSITORY_CONTAINER_ID, policy, repositoryPolicyViolation,
        "waiver comment", repositoryPolicyViolation.getHash());
  }

  private void testGetTransitivePolicyWaiversByAppScanComponent(UnaryOperator<HttpRequest> operator) throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId");
    ReportTestUtils.createReportFile(app.getId(), policyEvaluation.getScanId(),
        zipReportDir("/ApiPolicyWaiverResourceTest/report", tempDir),
        getCLMServer().getInstance(InsightWork.class));
    Policy policy = tempEntity.newPolicy();
    PolicyWaiver policyWaiver =
        tempEntity.newWaiver("hash2", policy.getId(), app.getId(), null, EXACT_COMPONENT, null);

    HttpRequest request = restRequest()
        .path(TRANSITIVE_VIOLATIONS_BY_SCAN_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getPublicId(), policyEvaluation.getScanId());

    HttpResponse response = operator.apply(request).get();
    assertResponseStatus(200, response);
    ApiComponentPolicyWaiversDTO result = response.getBody(ApiComponentPolicyWaiversDTO.class);
    assertThat(result).isNotNull();
    assertThat(result.componentPolicyWaivers).isNotNull();
    assertThat(result.componentPolicyWaivers).extracting(componentPolicyWaiver -> componentPolicyWaiver.policyWaiverId)
        .containsExactly(policyWaiver.getId());
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.POLICY_WAIVER_PATH);
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
  public void testGetPolicyWaiver_Application() throws Exception {
    Instant now = Instant.now();
    Date today = Date.from(now);
    Date aWeekFromNow = Date.from(now.plus(7, ChronoUnit.DAYS));
    Application application = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(application);

    TriggerReference triggerReference = new TriggerReference(TriggerReference.Type.SECURITY_VULNERABILITY_REFID,
        "vulnerability-1");
    ConditionFact conditionFact = new ConditionFact("condition type id", 0, "summary", "reason", triggerReference);
    ConstraintFact constraintFact = new ConstraintFact("constraint id", "constraint name", "operator", conditionFact);
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash", policy.getId(), application.getId(),
        singletonList(constraintFact), EXACT_COMPONENT, "a comment", today, aWeekFromNow);

    HttpResponse response = restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, application.getId(), policyWaiver.getId()).get();

    assertResponseStatus(200, response);

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
  public void testGetPolicyWaiver_Organization() throws Exception {
    Instant now = Instant.now();
    Date today = Date.from(now);
    Date aWeekFromNow = Date.from(now.plus(7, ChronoUnit.DAYS));

    Organization organization = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(organization);
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash", policy.getId(), organization.getId(),
        null, EXACT_COMPONENT, "a comment in org waiver", today, aWeekFromNow);

    HttpResponse response = restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.ORGANIZATION, organization.getId(), policyWaiver.getId()).get();

    assertResponseStatus(200, response);

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
  public void testGetPolicyWaiver_Repository() throws Exception {
    Instant now = Instant.now();
    Date today = Date.from(now);
    Date aWeekFromNow = Date.from(now.plus(7, ChronoUnit.DAYS));

    Repository repository = tempEntity.newRepository();
    Policy policy = tempEntity.newPolicy();
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash", policy.getId(), repository.getId(),
        null, EXACT_COMPONENT, "comment", today, aWeekFromNow);

    HttpResponse response = restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.REPOSITORY, repository.getId(), policyWaiver.getId()).get();

    assertResponseStatus(200, response);

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
  public void testGetPolicyWaiver_RepositoryManager() throws Exception {
    Instant now = Instant.now();
    Date today = Date.from(now);
    Date aWeekFromNow = Date.from(now.plus(7, ChronoUnit.DAYS));

    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Policy policy = tempEntity.newPolicy();
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash", policy.getId(), repositoryManager.getId(), null,
        EXACT_COMPONENT, "comment", today, aWeekFromNow);

    HttpResponse response = restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId(), policyWaiver.getId()).get();

    assertResponseStatus(200, response);

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
  public void testGetPolicyWaiver_RepositoryContainer() throws Exception {
    Instant now = Instant.now();
    Date today = Date.from(now);
    Date aWeekFromNow = Date.from(now.plus(7, ChronoUnit.DAYS));

    Policy policy = tempEntity.newPolicy();
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash", policy.getId(), REPOSITORY_CONTAINER_ID,
        null, EXACT_COMPONENT, "comment", today, aWeekFromNow);

    HttpResponse response = restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID, policyWaiver.getId()).get();

    assertResponseStatus(200, response);

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

  @Test
  public void testRequestWaiver_applicationPolicyViolation() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app);
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(app.getId(), StageTypes.BUILD.getName(), "scanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    ApiRequestPolicyWaiverDTO dto = new ApiRequestPolicyWaiverDTO();
    dto.addWaiverLink = "addWaiverLink";
    dto.policyViolationLink = "policyViolationLink";
    HttpResponse post = restRequest()
        .path(ApiPolicyWaiverResource.REQUEST_WAIVER_BY_POLICY_VIOLATION_ID_PATH)
        .parameter(policyViolation.getId())
        .body(dto, MediaType.APPLICATION_JSON)
        .post();
    assertThat(post.getStatusCode()).isEqualTo(HttpServletResponse.SC_NO_CONTENT);
  }

  @Test
  public void testRequestWaiver_unknownOrRepositoryPolicyViolation() throws Exception {
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

    Repository repository = tempEntity.newRepository();
    RepositoryPolicyViolation
        repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), Date.from(Instant.now()));
    post = restRequest()
        .path(ApiPolicyWaiverResource.REQUEST_WAIVER_BY_POLICY_VIOLATION_ID_PATH)
        .parameter(repositoryPolicyViolation.getId())
        .body(dto, MediaType.APPLICATION_JSON)
        .post();
    assertThat(post.getStatusCode()).isEqualTo(HttpServletResponse.SC_NOT_FOUND);
    assertThat(post.getBodyText()).isEqualTo("Could not find associated policy violation");
  }
}
