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
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.PathParam;

import com.sonatype.insight.brain.api.v2.dto.WaivedComponentUpgradeNotificationDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLoggerFactory;
import com.sonatype.insight.brain.policy.waiver.WaivedComponentUpgradeListener;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.security.AuthzFilter;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.tenancy.TenantManaged;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.webhook.ManagementEventService;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
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

  private final List<WaivedComponentUpgradeListener> listeners = new CopyOnWriteArrayList<>();

  @Inject
  public OrganizationService(
      final InsightWork work,
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

  @Authorize(permission = Permission.READ)
  public Organization getOrganization(@AuthzContext(AuthzContext.Key.ORGANIZATION_ID) String orgId) {
    return organizationDAO.getById(orgId);
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
    Organization organization = organizationDAO.getByIdNotNull(orgId);
    AuditData.get().setOrganization(organization);
    deleteOrganization(organization);
    AuditData.get().commitSubEvents();
  }

  /**
   * @since 1.159
   */
  public Organization updateWaivedComponentUpgradeNotification(
      WaivedComponentUpgradeNotificationDTO waivedComponentUpgradeNotificationDTO)
  {
    Organization rootOrganization = organizationDAO.getByIdNotNull(Organization.ROOT_ORGANIZATION_ID);
    rootOrganization.setWaivedComponentUpgradeStageTypeId(waivedComponentUpgradeNotificationDTO.getStage());

    rootOrganization = updateOrganization(rootOrganization);
    notifyWaivedComponentUpgradeListeners(rootOrganization.getWaivedComponentUpgradeStageTypeId());
    return rootOrganization;
  }

  /**
   * @since 1.159
   */
  public WaivedComponentUpgradeNotificationDTO getWaivedComponentUpgradeNotification() {
    checkReadPermission(Organization.ROOT_ORGANIZATION_ID);
    Organization rootOrganization = organizationDAO.getByIdNotNull(Organization.ROOT_ORGANIZATION_ID);

    WaivedComponentUpgradeNotificationDTO waivedComponentUpgradeNotificationDTO =
        new WaivedComponentUpgradeNotificationDTO();
    waivedComponentUpgradeNotificationDTO.setStage(rootOrganization.getWaivedComponentUpgradeStageTypeId());

    return waivedComponentUpgradeNotificationDTO;
  }

  /**
   * Registers the specified listener to be notified of changes to the waived component upgrade stage on root org.
   *
   * @since 1.159
   */
  public void addListener(WaivedComponentUpgradeListener listener) {
    if (Objects.isNull(listener)) {
      throw new IllegalArgumentException("listener not specified");
    }
    listeners.add(listener);
    log.debug("Added listener {}", listener);
  }

  /**
   * Unregisters the specified listener.
   *
   * @since 1.159
   */
  public void removeListener(WaivedComponentUpgradeListener listener) {
    listeners.remove(listener);
    log.debug("Removed listener {}", listener);
  }

  /**
   * Notifies the listeners of the waived component upgrade notification stage change.
   *
   * @param newWaivedComponentUpgradeStageTypeId ID of the evaluation stage to monitor for upgrades
   *
   * @since 1.159
   */
  private void notifyWaivedComponentUpgradeListeners(String newWaivedComponentUpgradeStageTypeId) {
    for (WaivedComponentUpgradeListener listener  : listeners) {
      try {
        if (listener instanceof TenantManaged && new TenantUtil().isGlobalTenant()) {
          // TenantManaged listeners should not be called in the context of the Global tenant
          continue;
        }

        log.debug("Notifying listener {}", listener);
        listener.waivedComponentUpgradeNotificationStageUpdated(newWaivedComponentUpgradeStageTypeId);
      }
      catch (RuntimeException e) {
        log.warn("Failed to notify {} of waived component upgrade stage update", listener, e);
      }
    }
  }

  @Authorize(permission = Permission.READ)
  void checkReadPermission(@AuthzContext(Key.ORGANIZATION_ID) String organizationId) {
    // Do nothing as this method is only used to perform authz check for the caller
  }

  private void deleteOrganization(final Organization organization) throws IOException {
    if (organization.getParentOrganizationId() == null) {
      throw new BadRequestException("The root organization cannot be deleted.");
    }

    List<Organization> childOrganizations = organizationDAO.getByParentOrganizationId(organization.getId());
    List<Application> childApplications = new ApplicationDAO().getByOrganizationId(organization.getId());

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
        // delete organization last, this way the operation can be retried later if anything goes wrong
        organizationDAO.delete(tx, organization);
        tx.commit();
        managementEventService.postEvent(DELETED, organization);
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

  private void fillAllParentOrgs(final Owner owner, Map<String, Organization> parentOrgs) {
    if (owner == null || owner.getParentOwnerId() == null) {
      return;
    }

    String parentOrgId = owner.getParentOwnerId();

    while (parentOrgId != null) {
      if (parentOrgs.containsKey(parentOrgId)) {
        break;
      }
      parentOrgId = parentOrgs.computeIfAbsent(parentOrgId, organizationDAO::getById).getParentOwnerId();
    }
  }

  public Map<String, Organization> getAllParentOrgsNoAuthz(Collection<? extends Owner> owners) {
    return getAllParentOrgsNoAuthz(owners, null);
  }

  public Map<String, Organization> getAllParentOrgsNoAuthz(
      Collection<? extends Owner> owners,
      Map<String, Organization> knownParentOrgs)
  {
    if (CollectionUtils.isEmpty(owners)) {
      return Collections.emptyMap();
    }

    Map<String, Organization> parentOrgs = MapUtils.isNotEmpty(knownParentOrgs) ? knownParentOrgs : new HashMap<>();
    owners.forEach(owner -> fillAllParentOrgs(owner, parentOrgs));
    return parentOrgs;
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
