/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.configuration.KeyValue;

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
}
