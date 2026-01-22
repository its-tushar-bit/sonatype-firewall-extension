/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.resultsview;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.api.experimental.resultsview.RepositoryResultsDetailsRequestDto.MatchStateFilter;
import com.sonatype.insight.brain.api.experimental.resultsview.RepositoryResultsDetailsRequestDto.SearchFilter;
import com.sonatype.insight.brain.api.experimental.resultsview.RepositoryResultsDetailsRequestDto.SearchFilter.FilterableField;
import com.sonatype.insight.brain.api.experimental.resultsview.RepositoryResultsDetailsRequestDto.ViolationStateFilter;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsDetailsFilter;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsDetailsFilter.SortField;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsDetailsFilter.SortField.SortableField;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Category(SlowTest.class)
public class RepositoryResultsServiceTest
    extends AbstractComponentTest
{
  @Inject
  private RepositoryResultsService repositoryResultsService;

  private RepositoryManager repositoryManager;

  private Repository repository;

  private RepositoryManager repositoryManager2;

  private Repository repository2;

  private final Date date = new Date();

  @Before
  public void setup() {
    repositoryManager = tempEntity.newRepositoryManager();
    repository = tempEntity.newRepository(repositoryManager, "publicId");

    // Repository components
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "path1", "hash1",
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"), date, date, null);
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "path3", "hash3",
        ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3", "c3", "e3"), date, null, null);
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.UNKNOWN, "path4", "hash4",
        ComponentIdentifier.createMavenCoordinates("g4", "a4", "v4", "c4", "e4"), date, date, null);

    // Repository policy violations
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 10, "path1", false, Action.ID_FAIL, "1", "policy1",
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"));
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 5, "path1", false, Action.ID_WARN, "2", "policy2",
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"));
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 10, "path4", false, Action.ID_FAIL, "1", "policy1",
        ComponentIdentifier.createMavenCoordinates("g4", "a4", "v4", "c4", "e4"));
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 5, "path4", true, Action.ID_FAIL, "2", "policy2",
        ComponentIdentifier.createMavenCoordinates("g4", "a4", "v4", "c4", "e4"));
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 1, "path4", false, "3", "policy3",
        ComponentIdentifier.createMavenCoordinates("g4", "a4", "v4", "c4", "e4"));

    repositoryManager2 = tempEntity.newRepositoryManager();
    repository2 = tempEntity.newRepository(repositoryManager2, "publicId2");

    tempEntity.newRepositoryComponent(repository2.getId(), MatchState.EXACT, "path", "hash",
        ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"), date, date, null);

    tempEntity.newRepositoryPolicyViolation(repository2.getId(), 10, "path", false, Action.ID_FAIL, "1", "policy1",
        ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"));
    tempEntity.newRepositoryPolicyViolation(repository2.getId(), 5, "path", false, Action.ID_FAIL, "2", "policy2",
        ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"));
  }

  @Test
  public void testGetDetails_Repository_MatchStateFilters_NotSpecified() {
    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField1.sortPriority = 1;
    sortField1.asc = true;

    SortField sortField2 = new SortField();
    sortField2.sortableField = SortableField.COMPONENT_COORDINATES;
    sortField2.sortPriority = 2;
    sortField2.asc = true;

    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.sortFields = Arrays.asList(sortField1, sortField2);

    RepositoryResultsDetailsResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY, repository.getId(), detailsRequest);
    List<RepositoryResultsDetailsDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).hasSize(6);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(1);
    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo("policy3");
    assertThat(repositoryResultsDetails.get(0).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(repositoryResultsDetails.get(0).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(0).waived).isFalse();
    assertThat(repositoryResultsDetails.get(1).threatLevel).isEqualTo(5);
    assertThat(repositoryResultsDetails.get(1).policyName).isEqualTo("policy2");
    assertThat(repositoryResultsDetails.get(1).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v1");
    assertThat(repositoryResultsDetails.get(1).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(1).waived).isFalse();
    assertThat(repositoryResultsDetails.get(2).threatLevel).isEqualTo(5);
    assertThat(repositoryResultsDetails.get(2).policyName).isEqualTo("policy2");
    assertThat(repositoryResultsDetails.get(2).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(repositoryResultsDetails.get(2).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(2).waived).isTrue();
    assertThat(repositoryResultsDetails.get(3).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(3).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(3).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v1");
    assertThat(repositoryResultsDetails.get(3).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(3).waived).isFalse();
    assertThat(repositoryResultsDetails.get(4).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(4).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(4).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(repositoryResultsDetails.get(4).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(4).waived).isFalse();
    assertThat(repositoryResultsDetails.get(5).threatLevel).isNull();
    assertThat(repositoryResultsDetails.get(5).policyName).isNull();
    assertThat(repositoryResultsDetails.get(5).componentDisplayText).isEqualTo("g3 : a3 : e3 : c3 : v3");
    assertThat(repositoryResultsDetails.get(5).quarantineTime).isNull();
    assertThat(repositoryResultsDetails.get(5).waived).isEqualTo(null);
  }

  @Test
  public void testGetDetails_Repository_MatchStateFilters_All() {
    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField1.sortPriority = 1;
    sortField1.asc = true;

    SortField sortField2 = new SortField();
    sortField2.sortableField = SortableField.COMPONENT_COORDINATES;
    sortField2.sortPriority = 2;
    sortField2.asc = true;

    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.sortFields = Arrays.asList(sortField1, sortField2);

    detailsRequest.matchStateFilters = ImmutableList.of(MatchStateFilter.MATCH_STATE_ALL);

    RepositoryResultsDetailsResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY, repository.getId(), detailsRequest);
    List<RepositoryResultsDetailsDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).hasSize(6);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(1);
    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo("policy3");
    assertThat(repositoryResultsDetails.get(0).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(repositoryResultsDetails.get(0).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(0).waived).isFalse();
    assertThat(repositoryResultsDetails.get(1).threatLevel).isEqualTo(5);
    assertThat(repositoryResultsDetails.get(1).policyName).isEqualTo("policy2");
    assertThat(repositoryResultsDetails.get(1).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v1");
    assertThat(repositoryResultsDetails.get(1).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(1).waived).isFalse();
    assertThat(repositoryResultsDetails.get(2).threatLevel).isEqualTo(5);
    assertThat(repositoryResultsDetails.get(2).policyName).isEqualTo("policy2");
    assertThat(repositoryResultsDetails.get(2).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(repositoryResultsDetails.get(2).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(2).waived).isTrue();
    assertThat(repositoryResultsDetails.get(3).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(3).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(3).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v1");
    assertThat(repositoryResultsDetails.get(3).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(3).waived).isFalse();
    assertThat(repositoryResultsDetails.get(4).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(4).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(4).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(repositoryResultsDetails.get(4).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(4).waived).isFalse();
    assertThat(repositoryResultsDetails.get(5).threatLevel).isNull();
    assertThat(repositoryResultsDetails.get(5).policyName).isNull();
    assertThat(repositoryResultsDetails.get(5).componentDisplayText).isEqualTo("g3 : a3 : e3 : c3 : v3");
    assertThat(repositoryResultsDetails.get(5).quarantineTime).isNull();
    assertThat(repositoryResultsDetails.get(5).waived).isEqualTo(null);
  }

  @Test
  public void testGetDetails_Repository_MatchStateFilters_ExactAndUnkown() {
    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField1.sortPriority = 1;
    sortField1.asc = true;

    SortField sortField2 = new SortField();
    sortField2.sortableField = SortableField.COMPONENT_COORDINATES;
    sortField2.sortPriority = 2;
    sortField2.asc = true;

    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.sortFields = Arrays.asList(sortField1, sortField2);

    detailsRequest.matchStateFilters =
        ImmutableList.of(MatchStateFilter.MATCH_STATE_EXACT, MatchStateFilter.MATCH_STATE_UNKNOWN);

    RepositoryResultsDetailsResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY, repository.getId(), detailsRequest);
    List<RepositoryResultsDetailsDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).hasSize(6);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(1);
    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo("policy3");
    assertThat(repositoryResultsDetails.get(0).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(repositoryResultsDetails.get(0).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(0).waived).isFalse();
    assertThat(repositoryResultsDetails.get(1).threatLevel).isEqualTo(5);
    assertThat(repositoryResultsDetails.get(1).policyName).isEqualTo("policy2");
    assertThat(repositoryResultsDetails.get(1).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v1");
    assertThat(repositoryResultsDetails.get(1).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(1).waived).isFalse();
    assertThat(repositoryResultsDetails.get(2).threatLevel).isEqualTo(5);
    assertThat(repositoryResultsDetails.get(2).policyName).isEqualTo("policy2");
    assertThat(repositoryResultsDetails.get(2).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(repositoryResultsDetails.get(2).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(2).waived).isTrue();
    assertThat(repositoryResultsDetails.get(3).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(3).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(3).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v1");
    assertThat(repositoryResultsDetails.get(3).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(3).waived).isFalse();
    assertThat(repositoryResultsDetails.get(4).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(4).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(4).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(repositoryResultsDetails.get(4).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(4).waived).isFalse();
    assertThat(repositoryResultsDetails.get(5).threatLevel).isNull();
    assertThat(repositoryResultsDetails.get(5).policyName).isNull();
    assertThat(repositoryResultsDetails.get(5).componentDisplayText).isEqualTo("g3 : a3 : e3 : c3 : v3");
    assertThat(repositoryResultsDetails.get(5).quarantineTime).isNull();
    assertThat(repositoryResultsDetails.get(5).waived).isEqualTo(null);
  }

  @Test
  public void testGetDetails_Repository_MatchStateFilters_Exact() {
    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField1.sortPriority = 1;
    sortField1.asc = true;

    SortField sortField2 = new SortField();
    sortField2.sortableField = SortableField.COMPONENT_COORDINATES;
    sortField2.sortPriority = 2;
    sortField2.asc = true;

    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.sortFields = Arrays.asList(sortField1, sortField2);

    detailsRequest.matchStateFilters = ImmutableList.of(MatchStateFilter.MATCH_STATE_EXACT);

    RepositoryResultsDetailsResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY, repository.getId(), detailsRequest);
    List<RepositoryResultsDetailsDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).hasSize(3);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(5);
    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo("policy2");
    assertThat(repositoryResultsDetails.get(0).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v1");
    assertThat(repositoryResultsDetails.get(0).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(0).waived).isFalse();
    assertThat(repositoryResultsDetails.get(1).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(1).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(1).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v1");
    assertThat(repositoryResultsDetails.get(1).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(1).waived).isFalse();
    assertThat(repositoryResultsDetails.get(2).threatLevel).isNull();
    assertThat(repositoryResultsDetails.get(2).policyName).isNull();
    assertThat(repositoryResultsDetails.get(2).componentDisplayText).isEqualTo("g3 : a3 : e3 : c3 : v3");
    assertThat(repositoryResultsDetails.get(2).quarantineTime).isNull();
    assertThat(repositoryResultsDetails.get(2).waived).isEqualTo(null);
  }

  @Test
  public void testGetDetails_Repository_MatchStateFilters_Unknown() {
    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField1.sortPriority = 1;
    sortField1.asc = true;

    SortField sortField2 = new SortField();
    sortField2.sortableField = SortableField.COMPONENT_COORDINATES;
    sortField2.sortPriority = 2;
    sortField2.asc = true;

    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.sortFields = Arrays.asList(sortField1, sortField2);

    detailsRequest.matchStateFilters = ImmutableList.of(MatchStateFilter.MATCH_STATE_UNKNOWN);

    RepositoryResultsDetailsResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY, repository.getId(), detailsRequest);
    List<RepositoryResultsDetailsDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).hasSize(3);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(1);
    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo("policy3");
    assertThat(repositoryResultsDetails.get(0).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(repositoryResultsDetails.get(0).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(0).waived).isFalse();
    assertThat(repositoryResultsDetails.get(1).threatLevel).isEqualTo(5);
    assertThat(repositoryResultsDetails.get(1).policyName).isEqualTo("policy2");
    assertThat(repositoryResultsDetails.get(1).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(repositoryResultsDetails.get(1).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(1).waived).isTrue();
    assertThat(repositoryResultsDetails.get(2).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(2).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(2).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(repositoryResultsDetails.get(2).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(2).waived).isFalse();
  }

  @Test
  public void testGetDetails_Repository_All() {
    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField1.sortPriority = 1;
    sortField1.asc = true;

    SortField sortField2 = new SortField();
    sortField2.sortableField = SortableField.COMPONENT_COORDINATES;
    sortField2.sortPriority = 2;
    sortField2.asc = true;

    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.matchStateFilters = ImmutableList.of(MatchStateFilter.MATCH_STATE_EXACT);
    detailsRequest.violationStateFilters = ImmutableList.of(ViolationStateFilter.VIOLATION_STATE_ALL);
    detailsRequest.sortFields = Arrays.asList(sortField1, sortField2);

    RepositoryResultsDetailsResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY, repository.getId(), detailsRequest);
    List<RepositoryResultsDetailsDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).hasSize(3);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(5);
    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo("policy2");
    assertThat(repositoryResultsDetails.get(0).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v1");
    assertThat(repositoryResultsDetails.get(0).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(0).waived).isEqualTo(false);
    assertThat(repositoryResultsDetails.get(1).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(1).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(1).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v1");
    assertThat(repositoryResultsDetails.get(1).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(1).waived).isEqualTo(false);
    assertThat(repositoryResultsDetails.get(2).threatLevel).isNull();
    assertThat(repositoryResultsDetails.get(2).policyName).isNull();
    assertThat(repositoryResultsDetails.get(2).componentDisplayText).isEqualTo("g3 : a3 : e3 : c3 : v3");
    assertThat(repositoryResultsDetails.get(2).quarantineTime).isNull();
    assertThat(repositoryResultsDetails.get(2).waived).isEqualTo(null);
  }

  @Test
  public void testGetDetails_Repository_Aggregate() {
    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField1.sortPriority = 1;
    sortField1.asc = true;

    SortField sortField2 = new SortField();
    sortField2.sortableField = SortableField.COMPONENT_COORDINATES;
    sortField2.sortPriority = 2;
    sortField2.asc = true;

    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.matchStateFilters = ImmutableList.of(MatchStateFilter.MATCH_STATE_EXACT);
    detailsRequest.violationStateFilters = ImmutableList.of(ViolationStateFilter.VIOLATION_STATE_ALL);
    detailsRequest.sortFields = Arrays.asList(sortField1, sortField2);
    detailsRequest.aggregate = true;

    RepositoryResultsDetailsResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY, repository.getId(), detailsRequest);
    List<RepositoryResultsDetailsDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).hasSize(2);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(0).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v1");
    assertThat(repositoryResultsDetails.get(0).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(0).waived).isNull();
    assertThat(repositoryResultsDetails.get(1).threatLevel).isNull();
    assertThat(repositoryResultsDetails.get(1).policyName).isNull();
    assertThat(repositoryResultsDetails.get(1).componentDisplayText).isEqualTo("g3 : a3 : e3 : c3 : v3");
    assertThat(repositoryResultsDetails.get(1).quarantineTime).isNull();
    assertThat(repositoryResultsDetails.get(1).waived).isNull();
  }

  @Test
  public void testGetDetails_Repository_NotViolating() {
    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.violationStateFilters = ImmutableList.of(ViolationStateFilter.VIOLATION_STATE_NOT_VIOLATING);

    RepositoryResultsDetailsResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY, repository.getId(), detailsRequest);
    List<RepositoryResultsDetailsDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).hasSize(1);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(null);
    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo(null);
    assertThat(repositoryResultsDetails.get(0).componentDisplayText).isEqualTo("g3 : a3 : e3 : c3 : v3");
    assertThat(repositoryResultsDetails.get(0).quarantineTime).isNull();
    assertThat(repositoryResultsDetails.get(0).waived).isEqualTo(null);
  }

  @Test
  public void testGetDetails_Repository_Open() {
    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField1.sortPriority = 1;
    sortField1.asc = false;

    SortField sortField2 = new SortField();
    sortField2.sortableField = SortableField.COMPONENT_COORDINATES;
    sortField2.sortPriority = 2;
    sortField2.asc = true;

    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.matchStateFilters = ImmutableList.of(MatchStateFilter.MATCH_STATE_ALL);
    detailsRequest.violationStateFilters = ImmutableList.of(ViolationStateFilter.VIOLATION_STATE_OPEN);
    detailsRequest.sortFields = Arrays.asList(sortField1, sortField2);

    RepositoryResultsDetailsResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY, repository.getId(), detailsRequest);
    List<RepositoryResultsDetailsDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).hasSize(4);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(0).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v1");
    assertThat(repositoryResultsDetails.get(0).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(0).waived).isEqualTo(false);
    assertThat(repositoryResultsDetails.get(1).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(1).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(1).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(repositoryResultsDetails.get(1).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(1).waived).isEqualTo(false);
    assertThat(repositoryResultsDetails.get(2).threatLevel).isEqualTo(5);
    assertThat(repositoryResultsDetails.get(2).policyName).isEqualTo("policy2");
    assertThat(repositoryResultsDetails.get(2).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v1");
    assertThat(repositoryResultsDetails.get(2).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(2).waived).isEqualTo(false);
    assertThat(repositoryResultsDetails.get(3).threatLevel).isEqualTo(1);
    assertThat(repositoryResultsDetails.get(3).policyName).isEqualTo("policy3");
    assertThat(repositoryResultsDetails.get(3).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(repositoryResultsDetails.get(3).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(3).waived).isEqualTo(false);
  }

  @Test
  public void testGetDetails_Repository_Quarantined() {
    SortField sortField = new SortField();
    sortField.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField.sortPriority = 1;
    sortField.asc = false;

    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 10;
    detailsRequest.matchStateFilters = ImmutableList.of(MatchStateFilter.MATCH_STATE_UNKNOWN);
    detailsRequest.violationStateFilters = ImmutableList.of(ViolationStateFilter.VIOLATION_STATE_QUARANTINED);
    detailsRequest.sortFields = Collections.singletonList(sortField);

    RepositoryResultsDetailsResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY, repository.getId(), detailsRequest);
    List<RepositoryResultsDetailsDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).hasSize(2);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(0).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(repositoryResultsDetails.get(0).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(0).waived).isEqualTo(false);
    assertThat(repositoryResultsDetails.get(1).threatLevel).isEqualTo(5);
    assertThat(repositoryResultsDetails.get(1).policyName).isEqualTo("policy2");
    assertThat(repositoryResultsDetails.get(1).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(repositoryResultsDetails.get(1).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(1).waived).isEqualTo(true);
  }

  @Test
  public void testGetDetails_Repository_Waived() {
    SortField sortField = new SortField();
    sortField.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField.sortPriority = 1;
    sortField.asc = true;

    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.violationStateFilters = ImmutableList.of(ViolationStateFilter.VIOLATION_STATE_WAIVED);

    RepositoryResultsDetailsResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY, repository.getId(), detailsRequest);
    List<RepositoryResultsDetailsDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).hasSize(1);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(5);
    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo("policy2");
    assertThat(repositoryResultsDetails.get(0).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(repositoryResultsDetails.get(0).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(0).waived).isEqualTo(true);
  }

  @Test
  public void testGetDetails_Repository_NotViolatingAndOpen() {
    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField1.sortPriority = 1;
    sortField1.asc = true;

    SortField sortField2 = new SortField();
    sortField2.sortableField = SortableField.COMPONENT_COORDINATES;
    sortField2.sortPriority = 2;
    sortField2.asc = true;

    SearchFilter searchFilter = new SearchFilter();
    searchFilter.filterableField = FilterableField.COMPONENT_COORDINATES;
    searchFilter.value = "g4";

    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.violationStateFilters =
        ImmutableList.of(ViolationStateFilter.VIOLATION_STATE_NOT_VIOLATING, ViolationStateFilter.VIOLATION_STATE_OPEN);
    detailsRequest.searchFilters = ImmutableList.of(searchFilter);
    detailsRequest.sortFields = Arrays.asList(sortField1, sortField2);

    RepositoryResultsDetailsResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY, repository.getId(), detailsRequest);
    List<RepositoryResultsDetailsDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).hasSize(2);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(1);
    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo("policy3");
    assertThat(repositoryResultsDetails.get(0).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(repositoryResultsDetails.get(0).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(0).waived).isEqualTo(false);
    assertThat(repositoryResultsDetails.get(1).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(1).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(1).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(repositoryResultsDetails.get(1).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(1).waived).isEqualTo(false);
  }

  @Test
  public void testGetDetails_Repository_NotViolatingAndQuarantined() {
    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.POLICY_NAME;
    sortField1.sortPriority = 1;
    sortField1.asc = true;

    SortField sortField2 = new SortField();
    sortField2.sortableField = SortableField.COMPONENT_COORDINATES;
    sortField2.sortPriority = 2;
    sortField2.asc = true;

    SearchFilter searchFilter = new SearchFilter();
    searchFilter.filterableField = FilterableField.POLICY_NAME;
    searchFilter.value = "policy1";

    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.violationStateFilters = ImmutableList.of(ViolationStateFilter.VIOLATION_STATE_NOT_VIOLATING,
        ViolationStateFilter.VIOLATION_STATE_QUARANTINED);
    detailsRequest.searchFilters = ImmutableList.of(searchFilter);
    detailsRequest.sortFields = Arrays.asList(sortField1, sortField2);

    RepositoryResultsDetailsResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY, repository.getId(), detailsRequest);
    List<RepositoryResultsDetailsDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).hasSize(2);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(0).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v1");
    assertThat(repositoryResultsDetails.get(0).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(0).waived).isEqualTo(false);
    assertThat(repositoryResultsDetails.get(1).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(1).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(1).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(repositoryResultsDetails.get(1).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(1).waived).isEqualTo(false);
  }

  @Test
  public void testGetDetails_Repository_NotViolatingAndWaived() {
    SortField sortField = new SortField();
    sortField.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField.sortPriority = 1;
    sortField.asc = true;

    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.violationStateFilters = ImmutableList.of(ViolationStateFilter.VIOLATION_STATE_NOT_VIOLATING,
        ViolationStateFilter.VIOLATION_STATE_WAIVED);
    detailsRequest.sortFields = Collections.singletonList(sortField);

    RepositoryResultsDetailsResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY, repository.getId(), detailsRequest);
    List<RepositoryResultsDetailsDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).hasSize(2);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(5);
    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo("policy2");
    assertThat(repositoryResultsDetails.get(0).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(repositoryResultsDetails.get(0).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(0).waived).isEqualTo(true);
    assertThat(repositoryResultsDetails.get(1).threatLevel).isEqualTo(null);
    assertThat(repositoryResultsDetails.get(1).policyName).isEqualTo(null);
    assertThat(repositoryResultsDetails.get(1).componentDisplayText).isEqualTo("g3 : a3 : e3 : c3 : v3");
    assertThat(repositoryResultsDetails.get(1).quarantineTime).isNull();
    assertThat(repositoryResultsDetails.get(1).waived).isEqualTo(null);
  }

  @Test
  public void testGetDetails_Repository_OpenAndQuarantined() {
    SortField sortField = new SortField();
    sortField.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField.sortPriority = 1;
    sortField.asc = false;

    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.matchStateFilters = ImmutableList.of(MatchStateFilter.MATCH_STATE_EXACT);
    detailsRequest.violationStateFilters =
        ImmutableList.of(ViolationStateFilter.VIOLATION_STATE_OPEN, ViolationStateFilter.VIOLATION_STATE_QUARANTINED);
    detailsRequest.sortFields = Collections.singletonList(sortField);

    RepositoryResultsDetailsResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY, repository.getId(), detailsRequest);
    List<RepositoryResultsDetailsDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).hasSize(2);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(0).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v1");
    assertThat(repositoryResultsDetails.get(0).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(0).waived).isEqualTo(false);
    assertThat(repositoryResultsDetails.get(1).threatLevel).isEqualTo(5);
    assertThat(repositoryResultsDetails.get(1).policyName).isEqualTo("policy2");
    assertThat(repositoryResultsDetails.get(1).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v1");
    assertThat(repositoryResultsDetails.get(1).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(1).waived).isEqualTo(false);
  }

  @Test
  public void testGetDetails_Repository_OpenAndWaived() {
    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.COMPONENT_COORDINATES;
    sortField1.sortPriority = 1;
    sortField1.asc = true;

    SortField sortField2 = new SortField();
    sortField2.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField2.sortPriority = 2;
    sortField2.asc = false;

    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 2;
    detailsRequest.violationStateFilters =
        ImmutableList.of(ViolationStateFilter.VIOLATION_STATE_OPEN, ViolationStateFilter.VIOLATION_STATE_WAIVED);
    detailsRequest.sortFields = Arrays.asList(sortField1, sortField2);

    RepositoryResultsDetailsResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY, repository.getId(), detailsRequest);
    List<RepositoryResultsDetailsDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).hasSize(2);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(0).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v1");
    assertThat(repositoryResultsDetails.get(0).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(0).waived).isEqualTo(false);
    assertThat(repositoryResultsDetails.get(1).threatLevel).isEqualTo(5);
    assertThat(repositoryResultsDetails.get(1).policyName).isEqualTo("policy2");
    assertThat(repositoryResultsDetails.get(1).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v1");
    assertThat(repositoryResultsDetails.get(1).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(1).waived).isEqualTo(false);
  }

  @Test
  public void testGetDetails_Repository_QuarantinedAndWaived() {
    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField1.sortPriority = 1;
    sortField1.asc = true;

    SortField sortField2 = new SortField();
    sortField2.sortableField = SortableField.COMPONENT_COORDINATES;
    sortField2.sortPriority = 2;
    sortField2.asc = true;

    SearchFilter searchFilter = new SearchFilter();
    searchFilter.filterableField = FilterableField.COMPONENT_COORDINATES;
    searchFilter.value = "g1";

    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.searchFilters = ImmutableList.of(searchFilter);
    detailsRequest.violationStateFilters =
        ImmutableList.of(ViolationStateFilter.VIOLATION_STATE_QUARANTINED, ViolationStateFilter.VIOLATION_STATE_WAIVED);
    detailsRequest.sortFields = Arrays.asList(sortField1, sortField2);

    RepositoryResultsDetailsResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY, repository.getId(), detailsRequest);
    List<RepositoryResultsDetailsDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).hasSize(1);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(0).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v1");
    assertThat(repositoryResultsDetails.get(0).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(0).waived).isEqualTo(false);
  }

  @Test
  public void testGetDetails_Repository_NotViolatingAndOpenAndQuarantined() {
    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField1.sortPriority = 1;
    sortField1.asc = true;

    SortField sortField2 = new SortField();
    sortField2.sortableField = SortableField.COMPONENT_COORDINATES;
    sortField2.sortPriority = 2;
    sortField2.asc = true;

    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.violationStateFilters =
        ImmutableList.of(ViolationStateFilter.VIOLATION_STATE_NOT_VIOLATING, ViolationStateFilter.VIOLATION_STATE_OPEN,
            ViolationStateFilter.VIOLATION_STATE_QUARANTINED);
    detailsRequest.sortFields = Arrays.asList(sortField1, sortField2);

    RepositoryResultsDetailsResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY, repository.getId(), detailsRequest);
    List<RepositoryResultsDetailsDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).hasSize(6);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(1);
    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo("policy3");
    assertThat(repositoryResultsDetails.get(0).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(repositoryResultsDetails.get(0).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(0).waived).isEqualTo(false);
    assertThat(repositoryResultsDetails.get(5).threatLevel).isEqualTo(null);
    assertThat(repositoryResultsDetails.get(5).policyName).isEqualTo(null);
    assertThat(repositoryResultsDetails.get(5).componentDisplayText).isEqualTo("g3 : a3 : e3 : c3 : v3");
    assertThat(repositoryResultsDetails.get(5).quarantineTime).isNull();
    assertThat(repositoryResultsDetails.get(5).waived).isEqualTo(null);
  }

  @Test
  public void testGetDetails_Repository_NotViolatingAndQuarantinedAndWaived() {
    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField1.sortPriority = 1;
    sortField1.asc = true;

    SortField sortField2 = new SortField();
    sortField2.sortableField = SortableField.COMPONENT_COORDINATES;
    sortField2.sortPriority = 2;
    sortField2.asc = true;

    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.violationStateFilters = ImmutableList.of(ViolationStateFilter.VIOLATION_STATE_NOT_VIOLATING,
        ViolationStateFilter.VIOLATION_STATE_QUARANTINED, ViolationStateFilter.VIOLATION_STATE_WAIVED);
    detailsRequest.sortFields = Arrays.asList(sortField1, sortField2);

    RepositoryResultsDetailsResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY, repository.getId(), detailsRequest);
    List<RepositoryResultsDetailsDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).hasSize(4);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(5);
    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo("policy2");
    assertThat(repositoryResultsDetails.get(0).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(repositoryResultsDetails.get(0).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(0).waived).isEqualTo(true);
    assertThat(repositoryResultsDetails.get(3).threatLevel).isEqualTo(null);
    assertThat(repositoryResultsDetails.get(3).policyName).isEqualTo(null);
    assertThat(repositoryResultsDetails.get(3).componentDisplayText).isEqualTo("g3 : a3 : e3 : c3 : v3");
    assertThat(repositoryResultsDetails.get(3).quarantineTime).isNull();
    assertThat(repositoryResultsDetails.get(3).waived).isEqualTo(null);
  }

  @Test
  public void testGetDetails_Repository_invalidPage() {
    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = -1;

    assertThatThrownBy(
        () -> repositoryResultsService.getDetails(OwnerType.REPOSITORY, repository.getId(), detailsRequest))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Page and Page size must be greater than 0");
  }

  @Test
  public void testGetDetails_Repository_invalidSortPriority() {
    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField1.sortPriority = 2;
    sortField1.asc = false;

    SortField sortField2 = new SortField();
    sortField2.sortableField = SortableField.COMPONENT_COORDINATES;
    sortField2.sortPriority = 2;
    sortField2.asc = true;

    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 1;
    detailsRequest.sortFields = Arrays.asList(sortField1, sortField2);

    assertThatThrownBy(
        () -> repositoryResultsService.getDetails(OwnerType.REPOSITORY, repository.getId(), detailsRequest))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("sort priority cannot be the same for different fields");
  }

  @Test
  public void testGetDetails_Repository_MissingRequestParameters() {
    assertThatThrownBy(() -> repositoryResultsService.getDetails(OwnerType.REPOSITORY, repository.getId(), null))
        .isInstanceOf(BadRequestException.class).hasMessage("Missing request parameters");
  }

  @Test
  public void testValidateAndInitializeDetailsFilter() {
    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField1.sortPriority = 2;
    sortField1.asc = false;

    SortField sortField2 = new SortField();
    sortField2.sortableField = SortableField.COMPONENT_COORDINATES;
    sortField2.sortPriority = 1;
    sortField2.asc = true;

    SearchFilter searchFilter = new SearchFilter();
    searchFilter.filterableField = FilterableField.COMPONENT_COORDINATES;
    searchFilter.value = "g3";

    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 1;
    detailsRequest.matchStateFilters = Collections.singletonList(MatchStateFilter.MATCH_STATE_ALL);
    detailsRequest.violationStateFilters = Collections.singletonList(ViolationStateFilter.VIOLATION_STATE_ALL);
    detailsRequest.searchFilters = Collections.singletonList(searchFilter);
    detailsRequest.sortFields = Arrays.asList(sortField1, sortField2);

    RepositoryResultsDetailsFilter filter =
        repositoryResultsService.validateAndInitializeDetailsFilter(OwnerType.REPOSITORY, detailsRequest);
    assertThat(filter.page).isEqualTo(1);
    assertThat(filter.pageSize).isEqualTo(1);
    assertThat(filter.matchStateFilter).isEmpty();
    assertThat(filter.violationStateFilters).isEqualTo(ImmutableSet.of("VIOLATION_STATE_ALL"));
    Map<String, String> map = new HashMap<>();
    map.put("COMPONENT_COORDINATES", "g3");
    assertThat(filter.searchFilters).isEqualTo(map);
    assertThat(filter.sortFields).isEqualTo(detailsRequest.sortFields);
    assertThat(filter.formatExclusionPatterns).isNotNull();
    assertThat(filter.formatExclusionPatterns).containsEntry("nuget", List.of("%.json"));
  }

  @Test
  public void testGetDetails_Repository_SearchByComponentCoordinates() {
    SortField sortField = new SortField();
    sortField.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField.sortPriority = 1;
    sortField.asc = false;

    SearchFilter searchFilter = new SearchFilter();
    searchFilter.filterableField = FilterableField.COMPONENT_COORDINATES;
    searchFilter.value = "g1 : a1";

    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.searchFilters = Collections.singletonList(searchFilter);
    detailsRequest.sortFields = Collections.singletonList(sortField);

    RepositoryResultsDetailsResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY, repository.getId(), detailsRequest);
    List<RepositoryResultsDetailsDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).hasSize(2);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(0).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v1");
    assertThat(repositoryResultsDetails.get(0).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(0).waived).isEqualTo(false);
    assertThat(repositoryResultsDetails.get(1).threatLevel).isEqualTo(5);
    assertThat(repositoryResultsDetails.get(1).policyName).isEqualTo("policy2");
    assertThat(repositoryResultsDetails.get(1).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v1");
    assertThat(repositoryResultsDetails.get(1).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(1).waived).isEqualTo(false);
  }

  @Test
  public void testGetDetails_Repository_SortByComponentCoordinates() {
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "path0", "hash0",
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v0", "c1", "e1"), date, date, null);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 5, "path0", false, Action.ID_FAIL, "1", "policy1",
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v0", "c1", "e1"));

    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.COMPONENT_COORDINATES;
    sortField1.sortPriority = 1;
    sortField1.asc = true;
    SortField sortField2 = new SortField();
    sortField2.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField2.sortPriority = 2;
    sortField2.asc = false;

    SearchFilter searchFilter = new SearchFilter();
    searchFilter.filterableField = FilterableField.COMPONENT_COORDINATES;
    searchFilter.value = "g1 : a1";

    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.searchFilters = Collections.singletonList(searchFilter);
    detailsRequest.sortFields = Arrays.asList(sortField1, sortField2);

    RepositoryResultsDetailsResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY, repository.getId(), detailsRequest);
    List<RepositoryResultsDetailsDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).hasSize(3);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(5);
    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(0).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v0");
    assertThat(repositoryResultsDetails.get(0).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(0).waived).isEqualTo(false);
    assertThat(repositoryResultsDetails.get(1).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(1).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(1).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v1");
    assertThat(repositoryResultsDetails.get(1).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(1).waived).isEqualTo(false);
    assertThat(repositoryResultsDetails.get(2).threatLevel).isEqualTo(5);
    assertThat(repositoryResultsDetails.get(2).policyName).isEqualTo("policy2");
    assertThat(repositoryResultsDetails.get(2).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v1");
    assertThat(repositoryResultsDetails.get(2).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(2).waived).isEqualTo(false);
  }

  @Test
  public void testGetDetails_Repository_UnknownComponent_WithPolicyViolation() {
    repository = tempEntity.newRepository();
    RepositoryComponent unknownComponent = tempEntity.newRepositoryComponent(repository.getId(), MatchState.UNKNOWN,
        "testpathname", null /* componentIdentifier */, false);
    tempEntity.newRepositoryPolicyViolation(unknownComponent, "testPolicyId");

    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;

    RepositoryResultsDetailsResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY, repository.getId(), detailsRequest);
    List<RepositoryResultsDetailsDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).hasSize(1);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(5);
    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo("policyName");
    assertThat(repositoryResultsDetails.get(0).componentDisplayText).isEqualTo("testpathname (testpathname)");
    assertThat(repositoryResultsDetails.get(0).quarantineTime).isNull();
    assertThat(repositoryResultsDetails.get(0).waived).isEqualTo(false);
  }

  @Test
  public void testGetDetails_Repository_UnknownComponent_WithoutPolicyViolation() {
    repository = tempEntity.newRepository();
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.UNKNOWN, "testpathname",
        null /* componentIdentifier */, false);

    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;

    RepositoryResultsDetailsResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY, repository.getId(), detailsRequest);
    List<RepositoryResultsDetailsDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).hasSize(1);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isNull();
    assertThat(repositoryResultsDetails.get(0).policyName).isNull();
    assertThat(repositoryResultsDetails.get(0).componentDisplayText).isEqualTo("testpathname (testpathname)");
    assertThat(repositoryResultsDetails.get(0).quarantineTime).isNull();
    assertThat(repositoryResultsDetails.get(0).waived).isNull();
  }

  @Test
  public void testGetDetails_Repository_ThreatLevelFilters() {
    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField1.sortPriority = 1;
    sortField1.asc = true;

    SortField sortField2 = new SortField();
    sortField2.sortableField = SortableField.COMPONENT_COORDINATES;
    sortField2.sortPriority = 2;
    sortField2.asc = true;

    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.sortFields = Arrays.asList(sortField1, sortField2);

    detailsRequest.threatLevelFilters = Arrays.asList(5, 5);

    RepositoryResultsDetailsResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY, repository.getId(), detailsRequest);
    List<RepositoryResultsDetailsDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).hasSize(2);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(5);
    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo("policy2");
    assertThat(repositoryResultsDetails.get(0).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v1");
    assertThat(repositoryResultsDetails.get(0).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(0).waived).isFalse();
    assertThat(repositoryResultsDetails.get(1).threatLevel).isEqualTo(5);
    assertThat(repositoryResultsDetails.get(1).policyName).isEqualTo("policy2");
    assertThat(repositoryResultsDetails.get(1).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(repositoryResultsDetails.get(1).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(1).waived).isTrue();
  }

  @Test
  public void testGetDetails_Repository_QuarantineTimeFilter() {
    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField1.sortPriority = 1;
    sortField1.asc = true;

    SortField sortField2 = new SortField();
    sortField2.sortableField = SortableField.COMPONENT_COORDINATES;
    sortField2.sortPriority = 2;
    sortField2.asc = true;

    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.sortFields = Arrays.asList(sortField1, sortField2);

    SearchFilter searchFilter = new SearchFilter();
    searchFilter.filterableField = FilterableField.QUARANTINE_TIME;
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    searchFilter.value = simpleDateFormat.format(date);
    detailsRequest.searchFilters = Collections.singletonList(searchFilter);

    RepositoryResultsDetailsResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY, repository.getId(), detailsRequest);
    List<RepositoryResultsDetailsDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).hasSize(5);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(1);
    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo("policy3");
    assertThat(repositoryResultsDetails.get(0).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(repositoryResultsDetails.get(0).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(0).waived).isFalse();
    assertThat(repositoryResultsDetails.get(1).threatLevel).isEqualTo(5);
    assertThat(repositoryResultsDetails.get(1).policyName).isEqualTo("policy2");
    assertThat(repositoryResultsDetails.get(1).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v1");
    assertThat(repositoryResultsDetails.get(1).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(1).waived).isFalse();
    assertThat(repositoryResultsDetails.get(2).threatLevel).isEqualTo(5);
    assertThat(repositoryResultsDetails.get(2).policyName).isEqualTo("policy2");
    assertThat(repositoryResultsDetails.get(2).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(repositoryResultsDetails.get(2).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(2).waived).isTrue();
    assertThat(repositoryResultsDetails.get(3).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(3).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(3).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v1");
    assertThat(repositoryResultsDetails.get(3).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(3).waived).isFalse();
    assertThat(repositoryResultsDetails.get(4).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(4).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(4).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(repositoryResultsDetails.get(4).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(4).waived).isFalse();
  }

  @Test
  public void testGetDetails_Repository_hasNextPage() {
    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 5;

    RepositoryResultsDetailsResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY, repository.getId(), detailsRequest);
    assertThat(responseDto.hasNextPage).isTrue();

    detailsRequest.pageSize = 10;

    responseDto = repositoryResultsService.getDetails(OwnerType.REPOSITORY, repository.getId(), detailsRequest);
    assertThat(responseDto.hasNextPage).isFalse();
  }

  @Test
  public void testGetDetails_Repository_SearchIdFilters() {
    SearchFilter searchFilter1 = new SearchFilter();
    searchFilter1.filterableField = FilterableField.REPOSITORY_ID;
    searchFilter1.value = repository.getId();

    SearchFilter searchFilter2 = new SearchFilter();
    searchFilter2.filterableField = FilterableField.REPOSITORY_MANAGER_ID;
    searchFilter2.value = repositoryManager.getId();

    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.searchFilters = ImmutableList.of(searchFilter1);

    // throws exception as repositoryId is not a valid for the owner type Repository
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> repositoryResultsService.getDetails(OwnerType.REPOSITORY, repository.getId(), detailsRequest))
        .withMessage("SearchFilter is not valid for the ownerType REPOSITORY.");

    detailsRequest.searchFilters = ImmutableList.of(searchFilter2);

    // throws exception as repositoryManagerId is not a valid for the owner type Repository
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> repositoryResultsService.getDetails(OwnerType.REPOSITORY, repository.getId(), detailsRequest))
        .withMessage("SearchFilter is not valid for the ownerType REPOSITORY.");
  }

  @Test
  public void testGetDetails_RepositoryContainer_NonAggregate() {
    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField1.sortPriority = 1;
    sortField1.asc = true;

    SortField sortField2 = new SortField();
    sortField2.sortableField = SortableField.COMPONENT_COORDINATES;
    sortField2.sortPriority = 2;
    sortField2.asc = true;

    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.sortFields = Arrays.asList(sortField1, sortField2);

    // Repository Container level: returns details of all repository managers
    RepositoryResultsDetailsResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID,
            detailsRequest);
    List<RepositoryResultsDetailsDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).hasSize(8);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(1);
    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo("policy3");
    assertThat(repositoryResultsDetails.get(0).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(repositoryResultsDetails.get(0).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(0).waived).isFalse();
    assertThat(repositoryResultsDetails.get(1).threatLevel).isEqualTo(5);
    assertThat(repositoryResultsDetails.get(1).policyName).isEqualTo("policy2");
    assertThat(repositoryResultsDetails.get(1).componentDisplayText).isEqualTo("g : a : e : c : v");
    assertThat(repositoryResultsDetails.get(1).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(1).waived).isFalse();
    assertThat(repositoryResultsDetails.get(2).threatLevel).isEqualTo(5);
    assertThat(repositoryResultsDetails.get(2).policyName).isEqualTo("policy2");
    assertThat(repositoryResultsDetails.get(2).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v1");
    assertThat(repositoryResultsDetails.get(2).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(2).waived).isFalse();
    assertThat(repositoryResultsDetails.get(3).threatLevel).isEqualTo(5);
    assertThat(repositoryResultsDetails.get(3).policyName).isEqualTo("policy2");
    assertThat(repositoryResultsDetails.get(3).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(repositoryResultsDetails.get(3).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(3).waived).isTrue();
    assertThat(repositoryResultsDetails.get(4).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(4).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(4).componentDisplayText).isEqualTo("g : a : e : c : v");
    assertThat(repositoryResultsDetails.get(4).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(4).waived).isFalse();
    assertThat(repositoryResultsDetails.get(5).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(5).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(5).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v1");
    assertThat(repositoryResultsDetails.get(5).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(5).waived).isFalse();
    assertThat(repositoryResultsDetails.get(6).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(6).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(6).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(repositoryResultsDetails.get(6).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(6).waived).isFalse();
    assertThat(repositoryResultsDetails.get(7).threatLevel).isNull();
    assertThat(repositoryResultsDetails.get(7).policyName).isNull();
    assertThat(repositoryResultsDetails.get(7).componentDisplayText).isEqualTo("g3 : a3 : e3 : c3 : v3");
    assertThat(repositoryResultsDetails.get(7).quarantineTime).isNull();
    assertThat(repositoryResultsDetails.get(7).waived).isEqualTo(null);
  }

  @Test
  public void testGetDetails_RepositoryContainer_Aggregate() {
    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField1.sortPriority = 1;
    sortField1.asc = false;

    SortField sortField2 = new SortField();
    sortField2.sortableField = SortableField.COMPONENT_COORDINATES;
    sortField2.sortPriority = 2;
    sortField2.asc = true;

    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.sortFields = Arrays.asList(sortField1, sortField2);
    detailsRequest.aggregate = true;

    // Repository Container level: returns details of all repository managers
    RepositoryResultsDetailsResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID,
            detailsRequest);
    List<RepositoryResultsDetailsDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).hasSize(4);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(0).componentDisplayText).isEqualTo("g : a : e : c : v");
    assertThat(repositoryResultsDetails.get(0).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(0).waived).isNull();
    assertThat(repositoryResultsDetails.get(1).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(1).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(1).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v1");
    assertThat(repositoryResultsDetails.get(1).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(1).waived).isNull();
    assertThat(repositoryResultsDetails.get(2).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(2).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(2).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(repositoryResultsDetails.get(2).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(2).waived).isNull();
    assertThat(repositoryResultsDetails.get(3).threatLevel).isNull();
    assertThat(repositoryResultsDetails.get(3).policyName).isNull();
    assertThat(repositoryResultsDetails.get(3).componentDisplayText).isEqualTo("g3 : a3 : e3 : c3 : v3");
    assertThat(repositoryResultsDetails.get(3).quarantineTime).isNull();
    assertThat(repositoryResultsDetails.get(3).waived).isNull();
  }

  @Test
  public void testGetDetails_RepositoryContainer_SearchIdFilters() {
    SearchFilter searchFilter1 = new SearchFilter();
    searchFilter1.filterableField = FilterableField.REPOSITORY_ID;
    searchFilter1.value = repository2.getId();

    SearchFilter searchFilter2 = new SearchFilter();
    searchFilter2.filterableField = FilterableField.REPOSITORY_MANAGER_ID;
    searchFilter2.value = repositoryManager.getId();

    SearchFilter searchFilter3 = new SearchFilter();
    searchFilter3.filterableField = FilterableField.REPOSITORY_MANAGER_ID;
    searchFilter3.value = repositoryManager2.getId();

    SearchFilter searchFilter4 = new SearchFilter();
    searchFilter4.filterableField = FilterableField.REPOSITORY_MANAGER_ID;
    searchFilter4.value = "randomID";

    SearchFilter searchFilter5 = new SearchFilter();
    searchFilter5.filterableField = FilterableField.REPOSITORY_ID;
    searchFilter5.value = "randomID";

    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField1.sortPriority = 1;
    sortField1.asc = true;

    SortField sortField2 = new SortField();
    sortField2.sortableField = SortableField.COMPONENT_COORDINATES;
    sortField2.sortPriority = 2;
    sortField2.asc = true;

    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.searchFilters = ImmutableList.of(searchFilter1);
    detailsRequest.sortFields = Arrays.asList(sortField1, sortField2);

    // return details of only repository 2
    RepositoryResultsDetailsResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID,
            detailsRequest);
    List<RepositoryResultsDetailsDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;
    assertThat(repositoryResultsDetails).hasSize(2);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(5);
    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo("policy2");
    assertThat(repositoryResultsDetails.get(0).componentDisplayText).isEqualTo("g : a : e : c : v");
    assertThat(repositoryResultsDetails.get(0).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(0).waived).isEqualTo(false);
    assertThat(repositoryResultsDetails.get(1).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(1).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(1).componentDisplayText).isEqualTo("g : a : e : c : v");
    assertThat(repositoryResultsDetails.get(1).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(1).waived).isEqualTo(false);

    detailsRequest.searchFilters = ImmutableList.of(searchFilter2);

    // return details of only repositories in repositoryManager and not in repositoryManager2
    responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID,
            detailsRequest);
    repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).hasSize(6);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(1);
    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo("policy3");
    assertThat(repositoryResultsDetails.get(0).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(repositoryResultsDetails.get(0).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(0).waived).isFalse();
    assertThat(repositoryResultsDetails.get(1).threatLevel).isEqualTo(5);
    assertThat(repositoryResultsDetails.get(1).policyName).isEqualTo("policy2");
    assertThat(repositoryResultsDetails.get(1).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v1");
    assertThat(repositoryResultsDetails.get(1).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(1).waived).isFalse();
    assertThat(repositoryResultsDetails.get(2).threatLevel).isEqualTo(5);
    assertThat(repositoryResultsDetails.get(2).policyName).isEqualTo("policy2");
    assertThat(repositoryResultsDetails.get(2).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(repositoryResultsDetails.get(2).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(2).waived).isTrue();
    assertThat(repositoryResultsDetails.get(3).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(3).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(3).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v1");
    assertThat(repositoryResultsDetails.get(3).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(3).waived).isFalse();
    assertThat(repositoryResultsDetails.get(4).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(4).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(4).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(repositoryResultsDetails.get(4).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(4).waived).isFalse();
    assertThat(repositoryResultsDetails.get(5).threatLevel).isNull();
    assertThat(repositoryResultsDetails.get(5).policyName).isNull();
    assertThat(repositoryResultsDetails.get(5).componentDisplayText).isEqualTo("g3 : a3 : e3 : c3 : v3");
    assertThat(repositoryResultsDetails.get(5).quarantineTime).isNull();
    assertThat(repositoryResultsDetails.get(5).waived).isEqualTo(null);

    detailsRequest.searchFilters = ImmutableList.of(searchFilter1, searchFilter3);

    // return details of repository2 as it exists in repositoryManager2
    responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID,
            detailsRequest);
    repositoryResultsDetails = responseDto.repositoryResultsDetails;
    assertThat(repositoryResultsDetails).hasSize(2);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(5);
    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo("policy2");
    assertThat(repositoryResultsDetails.get(0).componentDisplayText).isEqualTo("g : a : e : c : v");
    assertThat(repositoryResultsDetails.get(0).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(0).waived).isEqualTo(false);
    assertThat(repositoryResultsDetails.get(1).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(1).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(1).componentDisplayText).isEqualTo("g : a : e : c : v");
    assertThat(repositoryResultsDetails.get(1).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(1).waived).isEqualTo(false);

    detailsRequest.searchFilters = ImmutableList.of(searchFilter1, searchFilter2);

    // returns empty list as repository2 does not exist in repositoryManager
    responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID,
            detailsRequest);
    repositoryResultsDetails = responseDto.repositoryResultsDetails;
    assertThat(repositoryResultsDetails).isEmpty();

    detailsRequest.searchFilters = ImmutableList.of(searchFilter4);

    // returns empty list as repository manager does not exist
    responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID,
            detailsRequest);
    repositoryResultsDetails = responseDto.repositoryResultsDetails;
    assertThat(repositoryResultsDetails).isEmpty();

    detailsRequest.searchFilters = ImmutableList.of(searchFilter5);

    // returns empty list as repository does not exist
    responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID,
            detailsRequest);
    repositoryResultsDetails = responseDto.repositoryResultsDetails;
    assertThat(repositoryResultsDetails).isEmpty();
  }

  @Test
  public void testGetDetails_RepositoryContainer_AllFilters() {
    SearchFilter searchFilter1 = new SearchFilter();
    searchFilter1.filterableField = FilterableField.REPOSITORY_ID;
    searchFilter1.value = repository.getId();

    SearchFilter searchFilter2 = new SearchFilter();
    searchFilter2.filterableField = FilterableField.COMPONENT_COORDINATES;
    searchFilter2.value = "g1";

    SearchFilter searchFilter3 = new SearchFilter();
    searchFilter3.filterableField = FilterableField.POLICY_NAME;
    searchFilter3.value = "policy1";

    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField1.sortPriority = 1;
    sortField1.asc = false;

    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 5;
    detailsRequest.searchFilters = ImmutableList.of(searchFilter1, searchFilter2, searchFilter3);
    detailsRequest.matchStateFilters =
        ImmutableList.of(MatchStateFilter.MATCH_STATE_EXACT);
    detailsRequest.violationStateFilters = ImmutableList.of(ViolationStateFilter.VIOLATION_STATE_OPEN);
    detailsRequest.threatLevelFilters = Arrays.asList(5, 10);
    detailsRequest.sortFields = Arrays.asList(sortField1);

    RepositoryResultsDetailsResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID,
            detailsRequest);
    List<RepositoryResultsDetailsDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;
    assertThat(repositoryResultsDetails).hasSize(1);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(0).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v1");
    assertThat(repositoryResultsDetails.get(0).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(0).waived).isEqualTo(false);
  }

  @Test
  public void testGetDetails_RepositoryManager_NonAggregate() {
    SortField sortField = new SortField();
    sortField.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField.sortPriority = 1;
    sortField.asc = false;

    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.sortFields = Arrays.asList(sortField);

    // Repository Manager level: returns only details of repositoryManager2
    RepositoryResultsDetailsResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY_MANAGER, repositoryManager2.getId(), detailsRequest);
    List<RepositoryResultsDetailsDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).hasSize(2);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(0).componentDisplayText).isEqualTo("g : a : e : c : v");
    assertThat(repositoryResultsDetails.get(0).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(0).waived).isFalse();
    assertThat(repositoryResultsDetails.get(1).threatLevel).isEqualTo(5);
    assertThat(repositoryResultsDetails.get(1).policyName).isEqualTo("policy2");
    assertThat(repositoryResultsDetails.get(1).componentDisplayText).isEqualTo("g : a : e : c : v");
    assertThat(repositoryResultsDetails.get(1).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(1).waived).isFalse();
    assertThat(responseDto.hasNextPage).isFalse();
  }

  @Test
  public void testGetDetails_RepositoryManager_Aggregate() {
    SortField sortField = new SortField();
    sortField.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField.sortPriority = 1;
    sortField.asc = false;

    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.sortFields = Arrays.asList(sortField);
    detailsRequest.aggregate = true;

    // Repository Manager level: returns only details of repositoryManager2
    RepositoryResultsDetailsResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY_MANAGER, repositoryManager2.getId(), detailsRequest);
    List<RepositoryResultsDetailsDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).hasSize(1);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(0).componentDisplayText).isEqualTo("g : a : e : c : v");
    assertThat(repositoryResultsDetails.get(0).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(0).waived).isNull();
    assertThat(responseDto.hasNextPage).isFalse();
  }

  @Test
  public void testGetDetails_RepositoryManager_SearchIdFilters() {
    SearchFilter searchFilter1 = new SearchFilter();
    searchFilter1.filterableField = FilterableField.REPOSITORY_ID;
    searchFilter1.value = repository2.getId();

    SearchFilter searchFilter2 = new SearchFilter();
    searchFilter2.filterableField = FilterableField.REPOSITORY_MANAGER_ID;
    searchFilter2.value = repositoryManager2.getId();

    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField1.sortPriority = 1;
    sortField1.asc = true;

    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.searchFilters = ImmutableList.of(searchFilter1);
    detailsRequest.sortFields = Arrays.asList(sortField1);

    // return details of repository2
    RepositoryResultsDetailsResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY_MANAGER, repositoryManager2.getId(), detailsRequest);
    List<RepositoryResultsDetailsDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;
    assertThat(repositoryResultsDetails).hasSize(2);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(5);
    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo("policy2");
    assertThat(repositoryResultsDetails.get(0).componentDisplayText).isEqualTo("g : a : e : c : v");
    assertThat(repositoryResultsDetails.get(0).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(0).waived).isFalse();
    assertThat(repositoryResultsDetails.get(1).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(1).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(1).componentDisplayText).isEqualTo("g : a : e : c : v");
    assertThat(repositoryResultsDetails.get(1).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(1).waived).isFalse();

    detailsRequest.searchFilters = ImmutableList.of(searchFilter1, searchFilter2);

    // throws exception as repositoryManagerId is not a valid for the owner type Repository Manager
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> repositoryResultsService.getDetails(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId(),
            detailsRequest))
        .withMessage("SearchFilter is not valid for the ownerType REPOSITORY_MANAGER.");
  }

  @Test
  public void testGetDetails_RepositoryManager_AllFilters() {
    SearchFilter searchFilter1 = new SearchFilter();
    searchFilter1.filterableField = FilterableField.REPOSITORY_ID;
    searchFilter1.value = repository2.getId();

    SearchFilter searchFilter2 = new SearchFilter();
    searchFilter2.filterableField = FilterableField.COMPONENT_COORDINATES;
    searchFilter2.value = "g";

    SearchFilter searchFilter3 = new SearchFilter();
    searchFilter3.filterableField = FilterableField.POLICY_NAME;
    searchFilter3.value = "policy1";

    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField1.sortPriority = 1;
    sortField1.asc = true;

    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.searchFilters = ImmutableList.of(searchFilter1, searchFilter2, searchFilter3);
    detailsRequest.matchStateFilters =
        ImmutableList.of(MatchStateFilter.MATCH_STATE_EXACT, MatchStateFilter.MATCH_STATE_UNKNOWN);
    detailsRequest.violationStateFilters = ImmutableList.of(ViolationStateFilter.VIOLATION_STATE_ALL);
    detailsRequest.threatLevelFilters = Arrays.asList(3, 10);
    detailsRequest.sortFields = Arrays.asList(sortField1);

    RepositoryResultsDetailsResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY_MANAGER, repositoryManager2.getId(), detailsRequest);
    List<RepositoryResultsDetailsDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;
    assertThat(repositoryResultsDetails).hasSize(1);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(0).componentDisplayText).isEqualTo("g : a : e : c : v");
    assertThat(repositoryResultsDetails.get(0).quarantineTime).isEqualTo(date);
    assertThat(repositoryResultsDetails.get(0).waived).isFalse();
  }
}
