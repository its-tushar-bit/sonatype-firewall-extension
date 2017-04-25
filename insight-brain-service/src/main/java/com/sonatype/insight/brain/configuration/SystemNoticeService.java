/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.configuration.SystemNoticeDAO;
import com.sonatype.insight.brain.model.configuration.SystemNotice;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;

/**
 * @since 1.28.0
 */
@Named
public class SystemNoticeService
{
  private final SystemNoticeDAO systemNoticeDAO;

  @Inject
  public SystemNoticeService(SystemNoticeDAO systemNoticeDAO) {
    this.systemNoticeDAO = systemNoticeDAO;
  }

  /**
   * @since 1.28.0
   */
  public SystemNotice getSystemNotice() {
    return systemNoticeDAO.get();
  }

  /**
   * @since 1.28.0
   */
  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public SystemNotice updateSystemNotice(SystemNotice systemNotice) {
    systemNoticeDAO.update(systemNotice);
    return systemNotice;
  }
}
