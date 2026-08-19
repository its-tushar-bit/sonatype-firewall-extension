/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.configuration.ProxyServerConfiguration;
import com.sonatype.insight.brain.security.RotatableSecrets;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.ProxyServerConfiguration.PROXY_SERVER_CONFIGURATION;

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
  public ProxyServerConfigurationDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public ProxyServerConfiguration get() {
    return getById(SINGLETON_ENTITY_ID);
  }

  public void set(final ProxyServerConfiguration proxyServerConfiguration) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      if (getById(tx, SINGLETON_ENTITY_ID) == null) {
        insert(tx, proxyServerConfiguration);
      }
      else {
        update(tx, proxyServerConfiguration);
      }
      tx.commit();
    }
  }

  @Override
  public int insert(final TransactionContext tx, final ProxyServerConfiguration entity) {
    validate(entity);
    entity.setId(SINGLETON_ENTITY_ID);
    return super.insert(tx, entity);
  }

  @Override
  public int update(final TransactionContext tx, final ProxyServerConfiguration entity) {
    validate(entity);
    entity.setId(SINGLETON_ENTITY_ID);
    return super.update(tx, entity);
  }

  public void delete() {
    ProxyServerConfiguration proxyServerConfiguration = get();
    if (proxyServerConfiguration != null) {
      delete(proxyServerConfiguration);
    }
  }

  private void validate(final ProxyServerConfiguration entity) {
    if (StringUtils.isBlank(entity.getHostname())) {
      throw new BadRequestException("Host is required.");
    }
    if (entity.getPort() <= 0 || entity.getPort() > 65535) {
      throw new BadRequestException("The port must be from the range 1 - 65535.");
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return PROXY_SERVER_CONFIGURATION;
  }

  @Override
  public Class<ProxyServerConfiguration> getEntityClass() {
    return ProxyServerConfiguration.class;
  }
}
