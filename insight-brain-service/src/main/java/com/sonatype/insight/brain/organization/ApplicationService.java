/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;

import com.sonatype.insight.brain.api.service.UserValidationService;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.InvalidApplicationException;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzFilter;
import com.sonatype.insight.dataaccess.AbstractDAO;
import com.sonatype.insight.error.exception.PaymentRequiredException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ApplicationService
{
  private static final Logger log = LoggerFactory.getLogger(ApplicationService.class);

  private final ApplicationDAO applicationDAO;

  private final OrganizationDAO organizationDAO;

  private final UserValidationService userValidationService;

  private final ApplicationCleaner applicationCleaner;

  private final CLMLicenseManager licenseManager;

  @Inject
  public ApplicationService(ApplicationDAO applicationDAO, final OrganizationDAO organizationDAO,
      final UserValidationService userValidationService,
      final ApplicationCleaner applicationCleaner, final CLMLicenseManager licenseManager)
  {
    this.applicationDAO = applicationDAO;
    this.licenseManager = licenseManager;
    this.organizationDAO = organizationDAO;
    this.userValidationService = userValidationService;
    this.applicationCleaner = applicationCleaner;
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

  @Authorize(permission = Permission.READ)
  public Application getApplicationByIdNotNull(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) final String applicationId) {

    return applicationDAO.getByIdNotNull(applicationId);
  }

  @Authorize(permission = Permission.WRITE)
  public Application addApplication(
      @AuthzContext(AuthzContext.Key.APPLICATION_OWNER) final Application application)
  {
    validate(application);

    applicationDAO.insert(application);

    return application;
  }

  @Authorize(permission = Permission.WRITE)
  public Application updateApplication(@AuthzContext(AuthzContext.Key.APPLICATION) Application application) {
    if (application.getOrganizationId() == null) {
      throw new InvalidApplicationException("Applications must have a parent organization.");
    }
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

  @Authorize(permission = Permission.WRITE)
  public void deleteApplicationById(@AuthzContext(AuthzContext.Key.APPLICATION_ID) final String applicationId)
      throws IOException
  {
    final EntityManager em = applicationDAO.createEntityManager();
    try {
      em.getTransaction().begin();
      final Application app = applicationDAO.getByIdNotNull(em, applicationId);
      applicationCleaner.delete(em, app);
      em.getTransaction().commit();
    }
    finally {
      AbstractDAO.close(em);
    }
  }

  private void validate(final Application application) {

    int appLimit = licenseManager.getApplicationCountLimit();
    if (applicationDAO.getAll().size() >= appLimit) {
      throw new PaymentRequiredException("You have exceeded the licensed limit of " + appLimit + " applications.");
    }

    if (application.getId() != null) {
      throw new InvalidApplicationException("Application must not have an id set on creation.");
    }

    final String organizationId = application.getOrganizationId();
    if (organizationId == null) {
      throw new InvalidApplicationException("Application must have a parent organization.");
    }

    final Organization org = organizationDAO.getById(organizationId);
    if (org == null) {
      throw new InvalidApplicationException(
          "Application references an organization (id=" + organizationId + ") that does not exist.");
    }

    final String contact = application.getContactInternalName();
    if (contact != null) {
      final Set<String> users = new HashSet<>();
      users.add(contact);
      final Set<String> invalidUsers = userValidationService.validateUsers(users);
      if (!invalidUsers.isEmpty()) {
        throw new InvalidApplicationException(
            "Application has a contactUserName=" + invalidUsers.iterator().next() + " that does not exist.");
      }
    }
  }
}
