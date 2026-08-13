/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.dto.ApiPageResult;
import com.sonatype.insight.brain.api.v2.dto.containerimagewaiver.ApiContainerImageWaiverDTO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO.PolicyContainerWaiverData;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverReason;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.variant.IqPostgresTest;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.license.model.LicensedFeature;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.time.DateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.api.PublicApiPaths.FIREWALL_CONTAINER_IMAGE_RESOURCE_PATH;
import static com.sonatype.insight.brain.api.v2.ApiFirewallContainerImagePolicyWaiverResource.CONTAINER_IMAGE_ID;
import static com.sonatype.insight.brain.api.v2.ApiFirewallContainerImagePolicyWaiverResource.POLICY_WAIVER;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_COMPONENTS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Converted from {@code ApiFirewallContainerImagePolicyWaiverResourceTest} to run against the
 * reused PostgreSQL-backed {@link IqPostgresTest} server.
 */
@IqPostgresTest
class IqPostgresApiFirewallContainerImagePolicyWaiverResourceTest
{
  private static final ObjectMapper JSON = new ObjectMapper();

  // Injected by IqPostgresServerExtension: the extension owns the shared, reused server.
  private IqTestContext ctx;

  private RepositoryDAO repositoryDAO;

  private OrganizationDAO organizationDAO;

  private PolicyWaiverDAO policyWaiverDAO;

  private Application application;

  private PolicyEvaluation policyEvaluation;

  private Policy policy;

  private PolicyViolation policyViolation;

  @BeforeEach
  void setUp() throws Exception {
    repositoryDAO = ctx.lookup(RepositoryDAO.class);
    organizationDAO = ctx.lookup(OrganizationDAO.class);
    policyWaiverDAO = ctx.lookup(PolicyWaiverDAO.class);

    Organization organization = ctx.tempEntity().newOrganization();
    application = ctx.tempEntity().newApplicationWithParent(organization);
    policy = ctx.tempEntity().newPolicy(application);
    policyEvaluation =
        ctx.tempEntity().newPolicyEvaluation(application.getId(), ProxyStageType.ID, "scanId");
    policyViolation = ctx.tempEntity()
        .newPolicyViolation(policyEvaluation, policy, 5, PolicyThreatCategory.SECURITY,
            "g", "a", "v", "hash", FailActionType.ID);
    Repository repository =
        ctx.tempEntity()
            .newRepository(ctx.tempEntity().newRepositoryManager(), "docker-repo", RepositoryType.proxy,
                "docker");
    repository.setRelatedOrganizationId(organization.getId());
    repositoryDAO.update(repository);
    organization.setRelatedRepositoryId(repository.getId());
    organizationDAO.update(organization);

    ctx.setFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
  }

  @AfterEach
  void tearDown() {
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(FIREWALL_CONTAINER_IMAGE_RESOURCE_PATH);
  }

  @Test
  void testAddWaiver() throws Exception {
    PolicyWaiverReason policyWaiverReason = ctx.tempEntity().newWaiverReason("type", "reasons");

    ApiContainerImageWaiverDTO waiverDTO = new ApiContainerImageWaiverDTO();
    waiverDTO.expiryTime = DateUtils.addDays(new Date(), 1);
    waiverDTO.waiverReasonId = policyWaiverReason.getId();
    waiverDTO.comment = "Container image waiver comment";

    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getActiveByOwnerId(application.getId());
    assertThat(policyWaivers).hasSize(0);

    HttpResponse response = restRequest()
        .path(CONTAINER_IMAGE_ID + POLICY_WAIVER)
        .parameter(application.getId())
        .body(waiverDTO)
        .post();

    ctx.assertResponseStatus(204, response);

    policyWaivers = policyWaiverDAO.getActiveByOwnerId(application.getId());
    assertThat(policyWaivers).hasSize(2);

    policyWaivers = new ArrayList<>(policyWaivers);
    policyWaivers.sort(Comparator.comparing(PolicyWaiver::getHash, Comparator.nullsLast(Comparator.naturalOrder())));

    PolicyWaiver createdWaiver = policyWaivers.get(0);
    assertThat(createdWaiver.getExpiryTime()).isEqualTo(waiverDTO.expiryTime);
    assertThat(createdWaiver.getWaiverReasonId()).isEqualTo(policyWaiverReason.getId());
    assertThat(createdWaiver.getComment()).isEqualTo("Container image waiver comment");
    assertThat(createdWaiver.getComponentMatchStrategy()).isEqualTo(EXACT_COMPONENT);
    assertThat(createdWaiver.isExpireWhenRemediationAvailable()).isFalse();
    assertThat(createdWaiver.isForContainerImageComponent()).isTrue();
    assertThat(createdWaiver.isForContainerImage()).isFalse();

    createdWaiver = policyWaivers.get(1);
    assertThat(createdWaiver.getExpiryTime()).isEqualTo(waiverDTO.expiryTime);
    assertThat(createdWaiver.getWaiverReasonId()).isEqualTo(policyWaiverReason.getId());
    assertThat(createdWaiver.getComment()).isEqualTo("Container image waiver comment");
    assertThat(createdWaiver.getComponentMatchStrategy()).isEqualTo(ALL_COMPONENTS);
    assertThat(createdWaiver.isExpireWhenRemediationAvailable()).isFalse();
    assertThat(createdWaiver.isForContainerImageComponent()).isFalse();
    assertThat(createdWaiver.isForContainerImage()).isTrue();
  }

  @Test
  void testDeleteContainerImagePolicyWaiver() throws Exception {
    PolicyWaiver policyWaiverImage = new PolicyWaiver(policyViolation.getPolicyId(), application.getId(), "comment");
    policyWaiverImage.setHash("hash1");
    policyWaiverImage.setForContainerImage(true);
    policyWaiverDAO.insert(policyWaiverImage);
    PolicyWaiver policyWaiverComponent =
        new PolicyWaiver(policyViolation.getPolicyId(), application.getId(), "comment");
    policyWaiverImage.setHash("hash2");
    policyWaiverComponent.setForContainerImageComponent(true);
    policyWaiverDAO.insert(policyWaiverComponent);

    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getActiveByOwnerId(application.getId());
    assertThat(policyWaivers).hasSize(2);

    HttpResponse response = restRequest()
        .path(CONTAINER_IMAGE_ID + POLICY_WAIVER)
        .parameter(application.getId())
        .delete();

    assertThat(response.getStatusCode()).isEqualTo(204);

    policyWaivers = policyWaiverDAO.getActiveByOwnerId(application.getId());
    assertThat(policyWaivers).hasSize(0);
  }

  @Test
  void testGetWaivers() throws Exception {
    Organization organization = ctx.tempEntity().newOrganization();
    Policy policy = ctx.tempEntity().newPolicy(organization, 10);

    createContainerWaiver(application, policy, null, true, false);
    createContainerWaiver(application, policy, "comp01-hash1", false, true);

    HttpResponse response = restRequest()
        .path(POLICY_WAIVER)
        .query("page", 1)
        .query("pageSize", 10)
        .get();

    ctx.assertResponseStatus(200, response);

    ApiPageResult<PolicyContainerWaiverData> pageResult =
        getBodyByTypeReference(response.getBodyBytes(),
            new TypeReference<ApiPageResult<PolicyContainerWaiverData>>()
            {
            });
    assertThat(pageResult).isNotNull();
    assertThat(pageResult.getResults()).hasSize(1);
  }

  @Test
  void testGetWaivers_testPagination() throws Exception {
    Organization organization = ctx.tempEntity().newOrganization("org01");
    Policy policy = ctx.tempEntity().newPolicy(organization, 10);

    createContainerWaiver(application, policy, null, true, false);
    createContainerWaiver(application, policy, "comp01-hash1", false, true);

    Application application02 = ctx.tempEntity().newApplicationWithParent(organization);
    createContainerWaiver(application02, policy, null, true, false);
    createContainerWaiver(application02, policy, "comp02-hash1", false, true);

    // Get all results to verify pagination works correctly
    HttpResponse responsePage1 = restRequest()
        .path(POLICY_WAIVER)
        .query("page", 1)
        .query("pageSize", 1)
        .get();

    ApiPageResult<PolicyContainerWaiverData> pageResult1 =
        getBodyByTypeReference(responsePage1.getBodyBytes(),
            new TypeReference<ApiPageResult<PolicyContainerWaiverData>>()
            {
            });
    assertThat(pageResult1).isNotNull();
    assertThat(pageResult1.getResults()).hasSize(1);

    HttpResponse responsePage2 = restRequest()
        .path(POLICY_WAIVER)
        .query("page", 2)
        .query("pageSize", 1)
        .get();

    ApiPageResult<PolicyContainerWaiverData> pageResult2 =
        getBodyByTypeReference(responsePage2.getBodyBytes(),
            new TypeReference<ApiPageResult<PolicyContainerWaiverData>>()
            {
            });
    assertThat(pageResult2).isNotNull();
    assertThat(pageResult2.getResults()).hasSize(1);

    // Verify both pages have different waivers
    String page1AppScope = pageResult1.getResults().get(0).applicationScope();
    String page2AppScope = pageResult2.getResults().get(0).applicationScope();
    assertThat(page1AppScope).isNotEqualTo(page2AppScope);

    // Verify both applications are present across both pages
    assertThat(List.of(page1AppScope, page2AppScope))
        .containsExactlyInAnyOrder(application.getName(), application02.getName());
  }

  @Test
  void testGetWaivers_checkPageValue() throws Exception {
    HttpResponse response = restRequest()
        .path(POLICY_WAIVER)
        .query("page", 0)
        .query("pageSize", 0)
        .get();

    ctx.assertResponseStatus(400, response);
  }

  @Test
  void testGetWaivers_checkPageSizeValue() throws Exception {
    HttpResponse response = restRequest()
        .path(POLICY_WAIVER)
        .query("page", 1)
        .query("pageSize", 101)
        .get();

    ctx.assertResponseStatus(400, response);

    response = restRequest()
        .path(POLICY_WAIVER)
        .query("page", 1)
        .query("pageSize", 0)
        .get();

    ctx.assertResponseStatus(400, response);
  }

  @Test
  void testGetWaivers_returnsEmptyList() throws Exception {
    HttpResponse response = restRequest()
        .path(POLICY_WAIVER)
        .query("page", 1)
        .query("pageSize", 10)
        .get();

    ctx.assertResponseStatus(200, response);

    ApiPageResult<PolicyContainerWaiverData> pageResult =
        getBodyByTypeReference(response.getBodyBytes(),
            new TypeReference<ApiPageResult<PolicyContainerWaiverData>>()
            {
            });
    assertThat(pageResult.getResults()).isEmpty();
  }

  private void createContainerWaiver(
      final Application application,
      final Policy policy,
      final String hash,
      final boolean forContainerImage,
      final boolean forceContainerImageComponent)
  {
    PolicyWaiver waiverContainerComponent =
        new PolicyWaiver(hash, policy.getId(), application.getId(), "comment");
    waiverContainerComponent.setForContainerImageComponent(forceContainerImageComponent);
    waiverContainerComponent.setForContainerImage(forContainerImage);
    ctx.tempEntity().newWaiver(waiverContainerComponent);
  }

  private <T> T getBodyByTypeReference(byte[] bodyBytes, final TypeReference<T> typeRef) {
    try {
      return JSON.readValue(bodyBytes, typeRef);
    }
    catch (IOException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
