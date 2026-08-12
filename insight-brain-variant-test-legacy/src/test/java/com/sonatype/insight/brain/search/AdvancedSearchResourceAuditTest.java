/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Test;

@Category(SlowTest.class)
public class AdvancedSearchResourceAuditTest
    extends AbstractAuditTest
{
  @Test
  public void testSetStatus_Enabled() throws Exception {
    AdvancedSearchStatusDTO statusDTO = new AdvancedSearchStatusDTO();
    statusDTO.isEnabled = true;

    restRequest().body(statusDTO).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_ADVANCED_SEARCH, null);
    assertCustomData(auditDTO, "advancedSearch", "enabled");
  }

  @Test
  public void testSetStatus_Disabled() throws Exception {
    AdvancedSearchStatusDTO statusDTO = new AdvancedSearchStatusDTO();
    statusDTO.isEnabled = false;

    restRequest().body(statusDTO).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_ADVANCED_SEARCH, null);
    assertCustomData(auditDTO, "advancedSearch", "disabled");
  }

  @Test
  public void testSetStatus_Unauthorized() throws Exception {
    restRequest().with(unauthorizedUser()).body(new AdvancedSearchStatusDTO()).put();

    assertAuditLog(AuditEvent.CONFIGURE_ADVANCED_SEARCH, "unauthorized");
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(AdvancedSearchResource.RESOURCE_PATH, AdvancedSearchResource.STATUS_PATH);
  }
}
