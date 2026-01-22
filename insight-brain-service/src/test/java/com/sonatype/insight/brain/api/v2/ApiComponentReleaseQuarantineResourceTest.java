/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Date;

import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentReleasedFromQuarantineDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentPolicyViolationDTO;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiComponentReleaseQuarantineResourceTest
    extends AbstractResourceTest
{
  private static final String REPO_PUBLIC_ID = "publicId";

  @Test
  public void testReleaseQuarantineWithoutReEval() throws Exception {
    PackageUrlIdentifier packageURLIdentifier = new PackageUrlIdentifier("pkg:maven/g1/a1@v1?type=e1");

    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID);

    Date quarantineTime = new Date(System.currentTimeMillis() - 1000);

    RepositoryComponent repositoryComponent = tempEntity
        .newRepositoryComponent(repository.getId(), MatchState.EXACT, "pathname", "hash",
            packageURLIdentifier.ensureCompleteIdentifier(), quarantineTime, quarantineTime);

    HttpResponse response = restRequest().path(PublicApiPaths.COMPONENT_QUARANTINE_RELEASE_PATH_V2)
        .parameter(repositoryComponent.getId())
        .body("waiver comment", MediaType.TEXT_PLAIN).post();
    assertResponseStatus(200, response);

    ApiComponentReleasedFromQuarantineDTO result = response.getBody(ApiComponentReleasedFromQuarantineDTO.class);

    ApiRepositoryComponentPolicyViolationDTO repositoryComponentPolicyViolationDTO =
        result.componentReleasedFromQuarantine;
    assertThat(repositoryComponentPolicyViolationDTO).isNotNull();
    assertThat(repositoryComponentPolicyViolationDTO.component).isNotNull();
    assertThat(repositoryComponentPolicyViolationDTO.component.quarantineTime).isEqualTo(quarantineTime);
    assertThat(repositoryComponentPolicyViolationDTO.component.quarantineReleaseTime).isAfter(quarantineTime);
    assertThat(repositoryComponentPolicyViolationDTO.waivedPolicyViolations).isEmpty();
    assertThat(repositoryComponentPolicyViolationDTO.policyViolations).isEmpty();
  }
}
