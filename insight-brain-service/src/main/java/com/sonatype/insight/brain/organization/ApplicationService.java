/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
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

import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.git.event.SourceControlEventFinder;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLoggerFactory;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzFilter;
import com.sonatype.insight.brain.security.UserDirectory;
import com.sonatype.insight.brain.webhook.ManagementEventService;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.collections.CollectionUtils;
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

  private final UserDirectory userDirectory;

  private final ApplicationCleaner applicationCleaner;

  private final ApplicationHelper applicationHelper;

  private final ManagementEventService managementEventService;

  private final OrganizationDAO organizationDAO;

  private final ScanPolicyEvaluator scanPolicyEvaluator;

  private final PolicyViolationLoggerFactory policyViolationLoggerFactory;

  private final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  private final SourceControlEventFinder sourceControlEventFinder;

  @Inject
  public ApplicationService(
      ApplicationDAO applicationDAO,
      UserDirectory userDirectory,
      final ApplicationCleaner applicationCleaner,
      final ApplicationHelper applicationHelper,
      final ManagementEventService managementEventService,
      final OrganizationDAO organizationDAO,
      ScanPolicyEvaluator scanPolicyEvaluator,
      final PolicyViolationLoggerFactory policyViolationLoggerFactory,
      final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO,
      final SourceControlEventFinder sourceControlEventFinder)
  {
    this.applicationDAO = applicationDAO;
    this.userDirectory = userDirectory;
    this.applicationCleaner = applicationCleaner;
    this.applicationHelper = applicationHelper;
    this.managementEventService = managementEventService;
    this.organizationDAO = organizationDAO;
    this.scanPolicyEvaluator = scanPolicyEvaluator;
    this.policyViolationLoggerFactory = policyViolationLoggerFactory;
    this.systemConfigurationPropertyDAO = systemConfigurationPropertyDAO;
    this.sourceControlEventFinder = sourceControlEventFinder;
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

  public ApplicationManagementSummaryDTO getApplicationManagementSummary(String applicationPublicId) {
    final Application application = getApplicationByPublicIdNotNull(applicationPublicId);
    return getApplicationManagementSummary(application);
  }

  public List<ApplicationManagementSummaryDTO> getApplicationManagementSummaries(
      String nameFilter,
      ApplicationManagementSummaryOrder order,
      Integer page,
      Integer pageSize)
  {
    validateReportsListFeatureEnabled();

    if (page == null || pageSize == null) {
      throw new BadRequestException("Request must include required query parameters page and pageSize.");
    }

    if (nameFilter != null && nameFilter.isEmpty()) {
      nameFilter = null;
    }

    List<Application> applications = getApplications();
    List<ApplicationManagementSummaryDTO> applicationManagementSummaryDTOs =
        ApplicationAdapter.getInstance(userDirectory).createApplicationManagementSummaries(applications, nameFilter);

    Comparator<ApplicationManagementSummaryDTO> comparator;
    switch (order) {
      case APP_NAME_ASC:
        comparator = Comparator.comparing(ApplicationManagementSummaryDTO::getName, String.CASE_INSENSITIVE_ORDER);
        break;
      case APP_NAME_DESC:
        comparator =
            Comparator.comparing(ApplicationManagementSummaryDTO::getName, String.CASE_INSENSITIVE_ORDER).reversed();
        break;
      case ORG_NAME_ASC:
        comparator =
            Comparator.comparing(ApplicationManagementSummaryDTO::getOrganizationName, String.CASE_INSENSITIVE_ORDER);
        break;
      case ORG_NAME_DESC:
        comparator =
            Comparator.comparing(ApplicationManagementSummaryDTO::getOrganizationName, String.CASE_INSENSITIVE_ORDER)
                .reversed();
        break;
      default:
        throw new IllegalArgumentException("Unknown ordering: " + order);
    }
    applicationManagementSummaryDTOs.sort(comparator);

    applicationManagementSummaryDTOs = applicationManagementSummaryDTOs.subList((page - 1) * pageSize,
        Math.min(page * pageSize, applicationManagementSummaryDTOs.size()));

    loadPolicyEvaluations(applicationManagementSummaryDTOs);
    loadPolicyEvaluationsResults(applicationManagementSummaryDTOs);
    loadPendingSourceControlPolicyEvaluations(applicationManagementSummaryDTOs);

    return applicationManagementSummaryDTOs;
  }

  private ApplicationManagementSummaryDTO getApplicationManagementSummary(final Application application) {
    final ApplicationManagementSummaryDTO applicationManagement =
        ApplicationAdapter.getInstance(userDirectory).createApplicationManagementSummary(application);
    loadPolicyEvaluations(Arrays.asList(applicationManagement));

    return applicationManagement;
  }

  private void loadPendingSourceControlPolicyEvaluations(
      List<ApplicationManagementSummaryDTO> applicationManagementSummaries)
  {
    Map<String, SourceControlEvent> applicationEventMap =
        sourceControlEventFinder.getPendingOrInProgressSourceControlEvaluationEvents();
    for (ApplicationManagementSummaryDTO summaryDTO : applicationManagementSummaries) {
      summaryDTO.setHasPendingSourceControlPolicyEvaluation(applicationEventMap.containsKey(summaryDTO.getId()));
    }
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
            .createPolicyEvaluationResult(policyEvaluation, false);

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
