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
import com.sonatype.insight.brain.variant.LegacyServerTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@LegacyServerTest
public class NewInstancePopulatorAuditTest
    extends AbstractPolicyImportAuditTest
{
  @BeforeEach
  @Override
  public void setUp() {
    // Using DAOFactory instead of lookup as we have several tests using @ManualIqServerInit annotation
    roleDAO = daoFactory.createRoleDAO();
    policyDAO = daoFactory.createPolicyDAO();
  }

  @Test
  @ManualIqServerInit
  public void testPopulateIfNewInstance() throws Exception {
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = Arrays.asList(aComplexPolicy(), policy());
    policyExportResult.labels = Arrays.asList(label(), label(), label());
    policyExportResult.licenseThreatGroups = Collections.singletonList(licenseThreatGroup());
    policyExportResult.tags = Arrays.asList(tag(), tag(), tag(), tag());
    hdsRespondWith(policyExportResult).atUri(ReferencePolicyFetcher.REFERENCE_POLICY_PATH);
    startIqTestServer();

    getCLMServer().getInstance(NewInstancePopulator.class).populateIfNewInstance();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.IMPORT, null, SYSTEM_USER);
    assertOrganizationData(auditDTO, Organization.ROOT_ORGANIZATION_ID, "Root Organization");
    assertPolicyImportData(auditDTO, 2, 3, 1, 4);
    assertImportedPolicies(policyExportResult.policies, Organization.ROOT_ORGANIZATION_ID, "Root Organization",
        SYSTEM_USER);
  }

  @Test
  @ManualIqServerInit
  public void testPopulateIfNewInstance_BadGateway() throws Exception {
    hdsRespondWith("Internal Server Error").andStatus(500).atUri(ReferencePolicyFetcher.REFERENCE_POLICY_PATH);
    startIqTestServer();

    getCLMServer().getInstance(NewInstancePopulator.class).populateIfNewInstance();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.IMPORT, "server-error", SYSTEM_USER);
    assertOrganizationData(auditDTO, Organization.ROOT_ORGANIZATION_ID, "Root Organization");
    assertPolicyImportData(auditDTO, null, null, null, null);
  }

  @Override
  protected void startIqTestServer() throws Exception {
    // Start with the startup import disabled so this test can exercise populateIfNewInstance() explicitly below.
    // Server startup reconfigures logging, so re-arm the audit log capture after the server is running and before the
    // audited import is triggered.
    startIqTestServer(config -> config.setImportReferencePoliciesFromHDS(false));
    getCLMServer().getConfiguration().setImportReferencePoliciesFromHDS(true);
    getLogOutput().before();
    getLogOutput().clear();
  }
}
