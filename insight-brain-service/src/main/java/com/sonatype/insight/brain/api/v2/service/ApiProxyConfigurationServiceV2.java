/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.api.v2.dto.ApiProxyConfigurationDTOV2;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.configuration.ProxyServerConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.ProxyServerConfiguration;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;

import static java.lang.String.join;

/**
 * @since 1.65
 */
@Named
@Singleton
public class ApiProxyConfigurationServiceV2
{
  private final ProxyServerConfigurationDAO proxyServerConfigurationDAO;

  private final List<ProxyServerConfigurationListener> proxyServerConfigurationListeners;

  @Inject
  public ApiProxyConfigurationServiceV2(
      ProxyServerConfigurationDAO proxyServerConfigurationDAO,
      List<ProxyServerConfigurationListener> proxyConfigurationListeners)
  {
    this.proxyServerConfigurationDAO = proxyServerConfigurationDAO;
    this.proxyServerConfigurationListeners = proxyConfigurationListeners;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public ApiProxyConfigurationDTOV2 update(ApiProxyConfigurationDTOV2 configuration) {
    ProxyServerConfiguration proxyConfiguration = proxyServerConfigurationDAO.get();
    proxyConfiguration.setExcludeHosts(join(", ", configuration.getProxyExcludeHosts()));
    proxyServerConfigurationDAO.set(proxyConfiguration);
    auditProxyConfiguration(configuration);
    applyProxyServerConfigurationToClients();
    return configuration;
  }

  public void applyProxyServerConfigurationToClients() {
    proxyServerConfigurationListeners.forEach(ProxyServerConfigurationListener::proxyServerConfigurationChanged);
  }

  public ApiProxyConfigurationDTOV2 get() {
    ProxyServerConfiguration proxyConfiguration = proxyServerConfigurationDAO.get();
    return new ApiProxyConfigurationDTOV2(proxyConfiguration.getExcludeHostsList());
  }

  private void auditProxyConfiguration(ApiProxyConfigurationDTOV2 proxy) {
    AuditData.get().setData("proxyConfiguration", proxy);
  }
}
