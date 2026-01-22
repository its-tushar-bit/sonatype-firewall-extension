/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.organization.OrganizationSummaryList;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.AuthzFilter;

@Named
public class OrganizationSummaryService
{
  private static final Comparator<Organization> ORG_NAME_COMPARATOR = new Comparator<>()
  {
    @Override
    public int compare(Organization org1, Organization org2) {
      return org1.getName().compareToIgnoreCase(org2.getName());
    }
  };

  private final OrganizationDAO organizationDAO;

  @Inject
  public OrganizationSummaryService(final OrganizationDAO organizationDAO) {
    this.organizationDAO = organizationDAO;
  }

  public OrganizationSummaryList getOrganizations(Goal goal) {
    return toOrganizationSummaryList(getOrganizationsForGoal(goal));
  }

  private List<Organization> getOrganizationsForGoal(Goal goal) {
    if (goal == Goal.EVALUATE_APPLICATION) {
      return getOrganizationsForEvaluateApplication();
    }
    return getApplicationsForRead();
  }

  @AuthzFilter(permission = Permission.READ, context = AuthzFilter.Context.ORGANIZATION)
  protected List<Organization> getApplicationsForRead() {
    return getOrganizations();
  }

  @AuthzFilter(permission = Permission.EVALUATE_APPLICATION, context = AuthzFilter.Context.ORGANIZATION)
  protected List<Organization> getOrganizationsForEvaluateApplication() {
    return getOrganizations();
  }

  private List<Organization> getOrganizations() {
    return organizationDAO.getAll().stream()
        .filter(org -> !org.getId().equals(Organization.ROOT_ORGANIZATION_ID))
        .collect(Collectors.toList());
  }

  private OrganizationSummaryList toOrganizationSummaryList(List<Organization> organizations) {
    // The input list may be immutable
    organizations = new ArrayList<>(organizations);
    organizations.sort(ORG_NAME_COMPARATOR);
    return OrganizationSummaryAdapter.convert(organizations);
  }
}
