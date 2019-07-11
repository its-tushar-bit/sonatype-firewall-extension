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
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;
import com.sonatype.insight.mock.hds.HdsMockServer.HdsConfigurator;

import org.junit.Test;

public class NewInstancePopulatorAuditTest
    extends AbstractPolicyImportAuditTest
{
  @Test
  @ManualServerInit
  public void testPopulateIfNewInstance() throws Exception {
    PolicyExportResult policyExportResult = new PolicyExportResult();
    initServer((HdsConfigurator) hdsServer -> {
      policyExportResult.policies = Arrays.asList(aComplexPolicy(), policy());
      policyExportResult.labels = Arrays.asList(label(), label(), label());
      policyExportResult.licenseThreatGroups = Collections.singletonList(licenseThreatGroup());
      policyExportResult.tags = Arrays.asList(tag(), tag(), tag(), tag());

      hdsServer.setResponseForURI(ReferencePolicyFetcher.REFERENCE_POLICY_PATH, policyExportResult, 200);
    });

    getCLMServer().getInstance(NewInstancePopulator.class).populateIfNewInstance();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.IMPORT, null, SYSTEM_USER);
    assertOrganizationData(auditDTO, Organization.ROOT_ORGANIZATION_ID, "Root Organization");
    assertPolicyImportData(auditDTO, 2, 3, 1, 4);
    assertImportedPolicies(policyExportResult.policies, Organization.ROOT_ORGANIZATION_ID, "Root Organization",
        SYSTEM_USER);
  }

  @Test
  @ManualServerInit
  public void testPopulateIfNewInstance_BadGateway() throws Exception {
    initServer((HdsConfigurator) hdsServer -> hdsServer
        .setResponseForURI(ReferencePolicyFetcher.REFERENCE_POLICY_PATH, new PolicyExportResult(), 500));

    getCLMServer().getInstance(NewInstancePopulator.class).populateIfNewInstance();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.IMPORT, "bad-gateway", SYSTEM_USER);
    assertOrganizationData(auditDTO, Organization.ROOT_ORGANIZATION_ID, "Root Organization");
    assertPolicyImportData(auditDTO, null, null, null, null);
  }

  private void initServer(HdsConfigurator hdsConfigurator) throws Exception {
    // disable startup import and instead call it manually to properly setup log capture after DropWizard's log setup
    Configurator configurator = config -> config.setImportRefrencePoliciesFromHDS(false);
    initServer(configurator, hdsConfigurator);
    getCLMServer().getConfiguration().setImportRefrencePoliciesFromHDS(true);
  }
}
