/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;

import com.sonatype.insight.brain.api.v2.dto.ApiApplicationBaseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationDTO;
import com.sonatype.insight.brain.api.v2.service.ApiApplicationTagAdapter;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.tag.ApplicationTag;

/**
 * @since 1.11.0
 */
public class ApiApplicationAdapter
{
  /**
   * Converts an {@link Application} entity to an {@link ApiApplicationDTO} object, will return null if null is passed
   * in.
   *
   * @param application the entity to convert
   * @return ApiApplicationDTO or null if null passed in
   */
  public static ApiApplicationDTO convertToDTO(final Application application, List<ApplicationTag> appTags) {
    if (application == null) {
      return null;
    }

    final ApiApplicationDTO applicationDTO = new ApiApplicationDTO();
    populateDTO(applicationDTO, application);

    applicationDTO.applicationTags = ApiApplicationTagAdapter.convertToDTO(appTags);

    return applicationDTO;
  }

  /**
   * Converts an {@link ApiApplicationDTO} object to an {@link Application} entity, will return null if null is passed
   * in.
   *
   * @param applicationDTO the application DTO object to convert
   * @return Application or null if null passed in
   */
  public static Application convertFromDTO(final ApiApplicationDTO applicationDTO) {
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
  public static ApiApplicationBaseDTO convertToApplicationBaseDTO(final Application application) {
    if (application == null) {
      return null;
    }

    final ApiApplicationBaseDTO applicationDTO = new ApiApplicationBaseDTO();
    populateDTO(applicationDTO, application);
    return applicationDTO;
  }

  public static void populateDTO(ApiApplicationBaseDTO applicationDTO, Application application) {
    applicationDTO.id = application.getId();
    applicationDTO.publicId = application.getPublicId();
    applicationDTO.name = application.getName();
    applicationDTO.organizationId = application.getOrganizationId();
    applicationDTO.contactUserName = application.getContactInternalName();
  }
}
