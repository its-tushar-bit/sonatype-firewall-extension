/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.api.v2.dto.RepositoryResultsForImageContainerRequestDto.SearchFilter;
import com.sonatype.insight.brain.api.v2.dto.RepositoryResultsForImageContainerRequestDto.ViolationStateFilter;
import com.sonatype.insight.brain.api.v2.dto.RepositoryResultsForImageContainerDto;
import com.sonatype.insight.brain.api.v2.dto.RepositoryResultsForImageContainerRequestDto;
import com.sonatype.insight.brain.api.v2.dto.RepositoryResultsForImageContainerResponseDto;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsForImageContainer;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsForImageContainerFilter;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.AuthzFilter;
import com.sonatype.insight.brain.security.AuthzFilter.Context;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ApiRepositoryResultsForImageContainerService
{
  private static final Logger log = LoggerFactory.getLogger(ApiRepositoryResultsForImageContainerService.class);

  private final RepositoryDAO repositoryDAO;

  private final ApplicationDAO applicationDAO;

  private final PolicyViolationDAO policyViolationDAO;

  private final RepositoryManagerDAO repositoryManagerDAO;

  @Inject
  public ApiRepositoryResultsForImageContainerService(
      final RepositoryDAO repositoryDAO,
      final ApplicationDAO applicationDAO,
      final PolicyViolationDAO policyViolationDAO,
      final RepositoryManagerDAO repositoryManagerDAO)
  {
    this.repositoryDAO = repositoryDAO;
    this.applicationDAO = applicationDAO;
    this.policyViolationDAO = policyViolationDAO;
    this.repositoryManagerDAO = repositoryManagerDAO;
  }

  RepositoryResultsForImageContainerResponseDto getDetails(
      OwnerType ownerType,
      String ownerId,
      final RepositoryResultsForImageContainerRequestDto detailsRequest)
  {
    long start = System.currentTimeMillis();

    log.info("Getting repository results for {} id {}", ownerType, ownerId);

    if (detailsRequest == null) {
      throw new BadRequestException("Missing request parameters");
    }

    RepositoryResultsForImageContainerFilter detailsFilter = validateAndInitializeDetailsFilter(detailsRequest);

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
        proxyRepositoriesWithReadPermission.stream()
            .map(Repository::getId)
            .collect(Collectors.toSet());

    Set<String> applicationIds = proxyRepositoriesWithReadPermission.stream()
        .map(Repository::getRelatedOrganizationId) // Extract relatedOrganizationId
        .filter(Objects::nonNull) // Filter out null values
        .flatMap(orgId -> applicationDAO.getByOrganizationId(orgId).stream()) // Fetch applications by organizationId
        .map(Application::getId) // Extract applicationId
        .collect(Collectors.toSet()); // Collect into a Set

    List<RepositoryResultsForImageContainer> detailsList =
        repositoryIds.isEmpty()
            ? Collections.emptyList()
            : policyViolationDAO.getRepositoryResultsForImageContainer(
                repositoryIds, applicationIds, detailsFilter);

    RepositoryResultsForImageContainerResponseDto result = new RepositoryResultsForImageContainerResponseDto();

    int iPattern = 1;
    for (RepositoryResultsForImageContainer details : detailsList) {
      if (iPattern <= detailsRequest.pageSize) {
        result.repositoryResultsDetails.add(new RepositoryResultsForImageContainerDto(details));
      }
      else {
        result.hasNextPage = true;
        break;
      }

      iPattern++;
    }

    log.info("Got repository results for {} id {} in {} ms", ownerType, ownerId, System.currentTimeMillis() - start);

    return result;
  }

  @AuthzFilter(permission = Permission.READ, context = Context.REPOSITORY)
  List<Repository> filterRepositoriesWithReadPermission(List<Repository> repositories) {
    return repositories;
  }

  private List<Repository> convertToRepositoryList(Repository repository) {
    return repository != null ? Collections.singletonList(repository) : Collections.emptyList();
  }

  RepositoryResultsForImageContainerFilter validateAndInitializeDetailsFilter(
      RepositoryResultsForImageContainerRequestDto detailsRequest)
  {
    if (detailsRequest.page <= 0 || detailsRequest.pageSize <= 0) {
      throw new BadRequestException("Page and Page size must be greater than 0");
    }

    RepositoryResultsForImageContainerFilter filter = new RepositoryResultsForImageContainerFilter();
    filter.page = detailsRequest.page;
    filter.pageSize = detailsRequest.pageSize;
    filter.violationStateFilters = initializeViolationStateFilters(detailsRequest.violationStateFilters);
    filter.threatLevelFilters = detailsRequest.threatLevelFilters;
    filter.searchFilters = initializeSearchFilterMap(detailsRequest.searchFilters);
    filter.sortFields = detailsRequest.sortFields;
    filter.aggregate = detailsRequest.aggregate;

    return filter;
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
}
