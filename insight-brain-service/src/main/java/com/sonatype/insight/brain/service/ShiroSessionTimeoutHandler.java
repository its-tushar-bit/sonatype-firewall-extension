/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.api.v2.service.ConfigurationListener;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;

import io.dropwizard.lifecycle.Managed;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;

@Named
@Singleton
public class ShiroSessionTimeoutHandler
    implements ConfigurationListener, Managed
{
  private final ApiConfigurationService configurationService;

  private final DefaultWebSessionManager defaultWebSessionManager;

  @Inject
  public ShiroSessionTimeoutHandler(
      ApiConfigurationService configurationService,
      DefaultWebSessionManager defaultWebSessionManager)
  {
    this.configurationService = configurationService;
    this.defaultWebSessionManager = defaultWebSessionManager;
  }

  @Override
  public void start() throws Exception {
    configurationChanged(Collections.singleton(SystemConfigurationProperty.SESSION_TIMEOUT_MINUTES));
  }

  @Override
  public void configurationChanged(Set<String> propertyNames) {
    if (propertyNames.contains(SystemConfigurationProperty.SESSION_TIMEOUT_MINUTES)) {
      int sessionTimeout = (int) configurationService.getConfigurationNoAuthz(
              Collections.singleton(SystemConfigurationProperty.SESSION_TIMEOUT_MINUTES))
          .get(SystemConfigurationProperty.SESSION_TIMEOUT_MINUTES);
      defaultWebSessionManager.setGlobalSessionTimeout(sessionTimeout * 60000L);
    }
  }
}
