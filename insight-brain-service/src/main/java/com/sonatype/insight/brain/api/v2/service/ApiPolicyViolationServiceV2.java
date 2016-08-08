/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.ApiApplicationAdapter;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationViolationListDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiEnhancedPolicyViolationDTOV2;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.organization.ApplicationService;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

/**
 * @since 1.13.0
 */
@Named
public class ApiPolicyViolationServiceV2
{
  private final PolicyViolationDAO policyViolationDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final ApplicationService applicationService;

  private final PolicyViolationAdapter policyViolationAdapter;

  private final ApiApplicationAdapter applicationAdapter;

  private final ApplicationComponentDAO applicationComponentDAO;

  @Inject
  public ApiPolicyViolationServiceV2(final PolicyViolationDAO policyViolationDAO,
                                     final PolicyEvaluationDAO policyEvaluationDAO,
                                     final ApplicationService applicationService,
                                     final PolicyViolationAdapter policyViolationAdapter,
                                     final ApiApplicationAdapter applicationAdapter,
                                     final ApplicationComponentDAO applicationComponentDAO)
  {
    this.policyViolationDAO = policyViolationDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.applicationService = applicationService;
    this.policyViolationAdapter = policyViolationAdapter;
    this.applicationAdapter = applicationAdapter;
    this.applicationComponentDAO = applicationComponentDAO;
  }

  public ApiApplicationViolationListDTOV2 getPolicyViolations(final Set<String> policyIds) {
    List<Application> applications = applicationService.getApplications();
    Set<String> appIds = new HashSet<>();
    for (Application application : applications) {
      appIds.add(application.getId());
    }

    List<PolicyEvaluation> policyEvaluations = policyEvaluationDAO.getLastByApplicationIds(appIds);

    ListMultimap<String, PolicyEvaluation> policyEvaluationMapByAppId = ArrayListMultimap.create();
    Set<String> policyEvaluationIds = new HashSet<>();
    for (PolicyEvaluation policyEvaluation : policyEvaluations) {
      policyEvaluationIds.add(policyEvaluation.getId());
      policyEvaluationMapByAppId.put(policyEvaluation.getApplicationId(), policyEvaluation);
    }

    // Get the list of violations for the evals in the above list and filter by policies
    List<PolicyViolation> policyViolations = policyViolationDAO.getActiveByEvaluationIds(policyEvaluationIds);
    ListMultimap<String, PolicyViolation> policyViolationMapByEvaluationId = ArrayListMultimap.create();
    for (PolicyViolation policyViolation : policyViolations) {
      if (policyIds.contains(policyViolation.getPolicyId())) {
        policyViolationMapByEvaluationId.put(policyViolation.getPolicyEvaluationId(), policyViolation);
      }
    }

    return buildApplicationDTOs(applications, policyViolationMapByEvaluationId, policyEvaluationMapByAppId);
  }

  private ApiApplicationViolationListDTOV2 buildApplicationDTOs(List<Application> applications,
                                                                ListMultimap<String, PolicyViolation> policyViolationMapByEvaluationId,
                                                                ListMultimap<String, PolicyEvaluation> policyEvaluationMapByAppId)
  {
    ApiApplicationViolationListDTOV2 apiViolationListDTO = new ApiApplicationViolationListDTOV2();
    for (Application application : applications) {
      List<ApiEnhancedPolicyViolationDTOV2> policyViolationDTOs = buildPolicyViolationDTOs(application,
          policyViolationMapByEvaluationId, policyEvaluationMapByAppId);
      if (!policyViolationDTOs.isEmpty()) {
        ApiApplicationViolationDTOV2 apiApplicationViolationDTO = new ApiApplicationViolationDTOV2();
        apiViolationListDTO.applicationViolations.add(apiApplicationViolationDTO);
        apiApplicationViolationDTO.application = applicationAdapter.convertToApplicationBaseDTO(application);
        apiApplicationViolationDTO.policyViolations = policyViolationDTOs;
      }
    }

    return apiViolationListDTO;
  }

  private List<ApiEnhancedPolicyViolationDTOV2> buildPolicyViolationDTOs(Application application,
                                                                         ListMultimap<String, PolicyViolation> policyViolationMapByEvaluationId,
                                                                         ListMultimap<String, PolicyEvaluation> policyEvaluationMapByAppId)
  {
    List<ApiEnhancedPolicyViolationDTOV2> apiPolicyViolationDTOs = new ArrayList<>();
    List<PolicyEvaluation> policyEvaluations = policyEvaluationMapByAppId.get(application.getId());
    if (policyEvaluations != null) {
      for (PolicyEvaluation policyEvaluation : policyEvaluations) {
        List<PolicyViolation> policyViolations = policyViolationMapByEvaluationId.get(policyEvaluation.getId());
        if (policyViolations != null) {
          for (PolicyViolation policyViolation : policyViolations) {
            ComponentIdentifier componentIdentifier = policyViolation.getComponentIdentifier();
            if (componentIdentifier != null) {
              ApiEnhancedPolicyViolationDTOV2 apiPolicyViolationDTO = new ApiEnhancedPolicyViolationDTOV2();
              apiPolicyViolationDTOs.add(apiPolicyViolationDTO);
              apiPolicyViolationDTO.policyId = policyViolation.getPolicyId();
              apiPolicyViolationDTO.policyName = policyViolation.getPolicyName();
              apiPolicyViolationDTO.threatLevel = policyViolation.getThreatLevel();
              apiPolicyViolationDTO.reportUrl = UserInterfaceLinksResource.getReportUrl(application.getPublicId(),
                  policyEvaluation.getScanId());
              apiPolicyViolationDTO.stageId = policyEvaluation.getStageTypeId();
              ApplicationComponent applicationComponent = applicationComponentDAO
                  .getByApplicationIdAndStageTypeIdAndHash(application.getId(), policyEvaluation.getStageTypeId(),
                      policyViolation.getHash());
              apiPolicyViolationDTO.component = new ApiComponentDTOV2();
              apiPolicyViolationDTO.component.hash = policyViolation.getHash();
              apiPolicyViolationDTO.component.proprietary = applicationComponent != null
                  && applicationComponent.isProprietary();
              apiPolicyViolationDTO.component.componentIdentifier = ApiComponentIdentifierDTOV2
                  .fromComponentIdentifier(componentIdentifier);
              apiPolicyViolationDTO.constraintViolations = policyViolationAdapter.convert(policyViolation);
            }
          }
        }
      }
    }
    return apiPolicyViolationDTOs;
  }
}
