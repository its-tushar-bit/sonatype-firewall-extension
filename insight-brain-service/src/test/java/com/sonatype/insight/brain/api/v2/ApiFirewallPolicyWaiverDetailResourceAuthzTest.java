/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverDTO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.brain.common.test.SlowTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class ApiFirewallPolicyWaiverDetailResourceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiFirewallPolicyWaiverDetailResource resource;

  @Inject
  private OrganizationDAO organizationDAO;

  private Policy policy;

  private PolicyWaiver rootOrgWaiver;

  private PolicyWaiver repoWaiver;

  private Repository proxyRepo;

  @Before
  public void setUpWaivers() {
    policy = tempEntity.newPolicy(org);
    rootOrgWaiver = tempEntity.newWaiver(policy.getId(), Organization.ROOT_ORGANIZATION_ID);
    proxyRepo = tempEntity.newRepository(repositoryManager, "proxy-repo", RepositoryType.proxy, "maven2");
    repoWaiver = tempEntity.newWaiver(policy.getId(), proxyRepo.getId());
  }

  // --- Full-access user (container READ) ---

  @Test
  public void testGetPolicyWaiver_FullAccessUser_RootOrgWaiver() {
    grantPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID, Permission.READ);

    ApiPolicyWaiverDTO dto = resource.getPolicyWaiver(
        OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID, rootOrgWaiver.getId());

    assertThat(dto).isNotNull();
    assertThat(dto.policyWaiverId).isEqualTo(rootOrgWaiver.getId());
  }

  @Test
  public void testGetPolicyWaiver_FullAccessUser_RepoWaiver() {
    grantPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID, Permission.READ);

    ApiPolicyWaiverDTO dto = resource.getPolicyWaiver(
        OwnerType.REPOSITORY, proxyRepo.getId(), repoWaiver.getId());

    assertThat(dto).isNotNull();
    assertThat(dto.policyWaiverId).isEqualTo(repoWaiver.getId());
  }

  // --- Scoped user (READ on specific repo) succeeds ---

  @Test
  public void testGetPolicyWaiver_ScopedUser_RootOrgWaiver_Succeeds() {
    grantPermission(proxyRepo.getId(), Permission.READ);

    ApiPolicyWaiverDTO dto = resource.getPolicyWaiver(
        OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID, rootOrgWaiver.getId());

    assertThat(dto).isNotNull();
    assertThat(dto.policyWaiverId).isEqualTo(rootOrgWaiver.getId());
  }

  @Test
  public void testGetPolicyWaiver_ScopedUser_OwnPermittedRepo_Succeeds() {
    grantPermission(proxyRepo.getId(), Permission.READ);

    ApiPolicyWaiverDTO dto = resource.getPolicyWaiver(
        OwnerType.REPOSITORY, proxyRepo.getId(), repoWaiver.getId());

    assertThat(dto).isNotNull();
    assertThat(dto.policyWaiverId).isEqualTo(repoWaiver.getId());
  }

  @Test
  public void testGetPolicyWaiver_ScopedUser_ContainerAppWaiver_Succeeds() {
    // Container image apps live under shadow orgs whose relatedRepositoryId links to the docker proxy repo.
    // Set up: shadow org → proxyRepo, container app → shadow org.
    Organization shadowOrg = tempEntity.newOrganization("shadow-org-for-docker");
    shadowOrg.setRelatedRepositoryId(proxyRepo.getId());
    organizationDAO.update(shadowOrg);
    Application containerApp = tempEntity.newApplication("container-app", shadowOrg.getId());
    PolicyWaiver appWaiver = tempEntity.newWaiver(policy.getId(), containerApp.getId());

    grantPermission(proxyRepo.getId(), Permission.READ);

    ApiPolicyWaiverDTO dto = resource.getPolicyWaiver(
        OwnerType.APPLICATION, containerApp.getId(), appWaiver.getId());

    assertThat(dto).isNotNull();
    assertThat(dto.policyWaiverId).isEqualTo(appWaiver.getId());
  }

  // --- Scoped user is blocked from out-of-scope owners ---

  @Test(expected = UnauthorizedException.class)
  public void testGetPolicyWaiver_ScopedUser_DifferentRepo_Unauthorized() {
    Repository otherRepo =
        tempEntity.newRepository(repositoryManager, "other-proxy-repo", RepositoryType.proxy, "maven2");
    PolicyWaiver otherRepoWaiver = tempEntity.newWaiver(policy.getId(), otherRepo.getId());

    grantPermission(proxyRepo.getId(), Permission.READ); // access to proxyRepo only, not otherRepo

    resource.getPolicyWaiver(OwnerType.REPOSITORY, otherRepo.getId(), otherRepoWaiver.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetPolicyWaiver_ScopedUser_AppNotLinkedToPermittedRepo_Unauthorized() {
    // app lives under org with NO relatedRepositoryId link to any permitted repo
    PolicyWaiver appWaiver = tempEntity.newWaiver(policy.getId(), app.getId());

    grantPermission(proxyRepo.getId(), Permission.READ);

    resource.getPolicyWaiver(OwnerType.APPLICATION, app.getId(), appWaiver.getId());
  }

  // --- No-repo-access user gets 403 ---

  @Test(expected = UnauthorizedException.class)
  public void testGetPolicyWaiver_NoRepoAccess_Unauthorized() {
    login();
    resource.getPolicyWaiver(
        OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID, rootOrgWaiver.getId());
  }

  // --- Anonymous user gets 401 ---

  @Test(expected = UnauthenticatedException.class)
  public void testGetPolicyWaiver_Unauthenticated() {
    resource.getPolicyWaiver(
        OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID, rootOrgWaiver.getId());
  }
}
