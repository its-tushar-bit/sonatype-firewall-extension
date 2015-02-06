/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.InvalidApplicationException;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.security.UserDirectory;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.PaymentRequiredException;

/**
 * @since 1.11.0
 */
@Named
public class ApplicationHelper
{
  private final ApplicationDAO applicationDAO;

  private final OrganizationDAO organizationDAO;

  private final UserDirectory userDirectory;

  private final ApplicationCleaner applicationCleaner;

  private final CLMLicenseManager licenseManager;

  @Inject
  public ApplicationHelper(final ApplicationDAO applicationDAO, final OrganizationDAO organizationDAO,
      final UserDirectory userDirectory, final ApplicationCleaner applicationCleaner,
      final CLMLicenseManager licenseManager)
  {
    this.applicationDAO = applicationDAO;
    this.licenseManager = licenseManager;
    this.organizationDAO = organizationDAO;
    this.userDirectory = userDirectory;
    this.applicationCleaner = applicationCleaner;
  }

  public Application getApplicationByIdNotNull(final String applicationId) {
    return applicationDAO.getByIdNotNull(applicationId);
  }

  public Application addApplication(final TransactionContext tx, final Application application) {
    validate(application);
    applicationDAO.insert(tx, application);
    return application;
  }

  public Application addApplication(final Application application) {
    try (TransactionContext tx = applicationDAO.createTransactionContext()) {
      tx.begin();
      addApplication(tx, application);
      tx.commit();
    }
    return application;
  }

  public void deleteApplicationById(final String applicationId) throws IOException {
    try (TransactionContext tx = applicationDAO.createTransactionContext()) {
      tx.begin();
      final Application app = applicationDAO.getByIdNotNull(tx, applicationId);
      applicationCleaner.delete(tx, app);
      tx.commit();
    }
  }

  private void validate(final Application application) {
    int appLimit = licenseManager.getApplicationCountLimit();
    if (applicationDAO.getAll().size() >= appLimit) {
      throw new PaymentRequiredException("You have exceeded the licensed limit of " + appLimit + " applications.");
    }

    if (application.getId() != null) {
      throw new InvalidApplicationException("Application must not have an ID set on creation.");
    }

    final String organizationId = application.getOrganizationId();
    if (organizationId == null) {
      throw new InvalidApplicationException("Application must have a parent organization.");
    }

    final Organization org = organizationDAO.getById(organizationId);
    if (org == null) {
      throw new InvalidApplicationException(
          "Application references an organization (ID=" + organizationId + ") that does not exist.");
    }

    final String contact = application.getContactInternalName();
    if (contact != null) {
      final Set<String> users = new HashSet<>();
      users.add(contact);
      final Set<String> invalidUsers = userDirectory.validateUsers(users);
      if (!invalidUsers.isEmpty()) {
        throw new InvalidApplicationException(
            "Application has a contactUserName=" + invalidUsers.iterator().next() + " that does not exist.");
      }
    }
  }
}
