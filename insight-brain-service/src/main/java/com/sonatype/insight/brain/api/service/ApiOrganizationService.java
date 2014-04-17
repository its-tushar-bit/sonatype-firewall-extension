/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.api.dto.ApiOrganizationListDTO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.organization.OrganizationService;

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
  public ApiOrganizationService(final TagDAO tagDAO, final OrganizationService organizationService,
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
}
