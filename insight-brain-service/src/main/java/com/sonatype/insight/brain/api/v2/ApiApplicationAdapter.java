/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.ApiApplicationBaseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationDTO;
import com.sonatype.insight.brain.api.v2.service.ApiApplicationTagAdapter;
import com.sonatype.insight.brain.dataaccess.tag.ApplicationTagDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.tag.ApplicationTag;

/**
 * @since 1.11.0
 */
@Named
public class ApiApplicationAdapter
{
  private final ApplicationTagDAO applicationTagDAO;

  @Inject
  public ApiApplicationAdapter(ApplicationTagDAO applicationTagDAO) {
    this.applicationTagDAO = applicationTagDAO;
  }

  /**
   * Converts an {@link Application} entity to an {@link ApiApplicationDTO} object, will return null if null is passed
   * in.
   *
   * @param application the entity to convert
   * @return ApiApplicationDTO or null if null passed in
   */
  public ApiApplicationDTO convertToDTO(final Application application) {
    if (application == null) {
      return null;
    }

    final ApiApplicationDTO applicationDTO = new ApiApplicationDTO();
    populateDTO(applicationDTO, application);

    List<ApplicationTag> tags = applicationTagDAO.getByApplicationId(application.getId());
    applicationDTO.applicationTags = ApiApplicationTagAdapter.convertToDTO(tags);

    return applicationDTO;
  }

  /**
   * Converts an {@link ApiApplicationDTO} object to an {@link Application} entity, will return null if null is passed
   * in.
   *
   * @param applicationDTO the application DTO object to convert
   * @return Application or null if null passed in
   */
  public Application convertFromDTO(final ApiApplicationDTO applicationDTO) {
    if (applicationDTO == null) {
      return null;
    }

    final Application application = new Application();
    application.setId(applicationDTO.id);
    application.setPublicId(applicationDTO.publicId);
    application.setName(applicationDTO.name);
    application.setOrganizationId(applicationDTO.organizationId);
    application.setContactInternalName(applicationDTO.contactUserName);
    return application;
  }

  /**
   * @since 1.13.0
   */
  public ApiApplicationBaseDTO convertToApplicationBaseDTO(final Application application) {
    if (application == null) {
      return null;
    }

    final ApiApplicationBaseDTO applicationDTO = new ApiApplicationBaseDTO();
    populateDTO(applicationDTO, application);
    return applicationDTO;
  }

  private void populateDTO(ApiApplicationBaseDTO applicationDTO, Application application) {
    applicationDTO.id = application.getId();
    applicationDTO.publicId = application.getPublicId();
    applicationDTO.name = application.getName();
    applicationDTO.organizationId = application.getOrganizationId();
    applicationDTO.contactUserName = application.getContactInternalName();
  }
}
