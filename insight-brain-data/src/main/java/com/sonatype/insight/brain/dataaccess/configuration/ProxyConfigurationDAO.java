/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.configuration.ProxyConfiguration;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;

/**
 * @since MIGRATE_PROXY_CONFIG
 */
public class ProxyConfigurationDAO
    extends AbstractOperationalSqlDAO<ProxyConfiguration>
{
  static final String SINGLETON_ENTITY_ID = "proxy-configuration";

  /**
   * @return The proxy configuration or {@code null} if none.
   */
  public ProxyConfiguration get() {
    return getById(SINGLETON_ENTITY_ID);
  }

  public void set(ProxyConfiguration proxyConfiguration) {
    update(proxyConfiguration);
  }

  @Override
  protected ProxyConfiguration getById(TransactionContext tx, String id) {
    return get(tx, "SELECT entity FROM ProxyConfiguration entity WHERE entity.id=?1", SINGLETON_ENTITY_ID);
  }

  @Override
  public void insert(TransactionContext tx, ProxyConfiguration proxyConfiguration) {
    validate(proxyConfiguration);
    proxyConfiguration.setId(SINGLETON_ENTITY_ID);
    super.insert(tx, proxyConfiguration);
  }

  @Override
  public void update(TransactionContext tx, ProxyConfiguration proxyConfiguration) {
    validate(proxyConfiguration);
    proxyConfiguration.setId(SINGLETON_ENTITY_ID);
    super.update(tx, proxyConfiguration);
  }

  public void delete() {
    ProxyConfiguration proxyConfiguration = get();
    if (proxyConfiguration != null) {
      delete(proxyConfiguration);
    }
  }

  private void validate(ProxyConfiguration proxyConfiguration) {
    if (StringUtils.isBlank(proxyConfiguration.getHostname())) {
      throw new BadRequestException("Host is required.");
    }
    if (proxyConfiguration.getPort() <= 0 || proxyConfiguration.getPort() > 65535) {
      throw new BadRequestException("The port must be from the range 1 - 65535.");
    }
  }
}
