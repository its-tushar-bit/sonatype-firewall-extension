/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Arrays;
import java.util.Collections;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryPathResponseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryPathResponseDTO.ApiRepositoryComponentPath;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ApiRepositoryPathServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiRepositoryPathService apiRepositoryPathService;

  @Test
  public void testGetQuarantinedByPathnames_Npm() {
    Repository repository = tempEntity.newRepository("repositoryManager1", "repo1", "npm");
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "comp1/-/comp1-1.tgz", "hash1-1",
        ComponentIdentifier.createNpmCoordinates("comp1", "1"), true);
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "comp1/-/comp1-2.tgz", "hash1-2",
        ComponentIdentifier.createNpmCoordinates("comp1", "2"), false);
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "comp1/-/comp1-3.tgz", "hash1-3",
        ComponentIdentifier.createNpmCoordinates("comp1", "3"), true);

    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "comp2/-/comp2-1.tgz", "hash2-1",
        ComponentIdentifier.createNpmCoordinates("comp2", "1"), true);

    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "@scope/comp3/-/comp3-1.tgz", "hash3-1",
        ComponentIdentifier.createNpmCoordinates("@scope/comp3", "1"), true);
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "@scope/comp3/-/comp3-2.tgz", "hash3-2",
        ComponentIdentifier.createNpmCoordinates("@scope/comp3", "2"), true);

    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "comp4/-/comp4-1.tgz", "hash4-1",
        ComponentIdentifier.createNpmCoordinates("comp4", "1"), false);

    ApiRepositoryPathResponseDTO dto =
        apiRepositoryPathService.getQuarantinedByPathnames("repositoryManager1", repository.getPublicId(),
            Arrays.asList("comp1/-/comp1-1.tgz", "unknown/-/unknown-1.tgz", "@scope/comp3/-/comp3-3.tgz"));

    assertThat(dto.pathVersions).hasSize(3);
    assertThat(dto.pathVersions.get(0).requestIndex).isEqualTo(0);
    assertThat(dto.pathVersions.get(0).repositoryComponentPaths).hasSize(2);
    assertPath(dto.pathVersions.get(0).repositoryComponentPaths.get(0), "comp1/-/comp1-1.tgz");
    assertPath(dto.pathVersions.get(0).repositoryComponentPaths.get(1), "comp1/-/comp1-3.tgz" );
    assertThat(dto.pathVersions.get(1).requestIndex).isEqualTo(1);
    assertThat(dto.pathVersions.get(1).repositoryComponentPaths).isEmpty();
    assertThat(dto.pathVersions.get(2).requestIndex).isEqualTo(2);
    assertThat(dto.pathVersions.get(2).repositoryComponentPaths).hasSize(2);
    assertPath(dto.pathVersions.get(2).repositoryComponentPaths.get(0), "@scope/comp3/-/comp3-1.tgz");
    assertPath(dto.pathVersions.get(2).repositoryComponentPaths.get(1), "@scope/comp3/-/comp3-2.tgz" );
  }

  @Test
  public void testGetQuarantinedByPathnames_NormalizePaths() {
    Repository repository = tempEntity.newRepository("repositoryManager1", "repo1", "npm");
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "comp1/-/comp1-1.tgz", "hash1-1",
        ComponentIdentifier.createNpmCoordinates("comp1", "1"), true);

    // accepts beginning slashes and normalizes the path
    ApiRepositoryPathResponseDTO dto =
        apiRepositoryPathService.getQuarantinedByPathnames("repositoryManager1", repository.getPublicId(),
            Collections.singletonList("/comp1/-/comp1-1.tgz"));

    assertThat(dto.pathVersions).hasSize(1);
    assertThat(dto.pathVersions.get(0).requestIndex).isEqualTo(0);
    assertThat(dto.pathVersions.get(0).repositoryComponentPaths).hasSize(1);
    assertPath(dto.pathVersions.get(0).repositoryComponentPaths.get(0), "comp1/-/comp1-1.tgz");
  }

  @Test
  public void testGetQuarantinedByPathnames_EmptyList() {
    Repository repository = tempEntity.newRepository("repositoryManager1", "repo1", "npm");
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "comp1/-/comp1-1.tgz", "hash1-1",
        ComponentIdentifier.createNpmCoordinates("comp1", "1"), true);

    ApiRepositoryPathResponseDTO dto =
        apiRepositoryPathService.getQuarantinedByPathnames("repositoryManager1", repository.getPublicId(),
            Collections.emptyList());

    assertThat(dto.pathVersions).isEmpty();
  }

  @Test
  public void testGetQuarantinedByPathnames_Null() {
    Repository repository = tempEntity.newRepository("repositoryManager1", "repo1", "npm");
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "comp1/-/comp1-1.tgz", "hash1-1",
        ComponentIdentifier.createNpmCoordinates("comp1", "1"), true);

    ApiRepositoryPathResponseDTO dto =
        apiRepositoryPathService.getQuarantinedByPathnames("repositoryManager1", repository.getPublicId(), null);

    assertThat(dto.pathVersions).isEmpty();
  }

  @Test
  public void testGetQuarantinedByPathnames_InvalidPathFormat() {
    Repository repository = tempEntity.newRepository("repositoryManager1", "repo1", "npm");
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "comp1/-/comp1-1.tgz", "hash1-1",
        ComponentIdentifier.createNpmCoordinates("comp1", "1"), true);

    // using a maven path instead of an npm path
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
        apiRepositoryPathService.getQuarantinedByPathnames("repositoryManager1", repository.getPublicId(),
            Collections.singletonList("g/a/v/a-v.jar")));
  }

  @Test
  public void testGetQuarantinedByPathnames_UnsupportedRepositoryFormat() {
    Repository repository = tempEntity.newRepository("repositoryManager1", "repo1", "maven2");
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "g/a/v/a-v.jar", "hash",
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), true);

    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
        apiRepositoryPathService.getQuarantinedByPathnames("repositoryManager1", repository.getPublicId(),
            Collections.singletonList("g/a/v/a-v.jar")));
  }

  @Test
  public void testGetQuarantinedByPathnames_NotProxyRepository() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo =
        tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_NPM);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      apiRepositoryPathService.getQuarantinedByPathnames(repoManager.getInstanceId(), repo.getPublicId(), null);
    }).withMessage("Repository " + repo.getPublicId() + " (" + repo.getId() + ") is not a proxy repository");
  }

  private void assertPath(final ApiRepositoryComponentPath apiRepositoryComponentPath, String pathname) {
    assertThat(apiRepositoryComponentPath.pathname).isEqualTo(pathname);
    assertThat(apiRepositoryComponentPath.quarantine).isTrue();
  }
}
