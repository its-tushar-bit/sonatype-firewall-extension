/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.resultsview;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsDetails;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsDetailsFilter;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.api.experimental.resultsview.RepositoryResultsDetailsRequestDto.MatchStateFilter;
import com.sonatype.insight.brain.api.experimental.resultsview.RepositoryResultsDetailsRequestDto.SearchFilter;
import com.sonatype.insight.brain.api.experimental.resultsview.RepositoryResultsDetailsRequestDto.ViolationStateFilter;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.api.experimental.resultsview.RepositoryResultsDetailsRequestDto.ViolationStateFilter.VIOLATION_STATE_ALL;

@Named
class RepositoryResultsService
{
  private static final Logger log = LoggerFactory.getLogger(RepositoryResultsService.class);

  private final RepositoryDAO repositoryDAO;

  private final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  @Inject
  RepositoryResultsService(
      final RepositoryDAO repositoryDAO,
      final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO)
  {
    this.repositoryDAO = repositoryDAO;
    this.repositoryPolicyViolationDAO = repositoryPolicyViolationDAO;
  }

  @Authorize(permission = Permission.READ)
  List<RepositoryResultsDetailsResponseDto> getDetails(
      @AuthzContext(Key.REPOSITORY_ID) final String repositoryId,
      final RepositoryResultsDetailsRequestDto detailsRequest)
  {
    final Repository repository = repositoryDAO.getByIdNotNull(repositoryId);

    log.info("Getting repository results for {}:{} ({})", repository.getRepositoryManagerId(), repository.getPublicId(),
        repository.getId());

    RepositoryResultsDetailsFilter detailsFilter = validateAndInitializeDetailsFilter(detailsRequest);

    List<RepositoryResultsDetails> detailsList =
        repositoryPolicyViolationDAO.getRepositoryResultsDetails(repository.getId(), detailsFilter);

    List<RepositoryResultsDetailsResponseDto> detailsResponseDtoList = new ArrayList<>();

    for (RepositoryResultsDetails details : detailsList) {
      detailsResponseDtoList.add(new RepositoryResultsDetailsResponseDto(details));
    }

    return detailsResponseDtoList;
  }

  RepositoryResultsDetailsFilter validateAndInitializeDetailsFilter(
      RepositoryResultsDetailsRequestDto detailsRequest)
  {
    if (detailsRequest.page <= 0 || detailsRequest.pageSize <= 0) {
      throw new BadRequestException("Page and Page size must be greater than 0");
    }

    RepositoryResultsDetailsFilter filter = new RepositoryResultsDetailsFilter();
    filter.page = detailsRequest.page;
    filter.pageSize = detailsRequest.pageSize;
    filter.matchStateFilter = initializeMatchStateFilter(detailsRequest.matchStateFilters);
    filter.violationStateFilters = initializeViolationStateFilters(detailsRequest.violationStateFilters);
    filter.searchFilters = initializeSearchFilterMap(detailsRequest.searchFilters);
    filter.sortFields = detailsRequest.sortFields;

    return filter;
  }

  private String initializeMatchStateFilter(final List<MatchStateFilter> matchStateFilters) {
    String result = "";
    if (!CollectionUtils.isEmpty(matchStateFilters)) {
      for (MatchStateFilter filter : matchStateFilters) {
        switch (filter) {
          case MATCH_STATE_ALL:
            return result;
          case MATCH_STATE_EXACT:
            result = MatchState.EXACT.getId();
            break;
          case MATCH_STATE_UNKNOWN:
            result = MatchState.UNKNOWN.getId();
            break;
          default:
            throw new BadRequestException("Invalid match state filter");
        }
      }
    }

    return result;
  }

  private Set<String> initializeViolationStateFilters(final List<ViolationStateFilter> violationStateFilters) {
    Set<String> result = new HashSet<>();
    if (!CollectionUtils.isEmpty(violationStateFilters)) {
      for (ViolationStateFilter filter : violationStateFilters) {
        if (filter.equals(VIOLATION_STATE_ALL)) {
          return ImmutableSet.of(filter.name());
        }
        result.add(filter.name());
      }
    }

    return result;
  }

  private Map<String, String> initializeSearchFilterMap(final List<SearchFilter> searchFilters) {
    return CollectionUtils.emptyIfNull(searchFilters).stream()
        .collect(Collectors.toMap(searchFilter -> searchFilter.filterableField.name(),
            searchFilter -> searchFilter.value.toLowerCase()));
  }
}
