/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyViolation;

@Named
@Singleton
public class PolicyViolationAdapter
{

  public List<PolicyViolationDTO> createPolicyViolationDTOs(Application application,
      List<PolicyViolation> policyViolations)
  {
    List<PolicyViolationDTO> policyViolationDTOs = new ArrayList<>();

    if (policyViolations == null) {
      return policyViolationDTOs;
    }

    for (PolicyViolation violation : policyViolations) {
      policyViolationDTOs.add(createPolicyViolationDTO(application, violation));
    }

    return policyViolationDTOs;
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
}
