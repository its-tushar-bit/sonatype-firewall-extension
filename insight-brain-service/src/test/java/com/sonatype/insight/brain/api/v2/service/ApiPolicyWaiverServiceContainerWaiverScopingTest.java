/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Set;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.api.v2.dto.ApiPageResult;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO.PolicyContainerWaiverData;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.AuthorizationException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for getAllPolicyContainerWaivers() scoping in ApiPolicyWaiverService.
 */
@Category(SlowTest.class)
public class ApiPolicyWaiverServiceContainerWaiverScopingTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiPolicyWaiverService apiPolicyWaiverService;

  @Inject
  private OrganizationDAO organizationDAO;

  @Inject
  private PolicyWaiverDAO policyWaiverDAO;

  private RepositoryManager repoManager;

  private Repository proxyRepo1;

  private Repository proxyRepo2;

  private Organization org1;

  private Organization org2;

  private Application containerImage1;

  private Application containerImage2;

  @Before
  public void setUp() {
    // Create repository manager and proxy repos
    repoManager = tempEntity.newRepositoryManager();
    proxyRepo1 = tempEntity.newRepository(repoManager, "proxyRepo1", RepositoryType.proxy, "docker");
    proxyRepo2 = tempEntity.newRepository(repoManager, "proxyRepo2", RepositoryType.proxy, "docker");

    // Create organizations with related repository links
    // Container image orgs are linked to proxy repos via relatedRepositoryId
    org1 = tempEntity.newOrganization();
    org1.setRelatedRepositoryId(proxyRepo1.getId());
    organizationDAO.update(org1);

    org2 = tempEntity.newOrganization();
    org2.setRelatedRepositoryId(proxyRepo2.getId());
    organizationDAO.update(org2);

    // Create applications (container images) under the organizations
    containerImage1 = tempEntity.newApplication(org1.getId());
    containerImage2 = tempEntity.newApplication(org2.getId());
  }

  @Test
  public void testGetAllPolicyContainerWaivers_FullAccessUser_SeesAllWaivers() {
    grantPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID, Permission.READ);

    createContainerWaiver(containerImage1);
    createContainerWaiver(containerImage2);

    ApiPageResult<PolicyContainerWaiverData> result = apiPolicyWaiverService.getAllPolicyContainerWaivers(1, 100);

    assertThat(result.getResults()).hasSize(2);
    Set<String> ownerIds =
        result.getResults().stream().map(w -> w.ownerId()).collect(java.util.stream.Collectors.toSet());
    assertThat(ownerIds).contains(containerImage1.getId(), containerImage2.getId());
  }

  @Test
  public void testGetAllPolicyContainerWaivers_ScopedUser_SeesOnlyPermittedRepos() {
    grantReadPermission(proxyRepo1.getId());

    createContainerWaiver(containerImage1);
    createContainerWaiver(containerImage2);

    ApiPageResult<PolicyContainerWaiverData> result = apiPolicyWaiverService.getAllPolicyContainerWaivers(1, 100);

    assertThat(result.getResults()).hasSize(1);
    assertThat(result.getResults().get(0).ownerId()).isEqualTo(containerImage1.getId());
  }

  @Test
  public void testGetAllPolicyContainerWaivers_NoAccess_ThrowsAuthorizationException() {
    login();

    assertThatThrownBy(() -> apiPolicyWaiverService.getAllPolicyContainerWaivers(1, 100))
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining("No access to any proxy repository");
  }

  @Test
  public void testGetAllPolicyContainerWaivers_ScopedUserNoMatchingApps_ReturnsEmpty() {
    grantReadPermission(proxyRepo2.getId());

    // Waiver for containerImage1 (linked to proxyRepo1, not proxyRepo2)
    createContainerWaiver(containerImage1);

    ApiPageResult<PolicyContainerWaiverData> result = apiPolicyWaiverService.getAllPolicyContainerWaivers(1, 100);

    assertThat(result.getResults()).isEmpty();
  }

  /**
   * Creates a container-image waiver directly via DAO to avoid service-layer auth checks in test setup.
   */
  private void createContainerWaiver(Application containerImage) {
    Policy policy = tempEntity.newPolicy(containerImage.getOrganizationId());
    PolicyWaiver waiver = new PolicyWaiver(null, policy.getId(), containerImage.getId(), "test-setup");
    waiver.setForContainerImage(true);
    policyWaiverDAO.insert(waiver);
  }
}
