/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyApplicationDTO;
import com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyEntityDTO;
import com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyOrganizationDTO;

import static com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyApplicationDTO.transformToApplicationDTO;
import static com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyOrganizationDTO.transformToOrganizationDTO;
import static com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyOrganizationDTO.transformToSyntheticOrganizationDTO;

public class OwnerHierarchy
{
  private String topParentOrganizationId;

  private Map<String, OwnerHierarchyOrganizationDTO> organizationMap;

  private Map<String, OwnerHierarchyOrganizationDTO> syntheticOrganizationMap;

  private Map<String, OwnerHierarchyApplicationDTO> applicationMap;

  private OrganizationDAO organizationDAO;

  public OwnerHierarchy(
      List<Organization> orgs,
      List<Application> apps)
  {
    this.organizationDAO = new OrganizationDAO();
    this.syntheticOrganizationMap = new HashMap<>();
    this.applicationMap = new HashMap<>();
    initSidebarOrganizationMap(orgs);
    addAppsToParentOrgs(apps);
    createOrganizationHierarchy();
    removeUnauthorizedAncestor();
  }

  public OwnerHierarchyOrganizationDTO root() {
    return getOrganizationById(topParentOrganizationId);
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

  public Map<String, OwnerHierarchyEntityDTO> asHashMap() {
    Map<String, OwnerHierarchyEntityDTO> owners = new HashMap<>();
    owners.putAll(applicationMap);
    owners.putAll(organizationMap);
    owners.putAll(syntheticOrganizationMap);
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

  private void addChildOrgToParentOrganizationIfAbsent(
      final OwnerHierarchyOrganizationDTO childOrganization,
      final OwnerHierarchyOrganizationDTO parentOrganization)
  {
    String childOrganizationId = childOrganization.id;
    boolean organizationExistsInParent = parentOrganization.organizationIds.stream()
        .anyMatch(id -> id.equals(childOrganizationId));

    if (!organizationExistsInParent) {
      parentOrganization.organizationIds.add(childOrganizationId);
    }
  }

  private void forEachOrgSorted(Consumer<OwnerHierarchyOrganizationDTO> consumer) {
    List<OwnerHierarchyOrganizationDTO> organizations = new ArrayList<>(this.organizationMap.values());
    Collections.sort(organizations, Comparator.comparing(o -> o.name, String.CASE_INSENSITIVE_ORDER));
    organizations.forEach(consumer);
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

  private void initSidebarOrganizationMap(List<Organization> organizations) {
    this.organizationMap = organizations.stream()
        .map(transformToOrganizationDTO)
        .collect(Collectors.toMap(
            organizationDTO -> organizationDTO.id,
            Function.identity()));
  }

  private void addAppsToParentOrgs(List<Application> apps) {
    apps.forEach(app -> {
      OwnerHierarchyOrganizationDTO parentOrganization = getOrComputeApplicationHolderIfAbsent(app.getOrganizationId());
      parentOrganization.applicationIds.add(app.getPublicId());
      applicationMap.put(app.getPublicId(), transformToApplicationDTO.apply(app));
    });
  }

  private void createOrganizationHierarchy() {
    this.forEachOrgSorted(currentOrganizationDTO -> {
      String parentOrganizationId = currentOrganizationDTO.parentOrganizationId;
      if (parentOrganizationId == null) {
        return;
      }

      OwnerHierarchyOrganizationDTO parentOrganization = getOrComputeIfAbsent(parentOrganizationId);
      addChildOrgToParentOrganizationIfAbsent(currentOrganizationDTO, parentOrganization);

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
        addChildOrgToParentOrganizationIfAbsent(currentOrganizationDTO, parentOrganizationDTO);
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

    return findLCA(targetOrganizationIds);
  }

  // returns the lowest common ancestor of N number of targets
  private OwnerHierarchyOrganizationDTO findLCA(HashSet<String> targetIds) {
    ArrayList<OwnerHierarchyOrganizationDTO> ancestors = new ArrayList<>();

    int initialMatchCount = 0;
    getMatchCount(
        getOrganizationById(Organization.ROOT_ORGANIZATION_ID),
        initialMatchCount,
        targetIds,
        ancestors
    );

    // the first organization to contain all occurrences of matching ids will be inserted first
    // following ancestors can be ignored as they're added during the process of backtracking to the root.
    return ancestors.get(0);
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
