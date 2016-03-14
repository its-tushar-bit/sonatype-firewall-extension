/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzFilter;

@Named
public class ApplicationMigrationService
{
  private final ApplicationDAO applicationDAO;

  private final OrganizationDAO organizationDAO;

  @Inject
  public ApplicationMigrationService(ApplicationDAO applicationDAO, OrganizationDAO organizationDAO) {
    this.applicationDAO = applicationDAO;
    this.organizationDAO = organizationDAO;
  }

  @Authorize(permission = Permission.WRITE)
  public List<Organization> getDestinationOrganizations(@AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId)
  {
    Application application = applicationDAO.getByIdNotNull(applicationId);
    List<Organization> organizations = new ArrayList<>();
    for (Organization organization : getPermittedDestinationOrganizations()) {
      // moving to the current parent organization is pointless so exclude it
      if (!organization.getId().equals(application.getOrganizationId())) {
        organizations.add(organization);
      }
    }
    return organizations;
  }

  @AuthzFilter(permission = Permission.ADD_APPLICATION, context = AuthzFilter.Context.ORGANIZATION)
  List<Organization> getPermittedDestinationOrganizations() {
    return organizationDAO.getAll(false);
  }

  @Authorize(permission = Permission.WRITE)
  public void migrateApplication(@AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId,
                                 String organizationId)
  {
    Application application = applicationDAO.getByIdNotNull(applicationId);
    if (application.getOrganizationId().equals(organizationId)) {
      return;
    }
    migrateApplication(application, organizationId);
  }

  @Authorize(permission = Permission.ADD_APPLICATION)
  void migrateApplication(Application application, @AuthzContext(AuthzContext.Key.ORGANIZATION_ID) String organizationId)
  {
    // stay tuned for scenes from our next episode
  }
}
