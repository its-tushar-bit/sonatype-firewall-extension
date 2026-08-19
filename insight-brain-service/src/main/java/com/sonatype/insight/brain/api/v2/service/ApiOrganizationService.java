/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.ApiOrganizationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiOrganizationListDTO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.organization.OrganizationService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzFilter;
import com.sonatype.insight.error.exception.BadRequestException;
import org.apache.commons.collections4.CollectionUtils;

/**
 * @since 1.11.0
 */
@Named
public class ApiOrganizationService
{
  private final OrganizationDAO organizationDAO;

  private final TagDAO tagDAO;

  private final OrganizationService organizationService;

  @Inject
  public ApiOrganizationService(
      OrganizationDAO organizationDAO,
      final TagDAO tagDAO,
      final OrganizationService organizationService)
  {
    this.organizationDAO = organizationDAO;
    this.tagDAO = tagDAO;
    this.organizationService = organizationService;
  }

  public ApiOrganizationListDTO getOrganizations(Set<String> orgNames) {
    List<Organization> organizations = orgNames.isEmpty()
        ? organizationService.getAllWithoutRelatedRepositories()
        : getOrganizationsByNames(orgNames);

    Set<String> orgIds = organizations.stream()
        .map(Organization::getId)
        .collect(Collectors.toSet());

    Map<String, List<Tag>> orgTagMap = tagDAO.getByOrganizationIds(orgIds)
        .stream()
        .collect(Collectors.groupingBy(Tag::getOrganizationId));

    return ApiOrganizationAdapter.convert(organizations, orgTagMap);
  }

  /**
   * Retrieves organizations by their internal IDs.
   *
   * @param ids the set of internal organization IDs
   * @return list of organizations without tags
   * @since 1.201
   */
  public ApiOrganizationListDTO getOrganizationsByIds(Set<String> ids) {
    if (CollectionUtils.isEmpty(ids)) {
      return new ApiOrganizationListDTO();
    }

    List<Organization> organizations = getOrganizationsByIdsFiltered(ids);
    return ApiOrganizationAdapter.convert(organizations, Collections.emptyMap());
  }

  /**
   * Helper method to retrieve and filter organizations by internal IDs based on user permissions.
   *
   * @param ids the set of internal organization IDs
   * @return filtered list of organizations the user has READ permission for
   */
  @AuthzFilter(permission = Permission.READ, context = AuthzFilter.Context.ORGANIZATION)
  List<Organization> getOrganizationsByIdsFiltered(Set<String> ids) {
    List<Organization> organizations = new ArrayList<>();
    for (String id : ids) {
      Optional.ofNullable(organizationDAO.getById(id))
          .ifPresent(organizations::add);
    }
    return organizations;
  }

  @Authorize(permission = Permission.READ)
  public ApiOrganizationDTO getOrganizationById(@AuthzContext(AuthzContext.Key.ORGANIZATION_ID) String organizationId) {
    Organization organization = organizationDAO.getByIdNotNull(organizationId);
    List<Tag> tags = tagDAO.getByOrganizationId(organizationId);
    return ApiOrganizationAdapter.convert(organization, tags);
  }

  @AuthzFilter(permission = Permission.READ, context = AuthzFilter.Context.ORGANIZATION)
  List<Organization> getOrganizationsByNames(Set<String> orgNames) {
    return organizationDAO.getByNamesAndWithoutRelatedRepositories(orgNames);
  }

  /**
   * @since 1.42
   */
  public ApiOrganizationDTO addOrganization(ApiOrganizationDTO apiOrganizationDTO) {
    if (apiOrganizationDTO.id != null) {
      throw new BadRequestException("Organization must not have an ID set on creation.");
    }
    if (apiOrganizationDTO.tags != null) {
      throw new BadRequestException("Organization must not have tags set on creation.");
    }

    Organization apiOrganization = new Organization(apiOrganizationDTO.name);
    apiOrganization.setParentOrganizationId(apiOrganizationDTO.parentOrganizationId);
    Organization newOrganization = organizationService.addOrganization(apiOrganization);

    return ApiOrganizationAdapter.convert(newOrganization, Collections.emptyList());
  }

  public void deleteOrganization(String organizationId) throws IOException {
    organizationService.deleteOrganization(organizationId);
  }

}
