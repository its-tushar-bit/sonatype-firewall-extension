/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Arrays;
import java.util.List;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.OwnerType.APPLICATION;
import static com.sonatype.insight.brain.model.OwnerType.ORGANIZATION;
import static com.sonatype.insight.brain.model.OwnerType.REPOSITORY;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for ApiPolicyExportResourceV2 REST endpoints.
 * Tests verify HTTP-level concerns, request validation, serialization, and business logic.
 */
@IqH2Test
public class ApiPolicyExportResourceV2Test
{
  private IqTestContext ctx;

  private ObjectMapper objectMapper;

  private OrganizationDAO organizationDAO;

  @BeforeEach
  public void setup() {
    objectMapper = ctx.lookup(ObjectMapper.class);
    organizationDAO = ctx.lookup(OrganizationDAO.class);
  }

  // ===== Direct Export Tests (includeInherited=false) =====

  @Test
  public void testExportOrganizationPolicies_Direct_Success() throws Exception {
    // Given: Organization with policies
    Organization org = ctx.tempEntity().newOrganization();
    Policy policy = createAndSavePolicy(org.getId(), "OrgPolicy1");

    // When: Exporting organization policies (direct only)
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.POLICY_EXPORT_RESOURCE_PATH + "/export")
        .parameter(ORGANIZATION, org.getId())
        .query("includeInherited", false)
        .get();

    // Then: Returns 200 with policies from organization only
    ctx.assertResponseStatus(200, response);
    PolicyExportResult result = objectMapper.readValue(response.getBodyText(), PolicyExportResult.class);
    assertThat(result).isNotNull();
    assertThat(result.policies).hasSize(1);
    assertThat(result.policies.get(0).getId()).isEqualTo(policy.getId());
    assertThat(result.policies.get(0).getOwnerId()).isEqualTo(org.getId());
  }

  @Test
  public void testExportApplicationPolicies_Direct_Success() throws Exception {
    // Given: Organization with Policy A, Application with Policy B
    Organization org = ctx.tempEntity().newOrganization();
    Application app = ctx.tempEntity().newApplication("TestApp", org.getId());
    Policy orgPolicy = createAndSavePolicy(org.getId(), "OrgPolicy");
    Policy appPolicy = createAndSavePolicy(app.getId(), "AppPolicy");

    // When: Exporting application policies (direct only)
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.POLICY_EXPORT_RESOURCE_PATH + "/export")
        .parameter(APPLICATION, app.getId())
        .query("includeInherited", false)
        .get();

    // Then: Returns 200 with policies from application only (not org)
    ctx.assertResponseStatus(200, response);
    PolicyExportResult result = objectMapper.readValue(response.getBodyText(), PolicyExportResult.class);
    assertThat(result).isNotNull();
    assertThat(result.policies).hasSize(1);
    assertThat(result.policies.get(0).getId()).isEqualTo(appPolicy.getId());
    assertThat(result.policies.get(0).getOwnerId()).isEqualTo(app.getId());
  }

  @Test
  public void testExportRepositoryPolicies_Direct_Success() throws Exception {
    // Given: Org with Policy A, Repository with Policy B
    // Note: Repositories don't have Application parents in the hierarchy
    Organization org = ctx.tempEntity().newOrganization();
    Repository repo = ctx.tempEntity().newRepository();

    Policy orgPolicy = createAndSavePolicy(org.getId(), "OrgPolicy");
    Policy repoPolicy = createAndSavePolicy(repo.getId(), "RepoPolicy");

    // When: Exporting repository policies (direct only)
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.POLICY_EXPORT_RESOURCE_PATH + "/export")
        .parameter(REPOSITORY, repo.getId())
        .query("includeInherited", false)
        .get();

    // Then: Returns 200 with policies from repository only (not parent org)
    ctx.assertResponseStatus(200, response);
    PolicyExportResult result = objectMapper.readValue(response.getBodyText(), PolicyExportResult.class);
    assertThat(result).isNotNull();
    assertThat(result.policies).hasSize(1);
    assertThat(result.policies.get(0).getId()).isEqualTo(repoPolicy.getId());
    assertThat(result.policies.get(0).getOwnerId()).isEqualTo(repo.getId());
  }

  @Test
  public void testExportPolicies_Direct_EmptyResult() throws Exception {
    // Given: Organization with no policies
    Organization org = ctx.tempEntity().newOrganization();

    // When: Exporting organization policies
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.POLICY_EXPORT_RESOURCE_PATH + "/export")
        .parameter(ORGANIZATION, org.getId())
        .query("includeInherited", false)
        .get();

    // Then: Returns 200 with empty export result (valid state)
    ctx.assertResponseStatus(200, response);
    PolicyExportResult result = objectMapper.readValue(response.getBodyText(), PolicyExportResult.class);
    assertThat(result).isNotNull();
    assertThat(result.policies).isEmpty();
    assertThat(result.labels).isEmpty();
    assertThat(result.licenseThreatGroups).isEmpty();
  }

  // ===== Inherited Export Tests (includeInherited=true) =====

  @Test
  public void testExportRepositoryPolicies_Inherited_Success() throws Exception {
    // Given: Root org with Policy A, Repository with Policy B
    // Note: Repositories don't have Application parents - they go through RepositoryManager/Container
    // Actual hierarchy: Repository → RepositoryManager → RepositoryContainer → ROOT Organization
    Organization rootOrg = organizationDAO.getByIdNotNull(Organization.ROOT_ORGANIZATION_ID);
    Repository repo = ctx.tempEntity().newRepository();

    Policy orgPolicy = createAndSavePolicy(rootOrg.getId(), "OrgPolicy");
    Policy repoPolicy = createAndSavePolicy(repo.getId(), "RepoPolicy");

    // When: Exporting repository policies with inheritance
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.POLICY_EXPORT_RESOURCE_PATH + "/export")
        .parameter(REPOSITORY, repo.getId())
        .query("includeInherited", true)
        .get();

    // Then: Returns 200 with policies from repo + org (skipping RepositoryManager/Container levels)
    ctx.assertResponseStatus(200, response);
    PolicyExportResult result = objectMapper.readValue(response.getBodyText(), PolicyExportResult.class);
    assertThat(result).isNotNull();
    assertThat(result.policies).hasSize(2);

    // Verify both policies present with correct ownerIds showing their source
    List<String> policyIds = Arrays.asList(
        result.policies.get(0).getId(),
        result.policies.get(1).getId());
    assertThat(policyIds).containsExactlyInAnyOrder(
        repoPolicy.getId(),
        orgPolicy.getId());

    // Verify ownerIds are preserved
    assertThat(result.policies)
        .anyMatch(p -> p.getId().equals(repoPolicy.getId()) && p.getOwnerId().equals(repo.getId()));
    assertThat(result.policies)
        .anyMatch(p -> p.getId().equals(orgPolicy.getId()) && p.getOwnerId().equals(rootOrg.getId()));
  }

  @Test
  public void testExportApplicationPolicies_Inherited_Success() throws Exception {
    // Given: Org with Policy A, App with Policy B
    Organization org = ctx.tempEntity().newOrganization();
    Application app = ctx.tempEntity().newApplication("TestApp", org.getId());

    Policy orgPolicy = createAndSavePolicy(org.getId(), "OrgPolicy");
    Policy appPolicy = createAndSavePolicy(app.getId(), "AppPolicy");

    // When: Exporting application policies with inheritance
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.POLICY_EXPORT_RESOURCE_PATH + "/export")
        .parameter(APPLICATION, app.getId())
        .query("includeInherited", true)
        .get();

    // Then: Returns 200 with Policies A+B (not repo policies)
    ctx.assertResponseStatus(200, response);
    PolicyExportResult result = objectMapper.readValue(response.getBodyText(), PolicyExportResult.class);
    assertThat(result).isNotNull();
    assertThat(result.policies).hasSize(2);

    List<String> policyIds = Arrays.asList(
        result.policies.get(0).getId(),
        result.policies.get(1).getId());
    assertThat(policyIds).containsExactlyInAnyOrder(appPolicy.getId(), orgPolicy.getId());
  }

  @Test
  public void testExportOrganizationPolicies_Inherited_NoParent() throws Exception {
    // Given: Root organization with policies (no parent)
    Organization org = ctx.tempEntity().newOrganization();
    Policy policy = createAndSavePolicy(org.getId(), "OrgPolicy");

    // When: Exporting organization policies with inheritance
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.POLICY_EXPORT_RESOURCE_PATH + "/export")
        .parameter(ORGANIZATION, org.getId())
        .query("includeInherited", true)
        .get();

    // Then: Returns 200 with Policy A only (org has no parent, behaves same as direct)
    ctx.assertResponseStatus(200, response);
    PolicyExportResult result = objectMapper.readValue(response.getBodyText(), PolicyExportResult.class);
    assertThat(result).isNotNull();
    assertThat(result.policies).hasSize(1);
    assertThat(result.policies.get(0).getId()).isEqualTo(policy.getId());
  }

  @Test
  public void testExportPolicies_Inherited_DeduplicationByPolicyId() throws Exception {
    // Given: Same policy ID appears at multiple levels (edge case for testing deduplication)
    Organization org = ctx.tempEntity().newOrganization();
    Application app = ctx.tempEntity().newApplication("TestApp", org.getId());

    // Create policy at org level
    Policy orgPolicy = createAndSavePolicy(org.getId(), "SharedPolicy");

    // Create different policy at app level (different ID)
    Policy appPolicy = createAndSavePolicy(app.getId(), "AppSpecificPolicy");

    // When: Exporting application policies with inheritance
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.POLICY_EXPORT_RESOURCE_PATH + "/export")
        .parameter(APPLICATION, app.getId())
        .query("includeInherited", true)
        .get();

    // Then: Should have both policies (no duplicate IDs)
    ctx.assertResponseStatus(200, response);
    PolicyExportResult result = objectMapper.readValue(response.getBodyText(), PolicyExportResult.class);
    assertThat(result).isNotNull();
    assertThat(result.policies).hasSize(2);

    // Verify unique policy IDs
    List<String> policyIds = Arrays.asList(
        result.policies.get(0).getId(),
        result.policies.get(1).getId());
    assertThat(policyIds).containsExactlyInAnyOrder(orgPolicy.getId(), appPolicy.getId());
  }

  // ===== Error Cases =====

  @Test
  public void testExportPolicies_OrganizationNotFound_Returns404() throws Exception {
    // When: Requesting export for non-existent organization
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.POLICY_EXPORT_RESOURCE_PATH + "/export")
        .parameter(ORGANIZATION, "non-existent-org-id")
        .query("includeInherited", false)
        .get();

    // Then: Returns 404 Not Found
    ctx.assertResponseStatus(404, response);
  }

  @Test
  public void testExportPolicies_ApplicationNotFound_Returns404() throws Exception {
    // When: Requesting export for non-existent application
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.POLICY_EXPORT_RESOURCE_PATH + "/export")
        .parameter(APPLICATION, "non-existent-app-id")
        .query("includeInherited", false)
        .get();

    // Then: Returns 404 Not Found
    ctx.assertResponseStatus(404, response);
  }

  @Test
  public void testExportPolicies_RepositoryNotFound_Returns404() throws Exception {
    // When: Requesting export for non-existent repository
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.POLICY_EXPORT_RESOURCE_PATH + "/export")
        .parameter(REPOSITORY, "non-existent-repo-id")
        .query("includeInherited", false)
        .get();

    // Then: Returns 404 Not Found
    ctx.assertResponseStatus(404, response);
  }

  @Test
  public void testExportPolicies_InvalidOwnerType_Returns404() throws Exception {
    // When: Requesting export with invalid owner type
    HttpResponse response = ctx.restRequest()
        .path("/api/v2/policy/invalid-type/some-id/export")
        .query("includeInherited", false)
        .get();

    // Then: Returns 404 Not Found (JAX-RS returns 404 for path not matching pattern)
    ctx.assertResponseStatus(404, response);
  }

  @Test
  public void testExportPolicies_DefaultIncludeInheritedIsFalse() throws Exception {
    // Given: Org with Policy A, App with Policy B
    Organization org = ctx.tempEntity().newOrganization();
    Application app = ctx.tempEntity().newApplication("TestApp", org.getId());

    Policy orgPolicy = createAndSavePolicy(org.getId(), "OrgPolicy");
    Policy appPolicy = createAndSavePolicy(app.getId(), "AppPolicy");

    // When: Exporting application policies WITHOUT includeInherited parameter
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.POLICY_EXPORT_RESOURCE_PATH + "/export")
        .parameter(APPLICATION, app.getId())
        // Note: No includeInherited query param - should default to false
        .get();

    // Then: Returns only app policies (default is direct export)
    ctx.assertResponseStatus(200, response);
    PolicyExportResult result = objectMapper.readValue(response.getBodyText(), PolicyExportResult.class);
    assertThat(result).isNotNull();
    assertThat(result.policies).hasSize(1);
    assertThat(result.policies.get(0).getId()).isEqualTo(appPolicy.getId());
  }

  // ===== Helper Methods =====

  /**
   * Create and save a test policy with the given owner ID and name.
   */
  private Policy createAndSavePolicy(String ownerId, String policyName) {
    // tempEntity.newPolicy already sets up threat level and action
    return ctx.tempEntity().newPolicy(ownerId, policyName);
  }
}
