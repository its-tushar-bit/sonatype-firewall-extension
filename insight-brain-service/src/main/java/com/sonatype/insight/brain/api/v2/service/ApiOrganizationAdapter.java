/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.sonatype.insight.brain.api.v2.dto.ApiOrganizationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiOrganizationListDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiTagDTO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.tag.Tag;

/**
 * @since 1.11.0
 */
class ApiOrganizationAdapter
{
  static ApiOrganizationListDTO convert(List<Organization> organizations, Map<String, List<Tag>> orgTagMap) {
    final List<ApiOrganizationDTO> dtoList = new ArrayList<>(organizations.size());
    for (final Organization organization : organizations) {
      List<Tag> tags = Optional.ofNullable(orgTagMap.get(organization.getId()))
          .orElse(List.of());
      dtoList.add(convert(organization, tags));
    }
    ApiOrganizationListDTO organizationListDTO = new ApiOrganizationListDTO();
    organizationListDTO.organizations = dtoList;
    return organizationListDTO;
  }

  static ApiOrganizationDTO convert(Organization organization, List<Tag> tags) {
    ApiOrganizationDTO apiOrganizationDTO = new ApiOrganizationDTO();
    apiOrganizationDTO.id = organization.getId();
    apiOrganizationDTO.name = organization.getName();
    apiOrganizationDTO.parentOrganizationId = organization.getParentOrganizationId();
    List<ApiTagDTO> apiTagDTOList = new ArrayList<>(tags.size());
    for (Tag tag : tags) {
      ApiTagDTO apiTagDTO = new ApiTagDTO();
      apiTagDTO.id = tag.getId();
      apiTagDTO.name = tag.getName();
      apiTagDTO.description = tag.getDescription();
      apiTagDTO.color = tag.getColor();
      apiTagDTOList.add(apiTagDTO);
    }
    apiOrganizationDTO.tags = apiTagDTOList;
    return apiOrganizationDTO;
  }
}
