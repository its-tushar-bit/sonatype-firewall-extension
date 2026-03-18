/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration;

import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.configuration.SystemNotice;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

@Category(SlowTest.class)
public class SystemNoticeServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private SystemNoticeService service;

  @Test
  public void testUpdateSystemNotice_whenAuthorizedForConfigureSystem() {
    grantConfigureSystemPermission();
    service.updateSystemNotice(new SystemNotice());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdateSystemNotice_failsWhenNotAuthenticated() {
    service.updateSystemNotice(new SystemNotice());
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdateSystemNotice_failsWhenNotAuthorizedForConfigureSystem() {
    login();
    service.updateSystemNotice(new SystemNotice());
  }
}
