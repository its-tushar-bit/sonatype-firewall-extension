/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.firewall;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FirewallServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  public FirewallService firewallService;

  @Test
  public void getFirewallStatus_Authorized() {
    grantGlobalPermission(Permission.READ);

    FirewallStatusDTO firewallStatusDTO = firewallService.getFirewallStatus();

    assertThat(firewallStatusDTO).isNotNull();
  }

  @Test(expected = UnauthenticatedException.class)
  public void getFirewallStatus_Unauthenticated() {
    firewallService.getFirewallStatus();
  }

  @Test(expected = UnauthorizedException.class)
  public void testPerformManifestScan_Unauthorized() {
    login();
    firewallService.getFirewallStatus();
  }
}
