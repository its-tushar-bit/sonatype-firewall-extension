/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.PathParam;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLoggerFactory;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzFilter;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.webhook.ManagementEventService;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.webhook.EventAction.CREATED;
import static com.sonatype.insight.brain.webhook.EventAction.DELETED;
import static com.sonatype.insight.brain.webhook.EventAction.UPDATED;

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

  private final ManagementEventService managementEventService;

  private final PolicyViolationLoggerFactory policyViolationLoggerFactory;

  @Inject
  public OrganizationService(final InsightWork work,
                             final ApplicationCleaner applicationCleaner,
                             final FileCleaner fileCleaner,
                             final OrganizationDAO organizationDAO,
                             final ManagementEventService managementEventService,
                             final PolicyViolationLoggerFactory policyViolationLoggerFactory)
  {
    this.work = work;
    this.applicationCleaner = applicationCleaner;
    this.fileCleaner = fileCleaner;
    this.organizationDAO = organizationDAO;
    this.managementEventService = managementEventService;
    this.policyViolationLoggerFactory = policyViolationLoggerFactory;
  }

  @AuthzFilter(permission = Permission.READ, context = AuthzFilter.Context.ORGANIZATION)
  public List<Organization> getAll() {
    return organizationDAO.getAll();
  }

  @Authorize(permission = Permission.WRITE)
  public Organization addOrganization(@AuthzContext(AuthzContext.Key.ORGANIZATION_OWNER) Organization organization) {
    organizationDAO.insert(organization);

    AuditData.get().setOrganization(organization);

    managementEventService.postEvent(CREATED, organization);

    return organization;
  }

  @Authorize(permission = Permission.WRITE)
  public Organization updateOrganization(@AuthzContext(AuthzContext.Key.ORGANIZATION) Organization organization) {
    organizationDAO.update(organization);

    AuditData.get().setOrganization(organization);

    managementEventService.postEvent(UPDATED, organization);
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
      @AuthzContext(AuthzContext.Key.ORGANIZATION_ID) @PathParam("organizationId") String orgId) throws IOException
  {
    Organization organization;
    try (TransactionContext tx = organizationDAO.createTransactionContext()) {
      tx.begin();
      organization = organizationDAO.getByIdNotNull(tx, orgId);
      AuditData.get().setOrganization(organization);
      deleteOrganization(tx, organization);
      tx.commit();
      AuditData.get().commitSubEvents();

      policyViolationLoggerFactory.newLogger(new Date(), organization).logClearEvent();
    }
    managementEventService.postEvent(DELETED, organization);
  }

  private void deleteOrganization(final TransactionContext tx, final Organization organization) throws IOException {
    if (organization.getParentOrganizationId() == null) {
      throw new BadRequestException("The root organization cannot be deleted.");
    }

    log.info("Deleting organization '{}' with id {}.", organization.getName(), organization.getId());

    // cascade to applications first
    for (Application application : new ApplicationDAO().getByOrganizationId(tx, organization.getId())) {
      log.info("Deleting application '{}' with id {}.", application.getName(), application.getId());

      applicationCleaner.delete(tx, application);
      try (AuditSession auditSession = AuditData.get().recordSubEvent(AuditEvent.DELETE_APPLICATION, false)) {
        AuditData.get().setApplicationWithDetails(application).setParentOrganization(organization);
      }

      log.info("Deleted application '{}' with id {}.", application.getName(), application.getId());
    }

    File organizationIconDirectory = new File(work.getOrganizationIconDir(), organization.getId());
    try {
      fileCleaner.delete(organizationIconDirectory);
    }
    catch (IOException e) {
      log.error("Could not delete organization icons: {}" + organizationIconDirectory, e);
    }

    // delete organization last, this way the operation can be retried later if anything goes wrong
    organizationDAO.delete(tx, organization);

    log.info("Deleted organization '{}' with id {}.", organization.getName(), organization.getId());
  }
}
