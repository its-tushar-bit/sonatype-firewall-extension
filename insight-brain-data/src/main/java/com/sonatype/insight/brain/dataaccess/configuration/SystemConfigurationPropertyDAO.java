/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlWithFallbackDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

/**
 * @since 1.33
 */
public class SystemConfigurationPropertyDAO
    extends AbstractOperationalSqlWithFallbackDAO<SystemConfigurationProperty>
{
  @Override
  public SystemConfigurationProperty getById(final TransactionContext tx, final String id) {
    return get(tx, "SELECT entity FROM SystemConfigurationProperty entity WHERE entity.id=?1", id);
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
    return getByName(tx, name, false);
  }

  private SystemConfigurationProperty getByName(TransactionContext tx, String name, boolean fetchForUpdate) {
    String sQuery = "SELECT entity FROM SystemConfigurationProperty entity WHERE entity.name=?1";
    return getWithGlobalFallback(tx, sQuery, fetchForUpdate, name);
  }

  public SystemConfigurationProperty getByNameNotNull(TransactionContext tx, String name) {
    return getByNameNotNull(tx, name, false);
  }

  private SystemConfigurationProperty getByNameNotNull(TransactionContext tx, String name, boolean fetchForUpdate) {
    SystemConfigurationProperty property = getByName(tx, name, fetchForUpdate);
    if (property == null) {
      throw new NotFoundException("A system configuration property '" + name + "' does not exist.");
    }
    return property;
  }

  public List<SystemConfigurationProperty> getAll() {
    return getList("SELECT scp FROM SystemConfigurationProperty scp");
  }

  @Override
  public void update(TransactionContext tx, SystemConfigurationProperty property) {
    SystemConfigurationProperty existingProperty = getByNameNotNull(tx, property.getName(), true);
    property.setId(existingProperty.getId());
    super.update(tx, property);
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
    SystemConfigurationProperty property = getByName(tx, name, true);
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
}
