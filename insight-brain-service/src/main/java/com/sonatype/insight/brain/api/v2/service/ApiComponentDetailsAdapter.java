/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationData;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList.RepositoryComponentEvaluationDataRequest;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDetailsDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentPolicyViolationListDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiConstraintViolationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiConstraintViolationReasonDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentEvaluationRequestList;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentEvaluationRequestList.ApiRepositoryComponentEvaluationRequest;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentEvaluationResultList;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentEvaluationResultList.ApiRepositoryComponentEvaluationResult;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.utils.RepositoryPathnameSerializer;
import com.sonatype.insight.purl.PackageUrlIdentifier;

/**
 * @since 1.13.0
 */
@Named
public class ApiComponentDetailsAdapter
{
  private final ApiLicenseDataAdapter licenseDataAdapter;

  private final ApiSecurityDataAdapter securityDataAdapter;

  private final ApiComponentProjectDetailsAdapter componentProjectDetailsAdapter;

  private final RepositoryComponentDAO repositoryComponentDAO;
  
  private final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  @Inject
  public ApiComponentDetailsAdapter(
      final ApiLicenseDataAdapter licenseDataAdapter,
      final ApiSecurityDataAdapter securityDataAdapter,
      final ApiComponentProjectDetailsAdapter componentProjectDetailsAdapter,
      final RepositoryComponentDAO repositoryComponentDAO,
      final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO)
  {
    this.licenseDataAdapter = licenseDataAdapter;
    this.securityDataAdapter = securityDataAdapter;
    this.componentProjectDetailsAdapter = componentProjectDetailsAdapter;
    this.repositoryComponentDAO = repositoryComponentDAO;
    this.repositoryPolicyViolationDAO = repositoryPolicyViolationDAO;
  }

  public ApiComponentDetailsDTOV2 convertToDTO(final Component component, final Collection<PolicyAlert> policyAlerts) {
    ApiComponentDetailsDTOV2 componentDetailsDTO = new ApiComponentDetailsDTOV2();
    componentDetailsDTO.component = new ApiComponentDTOV2();
    componentDetailsDTO.component.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(component
        .getComponentIdentifier());
    componentDetailsDTO.component.hash = component.getHash();
    componentDetailsDTO.component.packageUrl = PackageUrlIdentifier.toPackageUrl(component.getComponentIdentifier());

    ComponentDisplayName componentDisplayName =
        ComponentDisplayNameUtil.fromIdentifier(component.getComponentIdentifier());
    componentDetailsDTO.component.displayName = componentDisplayName != null ? componentDisplayName.toString() : null;

    componentDetailsDTO.component.proprietary = component.isProprietary();
    componentDetailsDTO.matchState = component.getMatchState() == null ? MatchState.UNKNOWN.getId() : component
        .getMatchState().getId();

    if (component.getCatalogDate() != null) {
      componentDetailsDTO.catalogDate = new Date(component.getCatalogDate());
    }
    componentDetailsDTO.relativePopularity = component.getRelativePopularity();

    componentDetailsDTO.licenseData = licenseDataAdapter.convertToDTO(component);
    componentDetailsDTO.securityData = securityDataAdapter.convertToDTO(component);

    componentDetailsDTO.policyData = new ApiComponentPolicyViolationListDTOV2();
    for (PolicyAlert policyAlert : policyAlerts) {
      componentDetailsDTO.policyData.policyViolations.add(convert(policyAlert));
    }
    return componentDetailsDTO;
  }

  public ApiComponentDetailsDTOV2 convertToDTO(ComponentEvaluationData componentDetailsFromHds) {
    ApiComponentDetailsDTOV2 componentDetailsDTO = new ApiComponentDetailsDTOV2();
    componentDetailsDTO.component = new ApiComponentDTOV2();
    componentDetailsDTO.component.componentIdentifier = ApiComponentIdentifierDTOV2
        .fromComponentIdentifier(componentDetailsFromHds.componentIdentifier);
    componentDetailsDTO.component.hash = componentDetailsFromHds.hash;
    componentDetailsDTO.component.packageUrl =
        PackageUrlIdentifier.toPackageUrl(componentDetailsFromHds.componentIdentifier);

    ComponentDisplayName componentDisplayName =
        ComponentDisplayNameUtil.fromIdentifier(componentDetailsFromHds.componentIdentifier);
    componentDetailsDTO.component.displayName = componentDisplayName != null ? componentDisplayName.toString() : null;

    if (componentDetailsFromHds.matchState == null) {
      componentDetailsDTO.matchState = MatchState.UNKNOWN.getId();
    }
    else {
      // The matchState (which is an id) from HDS may be unknown to the CLM server and we don't want the CLM server to
      // fail in this case.
      MatchState matchState = MatchState.getById(componentDetailsFromHds.matchState);
      componentDetailsDTO.matchState = matchState == null ? MatchState.UNKNOWN.getId() : matchState.getId();
    }

    if (componentDetailsFromHds.catalogDate != null) {
      componentDetailsDTO.catalogDate = new Date(componentDetailsFromHds.catalogDate);
    }
    componentDetailsDTO.relativePopularity = componentDetailsFromHds.relativePopularity;

    if (componentDetailsFromHds.integrityRating != null) {
      componentDetailsDTO.integrityRating = componentDetailsFromHds.integrityRating.getLabel();
    }

    if (componentDetailsFromHds.hygieneRating != null) {
      componentDetailsDTO.hygieneRating = componentDetailsFromHds.hygieneRating.getLabel();
    }

    componentDetailsDTO.licenseData = licenseDataAdapter.convertToDTO(componentDetailsFromHds);
    componentDetailsDTO.securityData = securityDataAdapter.convertToDTO(componentDetailsFromHds);
    componentDetailsDTO.projectData = componentProjectDetailsAdapter.convertToDTO(componentDetailsFromHds);

    return componentDetailsDTO;
  }

  public RepositoryComponentEvaluationDataRequestList convertFromDTO(
      ApiRepositoryComponentEvaluationRequestList apiRepositoryComponentEvaluationRequestList)
  {
    RepositoryComponentEvaluationDataRequestList requestList = new RepositoryComponentEvaluationDataRequestList();

    for (ApiRepositoryComponentEvaluationRequest componentRequest :
        apiRepositoryComponentEvaluationRequestList.components) {
      String pathname = componentRequest.pathname;
      if (pathname == null && componentRequest.packageUrl != null) {
        pathname = RepositoryPathnameSerializer.toPathname(componentRequest.packageUrl);
      }
      requestList.components.add(
          new RepositoryComponentEvaluationDataRequest(apiRepositoryComponentEvaluationRequestList.format, pathname,
              HashHelper.truncateHash(componentRequest.hash)));
    }
    return requestList;
  }

  public ApiRepositoryComponentEvaluationResultList convertToDTO(
      RepositoryManager repositoryManager,
      Repository repository,
      ApiRepositoryComponentEvaluationRequestList apiRepositoryComponentEvaluationRequestList,
      RepositoryComponentEvaluationDataList repositoryComponentEvaluationDataList)
  {
    ApiRepositoryComponentEvaluationResultList resultDTO = new ApiRepositoryComponentEvaluationResultList();
    resultDTO.repositoryManagerId = repositoryManager.getId();
    resultDTO.repositoryId = repository.getId();
    resultDTO.repositoryPublicId = repository.getPublicId();
    resultDTO.repositoryType = repository.getRepositoryType().name();
    for (RepositoryComponentEvaluationData repositoryComponentEvaluationData :
        repositoryComponentEvaluationDataList.componentEvalResults) {
      ApiRepositoryComponentEvaluationResult componentEvaluationResult =
          new ApiRepositoryComponentEvaluationResult();

      int index = repositoryComponentEvaluationData.requestIndex;
      ApiRepositoryComponentEvaluationRequest apiRepositoryComponentEvaluationRequest =
          apiRepositoryComponentEvaluationRequestList.components.get(index);

      componentEvaluationResult.quarantined = repositoryComponentEvaluationData.quarantine;
      String pathname = apiRepositoryComponentEvaluationRequest.pathname;
      if (pathname == null && apiRepositoryComponentEvaluationRequest.packageUrl != null) {
        pathname = RepositoryPathnameSerializer.toPathname(apiRepositoryComponentEvaluationRequest.packageUrl);
      }
      RepositoryComponent repositoryComponent = repositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(),
          pathname);
      if (repositoryComponent != null) {
        componentEvaluationResult.quarantineDate = repositoryComponent.getQuarantineTime();
      }
      componentEvaluationResult.component = apiRepositoryComponentEvaluationRequest;
      componentEvaluationResult.catalogDate = repositoryComponentEvaluationData.catalogDate;
      List<RepositoryPolicyViolation> repositoryPolicyViolations =
          repositoryPolicyViolationDAO.getActiveByRepositoryIdAndPathname(repository.getId(), pathname);
      repositoryPolicyViolationDAO.loadConstraintFacts(repositoryPolicyViolations);
      componentEvaluationResult.policyViolations.addAll(
          repositoryPolicyViolations.stream().map(this::convert).collect(Collectors.toList()));
      resultDTO.results.add(componentEvaluationResult);
    }
    return resultDTO;
  }

  private ApiPolicyViolationDTOV2 convert(RepositoryPolicyViolation repositoryPolicyViolation) {
    ApiPolicyViolationDTOV2 dto = new ApiPolicyViolationDTOV2();
    dto.policyId = repositoryPolicyViolation.getPolicyId();
    dto.policyName = repositoryPolicyViolation.getPolicyName();
    dto.policyViolationId = repositoryPolicyViolation.getId();
    dto.openTime = repositoryPolicyViolation.getOpenTime();
    dto.waiveTime = repositoryPolicyViolation.getWaiveTime();
    dto.threatLevel = repositoryPolicyViolation.getThreatLevel();
    for (ConstraintFact constraintFact : repositoryPolicyViolation.getConstraintFacts()) {
      dto.constraintViolations.add(convert(constraintFact));
    }
    return dto;
  }

  private ApiPolicyViolationDTOV2 convert(final PolicyAlert policyAlert) {
    ApiPolicyViolationDTOV2 componentPolicyViolationDTO = new ApiPolicyViolationDTOV2();
    PolicyFact policyFact = policyAlert.getTrigger();
    componentPolicyViolationDTO.policyId = policyFact.getPolicyId();
    componentPolicyViolationDTO.policyName = policyFact.getPolicyName();
    componentPolicyViolationDTO.threatLevel = policyFact.getThreatLevel();

    for (ComponentFact componentFact : policyFact.getComponentFacts()) {
      for (ConstraintFact constraintFact : componentFact.getConstraintFacts()) {
        componentPolicyViolationDTO.constraintViolations.add(convert(constraintFact));
      }
    }
    return componentPolicyViolationDTO;
  }

  private ApiConstraintViolationDTO convert(final ConstraintFact constraintFact) {
    ApiConstraintViolationDTO constraintViolationDTO = new ApiConstraintViolationDTO();
    constraintViolationDTO.constraintId = constraintFact.getConstraintId();
    constraintViolationDTO.constraintName = constraintFact.getConstraintName();
    for (ConditionFact conditionFact : constraintFact.getConditionFacts()) {
      ApiConstraintViolationReasonDTO constraintViolationReasonDTO = new ApiConstraintViolationReasonDTO();
      constraintViolationReasonDTO.reason = conditionFact.getReason();
      constraintViolationDTO.reasons.add(constraintViolationReasonDTO);
    }
    return constraintViolationDTO;
  }
}
