/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;

@Named
public class ActivePolicyViolationsWithActionFailService
{
  private final ApplicationDAO applicationDAO;

  private final PolicyViolationDAO policyViolationDAO;

  @Inject
  public ActivePolicyViolationsWithActionFailService(
      ApplicationDAO applicationDAO,
      PolicyViolationDAO policyViolationDAO)
  {
    this.applicationDAO = applicationDAO;
    this.policyViolationDAO = policyViolationDAO;
  }

  @Authorize(permission = Permission.READ)
  public List<PolicyViolationWithoutConstraintFactsDTO> getActiveViolationsWithActionFail(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId,
      String stageId)
  {
    Application app = applicationDAO.getByPublicIdNotNull(applicationPublicId);

    List<PolicyViolation> policyViolations = policyViolationDAO.getActiveByApplicationIdAndStageIdAndActionId(
        app.getId(), stageId, Action.ID_FAIL);

    return policyViolations.stream()
        .map(policyViolation -> createPolicyViolationWithoutConstraintFactsDTO(app, policyViolation))
        .toList();
  }

  private PolicyViolationWithoutConstraintFactsDTO createPolicyViolationWithoutConstraintFactsDTO(
      Application application,
      PolicyViolation policyViolation)
  {
    PolicyViolationWithoutConstraintFactsDTO dto = new PolicyViolationWithoutConstraintFactsDTO();
    dto.applicationId = application.getId();
    dto.applicationName = application.getName();
    dto.componentIdentifier = policyViolation.getComponentIdentifier();
    dto.hash = policyViolation.getHash();
    dto.id = policyViolation.getId();
    dto.policyId = policyViolation.getPolicyId();
    dto.policyName = policyViolation.getPolicyName();
    dto.threatCategory = policyViolation.getThreatCategory();
    dto.threatLevel = policyViolation.getThreatLevel();
    dto.filename = policyViolation.getFilename();
    return dto;
  }
}
