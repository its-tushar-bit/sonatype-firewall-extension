/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.component.ApplicationComponentDetailsDTO.PolicyViolationSummaryDTO;
import com.sonatype.insight.brain.component.ApplicationComponentDetailsDTO.PolicyViolationSummaryDTO.ReasonDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.organization.ApplicationAdapter;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.error.exception.BadRequestException;

@Named
/**
 * @since 1.11
 */
public class ComponentDetailService
{
  private final ApplicationService appService;

  private final ApplicationAdapter appAdapter;

  private final ApplicationComponentDAO applicationComponentDAO;

  @Inject
  public ComponentDetailService(ApplicationService appService, ApplicationAdapter appAdapter,
      ApplicationComponentDAO applicationComponentDAO)
  {
    this.appService = appService;
    this.appAdapter = appAdapter;
    this.applicationComponentDAO = applicationComponentDAO;
  }

  public List<ApplicationComponentDetailsDTO> getApplicationDetailsByHash(String hash) {
    List<ApplicationComponentDetailsDTO> result = new ArrayList<>();

    PolicyEvaluationDAO policyEvaluationDAO = new PolicyEvaluationDAO();
    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();

    // Get the list of applications the user can see
    List<Application> applications = appService.getApplications();
    for (Application application : applications) {
      if (!isComponentPartOfApplication(application, hash)) {
        // Ignore this application because it does not contain the specified component.
        continue;
      }

      ApplicationComponentDetailsDTO applicationComponentDetails = new ApplicationComponentDetailsDTO();

      Map<String, PolicyViolationSummaryDTO> policyViolationDTOsByPolicyId = new LinkedHashMap<>();
      for (StageType stageType : StageTypes.getAll()) {
        if (StageTypes.isIgnoredForDashboard(stageType.getId())) {
          continue;
        }
        PolicyEvaluation policyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndStageId(application.getId(),
            stageType.getId());
        if (policyEvaluation == null) {
          continue;
        }

        List<PolicyViolation> policyViolations = policyViolationDAO.getByEvaluationIdAndHash(policyEvaluation.getId(),
            hash);
        for (PolicyViolation policyViolation : policyViolations) {
          String policyId = policyViolation.getPolicyId();
          PolicyViolationSummaryDTO policyViolationSummaryDTO = policyViolationDTOsByPolicyId.get(policyId);
          if (policyViolationSummaryDTO == null) {
            policyViolationSummaryDTO = new PolicyViolationSummaryDTO();
            policyViolationSummaryDTO.policyId = policyViolation.getPolicyId();
            policyViolationSummaryDTO.stageTypeIds = new LinkedHashSet<>();
            policyViolationDTOsByPolicyId.put(policyId, policyViolationSummaryDTO);
          }
          // Use the values from the most recent policy violation
          if (policyViolationSummaryDTO.time < policyViolation.getTime().getTime()) {
            policyViolationSummaryDTO.policyName = policyViolation.getPolicyName();
            policyViolationSummaryDTO.threatLevel = policyViolation.getThreatLevel();
            policyViolationSummaryDTO.reasons = new ArrayList<>();
            policyViolationSummaryDTO.time = policyViolation.getTime().getTime();
            for (ConstraintFact constraintFact : policyViolation.getConstraintFacts()) {
              ReasonDTO reasonDTO = new ReasonDTO();
              reasonDTO.constraintName = constraintFact.getConstraintName();
              for (ConditionFact conditionFact : constraintFact.getConditionFacts()) {
                reasonDTO.reasons.add(conditionFact.getReason());
              }
              policyViolationSummaryDTO.reasons.add(reasonDTO);
            }
          }
          policyViolationSummaryDTO.stageTypeIds.add(stageType.getId());
        }
      }

      applicationComponentDetails.application = appAdapter.convert(application);
      applicationComponentDetails.policyViolations.addAll(policyViolationDTOsByPolicyId.values());
      result.add(applicationComponentDetails);
    }

    return result;
  }

  private boolean isComponentPartOfApplication(Application application, String hash) {
    List<ApplicationComponent> appComponents = applicationComponentDAO.getByApplicationIdAndHash(application.getId(),
        hash);
    for (ApplicationComponent appComponent : appComponents) {
      if (!StageTypes.isIgnoredForDashboard(appComponent.getStageTypeId())) {
        return true;
      }
    }
    return false;
  }

  public String getComponentNameByHash(String hash) {
    ApplicationComponent applicationComponent = new ApplicationComponentDAO().getLastByHash(hash);
    if (applicationComponent == null) {
      throw new BadRequestException("Unknown component with hash " + hash);
    }

    if (applicationComponent.getGroupId() != null) {
      return applicationComponent.getGroupId() + ':' + applicationComponent.getArtifactId() + ':'
          + applicationComponent.getVersion();
    }

    if (!applicationComponent.getPathnames().isEmpty()) {
      return applicationComponent.getPathnames().get(0);
    }

    return null;
  }
}
