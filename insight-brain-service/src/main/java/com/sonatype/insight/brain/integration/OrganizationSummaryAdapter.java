/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.sonatype.clm.dto.model.organization.OrganizationSummary;
import com.sonatype.clm.dto.model.organization.OrganizationSummaryList;
import com.sonatype.insight.brain.model.Organization;

/**
 * Adapter class to translate between Organization entity objects and OrganizationSummary objects
 *
 * @since 1.144.0
 */
class OrganizationSummaryAdapter
{
  static OrganizationSummaryList convert(Collection<Organization> organizations) {
    OrganizationSummaryList organizationSummaryList = new OrganizationSummaryList();
    List<OrganizationSummary> applicationSummaries = new ArrayList<>();
    organizationSummaryList.setOrganizationSummaries(applicationSummaries);

    if (organizations != null) {
      for (Organization organization : organizations) {
        OrganizationSummary organizationSummary = convert(organization);
        applicationSummaries.add(organizationSummary);
      }
    }

    return organizationSummaryList;
  }

  static OrganizationSummary convert(Organization organization) {
    if (organization == null) {
      return null;
    }
    OrganizationSummary organizationDTO = new OrganizationSummary();
    organizationDTO.setId(organization.getId());
    organizationDTO.setName(organization.getName());
    return organizationDTO;
  }
}
