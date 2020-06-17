/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLoggerFactory;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzFilter;
import com.sonatype.insight.brain.webhook.ManagementEventService;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.ConflictException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.REPORTS_LIST_DISABLED;
import static com.sonatype.insight.brain.webhook.EventAction.CREATED;
import static com.sonatype.insight.brain.webhook.EventAction.DELETED;
import static com.sonatype.insight.brain.webhook.EventAction.UPDATED;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

@Named
public class ApplicationService
{
  private static final Logger log = LoggerFactory.getLogger(ApplicationService.class);

  private final ApplicationDAO applicationDAO;

  private final ApplicationAdapter applicationAdapter;

  private final ApplicationCleaner applicationCleaner;

  private final ApplicationHelper applicationHelper;

  private final ManagementEventService managementEventService;

  private final OrganizationDAO organizationDAO;

  private final ScanPolicyEvaluator scanPolicyEvaluator;

  private final PolicyViolationLoggerFactory policyViolationLoggerFactory;

  private final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  @Inject
  public ApplicationService(ApplicationDAO applicationDAO,
                            ApplicationAdapter applicationAdapter,
                            final ApplicationCleaner applicationCleaner,
                            final ApplicationHelper applicationHelper,
                            final ManagementEventService managementEventService,
                            final OrganizationDAO organizationDAO,
                            ScanPolicyEvaluator scanPolicyEvaluator,
                            final PolicyViolationLoggerFactory policyViolationLoggerFactory,
                            final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO)
  {
    this.applicationDAO = applicationDAO;
    this.applicationAdapter = applicationAdapter;
    this.applicationCleaner = applicationCleaner;
    this.applicationHelper = applicationHelper;
    this.managementEventService = managementEventService;
    this.organizationDAO = organizationDAO;
    this.scanPolicyEvaluator = scanPolicyEvaluator;
    this.policyViolationLoggerFactory = policyViolationLoggerFactory;
    this.systemConfigurationPropertyDAO = systemConfigurationPropertyDAO;
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

  @Authorize(permission = Permission.READ)
  public Application getApplicationByIdForRead(@AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId) {
    return applicationDAO.getById(applicationId);
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

  public List<ApplicationManagementSummaryDTO> getApplicationManagementSummaries() {
    validateReportsListFeatureEnabled();
    return getApplicationManagementSummaries(getApplications());
  }

  public ApplicationManagementSummaryDTO getApplicationManagementSummary(String applicationPublicId) {
    final Application application = getApplicationByPublicIdNotNull(applicationPublicId);
    return getApplicationManagementSummary(application);
  }

  private List<ApplicationManagementSummaryDTO> getApplicationManagementSummaries(
      final List<Application> applications)
  {
    // Create the summary DTOs from the applications
    final List<ApplicationManagementSummaryDTO> applicationManagementSummaryDTOs = applicationAdapter
        .createApplicationManagementSummaries(applications);

    loadPolicyEvaluations(applicationManagementSummaryDTOs);
    loadPolicyEvaluationsResults(applicationManagementSummaryDTOs);

    return applicationManagementSummaryDTOs;
  }

  private ApplicationManagementSummaryDTO getApplicationManagementSummary(final Application application) {
    final ApplicationManagementSummaryDTO applicationManagement = applicationAdapter
        .createApplicationManagementSummary(application);
    loadPolicyEvaluations(Arrays.asList(applicationManagement));

    return applicationManagement;
  }

  private void loadPolicyEvaluations(List<ApplicationManagementSummaryDTO> applicationManagementSummaries) {
    Map<String, ApplicationManagementSummaryDTO> summariesByAppId = new HashMap<>();
    for (ApplicationManagementSummaryDTO summary : applicationManagementSummaries) {
      summariesByAppId.put(summary.getId(), summary);
      summary.setPolicyEvaluations(new HashMap<String, PolicyEvaluation>());
    }
    Set<String> stageTypeIds = new HashSet<>();
    for (StageType stageType : StageTypes.getAll()) {
      stageTypeIds.add(stageType.getId());
    }
    List<PolicyEvaluation> policyEvaluations = new PolicyEvaluationDAO().getLastByApplicationIds(summariesByAppId
        .keySet());
    for (PolicyEvaluation policyEvaluation : policyEvaluations) {
      if (stageTypeIds.contains(policyEvaluation.getStageTypeId())) {
        ApplicationManagementSummaryDTO summary = summariesByAppId.get(policyEvaluation.getApplicationId());
        summary.getPolicyEvaluations().put(policyEvaluation.getStageTypeId(), policyEvaluation);
      }
    }
  }

  private void loadPolicyEvaluationsResults(List<ApplicationManagementSummaryDTO> applicationManagementSummaries) {
    for (ApplicationManagementSummaryDTO applicationManagement : applicationManagementSummaries) {
      Map<String, PolicyEvaluationResult> policyEvaluationResults = new HashMap<>();
      for (PolicyEvaluation policyEvaluation : applicationManagement.getPolicyEvaluations().values()) {
        // Alerts are not needed by the Application Management UI and greatly bloat the JSON response
        // they are also time-consuming when we deal with thousands of applications/evaluations
        final PolicyEvaluationResult policyEvaluationResult = scanPolicyEvaluator
            .createPolicyEvaluationResult(policyEvaluation);

        policyEvaluationResults.put(policyEvaluation.getStageTypeId(), policyEvaluationResult);
      }
      applicationManagement.setPolicyEvaluationsResults(policyEvaluationResults);
    }
  }

  private void validateReportsListFeatureEnabled() {
    if (systemConfigurationPropertyDAO.getByName(REPORTS_LIST_DISABLED) != null) {
      throw new ConflictException("The reports list feature has been disabled.");
    }
  }
}
