/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.configuration.SystemNoticeDAO;
import com.sonatype.insight.brain.model.configuration.SystemNotice;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class SystemNoticeServiceTest
    extends AbstractComponentTest
{
  @Inject
  private SystemNoticeService service;

  @Test
  public void testGetSystemNotice() {
    SystemNotice updated = service.getSystemNotice();
    assertThat(updated, is(notNullValue()));
  }

  @Test
  public void testUpdateSystemNotice_confirmThatSystemNoticeIsUpdated() {
    String message = "Show Me";
    SystemNotice systemNotice = new SystemNotice();
    systemNotice.setMessage(message);
    SystemNotice updated = service.updateSystemNotice(systemNotice);
    SystemNotice stored = new SystemNoticeDAO().get();
    assertThat(updated.getMessage(), equalTo(stored.getMessage()));
    assertThat(updated.isEnabled(), equalTo(stored.isEnabled()));
  }
}
