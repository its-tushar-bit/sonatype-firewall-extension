/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzFilter;
import com.sonatype.insight.dataaccess.AbstractDAO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ApplicationService
{
  private static final Logger log = LoggerFactory.getLogger(ApplicationService.class);

  private final ApplicationDAO applicationDAO;

  private final ApplicationCleaner applicationCleaner;

  private final ApplicationHelper applicationHelper;


  @Inject
  public ApplicationService(ApplicationDAO applicationDAO, final ApplicationCleaner applicationCleaner,
      final ApplicationHelper applicationHelper)
  {
    this.applicationDAO = applicationDAO;
    this.applicationCleaner = applicationCleaner;
    this.applicationHelper = applicationHelper;
  }

  public String validateApplicationPublicId(final String applicationPublicId) {
    if (applicationDAO.getByPublicId(applicationPublicId) == null) {
      return "Invalid application id " + applicationPublicId;
    }

    log.debug("Found application with public id {}", applicationPublicId);
    return "OK";
  }

  public Map<String, String> getApplicationNames() {
    List<Application> applications = applicationDAO.getAll();
    Map<String, String> applicationPublicIDNamePairs = new LinkedHashMap<>();

    for (Application application : applications) {
      log.debug("Found application with public id {}", application.getPublicId());
      applicationPublicIDNamePairs.put(application.getPublicId(), application.getName());
    }

    return applicationPublicIDNamePairs;
  }

  @Authorize(permission = Permission.READ)
  public Application getApplicationByPublicIdNotNull(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId)
  {
    return applicationDAO.getByPublicIdNotNull(applicationPublicId);
  }

  @Authorize(permission = Permission.READ)
  public Application getApplicationByPublicId(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId)
  {
    return applicationDAO.getByPublicId(applicationPublicId);
  }

  @AuthzFilter(permission = Permission.READ, context = AuthzFilter.Context.APPLICATION)
  public List<Application> getApplications() {
    return applicationDAO.getAll();
  }

  @Authorize(permission = Permission.WRITE)
  public Application addApplication(
      @AuthzContext(AuthzContext.Key.APPLICATION_OWNER) final Application application)
  {
    applicationHelper.addApplication(application);
    return application;
  }

  @Authorize(permission = Permission.WRITE)
  public Application updateApplication(@AuthzContext(AuthzContext.Key.APPLICATION) Application application) {
    applicationDAO.update(application);
    return application;
  }

  @Authorize(permission = Permission.WRITE)
  public void deleteApplicationByPublicId(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) final String applicationPublicId)
      throws IOException
  {
    EntityManager em = applicationDAO.createEntityManager();
    try {
      em.getTransaction().begin();
      Application application = applicationDAO.getByPublicIdNotNull(em, applicationPublicId);
      applicationCleaner.delete(em, application);
      em.getTransaction().commit();
    }
    finally {
      AbstractDAO.close(em);
    }
  }
}
