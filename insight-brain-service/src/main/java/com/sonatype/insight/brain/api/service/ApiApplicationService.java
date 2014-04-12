/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.service;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;

import com.sonatype.insight.brain.api.dto.ApiApplicationDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.InvalidApplicationException;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.organization.ApplicationCleaner;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.dataaccess.AbstractDAO;
import com.sonatype.insight.error.exception.PaymentRequiredException;

/**
 * @since 1.11.0
 */
@Named
public class ApiApplicationService
{
  private final CLMLicenseManager licenseManager;

  private final ApplicationDAO applicationDAO;

  private final OrganizationDAO organizationDAO;

  private final UserValidationService userValidationService;

  private final ApplicationCleaner applicationCleaner;


  @Inject
  public ApiApplicationService(final CLMLicenseManager licenseManager, final ApplicationDAO applicationDAO,
                               final OrganizationDAO organizationDAO, final UserValidationService userValidationService,
                               final ApplicationCleaner applicationCleaner)
  {
    this.licenseManager = licenseManager;
    this.applicationDAO = applicationDAO;
    this.organizationDAO = organizationDAO;
    this.userValidationService = userValidationService;
    this.applicationCleaner = applicationCleaner;
  }

  @Authorize(permission = Permission.READ)
  public ApiApplicationDTO getApplication(@AuthzContext(AuthzContext.Key.APPLICATION_ID) final String applicationId) {

    final Application application = applicationDAO.getByIdNotNull(applicationId);
    return convert(application);
  }

  @Authorize(permission = Permission.WRITE)
  public ApiApplicationDTO addApplication(
      @AuthzContext(AuthzContext.Key.APPLICATION_OWNER) final Application application)
  {
    validate(application);

    final EntityManager em = applicationDAO.createEntityManager();
    try {
      em.getTransaction().begin();

      applicationDAO.insert(em, application);

      em.getTransaction().commit();
    }
    finally {
      AbstractDAO.close(em);
    }

    return convert(application);
  }

  @Authorize(permission = Permission.WRITE)
  public void deleteApplication(@AuthzContext(AuthzContext.Key.APPLICATION_ID) final String applicationId)
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

  private ApiApplicationDTO convert(final Application application) {
    final ApiApplicationDTO applicationDTO = new ApiApplicationDTO();
    applicationDTO.setId(application.getId());
    applicationDTO.setPublicId(application.getPublicId());
    applicationDTO.setName(application.getName());
    applicationDTO.setOrganizationId(application.getOrganizationId());
    applicationDTO.setContactUserName(application.getContactInternalName());

    return applicationDTO;
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
