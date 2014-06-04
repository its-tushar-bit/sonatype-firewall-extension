/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.component.ApplicationComponentDetailsDTO.PolicyViolationSummaryDTO;
import com.sonatype.insight.brain.component.ApplicationComponentDetailsDTO.PolicyViolationSummaryDTO.ReasonDTO;
import com.sonatype.insight.brain.dashboard.StageDetailDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.actions.WarnActionType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.organization.ApplicationAdapter;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.policy.StageTypeService;
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

  private final StageTypeService stageTypeService;

  @Inject
  public ComponentDetailService(ApplicationService appService, ApplicationAdapter appAdapter,
      ApplicationComponentDAO applicationComponentDAO, StageTypeService stageTypeService)
  {
    this.appService = appService;
    this.appAdapter = appAdapter;
    this.applicationComponentDAO = applicationComponentDAO;
    this.stageTypeService = stageTypeService;
  }

  public List<ApplicationComponentDetailsDTO> getApplicationDetailsByHash(String hash) {
    List<ApplicationComponentDetailsDTO> result = new ArrayList<>();

    PolicyEvaluationDAO policyEvaluationDAO = new PolicyEvaluationDAO();
    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();

    List<StageType> stageTypes = new ArrayList<StageType>();
    for (StageType stageType : stageTypeService.getLicensedStageTypes()) {
      if (!StageTypes.isIgnoredForDashboard(stageType.getId())) {
        stageTypes.add(stageType);
      }
    }

    // Get the list of applications the user can see
    List<Application> applications = appService.getApplications();
    for (Application application : applications) {
      if (!isComponentPartOfApplication(application, hash)) {
        // Ignore this application because it does not contain the specified component.
        continue;
      }

      ApplicationComponentDetailsDTO applicationComponentDetails = new ApplicationComponentDetailsDTO();

      Map<String, PolicyViolationSummaryDTO> policyViolationDTOsByPolicyId = new LinkedHashMap<>();
      Map<String, Map<String, StageDetailDTO>> stageDetailsByPolicyId = new LinkedHashMap<>();
      for (StageType stageType : stageTypes) {
        StageDetailDTO appStageDetailDTO = new StageDetailDTO(stageType.getId(), stageType.getName());
        applicationComponentDetails.stageDetails.add(appStageDetailDTO);

        PolicyEvaluation policyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndStageId(application.getId(),
            stageType.getId());
        if (policyEvaluation == null) {
          continue;
        }

        List<PolicyViolation> policyViolations = policyViolationDAO.getByEvaluationIdAndHash(policyEvaluation.getId(),
            hash);
        for (PolicyViolation policyViolation : policyViolations) {
          String policyId = policyViolation.getPolicyId();

          Map<String, StageDetailDTO> stageDetailsById = stageDetailsByPolicyId.get(policyId);
          if (stageDetailsById == null) {
            stageDetailsById = initStageDetails(stageTypes);
            stageDetailsByPolicyId.put(policyId, stageDetailsById);
          }
          StageDetailDTO policyStageDetailDTO = stageDetailsById.get(stageType.getId());
          policyStageDetailDTO.scanId = policyEvaluation.getScanId();
          policyStageDetailDTO.actionTypeId = policyViolation.getActionTypeId();
          policyStageDetailDTO.time = policyViolationDAO
              .getFirstOccurrence(application.getId(), stageType.getId(), policyViolation).getTime().getTime();
          if (getSeverity(appStageDetailDTO.actionTypeId) < getSeverity(policyStageDetailDTO.actionTypeId)) {
            appStageDetailDTO.actionTypeId = policyStageDetailDTO.actionTypeId;
          }

          PolicyViolationSummaryDTO policyViolationSummaryDTO = policyViolationDTOsByPolicyId.get(policyId);
          if (policyViolationSummaryDTO == null) {
            policyViolationSummaryDTO = new PolicyViolationSummaryDTO();
            policyViolationSummaryDTO.policyId = policyViolation.getPolicyId();
            policyViolationSummaryDTO.stageDetails.addAll(stageDetailsById.values());
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
        }
      }

      applicationComponentDetails.application = appAdapter.convert(application);
      applicationComponentDetails.policyViolations.addAll(policyViolationDTOsByPolicyId.values());
      result.add(applicationComponentDetails);
    }

    return result;
  }

  private int getSeverity(String actionTypeId) {
    if (actionTypeId == null) {
      return 0;
    }
    else if (WarnActionType.ID.equals(actionTypeId)) {
      return 1;
    }
    else if (FailActionType.ID.equals(actionTypeId)) {
      return 2;
    }
    throw new IllegalStateException("unknown action type: " + actionTypeId);
  }

  private Map<String, StageDetailDTO> initStageDetails(Collection<StageType> stageTypes) {
    Map<String, StageDetailDTO> stageDetailsById = new LinkedHashMap<>();
    for (StageType stageType : stageTypes) {
      StageDetailDTO stageDetailDTO = new StageDetailDTO(stageType.getId(), stageType.getName());
      stageDetailsById.put(stageDetailDTO.stageTypeId, stageDetailDTO);
    }
    return stageDetailsById;
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
