/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.variant.AbstractComponentH2Test;

import java.util.Set;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.service.ConfigurationListener;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.security.InsightSessionManager;
import com.sonatype.insight.brain.tenancy.TenantManaged;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.AbstractSessionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@ComponentH2Test
public class ShiroSessionTimeoutHandlerTest
    extends AbstractComponentH2Test
{
  @Inject
  private ShiroSessionTimeoutHandler shiroSessionTimeoutHandler;

  @Inject
  private SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  @Inject
  private InsightSessionManager insightSessionManager;

  @AfterEach
  public void resetSessionTimeoutState() {
    insightSessionManager.setGlobalSessionTimeout(AbstractSessionManager.DEFAULT_GLOBAL_SESSION_TIMEOUT);
    insightSessionManager.setTenantSessionTimeout(null);
  }

  @Test
  public void testShiroSessionTimeout() {
    assertThat(shiroSessionTimeoutHandler).isInstanceOf(ConfigurationListener.class);
    assertThat(shiroSessionTimeoutHandler).isInstanceOf(TenantManaged.class);
  }

  @Test
  public void testConfigurationChanged() {
    systemConfigurationPropertyDAO.set(SystemConfigurationProperty.SESSION_TIMEOUT_MINUTES, "5");

    shiroSessionTimeoutHandler.configurationChanged(Set.of(SystemConfigurationProperty.SESSION_TIMEOUT_MINUTES));

    Session session = insightSessionManager.start(null);
    assertThat(session.getTimeout()).isEqualTo(5 * 60 * 1000);
  }

  @Test
  public void testConfigurationChanged_OtherProperty() {
    systemConfigurationPropertyDAO.set(SystemConfigurationProperty.SESSION_TIMEOUT_MINUTES, "5");

    shiroSessionTimeoutHandler.configurationChanged(Set.of("other"));

    Session session = insightSessionManager.start(null);
    assertThat(session.getTimeout()).isEqualTo(AbstractSessionManager.DEFAULT_GLOBAL_SESSION_TIMEOUT);
  }

  @Test
  public void testRegister() {
    systemConfigurationPropertyDAO.set(SystemConfigurationProperty.SESSION_TIMEOUT_MINUTES, "5");

    shiroSessionTimeoutHandler.register();

    Session session = insightSessionManager.start(null);
    assertThat(session.getTimeout()).isEqualTo(5 * 60 * 1000);
  }

  @Test
  public void testIncludeGlobalTenantDuringRegistration() {
    assertThat(shiroSessionTimeoutHandler.includeGlobalTenantDuringRegistration()).isTrue();
  }
}
