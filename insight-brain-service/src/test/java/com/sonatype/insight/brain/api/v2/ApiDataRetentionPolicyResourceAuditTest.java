/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiAgeDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiDataRetentionPoliciesDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRetentionPoliciesDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRetentionPolicyDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiSuccessMetricsRetentionPolicyDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Test;

@Category(SlowTest.class)
public class ApiDataRetentionPolicyResourceAuditTest
    extends AbstractAuditTest
{
  private HttpRequest restRequest(String organizationId) {
    return restRequest()
        .path(PublicApiPaths.DATA_RETENTION_POLICY_RESOURCE_PATH,
            ApiDataRetentionPolicyResource.ORGANIZATION_PATH)
        .parameter(organizationId);
  }

  @Test
  public void testSetDataRetentionPolicies_Organization() throws Exception {
    Organization org = tempEntity.newOrganization();
    ApiDataRetentionPoliciesDTO dto = new ApiDataRetentionPoliciesDTO();
    dto.applicationReports = new ApiReportRetentionPoliciesDTO();
    dto.applicationReports.stages.put(Stage.ID_BUILD,
        new ApiReportRetentionPolicyDTO(false, true, 30, ApiAgeDTO.fromString("2 weeks")));
    dto.successMetrics = new ApiSuccessMetricsRetentionPolicyDTO(false, true, ApiAgeDTO.fromString("1 year"));
    restRequest(org.getId()).body(dto).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_DATA_RETENTION, null);
    assertOrganizationData(auditDTO, org);
    assertCustomObject(auditDTO, "dataRetentionPolicies", dto);
  }

  @Test
  public void testSetDataRetentionPolicies_Unauthorized() throws Exception {
    Organization org = tempEntity.newOrganization();
    ApiDataRetentionPoliciesDTO dto = new ApiDataRetentionPoliciesDTO();
    restRequest(org.getId()).with(unauthorizedUser()).body(dto).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_DATA_RETENTION, "unauthorized");
    assertOrganizationData(auditDTO, org);
  }
}
