/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import javax.inject.Inject;

import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.service.AbstractComponentAuditTest;

import org.junit.After;
import org.junit.Test;

public class FirewallReleaseIntegrityLicenseListenerAuditTest
    extends AbstractComponentAuditTest
{
  @Inject
  private FirewallReleaseIntegrityLicenseListener firewallReleaseIntegrityLicenseListener;

  @Inject
  private SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  @Inject
  private PolicyMonitoringDAO policyMonitoringDAO;

  @After
  public void cleanup() {
    systemConfigurationPropertyDAO.delete(systemConfigurationPropertyDAO
        .getByName(SystemConfigurationProperty.FIREWALL_INTEGRITY_RATING_LICENSE_ENABLED));
    policyMonitoringDAO.getAll().forEach(policyMonitoringDAO::delete);
  }

  @Test
  public void testProductLicenseChanged() {
    firewallReleaseIntegrityLicenseListener.productLicenseChanged();

    AuditDTO auditLog = awaitLogEntries(AuditEvent.CONFIGURE_CONTINUOUS_MONITORING, 1).get(0);
    assertRepositoryContainerData(auditLog);
    assertCustomData(auditLog, "stageId", StageTypes.PROXY.getId());
  }
}
