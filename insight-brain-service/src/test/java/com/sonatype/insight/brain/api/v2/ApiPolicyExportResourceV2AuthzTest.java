/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.junit.Test;

import static com.sonatype.insight.brain.model.OwnerType.APPLICATION;
import static com.sonatype.insight.brain.model.OwnerType.ORGANIZATION;
import static com.sonatype.insight.brain.model.OwnerType.REPOSITORY;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Authorization tests for ApiPolicyExportResourceV2.
 * Tests verify that proper READ permissions are required for all policy export operations.
 *
 * Following the pattern from CiConfigurationServiceAuthzTest.java.
 */
public class ApiPolicyExportResourceV2AuthzTest
    extends AbstractResourceAuthzTest
{
  // ===== Organization Export Authorization =====

  @Test
  public void testExportOrganizationPolicies_AuthorizedUser_Returns200() throws Exception {
    // Given: Organization with policy
    Organization org = tempEntity.newOrganization();
    createAndSavePolicy(org.getId(), "OrgPolicy");

    // When: Authenticated admin user (has READ permission)
    HttpResponse response = restRequest()
        .path(PublicApiPaths.POLICY_EXPORT_RESOURCE_PATH + "/export")
        .parameter(ORGANIZATION, org.getId())
        .query("includeInherited", false)
        .auth()
        .get();

    // Then: Should return 200 OK
    assertThat(response.getStatusCode()).isEqualTo(200);
  }

  @Test
  public void testExportOrganizationPolicies_Unauthenticated_Returns401() throws Exception {
    // Given: Organization with policy
    Organization org = tempEntity.newOrganization();
    createAndSavePolicy(org.getId(), "OrgPolicy");

    // When: Unauthenticated request
    HttpResponse response = restRequest()
        .path(PublicApiPaths.POLICY_EXPORT_RESOURCE_PATH + "/export")
        .parameter(ORGANIZATION, org.getId())
        .query("includeInherited", false)
        .anon()
        .get();

    // Then: Should return 401 Unauthorized
    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  public void testExportOrganizationPolicies_UnauthorizedUser_Returns403() throws Exception {
    // Given: Organization with policy, user has no READ permission
    createAndSavePolicy(org.getId(), "OrgPolicy");

    // When: Unauthorized user attempts export
    HttpResponse response = restRequest()
        .path(PublicApiPaths.POLICY_EXPORT_RESOURCE_PATH + "/export")
        .parameter(ORGANIZATION, org.getId())
        .query("includeInherited", false)
        .auth(unauthorized)
        .get();

    // Then: Should return 403 Forbidden
    assertThat(response.getStatusCode()).isEqualTo(403);
  }

  // ===== Application Export Authorization =====

  @Test
  public void testExportApplicationPolicies_AuthorizedUser_Returns200() throws Exception {
    // Given: Application with policy
    Application app = tempEntity.newApplication(org.getPublicId());
    createAndSavePolicy(app.getId(), "AppPolicy");

    // When: Authenticated admin user (has READ permission)
    HttpResponse response = restRequest()
        .path(PublicApiPaths.POLICY_EXPORT_RESOURCE_PATH + "/export")
        .parameter(APPLICATION, app.getId())
        .query("includeInherited", false)
        .auth()
        .get();

    // Then: Should return 200 OK
    assertThat(response.getStatusCode()).isEqualTo(200);
  }

  @Test
  public void testExportApplicationPolicies_Unauthenticated_Returns401() throws Exception {
    // Given: Application with policy
    Application app = tempEntity.newApplication(org.getPublicId());
    createAndSavePolicy(app.getId(), "AppPolicy");

    // When: Unauthenticated request
    HttpResponse response = restRequest()
        .path(PublicApiPaths.POLICY_EXPORT_RESOURCE_PATH + "/export")
        .parameter(APPLICATION, app.getId())
        .query("includeInherited", false)
        .anon()
        .get();

    // Then: Should return 401 Unauthorized
    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  public void testExportApplicationPolicies_UnauthorizedUser_Returns403() throws Exception {
    // Given: Application with policy, user has no READ permission
    createAndSavePolicy(app.getId(), "AppPolicy");

    // When: Unauthorized user attempts export
    HttpResponse response = restRequest()
        .path(PublicApiPaths.POLICY_EXPORT_RESOURCE_PATH + "/export")
        .parameter(APPLICATION, app.getId())
        .query("includeInherited", false)
        .auth(unauthorized)
        .get();

    // Then: Should return 403 Forbidden
    assertThat(response.getStatusCode()).isEqualTo(403);
  }

  // ===== Repository Export Authorization =====

  @Test
  public void testExportRepositoryPolicies_AuthorizedUser_Returns200() throws Exception {
    // Given: Repository with policy
    Repository repo = tempEntity.newRepository();
    createAndSavePolicy(repo.getId(), "RepoPolicy");

    // When: Authenticated admin user (has READ permission)
    HttpResponse response = restRequest()
        .path(PublicApiPaths.POLICY_EXPORT_RESOURCE_PATH + "/export")
        .parameter(REPOSITORY, repo.getId())
        .query("includeInherited", false)
        .auth()
        .get();

    // Then: Should return 200 OK
    assertThat(response.getStatusCode()).isEqualTo(200);
  }

  @Test
  public void testExportRepositoryPolicies_Unauthenticated_Returns401() throws Exception {
    // Given: Repository with policy
    Repository repo = tempEntity.newRepository();
    createAndSavePolicy(repo.getId(), "RepoPolicy");

    // When: Unauthenticated request
    HttpResponse response = restRequest()
        .path(PublicApiPaths.POLICY_EXPORT_RESOURCE_PATH + "/export")
        .parameter(REPOSITORY, repo.getId())
        .query("includeInherited", false)
        .anon()
        .get();

    // Then: Should return 401 Unauthorized
    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  public void testExportRepositoryPolicies_UnauthorizedUser_Returns403() throws Exception {
    // Given: Repository with policy, user has no READ permission
    createAndSavePolicy(repo.getId(), "RepoPolicy");

    // When: Unauthorized user attempts export
    HttpResponse response = restRequest()
        .path(PublicApiPaths.POLICY_EXPORT_RESOURCE_PATH + "/export")
        .parameter(REPOSITORY, repo.getId())
        .query("includeInherited", false)
        .auth(unauthorized)
        .get();

    // Then: Should return 403 Forbidden
    assertThat(response.getStatusCode()).isEqualTo(403);
  }

  // ===== Inherited Export Authorization =====

  @Test
  public void testExportPolicies_InheritedFlag_RequiresPermissionOnRequestedOwner() throws Exception {
    // Given: Org with Policy A, App with Policy B
    // Authorized user has READ permission on org (which grants permission on child app via hierarchy)
    createAndSavePolicy(org.getId(), "OrgPolicy");
    createAndSavePolicy(app.getId(), "AppPolicy");

    // Grant permission on org (hierarchical permissions apply to child app)
    grantReadPermission(org.getId());

    // When: User with READ permission on org attempts app export
    HttpResponse response = restRequest()
        .path(PublicApiPaths.POLICY_EXPORT_RESOURCE_PATH + "/export")
        .parameter(APPLICATION, app.getId())
        .query("includeInherited", true)
        .auth(authorized)
        .get();

    // Then: Should return 200 OK
    // IQ Server uses hierarchical permissions - parent permissions apply to children
    assertThat(response.getStatusCode()).isEqualTo(200);
  }

  @Test
  public void testExportPolicies_InheritedFlag_UnauthorizedUser_Returns403() throws Exception {
    // Given: Org with Policy A, App with Policy B
    // Unauthorized user has NO permissions on org or app
    createAndSavePolicy(org.getId(), "OrgPolicy");
    createAndSavePolicy(app.getId(), "AppPolicy");

    // NO permissions granted

    // When: User without READ permission attempts app export with inherited flag
    HttpResponse response = restRequest()
        .path(PublicApiPaths.POLICY_EXPORT_RESOURCE_PATH + "/export")
        .parameter(APPLICATION, app.getId())
        .query("includeInherited", true)
        .auth(unauthorized)
        .get();

    // Then: Should return 403 Forbidden
    assertThat(response.getStatusCode()).isEqualTo(403);
  }

  @Test
  public void testExportPolicies_InheritedFlag_AuthorizedUser_Returns200() throws Exception {
    // Given: Org with Policy A, App with Policy B
    // Authorized user has READ permission on app (which allows export)
    createAndSavePolicy(org.getId(), "OrgPolicy");
    createAndSavePolicy(app.getId(), "AppPolicy");

    // Grant permission on app
    grantReadPermission(app.getId());

    // When: Authorized user requests inherited export
    HttpResponse response = restRequest()
        .path(PublicApiPaths.POLICY_EXPORT_RESOURCE_PATH + "/export")
        .parameter(APPLICATION, app.getId())
        .query("includeInherited", true)
        .auth(authorized)
        .get();

    // Then: Should return 200 OK with policies from both levels
    assertThat(response.getStatusCode()).isEqualTo(200);
  }

  @Test
  public void testExportRepositoryPolicies_InheritedFlag_RequiresPermissionOnRepository() throws Exception {
    // Given: Repository with policies in hierarchy
    // Note: Repository hierarchy goes through RepositoryManager/Container, not Application
    // Authorized user has READ permission on org but NOT on repo
    createAndSavePolicy(org.getId(), "OrgPolicy");
    createAndSavePolicy(repo.getId(), "RepoPolicy");

    // Grant permission on org, but not repo
    grantReadPermission(org.getId());

    // When: User without READ permission on repo attempts inherited export
    HttpResponse response = restRequest()
        .path(PublicApiPaths.POLICY_EXPORT_RESOURCE_PATH + "/export")
        .parameter(REPOSITORY, repo.getId())
        .query("includeInherited", true)
        .auth(authorized)
        .get();

    // Then: Should return 403 Forbidden
    // Authorization is checked at the repository level
    assertThat(response.getStatusCode()).isEqualTo(403);
  }

  // ===== Helper Methods =====

  /**
   * Create and save a test policy with the given owner ID and name.
   */
  private Policy createAndSavePolicy(String ownerId, String policyName) {
    // tempEntity.newPolicy already sets up threat level and action
    return tempEntity.newPolicy(ownerId, policyName);
  }
}
