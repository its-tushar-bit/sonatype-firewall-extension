/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.api.v2.ApiApplicationAdapter;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationViolationListDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiEnhancedPolicyViolationDTOV2;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.PolicyAuditDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader.ApplicationStageView;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader.ApplicationView;
import com.sonatype.insight.brain.purl.PurlIdentifier;

/**
 * @since 1.13.0
 */
@Named
public class ApiPolicyViolationServiceV2
{
  private final PolicyViolationLoader policyViolationLoader;

  private final ApplicationService applicationService;

  private final PolicyViolationAdapter policyViolationAdapter;

  private final ApiApplicationAdapter applicationAdapter;

  private final ApplicationComponentDAO applicationComponentDAO;

  @Inject
  public ApiPolicyViolationServiceV2(final PolicyViolationLoader policyViolationLoader,
                                     final ApplicationService applicationService,
                                     final PolicyViolationAdapter policyViolationAdapter,
                                     final ApiApplicationAdapter applicationAdapter,
                                     final ApplicationComponentDAO applicationComponentDAO)
  {
    this.policyViolationLoader = policyViolationLoader;
    this.applicationService = applicationService;
    this.policyViolationAdapter = policyViolationAdapter;
    this.applicationAdapter = applicationAdapter;
    this.applicationComponentDAO = applicationComponentDAO;
  }

  public ApiApplicationViolationListDTOV2 getPolicyViolations(final Set<String> policyIds) {
    List<Application> applications = applicationService.getApplications();

    AuditData.get().setData("selectedPolicies", PolicyAuditDTO.transcribe(policyIds))
        .setData("inspectedApplicationCount", applications.size());

    Collection<ApplicationView> appViews = policyViolationLoader.getViolations(applications, null,
        true, violation -> policyIds.contains(violation.getPolicyId()));

    return buildApplicationDTOs(appViews);
  }
  
  private ApiApplicationViolationListDTOV2 buildApplicationDTOs(Collection<ApplicationView> appViews) {
    ApiApplicationViolationListDTOV2 apiViolationListDTO = new ApiApplicationViolationListDTOV2();
    for (ApplicationView appView : appViews) {
      List<ApiEnhancedPolicyViolationDTOV2> policyViolationDTOs = buildPolicyViolationDTOs(appView);
      if (!policyViolationDTOs.isEmpty()) {
        ApiApplicationViolationDTOV2 apiApplicationViolationDTO = new ApiApplicationViolationDTOV2();
        apiViolationListDTO.applicationViolations.add(apiApplicationViolationDTO);
        apiApplicationViolationDTO.application =
            applicationAdapter.convertToApplicationBaseDTO(appView.getApplication());
        apiApplicationViolationDTO.policyViolations = policyViolationDTOs;
      }
    }
    return apiViolationListDTO;
  }

  private List<ApiEnhancedPolicyViolationDTOV2> buildPolicyViolationDTOs(ApplicationView appView) {
    List<ApiEnhancedPolicyViolationDTOV2> apiPolicyViolationDTOs = new ArrayList<>();
    Application application = appView.getApplication();
    for (ApplicationStageView appStageView : appView.getStageViews()) {
      PolicyEvaluation policyEvaluation = appStageView.getLastEvaluation();
      for (PolicyViolation policyViolation : appStageView.getFilteredViolations()) {
        ApiEnhancedPolicyViolationDTOV2 apiPolicyViolationDTO = new ApiEnhancedPolicyViolationDTOV2();
        apiPolicyViolationDTOs.add(apiPolicyViolationDTO);
        apiPolicyViolationDTO.policyId = policyViolation.getPolicyId();
        apiPolicyViolationDTO.policyName = policyViolation.getPolicyName();
        apiPolicyViolationDTO.threatLevel = policyViolation.getThreatLevel();
        apiPolicyViolationDTO.reportUrl = UserInterfaceLinksResource.getReportUrl(application.getPublicId(),
            policyEvaluation.getScanId());
        apiPolicyViolationDTO.stageId = policyEvaluation.getStageTypeId();
        ApplicationComponent applicationComponent = applicationComponentDAO.getByApplicationIdAndStageTypeIdAndHash(
            application.getId(), policyEvaluation.getStageTypeId(), policyViolation.getHash());
        apiPolicyViolationDTO.component = new ApiComponentDTOV2();
        apiPolicyViolationDTO.component.hash = policyViolation.getHash();
        apiPolicyViolationDTO.component.proprietary = applicationComponent != null
            && applicationComponent.isProprietary();
        apiPolicyViolationDTO.component.componentIdentifier = ApiComponentIdentifierDTOV2
            .fromComponentIdentifier(policyViolation.getComponentIdentifier());
        apiPolicyViolationDTO.component.packageUrl =
            PurlIdentifier.toPackageUrl(policyViolation.getComponentIdentifier());
        apiPolicyViolationDTO.constraintViolations = policyViolationAdapter.convert(policyViolation);
      }
    }
    return apiPolicyViolationDTOs;
  }
}
