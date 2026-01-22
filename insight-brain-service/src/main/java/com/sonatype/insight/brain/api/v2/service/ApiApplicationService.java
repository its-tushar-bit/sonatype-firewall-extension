/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.api.v2.ApiApplicationAdapter;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationCategoriesDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationCategoriesListDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationListDTO;
import com.sonatype.insight.brain.audit.ApplicationCategoryAuditDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.InvalidApplicationException;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.tag.ApplicationTagDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.organization.ApplicationHelper;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzFilter;
import com.sonatype.insight.brain.tag.TagService;
import com.sonatype.insight.brain.telemetry.OwnerMaintenanceTelemetry;
import com.sonatype.insight.brain.telemetry.OwnerMaintenanceTelemetryCreator;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.11.0
 */
@Named
public class ApiApplicationService
{
  private final ApplicationTagDAO applicationTagDAO;

  private final ApplicationHelper applicationHelper;

  private final ApplicationDAO applicationDAO;

  private final TagDAO tagDAO;

  private final OrganizationDAO organizationDAO;

  private final OwnerMaintenanceTelemetryCreator ownerMaintenanceTelemetryCreator;

  @Inject
  public ApiApplicationService(
      final ApplicationTagDAO applicationTagDAO,
      final ApplicationDAO applicationDAO,
      final ApplicationHelper applicationHelper,
      final TagDAO tagDAO,
      final OrganizationDAO organizationDAO,
      final OwnerMaintenanceTelemetryCreator ownerMaintenanceTelemetryCreator)
  {
    this.applicationTagDAO = applicationTagDAO;
    this.applicationDAO = applicationDAO;
    this.applicationHelper = applicationHelper;
    this.tagDAO = tagDAO;
    this.organizationDAO = organizationDAO;
    this.ownerMaintenanceTelemetryCreator = ownerMaintenanceTelemetryCreator;
  }

  @Authorize(permission = Permission.READ)
  public ApiApplicationDTO getApplicationById(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) final String applicationId)
  {
    Application application = applicationHelper.getApplicationByIdNotNull(applicationId);
    return ApiApplicationAdapter.convertToDTO(application, applicationTagDAO.getByApplicationId(application.getId()));
  }

  /**
   * Get the application DTO list filtered by the set of publicIds.
   * If the publicIds is empty then all applications are returned.
   *
   * @param publicIds The set of public ids to filter on (cannot be null)
   * @return The application DTO list found
   */
  public ApiApplicationListDTO getApplicationDTOs(final Set<String> publicIds) {
    List<Application> applications = getApplicationsWithReadPermission(publicIds);
    List<ApiApplicationDTO> applicationDTOs = new ArrayList<>(applications.size());

    List<String> applicationIds = applications.stream().map(Application::getId).collect(Collectors.toList());
    Map<String, List<ApplicationTag>> applicationTagsByAppId = applicationTagDAO.getByApplicationIds(applicationIds)
        .stream().collect(Collectors.groupingBy(ApplicationTag::getApplicationId));

    for (Application application : applications) {
      ApiApplicationDTO apiApplicationDTO =
          ApiApplicationAdapter.convertToDTO(application, applicationTagsByAppId.get(application.getId()));
      applicationDTOs.add(apiApplicationDTO);
    }
    ApiApplicationListDTO applicationListDTO = new ApiApplicationListDTO();
    applicationListDTO.applications = applicationDTOs;
    return applicationListDTO;
  }

  /**
   * @since 1.102
   */
  @Authorize(permission = Permission.READ)
  public ApiApplicationListDTO getApplicationsByOrganizationId(
      @AuthzContext(AuthzContext.Key.ORGANIZATION_ID) String organizationId)
  {
    ApiApplicationListDTO apiApplicationListDTO = new ApiApplicationListDTO();
    List<Application> applications = applicationDAO.getByOrganizationId(organizationId);
    List<String> applicationIds = applications.stream().map(Application::getId).collect(Collectors.toList());
    Map<String, List<ApplicationTag>> applicationTagsByAppId = applicationTagDAO.getByApplicationIds(applicationIds)
        .stream().collect(Collectors.groupingBy(ApplicationTag::getApplicationId));
    apiApplicationListDTO.applications = applications.stream()
        .map(app -> ApiApplicationAdapter.convertToDTO(app, applicationTagsByAppId.get(app.getId())))
        .collect(Collectors.toList());
    return apiApplicationListDTO;
  }

  public ApiApplicationDTO addApplication(final ApiApplicationDTO applicationDTO) {

    Application application = ApiApplicationAdapter.convertFromDTO(applicationDTO);

    try (TransactionContext tx = applicationTagDAO.createTransactionContext()) {
      tx.begin();
      AuditData.get().setParentOrganization(organizationDAO.getById(application.getParentOwnerId()));
      application = addApplication(tx, application);
      List<ApplicationTag> applicationTags = ApiApplicationTagAdapter.convertFromDTO(application.getId(),
          applicationDTO.applicationTags);
      addTags(tx, applicationTags, application);

      tx.commit();
      AuditData.get().commitSubEvents();
      AuditData.get().setApplicationWithDetails(application);
    }

    return ApiApplicationAdapter.convertToDTO(application, applicationTagDAO.getByApplicationId(application.getId()));
  }

  public ApiApplicationDTO updateApplication(final ApiApplicationDTO applicationDTO) {
    Application application = ApiApplicationAdapter.convertFromDTO(applicationDTO);
    try (TransactionContext tx = applicationTagDAO.createTransactionContext()) {
      tx.begin();

      AuditData.get().setParentOrganization(organizationDAO.getById(application.getParentOwnerId()));
      application = updateApplication(tx, application);
      List<ApplicationTag> applicationTags =
          ApiApplicationTagAdapter.convertFromDTO(application.getId(), applicationDTO.applicationTags);
      updateTags(tx, application, applicationTags);

      tx.commit();
      AuditData.get().commitSubEvents();
      AuditData.get().setApplicationWithDetails(application);
    }

    return ApiApplicationAdapter.convertToDTO(application, applicationTagDAO.getByApplicationId(application.getId()));
  }

  @Authorize(permission = Permission.WRITE)
  public void deleteApplication(@AuthzContext(AuthzContext.Key.APPLICATION_ID) final String applicationId)
      throws IOException
  {
    applicationHelper.deleteApplicationById(applicationId);
  }

  @Authorize(permission = Permission.ADD_APPLICATION)
  Application addApplication(final TransactionContext tx,
                             @AuthzContext(AuthzContext.Key.APPLICATION_OWNER) final Application application)
  {
    return applicationHelper.addApplication(tx, application);
  }

  @Authorize(permission = Permission.WRITE)
  Application updateApplication(final TransactionContext tx,
                                @AuthzContext(AuthzContext.Key.APPLICATION) final Application application)
  {
    applicationDAO.update(tx, application);
    ownerMaintenanceTelemetryCreator.sendOwnerMaintenanceTelemetry(application,
        OwnerMaintenanceTelemetry.TYPE_UPDATE);
    return application;
  }

  /**
   * Get all applications filtered by the set of publicIdsFilter.
   * If the publicIdsFilter is empty then all applications are returned
   *
   * @param publicIdsFilter The set of public ids to filter on (cannot be null)
   * @return The list of applications found
   */
  @AuthzFilter(permission = Permission.READ, context = AuthzFilter.Context.APPLICATION)
  List<Application> getApplicationsWithReadPermission(final Set<String> publicIdsFilter) {
    List<Application> applications;
    if (publicIdsFilter.isEmpty()) {
      applications = applicationDAO.getAllWithoutRelatedRepositories();
    }
    else {
      applications = applicationDAO.getByPublicIds(publicIdsFilter);
    }

    return applications;
  }

  private void addTags(final TransactionContext tx,
                       final List<ApplicationTag> applicationTags,
                       Application application)
  {
    List<Tag> tags = new ArrayList<>();
    for (ApplicationTag applicationTag : applicationTags) {
      if (applicationTag.getTagId() == null) {
        throw new InvalidApplicationException("Application tag must have an ID.");
      }
      applicationTagDAO.insert(tx, applicationTag);
      tags.add(tagDAO.getByIdNotNull(applicationTag.getTagId()));
    }
    auditConfigureApplicationCategory(tags, application, false);
  }

  private void auditConfigureApplicationCategory(final List<Tag> tags,
                                                 final Application application,
                                                 final boolean auditEmptyCategories)
  {
    if (auditEmptyCategories || !tags.isEmpty()) {
      try (AuditSession auditSession = AuditData.get()
          .recordSubEvent(AuditEvent.CONFIGURE_APPLICATION_CATEGORY, false)) {
        AuditData.get().setApplication(application)
            .setApplicationCategories(ApplicationCategoryAuditDTO.transcribe(tags));
      }
    }
  }

  private void updateTags(final TransactionContext tx,
                          final Application application,
                          final List<ApplicationTag> applicationTags)
  {
    // Delete existing tags
    for (ApplicationTag applicationTag : applicationTagDAO.getByApplicationId(tx, application.getId())) {
      applicationTagDAO.delete(tx, applicationTag);
    }
    // Now add the new tags
    List<Tag> tags = new ArrayList<>();
    for (ApplicationTag applicationTag : applicationTags) {
      if (applicationTag.getTagId() == null) {
        throw new InvalidApplicationException("Application tag must have an ID.");
      }
      applicationTagDAO.insert(tx, applicationTag);
      tags.add(tagDAO.getByIdNotNull(applicationTag.getTagId()));
    }
    auditConfigureApplicationCategory(tags, application, true);
  }

  public ApiApplicationCategoriesListDTO getApplicationsWithAppliedCategories(Set<String> publicIdsFilter) {
    ApiApplicationCategoriesListDTO results = new ApiApplicationCategoriesListDTO();

    List<Application> applications = getApplicationsWithReadPermission(publicIdsFilter);
    List<String> applicationIds = applications.stream().map(Application::getId).collect(Collectors.toList());
    List<ApplicationTag> appTags = applicationTagDAO.getByApplicationIds(applicationIds);
    Map<String, List<ApplicationTag>> applicationTagsByAppId = 
        appTags.stream().collect(Collectors.groupingBy(ApplicationTag::getApplicationId));
    List<Tag> tags =
        tagDAO.getByIds(appTags.stream().map(ApplicationTag::getTagId).distinct().collect(Collectors.toList()));
    Map<String, Tag> tagsById = tags.stream().collect(Collectors.toMap(Tag::getId, Function.identity()));

    for (Application application : applications) {
      ApiApplicationCategoriesDTO result = new ApiApplicationCategoriesDTO();
      ApiApplicationAdapter.populateDTO(result, application);
      List<ApplicationTag> appTagsForCurrentApp = applicationTagsByAppId.get(application.getId());
      if (appTagsForCurrentApp != null) {
        result.categories = appTagsForCurrentApp.stream() //
            .map(appTag -> tagsById.get(appTag.getTagId())) //
            .map(TagService::toDTO) //
            .collect(Collectors.toList());
      }
      results.applications.add(result);
    }
    return results;
  }
}
