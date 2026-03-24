/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.configuration.KeyValue;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.KeyValue.KEY_VALUE;

@Named
@Singleton
public class KeyValueDAO
    extends AbstractOperationalSqlDAO<KeyValue>
{
  @Inject
  public KeyValueDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public Table<?> getJooqTable() {
    return KEY_VALUE;
  }

  @Override
  public Class<KeyValue> getEntityClass() {
    return KeyValue.class;
  }

  public KeyValue getByKey(final String key) {
    return getById(key);
  }

  public String getValue(final String key) {
    KeyValue keyValue = getById(key);
    if (keyValue == null) {
      return null;
    }
    return keyValue.getValue();
  }

  public void setValue(final String key, final String value) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      setValue(tx, key, value);
      tx.commit();
    }
  }

  public void setValue(final TransactionContext tx, final String key, final String value) {
    KeyValue keyValue = getById(tx, key);
    if (keyValue == null) {
      keyValue = new KeyValue();
      keyValue.setKey(key);
      keyValue.setValue(value);
      insert(tx, keyValue);
    }
    else {
      keyValue.setValue(value);
      update(tx, keyValue);
    }
  }

  public void deleteByKey(final String key) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      deleteByKey(tx, key);
      tx.commit();
    }
  }

  public void deleteByKey(final TransactionContext tx, final String key) {
    tx.dsl()
        .deleteFrom(KEY_VALUE)
        .where(KEY_VALUE.KEY.eq(key))
        .execute();
  }
}
