/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Test;

public class FullTextSearchResourceAuditTest
    extends AbstractAuditTest
{
  @Test
  public void testSetStatus_Enabled() throws Exception {
    FullTextSearchStatusDTO fullTextSearchStatusDTO = new FullTextSearchStatusDTO();
    fullTextSearchStatusDTO.isEnabled = true;

    restRequest().body(fullTextSearchStatusDTO).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_FULL_TEXT_SEARCH, null);
    assertCustomData(auditDTO, "fullTextSearch", "enabled");
  }

  @Test
  public void testSetStatus_Disabled() throws Exception {
    FullTextSearchStatusDTO fullTextSearchStatusDTO = new FullTextSearchStatusDTO();
    fullTextSearchStatusDTO.isEnabled = false;

    restRequest().body(fullTextSearchStatusDTO).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_FULL_TEXT_SEARCH, null);
    assertCustomData(auditDTO, "fullTextSearch", "disabled");
  }

  @Test
  public void testSetStatus_Unauthorized() throws Exception {
    restRequest().with(unauthorizedUser()).body(new FullTextSearchStatusDTO()).put();

    assertAuditLog(AuditEvent.CONFIGURE_FULL_TEXT_SEARCH, "unauthorized");
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(FullTextSearchResource.RESOURCE_PATH, FullTextSearchResource.STATUS_PATH);
  }
}
