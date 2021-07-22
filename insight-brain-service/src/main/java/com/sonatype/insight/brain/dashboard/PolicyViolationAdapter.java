/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;

class PolicyViolationAdapter
{
  static PolicyViolationDTO createPolicyViolationDTO(
      Application application,
      PolicyEvaluation evaluation,
      PolicyViolation violation)
  {
    PolicyViolationDTO dto = new PolicyViolationDTO();
    dto.applicationId = application.getId();
    dto.applicationName = application.getName();
    dto.componentIdentifier = violation.getComponentIdentifier();
    dto.hash = violation.getHash();
    dto.id = violation.getId();
    dto.policyId = violation.getPolicyId();
    dto.policyName = violation.getPolicyName();
    dto.threatCategory = violation.getThreatCategory();
    dto.threatLevel = violation.getThreatLevel();
    dto.time = evaluation.getTime().getTime();
    dto.filename = violation.getFilename();
    dto.constraintFacts = violation.getConstraintFacts();
    return dto;
  }
}
