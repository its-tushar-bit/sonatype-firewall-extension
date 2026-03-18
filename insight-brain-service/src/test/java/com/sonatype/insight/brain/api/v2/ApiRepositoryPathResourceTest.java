/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.api.v2;

import java.util.Arrays;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryPathResponseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryPathResponseDTO.ApiRepositoryComponentPath;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiRepositoryPathResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testGetRepositoryPaths() throws Exception {
    Repository repository = tempEntity.newRepository("repositoryManager1", "repo1", "npm");
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "comp1/-/comp1-1.tgz", "hash1-1",
        ComponentIdentifier.createNpmCoordinates("comp1", "1"), true);

    HttpResponse response =
        restRequest().path(PublicApiPaths.REPOSITORIES_RESOURCE_PATH + "/" + ApiRepositoryPathResource.PATHNAMES_PATH)
            .parameter("repositoryManager1", repository.getPublicId())
            .body(
                Arrays.asList("comp1/-/comp1-1.tgz", "comp2/-/comp2-1.tgz"))
            .post();
    assertResponseStatus(200, response);

    ApiRepositoryPathResponseDTO dto = response.getBody(ApiRepositoryPathResponseDTO.class);

    assertThat(dto.pathVersions).hasSize(2);
    assertThat(dto.pathVersions.get(0).repositoryComponentPaths).hasSize(1);
    assertPath(dto.pathVersions.get(0).repositoryComponentPaths.get(0), "comp1/-/comp1-1.tgz");
    assertThat(dto.pathVersions.get(1).repositoryComponentPaths).isEmpty();
  }

  private void assertPath(final ApiRepositoryComponentPath apiRepositoryComponentPath, String pathname) {
    assertThat(apiRepositoryComponentPath.pathname).isEqualTo(pathname);
    assertThat(apiRepositoryComponentPath.quarantine).isTrue();
  }
}
