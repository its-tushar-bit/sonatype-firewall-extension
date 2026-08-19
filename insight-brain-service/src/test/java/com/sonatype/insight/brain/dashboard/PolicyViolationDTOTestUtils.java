/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.List;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class PolicyViolationDTOTestUtils
{
  static void assertPolicyViolationDTO(
      List<PolicyViolationDTO> dtos,
      PolicyViolation violation,
      Application application,
      PolicyEvaluation evaluation,
      Policy policy)
  {
    for (PolicyViolationDTO dto : dtos) {
      if (dto.id.equals(violation.getId())) {
        assertThat(application.getId()).isEqualTo(dto.applicationId);
        assertThat(application.getName()).isEqualTo(dto.applicationName);
        assertThat(violation.getComponentIdentifier()).isEqualTo(dto.componentIdentifier);
        assertThat(violation.getHash()).isEqualTo(dto.hash);
        assertThat(violation.getId()).isEqualTo(dto.id);
        assertThat(policy.getId()).isEqualTo(dto.policyId);
        assertThat(policy.getName()).isEqualTo(dto.policyName);
        assertThat(violation.getThreatCategory()).isEqualTo(dto.threatCategory);
        assertThat(violation.getThreatLevel()).isEqualTo(dto.threatLevel);
        assertThat(evaluation.getTime().getTime()).isEqualTo(dto.time);
        assertThat(violation.getFilename()).isEqualTo(dto.filename);
        return;
      }
    }
    fail("Unable to match violation with DTOs.");
  }
}
