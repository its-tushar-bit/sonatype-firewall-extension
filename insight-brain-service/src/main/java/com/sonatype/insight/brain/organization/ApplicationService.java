/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLoggerFactory;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzFilter;
import com.sonatype.insight.brain.telemetry.OwnerMaintenanceTelemetry;
import com.sonatype.insight.brain.telemetry.OwnerMaintenanceTelemetryCreator;
import com.sonatype.insight.brain.webhook.ManagementEventService;
import com.sonatype.insight.brain.webhook.OrganizationApplicationManagementEventService;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.webhook.EventAction.CREATED;
import static com.sonatype.insight.brain.webhook.EventAction.DELETED;
import static com.sonatype.insight.brain.webhook.EventAction.UPDATED;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

@Named
public class ApplicationService
{
  private static final Logger log = LoggerFactory.getLogger(ApplicationService.class);

  private final ApplicationDAO applicationDAO;

  private final ApplicationCleaner applicationCleaner;

  private final ApplicationHelper applicationHelper;

  private final ManagementEventService managementEventService;

  private final OrganizationDAO organizationDAO;

  private final PolicyViolationLoggerFactory policyViolationLoggerFactory;

  private final OwnerMaintenanceTelemetryCreator ownerMaintenanceTelemetryCreator;

  private final OrganizationApplicationManagementEventService organizationApplicationManagementEventService;

  @Inject
  public ApplicationService(
      ApplicationDAO applicationDAO,
      final ApplicationCleaner applicationCleaner,
      final ApplicationHelper applicationHelper,
      final ManagementEventService managementEventService,
      final OrganizationDAO organizationDAO,
      final PolicyViolationLoggerFactory policyViolationLoggerFactory,
      final OrganizationApplicationManagementEventService organizationApplicationManagementEventService,
      final OwnerMaintenanceTelemetryCreator ownerMaintenanceTelemetryCreator)
  {
    this.applicationDAO = applicationDAO;
    this.applicationCleaner = applicationCleaner;
    this.applicationHelper = applicationHelper;
    this.managementEventService = managementEventService;
    this.organizationDAO = organizationDAO;
    this.policyViolationLoggerFactory = policyViolationLoggerFactory;
    this.organizationApplicationManagementEventService = organizationApplicationManagementEventService;
    this.ownerMaintenanceTelemetryCreator = ownerMaintenanceTelemetryCreator;
  }

  public String validateApplicationPublicId(final String applicationPublicId) {
    try {
      if (getApplicationByPublicIdForWrite(applicationPublicId) == null) {
        return "Invalid application ID " + applicationPublicId + ".";
      }
    }
    catch (NotFoundException e) {
      // The auth context can throw not found exception
      return "Invalid application ID " + applicationPublicId + ".";
    }

    log.debug("Found application with public id {}", applicationPublicId);
    return "OK";
  }

  /**
   * @since 1.14.0
   */
  @AuthzFilter(permission = Permission.EVALUATE_COMPONENT, context = AuthzFilter.Context.APPLICATION)
  protected List<Application> getApplicationsForEvaluateComponent() {
    return applicationDAO.getAll();
  }

  @Authorize(permission = Permission.WRITE)
  Application getApplicationByPublicIdForWrite(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId)
  {
    return applicationDAO.getByPublicId(applicationPublicId);
  }

  public Map<String, String> getApplicationNamesForEvaluateComponent() {
    List<Application> applications = getApplicationsForEvaluateComponent();
    Map<String, String> applicationPublicIDNamePairs = new LinkedHashMap<>();

    log.debug("getApplicationNamesForEvaluateComponent: Found {} applications.", applications.size());
    for (Application application : applications) {
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
  Application getApplicationByPublicIdForRead(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId)
  {
    return applicationDAO.getByPublicId(applicationPublicId);
  }

  @Authorize(permission = Permission.LEGAL_REVIEWER)
  Application getApplicationByPublicIdForLegalReviewer(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId)
  {
    return applicationDAO.getByPublicId(applicationPublicId);
  }

  @Authorize(permission = Permission.READ)
  public Application getApplicationByIdForRead(@AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId) {
    return applicationDAO.getById(applicationId);
  }

  @AuthzFilter(permission = Permission.READ, context = AuthzFilter.Context.APPLICATION)
  public List<Application> getApplicationsByIdsAndOrganizationIdsAndTagIds(
      @Nullable final Set<String> organizationIds,
      @Nullable final Set<String> applicationIds,
      @Nullable final Set<String> tagIds)
  {
    return getApplicationsByIdsAndOrganizationIdsAndTagIdsNoAuthz(organizationIds, applicationIds, tagIds);
  }

  public List<Application> getApplicationsByIdsAndOrganizationIdsAndTagIdsNoAuthz(
      @Nullable final Set<String> organizationIds,
      @Nullable final Set<String> applicationIds,
      @Nullable final Set<String> tagIds)
  {
    if (isEmpty(applicationIds) && isEmpty(tagIds) && isEmpty(organizationIds)) {
      // none filled
      return applicationDAO.getAll();
    }

    return getAppsByIds(organizationIds, applicationIds, tagIds);
  }

  public List<Application> getAppsByIds(
      final Set<String> organizationIds,
      final Set<String> applicationIds,
      final Set<String> tagIds)
  {
    // We don't want to modify the original
    Set<String> internalApplicationIds = new HashSet<>();
    if (applicationIds != null) {
      internalApplicationIds.addAll(applicationIds);
    }
    // Add organizationIds
    internalApplicationIds.addAll(getApplicationIdsByOrganizationIds(organizationIds));

    if (isEmpty(internalApplicationIds) && !isEmpty(tagIds)) {
      return applicationDAO.getByTagIds(tagIds);
    }
    else if (isEmpty(tagIds)) {
      return applicationDAO.getByIds(internalApplicationIds);
    }
    else {
      // both filled
      return applicationDAO.getByIdsAndTagIds(internalApplicationIds, tagIds);
    }
  }

  public List<Application> getByPublicIdsNoAuthz(Set<String> applicationPublicIds) {
    return applicationDAO.getByPublicIds(applicationPublicIds);
  }

  public Set<Organization> getParentOrganizationsForApplicationsNoAuthz(final List<Application> applications) {
    if (CollectionUtils.isEmpty(applications)) {
      return Collections.emptySet();
    }

    /*
     * We are using DAO call to get all orgs since it is possible
     * that a user might not have permission for the parent org of given application.
     * However, we still want to show waivers being applied to that parent org
     * as they will also apply to given child application.
     * */
    final Map<String, Organization> allOrganizations = organizationDAO.getAll()
        .stream()
        .collect(Collectors.toMap(Organization::getId, Function.identity()));

    return applications
        .stream()
        .map(application -> allOrganizations.get(application.getOrganizationId()))
        .collect(Collectors.toSet());
  }

  public List<Application> getOwnerApplicationsByIdsOrTagIds(
      final Set<String> applicationIds,
      final Set<String> tagIds)
  {
    return getApplicationsByIdsAndOrganizationIdsAndTagIds(null, applicationIds, tagIds);
  }

  @AuthzFilter(permission = Permission.READ, context = AuthzFilter.Context.APPLICATION)
  public List<Application> getApplications() {
    return applicationDAO.getAll();
  }

  @AuthzFilter(permission = Permission.READ, context = AuthzFilter.Context.APPLICATION)
  public List<Application> getApplicationsOrderedByName() {
    return applicationDAO.getAllOrderedByName();
  }

  @Authorize(permission = Permission.ADD_APPLICATION)
  public Application addApplication(@AuthzContext(AuthzContext.Key.APPLICATION_OWNER) final Application application) {
    applicationHelper.addApplication(application);
    AuditData.get().setApplicationWithDetails(application);

    // Org app summary event post for application insert is in ApplicationHelper to cover SCM importing
    managementEventService.postEvent(CREATED, application);

    return application;
  }

  @Authorize(permission = Permission.WRITE)
  public Application updateApplication(@AuthzContext(AuthzContext.Key.APPLICATION) Application application) {
    applicationDAO.update(application);
    AuditData.get().setParentOrganization(organizationDAO.getByIdNotNull(application.getParentOwnerId()));
    ownerMaintenanceTelemetryCreator.sendOwnerMaintenanceTelemetry(application,
        OwnerMaintenanceTelemetry.TYPE_UPDATE);

    managementEventService.postEvent(UPDATED, application);
    organizationApplicationManagementEventService.postEvent();

    return application;
  }

  @Authorize(permission = Permission.WRITE)
  public void deleteApplicationByPublicId(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) final String applicationPublicId)
      throws IOException
  {
    Application application;
    try (TransactionContext tx = applicationDAO.createTransactionContext()) {
      tx.begin();
      application = applicationDAO.getByPublicIdNotNull(tx, applicationPublicId);
      AuditData.get()
          .setApplicationWithDetails(application)
          .setParentOrganization(organizationDAO.getByIdNotNull(application.getParentOwnerId()));
      applicationCleaner.delete(tx, application);
      tx.commit();

      policyViolationLoggerFactory.newLogger(new Date(), application).logClearEvent();
    }
    managementEventService.postEvent(DELETED, application);
    organizationApplicationManagementEventService.postEvent();
  }

  public Set<String> getApplicationIdsByOrganizationIds(Set<String> organizationIds) {
    Set<String> applicationIds = new HashSet<>();
    if (organizationIds != null) {
      for (String organizationId : organizationIds) {
        for (Application application : applicationDAO.getByOrganizationId(organizationId)) {
          applicationIds.add(application.getId());
        }
      }
    }
    return applicationIds;
  }
}
