/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.resultsview;

import java.util.List;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RepositoryResultsResourceTest
    extends AbstractResourceTest
{
  private Repository repository;

  @Before
  public void setup() {
    repository = tempEntity.newRepository();
  }

  @Test
  public void testGetDetails() throws Exception {
    final RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId());
    final RepositoryPolicyViolation policyViolation = tempEntity.newRepositoryPolicyViolation(repository.getId());

    RepositoryResultsDetailsRequestDto request = new RepositoryResultsDetailsRequestDto();
    request.page = 1;
    request.pageSize = 50;

    final HttpResponse response = restRequest()
        .path(RepositoryResultsResource.RESOURCE_PATH)
        .parameter(repository.getId())
        .body(request).post();

    assertResponseStatus(200, response);
    RepositoryResultsDetailsResponseDto responseDto = response.getBody(RepositoryResultsDetailsResponseDto.class);
    List<RepositoryResultsDetailsDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;
    assertThat(responseDto.repositoryResultsDetails).hasSize(1);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(policyViolation.getThreatLevel());
    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo(policyViolation.getPolicyName());
    assertThat(repositoryResultsDetails.get(0).waived).isEqualTo(policyViolation.isWaived());
    assertThat(repositoryResultsDetails.get(0).componentDisplayText).isEqualTo("g : a : v");
    assertThat(repositoryResultsDetails.get(0).pathname).isEqualTo(repositoryComponent.getPathname());
    assertThat(repositoryResultsDetails.get(0).hash).isEqualTo(repositoryComponent.getHash());
    assertThat(repositoryResultsDetails.get(0).matchStateId).isEqualTo(repositoryComponent.getMatchStateId());
    assertThat(repositoryResultsDetails.get(0).quarantineTime).isEqualTo(
        repositoryComponent.isQuarantined() ? repositoryComponent.getQuarantineTime() : null);

    assertThat(ApiComponentIdentifierDTOV2.toComponentIdentifier(repositoryResultsDetails.get(0).componentIdentifier))
        .isEqualTo(repositoryComponent.getComponentIdentifier());
  }
}
