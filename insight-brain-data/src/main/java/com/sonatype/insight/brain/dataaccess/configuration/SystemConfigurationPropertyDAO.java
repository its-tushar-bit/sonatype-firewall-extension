/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlWithFallbackDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.SystemConfigurationProperty.SYSTEM_CONFIGURATION_PROPERTY;

/**
 * @since 1.33
 */
@Named
@Singleton
public class SystemConfigurationPropertyDAO
    extends AbstractOperationalSqlWithFallbackDAO<SystemConfigurationProperty>
{
  @Inject
  public SystemConfigurationPropertyDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public SystemConfigurationProperty getByNameNotNull(String name) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByNameNotNull(tx, name);
    }
  }

  public SystemConfigurationProperty getByName(String name) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByName(tx, name);
    }
  }

  public SystemConfigurationProperty getByName(TransactionContext tx, String name) {
    return getByNameInternal(tx, name, false);
  }

  private SystemConfigurationProperty getByNameInternal(
      final TransactionContext tx,
      final String name,
      final boolean fetchForUpdate)
  {
    return getWithGlobalFallback(tx, t -> {
      var query = t.dsl()
          .selectFrom(SYSTEM_CONFIGURATION_PROPERTY)
          .where(SYSTEM_CONFIGURATION_PROPERTY.NAME.eq(name));
      return fetchForUpdate
          ? query.forUpdate().fetchOneInto(SystemConfigurationProperty.class)
          : query.fetchOneInto(SystemConfigurationProperty.class);
    }, fetchForUpdate);
  }

  public SystemConfigurationProperty getByNameNotNull(TransactionContext tx, String name) {
    return getByNameNotNull(tx, name, false);
  }

  private SystemConfigurationProperty getByNameNotNull(TransactionContext tx, String name, boolean fetchForUpdate) {
    SystemConfigurationProperty property = getByNameInternal(tx, name, fetchForUpdate);
    if (property == null) {
      throw new NotFoundException("A system configuration property '" + name + "' does not exist.");
    }
    return property;
  }

  @Override
  public void update(TransactionContext tx, SystemConfigurationProperty property) {
    SystemConfigurationProperty existingProperty = getByNameNotNull(tx, property.getName(), true);
    property.setId(existingProperty.getId());
    tx.dsl()
        .update(SYSTEM_CONFIGURATION_PROPERTY)
        .set(SYSTEM_CONFIGURATION_PROPERTY.NAME, property.getName())
        .set(SYSTEM_CONFIGURATION_PROPERTY.VALUE, property.getValue())
        .where(SYSTEM_CONFIGURATION_PROPERTY.SYSTEM_CONFIGURATION_PROPERTY_ID.eq(property.getId()))
        .execute();
  }

  @Override
  public void insert(TransactionContext tx, SystemConfigurationProperty entity) {
    if (entity.getId() == null) {
      entity.setId(UUID.randomUUID().toString());
    }
    tx.dsl()
        .insertInto(SYSTEM_CONFIGURATION_PROPERTY)
        .set(SYSTEM_CONFIGURATION_PROPERTY.SYSTEM_CONFIGURATION_PROPERTY_ID, entity.getId())
        .set(SYSTEM_CONFIGURATION_PROPERTY.NAME, entity.getName())
        .set(SYSTEM_CONFIGURATION_PROPERTY.VALUE, entity.getValue())
        .execute();
  }

  public String get(String name) {
    try (TransactionContext tx = createTransactionContext()) {
      return get(tx, name);
    }
  }

  public String get(TransactionContext tx, String name) {
    SystemConfigurationProperty property = getByName(tx, name);
    if (property == null) {
      return null;
    }
    return property.getValue();
  }

  public void set(String name, String value) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      set(tx, name, value);
      tx.commit();
    }
  }

  public void set(TransactionContext tx, String name, String value) {
    SystemConfigurationProperty property = getByNameInternal(tx, name, true);
    if (value == null) {
      if (property != null) {
        delete(tx, property);
      }
    }
    else {
      if (property == null) {
        property = new SystemConfigurationProperty(name, value);
        insert(tx, property);
      }
      else {
        property.setValue(value);
        update(tx, property);
      }
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return SYSTEM_CONFIGURATION_PROPERTY;
  }

  @Override
  public Class<SystemConfigurationProperty> getEntityClass() {
    return SystemConfigurationProperty.class;
  }
}
