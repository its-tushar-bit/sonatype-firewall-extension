/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.ApiPolicyDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyListDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyOwnerType;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.organization.OrganizationService;

/**
 * @since 1.12.0
 */
@Named
public class ApiPolicyService
{
  private final PolicyDAO policyDAO;

  private final ApplicationService applicationService;

  private final OrganizationService organizationService;

  private final OwnerDAO ownerDAO;

  @Inject
  public ApiPolicyService(final PolicyDAO policyDAO,
                          final ApplicationService applicationService,
                          final OrganizationService organizationService,
                          final OwnerDAO ownerDAO)
  {
    this.policyDAO = policyDAO;
    this.applicationService = applicationService;
    this.organizationService = organizationService;
    this.ownerDAO = ownerDAO;
  }

  public ApiPolicyListDTO getPolicies() {
    ApiPolicyListDTO policyListDTO = new ApiPolicyListDTO();
    policyListDTO.policies = filterPolicies();
    return policyListDTO;
  }

  private List<ApiPolicyDTO> filterPolicies() {
    List<ApiPolicyDTO> apiPolicyList = new ArrayList<>();
    Set<String> applicationIds = new HashSet<>();
    Set<String> organizationIds = new HashSet<>();
    getFilteredOwnerIds(applicationIds, organizationIds);

    List<Policy> appPolicies = policyDAO.getByOwnerIds(applicationIds);
    apiPolicyList.addAll(ApiPolicyAdapter.convert(appPolicies, ApiPolicyOwnerType.APPLICATION));
    List<Policy> orgPolicies = policyDAO.getByOwnerIds(organizationIds);
    apiPolicyList.addAll(ApiPolicyAdapter.convert(orgPolicies, ApiPolicyOwnerType.ORGANIZATION));

    return apiPolicyList;
  }

  private void getFilteredOwnerIds(Set<String> applicationIds, Set<String> organizationIds) {
    // Add the apps that the user has permissions to
    for (Application application : applicationService.getApplications()) {
      applicationIds.add(application.getId());
      // Since the user has permission to the app,
      // add the org hierarchy the app belongs to even if they don't have permissions to the orgs themselves)
      addOrganizationIds(application.getOrganizationId(), organizationIds);
    }

    // Now add the orgs that the user has permissions to
    for (Organization organization : organizationService.getAll()) {
      organizationIds.add(organization.getId());
      // as with apps, also add any parent orgs regardless of explicit permission
      addOrganizationIds(organization.getParentOrganizationId(), organizationIds);
    }
  }

  private void addOrganizationIds(String organizationId, Set<String> organizationIds) {
    if (organizationId != null && organizationIds.add(organizationId)) {
      for (Owner owner : ownerDAO.walkHierarchy(organizationId)) {
        if (owner.getParentOwnerId() == null || !organizationIds.add(owner.getParentOwnerId())) {
          break;
        }
      }
    }
  }
}
