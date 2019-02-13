/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLoggerFactory;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzFilter;
import com.sonatype.insight.brain.webhook.ManagementEventService;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

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

  @Inject
  public ApplicationService(ApplicationDAO applicationDAO,
                            final ApplicationCleaner applicationCleaner,
                            final ApplicationHelper applicationHelper,
                            final ManagementEventService managementEventService,
                            final OrganizationDAO organizationDAO,
                            final PolicyViolationLoggerFactory policyViolationLoggerFactory)
  {
    this.applicationDAO = applicationDAO;
    this.applicationCleaner = applicationCleaner;
    this.applicationHelper = applicationHelper;
    this.managementEventService = managementEventService;
    this.organizationDAO = organizationDAO;
    this.policyViolationLoggerFactory = policyViolationLoggerFactory;
  }

  public String validateApplicationPublicId(final String applicationPublicId) {
    try {
      if (getApplicationByPublicIdAllowAnonymous(applicationPublicId) == null) {
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
   *        Allows anonymous access. Only for use by the clients that evaluate components.
   */
  @AuthzFilter(permission = Permission.EVALUATE_COMPONENT, context = AuthzFilter.Context.APPLICATION,
      anonymousAllowed = true)
  protected List<Application> getApplicationsForEvaluateComponent() {
    return applicationDAO.getAll();
  }

  /**
   * @since 1.14.0
   *        Allows anonymous access. Only for use by the clients.
   */
  @Authorize(permission = Permission.WRITE, anonymousAllowed = true)
  @SuppressWarnings("checkstyle:LineLength")
  protected Application getApplicationByPublicIdAllowAnonymous(@AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId) {
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
  @SuppressWarnings("checkstyle:LineLength")
  public Application getApplicationByPublicIdNotNull(@AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId) {
    return applicationDAO.getByPublicIdNotNull(applicationPublicId);
  }

  @Authorize(permission = Permission.READ)
  @SuppressWarnings("checkstyle:LineLength")
  public Application getApplicationByPublicId(@AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId) {
    return applicationDAO.getByPublicId(applicationPublicId);
  }

  @AuthzFilter(permission = Permission.READ, context = AuthzFilter.Context.APPLICATION)
  public List<Application> getApplicationsByIdsAndOrganizationIdsAndTagIds(@Nullable final Set<String> organizationIds,
                                                                           @Nullable final Set<String> applicationIds,
                                                                           @Nullable final Set<String> tagIds)
  {
    if (isEmpty(applicationIds) && isEmpty(tagIds) && isEmpty(organizationIds)) {
      // none filled
      return applicationDAO.getAll();
    }

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

  @AuthzFilter(permission = Permission.READ, context = AuthzFilter.Context.APPLICATION)
  public List<Application> getApplications() {
    return applicationDAO.getAll();
  }

  @Authorize(permission = Permission.ADD_APPLICATION)
  public Application addApplication(@AuthzContext(AuthzContext.Key.APPLICATION_OWNER) final Application application) {
    applicationHelper.addApplication(application);
    AuditData.get().setApplicationWithDetails(application);

    managementEventService.postEvent(CREATED, application);

    return application;
  }

  @Authorize(permission = Permission.WRITE)
  public Application updateApplication(@AuthzContext(AuthzContext.Key.APPLICATION) Application application) {
    applicationDAO.update(application);
    AuditData.get().setParentOrganization(organizationDAO.getByIdNotNull(application.getParentOwnerId()));

    managementEventService.postEvent(UPDATED, application);

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
