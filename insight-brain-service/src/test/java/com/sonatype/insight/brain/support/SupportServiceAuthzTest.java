/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import javax.inject.Inject;

import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

/**
 * @since 1.27
 */
@Category(SlowTest.class)
public class SupportServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private SupportService supportService;

  @Test
  @H2DiskTest
  public void testCreateSupportZip_Authorized() throws Exception {
    grantConfigureSystemPermission();
    supportService.createSupportZip(false, null, false);
  }

  @Test(expected = UnauthorizedException.class)
  public void testCreateSupportZip_Unauthorized() throws Exception {
    login();
    supportService.createSupportZip(false, null, false);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testCreateSupportZip_Unauthenticated() throws Exception {
    supportService.createSupportZip(false, null, false);
  }
}
