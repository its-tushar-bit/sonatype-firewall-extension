/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.Collections;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.api.v2.service.ConfigurationListener;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.security.InsightSessionManager;
import com.sonatype.insight.brain.tenancy.TenantManaged;

@Named
@Singleton
public class ShiroSessionTimeoutHandler
    implements ConfigurationListener, TenantManaged
{
  private final ApiConfigurationService configurationService;

  private final InsightSessionManager insightSessionManager;

  @Inject
  public ShiroSessionTimeoutHandler(
      ApiConfigurationService configurationService,
      InsightSessionManager insightSessionManager)
  {
    this.configurationService = configurationService;
    this.insightSessionManager = insightSessionManager;
  }

  @Override
  public void register() {
    configurationChanged(Collections.singleton(SystemConfigurationProperty.SESSION_TIMEOUT_MINUTES));
  }

  @Override
  public void configurationChanged(Set<String> propertyNames) {
    if (propertyNames.contains(SystemConfigurationProperty.SESSION_TIMEOUT_MINUTES)) {
      int sessionTimeout = (int) configurationService.getConfigurationNoAuthz(
              Collections.singleton(SystemConfigurationProperty.SESSION_TIMEOUT_MINUTES))
          .get(SystemConfigurationProperty.SESSION_TIMEOUT_MINUTES);
      insightSessionManager.setTenantSessionTimeout(sessionTimeout * 60000L);
    }
  }

  @Override
  public boolean includeGlobalTenantDuringRegistration() {
    return true;
  }
}
