/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
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
    return getOrganizationsForRead();
  }

  /**
   * Returns organizations the caller has {@link Permission#READ} on.
   */
  @AuthzFilter(permission = Permission.READ, context = AuthzFilter.Context.ORGANIZATION)
  public List<Organization> getOrganizationsForRead() {
    return getOrganizations();
  }

  /**
   * Returns organizations the caller has {@link Permission#READ} on among {@code organizationIds}.
   * <p>
   * Prefer this over {@link #getOrganizationsForRead()} when the candidate set is already known
   * (Martha Violations facet name-search): fetches only those ids before {@code @AuthzFilter} runs,
   * avoiding a full-tenant load on every search settle.
   */
  @AuthzFilter(permission = Permission.READ, context = AuthzFilter.Context.ORGANIZATION)
  public List<Organization> getOrganizationsForRead(Set<String> organizationIds) {
    if (organizationIds == null || organizationIds.isEmpty()) {
      return List.of();
    }
    return organizationDAO.getByIds(organizationIds)
        .stream()
        .filter(org -> org != null && !Organization.ROOT_ORGANIZATION_ID.equals(org.getId()))
        .collect(Collectors.toList());
  }

  /** Legacy name for {@link #getOrganizationsForRead()}. */
  @AuthzFilter(permission = Permission.READ, context = AuthzFilter.Context.ORGANIZATION)
  protected List<Organization> getApplicationsForRead() {
    return getOrganizations();
  }

  /**
   * Returns organizations the caller has EVALUATE_APPLICATION permission on.
   * <p>
   * <strong>License bypass warning:</strong> This method does NOT check the ENFORCEMENT license
   * feature that {@link #getOrganizations(Goal)} enforces. Callers MUST gate this method with
   * their own license check (e.g., {@code @ProductLicenseEnforcementPoint}) or restrict usage to
   * license-independent contexts.
   * <p>
   * <strong>Approved callers:</strong> ApiPolicyContextOwnersResource (gated via GUIDE_SEARCH license).
   *
   * @since 1.14.0
   */
  @AuthzFilter(permission = Permission.EVALUATE_APPLICATION, context = AuthzFilter.Context.ORGANIZATION)
  public List<Organization> getOrganizationsForEvaluateApplication() {
    return getOrganizations();
  }

  private List<Organization> getOrganizations() {
    return organizationDAO.getAll()
        .stream()
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
