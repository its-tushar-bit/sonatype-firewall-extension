/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.configuration.ProxyServerConfiguration;
import com.sonatype.insight.brain.security.RotatableSecrets;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;

/**
 * @since 1.84
 */
@Named
@Singleton
public class ProxyServerConfigurationDAO
    extends AbstractOperationalSqlDAO<ProxyServerConfiguration>
    implements RotatableSecrets
{
  public static final String SINGLETON_ENTITY_ID = "proxy-server-configuration";

  @Inject
  public ProxyServerConfigurationDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  /**
   * @return The proxy server configuration or {@code null} if none.
   */
  public ProxyServerConfiguration get() {
    return getById(SINGLETON_ENTITY_ID);
  }

  public void set(ProxyServerConfiguration proxyServerConfiguration) {
    update(proxyServerConfiguration);
  }

  @Override
  public ProxyServerConfiguration getById(TransactionContext tx, String id) {
    return super.getById(tx, SINGLETON_ENTITY_ID);
  }

  @Override
  public void insert(TransactionContext tx, ProxyServerConfiguration proxyServerConfiguration) {
    validate(proxyServerConfiguration);
    proxyServerConfiguration.setId(SINGLETON_ENTITY_ID);
    super.insert(tx, proxyServerConfiguration);
  }

  @Override
  public void update(TransactionContext tx, ProxyServerConfiguration proxyServerConfiguration) {
    validate(proxyServerConfiguration);
    proxyServerConfiguration.setId(SINGLETON_ENTITY_ID);
    super.update(tx, proxyServerConfiguration);
  }

  public void delete() {
    ProxyServerConfiguration proxyServerConfiguration = get();
    if (proxyServerConfiguration != null) {
      delete(proxyServerConfiguration);
    }
  }

  private void validate(ProxyServerConfiguration proxyServerConfiguration) {
    if (StringUtils.isBlank(proxyServerConfiguration.getHostname())) {
      throw new BadRequestException("Host is required.");
    }
    if (proxyServerConfiguration.getPort() <= 0 || proxyServerConfiguration.getPort() > 65535) {
      throw new BadRequestException("The port must be from the range 1 - 65535.");
    }
  }
}
