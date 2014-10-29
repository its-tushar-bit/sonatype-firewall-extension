/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.api.dto.ApiApplicationBaseDTO;
import com.sonatype.insight.brain.api.dto.ApiApplicationViolationDTO;
import com.sonatype.insight.brain.api.dto.ApiApplicationViolationListDTO;
import com.sonatype.insight.brain.api.dto.ApiConstraintViolationDTO;
import com.sonatype.insight.brain.api.dto.ApiConstraintViolationReasonDTO;
import com.sonatype.insight.brain.api.dto.ApiMavenComponentDTO;
import com.sonatype.insight.brain.api.dto.ApiPolicyViolationDTO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.organization.ApplicationService;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

/**
 * @since 1.12.0
 */
@Named
public class ApiPolicyViolationService
{
  private final PolicyViolationDAO policyViolationDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final ApplicationService applicationService;

  @Inject
  public ApiPolicyViolationService(final PolicyViolationDAO policyViolationDAO,
      final PolicyEvaluationDAO policyEvaluationDAO, final ApplicationService applicationService)
  {
    this.policyViolationDAO = policyViolationDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.applicationService = applicationService;
  }

  public ApiApplicationViolationListDTO getPolicyViolations(final Set<String> policyIds) {
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

  private ApiApplicationViolationListDTO buildApplicationDTOs(List<Application> applications,
      ListMultimap<String, PolicyViolation> policyViolationMapByEvaluationId,
      ListMultimap<String, PolicyEvaluation> policyEvaluationMapByAppId)
  {
    ApiApplicationViolationListDTO apiViolationListDTO = new ApiApplicationViolationListDTO();
    for (Application application : applications) {
      List<ApiPolicyViolationDTO> policyViolationDTOs = buildPolicyViolationDTOs(application,
          policyViolationMapByEvaluationId, policyEvaluationMapByAppId);
      if (!policyViolationDTOs.isEmpty()) {
        ApiApplicationViolationDTO apiApplicationViolationDTO = new ApiApplicationViolationDTO();
        apiViolationListDTO.applicationViolations.add(apiApplicationViolationDTO);
        apiApplicationViolationDTO.application = new ApiApplicationBaseDTO();
        apiApplicationViolationDTO.application.id = application.getId();
        apiApplicationViolationDTO.application.publicId = application.getPublicId();
        apiApplicationViolationDTO.application.name = application.getName();
        apiApplicationViolationDTO.application.organizationId = application.getOrganizationId();
        apiApplicationViolationDTO.application.contactUserName = application.getContactInternalName();
        apiApplicationViolationDTO.policyViolations = policyViolationDTOs;
      }
    }

    return apiViolationListDTO;
  }

  private List<ApiPolicyViolationDTO> buildPolicyViolationDTOs(Application application,
      ListMultimap<String, PolicyViolation> policyViolationMapByEvaluationId,
      ListMultimap<String, PolicyEvaluation> policyEvaluationMapByAppId)
  {
    List<ApiPolicyViolationDTO> apiPolicyViolationDTOs = new ArrayList<>();
    List<PolicyEvaluation> policyEvaluations = policyEvaluationMapByAppId.get(application.getId());
    if (policyEvaluations != null) {
      for (PolicyEvaluation policyEvaluation : policyEvaluations) {
        List<PolicyViolation> policyViolations = policyViolationMapByEvaluationId.get(policyEvaluation.getId());
        if (policyViolations != null) {
          for (PolicyViolation policyViolation : policyViolations) {
            ApiPolicyViolationDTO apiPolicyViolationDTO = new ApiPolicyViolationDTO();
            apiPolicyViolationDTOs.add(apiPolicyViolationDTO);
            apiPolicyViolationDTO.policyId = policyViolation.getPolicyId();
            apiPolicyViolationDTO.policyName = policyViolation.getPolicyName();
            apiPolicyViolationDTO.reportUrl = UserInterfaceLinksResource.getReportUrl(application.getPublicId(),
                policyEvaluation.getScanId());
            apiPolicyViolationDTO.stageId = policyEvaluation.getStageTypeId();
            apiPolicyViolationDTO.mavenComponent = ApiMavenComponentDTO.create(policyViolation.getHash(),
                policyViolation.getComponentIdentifier());
            apiPolicyViolationDTO.constraintViolations = buildConstraintViolationDTOs(policyViolation);
          }
        }
      }
    }
    return apiPolicyViolationDTOs;
  }

  private List<ApiConstraintViolationDTO> buildConstraintViolationDTOs(PolicyViolation policyViolation) {
    List<ApiConstraintViolationDTO> apiConstraintViolationsDTO = new ArrayList<>();
    for (ConstraintFact constraintFact : policyViolation.getConstraintFacts()) {
      ApiConstraintViolationDTO apiConstraintViolationDTO = new ApiConstraintViolationDTO();
      apiConstraintViolationsDTO.add(apiConstraintViolationDTO);
      apiConstraintViolationDTO.constraintId = constraintFact.getConstraintId();
      apiConstraintViolationDTO.constraintName = constraintFact.getConstraintName();
      apiConstraintViolationDTO.reasons = new ArrayList<>();
      for (ConditionFact conditionFact : constraintFact.getConditionFacts()) {
        ApiConstraintViolationReasonDTO apiConstraintReasonDTO = new ApiConstraintViolationReasonDTO();
        apiConstraintReasonDTO.reason = conditionFact.getReason();
        apiConstraintViolationDTO.reasons.add(apiConstraintReasonDTO);
      }
    }
    return apiConstraintViolationsDTO;
  }
}
