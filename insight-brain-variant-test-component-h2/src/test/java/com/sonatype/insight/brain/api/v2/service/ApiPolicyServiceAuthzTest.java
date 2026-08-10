/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Set;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiPolicyListDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@ComponentH2Test
public class ApiPolicyServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  private static final String PARENT_ORG_POLICY_NAME1 = "parent-org-policy1";

  private static final String ORG_POLICY_NAME1 = "org-policy1";

  private static final String APP_POLICY_NAME1 = "app-policy1";

  private static final String ORG_POLICY_NAME2 = "org-policy2";

  private static final String APP_POLICY_NAME2 = "app-policy2";

  public static final String REPOSITORY_POLICY_1 = "repository-policy1";

  public static final String REPOSITORY_MANAGER_POLICY_1 = "repository-manager-policy1";

  public static final String REPOSITORY_CONTAINER_POLICY_1 = "repository-container-policy1";

  public static final String REPOSITORY_POLICY_2 = "repository-policy2";

  @Inject
  private ApiPolicyService apiPolicyService;

  @BeforeEach
  public void setUpPolicies() {
    tempEntity.newPolicy(org.getParentOrganizationId(), PARENT_ORG_POLICY_NAME1);
    tempEntity.newPolicy(org.getId(), ORG_POLICY_NAME1);
    tempEntity.newPolicy(app.getId(), APP_POLICY_NAME1);

    Organization org2 = tempEntity.newOrganization();
    Application app2 = tempEntity.newApplication(org2.getId());

    tempEntity.newPolicy(org2.getId(), ORG_POLICY_NAME2);
    tempEntity.newPolicy(app2.getId(), APP_POLICY_NAME2);

    tempEntity.newPolicy(repositoryManager.getParentOwnerId(), REPOSITORY_CONTAINER_POLICY_1);
    tempEntity.newPolicy(repositoryManager.getId(), REPOSITORY_MANAGER_POLICY_1);
    tempEntity.newPolicy(repository.getId(), REPOSITORY_POLICY_1);

    Repository repository1 = tempEntity.newRepository(repositoryManager);
    tempEntity.newPolicy(repository1.getId(), REPOSITORY_POLICY_2);
  }

  @Test
  public void testGetPolicies_AuthorizedOneApp() {
    grantReadPermission(app.getId());
    ApiPolicyListDTO policyListDTO = apiPolicyService.getPolicies();
    assertThat(policyListDTO).isNotNull();
    assertThat(policyListDTO.policies).hasSize(3);
    Set<String> policyNames = Sets.newHashSet(policyListDTO.policies.get(0).name, policyListDTO.policies.get(1).name,
        policyListDTO.policies.get(2).name);
    assertThat(policyNames).containsExactlyInAnyOrder(PARENT_ORG_POLICY_NAME1, ORG_POLICY_NAME1, APP_POLICY_NAME1);
  }

  @Test
  public void testGetPolicies_AuthorizedOneAppAndOrg() {
    grantReadPermission(org.getId());
    grantReadPermission(app.getId());
    ApiPolicyListDTO policyListDTO = apiPolicyService.getPolicies();
    assertThat(policyListDTO).isNotNull();
    assertThat(policyListDTO.policies).hasSize(3);
    Set<String> policyNames = Sets.newHashSet(policyListDTO.policies.get(0).name, policyListDTO.policies.get(1).name,
        policyListDTO.policies.get(2).name);
    assertThat(policyNames).containsExactlyInAnyOrder(PARENT_ORG_POLICY_NAME1, ORG_POLICY_NAME1, APP_POLICY_NAME1);
  }

  @Test
  public void testGetPolicies_Unauthenticated() {
    ApiPolicyListDTO policyListDTO = apiPolicyService.getPolicies();
    assertThat(policyListDTO).isNotNull();
    assertThat(policyListDTO.policies).isEmpty();
  }

  @Test
  public void testGetPolicies_Unauthorized() {
    login();
    ApiPolicyListDTO policyListDTO = apiPolicyService.getPolicies();
    assertThat(policyListDTO).isNotNull();
    assertThat(policyListDTO.policies).isEmpty();
  }

  @Test
  public void testGetPolicies_AuthorizedRepositoryContainer() {
    grantReadPermission(repositoryManager.getParentOwnerId());
    ApiPolicyListDTO policyListDTO = apiPolicyService.getPolicies();
    assertThat(policyListDTO).isNotNull();
    assertThat(policyListDTO.policies).hasSize(5);
    Set<String> policyNames = Sets.newHashSet(policyListDTO.policies.get(0).name, policyListDTO.policies.get(1).name,
        policyListDTO.policies.get(2).name, policyListDTO.policies.get(3).name, policyListDTO.policies.get(4).name);
    assertThat(policyNames).containsExactlyInAnyOrder(PARENT_ORG_POLICY_NAME1, REPOSITORY_CONTAINER_POLICY_1,
        REPOSITORY_MANAGER_POLICY_1, REPOSITORY_POLICY_1, REPOSITORY_POLICY_2);
  }

  @Test
  public void testGetPolicies_AuthorizedRepositoryManager() {
    grantReadPermission(repositoryManager.getId());
    ApiPolicyListDTO policyListDTO = apiPolicyService.getPolicies();
    assertThat(policyListDTO).isNotNull();
    assertThat(policyListDTO.policies).hasSize(5);
    Set<String> policyNames = Sets.newHashSet(policyListDTO.policies.get(0).name, policyListDTO.policies.get(1).name,
        policyListDTO.policies.get(2).name, policyListDTO.policies.get(3).name, policyListDTO.policies.get(4).name);
    assertThat(policyNames).containsExactlyInAnyOrder(PARENT_ORG_POLICY_NAME1, REPOSITORY_CONTAINER_POLICY_1,
        REPOSITORY_MANAGER_POLICY_1, REPOSITORY_POLICY_1, REPOSITORY_POLICY_2);
  }

  @Test
  public void testGetPolicies_AuthorizedRepositories() {
    grantReadPermission(repository.getId());
    ApiPolicyListDTO policyListDTO = apiPolicyService.getPolicies();
    assertThat(policyListDTO).isNotNull();
    assertThat(policyListDTO.policies).hasSize(4);
    Set<String> policyNames = Sets.newHashSet(policyListDTO.policies.get(0).name, policyListDTO.policies.get(1).name,
        policyListDTO.policies.get(2).name, policyListDTO.policies.get(3).name);
    assertThat(policyNames).containsExactlyInAnyOrder(PARENT_ORG_POLICY_NAME1, REPOSITORY_CONTAINER_POLICY_1,
        REPOSITORY_MANAGER_POLICY_1, REPOSITORY_POLICY_1);
  }
}
