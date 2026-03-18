/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.owner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dto.OwnerHierarchyDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.organization.OrganizationService;
import com.sonatype.insight.brain.repository.RepositoryService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.utils.IdUtils;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.collections.CollectionUtils;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;

/**
 * @since 1.105
 */
@Named
public class OwnerService
{
  private final OwnerDAO ownerDAO;

  private final IdUtils idUtils;

  private final OrganizationDAO organizationDAO;

  private final ApplicationService applicationService;

  private final OrganizationService organizationService;

  private final RepositoryService repositoryService;

  private final Collector<Owner, ?, Map<String, Owner>> ownerCollector =
      Collectors.toMap(Owner::getId, Function.identity(), (existing, replacement) -> existing);

  @Inject
  public OwnerService(
      OwnerDAO ownerDAO,
      final IdUtils idUtils,
      final OrganizationDAO organizationDAO,
      final ApplicationService applicationService,
      final OrganizationService organizationService,
      final RepositoryService repositoryService)
  {
    this.ownerDAO = ownerDAO;
    this.idUtils = idUtils;
    this.organizationDAO = organizationDAO;
    this.applicationService = applicationService;
    this.organizationService = organizationService;
    this.repositoryService = repositoryService;
  }

  @Authorize(permission = Permission.READ)
  public OwnerHierarchyDTO getHierarchyForRead(
      @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) String ownerId)
  {
    return getHierarchyNoAuth(ownerType, ownerId);
  }

  @Authorize(permission = Permission.LEGAL_REVIEWER)
  public OwnerHierarchyDTO getHierarchyForLegalReviewer(
      @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) String ownerId)
  {
    return getHierarchyNoAuth(ownerType, ownerId);
  }

  @VisibleForTesting
  OwnerHierarchyDTO getHierarchyNoAuth(OwnerType ownerType, String ownerId) {
    Owner currentOwner = idUtils.getOwnerNotNull(ownerType, ownerId);
    OwnerHierarchyDTO currentHierarchy = null;
    for (Owner owner : ownerDAO.walkHierarchy(currentOwner)) {
      OwnerHierarchyDTO hierarchy =
          new OwnerHierarchyDTO(owner.getId(), owner.getPublicId(), owner.getName(), owner.getType(), null);
      if (currentHierarchy != null) {
        hierarchy.setChildren(new ArrayList<>());
        hierarchy.getChildren().add(currentHierarchy);
      }
      currentHierarchy = hierarchy;
    }
    return currentHierarchy;
  }

  public Map<String, Owner> getOwnersWithReadPermissionsById() {
    return getOwnersWithReadPermissionsById(Collections.emptySet(), Collections.emptySet(), Collections.emptySet(),
        Collections.emptySet());
  }

  @Authorize(permission = Permission.READ)
  public OwnerDTO getOwnerByTypeAndInternalId(
      @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.INTERNAL_ID) String ownerInternalId)
  {
    return new OwnerDTO(idUtils.getOwnerNotNull(ownerType, ownerInternalId));
  }

  private Map<String, Owner> getOwnersWithReadPermissionsById(
      final Set<String> organizationIds,
      final Set<String> applicationIds,
      final Set<String> tagIds,
      final Set<String> repositoryIds)
  {
    Map<String, Owner> owners = new HashMap<>();
    BooleanSupplier isOwnerFilterEmpty = () -> CollectionUtils.isEmpty(organizationIds)
        && CollectionUtils.isEmpty(applicationIds)
        && CollectionUtils.isEmpty(tagIds)
        && CollectionUtils.isEmpty(repositoryIds);
    Predicate<List<Repository>> reposAreNotEmptyOrIsOnlyRepoContainer =
        repos -> !repos.isEmpty() || (CollectionUtils.isNotEmpty(repositoryIds)
            && repositoryIds.contains(RepositoryContainer.REPOSITORY_CONTAINER_ID));
    BooleanSupplier filtersAreEmptyAndRepoContainerReadPermission = () -> isOwnerFilterEmpty.getAsBoolean()
        && repositoryService.checkReadPermissionRepositoryContainer();
    Predicate<List<Repository>> shouldAddRepoContainer = repos -> reposAreNotEmptyOrIsOnlyRepoContainer.test(repos)
        || filtersAreEmptyAndRepoContainerReadPermission.getAsBoolean();

    List<Application> applications = getApplications(applicationIds, tagIds, isOwnerFilterEmpty);
    Map<String, Organization> appsParentOrgs = organizationService.getAllParentOrgsNoAuthz(applications);
    List<Organization> organizations = getOrganizations(organizationIds, isOwnerFilterEmpty);
    Map<String, Organization> orgsParentOrgs = organizationService
        .getAllParentOrgsNoAuthz(organizations, appsParentOrgs);
    List<Repository> repositories = getRepositories(repositoryIds, isOwnerFilterEmpty);

    owners.putAll(applications.stream().collect(ownerCollector));
    owners.putAll(appsParentOrgs);
    owners.putAll(organizations.stream().collect(ownerCollector));
    owners.putAll(orgsParentOrgs);
    owners.putAll(repositories.stream().collect(ownerCollector));

    if (shouldAddRepoContainer.test(repositories)) {
      owners.put(RepositoryContainer.REPOSITORY_CONTAINER_ID, RepositoryContainer.SINGLETON);
      owners.computeIfAbsent(ROOT_ORGANIZATION_ID, organizationDAO::getById);
    }

    return owners;
  }

  private List<Application> getApplications(
      Set<String> applicationIds,
      Set<String> tagIds,
      BooleanSupplier isOwnerFilterEmpty)
  {
    List<Application> applications = Collections.emptyList();
    if (isOwnerFilterEmpty.getAsBoolean()
        || (CollectionUtils.isNotEmpty(applicationIds)
            || CollectionUtils.isNotEmpty(tagIds)))
    {
      applications = applicationService.getOwnerApplicationsByIdsOrTagIds(applicationIds, tagIds);
    }
    return applications;
  }

  private List<Organization> getOrganizations(
      Set<String> organizationIds,
      BooleanSupplier isOwnerFilterEmpty)
  {
    List<Organization> allOrgs;
    List<Organization> organizations = Collections.emptyList();
    if (isOwnerFilterEmpty.getAsBoolean()) {
      organizations = organizationService.getAll();
    }
    else if (CollectionUtils.isNotEmpty(organizationIds)) {
      allOrgs = organizationService.getAll();
      organizations = allOrgs.stream()
          .filter(organization -> organizationIds.contains(organization.getId()))
          .collect(Collectors.toList());
    }
    return organizations;
  }

  private List<Repository> getRepositories(
      Set<String> repositoryIds,
      BooleanSupplier isOwnerFilterEmpty)
  {
    if (isOwnerFilterEmpty.getAsBoolean() || CollectionUtils.isNotEmpty(repositoryIds)) {
      return repositoryService.getRepositoriesWithReadPermissionByIds(repositoryIds);
    }
    return Collections.emptyList();
  }
}
