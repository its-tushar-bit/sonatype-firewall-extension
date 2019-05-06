/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.PROXY_EXCLUDE_HOSTS;

/**
 * @since 1.65
 */
public class ProxyConfigurationDAO
{
  private final SystemConfigurationPropertyDAO configurationPropertyDAO = new SystemConfigurationPropertyDAO();

  public String getProxyExcludeHosts() {
    SystemConfigurationProperty configurationProperty = configurationPropertyDAO.getByName(PROXY_EXCLUDE_HOSTS);
    return configurationProperty == null ? "" : configurationProperty.getValue();
  }

  public void setProxyExcludeHosts(String proxyExcludeHosts) {
    configurationPropertyDAO.update(new SystemConfigurationProperty(PROXY_EXCLUDE_HOSTS, proxyExcludeHosts));
  }
}
