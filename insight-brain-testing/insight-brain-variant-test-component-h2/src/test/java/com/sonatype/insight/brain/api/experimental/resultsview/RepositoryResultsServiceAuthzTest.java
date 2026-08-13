/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.resultsview;

import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@ComponentH2Test
public class RepositoryResultsServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private RepositoryResultsService repositoryResultsService;

  private RepositoryResultsDetailsRequestDto detailsRequest;

  @BeforeEach
  public void setup() {
    detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
  }

  @Test
  public void testGetDetails_RepositoriesWithReadPermission() {
    DateTime now = DateTime.now();
    final ProxyRepositoryComponent repositoryComponent1 =
        tempEntity.newRepositoryComponent(repository.getId(), "pathname1", now.minusDays(2).toDate(), null);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), repositoryComponent1.getPathname());

    final RepositoryManager repositoryManager2 = tempEntity.newRepositoryManager();
    final Repository repository2 = tempEntity.newRepository(repositoryManager2, "repository2");
    final ProxyRepositoryComponent repositoryComponent2 =
        tempEntity.newRepositoryComponent(repository2.getId(), "pathname2", now.minusDays(1).toDate(), null);
    tempEntity.newRepositoryPolicyViolation(repository2.getId(), repositoryComponent2.getPathname());
    final Repository repository3 = tempEntity.newRepository(repositoryManager2, "repository3");
    final ProxyRepositoryComponent repositoryComponent3 =
        tempEntity.newRepositoryComponent(repository3.getId(), "pathname3", now.toDate(), null);
    tempEntity.newRepositoryPolicyViolation(repository3.getId(), repositoryComponent3.getPathname());

    grantReadPermission(repository.getId());
    RepositoryResultsDetailsResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID,
            detailsRequest);
    List<RepositoryResultsDetailsDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails)
        .as("Read permission given for only one of the repositories.")
        .hasSize(1);
    assertThat(repositoryResultsDetails.get(0).hash).isEqualTo(repositoryComponent1.getHash());

    grantReadPermission(repository2.getId());
    responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY_MANAGER, repositoryManager2.getId(), detailsRequest);
    repositoryResultsDetails = responseDto.repositoryResultsDetails;
    assertThat(repositoryResultsDetails)
        .as("Read permission given for only one of the repositories in repository manager 2.")
        .hasSize(1);
    assertThat(repositoryResultsDetails.get(0).hash).isEqualTo(repositoryComponent2.getHash());

    responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY, repository3.getId(), detailsRequest);
    repositoryResultsDetails = responseDto.repositoryResultsDetails;
    assertThat(repositoryResultsDetails)
        .as("Read permission is not given for repository 3.")
        .hasSize(0);
  }

  @Test
  public void testGetDetails_Unauthenticated() {
    RepositoryResultsDetailsResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID,
            detailsRequest);
    assertThat(responseDto.repositoryResultsDetails).isEmpty();
  }
}
