/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.resultsview;

import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.util.Arrays;
import java.util.List;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsDetailsFilter.SortField;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsDetailsFilter.SortField.SortableField;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.joda.time.DateTime;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class RepositoryResultsResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testGetDetails() throws Exception {
    DateTime now = DateTime.now();
    final RepositoryManager repositoryManager1 = tempEntity.newRepositoryManager();
    final Repository repository1 = tempEntity.newRepository(repositoryManager1, "repository1");
    final RepositoryComponent repositoryComponent1 =
        tempEntity.newRepositoryComponent(repository1.getId(), "pathname1", now.minusDays(2).toDate(), null);
    final RepositoryPolicyViolation policyViolation1 =
        tempEntity.newRepositoryPolicyViolation(repository1.getId(), repositoryComponent1.getPathname());
    final RepositoryManager repositoryManager2 = tempEntity.newRepositoryManager();
    final Repository repository2 = tempEntity.newRepository(repositoryManager2, "repository2");
    final RepositoryComponent repositoryComponent2 =
        tempEntity.newRepositoryComponent(repository2.getId(), "pathname2", now.minusDays(1).toDate(), null);
    final RepositoryPolicyViolation policyViolation2 =
        tempEntity.newRepositoryPolicyViolation(repository2.getId(), repositoryComponent2.getPathname());
    final Repository repository3 = tempEntity.newRepository(repositoryManager2, "repository3");
    final RepositoryComponent repositoryComponent3 =
        tempEntity.newRepositoryComponent(repository3.getId(), "pathname3", now.toDate(), null);
    final RepositoryPolicyViolation policyViolation3 =
        tempEntity.newRepositoryPolicyViolation(repository3.getId(), repositoryComponent3.getPathname());

    SortField sortField = new SortField();
    sortField.sortableField = SortableField.QUARANTINE_TIME;
    sortField.sortPriority = 1;
    sortField.asc = true;

    RepositoryResultsDetailsRequestDto request = new RepositoryResultsDetailsRequestDto();
    request.page = 1;
    request.pageSize = 5;
    request.sortFields = Arrays.asList(sortField);

    // Repository Container Level: response must include all components in all repository managers
    HttpResponse response = restRequest()
        .path(RepositoryResultsResource.RESOURCE_PATH, RepositoryResultsResource.DETAILS_BY_OWNER_PATH)
        .parameter("repository_container", RepositoryContainer.REPOSITORY_CONTAINER_ID)
        .body(request)
        .post();

    assertResponseStatus(200, response);
    RepositoryResultsDetailsResponseDto responseDto = response.getBody(RepositoryResultsDetailsResponseDto.class);
    List<RepositoryResultsDetailsDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;
    assertThat(responseDto.repositoryResultsDetails).hasSize(3);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(policyViolation1.getThreatLevel());
    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo(policyViolation1.getPolicyName());
    assertThat(repositoryResultsDetails.get(0).waived).isEqualTo(policyViolation1.isWaived());
    assertThat(repositoryResultsDetails.get(0).componentDisplayText).isEqualTo("g : a : v");
    assertThat(repositoryResultsDetails.get(0).pathname).isEqualTo(repositoryComponent1.getPathname());
    assertThat(repositoryResultsDetails.get(0).hash).isEqualTo(repositoryComponent1.getHash());
    assertThat(repositoryResultsDetails.get(0).matchStateId).isEqualTo(repositoryComponent1.getMatchStateId());
    assertThat(repositoryResultsDetails.get(0).quarantineTime).isEqualTo(repositoryComponent1.getQuarantineTime());
    assertThat(repositoryResultsDetails.get(0).lastEvaluationTime)
        .isEqualTo(repositoryComponent1.getLastEvaluationTime());
    assertThat(ApiComponentIdentifierDTOV2.toComponentIdentifier(repositoryResultsDetails.get(0).componentIdentifier))
        .isEqualTo(repositoryComponent1.getComponentIdentifier());

    assertThat(repositoryResultsDetails.get(1).threatLevel).isEqualTo(policyViolation2.getThreatLevel());
    assertThat(repositoryResultsDetails.get(1).policyName).isEqualTo(policyViolation2.getPolicyName());
    assertThat(repositoryResultsDetails.get(1).waived).isEqualTo(policyViolation2.isWaived());
    assertThat(repositoryResultsDetails.get(1).componentDisplayText).isEqualTo("g : a : v");
    assertThat(repositoryResultsDetails.get(1).pathname).isEqualTo(repositoryComponent2.getPathname());
    assertThat(repositoryResultsDetails.get(1).hash).isEqualTo(repositoryComponent2.getHash());
    assertThat(repositoryResultsDetails.get(1).matchStateId).isEqualTo(repositoryComponent2.getMatchStateId());
    assertThat(repositoryResultsDetails.get(1).quarantineTime).isEqualTo(repositoryComponent2.getQuarantineTime());
    assertThat(repositoryResultsDetails.get(1).lastEvaluationTime)
        .isEqualTo(repositoryComponent2.getLastEvaluationTime());
    assertThat(ApiComponentIdentifierDTOV2.toComponentIdentifier(repositoryResultsDetails.get(1).componentIdentifier))
        .isEqualTo(repositoryComponent2.getComponentIdentifier());

    assertThat(repositoryResultsDetails.get(2).threatLevel).isEqualTo(policyViolation3.getThreatLevel());
    assertThat(repositoryResultsDetails.get(2).policyName).isEqualTo(policyViolation3.getPolicyName());
    assertThat(repositoryResultsDetails.get(2).waived).isEqualTo(policyViolation3.isWaived());
    assertThat(repositoryResultsDetails.get(2).componentDisplayText).isEqualTo("g : a : v");
    assertThat(repositoryResultsDetails.get(2).pathname).isEqualTo(repositoryComponent3.getPathname());
    assertThat(repositoryResultsDetails.get(2).hash).isEqualTo(repositoryComponent3.getHash());
    assertThat(repositoryResultsDetails.get(2).matchStateId).isEqualTo(repositoryComponent3.getMatchStateId());
    assertThat(repositoryResultsDetails.get(2).quarantineTime).isEqualTo(repositoryComponent3.getQuarantineTime());
    assertThat(repositoryResultsDetails.get(2).lastEvaluationTime)
        .isEqualTo(repositoryComponent3.getLastEvaluationTime());
    assertThat(ApiComponentIdentifierDTOV2.toComponentIdentifier(repositoryResultsDetails.get(2).componentIdentifier))
        .isEqualTo(repositoryComponent3.getComponentIdentifier());

    // Repository Manager Level: response must include all components in repository manager 2
    response = restRequest()
        .path(RepositoryResultsResource.RESOURCE_PATH, RepositoryResultsResource.DETAILS_BY_OWNER_PATH)
        .parameter("repository_manager", repositoryManager2.getId())
        .body(request)
        .post();

    assertResponseStatus(200, response);
    responseDto = response.getBody(RepositoryResultsDetailsResponseDto.class);
    repositoryResultsDetails = responseDto.repositoryResultsDetails;
    assertThat(responseDto.repositoryResultsDetails).hasSize(2);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(policyViolation2.getThreatLevel());
    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo(policyViolation2.getPolicyName());
    assertThat(repositoryResultsDetails.get(0).waived).isEqualTo(policyViolation2.isWaived());
    assertThat(repositoryResultsDetails.get(0).componentDisplayText).isEqualTo("g : a : v");
    assertThat(repositoryResultsDetails.get(0).pathname).isEqualTo(repositoryComponent2.getPathname());
    assertThat(repositoryResultsDetails.get(0).hash).isEqualTo(repositoryComponent2.getHash());
    assertThat(repositoryResultsDetails.get(0).matchStateId).isEqualTo(repositoryComponent2.getMatchStateId());
    assertThat(repositoryResultsDetails.get(0).quarantineTime).isEqualTo(repositoryComponent2.getQuarantineTime());
    assertThat(repositoryResultsDetails.get(0).lastEvaluationTime)
        .isEqualTo(repositoryComponent2.getLastEvaluationTime());
    assertThat(ApiComponentIdentifierDTOV2.toComponentIdentifier(repositoryResultsDetails.get(0).componentIdentifier))
        .isEqualTo(repositoryComponent2.getComponentIdentifier());

    assertThat(repositoryResultsDetails.get(1).threatLevel).isEqualTo(policyViolation3.getThreatLevel());
    assertThat(repositoryResultsDetails.get(1).policyName).isEqualTo(policyViolation3.getPolicyName());
    assertThat(repositoryResultsDetails.get(1).waived).isEqualTo(policyViolation3.isWaived());
    assertThat(repositoryResultsDetails.get(1).componentDisplayText).isEqualTo("g : a : v");
    assertThat(repositoryResultsDetails.get(1).pathname).isEqualTo(repositoryComponent3.getPathname());
    assertThat(repositoryResultsDetails.get(1).hash).isEqualTo(repositoryComponent3.getHash());
    assertThat(repositoryResultsDetails.get(1).matchStateId).isEqualTo(repositoryComponent3.getMatchStateId());
    assertThat(repositoryResultsDetails.get(1).quarantineTime).isEqualTo(repositoryComponent3.getQuarantineTime());
    assertThat(repositoryResultsDetails.get(1).lastEvaluationTime)
        .isEqualTo(repositoryComponent3.getLastEvaluationTime());
    assertThat(ApiComponentIdentifierDTOV2.toComponentIdentifier(repositoryResultsDetails.get(1).componentIdentifier))
        .isEqualTo(repositoryComponent3.getComponentIdentifier());

    // Repository Level: response must include all components in repository 1
    response = restRequest()
        .path(RepositoryResultsResource.RESOURCE_PATH, RepositoryResultsResource.DETAILS_BY_OWNER_PATH)
        .parameter("repository", repository1.getId())
        .body(request)
        .post();

    assertResponseStatus(200, response);
    responseDto = response.getBody(RepositoryResultsDetailsResponseDto.class);
    repositoryResultsDetails = responseDto.repositoryResultsDetails;
    assertThat(responseDto.repositoryResultsDetails).hasSize(1);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(policyViolation1.getThreatLevel());
    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo(policyViolation1.getPolicyName());
    assertThat(repositoryResultsDetails.get(0).waived).isEqualTo(policyViolation1.isWaived());
    assertThat(repositoryResultsDetails.get(0).componentDisplayText).isEqualTo("g : a : v");
    assertThat(repositoryResultsDetails.get(0).pathname).isEqualTo(repositoryComponent1.getPathname());
    assertThat(repositoryResultsDetails.get(0).hash).isEqualTo(repositoryComponent1.getHash());
    assertThat(repositoryResultsDetails.get(0).matchStateId).isEqualTo(repositoryComponent1.getMatchStateId());
    assertThat(repositoryResultsDetails.get(0).quarantineTime).isEqualTo(repositoryComponent1.getQuarantineTime());
    assertThat(repositoryResultsDetails.get(0).lastEvaluationTime)
        .isEqualTo(repositoryComponent1.getLastEvaluationTime());
    assertThat(ApiComponentIdentifierDTOV2.toComponentIdentifier(repositoryResultsDetails.get(0).componentIdentifier))
        .isEqualTo(repositoryComponent1.getComponentIdentifier());
  }
}
