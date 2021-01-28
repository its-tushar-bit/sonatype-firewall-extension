/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.policy.ActionDTO;
import com.sonatype.insight.brain.policy.ConstraintDTO;
import com.sonatype.insight.brain.policy.NotificationDTO;
import com.sonatype.insight.brain.service.AbstractComponentAuditTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.Feature;

import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Test;

import static com.sonatype.insight.brain.product.license.FirewallReleaseIntegrityLicenseListener.POLICY_NAME;
import static org.assertj.core.api.Assertions.assertThat;

public class FirewallReleaseIntegrityLicenseListenerAuditTest
    extends AbstractComponentAuditTest
{
  @Inject
  private InsightConfig insightConfig;

  @Inject
  private FirewallReleaseIntegrityLicenseListener firewallReleaseIntegrityLicenseListener;

  private final PolicyDAO policyDAO = new PolicyDAO();

  private final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO = new SystemConfigurationPropertyDAO();

  @After
  public void cleanup() {
    systemConfigurationPropertyDAO.delete(systemConfigurationPropertyDAO
        .getByName(SystemConfigurationProperty.FIREWALL_INTEGRITY_RATING_LICENSE_ENABLED));
    List<Policy> policies = policyDAO.getByName(POLICY_NAME);
    if (!policies.isEmpty()) {
      policyDAO.delete(policies.get(0));
    }
  }

  @Test
  public void testProductLicenseChanged() {
    insightConfig.setExperimentalFeatures(ImmutableMap.of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));

    firewallReleaseIntegrityLicenseListener.productLicenseChanged();

    Policy importedPolicy = policyDAO.getByName(POLICY_NAME).get(0);
    AuditDTO auditLog = awaitLogEntries(AuditEvent.CREATE_POLICY, 1).get(0);
    assertOrganizationData(auditLog, Organization.ROOT_ORGANIZATION_ID, "Root Organization");
    assertPolicyData(auditLog, importedPolicy, ConstraintDTO.transcribe(importedPolicy.getConstraints()));
  }

  private void assertPolicyData(
      AuditDTO auditDTO,
      Policy policy,
      List<ConstraintDTO> constraints)
  {
    String auditedPolicyId = (String) auditDTO.data.get("policyId");
    assertThat(auditedPolicyId).isNotNull();
    assertThat(new PolicyDAO().getById(auditedPolicyId)).isNotNull();
    assertCustomData(auditDTO, "policyName", policy.getName());
    assertCustomData(auditDTO, "policyThreatLevel", policy.getThreatLevel());
    assertCustomData(auditDTO, "policyGrandfatheringMode",
        policy.isPolicyViolationGrandfatheringAllowed() ? "allow" : "disallow");
    assertCustomObject(auditDTO, "policyConstraints", constraints);
    assertCustomObject(auditDTO, "actions", ActionDTO.transcribe(policy.getActions()));
    assertCustomObject(auditDTO, "notifications", NotificationDTO.transcribe(policy.getNotifications()));
  }
}
