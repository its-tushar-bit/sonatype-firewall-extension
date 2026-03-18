/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.InvalidApplicationException;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLoggerFactory;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.security.UserDirectory;
import com.sonatype.insight.brain.webhook.OrganizationApplicationManagementEventService;
import com.sonatype.insight.brain.telemetry.OwnerMaintenanceTelemetry;
import com.sonatype.insight.brain.telemetry.OwnerMaintenanceTelemetryCreator;
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

  private final MembershipMappingDAO membershipMappingDAO;

  private final UserDirectory userDirectory;

  private final ApplicationCleaner applicationCleaner;

  private final ProductLicense productLicense;

  private final CurrentUser currentUser;

  private final PolicyViolationLoggerFactory policyViolationLoggerFactory;

  private final OrganizationApplicationManagementEventService organizationApplicationManagementEventService;

  private final OwnerMaintenanceTelemetryCreator ownerMaintenanceTelemetryCreator;

  @Inject
  public ApplicationHelper(
      final ApplicationDAO applicationDAO,
      final OrganizationDAO organizationDAO,
      final MembershipMappingDAO membershipMappingDAO,
      final UserDirectory userDirectory,
      final ApplicationCleaner applicationCleaner,
      final ProductLicense productLicense,
      final CurrentUser currentUser,
      final PolicyViolationLoggerFactory policyViolationLoggerFactory,
      final OrganizationApplicationManagementEventService organizationApplicationManagementEventService,
      final OwnerMaintenanceTelemetryCreator ownerMaintenanceTelemetryCreator)
  {
    this.applicationDAO = applicationDAO;
    this.productLicense = productLicense;
    this.organizationDAO = organizationDAO;
    this.userDirectory = userDirectory;
    this.applicationCleaner = applicationCleaner;
    this.currentUser = currentUser;
    this.membershipMappingDAO = membershipMappingDAO;
    this.policyViolationLoggerFactory = policyViolationLoggerFactory;
    this.organizationApplicationManagementEventService = organizationApplicationManagementEventService;
    this.ownerMaintenanceTelemetryCreator = ownerMaintenanceTelemetryCreator;
  }

  public Application getApplicationByIdNotNull(final String applicationId) {
    return applicationDAO.getByIdNotNull(applicationId);
  }

  public Application addApplication(
      final TransactionContext tx,
      final Application application,
      final boolean setOwner)
  {
    validateNewApplication(application);
    applicationDAO.insert(tx, application);

    if (setOwner) {
      addUserToApplicationOwnerRole(tx, application);
    }

    ownerMaintenanceTelemetryCreator.sendOwnerMaintenanceTelemetry(application,
        OwnerMaintenanceTelemetry.TYPE_ADD);
    return application;
  }

  public Application addApplication(final TransactionContext tx, final Application application) {
    return addApplication(tx, application, true);
  }

  public Application addApplication(final Application application) {
    try (TransactionContext tx = applicationDAO.createTransactionContext()) {
      tx.begin();
      addApplication(tx, application);
      tx.commit();
    }
    postAddApplicationEvent();
    return application;
  }

  public void deleteApplicationById(final String applicationId) throws IOException {
    try (TransactionContext tx = applicationDAO.createTransactionContext()) {
      tx.begin();
      final Application app = applicationDAO.getByIdNotNull(tx, applicationId);
      AuditData.get()
          .setApplicationWithDetails(app)
          .setParentOrganization(organizationDAO.getByIdNotNull(app.getParentOwnerId()));
      applicationCleaner.delete(tx, app);

      tx.commit();

      policyViolationLoggerFactory.newLogger(new Date(), app).logClearEvent();
    }
  }

  public void validateNewApplication(final Application application) {
    Integer appLimit = productLicense.getMaxApplications();
    if (appLimit != null && (appLimit == 0 || applicationDAO.getCountWithoutRelatedRepositories() >= appLimit)) {
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
      throw new InvalidApplicationException("Application references an organization (ID=" + organizationId
          + ") that does not exist.");
    }
    if (org.getParentOrganizationId() == null) {
      throw new InvalidApplicationException("Applications cannot have the root organization as parent.");
    }

    final String contact = application.getContactInternalName();
    if (contact != null) {
      final Set<String> users = new HashSet<>();
      users.add(contact);
      final Set<String> invalidUsers = userDirectory.validateUsers(users);
      if (!invalidUsers.isEmpty()) {
        throw new InvalidApplicationException("Application has a contactUserName=" + invalidUsers.iterator().next()
            + " that does not exist.");
      }
    }
  }

  public void postAddApplicationEvent() {
    // Post event here instead of ApplicationService so that SCM imports and automatic application creation (CLI)
    // are covered
    organizationApplicationManagementEventService.postEvent();
  }

  private void addUserToApplicationOwnerRole(final TransactionContext tx, Application application) {
    membershipMappingDAO.insert(tx,
        new MembershipMapping(application.getId(), Role.OWNER_ROLE_ID, currentUser.getUsername(), MemberType.USER));
  }
}
