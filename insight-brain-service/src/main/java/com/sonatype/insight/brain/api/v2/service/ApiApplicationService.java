/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.api.v2.ApiApplicationAdapter;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationListDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleListDTO;
import com.sonatype.insight.brain.audit.ApplicationCategoryAuditDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.InvalidApplicationException;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.dataaccess.tag.ApplicationTagDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.organization.ApplicationHelper;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzFilter;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.11.0
 */
@Named
public class ApiApplicationService
{
  private final ApiApplicationAdapter apiApplicationAdapter;

  private final ApiApplicationTagAdapter apiApplicationTagAdapter;

  private final ApplicationTagDAO applicationTagDAO;

  private final RoleDAO roleDAO;

  private final ApiRoleAdapter roleAdapter;

  private final ApplicationHelper applicationHelper;

  private final ApplicationDAO applicationDAO;

  private final TagDAO tagDAO;

  private final OrganizationDAO organizationDAO;

  @Inject
  public ApiApplicationService(final ApiApplicationAdapter apiApplicationAdapter,
                               final ApiApplicationTagAdapter apiApplicationTagAdapter,
                               final ApplicationTagDAO applicationTagDAO,
                               final RoleDAO roleDAO,
                               final ApplicationDAO applicationDAO,
                               final ApiRoleAdapter roleAdapter,
                               final ApplicationHelper applicationHelper,
                               final TagDAO tagDAO,
                               final OrganizationDAO organizationDAO)
  {
    this.apiApplicationAdapter = apiApplicationAdapter;
    this.apiApplicationTagAdapter = apiApplicationTagAdapter;
    this.applicationTagDAO = applicationTagDAO;
    this.applicationDAO = applicationDAO;
    this.roleDAO = roleDAO;
    this.roleAdapter = roleAdapter;
    this.applicationHelper = applicationHelper;
    this.tagDAO = tagDAO;
    this.organizationDAO = organizationDAO;
  }

  @Authorize(permission = Permission.READ)
  public ApiApplicationDTO getApplicationById(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) final String applicationId)
  {
    Application application = applicationHelper.getApplicationByIdNotNull(applicationId);
    return convertApplicationToDTO(application);
  }

  /**
   * Get the application DTO list filtered by the set of publicIds.
   * If the publicIds is empty then all applications are returned.
   *
   * @param publicIds The set of public ids to filter on (cannot be null)
   * @return The application DTO list found
   */
  public ApiApplicationListDTO getApplicationDTOs(final Set<String> publicIds) {
    List<Application> applications = getApplications(publicIds);
    List<ApiApplicationDTO> applicationDTOs = new ArrayList<>(applications.size());
    for (Application application : applications) {
      ApiApplicationDTO apiApplicationDTO = convertApplicationToDTO(application);
      applicationDTOs.add(apiApplicationDTO);
    }
    ApiApplicationListDTO applicationListDTO = new ApiApplicationListDTO();
    applicationListDTO.applications = applicationDTOs;
    return applicationListDTO;
  }

  public ApiApplicationDTO addApplication(final ApiApplicationDTO applicationDTO) {

    Application application = apiApplicationAdapter.convertFromDTO(applicationDTO);

    try (TransactionContext tx = applicationTagDAO.createTransactionContext()) {
      tx.begin();
      AuditData.get().setParentOrganization(organizationDAO.getById(application.getParentOwnerId()));
      application = addApplication(tx, application);
      List<ApplicationTag> applicationTags = apiApplicationTagAdapter.convertFromDTO(application.getId(),
          applicationDTO.applicationTags);
      addTags(tx, applicationTags, application);

      tx.commit();
      AuditData.get().commitSubEvents();
      AuditData.get().setApplicationWithDetails(application);
    }

    ApiApplicationDTO apiApplicationDTO = apiApplicationAdapter.convertToDTO(application);
    List<ApplicationTag> tags = applicationTagDAO.getByApplicationId(application.getId());
    apiApplicationDTO.applicationTags = apiApplicationTagAdapter.convertToDTO(tags);

    return apiApplicationDTO;
  }

  public ApiApplicationDTO updateApplication(final ApiApplicationDTO applicationDTO) {
    Application application = apiApplicationAdapter.convertFromDTO(applicationDTO);
    try (TransactionContext tx = applicationTagDAO.createTransactionContext()) {
      tx.begin();

      AuditData.get().setParentOrganization(organizationDAO.getById(application.getParentOwnerId()));
      application = updateApplication(tx, application);
      List<ApplicationTag> applicationTags = apiApplicationTagAdapter.convertFromDTO(application.getId(),
          applicationDTO.applicationTags);
      updateTags(tx, application, applicationTags);

      tx.commit();
      AuditData.get().commitSubEvents();
      AuditData.get().setApplicationWithDetails(application);
    }

    ApiApplicationDTO apiApplicationDTO = apiApplicationAdapter.convertToDTO(application);
    List<ApplicationTag> tags = applicationTagDAO.getByApplicationId(application.getId());
    apiApplicationDTO.applicationTags = apiApplicationTagAdapter.convertToDTO(tags);

    return apiApplicationDTO;
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
    return application;
  }

  public ApiRoleListDTO getApplicationRoles() {
    List<Role> roles = roleDAO.getApplicationRoles();
    return roleAdapter.convertToDTO(roles);
  }

  /**
   * Get all applications filtered by the set of publicIdsFilter.
   * If the publicIdsFilter is empty then all applications are returned
   *
   * @param publicIdsFilter The set of public ids to filter on (cannot be null)
   * @return The list of applications found
   */
  @AuthzFilter(permission = Permission.READ, context = AuthzFilter.Context.APPLICATION)
  public List<Application> getApplications(final Set<String> publicIdsFilter) {
    List<Application> applications;
    if (publicIdsFilter.isEmpty()) {
      applications = applicationDAO.getAll();
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

  private ApiApplicationDTO convertApplicationToDTO(final Application application) {
    ApiApplicationDTO apiApplicationDTO = apiApplicationAdapter.convertToDTO(application);
    List<ApplicationTag> tags = applicationTagDAO.getByApplicationId(application.getId());
    apiApplicationDTO.applicationTags = apiApplicationTagAdapter.convertToDTO(tags);

    return apiApplicationDTO;
  }
}
