/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.dataaccess.TransactionContext;

@Named
@Singleton
public class AutomaticSourceControlConfigurationDAO
{
  private final SystemConfigurationPropertyDAO configPropertyDAO;

  @Inject
  public AutomaticSourceControlConfigurationDAO(final SystemConfigurationPropertyDAO configPropertyDAO) {
    this.configPropertyDAO = configPropertyDAO;
  }

  public boolean isSourceControlConfigurationEnabled() {
    try (TransactionContext tx = configPropertyDAO.createTransactionContext()) {
      return isSourceControlConfigurationEnabled(tx);
    }
  }

  public boolean isSourceControlConfigurationEnabled(TransactionContext tx) {
    SystemConfigurationProperty configProperty = configPropertyDAO
        .getByNameNotNull(tx, SystemConfigurationProperty.AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_ENABLED);
    return Boolean.parseBoolean(configProperty.getValue());
  }

  public void setSourceControlConfigurationEnabled(boolean enabled) {
    try (TransactionContext tx = configPropertyDAO.createTransactionContext()) {
      tx.begin();
      setSourceControlConfigurationEnabled(tx, enabled);
      tx.commit();
    }
  }

  public void setSourceControlConfigurationEnabled(TransactionContext tx, boolean enabled) {
    configPropertyDAO.update(tx, new SystemConfigurationProperty(
        SystemConfigurationProperty.AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_ENABLED, Boolean.toString(enabled)));
  }
}
