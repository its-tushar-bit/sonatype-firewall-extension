/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.configuration.SystemNoticeDAO;
import com.sonatype.insight.brain.model.configuration.SystemNotice;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@ComponentH2Test
public class SystemNoticeServiceTest
    extends AbstractComponentH2Test
{
  @Inject
  private SystemNoticeDAO systemNoticeDAO;

  @Inject
  private SystemNoticeService service;

  @Test
  public void testGetSystemNotice() {
    SystemNotice updated = service.getSystemNotice();
    assertThat(updated).isNotNull();
  }

  @Test
  public void testUpdateSystemNotice_confirmThatSystemNoticeIsUpdated() {
    String message = "Show Me";
    SystemNotice systemNotice = new SystemNotice();
    systemNotice.setMessage(message);
    SystemNotice updated = service.updateSystemNotice(systemNotice);
    SystemNotice stored = systemNoticeDAO.get();
    assertThat(updated.getMessage()).isEqualTo(stored.getMessage());
    assertThat(updated.isEnabled()).isEqualTo(stored.isEnabled());
  }
}
