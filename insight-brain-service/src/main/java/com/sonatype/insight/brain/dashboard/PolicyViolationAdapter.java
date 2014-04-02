/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;

@Named
@Singleton
public class PolicyViolationAdapter
{
  private PolicyViolationDAO policyViolationDAO;

  @Inject
  public PolicyViolationAdapter(PolicyViolationDAO policyViolationDAO) {
    this.policyViolationDAO = policyViolationDAO;
  }

  public PolicyViolationDTO createPolicyViolationDTO(Application application, PolicyViolation violation) {
    PolicyViolationDTO dto = new PolicyViolationDTO();
    dto.applicationId = application.getId();
    dto.applicationName = application.getName();
    dto.artifactId = violation.getArtifactId();
    dto.groupId = violation.getGroupId();
    dto.hash = violation.getHash();
    dto.id = violation.getId();
    dto.policyEvaluationId = violation.getPolicyEvaluationId();
    dto.policyId = violation.getPolicyId();
    dto.policyName = violation.getPolicyName();
    dto.threatCategory = violation.getThreatCategory();
    dto.threatLevel = violation.getThreatLevel();
    dto.version = violation.getVersion();
    return dto;
  }

  public List<PolicyViolationDTO> createPolicyViolationDTOs(Application application, List<PolicyEvaluation> evaluations)
  {
    List<PolicyViolationDTO> policyViolationDTOs = new ArrayList<>();

    if (evaluations == null) {
      return policyViolationDTOs;
    }

    for (PolicyEvaluation evaluation : evaluations) {
      policyViolationDTOs.addAll(createPolicyViolationDTOs(application, evaluation));
    }

    return policyViolationDTOs;
  }

  public List<PolicyViolationDTO> createPolicyViolationDTOs(Application application, PolicyEvaluation evaluation) {
    List<PolicyViolationDTO> policyViolationDTOs = new ArrayList<>();

    if (evaluation == null) {
      return policyViolationDTOs;
    }

    List<PolicyViolation> violations = policyViolationDAO.getByEvaluationId(evaluation.getId());
    if (violations != null) {
      for (PolicyViolation violation : violations) {
        policyViolationDTOs.add(createPolicyViolationDTO(application, violation));
      }
    }

    return policyViolationDTOs;
  }
}
