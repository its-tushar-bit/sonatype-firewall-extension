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
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.dataaccess.repository.FirewallRepositoryComponentFilter.FirewallComponentFilterState;
import com.sonatype.insight.brain.model.repository.ManagerType;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class ApiFirewallServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ApiFirewallService apiFirewallService;

  @Inject
  private RepositoryManagerDAO repositoryManagerDAO;

  @Test
  public void testGetReleaseQuarantineSummary_Authorized() {
    grantPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID, Permission.READ);

    ApiFirewallReleaseQuarantineSummaryDTO dto = apiFirewallService.getReleaseQuarantineSummary();

    assertThat(dto.autoReleaseQuarantineCountMTD).isZero();
  }

  @Test
  public void testGetReleaseQuarantineSummary_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> apiFirewallService.getReleaseQuarantineSummary());
  }

  @Test
  public void testGetReleaseQuarantineSummary_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> apiFirewallService.getReleaseQuarantineSummary());
  }

  @Test
  public void testGetReleaseQuarantineConfig_Authorized() {
    grantPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID, Permission.READ);

    List<ApiFirewallReleaseQuarantineConfigDTO> dtos = apiFirewallService.getReleaseQuarantineConfig();

    assertThat(dtos).isNotNull().isNotEmpty();
  }

  @Test
  public void testGetReleaseQuarantineConfig_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> apiFirewallService.getReleaseQuarantineConfig());
  }

  @Test
  public void testGetReleaseQuarantineConfig_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> apiFirewallService.getReleaseQuarantineConfig());
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

  @Test
  public void testSetReleaseQuarantineConfig_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> apiFirewallService.setReleaseQuarantineConfig(null));
  }

  @Test
  public void testSetReleaseQuarantineConfig_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> apiFirewallService.setReleaseQuarantineConfig(null));
  }

  @Test
  public void testGetQuarantineSummary_Authorized() {
    grantPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID, Permission.READ);

    assertThat(apiFirewallService.getQuarantineSummary()).isNotNull();
  }

  @Test
  public void testGetQuarantineSummary_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> apiFirewallService.getQuarantineSummary());
  }

  @Test
  public void testGetQuarantineSummary_Unauthorized() {
    login();

    assertThrows(UnauthorizedException.class, () -> apiFirewallService.getQuarantineSummary());
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

  @Test
  public void testGetComponents_Unauthorized() {
    login();

    final FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 2, FirewallComponentFilterState.UNQUARANTINE_AUTO, null, true,
            Collections.emptyList());
    assertThrows(UnauthorizedException.class, () -> apiFirewallService.getComponents(filter));
  }

  @Test
  public void testGetComponents_Unauthenticated() {
    final FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 2, FirewallComponentFilterState.UNQUARANTINE_AUTO, null, true,
            Collections.emptyList());
    assertThrows(UnauthenticatedException.class, () -> apiFirewallService.getComponents(filter));
  }

  @Test
  public void testSetQuarantinedComponentViewAnonymousAccess_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiFirewallService.setQuarantinedComponentViewAnonymousAccess(true));
  }

  @Test
  public void testSetQuarantinedComponentViewAnonymousAccess_Authorized() {
    grantPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID, Permission.WRITE);
    apiFirewallService.setQuarantinedComponentViewAnonymousAccess(true);
  }

  @Test
  public void testSetQuarantinedComponentViewAnonymousAccess_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiFirewallService.setQuarantinedComponentViewAnonymousAccess(true));
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

  @Test
  public void testGetConfiguredRepositories_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiFirewallService.getConfiguredRepositories(repository.getRepositoryManagerId(), 0L));
  }

  @Test
  public void testGetConfiguredRepositories_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiFirewallService.getConfiguredRepositories(repository.getRepositoryManagerId(), 0L));
  }

  @Test
  public void testConfigureRepositories_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, this::configureRepositories);
  }

  @Test
  public void testConfigureRepositories_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, this::configureRepositories);
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

  @Test
  public void testGetRepositoryManager_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> apiFirewallService.getRepositoryManager(repositoryManager.getId()));
  }

  @Test
  public void testGetRepositoryManager_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiFirewallService.getRepositoryManager(repositoryManager.getId()));
  }

  @Test
  public void testDeleteRepositoryManager_Authorized() {
    grantWritePermission(repositoryManager.getId());
    apiFirewallService.deleteRepositoryManager(repositoryManager.getId());
  }

  @Test
  public void testDeleteRepositoryManager_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiFirewallService.deleteRepositoryManager(repositoryManager.getId()));
  }

  @Test
  public void testDeleteRepositoryManager_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiFirewallService.deleteRepositoryManager(repositoryManager.getId()));
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

  @Test
  public void testAddRepositoryManager_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiFirewallService.addRepositoryManager(new ApiRepositoryManagerDTO()));
  }

  @Test
  public void testAddRepositoryManager_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiFirewallService.addRepositoryManager(new ApiRepositoryManagerDTO()));
  }

  @Test
  public void testAddVirtualRepositoryManager_Authorized() {
    grantWritePermission(RepositoryContainer.SINGLETON.getId());

    // productName / productVersion / instanceId / id are rejected by
    // validateVirtualRepositoryManagerRequest for virtual managers — server-owned so a client
    // supplying them is a request shape error. Only name is required.
    ApiRepositoryManagerDTO apiRepositoryManagerDTO = new ApiRepositoryManagerDTO();
    apiRepositoryManagerDTO.name = "testVirtualName";

    apiFirewallService.addVirtualRepositoryManager(apiRepositoryManagerDTO);
  }

  @Test
  public void testAddVirtualRepositoryManager_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiFirewallService.addVirtualRepositoryManager(new ApiRepositoryManagerDTO()));
  }

  @Test
  public void testAddVirtualRepositoryManager_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiFirewallService.addVirtualRepositoryManager(new ApiRepositoryManagerDTO()));
  }

  @Test
  public void testAddRepository_Authorized() {
    setBaseUrl("http://localhost:8070/");
    repositoryManager.setManagerType(ManagerType.VIRTUAL);
    repositoryManagerDAO.update(repositoryManager);
    grantWritePermission(repositoryManager.getId());

    ApiRepositoryDTO apiRepositoryDTO = new ApiRepositoryDTO();
    apiRepositoryDTO.publicId = "test-repo";
    apiRepositoryDTO.format = "maven2";
    apiRepositoryDTO.upstreamUrl = "https://repo1.maven.org/maven2/";

    apiFirewallService.addRepository(repositoryManager.getId(), apiRepositoryDTO);
  }

  @Test
  public void testAddRepository_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiFirewallService.addRepository(repositoryManager.getId(), new ApiRepositoryDTO()));
  }

  @Test
  public void testAddRepository_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiFirewallService.addRepository(repositoryManager.getId(), new ApiRepositoryDTO()));
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

  @Test
  public void testGetRepositoryContainer_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> apiFirewallService.getRepositoryContainer());
  }

  @Test
  public void testGetRepositoryContainer_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> apiFirewallService.getRepositoryContainer());
  }

  @Test
  public void testCheckEvaluateComponentPermission_Authorized() {
    grantPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID, Permission.EVALUATE_COMPONENT);
    apiFirewallService.checkEvaluateComponentPermission(RepositoryContainer.SINGLETON);
  }

  @Test
  public void testCheckEvaluateComponentPermission_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiFirewallService.checkEvaluateComponentPermission(RepositoryContainer.SINGLETON));
  }

  @Test
  public void testCheckEvaluateComponentPermission_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiFirewallService.checkEvaluateComponentPermission(RepositoryContainer.SINGLETON));
  }
}
