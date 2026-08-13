/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @since 1.27
 */
@ComponentH2Test
public class SupportServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private SupportService supportService;

  @Test
  @H2DiskTest
  public void testCreateSupportZip_Authorized() throws Exception {
    grantConfigureSystemPermission();
    supportService.createSupportZip(false, null, false);
  }

  @Test
  public void testCreateSupportZip_Unauthorized() throws Exception {
    login();
    assertThrows(UnauthorizedException.class, () -> supportService.createSupportZip(false, null, false));
  }

  @Test
  public void testCreateSupportZip_Unauthenticated() throws Exception {
    assertThrows(UnauthenticatedException.class, () -> supportService.createSupportZip(false, null, false));
  }
}
