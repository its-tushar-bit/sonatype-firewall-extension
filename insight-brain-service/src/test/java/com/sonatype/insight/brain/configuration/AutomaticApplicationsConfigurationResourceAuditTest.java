/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration;
import com.sonatype.insight.brain.common.test.SlowTest;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class AutomaticApplicationsConfigurationResourceAuditTest
    extends AbstractAuditTest
{
  private Organization organization;

  @Before
  public void before() {
    organization = tempEntity.newOrganization();
  }

  @Test
  public void testUpdate_Enabled() throws Exception {
    automaticApplicationsConfigurationRequest().body(new AutomaticApplicationsConfiguration(true, organization.getId()))
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_AUTOMATIC_APPLICATIONS, null);
    assertCustomData(auditDTO, "automaticApplicationCreation", "enabled");
    assertParentOrganizationData(auditDTO, organization);
  }

  @Test
  public void testUpdate_Disabled() throws Exception {
    automaticApplicationsConfigurationRequest().body(new AutomaticApplicationsConfiguration(false, null)).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_AUTOMATIC_APPLICATIONS, null);
    assertCustomData(auditDTO, "automaticApplicationCreation", "disabled");
  }

  @Test
  public void testUpdate_Unauthorized() throws Exception {
    automaticApplicationsConfigurationRequest().body(new AutomaticApplicationsConfiguration(true, organization.getId()))
        .with(unauthorizedUser()).put();

    assertAuditLog(AuditEvent.CONFIGURE_AUTOMATIC_APPLICATIONS, "unauthorized");
  }

  private HttpRequest automaticApplicationsConfigurationRequest() {
    return restRequest().path(AutomaticApplicationsConfigurationResource.RESOURCE_PATH);
  }
}
