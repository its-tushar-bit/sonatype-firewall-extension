/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.configuration.SystemNoticeDAO;
import com.sonatype.insight.brain.model.configuration.SystemNotice;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;

/**
 * @since 1.29.0
 */
@Named
public class SystemNoticeService
{
  private final SystemNoticeDAO systemNoticeDAO;

  @Inject
  public SystemNoticeService(SystemNoticeDAO systemNoticeDAO) {
    this.systemNoticeDAO = systemNoticeDAO;
  }

  public SystemNotice getSystemNotice() {
    return systemNoticeDAO.get();
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public SystemNotice updateSystemNotice(SystemNotice systemNotice) {
    systemNoticeDAO.update(systemNotice);
    auditSystemNoticeUpdate(systemNotice);
    return systemNotice;
  }

  private void auditSystemNoticeUpdate(final SystemNotice systemNotice) {
    if (systemNotice.isEnabled()) {
      AuditData.get().setData("systemNoticeDisplay", "enabled").setData("systemNoticeText", systemNotice.getMessage());
    }
    else {
      AuditData.get().setData("systemNoticeDisplay", "disabled");
    }
  }
}
