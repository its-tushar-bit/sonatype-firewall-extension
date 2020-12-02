/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiWaiverOptionsDTO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.joda.time.DateTime;
import org.junit.Test;

import static com.sonatype.insight.brain.api.v2.DefaultApiPolicyWaiverResource.BY_POLICY_VIOLATION_ID_PATH;
import static com.sonatype.insight.brain.api.v2.DefaultApiPolicyWaiverResource.BY_POLICY_WAIVER_ID_PATH;
import static com.sonatype.insight.brain.model.repository.RepositoryContainer.REPOSITORY_CONTAINER_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiPolicyWaiverResourceTest
    extends AbstractResourceTest
{
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
    assertThat(new PolicyWaiverDAO().getById(policyWaiver.getId())).isNull();
  }

  @Test
  public void testGetPolicyWaivers_Application() throws Exception {
    Instant now = Instant.now();
    Date today = Date.from(now);
    Date aWeekFromNow = Date.from(now.plus(7, ChronoUnit.DAYS));
    Application application = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(application);

    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash", policy.getId(), application.getId(),
        null, "a comment", today, aWeekFromNow);

    HttpResponse response = restRequest().parameter(OwnerType.APPLICATION, application.getId()).get();

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
  }

  @Test
  public void testGetPolicyWaivers_Organization() throws Exception {
    Instant now = Instant.now();
    Date today = Date.from(now);
    Date aWeekFromNow = Date.from(now.plus(7, ChronoUnit.DAYS));

    Organization organization = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(organization);
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash", policy.getId(), organization.getId(),
        null, "a comment in org waiver", today, aWeekFromNow);

    HttpResponse response = restRequest().parameter(OwnerType.ORGANIZATION, organization.getId()).get();

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
        null, "comment", today, aWeekFromNow);

    HttpResponse response = restRequest().parameter(OwnerType.REPOSITORY, repository.getId()).get();

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
  public void testGetPolicyWaivers_RepositoryContainer() throws Exception {
    Instant now = Instant.now();
    Date today = Date.from(now);
    Date aWeekFromNow = Date.from(now.plus(7, ChronoUnit.DAYS));

    Policy policy = tempEntity.newPolicy();
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash", policy.getId(), REPOSITORY_CONTAINER_ID,
        null, "comment", today, aWeekFromNow);

    HttpResponse response = restRequest().parameter(OwnerType.REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID).get();

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
    assertThat(apiPolicyWaiverDTO.scopeOwnerName).isEqualTo("All Repositories");
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
    List<PolicyWaiver> policyWaivers = new PolicyWaiverDAO().getActiveByOwnerId(app.getId());
    assertThat(policyWaivers).isNotEmpty().hasSize(1);
    assertPolicyWaiver(app.getId(), policy, policyViolation, policyWaivers.get(0), "waiver comment",
        policyViolation.getHash(), expiryTime);
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_Expired() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    PolicyViolation policyViolation = tempEntity
        .newPolicyViolation(policyEvaluation, policy, "g1", "a1", "v1", "h1", "r1");

    ApiWaiverOptionsDTO waiverOptionsDTO = new ApiWaiverOptionsDTO();
    waiverOptionsDTO.comment = "waiver comment";
    Date expiryTime = DateTime.now().minusDays(1).toDate();
    waiverOptionsDTO.expiryTime = expiryTime;
    HttpResponse response = restRequest().path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getId(), policyViolation.getId())
        .body(waiverOptionsDTO, MediaType.APPLICATION_JSON)
        .post();

    // should not return a policy as the one existing is expired
    assertResponseStatus(204, response);
    List<PolicyWaiver> activePolicyWaivers = new PolicyWaiverDAO().getActiveByOwnerId(app.getId());
    assertThat(activePolicyWaivers).isEmpty();

    // getByOwnerId should still return the expired policy
    List<PolicyWaiver> allPolicyWaivers = new PolicyWaiverDAO().getByOwnerId(app.getId());
    assertThat(allPolicyWaivers).hasSize(1);
    assertPolicyWaiver(app.getId(), policy, policyViolation, allPolicyWaivers.get(0), "waiver comment",
        policyViolation.getHash(), expiryTime);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.POLICY_WAIVER_PATH);
  }

  private void assertNonExpiringPolicyWaiver(String ownerId,
                                             Policy policy,
                                             PolicyViolation policyViolation,
                                             String comment,
                                             String hash)
  {
    List<PolicyWaiver> policyWaivers = new PolicyWaiverDAO().getActiveByOwnerId(ownerId);
    assertThat(policyWaivers).isNotEmpty().hasSize(1);
    assertPolicyWaiver(ownerId, policy, policyViolation, policyWaivers.get(0), comment, hash, null);
  }

  private void assertPolicyWaiver(String ownerId,
                                  Policy policy,
                                  PolicyViolation policyViolation,
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
    assertThat(policyWaiver.getConstraintFactsJson()).isEqualTo(policyViolation.getConstraintFactsJson());
  }
}
