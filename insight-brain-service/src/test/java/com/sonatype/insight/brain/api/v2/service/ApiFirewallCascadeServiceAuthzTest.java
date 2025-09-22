/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Date;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.CascadeReevaluateTicketDTO;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.component.MatchState;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiFirewallCascadeServiceAuthzTest
    extends AbstractServiceAuthzTest
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
    assertThat(result.statusUrl).startsWith(PublicApiPaths.MALWARE_CASCADE_REEVALUATE_PATH + "/status/");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testInitiateCascadeReevaluation_Unauthenticated() {
    cascadeService.initiateCascadeReevaluation("component-hash");
  }

  @Test(expected = UnauthorizedException.class)
  public void testInitiateCascadeReevaluation_Unauthorized() {
    String componentHash = "unauth_test_hash";
    createRepositoryWithComponent(componentHash);

    login();
    cascadeService.initiateCascadeReevaluation(componentHash);
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
    assertThat(result.statusUrl).startsWith(PublicApiPaths.MALWARE_CASCADE_REEVALUATE_PATH + "/status/");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testInitiateCascadeReevaluation_MultipleRepositories_Unauthenticated() {
    cascadeService.initiateCascadeReevaluation("multi-repo-hash");
  }

  @Test(expected = UnauthorizedException.class)
  public void testInitiateCascadeReevaluation_MultipleRepositories_Unauthorized() {
    String componentHash = "multi_repo_hash";
    createRepositoryWithComponent("repo1", componentHash);
    createRepositoryWithComponent("repo2", componentHash);

    login();
    cascadeService.initiateCascadeReevaluation(componentHash);
  }

  @Test(expected = UnauthorizedException.class)
  public void testInitiateCascadeReevaluation_AuthorizationCheckedFirst() {
    // Test that authorization is checked before component lookup
    // This ensures security-first behavior: fail fast on unauthorized access
    String nonExistentHash = "non_existent_component_hash";

    login(); // authenticated but not authorized
    // Should fail with UnauthorizedException, not "component not found" error
    cascadeService.initiateCascadeReevaluation(nonExistentHash);
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
}
