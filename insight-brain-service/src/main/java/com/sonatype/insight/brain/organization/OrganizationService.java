/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.ws.rs.PathParam;

import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzFilter;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.dataaccess.AbstractDAO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @since 1.11.0
 */
@Named
public class OrganizationService
{
  private static final Logger log = LoggerFactory.getLogger(OrganizationService.class);

  private final OrganizationDAO organizationDAO;

  private final ApplicationCleaner applicationCleaner;

  private final InsightWork work;

  private final FileCleaner fileCleaner;

  @Inject
  public OrganizationService(final InsightWork work, final ApplicationCleaner applicationCleaner,
                             final FileCleaner fileCleaner, final OrganizationDAO organizationDAO)
  {
    this.work = work;
    this.applicationCleaner = applicationCleaner;
    this.fileCleaner = fileCleaner;
    this.organizationDAO = organizationDAO;
  }

  @AuthzFilter(permission = Permission.READ, context = AuthzFilter.Context.ORGANIZATION)
  public List<Organization> getAll() {
    return organizationDAO.getAll();
  }

  @Authorize(permission = Permission.WRITE)
  public Organization addOrganization(@AuthzContext(AuthzContext.Key.ORGANIZATION_OWNER) Organization organization) {
    organizationDAO.insert(organization);
    return organization;
  }

  @Authorize(permission = Permission.WRITE)
  public Organization updateOrganization(@AuthzContext(AuthzContext.Key.ORGANIZATION) Organization organization) {
    organizationDAO.update(organization);
    return organization;
  }

  /**
   * Deletes an organization and associated policies, license threat groups, labels and waivers. Also deletes all
   * applications under the organization.
   *
   * @since 1.11.0
   */
  @Authorize(permission = Permission.WRITE)
  public void deleteOrganization(
      @AuthzContext(AuthzContext.Key.ORGANIZATION_ID) @PathParam("organizationId") final String organizationId)
      throws IOException
  {
    EntityManager em = organizationDAO.createEntityManager();
    try {
      em.getTransaction().begin();
      deleteOrganization(em, organizationId);
      em.getTransaction().commit();
    }
    finally {
      AbstractDAO.close(em);
    }
  }

  private void deleteOrganization(final EntityManager em, final String organizationId) throws IOException {
    Organization organization = organizationDAO.getByIdNotNull(em, organizationId);

    // cascade to applications first
    for (Application application : new ApplicationDAO().getByOrganizationId(em, organizationId)) {
      applicationCleaner.delete(em, application);
    }

    File organizationIconDirectory = new File(work.getOrganizationIconDir(), organizationId);
    try {
      fileCleaner.delete(organizationIconDirectory);
    }
    catch (IOException e) {
      log.error("Could not delete organization icons: {}" + organizationIconDirectory, e);
    }

    // delete organization last, this way the operation can be retried later if anything goes wrong
    organizationDAO.delete(em, organization);
  }
}
