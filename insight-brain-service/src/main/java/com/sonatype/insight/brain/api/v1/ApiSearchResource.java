/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v1;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v1.dto.ApiSearchCriteriaDTO;
import com.sonatype.insight.brain.api.v1.dto.ApiSearchResultDTO;
import com.sonatype.insight.brain.api.v1.dto.ApiSearchResultsDTO;
import com.sonatype.insight.brain.api.v2.ApiSearchResourceV2;
import com.sonatype.insight.brain.api.v2.dto.ApiSearchCriteriaDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiSearchResultDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiSearchResultsDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiSearchServiceV2;

/**
 * Enables end users to search for components within their applications. This REST API is exposed directly to users.
 *
 * @since 1.7
 * @deprecated since 1.13.0, use {@link ApiSearchResourceV2}
 */
@Deprecated
@Path(PublicApiPaths.SEARCH_SERVICE_PATH)
@Named
public class ApiSearchResource
{
  private final ApiSearchServiceV2 searchService;


  @Inject
  public ApiSearchResource(final ApiSearchServiceV2 searchService) {
    this.searchService = searchService;
  }

  /**
   * Searches all currently registered applications for a component matching the given search criteria. A component can
   * be searched for by its hash or its coordinates, the latter supporting wildcards like the equivalent policy
   * condition. The mandatory stageId parameter restricts which scans/reports of the applications are inspected for the
   * component.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ApiSearchResultsDTO searchComponent(@QueryParam("stageId") String stageId, @QueryParam("hash") String hash,
      @QueryParam("groupId") String groupId, @QueryParam("artifactId") String artifactId,
      @QueryParam("version") String version)
  {

    ComponentIdentifier componentIdentifier = getComponentIdentifier(groupId, artifactId, version);
    ApiSearchResultsDTOV2 searchResultsDTOV2 = searchService.searchComponent(stageId, hash, componentIdentifier);

    return convert(searchResultsDTOV2);
  }

  private ApiSearchResultsDTO convert(final ApiSearchResultsDTOV2 searchResultsDTOV2) {
    ApiSearchResultsDTO searchResultsDTO = new ApiSearchResultsDTO();

    searchResultsDTO.criteria = convert(searchResultsDTOV2.criteria);
    searchResultsDTO.results = convert(searchResultsDTOV2.results);

    return searchResultsDTO;
  }

  private List<ApiSearchResultDTO> convert(final List<ApiSearchResultDTOV2> searchResultDTOV2s) {
    List<ApiSearchResultDTO> searchResultDTOs = new ArrayList<>();
    for (ApiSearchResultDTOV2 searchResultDTOV2 : searchResultDTOV2s) {
      ApiSearchResultDTO searchResultDTO = new ApiSearchResultDTO();
      searchResultDTO.applicationId = searchResultDTOV2.applicationId;
      searchResultDTO.applicationName = searchResultDTOV2.applicationName;
      if (searchResultDTOV2.componentIdentifier != null) {
        searchResultDTO.groupId = searchResultDTOV2.componentIdentifier.getCoordinates()
            .get(ComponentIdentifier.MAVEN_GROUP_ID);
        searchResultDTO.artifactId = searchResultDTOV2.componentIdentifier.getCoordinates()
            .get(ComponentIdentifier.MAVEN_ARTIFACT_ID);
        searchResultDTO.version = searchResultDTOV2.componentIdentifier.getCoordinates()
            .get(ComponentIdentifier.VERSION);
      }
      searchResultDTO.hash = searchResultDTOV2.hash;
      searchResultDTO.reportUrl = searchResultDTOV2.reportUrl;
      searchResultDTO.threatLevel = searchResultDTOV2.threatLevel;
      searchResultDTOs.add(searchResultDTO);
    }
    return searchResultDTOs;
  }

  private ApiSearchCriteriaDTO convert(final ApiSearchCriteriaDTOV2 searchCriteriaDTOV2) {
    ApiSearchCriteriaDTO criteriaDTO = new ApiSearchCriteriaDTO();
    criteriaDTO.hash = searchCriteriaDTOV2.hash;
    criteriaDTO.stageId = searchCriteriaDTOV2.stageId;
    if (searchCriteriaDTOV2.componentIdentifier != null) {
      criteriaDTO.groupId = searchCriteriaDTOV2.componentIdentifier.getCoordinates()
          .get(ComponentIdentifier.MAVEN_GROUP_ID);
      criteriaDTO.artifactId = searchCriteriaDTOV2.componentIdentifier.getCoordinates()
          .get(ComponentIdentifier.MAVEN_ARTIFACT_ID);
      criteriaDTO.version = searchCriteriaDTOV2.componentIdentifier.getCoordinates().get(ComponentIdentifier.VERSION);
    }
    return criteriaDTO;
  }

  private ComponentIdentifier getComponentIdentifier(final String groupId, final String artifactId,
      final String version)
  {
    if (groupId == null && artifactId == null && version == null) {
      return null;
    }

    return ComponentIdentifier.createMavenCoordinates(groupId, artifactId, version);
  }
}
