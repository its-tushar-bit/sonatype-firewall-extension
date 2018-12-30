/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.PolicyAuditDTO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiPolicyViolationResourceV2AuditTest
    extends AbstractAuditTest
{
  @Test
  public void testGetPolicyViolations() throws Exception {
    Organization org = tempEntity.newOrganization();
    tempEntity.newApplication(org.getId());
    Policy policy1 = tempEntity.newPolicy(org);
    Policy policy2 = tempEntity.newPolicy(org);
    tempEntity.newApplicationWithParent();
    String unknownPolicyId = "unknownPolicyId";

    restRequest().path(PublicApiPaths.POLICY_VIOLATION_RESOURCE_PATH_V2)
        .query("p", policy1.getId(), policy2.getId(), unknownPolicyId).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_POLICY_VIOLATIONS, null);
    PolicyAuditDTO[] actuals = objectMapper.convertValue(auditDTO.data.get("selectedPolicies"), PolicyAuditDTO[].class);

    assertThat(actuals).containsExactlyInAnyOrder(
        new PolicyAuditDTO(policy1.getId(), policy1),
        new PolicyAuditDTO(policy2.getId(), policy2),
        new PolicyAuditDTO(unknownPolicyId, null));
    assertCustomData(auditDTO, "inspectedApplicationCount", 2);
  }
}
