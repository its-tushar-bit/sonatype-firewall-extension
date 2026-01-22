/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.ApiApplicationTagDTO;
import com.sonatype.insight.brain.model.tag.ApplicationTag;

/**
 * @since 1.11.0
 */
@Named
public class ApiApplicationTagAdapter
{
  static List<ApplicationTag> convertFromDTO(
      final String applicationId,
      final List<ApiApplicationTagDTO> applicationTagDTOs)
  {
    if (applicationTagDTOs == null) {
      return Collections.emptyList();
    }

    List<ApplicationTag> applicationTags = new ArrayList<>();
    for (ApiApplicationTagDTO applicationTagDTO : applicationTagDTOs) {
      ApplicationTag applicationTag = new ApplicationTag(applicationId, applicationTagDTO.tagId);
      applicationTag.setId(applicationTagDTO.id);
      applicationTags.add(applicationTag);
    }
    return applicationTags;
  }

  public static List<ApiApplicationTagDTO> convertToDTO(final List<ApplicationTag> applicationTags) {
    if (applicationTags == null) {
      return Collections.emptyList();
    }

    List<ApiApplicationTagDTO> applicationTagDTOs = new ArrayList<>(applicationTags.size());
    for (ApplicationTag applicationTag : applicationTags) {
      ApiApplicationTagDTO applicationTagDTO = new ApiApplicationTagDTO();
      applicationTagDTO.id = applicationTag.getId();
      applicationTagDTO.applicationId = applicationTag.getApplicationId();
      applicationTagDTO.tagId = applicationTag.getTagId();
      applicationTagDTOs.add(applicationTagDTO);
    }
    return applicationTagDTOs;
  }
}
