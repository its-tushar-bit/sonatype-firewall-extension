/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuditTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.junit.jupiter.api.Test;

@ComponentH2Test
public class FirewallReleaseIntegrityLicenseListenerAuditTest
    extends AbstractComponentH2AuditTest
{
  @Inject
  private FirewallReleaseIntegrityLicenseListener firewallReleaseIntegrityLicenseListener;

  @Test
  public void testProductLicenseChanged() {
    firewallReleaseIntegrityLicenseListener.productLicenseChanged();

    AuditDTO auditLog = awaitLogEntries(AuditEvent.CONFIGURE_CONTINUOUS_MONITORING, 1).get(0);
    assertRepositoryContainerData(auditLog);
    assertCustomData(auditLog, "stageId", StageTypes.PROXY.getId());
  }
}
