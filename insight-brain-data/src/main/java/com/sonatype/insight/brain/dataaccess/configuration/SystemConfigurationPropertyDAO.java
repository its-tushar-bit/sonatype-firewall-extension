/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

/**
 * @since 1.33
 */
public class SystemConfigurationPropertyDAO
    extends AbstractOperationalSqlDAO<SystemConfigurationProperty>
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
    String sQuery = "SELECT entity FROM SystemConfigurationProperty entity WHERE entity.name=?1";
    return get(tx, sQuery, name);
  }

  public SystemConfigurationProperty getByNameNotNull(TransactionContext tx, String name) {
    SystemConfigurationProperty property = getByName(tx, name);
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
    SystemConfigurationProperty existingProperty = getByNameNotNull(tx, property.getName());
    property.setId(existingProperty.getId());
    super.update(tx, property);
  }
}
