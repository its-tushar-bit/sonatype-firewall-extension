/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.resultsview;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.api.experimental.resultsview.RepositoryResultsDetailsRequestDto.MatchStateFilter;
import com.sonatype.insight.brain.api.experimental.resultsview.RepositoryResultsDetailsRequestDto.SearchFilter;
import com.sonatype.insight.brain.api.experimental.resultsview.RepositoryResultsDetailsRequestDto.ViolationStateFilter;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsCountSummary;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsDetails;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsDetailsFilter;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.AuthzFilter;
import com.sonatype.insight.brain.security.AuthzFilter.Context;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class RepositoryResultsService
{
  private static final Logger log = LoggerFactory.getLogger(RepositoryResultsService.class);

  private static final int MIN_THREAT_LEVEL = 0;

  private static final int MAX_THREAT_LEVEL = 10;

  /**
   * Maximum page size allowed for bulk waiver page results pagination.
   * This limit only applies when isBulkWaiverPage flag is true in the request.
   */
  static final int MAX_BULK_WAIVER_PAGE_SIZE = 1000;

  private final RepositoryDAO repositoryDAO;

  private final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  private final RepositoryManagerDAO repositoryManagerDAO;

  @Inject
  public RepositoryResultsService(
      final RepositoryDAO repositoryDAO,
      final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO,
      final RepositoryManagerDAO repositoryManagerDAO)
  {
    this.repositoryDAO = repositoryDAO;
    this.repositoryPolicyViolationDAO = repositoryPolicyViolationDAO;
    this.repositoryManagerDAO = repositoryManagerDAO;
  }

  @AuthzFilter(permission = Permission.READ, context = Context.REPOSITORY)
  List<Repository> filterRepositoriesWithReadPermission(List<Repository> repositories) {
    return repositories;
  }

  RepositoryResultsDetailsResponseDto getDetails(
      OwnerType ownerType,
      String ownerId,
      final RepositoryResultsDetailsRequestDto detailsRequest)
  {
    long start = System.currentTimeMillis();

    log.info("Getting repository results for {} id {}", ownerType, ownerId);

    if (detailsRequest == null) {
      throw new BadRequestException("Missing request parameters");
    }

    RepositoryResultsDetailsFilter detailsFilter = validateAndInitializeDetailsFilter(ownerType, detailsRequest);

    List<Repository> repositories;
    String repositoryManagerId = detailsFilter.searchFilters.getOrDefault("REPOSITORY_MANAGER_ID", null);
    String repositoryId = detailsFilter.searchFilters.getOrDefault("REPOSITORY_ID", null);

    switch (ownerType) {
      case REPOSITORY:
        repositories = Collections.singletonList(repositoryDAO.getByIdNotNull(ownerId));
        break;
      case REPOSITORY_MANAGER:
        ownerId = repositoryManagerDAO.getByIdNotNull(ownerId).getId();
        if (repositoryId != null) {
          repositories = convertToRepositoryList(repositoryDAO.getByRepositoryIdAndManagerId(ownerId, repositoryId));
        }
        else {
          repositories = repositoryDAO.getByRepositoryManagerIdAndRepositoryType(ownerId, RepositoryType.proxy);
        }
        break;
      case REPOSITORY_CONTAINER:
        if (repositoryManagerId != null && repositoryId != null) {
          repositories =
              convertToRepositoryList(repositoryDAO.getByRepositoryIdAndManagerId(repositoryManagerId, repositoryId));
        }
        else if (repositoryManagerId != null) {
          repositories = repositoryDAO.getByRepositoryManagerId(repositoryManagerId);
        }
        else if (repositoryId != null) {
          repositories = convertToRepositoryList(repositoryDAO.getById(repositoryId));
        }
        else {
          repositories = repositoryDAO.getByRepositoryType(RepositoryType.proxy);
        }
        break;
      default:
        throw new IllegalStateException("Invalid owner type: " + ownerType);
    }

    List<Repository> proxyRepositoriesWithReadPermission = filterRepositoriesWithReadPermission(repositories);

    Set<String> repositoryIds =
        proxyRepositoriesWithReadPermission.stream().map(Repository::getId).collect(Collectors.toSet());

    List<RepositoryResultsDetails> detailsList =
        repositoryIds.isEmpty()
            ? Collections.emptyList()
            : repositoryPolicyViolationDAO.getRepositoryResultsDetails(
                repositoryIds, detailsFilter);

    RepositoryResultsDetailsResponseDto result = new RepositoryResultsDetailsResponseDto();

    int iPattern = 1;
    for (RepositoryResultsDetails details : detailsList) {
      if (iPattern <= detailsRequest.pageSize) {
        result.repositoryResultsDetails.add(new RepositoryResultsDetailsDto(details));
      }
      else {
        result.hasNextPage = true;
        break;
      }

      iPattern++;
    }

    // Only execute count query when isBulkWaiverPage is true (performance optimization)
    if (detailsRequest.isBulkWaiverPage) {
      if (repositoryIds.isEmpty()) {
        result.totalCount = 0L;
        result.filterCount = 0L;
      }
      else {
        RepositoryResultsCountSummary filteredCountSummary =
            repositoryPolicyViolationDAO.countRepositoryResultsDetails(repositoryIds, detailsFilter);
        result.filterCount = filteredCountSummary.totalCount;
        if (hasActiveFilters(detailsFilter)) {
          RepositoryResultsCountSummary totalCountSummary =
              repositoryPolicyViolationDAO.countRepositoryResultsDetails(
                  repositoryIds, createUnfilteredBulkWaiverCountFilter(detailsFilter));
          result.totalCount = totalCountSummary.totalCount;
        }
        else {
          result.totalCount = filteredCountSummary.totalCount;
        }
      }
    }

    log.info("Got repository results for {} id {} in {} ms", ownerType, ownerId, System.currentTimeMillis() - start);

    return result;
  }

  private List<Repository> convertToRepositoryList(Repository repository) {
    return repository != null ? Collections.singletonList(repository) : Collections.emptyList();
  }

  RepositoryResultsDetailsFilter validateAndInitializeDetailsFilter(
      OwnerType ownerType,
      RepositoryResultsDetailsRequestDto detailsRequest)
  {
    if (detailsRequest.page <= 0 || detailsRequest.pageSize <= 0) {
      throw new BadRequestException("Page and Page size must be greater than 0");
    }
    if (detailsRequest.isBulkWaiverPage && detailsRequest.pageSize > MAX_BULK_WAIVER_PAGE_SIZE) {
      throw new BadRequestException("Page size cannot exceed " + MAX_BULK_WAIVER_PAGE_SIZE + " for bulk waiver page");
    }

    if (!containsValidSearchFilter(ownerType, detailsRequest)) {
      throw new BadRequestException("SearchFilter is not valid for the ownerType " + ownerType.name() + ".");
    }

    RepositoryResultsDetailsFilter filter = new RepositoryResultsDetailsFilter();
    filter.page = detailsRequest.page;
    filter.pageSize = detailsRequest.pageSize;
    filter.matchStateFilter = initializeMatchStateFilter(detailsRequest.matchStateFilters);
    filter.violationStateFilters = initializeViolationStateFilters(detailsRequest.violationStateFilters);
    filter.threatLevelFilters = detailsRequest.threatLevelFilters;
    filter.excludeThreatLevelZero = detailsRequest.isBulkWaiverPage;
    filter.searchFilters = initializeSearchFilterMap(detailsRequest.searchFilters);
    filter.sortFields = detailsRequest.sortFields;
    filter.aggregate = detailsRequest.aggregate;

    return filter;
  }

  private boolean containsValidSearchFilter(OwnerType ownerType, RepositoryResultsDetailsRequestDto detailsRequest) {
    if (CollectionUtils.isNotEmpty(detailsRequest.searchFilters) && ownerType.equals(OwnerType.REPOSITORY)) {
      return detailsRequest.searchFilters.stream()
          .map(searchFilter -> searchFilter.filterableField.name())
          .noneMatch(field -> field.equals("REPOSITORY_ID") || field.equals("REPOSITORY_MANAGER_ID"));
    }

    if (CollectionUtils.isNotEmpty(detailsRequest.searchFilters) && ownerType.equals(OwnerType.REPOSITORY_MANAGER)) {
      return detailsRequest.searchFilters.stream()
          .map(searchFilter -> searchFilter.filterableField.name())
          .noneMatch(field -> field.equals("REPOSITORY_MANAGER_ID"));
    }

    else {
      return true;
    }
  }

  private String initializeMatchStateFilter(final List<MatchStateFilter> matchStateFilters) {
    if (CollectionUtils.isEmpty(matchStateFilters)) {
      return "";
    }

    if (matchStateFilters.contains(MatchStateFilter.MATCH_STATE_ALL)) {
      return "";
    }

    if (matchStateFilters.size() == 2) {
      // If size is 2, then the filters are MATCH_STATE_EXACT and MATCH_STATE_UNKNOWN,
      // which is the same as MATCH_STATE_ALL
      return "";
    }

    switch (matchStateFilters.get(0)) {
      case MATCH_STATE_EXACT:
        return MatchState.EXACT.getId();
      case MATCH_STATE_UNKNOWN:
        return MatchState.UNKNOWN.getId();
      default:
        throw new BadRequestException("Invalid match state filter");
    }
  }

  private Set<String> initializeViolationStateFilters(final List<ViolationStateFilter> violationStateFilters) {
    if (CollectionUtils.isEmpty(violationStateFilters)
        || violationStateFilters.contains(ViolationStateFilter.VIOLATION_STATE_ALL))
    {
      return ImmutableSet.of(ViolationStateFilter.VIOLATION_STATE_ALL.name());
    }

    return violationStateFilters.stream().map(filter -> filter.name()).collect(Collectors.toSet());
  }

  private Map<String, String> initializeSearchFilterMap(final List<SearchFilter> searchFilters) {
    return CollectionUtils.emptyIfNull(searchFilters)
        .stream()
        .collect(Collectors.toMap(searchFilter -> searchFilter.filterableField.name(),
            searchFilter -> searchFilter.value.toLowerCase()));
  }

  private boolean hasActiveFilters(final RepositoryResultsDetailsFilter filter) {
    return MapUtils.isNotEmpty(filter.searchFilters)
        || !filter.matchStateFilter.isEmpty()
        || hasFilteredViolationState(filter.violationStateFilters)
        || hasThreatLevelFilter(filter.threatLevelFilters);
  }

  private boolean hasFilteredViolationState(final Set<String> violationStateFilters) {
    return CollectionUtils.isNotEmpty(violationStateFilters)
        && !(violationStateFilters.size() == 1
            && violationStateFilters.contains(ViolationStateFilter.VIOLATION_STATE_ALL.name()));
  }

  private boolean hasThreatLevelFilter(final List<Integer> threatLevelFilters) {
    return CollectionUtils.isNotEmpty(threatLevelFilters)
        && threatLevelFilters.size() == 2
        && (threatLevelFilters.get(0) > MIN_THREAT_LEVEL || threatLevelFilters.get(1) < MAX_THREAT_LEVEL);
  }

  private RepositoryResultsDetailsFilter createUnfilteredBulkWaiverCountFilter(
      final RepositoryResultsDetailsFilter sourceFilter)
  {
    RepositoryResultsDetailsFilter filter = new RepositoryResultsDetailsFilter();
    filter.page = sourceFilter.page;
    filter.pageSize = sourceFilter.pageSize;
    filter.matchStateFilter = "";
    filter.violationStateFilters = ImmutableSet.of(ViolationStateFilter.VIOLATION_STATE_ALL.name());
    filter.threatLevelFilters = List.of(MIN_THREAT_LEVEL, MAX_THREAT_LEVEL);
    filter.excludeThreatLevelZero = sourceFilter.excludeThreatLevelZero;
    filter.searchFilters = Collections.emptyMap();
    filter.sortFields = sourceFilter.sortFields;
    filter.aggregate = sourceFilter.aggregate;
    return filter;
  }
}
