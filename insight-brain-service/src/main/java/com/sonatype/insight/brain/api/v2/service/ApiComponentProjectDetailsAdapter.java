/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentProjectDetails;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentProjectDataDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentProjectMetadataDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentProjectScmDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentProjectScmDetailsDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentProjectScmMetadataDTO;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

/**
 * @since 1.100
 */
@Named
@Singleton
public class ApiComponentProjectDetailsAdapter
{
  public ApiComponentProjectDataDTO convertToDTO(final ComponentEvaluationData componentEvaluationData) {
    if (componentEvaluationData.componentProjectDetails == null) {
      return null;
    }

    ComponentProjectDetails componentProjectDetails = componentEvaluationData.componentProjectDetails;

    ApiComponentProjectDataDTO projectDataDTO = new ApiComponentProjectDataDTO();
    projectDataDTO.setFirstReleaseDate(componentProjectDetails.getFirstRelease());
    projectDataDTO.setLastReleaseDate(componentProjectDetails.getLastRelease());

    ApiComponentProjectScmDTO projectScmDTO = new ApiComponentProjectScmDTO();
    projectScmDTO.setScmUrl(componentProjectDetails.getScmUrl());
    projectDataDTO.setSourceControlManagement(projectScmDTO);

    ApiComponentProjectMetadataDTO projectMetadata = new ApiComponentProjectMetadataDTO();
    projectMetadata.organization = componentProjectDetails.getOrganization();
    projectMetadata.description = componentProjectDetails.getDescription();
    projectDataDTO.setProjectMetadata(projectMetadata);

    ApiComponentProjectScmMetadataDTO scmMetadata = new ApiComponentProjectScmMetadataDTO();
    scmMetadata.forks = componentProjectDetails.getScmForks();
    scmMetadata.stars = componentProjectDetails.getScmStars();
    projectScmDTO.setScmMetadata(scmMetadata);

    ApiComponentProjectScmDetailsDTO scmDetails = new ApiComponentProjectScmDetailsDTO();
    scmDetails.commitsPerMonth = componentProjectDetails.getCommitsPerMonth();
    scmDetails.uniqueDevsPerMonth = componentProjectDetails.getUniqueDevsPerMonth();
    projectScmDTO.setScmDetails(scmDetails);

    return projectDataDTO;
  }
}
