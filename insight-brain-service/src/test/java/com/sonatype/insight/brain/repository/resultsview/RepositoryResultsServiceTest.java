/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.resultsview;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsDetailsFilter;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsDetailsFilter.SortField;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsDetailsFilter.SortField.SortableField;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.repository.resultsview.RepositoryResultsDetailsRequestDto.MatchStateFilter;
import com.sonatype.insight.brain.repository.resultsview.RepositoryResultsDetailsRequestDto.SearchFilter;
import com.sonatype.insight.brain.repository.resultsview.RepositoryResultsDetailsRequestDto.SearchFilter.FilterableField;
import com.sonatype.insight.brain.repository.resultsview.RepositoryResultsDetailsRequestDto.ViolationStateFilter;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RepositoryResultsServiceTest
    extends AbstractComponentTest
{
  @Inject
  private RepositoryResultsService repositoryResultsService;

  private Repository repository;

  private Date date = new Date();

  @Before
  public void setup() {
    repository = tempEntity.newRepository();

    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "path1", "hash1",
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"), date, date, null);
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "path3", "hash3",
        ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3", "c3", "e3"), date, null, null);
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.UNKNOWN, "path4", "hash4",
        ComponentIdentifier.createMavenCoordinates("g4", "a4", "v4", "c4", "e4"), date, date, null);

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
  }

  @Test
  public void testGetDetails_All() {
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

    List<RepositoryResultsDetailsResponseDto> responseDtos =
        repositoryResultsService.getDetails(repository.getId(), detailsRequest);

    assertThat(responseDtos).hasSize(3);
    assertThat(responseDtos.get(0).threatLevel).isEqualTo(5);
    assertThat(responseDtos.get(0).policyName).isEqualTo("policy2");
    assertThat(responseDtos.get(0).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v1");
    assertThat(responseDtos.get(0).quarantineTime).isEqualTo(date);
    assertThat(responseDtos.get(0).waived).isEqualTo(false);
    assertThat(responseDtos.get(1).threatLevel).isEqualTo(10);
    assertThat(responseDtos.get(1).policyName).isEqualTo("policy1");
    assertThat(responseDtos.get(1).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v1");
    assertThat(responseDtos.get(1).quarantineTime).isEqualTo(date);
    assertThat(responseDtos.get(1).waived).isEqualTo(false);
    assertThat(responseDtos.get(2).threatLevel).isNull();
    assertThat(responseDtos.get(2).policyName).isNull();
    assertThat(responseDtos.get(2).componentDisplayText).isEqualTo("g3 : a3 : e3 : c3 : v3");
    assertThat(responseDtos.get(2).quarantineTime).isNull();
    assertThat(responseDtos.get(2).waived).isEqualTo(null);
  }

  @Test
  public void testGetDetails_NotViolating() {
    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.violationStateFilters = ImmutableList.of(ViolationStateFilter.VIOLATION_STATE_NOT_VIOLATING);

    List<RepositoryResultsDetailsResponseDto> responseDtos =
        repositoryResultsService.getDetails(repository.getId(), detailsRequest);

    assertThat(responseDtos).hasSize(1);
    assertThat(responseDtos.get(0).threatLevel).isEqualTo(null);
    assertThat(responseDtos.get(0).policyName).isEqualTo(null);
    assertThat(responseDtos.get(0).componentDisplayText).isEqualTo("g3 : a3 : e3 : c3 : v3");
    assertThat(responseDtos.get(0).quarantineTime).isNull();
    assertThat(responseDtos.get(0).waived).isEqualTo(null);
  }

  @Test
  public void testGetDetails_Open() {
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

    List<RepositoryResultsDetailsResponseDto> responseDtos =
        repositoryResultsService.getDetails(repository.getId(), detailsRequest);

    assertThat(responseDtos).hasSize(4);
    assertThat(responseDtos.get(0).threatLevel).isEqualTo(10);
    assertThat(responseDtos.get(0).policyName).isEqualTo("policy1");
    assertThat(responseDtos.get(0).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v1");
    assertThat(responseDtos.get(0).quarantineTime).isEqualTo(date);
    assertThat(responseDtos.get(0).waived).isEqualTo(false);
    assertThat(responseDtos.get(1).threatLevel).isEqualTo(10);
    assertThat(responseDtos.get(1).policyName).isEqualTo("policy1");
    assertThat(responseDtos.get(1).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(responseDtos.get(1).quarantineTime).isEqualTo(date);
    assertThat(responseDtos.get(1).waived).isEqualTo(false);
    assertThat(responseDtos.get(2).threatLevel).isEqualTo(5);
    assertThat(responseDtos.get(2).policyName).isEqualTo("policy2");
    assertThat(responseDtos.get(2).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v1");
    assertThat(responseDtos.get(2).quarantineTime).isEqualTo(date);
    assertThat(responseDtos.get(2).waived).isEqualTo(false);
    assertThat(responseDtos.get(3).threatLevel).isEqualTo(1);
    assertThat(responseDtos.get(3).policyName).isEqualTo("policy3");
    assertThat(responseDtos.get(3).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(responseDtos.get(3).quarantineTime).isEqualTo(date);
    assertThat(responseDtos.get(3).waived).isEqualTo(false);
  }

  @Test
  public void testGetDetails_Quarantined() {
    SortField sortField = new SortField();
    sortField.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField.sortPriority = 1;
    sortField.asc = false;

    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 10;
    detailsRequest.matchStateFilters = ImmutableList.of(MatchStateFilter.MATCH_STATE_UNKNOWN);
    detailsRequest.violationStateFilters = ImmutableList.of(ViolationStateFilter.VIOLATION_STATE_QUARANTINED);
    detailsRequest.sortFields = Arrays.asList(sortField);

    List<RepositoryResultsDetailsResponseDto> responseDtos =
        repositoryResultsService.getDetails(repository.getId(), detailsRequest);

    assertThat(responseDtos).hasSize(2);
    assertThat(responseDtos.get(0).threatLevel).isEqualTo(10);
    assertThat(responseDtos.get(0).policyName).isEqualTo("policy1");
    assertThat(responseDtos.get(0).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(responseDtos.get(0).quarantineTime).isEqualTo(date);
    assertThat(responseDtos.get(0).waived).isEqualTo(false);
    assertThat(responseDtos.get(1).threatLevel).isEqualTo(5);
    assertThat(responseDtos.get(1).policyName).isEqualTo("policy2");
    assertThat(responseDtos.get(1).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(responseDtos.get(1).quarantineTime).isEqualTo(date);
    assertThat(responseDtos.get(1).waived).isEqualTo(true);
  }

  @Test
  public void testGetDetails_Waived() {
    SortField sortField = new SortField();
    sortField.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField.sortPriority = 1;
    sortField.asc = true;

    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.violationStateFilters = ImmutableList.of(ViolationStateFilter.VIOLATION_STATE_WAIVED);

    List<RepositoryResultsDetailsResponseDto> responseDtos =
        repositoryResultsService.getDetails(repository.getId(), detailsRequest);

    assertThat(responseDtos).hasSize(1);
    assertThat(responseDtos.get(0).threatLevel).isEqualTo(5);
    assertThat(responseDtos.get(0).policyName).isEqualTo("policy2");
    assertThat(responseDtos.get(0).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(responseDtos.get(0).quarantineTime).isEqualTo(date);
    assertThat(responseDtos.get(0).waived).isEqualTo(true);
  }

  @Test
  public void testGetDetails_NotViolatingAndOpen() {
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

    List<RepositoryResultsDetailsResponseDto> responseDtos =
        repositoryResultsService.getDetails(repository.getId(), detailsRequest);

    assertThat(responseDtos).hasSize(2);
    assertThat(responseDtos.get(0).threatLevel).isEqualTo(1);
    assertThat(responseDtos.get(0).policyName).isEqualTo("policy3");
    assertThat(responseDtos.get(0).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(responseDtos.get(0).quarantineTime).isEqualTo(date);
    assertThat(responseDtos.get(0).waived).isEqualTo(false);
    assertThat(responseDtos.get(1).threatLevel).isEqualTo(10);
    assertThat(responseDtos.get(1).policyName).isEqualTo("policy1");
    assertThat(responseDtos.get(1).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(responseDtos.get(1).quarantineTime).isEqualTo(date);
    assertThat(responseDtos.get(1).waived).isEqualTo(false);
  }

  @Test
  public void testGetDetails_NotViolatingAndQuarantined() {
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

    List<RepositoryResultsDetailsResponseDto> responseDtos =
        repositoryResultsService.getDetails(repository.getId(), detailsRequest);

    assertThat(responseDtos).hasSize(2);
    assertThat(responseDtos.get(0).threatLevel).isEqualTo(10);
    assertThat(responseDtos.get(0).policyName).isEqualTo("policy1");
    assertThat(responseDtos.get(0).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v1");
    assertThat(responseDtos.get(0).quarantineTime).isEqualTo(date);
    assertThat(responseDtos.get(0).waived).isEqualTo(false);
    assertThat(responseDtos.get(1).threatLevel).isEqualTo(10);
    assertThat(responseDtos.get(1).policyName).isEqualTo("policy1");
    assertThat(responseDtos.get(1).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(responseDtos.get(1).quarantineTime).isEqualTo(date);
    assertThat(responseDtos.get(1).waived).isEqualTo(false);
  }

  @Test
  public void testGetDetails_NotViolatingAndWaived() {
    SortField sortField = new SortField();
    sortField.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField.sortPriority = 1;
    sortField.asc = true;

    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.violationStateFilters = ImmutableList.of(ViolationStateFilter.VIOLATION_STATE_NOT_VIOLATING,
        ViolationStateFilter.VIOLATION_STATE_WAIVED);
    detailsRequest.sortFields = Arrays.asList(sortField);

    List<RepositoryResultsDetailsResponseDto> responseDtos =
        repositoryResultsService.getDetails(repository.getId(), detailsRequest);

    assertThat(responseDtos).hasSize(2);
    assertThat(responseDtos.get(0).threatLevel).isEqualTo(5);
    assertThat(responseDtos.get(0).policyName).isEqualTo("policy2");
    assertThat(responseDtos.get(0).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(responseDtos.get(0).quarantineTime).isEqualTo(date);
    assertThat(responseDtos.get(0).waived).isEqualTo(true);
    assertThat(responseDtos.get(1).threatLevel).isEqualTo(null);
    assertThat(responseDtos.get(1).policyName).isEqualTo(null);
    assertThat(responseDtos.get(1).componentDisplayText).isEqualTo("g3 : a3 : e3 : c3 : v3");
    assertThat(responseDtos.get(1).quarantineTime).isNull();
    assertThat(responseDtos.get(1).waived).isEqualTo(null);
  }

  @Test
  public void testGetDetails_OpenAndQuarantined() {
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
    detailsRequest.sortFields = Arrays.asList(sortField);

    List<RepositoryResultsDetailsResponseDto> responseDtos =
        repositoryResultsService.getDetails(repository.getId(), detailsRequest);

    assertThat(responseDtos).hasSize(2);
    assertThat(responseDtos.get(0).threatLevel).isEqualTo(10);
    assertThat(responseDtos.get(0).policyName).isEqualTo("policy1");
    assertThat(responseDtos.get(0).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v1");
    assertThat(responseDtos.get(0).quarantineTime).isEqualTo(date);
    assertThat(responseDtos.get(0).waived).isEqualTo(false);
    assertThat(responseDtos.get(1).threatLevel).isEqualTo(5);
    assertThat(responseDtos.get(1).policyName).isEqualTo("policy2");
    assertThat(responseDtos.get(1).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v1");
    assertThat(responseDtos.get(1).quarantineTime).isEqualTo(date);
    assertThat(responseDtos.get(1).waived).isEqualTo(false);
  }

  @Test
  public void testGetDetails_OpenAndWaived() {
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
    detailsRequest.pageSize = 1;
    detailsRequest.violationStateFilters =
        ImmutableList.of(ViolationStateFilter.VIOLATION_STATE_OPEN, ViolationStateFilter.VIOLATION_STATE_WAIVED);
    detailsRequest.sortFields = Arrays.asList(sortField1, sortField2);

    List<RepositoryResultsDetailsResponseDto> responseDtos =
        repositoryResultsService.getDetails(repository.getId(), detailsRequest);

    assertThat(responseDtos).hasSize(2);
    assertThat(responseDtos.get(0).threatLevel).isEqualTo(10);
    assertThat(responseDtos.get(0).policyName).isEqualTo("policy1");
    assertThat(responseDtos.get(0).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v1");
    assertThat(responseDtos.get(0).quarantineTime).isEqualTo(date);
    assertThat(responseDtos.get(0).waived).isEqualTo(false);
    assertThat(responseDtos.get(1).threatLevel).isEqualTo(5);
    assertThat(responseDtos.get(1).policyName).isEqualTo("policy2");
    assertThat(responseDtos.get(1).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v1");
    assertThat(responseDtos.get(1).quarantineTime).isEqualTo(date);
    assertThat(responseDtos.get(1).waived).isEqualTo(false);
  }

  @Test
  public void testGetDetails_QuarantinedAndWaived() {
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

    List<RepositoryResultsDetailsResponseDto> responseDtos =
        repositoryResultsService.getDetails(repository.getId(), detailsRequest);

    assertThat(responseDtos).hasSize(1);
    assertThat(responseDtos.get(0).threatLevel).isEqualTo(10);
    assertThat(responseDtos.get(0).policyName).isEqualTo("policy1");
    assertThat(responseDtos.get(0).componentDisplayText).isEqualTo("g1 : a1 : e1 : c1 : v1");
    assertThat(responseDtos.get(0).quarantineTime).isEqualTo(date);
    assertThat(responseDtos.get(0).waived).isEqualTo(false);
  }

  @Test
  public void testGetDetails_NotViolatingAndOpenAndQuarantined() {
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

    List<RepositoryResultsDetailsResponseDto> responseDtos =
        repositoryResultsService.getDetails(repository.getId(), detailsRequest);

    assertThat(responseDtos).hasSize(6);
    assertThat(responseDtos.get(0).threatLevel).isEqualTo(1);
    assertThat(responseDtos.get(0).policyName).isEqualTo("policy3");
    assertThat(responseDtos.get(0).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(responseDtos.get(0).quarantineTime).isEqualTo(date);
    assertThat(responseDtos.get(0).waived).isEqualTo(false);
    assertThat(responseDtos.get(5).threatLevel).isEqualTo(null);
    assertThat(responseDtos.get(5).policyName).isEqualTo(null);
    assertThat(responseDtos.get(5).componentDisplayText).isEqualTo("g3 : a3 : e3 : c3 : v3");
    assertThat(responseDtos.get(5).quarantineTime).isNull();
    assertThat(responseDtos.get(5).waived).isEqualTo(null);
  }

  @Test
  public void testGetDetails_NotViolatingAndQuarantinedAndWaived() {
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

    List<RepositoryResultsDetailsResponseDto> responseDtos =
        repositoryResultsService.getDetails(repository.getId(), detailsRequest);

    assertThat(responseDtos).hasSize(4);
    assertThat(responseDtos.get(0).threatLevel).isEqualTo(5);
    assertThat(responseDtos.get(0).policyName).isEqualTo("policy2");
    assertThat(responseDtos.get(0).componentDisplayText).isEqualTo("g4 : a4 : e4 : c4 : v4");
    assertThat(responseDtos.get(0).quarantineTime).isEqualTo(date);
    assertThat(responseDtos.get(0).waived).isEqualTo(true);
    assertThat(responseDtos.get(3).threatLevel).isEqualTo(null);
    assertThat(responseDtos.get(3).policyName).isEqualTo(null);
    assertThat(responseDtos.get(3).componentDisplayText).isEqualTo("g3 : a3 : e3 : c3 : v3");
    assertThat(responseDtos.get(3).quarantineTime).isNull();
    assertThat(responseDtos.get(3).waived).isEqualTo(null);
  }

  @Test
  public void testGetDetails_invalidPage() {
    RepositoryResultsDetailsRequestDto detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = -1;

    assertThatThrownBy(() -> {
      repositoryResultsService.getDetails(repository.getId(), detailsRequest);
    }).isInstanceOf(BadRequestException.class)
        .hasMessage("Page and Page size must be greater than 0");
  }

  @Test
  public void testGetDetails_invalidSortPriority() {
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

    assertThatThrownBy(() -> {
      repositoryResultsService.getDetails(repository.getId(), detailsRequest);
    }).isInstanceOf(BadRequestException.class)
        .hasMessage("sort priority cannot be same for different fields");
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
    detailsRequest.matchStateFilters = Arrays.asList(MatchStateFilter.MATCH_STATE_ALL);
    detailsRequest.violationStateFilters = Arrays.asList(ViolationStateFilter.VIOLATION_STATE_ALL);
    detailsRequest.searchFilters = Arrays.asList(searchFilter);
    detailsRequest.sortFields = Arrays.asList(sortField1, sortField2);

    RepositoryResultsDetailsFilter filter = repositoryResultsService.validateAndInitializeDetailsFilter(detailsRequest);
    assertThat(filter.page).isEqualTo(1);
    assertThat(filter.pageSize).isEqualTo(1);
    assertThat(filter.matchStateFilter).isEmpty();
    assertThat(filter.violationStateFilters).isEqualTo(ImmutableSet.of("VIOLATION_STATE_ALL"));
    Map<String, String> map = new HashMap<>();
    map.put("COMPONENT_COORDINATES", "g3");
    assertThat(filter.searchFilters).isEqualTo(map);
    assertThat(filter.sortFields).isEqualTo(detailsRequest.sortFields);
  }
}
