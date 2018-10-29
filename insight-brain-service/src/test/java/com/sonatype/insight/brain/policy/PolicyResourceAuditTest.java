/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.Arrays;
import java.util.Collections;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;

import org.junit.Before;
import org.junit.Test;

public class PolicyResourceAuditTest
    extends AbstractPolicyImportAuditTest
{
  private Organization organization;

  @Before
  public void before() {
    organization = tempEntity.newOrganization();
  }

  @Test
  public void testImportPolicies() throws Exception {
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = Arrays.asList(policy(), policy());
    policyExportResult.labels = Arrays.asList(label(), label(), label());
    policyExportResult.licenseThreatGroups = Collections.singletonList(licenseThreatGroup());
    policyExportResult.tags = Arrays.asList(tag(), tag(), tag(), tag());

    restRequest(OwnerType.ORGANIZATION, organization.getId()).path("import").part("file", "file", policyExportResult)
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.IMPORT, null);
    assertOrganizationData(auditDTO, organization);
    assertPolicyImportData(auditDTO, 2, 3, 1, 4);
  }

  @Test
  public void testImportPolicies_Unauthorized() throws Exception {
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = Collections.singletonList(policy());

    restRequest(OwnerType.ORGANIZATION, organization.getId()).with(unauthorizedUser()).path("import")
        .part("file", "file", policyExportResult).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.IMPORT, "unauthorized");
    assertOrganizationData(auditDTO, organization);
    assertPolicyImportData(auditDTO, null, null, null, null);
  }

  private HttpRequest restRequest(OwnerType ownerType, String ownerId) {
    return restRequest().path(PolicyResource.RESOURCE_PATH).parameter(ownerType, ownerId);
  }

  private AuditDTO assertAuditLog(AuditEvent auditEvent, String error) {
    AuditDTO auditDTO = awaitLogEntries(auditEvent, 1).get(0);
    assertStandardData(auditDTO, auditEvent, error);
    return auditDTO;
  }
}
