/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Collections;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryPathResponseDTO;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ApiRepositoryPathServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiRepositoryPathService apiRepositoryPathService;

  @Test
  public void testGetQuarantinedByPathnames_Authorized() {
    grantGlobalPermission(Permission.READ);
    Repository repository = tempEntity.newRepository("repositoryManager1", "repo1", "npm");
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "comp1/-/comp1-1.tgz", "hash1",
            ComponentIdentifier.createNpmCoordinates("comp1", "1"), true);

    ApiRepositoryPathResponseDTO dto = apiRepositoryPathService.getQuarantinedByPathnames(
        "repositoryManager1", repository.getPublicId(), Collections.singletonList("comp1/-/comp1-1.tgz"));
    assertThat(dto).isNotNull();
  }

  @Test
  public void testGetQuarantinedByPathnames_Unauthorized() {
    login();
    Repository repository = tempEntity.newRepository("repositoryManager1", "repo1", "npm");
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "comp1/-/comp1-1.tgz", "hash1",
        ComponentIdentifier.createNpmCoordinates("comp1", "1"), true);
    assertThatExceptionOfType(UnauthorizedException.class).isThrownBy(
        () -> apiRepositoryPathService.getQuarantinedByPathnames("repositoryManager1",
            repository.getPublicId(), Collections.singletonList("comp1/-/comp1-1.tgz")));
  }

  @Test
  public void testGetQuarantinedByPathnames_Unauthenticated() {
    Repository repository = tempEntity.newRepository("repositoryManager1", "repo1", "npm");
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "comp1/-/comp1-1.tgz", "hash1",
        ComponentIdentifier.createNpmCoordinates("comp1", "1"), true);
    assertThatExceptionOfType(UnauthenticatedException.class).isThrownBy(
        () -> apiRepositoryPathService.getQuarantinedByPathnames("repositoryManager1",
            repository.getPublicId(), Collections.singletonList("comp1/-/comp1-1.tgz")));
  }
}
