/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.configuration.SystemNotice;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Test;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class SystemNoticeResourceAuditTest
    extends AbstractAuditTest
{
  @Test
  public void testUpdateSystemNotice_Enabled() throws Exception {
    SystemNotice notice = new SystemNotice();
    notice.setEnabled(true);
    notice.setMessage("notice");
    systemNoticeRequest().body(notice).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_SYSTEM_NOTICE, null);
    assertCustomData(auditDTO, "systemNoticeDisplay", "enabled");
    assertCustomData(auditDTO, "systemNoticeText", notice.getMessage());
  }

  @Test
  public void testUpdateSystemNotice_Disabled() throws Exception {
    SystemNotice notice = new SystemNotice();
    notice.setEnabled(false);
    systemNoticeRequest().body(notice).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_SYSTEM_NOTICE, null);
    assertCustomData(auditDTO, "systemNoticeDisplay", "disabled");
  }

  @Test
  public void testUpdateSystemNotice_Unauthorized() throws Exception {
    systemNoticeRequest().with(unauthorizedUser()).body(new SystemNotice()).put();

    assertAuditLog(AuditEvent.CONFIGURE_SYSTEM_NOTICE, "unauthorized");
  }

  private HttpRequest systemNoticeRequest() {
    return restRequest().path(SystemNoticeResource.RESOURCE_PATH);
  }
}
