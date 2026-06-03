/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.relay;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.relay.RelayConfiguration;
import com.sonatype.insight.brain.security.RotatableSecrets;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.RelayConfiguration.RELAY_CONFIGURATION;

/**
 * Singleton DAO for the relay integration configuration. The single row is keyed by
 * {@link #SINGLETON_ENTITY_ID}; {@link #insert} and {@link #update} silently coerce the
 * entity id to that value, so callers cannot accidentally create a second row.
 */
@Named
@Singleton
public class RelayConfigurationDAO
    extends AbstractOperationalSqlDAO<RelayConfiguration>
    implements RotatableSecrets
{
  public static final String SINGLETON_ENTITY_ID = "relay-configuration";

  @Inject
  public RelayConfigurationDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public RelayConfiguration get() {
    return getById(SINGLETON_ENTITY_ID);
  }

  public void set(final RelayConfiguration relayConfiguration) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      if (getById(tx, SINGLETON_ENTITY_ID) == null) {
        insert(tx, relayConfiguration);
      }
      else {
        update(tx, relayConfiguration);
      }
      tx.commit();
    }
  }

  @Override
  public void insert(final TransactionContext tx, final RelayConfiguration entity) {
    entity.setId(SINGLETON_ENTITY_ID);
    super.insert(tx, entity);
  }

  @Override
  public void update(final TransactionContext tx, final RelayConfiguration entity) {
    entity.setId(SINGLETON_ENTITY_ID);
    super.update(tx, entity);
  }

  public void delete() {
    RelayConfiguration relayConfiguration = get();
    if (relayConfiguration != null) {
      delete(relayConfiguration);
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return RELAY_CONFIGURATION;
  }

  @Override
  public Class<RelayConfiguration> getEntityClass() {
    return RelayConfiguration.class;
  }
}
