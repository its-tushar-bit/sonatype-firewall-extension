/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Test;

public class SuccessMetricsResourceAuditTest
    extends AbstractAuditTest
{
  @Test
  public void testUpdate_Enabled() throws Exception {
    SuccessMetricsConfigurationDTO configuration = new SuccessMetricsConfigurationDTO();
    configuration.enabled = true;
    successMetricsRequest().body(configuration).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_SUCCESS_METRICS, null);
    assertCustomData(auditDTO, "successMetricsFeature", "enabled");
  }

  @Test
  public void testUpdate_Disabled() throws Exception {
    successMetricsRequest().body(new SuccessMetricsConfigurationDTO()).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_SUCCESS_METRICS, null);
    assertCustomData(auditDTO, "successMetricsFeature", "disabled");
  }

  @Test
  public void testUpdate_Unauthorized() throws Exception {
    successMetricsRequest().with(unauthorizedUser()).body(new SuccessMetricsConfigurationDTO()).put();

    assertAuditLog(AuditEvent.CONFIGURE_SUCCESS_METRICS, "unauthorized");
  }

  private HttpRequest successMetricsRequest() {
    return restRequest().path(SuccessMetricsResource.RESOURCE_PATH);
  }
}
