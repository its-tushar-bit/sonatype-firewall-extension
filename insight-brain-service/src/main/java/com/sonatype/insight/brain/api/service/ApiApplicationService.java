/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.service;


import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;

import com.sonatype.insight.brain.api.ApiApplicationAdapter;
import com.sonatype.insight.brain.api.dto.ApiApplicationDTO;
import com.sonatype.insight.brain.api.dto.ApiRoleListDTO;
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

  private final ApiRoleAdapter roleAdapter;

  private final ApplicationHelper applicationHelper;

  @Inject
  public ApiApplicationService(final ApiApplicationAdapter apiApplicationAdapter,
      final ApiApplicationTagAdapter apiApplicationTagAdapter,
      final ApplicationTagDAO applicationTagDAO,
      final RoleDAO roleDAO,
      final ApiRoleAdapter roleAdapter,
      final ApplicationHelper applicationHelper)
  {
    this.apiApplicationAdapter = apiApplicationAdapter;
    this.apiApplicationTagAdapter = apiApplicationTagAdapter;
    this.applicationTagDAO = applicationTagDAO;
    this.roleDAO = roleDAO;
    this.roleAdapter = roleAdapter;
    this.applicationHelper = applicationHelper;
  }

  @Authorize(permission = Permission.READ)
  public ApiApplicationDTO getApplicationById(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) final String applicationId)
  {
    Application application = applicationHelper.getApplicationByIdNotNull(applicationId);
    List<ApplicationTag> tags = applicationTagDAO.getByApplicationId(applicationId);
    ApiApplicationDTO apiApplicationDTO = apiApplicationAdapter.convertToDTO(application);
    apiApplicationDTO.applicationTags = apiApplicationTagAdapter.convertToDTO(tags);
    return apiApplicationDTO;
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

  @Authorize(permission = Permission.WRITE)
  public void deleteApplication(@AuthzContext(AuthzContext.Key.APPLICATION_ID) final String applicationId)
      throws IOException
  {
    applicationHelper.deleteApplicationById(applicationId);
  }

  @Authorize(permission = Permission.WRITE)
  public Application addApplication(final EntityManager entityManager,
      @AuthzContext(AuthzContext.Key.APPLICATION_OWNER) final Application application)
  {
    return applicationHelper.addApplication(entityManager, application);
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
}
