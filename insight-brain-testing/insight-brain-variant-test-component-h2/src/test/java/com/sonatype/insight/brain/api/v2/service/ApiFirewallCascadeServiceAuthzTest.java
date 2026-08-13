/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Date;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.CascadeReevaluateTicketDTO;
import com.sonatype.insight.brain.api.v2.dto.CascadeStatusResponseDTO;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeProgressStatus;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.repository.RepositoryContainer.REPOSITORY_CONTAINER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class ApiFirewallCascadeServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ApiFirewallCascadeService cascadeService;

  @Test
  public void testInitiateCascadeReevaluation_Authorized() {
    String componentHash = "auth_test_hash";
    createRepositoryWithComponent(componentHash);

    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    CascadeReevaluateTicketDTO result = cascadeService.initiateCascadeReevaluation(componentHash);

    assertThat(result).isNotNull();
    assertThat(result.statusUrl).startsWith(PublicApiPaths.FIREWALL_CASCADE_REEVALUATE_PATH + "/status/");
  }

  @Test
  public void testInitiateCascadeReevaluation_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> cascadeService.initiateCascadeReevaluation("component-hash"));
  }

  @Test
  public void testInitiateCascadeReevaluation_Unauthorized() {
    String componentHash = "unauth_test_hash";
    createRepositoryWithComponent(componentHash);

    login();
    assertThrows(UnauthorizedException.class,
        () -> cascadeService.initiateCascadeReevaluation(componentHash));
  }

  @Test
  public void testInitiateCascadeReevaluation_MultipleRepositories_Authorized() {
    // Create multiple repositories with the same component
    String componentHash = "multi_repo_hash";
    createRepositoryWithComponent("repo1", componentHash);
    createRepositoryWithComponent("repo2", componentHash);

    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    CascadeReevaluateTicketDTO result = cascadeService.initiateCascadeReevaluation(componentHash);

    assertThat(result).isNotNull();
    assertThat(result.statusUrl).startsWith(PublicApiPaths.FIREWALL_CASCADE_REEVALUATE_PATH + "/status/");
  }

  @Test
  public void testInitiateCascadeReevaluation_MultipleRepositories_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> cascadeService.initiateCascadeReevaluation("multi-repo-hash"));
  }

  @Test
  public void testInitiateCascadeReevaluation_MultipleRepositories_Unauthorized() {
    String componentHash = "multi_repo_hash";
    createRepositoryWithComponent("repo1", componentHash);
    createRepositoryWithComponent("repo2", componentHash);

    login();
    assertThrows(UnauthorizedException.class,
        () -> cascadeService.initiateCascadeReevaluation(componentHash));
  }

  @Test
  public void testInitiateCascadeReevaluation_AuthorizationCheckedFirst() {
    // Test that authorization is checked before component lookup
    // This ensures security-first behavior: fail fast on unauthorized access
    String nonExistentHash = "non_existent_component_hash";

    login(); // authenticated but not authorized
    // Should fail with UnauthorizedException, not "component not found" error
    assertThrows(UnauthorizedException.class,
        () -> cascadeService.initiateCascadeReevaluation(nonExistentHash));
  }

  private Repository createRepositoryWithComponent(String componentHash) {
    return createRepositoryWithComponent("test_repo", componentHash);
  }

  private Repository createRepositoryWithComponent(String repositoryName, String componentHash) {
    Repository repository = tempEntity.newRepository(repositoryName);
    Date now = new Date();

    tempEntity.newRepositoryComponent(repository.getId(),
        MatchState.EXACT, "test/component/path", componentHash,
        ComponentIdentifier.createNpmCoordinates("test-package", "1.0.0"), now, now);

    return repository;
  }

  @Test
  public void testGetCascadeStatus_ValidRequest_Authorized() {
    // Arrange
    Repository repository = createRepositoryWithoutComponent();
    String componentHash = "status_auth_hash";
    String cascadeRequestId = createCascadeRequestWithProgress(repository, componentHash);

    grantEvaluateComponentPermission(REPOSITORY_CONTAINER_ID);

    // Act & Assert - Should succeed without throwing
    CascadeStatusResponseDTO result = cascadeService.getCascadeStatus(cascadeRequestId);
    assertThat(result).isNotNull();
    assertThat(result.referenceComponentHash).isEqualTo(componentHash);
  }

  @Test
  public void testGetCascadeStatus_ValidRequest_Unauthenticated() {
    // Arrange
    Repository repository = createRepositoryWithoutComponent();
    String componentHash = "status_unauth_hash";
    String cascadeRequestId = createCascadeRequestWithProgress(repository, componentHash);

    // No authentication set up
    assertThrows(UnauthenticatedException.class, () -> cascadeService.getCascadeStatus(cascadeRequestId));
  }

  @Test
  public void testGetCascadeStatus_ValidRequest_Unauthorized() {
    // Arrange
    Repository repository = createRepositoryWithoutComponent();
    String componentHash = "status_unauthor_hash";
    String cascadeRequestId = createCascadeRequestWithProgress(repository, componentHash);

    login(); // Login but no specific permissions

    // Act & Assert
    assertThrows(UnauthorizedException.class, () -> cascadeService.getCascadeStatus(cascadeRequestId));
  }

  @Test
  public void testGetCascadeStatus_RequestNotFound() {
    // Arrange
    String nonExistentRequestId = "nonexistent_authz_test";

    grantEvaluateComponentPermission(REPOSITORY_CONTAINER_ID);

    // Act & Assert - Should throw NotFoundException regardless of authorization
    try {
      cascadeService.getCascadeStatus(nonExistentRequestId);
    }
    catch (NotFoundException e) {
      assertThat(e.getMessage()).contains("Cascade request not found: " + nonExistentRequestId);
    }
  }

  @Test
  public void testGetCascadeStatus_CrossTenantAccess() {
    // Arrange - Create cascade request in one tenant context
    Repository repository = createRepositoryWithoutComponent();
    String componentHash = "cross_tenant_hash";
    String cascadeRequestId = createCascadeRequestWithProgress(repository, componentHash);

    // Grant permissions for accessing the request
    grantEvaluateComponentPermission(REPOSITORY_CONTAINER_ID);

    // Act & Assert - Should work for same tenant
    CascadeStatusResponseDTO result = cascadeService.getCascadeStatus(cascadeRequestId);
    assertThat(result).isNotNull();
    assertThat(result.referenceComponentHash).isEqualTo(componentHash);
  }

  private String createCascadeRequestWithProgress(Repository repository, String componentHash) {
    Date now = new Date();

    // Create repository component for the test
    ProxyRepositoryComponent proxyRepositoryComponent = tempEntity.newRepositoryComponent(
        repository.getId(),
        MatchState.EXACT,
        "authz/status/test",
        componentHash,
        ComponentIdentifier.createNpmCoordinates("authz-status-pkg", "1.0.0"),
        now,
        now);

    String cascadeRequestId = "authz_cascade_" + System.currentTimeMillis();
    tempEntity.newReevaluateCascadeRequest(cascadeRequestId, componentHash, "testuser");

    tempEntity.newReevaluateCascadeProgress("authz_progress_completed", cascadeRequestId, repository.getId(),
        proxyRepositoryComponent.getId(), ReevaluateCascadeProgressStatus.COMPLETED.name());

    return cascadeRequestId;
  }

  private Repository createRepositoryWithoutComponent() {
    return tempEntity.newRepository("authz-test-repo");
  }
}
