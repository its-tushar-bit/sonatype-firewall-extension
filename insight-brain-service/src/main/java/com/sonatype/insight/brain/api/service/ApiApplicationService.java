/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.service;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;

import com.sonatype.insight.brain.api.ApiApplicationAdapter;
import com.sonatype.insight.brain.api.dto.ApiApplicationDTO;
import com.sonatype.insight.brain.api.dto.ApiApplicationListDTO;
import com.sonatype.insight.brain.api.dto.ApiRoleListDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.InvalidApplicationException;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.dataaccess.tag.ApplicationTagDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.brain.organization.ApplicationHelper;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzFilter;
import com.sonatype.insight.dataaccess.AbstractDAO;

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

  private final ApplicationDAO applicationDAO;

  private final ApiRoleAdapter roleAdapter;

  private final ApplicationHelper applicationHelper;

  @Inject
  public ApiApplicationService(final ApiApplicationAdapter apiApplicationAdapter,
      final ApiApplicationTagAdapter apiApplicationTagAdapter,
      final ApplicationTagDAO applicationTagDAO,
      final RoleDAO roleDAO,
      final ApplicationDAO applicationDAO,
      final ApiRoleAdapter roleAdapter,
      final ApplicationHelper applicationHelper)
  {
    this.apiApplicationAdapter = apiApplicationAdapter;
    this.apiApplicationTagAdapter = apiApplicationTagAdapter;
    this.applicationTagDAO = applicationTagDAO;
    this.roleDAO = roleDAO;
    this.applicationDAO = applicationDAO;
    this.roleAdapter = roleAdapter;
    this.applicationHelper = applicationHelper;
  }

  @Authorize(permission = Permission.READ)
  public ApiApplicationDTO getApplicationById(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) final String applicationId)
  {
    Application application = applicationHelper.getApplicationByIdNotNull(applicationId);
    return convertApplicationToDTO(application);
  }

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

  /**
   * Get all applications filtered by the set of publicIdsFilter.
   * If the publicIdsFilter is empty then all applications are returned
   *
   * @param publicIdsFilter The set of public ids to filter on (cannot be null)
   * @return The list of applications found
   */
  @AuthzFilter(permission = Permission.READ, context = AuthzFilter.Context.APPLICATION)
  List<Application> getApplications(final Set<String> publicIdsFilter) {
    List<Application> applications;
    if (publicIdsFilter.isEmpty()) {
      applications = applicationDAO.getAll();
    }
    else {
      applications = applicationDAO.getByPublicIds(publicIdsFilter);
    }

    return applications;
  }

  public ApiApplicationDTO addApplication(final ApiApplicationDTO applicationDTO) {

    Application application = apiApplicationAdapter.convertFromDTO(applicationDTO);

    EntityManager entityManager = applicationTagDAO.createEntityManager();
    try {
      entityManager.getTransaction().begin();

      application = addApplication(entityManager, application);
      List<ApplicationTag> applicationTags = apiApplicationTagAdapter
          .convertFromDTO(application.getId(), applicationDTO.applicationTags);
      addTags(entityManager, applicationTags);

      entityManager.getTransaction().commit();
    }
    finally {
      AbstractDAO.close(entityManager);
    }

    ApiApplicationDTO apiApplicationDTO = apiApplicationAdapter.convertToDTO(application);
    List<ApplicationTag> tags = applicationTagDAO.getByApplicationId(application.getId());
    apiApplicationDTO.applicationTags = apiApplicationTagAdapter.convertToDTO(tags);

    return apiApplicationDTO;
  }

  public ApiApplicationDTO updateApplication(final ApiApplicationDTO applicationDTO) {
    Application application = apiApplicationAdapter.convertFromDTO(applicationDTO);
    EntityManager entityManager = applicationTagDAO.createEntityManager();
    try {
      entityManager.getTransaction().begin();

      application = updateApplication(entityManager, application);
      List<ApplicationTag> applicationTags = apiApplicationTagAdapter
          .convertFromDTO(application.getId(), applicationDTO.applicationTags);
      updateTags(entityManager, application, applicationTags);

      entityManager.getTransaction().commit();
    }
    finally {
      AbstractDAO.close(entityManager);
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

  @Authorize(permission = Permission.WRITE)
  Application addApplication(final EntityManager entityManager,
      @AuthzContext(AuthzContext.Key.APPLICATION_OWNER) final Application application)
  {
    return applicationHelper.addApplication(entityManager, application);
  }

  @Authorize(permission = Permission.WRITE)
  Application updateApplication(final EntityManager entityManager,
      @AuthzContext(AuthzContext.Key.APPLICATION) final Application application)
  {
    applicationDAO.update(entityManager, application);
    return application;
  }

  public ApiRoleListDTO getApplicationRoles() {
    List<Role> roles = roleDAO.getApplicationRoles();
    return roleAdapter.convertToDTO(roles);
  }

  private void addTags(final EntityManager entityManager, final List<ApplicationTag> applicationTags) {
    for (ApplicationTag applicationTag : applicationTags) {
      if (applicationTag.getTagId() == null) {
        throw new InvalidApplicationException("Application tag must have an ID.");
      }
      applicationTagDAO.insert(entityManager, applicationTag);
    }
  }

  private void updateTags(final EntityManager entityManager, final Application application,
      final List<ApplicationTag> applicationTags)
  {
    // Delete existing tags
    for (ApplicationTag applicationTag : applicationTagDAO.getByApplicationId(entityManager, application.getId())) {
      applicationTagDAO.delete(entityManager, applicationTag);
    }
    // Now add the new tags
    for (ApplicationTag applicationTag : applicationTags) {
      if (applicationTag.getTagId() == null) {
        throw new InvalidApplicationException("Application tag must have an ID.");
      }
      applicationTagDAO.insert(entityManager, applicationTag);
    }
  }

  private ApiApplicationDTO convertApplicationToDTO(final Application application) {
    ApiApplicationDTO apiApplicationDTO = apiApplicationAdapter.convertToDTO(application);
    List<ApplicationTag> tags = applicationTagDAO.getByApplicationId(application.getId());
    apiApplicationDTO.applicationTags = apiApplicationTagAdapter.convertToDTO(tags);
    return apiApplicationDTO;
  }
}
