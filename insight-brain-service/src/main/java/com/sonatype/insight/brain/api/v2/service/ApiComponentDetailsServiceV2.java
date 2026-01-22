/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.InvalidComponentIdentifierException;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDetailsDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDetailsRequestDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDetailsResultDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiSecurityIssueDTO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.16.0
 */
@Named
@Singleton
public class ApiComponentDetailsServiceV2
{
  private static final Logger log = LoggerFactory.getLogger(ApiComponentDetailsServiceV2.class);

  public static final String HDS_COMPONENT_DETAILS_PATH = "rest/component/details/{purpose: evaluation|integration}";

  public static final String PURPOSE_INTEGRATION = "integration";

  public static final String PURPOSE_EVALUATION = "evaluation";

  private final ApiComponentDetailsAdapter componentDetailsAdapter;

  private final HdsClient client;

  private int chunkSize = 100;

  @Inject
  public ApiComponentDetailsServiceV2(ApiComponentDetailsAdapter componentDetailsAdapter, HdsClient client) {
    this.componentDetailsAdapter = componentDetailsAdapter;
    this.client = client;
  }

  protected ApiComponentDetailsDTOV2 convertToDTO(ComponentEvaluationData componentDetailsFromHds) {
    return componentDetailsAdapter.convertToDTO(componentDetailsFromHds);
  }

  protected ComponentEvaluationDataList post(ComponentEvaluationDataRequestList requestList, String purpose) {
    return client.post(ComponentEvaluationDataList.class, HDS_COMPONENT_DETAILS_PATH, requestList, purpose);
  }

  public ApiComponentDetailsResultDTOV2 getComponentDetails(ApiComponentDetailsRequestDTOV2 componentDetailsRequest) {
    long start = System.currentTimeMillis();

    validateRequest(componentDetailsRequest);

    ApiComponentDetailsResultDTOV2 result = new ApiComponentDetailsResultDTOV2();
    List<ComponentEvaluationDataList.ComponentEvaluationData> componentDetailsFromHdsList =
        getComponentDetailsListFromHds(componentDetailsRequest, PURPOSE_INTEGRATION);
    for (ComponentEvaluationDataList.ComponentEvaluationData componentDetailsFromHds : componentDetailsFromHdsList) {
      ApiComponentDetailsDTOV2 componentDetails = convertToDTO(componentDetailsFromHds);
      clearUnapplicableData(componentDetails);
      result.componentDetails.add(componentDetails);
    }

    log.debug("Got component details for {} components and {} purpose in {} ms.",
        componentDetailsRequest.components.size(), PURPOSE_INTEGRATION, System.currentTimeMillis() - start);
    return result;
  }

  private void clearUnapplicableData(ApiComponentDetailsDTOV2 componentDetails) {
    componentDetails.component.proprietary = null;
    componentDetails.licenseData.overriddenLicenses = null;
    componentDetails.licenseData.status = null;
    if (componentDetails.securityData.securityIssues != null) {
      for (ApiSecurityIssueDTO apiSecurityIssueDTO : componentDetails.securityData.securityIssues) {
        apiSecurityIssueDTO.status = null;
      }
    }
  }

  // For testing
  public void setChunkSize(int chunkSize) {
    this.chunkSize = chunkSize;
  }

  private void validateRequest(ApiComponentDetailsRequestDTOV2 componentDetailsRequest) {
    if (componentDetailsRequest == null || componentDetailsRequest.components == null
        || componentDetailsRequest.components.isEmpty()) {
      throw new BadRequestException("No components provided in the request");
    }
    for (ApiComponentDTOV2 componentDTO : componentDetailsRequest.components) {
      if (componentDTO.packageUrl != null) {
        validatePackageUrl(componentDTO);
      }
      else if (componentDTO.componentIdentifier != null) {
        validateComponentIdentifier(componentDTO);
      }
      else if (componentDTO.hash == null) {
        throw new BadRequestException("One of either componentIdentifier, packageUrl, or hash must be supplied.");
      }
    }
  }

  private void validatePackageUrl(ApiComponentDTOV2 componentDTO) {
    new PackageUrlIdentifier(componentDTO.packageUrl).ensureCompleteIdentifier();
  }

  private void validateComponentIdentifier(ApiComponentDTOV2 componentDTO) {
    try {
      ComponentIdentifier componentIdentifier = componentDTO.componentIdentifier.toComponentIdentifier();
      componentIdentifier.ensureComplete();
    }
    catch (InvalidComponentIdentifierException e) {
      throw new BadRequestException(e.getMessage(), e);
    }
  }

  public List<ComponentEvaluationDataList.ComponentEvaluationData> getComponentDetailsListFromHds(
      ApiComponentDetailsRequestDTOV2 componentDetailsRequestDTO,
      String purpose)
  {
    // The client may use long hashes. Truncate all hashes to the length used by CLM.
    for (ApiComponentDTOV2 apiComponentDTOV2 : componentDetailsRequestDTO.components) {
      apiComponentDTOV2.hash = HashHelper.truncateHash(apiComponentDTOV2.hash);
    }

    return getComponentDetailsListFromHds(componentDetailsRequestDTO.components, this::convert, purpose);
  }

  public List<ComponentEvaluationDataList.ComponentEvaluationData> getComponentDetailsListFromHds(
      List<ComponentIdentifier> componentIdentifiers,
      String purpose)
  {
    return getComponentDetailsListFromHds(componentIdentifiers, this::convert, purpose);
  }

  private <T> List<ComponentEvaluationDataList.ComponentEvaluationData> getComponentDetailsListFromHds(
      List<T> components,
      Function<T, ComponentEvaluationDataRequestList.ComponentEvaluationDataRequest> convert,
      String purpose)
  {
    long start = System.currentTimeMillis();

    ComponentEvaluationDataList returnList = new ComponentEvaluationDataList();

    int indexAdjust = 0;
    List<List<T>> componentChunks = createChunks(components, chunkSize);
    for (List<T> componentChunk : componentChunks) {
      ComponentEvaluationDataRequestList componentEvaluationDataRequestList = convert(componentChunk, convert);
      ComponentEvaluationDataList componentEvaluationDataList;
      componentEvaluationDataList = post(componentEvaluationDataRequestList, purpose);
      for (ComponentEvaluationDataList.ComponentEvaluationData componentEvaluationData : 
          componentEvaluationDataList.components) {
        componentEvaluationData.requestIndex += indexAdjust * chunkSize;
        returnList.components.add(componentEvaluationData);
      }
      indexAdjust++;
    }

    log.debug("Got component details from HDS for {} components and {} purpose in {} ms.", components.size(), purpose,
        System.currentTimeMillis() - start);
    return returnList.components;
  }

  private <T> List<List<T>> createChunks(List<T> bigList, int chunkSize) {
    List<List<T>> chunks = new ArrayList<>();

    for (int i = 0; i < bigList.size(); i += chunkSize) {
      List<T> chunk = bigList.subList(i, Math.min(bigList.size(), i + chunkSize));
      chunks.add(chunk);
    }

    return chunks;
  }

  private <T> ComponentEvaluationDataRequestList convert(
      final List<T> components,
      Function<T, ComponentEvaluationDataRequestList.ComponentEvaluationDataRequest> convert)
  {
    ComponentEvaluationDataRequestList componentEvaluationDataRequestList = new ComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components = new ArrayList<>();
    for (T component : components) {
      ComponentEvaluationDataRequestList.ComponentEvaluationDataRequest componentEvaluationDataRequest =
          convert.apply(component);
      componentEvaluationDataRequestList.components.add(componentEvaluationDataRequest);
    }
    return componentEvaluationDataRequestList;
  }

  private ComponentEvaluationDataRequestList.ComponentEvaluationDataRequest convert(
      final ApiComponentDTOV2 componentDTO)
  {
    ComponentEvaluationDataRequestList.ComponentEvaluationDataRequest componentEvaluationDataRequest =
        new ComponentEvaluationDataRequestList.ComponentEvaluationDataRequest();
    componentEvaluationDataRequest.hash = componentDTO.hash;
    if (componentDTO.packageUrl != null) {
      componentEvaluationDataRequest.componentIdentifier =
          new PackageUrlIdentifier(componentDTO.packageUrl).ensureCompleteIdentifier();
    }
    else if (componentDTO.componentIdentifier != null) {
      componentEvaluationDataRequest.componentIdentifier = componentDTO.componentIdentifier.toComponentIdentifier();
      componentEvaluationDataRequest.componentIdentifier.ensureComplete();
    }
    return componentEvaluationDataRequest;
  }

  // Visible for testing
  ComponentEvaluationDataRequestList.ComponentEvaluationDataRequest convert(
      final ComponentIdentifier componentIdentifier)
  {
    ComponentEvaluationDataRequestList.ComponentEvaluationDataRequest componentEvaluationDataRequest =
        new ComponentEvaluationDataRequestList.ComponentEvaluationDataRequest();
    componentEvaluationDataRequest.componentIdentifier =
        new ComponentIdentifier(componentIdentifier.getFormat(), componentIdentifier.getCoordinates());
    componentEvaluationDataRequest.componentIdentifier.ensureComplete();
    return componentEvaluationDataRequest;
  }
}
