/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.PathParam;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLoggerFactory;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzFilter;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.service.githubapp.GitHubAppDeletionService;
import com.sonatype.insight.brain.telemetry.OwnerMaintenanceTelemetry;
import com.sonatype.insight.brain.telemetry.OwnerMaintenanceTelemetryCreator;
import com.sonatype.insight.brain.webhook.ManagementEventService;
import com.sonatype.insight.brain.webhook.OrganizationApplicationManagementEventService;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.webhook.EventAction.CREATED;
import static com.sonatype.insight.brain.webhook.EventAction.DELETED;
import static com.sonatype.insight.brain.webhook.EventAction.UPDATED;
import static java.util.Objects.nonNull;

/**
 * @since 1.11.0
 */
@Named
public class OrganizationService
{
  private static final Logger log = LoggerFactory.getLogger(OrganizationService.class);

  private final OrganizationDAO organizationDAO;

  private final ApplicationDAO applicationDAO;

  private final ApplicationCleaner applicationCleaner;

  private final InsightWork work;

  private final FileCleaner fileCleaner;

  private final ManagementEventService managementEventService;

  private final PolicyViolationLoggerFactory policyViolationLoggerFactory;

  private final OrganizationApplicationManagementEventService organizationApplicationManagementEventService;

  private final OwnerMaintenanceTelemetryCreator ownerMaintenanceTelemetryCreator;

  private final GitHubAppDeletionService gitHubAppDeletionService;

  @Inject
  public OrganizationService(
      final InsightWork work,
      final ApplicationCleaner applicationCleaner,
      final FileCleaner fileCleaner,
      final OrganizationDAO organizationDAO,
      final ApplicationDAO applicationDAO,
      final ManagementEventService managementEventService,
      final PolicyViolationLoggerFactory policyViolationLoggerFactory,
      final OrganizationApplicationManagementEventService organizationApplicationManagementEventService,
      final OwnerMaintenanceTelemetryCreator ownerMaintenanceTelemetryCreator,
      final GitHubAppDeletionService gitHubAppDeletionService)
  {
    this.work = work;
    this.applicationCleaner = applicationCleaner;
    this.fileCleaner = fileCleaner;
    this.organizationDAO = organizationDAO;
    this.applicationDAO = applicationDAO;
    this.managementEventService = managementEventService;
    this.policyViolationLoggerFactory = policyViolationLoggerFactory;
    this.organizationApplicationManagementEventService = organizationApplicationManagementEventService;
    this.ownerMaintenanceTelemetryCreator = ownerMaintenanceTelemetryCreator;
    this.gitHubAppDeletionService = gitHubAppDeletionService;
  }

  @AuthzFilter(permission = Permission.READ, context = AuthzFilter.Context.ORGANIZATION)
  public List<Organization> getAll() {
    return organizationDAO.getAll();
  }

  @AuthzFilter(permission = Permission.READ, context = AuthzFilter.Context.ORGANIZATION)
  public List<Organization> getAllWithoutRelatedRepositories() {
    return organizationDAO.getAllWithoutRelatedRepositories();
  }

  @AuthzFilter(permission = Permission.WRITE, context = AuthzFilter.Context.ORGANIZATION)
  List<Organization> getAllWithWritePermissions() {
    return organizationDAO.getAll();
  }

  @Authorize(permission = Permission.READ)
  public Organization getOrganization(@AuthzContext(AuthzContext.Key.ORGANIZATION_ID) String orgId) {
    return organizationDAO.getByIdNotNull(orgId);
  }

  @Authorize(permission = Permission.WRITE)
  public Organization addOrganization(@AuthzContext(AuthzContext.Key.ORGANIZATION_OWNER) Organization organization) {
    organizationDAO.insert(organization);

    AuditData.get().setOrganization(organization);

    managementEventService.postEvent(CREATED, organization);
    organizationApplicationManagementEventService.postEventForLifecycle();
    ownerMaintenanceTelemetryCreator.sendOwnerMaintenanceTelemetry(organization, OwnerMaintenanceTelemetry.TYPE_ADD);

    return organization;
  }

  @Authorize(permission = Permission.WRITE)
  public Organization updateOrganization(@AuthzContext(AuthzContext.Key.ORGANIZATION) Organization organization) {
    AuditData.get().setOrganization(organization);

    Organization existingOrganization = organizationDAO.getById(organization.getId());
    if (!Objects.equals(existingOrganization.getParentOrganizationId(), organization.getParentOrganizationId())) {
      throw new BadRequestException("Cannot change the parent organization. Use move organization instead.");
    }
    organizationDAO.update(organization);

    managementEventService.postEvent(UPDATED, organization);
    organizationApplicationManagementEventService.postEventForLifecycle();
    ownerMaintenanceTelemetryCreator.sendOwnerMaintenanceTelemetry(organization, OwnerMaintenanceTelemetry.TYPE_UPDATE);

    return organization;
  }

  /**
   * Deletes an organization and associated policies, license threat groups, labels and waivers. The deletion is also
   * cascaded to all child organizations and applications under the organization.
   *
   * @param orgId ID of the organization that will be deleted
   *
   * @since 1.11.0
   */
  @Authorize(permission = Permission.WRITE)
  public void deleteOrganization(
      @AuthzContext(AuthzContext.Key.ORGANIZATION_ID) @PathParam("organizationId") String orgId) throws IOException
  {
    deleteOrganizationNoAuthz(orgId);
  }

  public void deleteOrganizationNoAuthz(String orgId) throws IOException {
    Organization organization = organizationDAO.getByIdNotNull(orgId);
    AuditData.get().setOrganization(organization);
    deleteOrganization(organization);
    AuditData.get().commitSubEvents();
  }

  private void deleteOrganization(final Organization organization) throws IOException {
    if (organization.getParentOrganizationId() == null) {
      throw new BadRequestException("The root organization cannot be deleted.");
    }

    List<Organization> childOrganizations = organizationDAO.getByParentOrganizationId(organization.getId());
    List<Application> childApplications = applicationDAO.getByOrganizationId(organization.getId());

    try {
      // cascade to children orgs
      log.info("Deleting organization '{}' with id {}.", organization.getName(), organization.getId());
      for (Organization childOrg : childOrganizations) {
        log.info("Deleting child organization '{}' with id {}.", childOrg.getName(), childOrg.getId());
        try (AuditSession auditSession = AuditData.get().recordSubEvent(AuditEvent.DELETE_ORGANIZATION, false)) {
          deleteOrganization(childOrg);
          AuditData.get().setOrganization(childOrg).setParentOrganization(organization);
        }
      }

      try (final TransactionContext tx = organizationDAO.createTransactionContext()) {
        tx.begin();

        // cascade to applications
        for (Application application : childApplications) {
          log.info("Deleting application '{}' with id {}.", application.getName(), application.getId());

          applicationCleaner.delete(tx, application);
          try (AuditSession auditSession = AuditData.get().recordSubEvent(AuditEvent.DELETE_APPLICATION, false)) {
            AuditData.get().setApplicationWithDetails(application).setParentOrganization(organization);
          }

          log.info("Deleted application '{}' with id {}.", application.getName(), application.getId());
        }
        deleteOrganizationIconFolder(organization);

        gitHubAppDeletionService.deactivateGitHubApps(tx, organization.getId());

        // delete organization last, this way the operation can be retried later if anything goes wrong
        organizationDAO.delete(tx, organization);
        tx.commit();
        managementEventService.postEvent(DELETED, organization);
        organizationApplicationManagementEventService.postEventForLifecycle();
        ownerMaintenanceTelemetryCreator
            .sendOwnerMaintenanceTelemetry(organization, OwnerMaintenanceTelemetry.TYPE_DELETE);
        policyViolationLoggerFactory.newLogger(new Date(), organization).logClearEvent();

        log.info("Deleted organization '{}' with id {}.", organization.getName(), organization.getId());
      }
    }
    catch (PartialDeletionException e) {
      throw e;
    }
    catch (Exception e) {
      if (!childOrganizations.isEmpty() || !childApplications.isEmpty()) {
        throw new PartialDeletionException(e);
      }
      else {
        throw e;
      }
    }
  }

  public Map<String, Organization> getAllParentOrgsNoAuthz(Collection<? extends Owner> owners) {
    return getAllParentOrgsNoAuthz(owners, null);
  }

  public Map<String, Organization> getAllParentOrgsNoAuthz(
      Collection<? extends Owner> owners,
      Map<String, Organization> knownParentOrgs)
  {
    return getAllParentOrgsNoAuthz(owners, knownParentOrgs, null);
  }

  // you can pass the owner type if it's known and consistent for all entries in the owners collection
  public Map<String, Organization> getAllParentOrgsNoAuthz(
      Collection<? extends Owner> owners,
      Map<String, Organization> knownParentOrgs,
      final OwnerType ownerType)
  {
    if (CollectionUtils.isEmpty(owners)) {
      return Collections.emptyMap();
    }

    List<String> needsFetch = owners.stream()
        .map(owner -> owner.getId())
        .filter(Objects::nonNull)
        .toList();

    final Map<String, Organization> results = organizationDAO.getAllParentOrganizations(needsFetch, ownerType)
        .stream()
        .collect(
            Collectors.toMap(Owner::getId, Function.identity(), (existing, replacement) -> existing));

    if (nonNull(knownParentOrgs)) {
      results.putAll(knownParentOrgs);
    }

    return results;
  }

  private void deleteOrganizationIconFolder(Organization organization) {
    File organizationIconDirectory = new File(work.getOrganizationIconDir(), organization.getId());
    try {
      fileCleaner.delete(organizationIconDirectory);
    }
    catch (IOException e) {
      log.error("Could not delete organization icon: {}" + organizationIconDirectory, e);
    }
  }
}
