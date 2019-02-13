/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.ApiOrganizationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiOrganizationListDTO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.organization.OrganizationService;
import com.sonatype.insight.error.exception.BadRequestException;

/**
 * @since 1.11.0
 */
@Named
public class ApiOrganizationService
{
  private final TagDAO tagDAO;

  private final OrganizationService organizationService;

  private final ApiOrganizationAdapter apiOrganizationAdapter;

  @Inject
  public ApiOrganizationService(final TagDAO tagDAO,
                                final OrganizationService organizationService,
                                final ApiOrganizationAdapter apiOrganizationAdapter)
  {
    this.tagDAO = tagDAO;
    this.organizationService = organizationService;
    this.apiOrganizationAdapter = apiOrganizationAdapter;
  }

  public ApiOrganizationListDTO getAll() {
    Map<String, List<Tag>> orgTagMap = new HashMap<>();
    List<Organization> organizations = organizationService.getAll();
    for (Organization organization : organizations) {
      List<Tag> tags = tagDAO.getByOrganizationId(organization.getId());
      orgTagMap.put(organization.getId(), tags);
    }

    return apiOrganizationAdapter.convert(organizations, orgTagMap);
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
    Organization newOrganization = organizationService.addOrganization(apiOrganization);

    return apiOrganizationAdapter.convert(newOrganization, Collections.emptyList());
  }
}
