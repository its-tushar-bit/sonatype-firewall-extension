/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Set;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.api.v2.dto.ApiFirewallQuarantineSummaryDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiFirewallReleaseQuarantineSummaryDTO;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.AuthorizationException;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for resolvePermittedRepositoryIds() in FirewallPermissionGate (via ApiFirewallService integration).
 */
@Category(SlowTest.class)
public class ApiFirewallServiceResolvePermittedRepositoryIdsTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiFirewallService apiFirewallService;

  @Inject
  private FirewallPermissionGate firewallPermissionGate;

  @Test
  public void testResolvePermittedRepositoryIds_FullAccess_ReturnsNull() {
    // Grant READ on container - should return null (full access)
    grantPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID, Permission.READ);

    Set<String> result = firewallPermissionGate.resolvePermittedRepositoryIds();

    assertThat(result).isNull();
  }

  @Test
  public void testResolvePermittedRepositoryIds_ScopedAccess_ReturnsPermittedRepoIds() {
    // Create a proxy repository
    Repository proxyRepo = tempEntity.newRepository(repositoryManager, "testProxyRepo",
        RepositoryType.proxy, "docker");

    // Grant READ on the proxy repo only
    grantReadPermission(proxyRepo.getId());

    Set<String> result = firewallPermissionGate.resolvePermittedRepositoryIds();

    assertThat(result).isNotNull();
    assertThat(result).contains(proxyRepo.getId());
  }

  @Test
  public void testResolvePermittedRepositoryIds_NoAccess_ThrowsAuthorizationException() {
    // Login but grant no permissions
    login();

    assertThatThrownBy(() -> firewallPermissionGate.resolvePermittedRepositoryIds())
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining("No access to any proxy repository");
  }

  @Test
  public void testResolvePermittedRepositoryIds_MultipleRepos_ReturnsOnlyPermitted() {
    // Create two proxy repositories
    Repository proxyRepo1 = tempEntity.newRepository(repositoryManager, "testProxyRepo1",
        RepositoryType.proxy, "docker");
    Repository proxyRepo2 = tempEntity.newRepository(repositoryManager, "testProxyRepo2",
        RepositoryType.proxy, "docker");

    // Grant READ on only one proxy repo
    grantReadPermission(proxyRepo1.getId());

    Set<String> result = firewallPermissionGate.resolvePermittedRepositoryIds();

    assertThat(result).isNotNull();
    assertThat(result).containsExactly(proxyRepo1.getId());
  }

  @Test
  public void testGetQuarantineSummary_ProxyUserCanCall_ReturnsGlobalCounts() {
    // Create a proxy repository with some data
    Repository proxyRepo = tempEntity.newRepository(repositoryManager, "testProxyRepo",
        RepositoryType.proxy, "docker");
    tempEntity.newRepositoryComponent(proxyRepo, "hash1");
    tempEntity.newRepositoryComponent(proxyRepo.getId(), "path1", new java.util.Date(), null);

    // Grant READ on the proxy repo (scoped access)
    grantReadPermission(proxyRepo.getId());

    // Proxy user can call getQuarantineSummary and sees global counts (not scoped)
    ApiFirewallQuarantineSummaryDTO summary = apiFirewallService.getQuarantineSummary();

    assertThat(summary).isNotNull();
    // The counts are global, not filtered to the user's permitted repo
    assertThat(summary.repositoryCount).isGreaterThanOrEqualTo(1);
    assertThat(summary.totalComponentCount).isGreaterThanOrEqualTo(2);
    assertThat(summary.quarantinedComponentCount).isGreaterThanOrEqualTo(1);
  }

  @Test
  public void testGetReleaseQuarantineSummary_ProxyUserCanCall_ReturnsGlobalCounts() {
    // Create a proxy repository with some data
    Repository proxyRepo = tempEntity.newRepository(repositoryManager, "testProxyRepo",
        RepositoryType.proxy, "docker");
    java.util.Date now = new java.util.Date();
    tempEntity.newRepositoryComponent(proxyRepo.getId(), "/autoUnquarantined", now, now, true);

    // Grant READ on the proxy repo (scoped access)
    grantReadPermission(proxyRepo.getId());

    // Proxy user can call getReleaseQuarantineSummary and sees global counts (not scoped)
    ApiFirewallReleaseQuarantineSummaryDTO summary = apiFirewallService.getReleaseQuarantineSummary();

    assertThat(summary).isNotNull();
    // The counts are global, not filtered to the user's permitted repo
    assertThat(summary.autoReleaseQuarantineCountYTD).isGreaterThanOrEqualTo(1);
  }

  @Test
  public void testGetQuarantinedComponents_ScopedAccess_ReturnsOnlyPermittedRepoComponents() {
    // Create two proxy repositories with quarantined components
    Repository proxyRepo1 = tempEntity.newRepository(repositoryManager, "proxyRepo1",
        RepositoryType.proxy, "docker");
    Repository proxyRepo2 = tempEntity.newRepository(repositoryManager, "proxyRepo2",
        RepositoryType.proxy, "docker");

    java.util.Date quarantineDate = new java.util.Date();
    com.sonatype.insight.brain.model.repository.RepositoryComponent c1 =
        tempEntity.newRepositoryComponent(proxyRepo1.getId(), "/path1", quarantineDate, null);
    com.sonatype.insight.brain.model.repository.RepositoryComponent c2 =
        tempEntity.newRepositoryComponent(proxyRepo2.getId(), "/path2", quarantineDate, null);

    // Create policy violations for the components
    com.sonatype.insight.brain.model.policy.Policy policy = tempEntity.newPolicy();
    com.sonatype.insight.brain.api.v2.service.PolicyViolationTestHelper
        .createPolicyViolationFail(policy, c1, tempEntity);
    com.sonatype.insight.brain.api.v2.service.PolicyViolationTestHelper
        .createPolicyViolationFail(policy, c2, tempEntity);

    // Grant READ on proxyRepo1 only (scoped access)
    grantReadPermission(proxyRepo1.getId());

    // Call getQuarantinedComponents
    com.sonatype.insight.brain.dataaccess.repository.FirewallRepositoryComponentFilter filter =
        new com.sonatype.insight.brain.dataaccess.repository.FirewallRepositoryComponentFilter(
            1, 10,
            com.sonatype.insight.brain.dataaccess.repository.FirewallRepositoryComponentFilter.FirewallComponentFilterState.QUARANTINE,
            com.sonatype.insight.brain.dataaccess.repository.FirewallSortableField.QUARANTINE_TIME,
            true, java.util.Collections.emptyList());

    com.sonatype.insight.brain.api.v2.dto.ApiPageResult<com.sonatype.insight.brain.api.v2.dto.ApiFirewallQuarantinedComponentDto> result =
        apiFirewallService.getQuarantinedComponents(filter);

    // Verify filter.permittedRepositoryIds was set
    assertThat(filter.permittedRepositoryIds).isNotNull();
    assertThat(filter.permittedRepositoryIds).containsExactly(proxyRepo1.getId());

    // Verify result contains only components from permitted repo
    assertThat(result.getTotal()).isEqualTo(1);
    assertThat(result.getResults()).hasSize(1);
    assertThat(result.getResults().get(0).repositoryId).isEqualTo(proxyRepo1.getId());
  }

  @Test
  public void testGetQuarantinedComponents_FullAccess_ReturnsAllComponents() {
    // Create two proxy repositories with quarantined components
    Repository proxyRepo1 = tempEntity.newRepository(repositoryManager, "proxyRepo1",
        RepositoryType.proxy, "docker");
    Repository proxyRepo2 = tempEntity.newRepository(repositoryManager, "proxyRepo2",
        RepositoryType.proxy, "docker");

    java.util.Date quarantineDate = new java.util.Date();
    com.sonatype.insight.brain.model.repository.RepositoryComponent c1 =
        tempEntity.newRepositoryComponent(proxyRepo1.getId(), "/path1", quarantineDate, null);
    com.sonatype.insight.brain.model.repository.RepositoryComponent c2 =
        tempEntity.newRepositoryComponent(proxyRepo2.getId(), "/path2", quarantineDate, null);

    // Create policy violations for the components
    com.sonatype.insight.brain.model.policy.Policy policy = tempEntity.newPolicy();
    com.sonatype.insight.brain.api.v2.service.PolicyViolationTestHelper
        .createPolicyViolationFail(policy, c1, tempEntity);
    com.sonatype.insight.brain.api.v2.service.PolicyViolationTestHelper
        .createPolicyViolationFail(policy, c2, tempEntity);

    // Grant container READ (full access)
    grantPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID, Permission.READ);

    // Call getQuarantinedComponents
    com.sonatype.insight.brain.dataaccess.repository.FirewallRepositoryComponentFilter filter =
        new com.sonatype.insight.brain.dataaccess.repository.FirewallRepositoryComponentFilter(
            1, 10,
            com.sonatype.insight.brain.dataaccess.repository.FirewallRepositoryComponentFilter.FirewallComponentFilterState.QUARANTINE,
            com.sonatype.insight.brain.dataaccess.repository.FirewallSortableField.QUARANTINE_TIME,
            true, java.util.Collections.emptyList());

    com.sonatype.insight.brain.api.v2.dto.ApiPageResult<com.sonatype.insight.brain.api.v2.dto.ApiFirewallQuarantinedComponentDto> result =
        apiFirewallService.getQuarantinedComponents(filter);

    // Verify filter.permittedRepositoryIds is null (indicating full access)
    assertThat(filter.permittedRepositoryIds).isNull();

    // Verify result contains all components
    assertThat(result.getTotal()).isEqualTo(2);
    assertThat(result.getResults()).hasSize(2);
  }

  @Test
  public void testGetComponents_ScopedAccess_ReturnsOnlyPermittedRepoComponents() {
    // Create two proxy repositories
    Repository proxyRepo1 = tempEntity.newRepository(repositoryManager, "proxyRepo1",
        RepositoryType.proxy, "docker");
    Repository proxyRepo2 = tempEntity.newRepository(repositoryManager, "proxyRepo2",
        RepositoryType.proxy, "docker");

    java.util.Date june1st = java.util.Date.from(
        java.time.LocalDateTime.of(2020, 6, 1, 1, 0).toInstant(java.time.ZoneOffset.UTC));
    java.util.Date june2nd = java.util.Date.from(
        java.time.LocalDateTime.of(2020, 6, 2, 1, 0).toInstant(java.time.ZoneOffset.UTC));

    // Create auto-unquarantined components (for UNQUARANTINE_AUTO filter)
    tempEntity.newRepositoryComponent(proxyRepo1.getId(), "/unquarantined1", june1st, june2nd, true);
    tempEntity.newRepositoryComponent(proxyRepo2.getId(), "/unquarantined2", june2nd, null, true);

    // Grant READ on proxyRepo1 only (scoped access)
    grantReadPermission(proxyRepo1.getId());

    // Call getComponents with UNQUARANTINE_AUTO state
    com.sonatype.insight.brain.dataaccess.repository.FirewallRepositoryComponentFilter filter =
        new com.sonatype.insight.brain.dataaccess.repository.FirewallRepositoryComponentFilter(
            1, 10,
            com.sonatype.insight.brain.dataaccess.repository.FirewallRepositoryComponentFilter.FirewallComponentFilterState.UNQUARANTINE_AUTO,
            com.sonatype.insight.brain.dataaccess.repository.FirewallSortableField.RELEASE_QUARANTINE_TIME,
            true, java.util.Collections.emptyList());

    com.sonatype.insight.brain.api.v2.dto.ApiPageResult<com.sonatype.insight.brain.api.v2.dto.ApiFirewallComponentDTO> result =
        apiFirewallService.getComponents(filter);

    // Verify filter.permittedRepositoryIds was set
    assertThat(filter.permittedRepositoryIds).isNotNull();
    assertThat(filter.permittedRepositoryIds).containsExactly(proxyRepo1.getId());

    // Verify result contains only components from permitted repo
    assertThat(result.getTotal()).isEqualTo(1);
    assertThat(result.getResults()).hasSize(1);
    assertThat(result.getResults().get(0).repositoryId).isEqualTo(proxyRepo1.getId());
  }

  @Test
  public void testGetComponents_FullAccess_ReturnsAllComponents() {
    // Create two proxy repositories
    Repository proxyRepo1 = tempEntity.newRepository(repositoryManager, "proxyRepo1",
        RepositoryType.proxy, "docker");
    Repository proxyRepo2 = tempEntity.newRepository(repositoryManager, "proxyRepo2",
        RepositoryType.proxy, "docker");

    java.util.Date june1st = java.util.Date.from(
        java.time.LocalDateTime.of(2020, 6, 1, 1, 0).toInstant(java.time.ZoneOffset.UTC));
    java.util.Date june2nd = java.util.Date.from(
        java.time.LocalDateTime.of(2020, 6, 2, 1, 0).toInstant(java.time.ZoneOffset.UTC));

    // Create auto-unquarantined components (for UNQUARANTINE_AUTO filter)
    tempEntity.newRepositoryComponent(proxyRepo1.getId(), "/unquarantined1", june1st, june2nd, true);
    tempEntity.newRepositoryComponent(proxyRepo2.getId(), "/unquarantined2", june2nd, null, true);

    // Grant container READ (full access)
    grantPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID, Permission.READ);

    // Call getComponents with UNQUARANTINE_AUTO state
    com.sonatype.insight.brain.dataaccess.repository.FirewallRepositoryComponentFilter filter =
        new com.sonatype.insight.brain.dataaccess.repository.FirewallRepositoryComponentFilter(
            1, 10,
            com.sonatype.insight.brain.dataaccess.repository.FirewallRepositoryComponentFilter.FirewallComponentFilterState.UNQUARANTINE_AUTO,
            com.sonatype.insight.brain.dataaccess.repository.FirewallSortableField.RELEASE_QUARANTINE_TIME,
            true, java.util.Collections.emptyList());

    com.sonatype.insight.brain.api.v2.dto.ApiPageResult<com.sonatype.insight.brain.api.v2.dto.ApiFirewallComponentDTO> result =
        apiFirewallService.getComponents(filter);

    // Verify filter.permittedRepositoryIds is null (indicating full access)
    assertThat(filter.permittedRepositoryIds).isNull();

    // Verify result contains all components
    assertThat(result.getTotal()).isEqualTo(1);
    assertThat(result.getResults()).hasSize(1);
  }
}
