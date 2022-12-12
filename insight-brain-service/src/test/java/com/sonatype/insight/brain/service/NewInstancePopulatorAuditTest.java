/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.Arrays;
import java.util.Collections;

import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.hds.ReferencePolicyFetcher;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.policy.AbstractPolicyImportAuditTest;
import com.sonatype.insight.brain.policy.PolicyExportResult;

import org.junit.Ignore;
import org.junit.Test;

public class NewInstancePopulatorAuditTest
    extends AbstractPolicyImportAuditTest
{
  @Test
  @ManualServerInit
  @Ignore // fix in CLM-23389 and un-ignore
  public void testPopulateIfNewInstance() throws Exception {
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = Arrays.asList(aComplexPolicy(), policy());
    policyExportResult.labels = Arrays.asList(label(), label(), label());
    policyExportResult.licenseThreatGroups = Collections.singletonList(licenseThreatGroup());
    policyExportResult.tags = Arrays.asList(tag(), tag(), tag(), tag());
    hdsRespondWith(policyExportResult).atUri(ReferencePolicyFetcher.REFERENCE_POLICY_PATH);
    initServer();

    getCLMServer().getInstance(NewInstancePopulator.class).populateIfNewInstance();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.IMPORT, null, SYSTEM_USER);
    assertOrganizationData(auditDTO, Organization.ROOT_ORGANIZATION_ID, "Root Organization");
    assertPolicyImportData(auditDTO, 2, 3, 1, 4);
    assertImportedPolicies(policyExportResult.policies, Organization.ROOT_ORGANIZATION_ID, "Root Organization",
        SYSTEM_USER);
  }

  @Test
  @ManualServerInit
  @Ignore // fix in CLM-23389 and un-ignore
  public void testPopulateIfNewInstance_BadGateway() throws Exception {
    hdsRespondWith("Internal Server Error").andStatus(500).atUri(ReferencePolicyFetcher.REFERENCE_POLICY_PATH);
    initServer();

    getCLMServer().getInstance(NewInstancePopulator.class).populateIfNewInstance();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.IMPORT, "bad-gateway", SYSTEM_USER);
    assertOrganizationData(auditDTO, Organization.ROOT_ORGANIZATION_ID, "Root Organization");
    assertPolicyImportData(auditDTO, null, null, null, null);
  }

  @Override
  protected void initServer() throws Exception {
    initServer(config -> config.setImportRefrencePoliciesFromHDS(true));
  }
}
