/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyApplicationDTO;
import com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyEntityDTO;
import com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyOrganizationDTO;
import com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyRepositoryContainerDTO;
import com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyRepositoryDTO;
import com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyRepositoryManagerDTO;

import static com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyApplicationDTO.transformToApplicationDTO;
import static com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyOrganizationDTO.transformToOrganizationDTO;
import static com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyOrganizationDTO.transformToSyntheticOrganizationDTO;
import static com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyOrganizationDTO.transformToSyntheticRepositoryManagerDTO;
import static com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyRepositoryDTO.transformToRepositoryDTO;
import static com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyRepositoryManagerDTO.transformToRepositoryManagerDTO;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public class OwnerHierarchy
{
  private String topParentOrganizationId;

  private Map<String, OwnerHierarchyOrganizationDTO> organizationMap;

  private Map<String, OwnerHierarchyOrganizationDTO> syntheticOrganizationMap;

  private Map<String, OwnerHierarchyApplicationDTO> applicationMap;

  private Map<String, OwnerHierarchyRepositoryManagerDTO> repositoryManagerMap;

  private Map<String, OwnerHierarchyRepositoryDTO> repositoryMap;

  private OwnerHierarchyRepositoryContainerDTO repositoryContainer;

  private OrganizationDAO organizationDAO;

  private RepositoryManagerDAO repositoryManagerDAO;

  public OwnerHierarchy(
      List<Organization> orgs,
      List<Application> apps,
      List<RepositoryManager> repositoryManagers,
      List<Repository> repositories,
      OrganizationDAO organizationDAO,
      RepositoryManagerDAO repositoryManagerDAO)
  {
    this.organizationDAO = organizationDAO;
    this.repositoryManagerDAO = repositoryManagerDAO;
    this.syntheticOrganizationMap = new HashMap<>();
    this.applicationMap = new HashMap<>();
    this.repositoryManagerMap = new HashMap<>();
    this.repositoryMap = new HashMap<>();

    initSidebarOrganizationMap(orgs);
    addAppsToParentOrgs(apps);
    repositoryContainer = new OwnerHierarchyRepositoryContainerDTO();
    addRepositories(repositoryManagers, repositories);
    createOrganizationHierarchy();
    removeUnauthorizedAncestor();
  }

  private <T extends Owner> List<T> sortOwners(List<T> owners) {
    List<T> result = new ArrayList<>(owners);
    result.sort(Comparator.comparing(Owner::getName, String.CASE_INSENSITIVE_ORDER));
    return result;
  }

  public OwnerHierarchyOrganizationDTO root() {
    OwnerHierarchyOrganizationDTO root = getOrganizationById(topParentOrganizationId);
    if (root == null) {
      topParentOrganizationId = Organization.ROOT_ORGANIZATION_ID;
      root = getSyntheticOrganization(Organization.ROOT_ORGANIZATION_ID);

      boolean hasNoPermissions = repositoryManagerMap.isEmpty() && organizationMap.isEmpty();
      root.repositoryContainerId = hasNoPermissions ? root.repositoryContainerId : repositoryContainer.id;
    }
    else {
      root.repositoryContainerId = repositoryContainer.id;
    }
    return root;
  }

  public boolean contains(String organizationId) {
    return organizationMap.containsKey(organizationId)
        || syntheticOrganizationMap.containsKey(organizationId);
  }

  public OwnerHierarchyOrganizationDTO add(OwnerHierarchyOrganizationDTO organization) {
    if (organization == null) {
      return null;
    }
    if (organization.synthetic) {
      syntheticOrganizationMap.put(organization.id, organization);
    }
    else {
      organizationMap.put(organization.id, organization);
    }
    return organization;
  }

  public OwnerHierarchyOrganizationDTO remove(String organizationId) {
    if (organizationId == null) {
      return null;
    }

    return organizationMap.containsKey(organizationId)
        ? organizationMap.remove(organizationId)
        : syntheticOrganizationMap.remove(organizationId);
  }

  public OwnerHierarchyOrganizationDTO getOrganizationById(String organizationId) {
    return organizationMap.containsKey(organizationId)
        ? organizationMap.get(organizationId)
        : syntheticOrganizationMap.get(organizationId);
  }

  // Visible for testing
  OwnerHierarchyRepositoryContainerDTO getRepositoryContainer() {
    return repositoryContainer;
  }

  // Visible for testing
  OwnerHierarchyRepositoryManagerDTO getRepositoryManagerById(String repositoryManagerId) {
    return repositoryManagerMap.get(repositoryManagerId);
  }

  // Visible for testing
  OwnerHierarchyRepositoryDTO getRepositoryById(final String id) {
    return repositoryMap.get(id);
  }

  public Map<String, OwnerHierarchyEntityDTO> asHashMap() {
    Map<String, OwnerHierarchyEntityDTO> owners = new HashMap<>();
    owners.putAll(applicationMap);
    owners.putAll(organizationMap);
    owners.putAll(syntheticOrganizationMap);
    owners.putAll(repositoryManagerMap);
    owners.putAll(repositoryMap);

    if (!owners.isEmpty()) {
      owners.put(RepositoryContainer.REPOSITORY_CONTAINER_ID, repositoryContainer);
    }
    return owners;
  }

  private void removeParentsFor(String organizationId) {
    OwnerHierarchyOrganizationDTO organization = getOrganizationById(organizationId);
    if (organization != null) {
      OwnerHierarchyOrganizationDTO parentOrganization = getOrganizationById(organization.parentOrganizationId);

      while (parentOrganization != null) {
        remove(parentOrganization.id);
        parentOrganization = getOrganizationById(parentOrganization.parentOrganizationId);
      }
    }
  }

  // Get or Create Synthetic Organization By Id
  private OwnerHierarchyOrganizationDTO getOrComputeIfAbsent(String organizationId) {
    if (this.contains(organizationId)) {
      return getOrganizationById(organizationId);
    }

    return syntheticOrganizationMap.computeIfAbsent(organizationId, this::getSyntheticOrganization);
  }

  private void addChildToParentIfAbsent(final OwnerHierarchyEntityDTO child, final OwnerHierarchyEntityDTO parent) {
    if (!parent.hasChild(child.id)) {
      parent.addChild(child);
    }
  }

  private HashSet<String> filterOrganizationIdsBy(Predicate<OwnerHierarchyOrganizationDTO> predicate) {
    HashSet<String> organizationIds = new HashSet<>();
    Queue<String> organizationsToSearch = new LinkedList<>();
    organizationsToSearch.add(Organization.ROOT_ORGANIZATION_ID);

    while (!organizationsToSearch.isEmpty()) {
      OwnerHierarchyOrganizationDTO currentOrganization = getOrganizationById(organizationsToSearch.poll());

      boolean matchesPredicate = predicate.test(currentOrganization);
      if (matchesPredicate) {
        organizationIds.add(currentOrganization.id);
        continue;
      }

      organizationsToSearch.addAll(currentOrganization.organizationIds);
    }

    return organizationIds;
  }

  private OwnerHierarchyOrganizationDTO getSyntheticOrganization(String organizationId) {
    Organization organization = organizationDAO.getByIdNotNull(organizationId);
    return transformToSyntheticOrganizationDTO.apply(organization);
  }

  // Creates a synthetic parent organization that will be a holder for at least one of the provided applications.
  private OwnerHierarchyOrganizationDTO getOrComputeApplicationHolderIfAbsent(String organizationId) {
    if (this.contains(organizationId)) {
      return getOrganizationById(organizationId);
    }

    return organizationMap.computeIfAbsent(organizationId, this::getSyntheticOrganization);
  }

  private OwnerHierarchyRepositoryManagerDTO getRepositoryManager(String repositoryManagerId) {
    return repositoryManagerMap.get(repositoryManagerId);
  }

  private void initSidebarOrganizationMap(final List<Organization> organizations) {
    organizationMap = initializeEntityMap(organizations, transformToOrganizationDTO);
  }

  private <T, R extends OwnerHierarchyEntityDTO> Map<String, R> initializeEntityMap(
      final List<T> entities,
      final Function<T, R> transform)
  {
    return entities.stream()
        .map(transform)
        .collect(toMap(entityDto -> entityDto.id, identity()));
  }

  private void addAppsToParentOrgs(List<Application> apps) {
    apps = sortOwners(apps);
    convertToMapAndUpdateParents(apps, transformToApplicationDTO, this::getOrComputeApplicationHolderIfAbsent)
        .forEach(application -> applicationMap.put(application.publicId, application));
  }

  private void addRepositories(List<RepositoryManager> repositoryManagers, List<Repository> repositories) {
    List<String> repositoryManagersIds = repositoryManagers.stream()
        .map(RepositoryManager::getId)
        .collect(Collectors.toList());
    for (Repository repository : repositories) {
      // If user has permission over repo manager, continue
      if (repositoryManagersIds.contains(repository.getRepositoryManagerId())) {
        continue;
      }

      // If user permission is granted per repo, add the missing repo managers as synthetic repo managers
      if (!repositoryManagerMap.containsKey(repository.getRepositoryManagerId())) {
        RepositoryManager repositoryManager = repositoryManagerDAO.getByIdNotNull(repository.getRepositoryManagerId());
        OwnerHierarchyRepositoryManagerDTO syntheticDto =
            transformToSyntheticRepositoryManagerDTO.apply(repositoryManager);
        repositoryManagerMap.put(repository.getRepositoryManagerId(), syntheticDto);
        this.repositoryContainer.addChild(syntheticDto);
      }
    }

    addRepositoryManagersToParent(repositoryManagers);
    addRepositoriesToParent(repositories);
  }

  private void addRepositoriesToParent(List<Repository> repositories) {
    repositories = sortOwners(repositories);
    convertToMapAndUpdateParents(repositories, transformToRepositoryDTO, this::getRepositoryManager)
        .forEach(repository -> repositoryMap.put(repository.id, repository));
  }

  private void addRepositoryManagersToParent(List<RepositoryManager> repositoryManagers) {
    repositoryManagers = sortOwners(repositoryManagers);
    convertToMapAndUpdateParents(repositoryManagers, transformToRepositoryManagerDTO,
        repoManager -> getRepositoryContainer())
            .forEach(repositoryManager -> repositoryManagerMap.put(repositoryManager.id, repositoryManager));
  }

  private <T, R extends OwnerHierarchyEntityDTO> Stream<R> convertToMapAndUpdateParents(
      final List<T> entities,
      final Function<T, R> transform,
      final Function<String, OwnerHierarchyEntityDTO> getParent)
  {
    return entities.stream()
        .map(transform)
        .peek(addEntityToItsParent(getParent));
  }

  private static <R extends OwnerHierarchyEntityDTO> Consumer<R> addEntityToItsParent(
      final Function<String, OwnerHierarchyEntityDTO> getParent)
  {
    return entity -> getParent.andThen(Optional::ofNullable)
        .apply(entity.getParentId())
        .ifPresent(parentEntity -> parentEntity.addChild(entity));
  }

  private void createOrganizationHierarchy() {
    List<OwnerHierarchyOrganizationDTO> organizations = new ArrayList<>(organizationMap.values());
    organizations.sort(Comparator.comparing(o -> o.name, String.CASE_INSENSITIVE_ORDER));
    organizations.forEach(organizationDTO -> {
      String parentOrganizationId = organizationDTO.parentOrganizationId;
      if (parentOrganizationId == null) {
        return;
      }

      OwnerHierarchyOrganizationDTO parentOrganization = getOrComputeIfAbsent(parentOrganizationId);
      addChildToParentIfAbsent(organizationDTO, parentOrganization);

      if (parentOrganization.synthetic) {
        createMissingAncestors(parentOrganization);
      }
    });
  }

  // creates synthetic ancestors all the way up to ROOT_ORGANIZATION
  private void createMissingAncestors(final OwnerHierarchyOrganizationDTO startingOrganizationDTO) {
    OwnerHierarchyOrganizationDTO currentOrganizationDTO = startingOrganizationDTO;
    String currentOrgDTOParentOrganizationId;

    while (currentOrganizationDTO.parentOrganizationId != null) {
      currentOrgDTOParentOrganizationId = currentOrganizationDTO.parentOrganizationId;
      OwnerHierarchyOrganizationDTO parentOrganizationDTO = getOrganizationById(currentOrgDTOParentOrganizationId);

      if (parentOrganizationDTO != null) {
        addChildToParentIfAbsent(currentOrganizationDTO, parentOrganizationDTO);
        break;
      }

      OwnerHierarchyOrganizationDTO syntheticParentOrganization =
          getSyntheticOrganization(currentOrgDTOParentOrganizationId);
      add(syntheticParentOrganization);
      syntheticParentOrganization.organizationIds.add(currentOrganizationDTO.id);

      currentOrganizationDTO = syntheticParentOrganization;
    }
  }

  private void removeUnauthorizedAncestor() {
    OwnerHierarchyOrganizationDTO lowestCommonAncestor = getLowestCommonAncestor();
    if (lowestCommonAncestor != null) {
      topParentOrganizationId = lowestCommonAncestor.id;
      removeParentsFor(lowestCommonAncestor.id);
    }
  }

  private OwnerHierarchyOrganizationDTO getLowestCommonAncestor() {
    OwnerHierarchyOrganizationDTO rootOrganization = getOrganizationById(Organization.ROOT_ORGANIZATION_ID);
    if (rootOrganization == null) {
      return null;
    }

    boolean hasFullPermission = !rootOrganization.synthetic;
    if (hasFullPermission) {
      return rootOrganization;
    }

    // Use the lowest common ancestor organization as the root
    Predicate<OwnerHierarchyOrganizationDTO> predicate =
        org -> !org.synthetic || (org.applicationIds != null && !org.applicationIds.isEmpty());
    HashSet<String> targetOrganizationIds = filterOrganizationIdsBy(predicate);

    OwnerHierarchyOrganizationDTO lowestCommonAncestor = findLCA(targetOrganizationIds);
    return lowestCommonAncestor != null ? lowestCommonAncestor : rootOrganization;
  }

  // returns the lowest common ancestor of N number of targets
  private OwnerHierarchyOrganizationDTO findLCA(HashSet<String> targetIds) {
    ArrayList<OwnerHierarchyOrganizationDTO> ancestors = new ArrayList<>();

    int initialMatchCount = 0;
    getMatchCount(
        getOrganizationById(Organization.ROOT_ORGANIZATION_ID),
        initialMatchCount,
        targetIds,
        ancestors);

    // the first organization to contain all occurrences of matching ids will be inserted first
    // following ancestors can be ignored as they're added during the process of backtracking to the root.
    return ancestors.isEmpty() ? null : ancestors.get(0);
  }

  private int getMatchCount(
      OwnerHierarchyOrganizationDTO root,
      int matchCount,
      HashSet<String> targetIds,
      ArrayList<OwnerHierarchyOrganizationDTO> ancestors)
  {
    if (targetIds.contains(root.id)) {
      matchCount++;
      if (matchCount == targetIds.size()) {
        ancestors.add(root);
      }
      return matchCount;
    }

    int matchCountInChildren = 0;
    for (String organizationId : root.organizationIds) {
      OwnerHierarchyOrganizationDTO organization = getOrganizationById(organizationId);
      matchCountInChildren += getMatchCount(organization, matchCount, targetIds, ancestors);
    }
    matchCount += matchCountInChildren;

    if (matchCount == targetIds.size()) {
      ancestors.add(root);
    }
    return matchCount;
  }
}
