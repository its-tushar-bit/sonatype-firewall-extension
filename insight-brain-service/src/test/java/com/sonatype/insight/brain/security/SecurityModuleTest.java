/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import javax.inject.Inject;

import com.google.inject.Binder;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SecurityModuleTest
    extends InjectedTest
{
  @Inject
  private SecurityManager securityManager;

  @Override
  public void configure(Binder binder) {
    binder.install(new SecurityModule());
  }

  @Test
  public void testRememberMeManagerIsNullToAvoidDeserializationVuln() {
    // CLM-6473
    assertThat(((DefaultWebSecurityManager) securityManager).getRememberMeManager()).isNull();
  }
}
