/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

@Category(SlowTest.class)
public class AutomaticSourceControlConfigurationResourceAuditTest
    extends AbstractAuditTest
{
  @Test
  public void testUpdateAutomaticSourceControl_Enabled() throws Exception {
    automaticSourceControlConfigurationRequest().body(
        new AutomaticSourceControlConfiguration(true)).put();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_AUTOMATIC_SOURCE_CONTROL, null);
    assertCustomData(auditDTO, "automaticSourceControlConfiguration", "enabled");
  }

  @Test
  public void testUpdateAutomaticSourceControl_Disabled() throws Exception {
    automaticSourceControlConfigurationRequest().body(
        new AutomaticSourceControlConfiguration(false)).put();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_AUTOMATIC_SOURCE_CONTROL, null);
    assertCustomData(auditDTO, "automaticSourceControlConfiguration", "disabled");
  }

  @Test
  public void testUpdateAutomaticSourceControl_Unauthorized() throws Exception {
    automaticSourceControlConfigurationRequest().body(
        new AutomaticSourceControlConfiguration(true)).with(unauthorizedUser()).put();
    assertAuditLog(AuditEvent.CONFIGURE_AUTOMATIC_SOURCE_CONTROL, "unauthorized");
  }

  private HttpRequest automaticSourceControlConfigurationRequest() {
    return restRequest().path(AutomaticSourceControlConfigurationResource.RESOURCE_PATH);
  }
}
