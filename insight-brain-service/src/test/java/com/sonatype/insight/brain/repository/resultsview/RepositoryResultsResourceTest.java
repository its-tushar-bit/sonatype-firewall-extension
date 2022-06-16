/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.resultsview;

import java.util.Arrays;
import java.util.List;

import com.sonatype.insight.brain.HttpResponse;
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
    List<RepositoryResultsDetailsResponseDto> responseDtos =
        Arrays.asList(response.getBody(RepositoryResultsDetailsResponseDto[].class));
    assertThat(responseDtos).hasSize(1);
    assertThat(responseDtos.get(0).threatLevel).isEqualTo(policyViolation.getThreatLevel());
    assertThat(responseDtos.get(0).policyName).isEqualTo(policyViolation.getPolicyName());
    assertThat(responseDtos.get(0).waived).isEqualTo(policyViolation.isWaived());
    assertThat(responseDtos.get(0).componentDisplayText).isEqualTo("g : a : v");
    assertThat(responseDtos.get(0).quarantineTime).isEqualTo(
        repositoryComponent.isQuarantined() ? repositoryComponent.getQuarantineTime() : null);
  }
}
