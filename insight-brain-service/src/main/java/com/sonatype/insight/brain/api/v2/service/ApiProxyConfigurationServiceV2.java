/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.ApiProxyConfigurationDTOV2;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.configuration.ProxyConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.ProxyConfiguration;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;

import static java.lang.String.join;

/**
 * @since 1.65
 */
@Named
public class ApiProxyConfigurationServiceV2
{
  private final ProxyConfigurationDAO proxyConfigurationDAO;

  @Inject
  public ApiProxyConfigurationServiceV2(ProxyConfigurationDAO proxyConfigurationDAO) {
    this.proxyConfigurationDAO = proxyConfigurationDAO;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public ApiProxyConfigurationDTOV2 update(ApiProxyConfigurationDTOV2 configuration) {
    ProxyConfiguration proxyConfiguration = proxyConfigurationDAO.get();
    proxyConfiguration.setExcludeHosts(join(", ", configuration.getProxyExcludeHosts()));
    proxyConfigurationDAO.set(proxyConfiguration);
    auditProxyConfiguration(configuration);
    return configuration;
  }

  public ApiProxyConfigurationDTOV2 get() {
    ProxyConfiguration proxyConfiguration = proxyConfigurationDAO.get();
    return new ApiProxyConfigurationDTOV2(proxyConfiguration.getExcludeHostsList());
  }

  private void auditProxyConfiguration(ApiProxyConfigurationDTOV2 proxy) {
    AuditData.get().setData("proxyConfiguration", proxy);
  }
}
