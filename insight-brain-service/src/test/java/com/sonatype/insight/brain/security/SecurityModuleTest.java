/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.ArrayList;
import java.util.List;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.configuration.ldap.LdapRealm;
import com.sonatype.insight.brain.testing.BrainInjectedTest;

import com.google.inject.Binder;
import org.apache.shiro.lang.util.LifecycleUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.junit.After;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SecurityModuleTest
    extends BrainInjectedTest
{
  @Inject
  private SecurityManager securityManager;

  @After
  @Override
  public void tearDown() throws Exception {
    LifecycleUtils.destroy(securityManager);
  }

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    binder.install(new SecurityModule());
  }

  @Test
  public void testRememberMeManagerIsNullToAvoidDeserializationVuln() {
    // CLM-6473
    assertThat(((DefaultWebSecurityManager) securityManager).getRememberMeManager()).isNull();
  }

  @Test
  public void testCheckRealmsOrdering() {
    List<Realm> realmsList = new ArrayList<>(((DefaultWebSecurityManager) securityManager).getRealms());
    assertThat(realmsList.get(0)).isInstanceOf(InternalRealm.class);
    assertThat(realmsList.get(1)).isInstanceOf(UserTokenRealm.class);
    assertThat(realmsList.get(2)).isInstanceOf(LdapRealm.class);
    assertThat(realmsList.get(3)).isInstanceOf(CrowdRealm.class);
    assertThat(realmsList.get(4)).isInstanceOf(ReverseProxyRealm.class);
    assertThat(realmsList.get(5)).isInstanceOf(SamlRealm.class);
  }
}
