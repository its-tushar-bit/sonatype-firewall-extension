/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api;

import javax.inject.Named;

import com.sonatype.insight.brain.api.dto.ApiApplicationDTO;
import com.sonatype.insight.brain.model.Application;

/**
 * @since 1.11.0
 */
@Named
public class ApiApplicationAdapter
{
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
    applicationDTO.id = application.getId();
    applicationDTO.publicId = application.getPublicId();
    applicationDTO.name = application.getName();
    applicationDTO.organizationId = application.getOrganizationId();
    applicationDTO.contactUserName = application.getContactInternalName();
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
}
