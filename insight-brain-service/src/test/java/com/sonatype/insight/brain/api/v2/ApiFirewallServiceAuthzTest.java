/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.api.v2.dto.ApiFirewallComponentDTO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.api.v2.dto.ApiFirewallReleaseQuarantineConfigDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiFirewallReleaseQuarantineSummaryDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPageResult;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryListDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryManagerDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryManagerListDTO;
import com.sonatype.insight.brain.dataaccess.repository.FirewallRepositoryComponentFilter;
import com.sonatype.insight.brain.dataaccess.repository.FirewallRepositoryComponentFilter.FirewallComponentFilterState;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class ApiFirewallServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiFirewallService apiFirewallService;

  @Test
  public void testGetReleaseQuarantineSummary_Authorized() {
    grantPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID, Permission.READ);

    ApiFirewallReleaseQuarantineSummaryDTO dto = apiFirewallService.getReleaseQuarantineSummary();

    assertThat(dto.autoReleaseQuarantineCountMTD).isZero();
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetReleaseQuarantineSummary_Unauthorized() {
    login();
    apiFirewallService.getReleaseQuarantineSummary();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetReleaseQuarantineSummary_Unauthenticated() {
    apiFirewallService.getReleaseQuarantineSummary();
  }

  @Test
  public void testGetReleaseQuarantineConfig_Authorized() {
    grantPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID, Permission.READ);

    List<ApiFirewallReleaseQuarantineConfigDTO> dtos = apiFirewallService.getReleaseQuarantineConfig();

    assertThat(dtos).isNotNull().isNotEmpty();
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetReleaseQuarantineConfig_Unauthorized() {
    login();
    apiFirewallService.getReleaseQuarantineConfig();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetReleaseQuarantineConfig_Unauthenticated() {
    apiFirewallService.getReleaseQuarantineConfig();
  }

  @Test
  public void testGetReleaseQuarantineConfig_ScopedUser() {
    Repository proxyRepo = tempEntity.newRepository(repositoryManager, "scopedRepo", RepositoryType.proxy, "docker");
    grantPermission(proxyRepo.getId(), Permission.READ);

    List<ApiFirewallReleaseQuarantineConfigDTO> dtos = apiFirewallService.getReleaseQuarantineConfig();

    assertThat(dtos).isNotNull().isNotEmpty();
  }

  @Test
  public void testSetReleaseQuarantineConfig_Authorized() {
    grantPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID, Permission.WRITE);

    List<ApiFirewallReleaseQuarantineConfigDTO> dtos = apiFirewallService.setReleaseQuarantineConfig(new ArrayList<>());

    assertThat(dtos).isNotNull().isNotEmpty();
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetReleaseQuarantineConfig_Unauthorized() {
    login();
    apiFirewallService.setReleaseQuarantineConfig(null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetReleaseQuarantineConfig_Unauthenticated() {
    apiFirewallService.setReleaseQuarantineConfig(null);
  }

  @Test
  public void testGetQuarantineSummary_Authorized() {
    grantPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID, Permission.READ);

    assertThat(apiFirewallService.getQuarantineSummary()).isNotNull();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetQuarantineSummary_Unauthenticated() {
    apiFirewallService.getQuarantineSummary();
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetQuarantineSummary_Unauthorized() {
    login();

    apiFirewallService.getQuarantineSummary();
  }

  @Test
  public void testGetComponents_Authorized() {
    grantPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID, Permission.READ);

    final FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 2, FirewallComponentFilterState.UNQUARANTINE_AUTO, null, true,
            Collections.emptyList());
    final ApiPageResult<ApiFirewallComponentDTO> dto = apiFirewallService.getComponents(filter);

    assertThat(dto.getTotal()).isZero();
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetComponents_Unauthorized() {
    login();

    final FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 2, FirewallComponentFilterState.UNQUARANTINE_AUTO, null, true,
            Collections.emptyList());
    apiFirewallService.getComponents(filter);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetComponents_Unauthenticated() {
    final FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 2, FirewallComponentFilterState.UNQUARANTINE_AUTO, null, true,
            Collections.emptyList());
    apiFirewallService.getComponents(filter);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetQuarantinedComponentViewAnonymousAccess_Unauthenticated() {
    apiFirewallService.setQuarantinedComponentViewAnonymousAccess(true);
  }

  @Test
  public void testSetQuarantinedComponentViewAnonymousAccess_Authorized() {
    grantPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID, Permission.WRITE);
    apiFirewallService.setQuarantinedComponentViewAnonymousAccess(true);
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetQuarantinedComponentViewAnonymousAccess_Unauthorized() {
    login();
    apiFirewallService.setQuarantinedComponentViewAnonymousAccess(true);
  }

  @Test
  public void testGetRepositoryManagers_Authorized() {
    RepositoryManager repositoryManager1 = tempEntity.newRepositoryManager();

    grantReadPermission(repositoryManager.getId());
    ApiRepositoryManagerListDTO result = apiFirewallService.getRepositoryManagers();
    assertThat(result.repositoryManagers).extracting(rm -> rm.id).containsExactlyInAnyOrder(repositoryManager.getId());

    grantReadPermission(repositoryManager1.getId());
    result = apiFirewallService.getRepositoryManagers();
    assertThat(result.repositoryManagers).extracting(rm -> rm.id)
        .containsExactlyInAnyOrder(repositoryManager.getId(),
            repositoryManager1.getId());
  }

  @Test
  public void testGetConfiguredRepositories_Authorized() {
    grantReadPermission(repository.getRepositoryManagerId());
    apiFirewallService
        .getConfiguredRepositories(repository.getRepositoryManagerId(), 0L);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetConfiguredRepositories_Unauthenticated() {
    apiFirewallService.getConfiguredRepositories(repository.getRepositoryManagerId(), 0L);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetConfiguredRepositories_Unauthorized() {
    login();
    apiFirewallService.getConfiguredRepositories(repository.getRepositoryManagerId(), 0L);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testConfigureRepositories_Unauthenticated() {
    configureRepositories();
  }

  @Test(expected = UnauthorizedException.class)
  public void testConfigureRepositories_Unauthorized() {
    login();
    configureRepositories();
  }

  @Test
  public void testConfigureRepositories_Authorized() {
    grantWritePermission(repository.getRepositoryManagerId());
    configureRepositories();
  }

  @Test
  public void testGetRepositoryManager_Authorized() {
    grantReadPermission(repositoryManager.getId());
    apiFirewallService.getRepositoryManager(repositoryManager.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetRepositoryManager_Unauthorized() {
    login();
    apiFirewallService.getRepositoryManager(repositoryManager.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetRepositoryManager_Unauthenticated() {
    apiFirewallService.getRepositoryManager(repositoryManager.getId());
  }

  @Test
  public void testDeleteRepositoryManager_Authorized() {
    grantWritePermission(repositoryManager.getId());
    apiFirewallService.deleteRepositoryManager(repositoryManager.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteRepositoryManager_Unauthorized() {
    login();
    apiFirewallService.deleteRepositoryManager(repositoryManager.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteRepositoryManager_Unauthenticated() {
    apiFirewallService.deleteRepositoryManager(repositoryManager.getId());
  }

  @Test
  public void testAddRepositoryManager_Authorized() {
    grantWritePermission(RepositoryContainer.SINGLETON.getId());

    ApiRepositoryManagerDTO apiRepositoryManagerDTO = new ApiRepositoryManagerDTO();
    apiRepositoryManagerDTO.instanceId = "testInstanceId";
    apiRepositoryManagerDTO.name = "testName";
    apiRepositoryManagerDTO.productName = "testProductName";
    apiRepositoryManagerDTO.productVersion = "testProductVersion";

    apiFirewallService.addRepositoryManager(apiRepositoryManagerDTO);
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddRepositoryManager_Unauthorized() {
    login();
    apiFirewallService.addRepositoryManager(new ApiRepositoryManagerDTO());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddRepositoryManager_Unauthenticated() {
    apiFirewallService.addRepositoryManager(new ApiRepositoryManagerDTO());
  }

  private void configureRepositories() {
    ApiRepositoryListDTO apiRepositoryListDTO = new ApiRepositoryListDTO();
    apiRepositoryListDTO.repositories = Collections.singletonList(ApiRepositoryDTO.fromRepository(repository));
    apiFirewallService.configureRepositories(repository.getRepositoryManagerId(), apiRepositoryListDTO);
  }

  @Test
  public void testGetRepositoryContainer_Authorized() {
    grantReadPermission(RepositoryContainer.SINGLETON.getId());
    apiFirewallService.getRepositoryContainer();
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetRepositoryContainer_Unauthorized() {
    login();
    apiFirewallService.getRepositoryContainer();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetRepositoryContainer_Unauthenticated() {
    apiFirewallService.getRepositoryContainer();
  }

  @Test
  public void testCheckEvaluateComponentPermission_Authorized() {
    grantPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID, Permission.EVALUATE_COMPONENT);
    apiFirewallService.checkEvaluateComponentPermission(RepositoryContainer.SINGLETON);
  }

  @Test(expected = UnauthorizedException.class)
  public void testCheckEvaluateComponentPermission_Unauthorized() {
    login();
    apiFirewallService.checkEvaluateComponentPermission(RepositoryContainer.SINGLETON);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testCheckEvaluateComponentPermission_Unauthenticated() {
    apiFirewallService.checkEvaluateComponentPermission(RepositoryContainer.SINGLETON);
  }
}
