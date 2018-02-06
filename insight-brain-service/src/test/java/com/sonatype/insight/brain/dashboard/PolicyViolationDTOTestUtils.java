/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.List;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyViolation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

class PolicyViolationDTOTestUtils
{

  static void assertPolicyViolationDTO(List<PolicyViolationDTO> dtos,
                                       PolicyViolation violation,
                                       Application application,
                                       Policy policy)
  {
    for (PolicyViolationDTO dto : dtos) {
      if (dto.id.equals(violation.getId())) {
        assertEquals(dto.applicationId, application.getId());
        assertEquals(dto.applicationName, application.getName());
        assertEquals(dto.componentIdentifier, violation.getComponentIdentifier());
        assertEquals(dto.hash, violation.getHash());
        assertEquals(dto.id, violation.getId());
        assertEquals(dto.policyId, policy.getId());
        assertEquals(dto.policyName, policy.getName());
        assertEquals(dto.threatCategory, violation.getThreatCategory());
        assertEquals(dto.threatLevel, violation.getThreatLevel());
        assertEquals(dto.time, violation.getTime().getTime());
        assertEquals(dto.filename, violation.getFilename());
        return;
      }
    }
    fail("Unable to match violation with DTOs.");
  }

}
