/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.component.ApplicationComponentDetailsDTO.PolicyViolationSummaryDTO;
import com.sonatype.insight.brain.dashboard.StageDetailDTO;
import com.sonatype.insight.brain.dataaccess.OwnerComponentDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerComponent;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.organization.ApplicationAdapter;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationComparator;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.LicensedFeature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
/**
 * @since 1.11
 */
public class ComponentDetailService
{
  private static final Logger log = LoggerFactory.getLogger(ComponentDetailService.class);

  private final ApplicationService appService;

  private final OwnerComponentDAO applicationComponentDAO;

  private final StageTypeService stageTypeService;

  private final ProductLicense productLicense;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final PolicyViolationDAO policyViolationDAO;

  private final ApplicationAdapter appAdapter;

  @Inject
  public ComponentDetailService(
      ApplicationService appService,
      OwnerComponentDAO applicationComponentDAO,
      StageTypeService stageTypeService,
      ProductLicense productLicense,
      PolicyEvaluationDAO policyEvaluationDAO,
      PolicyViolationDAO policyViolationDAO,
      ApplicationAdapter appAdapter)
  {
    this.appService = appService;
    this.applicationComponentDAO = applicationComponentDAO;
    this.stageTypeService = stageTypeService;
    this.productLicense = productLicense;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.policyViolationDAO = policyViolationDAO;
    this.appAdapter = appAdapter;
  }

  public List<ApplicationComponentDetailsDTO> getApplicationDetailsByHash(String hash) {
    AuditData.get().setData("componentHash", hash);
    validateDashboardLicensed();

    long start = System.currentTimeMillis();

    List<ApplicationComponentDetailsDTO> result = new ArrayList<>();

    List<StageType> stageTypes = new ArrayList<>();
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

      Map<String, PolicyViolationSummaryDTO> policyViolationDTOsByPolicyAndConstraint = new LinkedHashMap<>();
      Map<String, Map<String, StageDetailDTO>> stageDetailsByPolicyAndConstraint = new LinkedHashMap<>();
      for (StageType stageType : stageTypes) {
        StageDetailDTO appStageDetailDTO = new StageDetailDTO(stageType.getId(), stageType.getName());
        applicationComponentDetails.stageDetails.add(appStageDetailDTO);

        PolicyEvaluation policyEvaluation = policyEvaluationDAO.getLastByOwnerIdAndStageId(application.getId(),
            stageType.getId());
        if (policyEvaluation == null) {
          continue;
        }

        List<PolicyViolation> policyViolations = policyViolationDAO
            .getActiveByOwnerIdAndStageIdAndHash(application.getId(), stageType.getId(), hash);
        if (policyViolations.isEmpty()) {
          continue;
        }

        // only set this value if we have violations
        appStageDetailDTO.time = policyEvaluation.getTime().getTime();
        appStageDetailDTO.scanId = policyEvaluation.getScanId();

        policyViolationDAO.loadConstraintFacts(policyViolations);
        for (PolicyViolation policyViolation : policyViolations) {
          String policyAndConstraintHashId = computeUniqueAppPolicyConstraintId(policyViolation);

          Map<String, StageDetailDTO> stageDetailsById =
              stageDetailsByPolicyAndConstraint.computeIfAbsent(policyAndConstraintHashId,
                  key -> initStageDetails(stageTypes));

          StageDetailDTO policyStageDetailDTO = stageDetailsById.get(stageType.getId());
          policyStageDetailDTO.scanId = policyEvaluation.getScanId();
          policyStageDetailDTO.actionTypeId = policyViolation.getActionTypeId();
          policyStageDetailDTO.time = policyViolation.getOpenTime().getTime();

          // Should always have the time/action of the first occurring violation for the stage, to indicate how long
          // violations have been around for this application.
          if (policyStageDetailDTO.time <= appStageDetailDTO.time) {
            appStageDetailDTO.time = policyStageDetailDTO.time;
            appStageDetailDTO.actionTypeId = policyStageDetailDTO.actionTypeId;
          }

          PolicyViolationSummaryDTO policyViolationSummaryDTO =
              policyViolationDTOsByPolicyAndConstraint.get(policyAndConstraintHashId);
          if (policyViolationSummaryDTO == null) {
            policyViolationSummaryDTO = new PolicyViolationSummaryDTO();
            policyViolationSummaryDTO.policyId = policyViolation.getPolicyId();
            policyViolationSummaryDTO.stageDetails.addAll(stageDetailsById.values());
            policyViolationDTOsByPolicyAndConstraint.put(policyAndConstraintHashId, policyViolationSummaryDTO);
          }
          // Use the values from the most recent policy violation
          if (policyViolationSummaryDTO.time < policyEvaluation.getTime().getTime()) {
            policyViolationSummaryDTO.policyName = policyViolation.getPolicyName();
            policyViolationSummaryDTO.threatLevel = policyViolation.getThreatLevel();
            policyViolationSummaryDTO.time = policyEvaluation.getTime().getTime();
          }
        }
      }

      applicationComponentDetails.application = appAdapter.convert(application, false);
      applicationComponentDetails.policyViolations.addAll(policyViolationDTOsByPolicyAndConstraint.values());
      result.add(applicationComponentDetails);
    }

    auditApplicationComponentDetails(hash, applications.size(), result.size());

    log.debug("Loaded component details from {} out of {} applications in {} ms", result.size(), applications.size(),
        System.currentTimeMillis() - start);

    return result;
  }

  private String computeUniqueAppPolicyConstraintId(PolicyViolation policyViolation) {
    return PolicyViolationComparator.computeUniqueAppPolicyConstraintId(policyViolation.getOwnerId(),
        policyViolation.getPolicyId(), policyViolation.getConstraintFacts());
  }

  private void auditApplicationComponentDetails(String hash, int inspectedApplicationCount, int resultRecordCount) {
    OwnerComponent applicationComponent = getLastByHash(hash);
    if (applicationComponent != null) {
      if (applicationComponent.getComponentIdentifier() != null) {
        AuditData.get().setComponentIdentifier(applicationComponent.getComponentIdentifier());
      }
      else {
        Optional<String> filename = new ComponentDisplayFilename().addPathnames(applicationComponent.getPathnames())
            .getFilename();
        AuditData.get().setData("componentFilename", filename.orElse(null));
      }
    }
    AuditData.get()
        .setData("inspectedApplicationCount", inspectedApplicationCount)
        .setData("resultRecordCount", resultRecordCount);
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
    List<OwnerComponent> appComponents = applicationComponentDAO.getByOwnerIdAndHash(application.getId(),
        hash);
    for (OwnerComponent appComponent : appComponents) {
      if (!StageTypes.isIgnoredForDashboard(appComponent.getStageTypeId())) {
        return true;
      }
    }
    return false;
  }

  public ComponentDisplayName getComponentNameByHash(String hash) {
    validateDashboardLicensed();

    OwnerComponent applicationComponent = getLastByHash(hash);
    if (applicationComponent == null) {
      throw new BadRequestException("Unknown component with hash " + hash + ".");
    }
    ComponentDisplayName componentNameDTO = null;
    if (applicationComponent.getComponentIdentifier() != null) {
      componentNameDTO = ComponentDisplayNameUtil.fromIdentifier(applicationComponent.getComponentIdentifier());
    }

    if (componentNameDTO == null && !applicationComponent.getPathnames().isEmpty()) {
      componentNameDTO = new ComponentDisplayName().add("Pathname", applicationComponent.getPathnames().get(0));
    }

    return componentNameDTO;
  }

  private OwnerComponent getLastByHash(String hash) {
    return applicationComponentDAO.getLastByHash(hash);
  }

  private void validateDashboardLicensed() {
    productLicense.validateFeature(LicensedFeature.DASHBOARD);
  }
}
