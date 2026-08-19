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
public class AutomaticApplicationsConfigurationDAO
{
  private final SystemConfigurationPropertyDAO configPropertyDAO;

  @Inject
  public AutomaticApplicationsConfigurationDAO(final SystemConfigurationPropertyDAO configPropertyDAO) {
    this.configPropertyDAO = configPropertyDAO;
  }

  public boolean isEnabled() {
    try (TransactionContext tx = configPropertyDAO.createTransactionContext()) {
      return isEnabled(tx);
    }
  }

  public boolean isEnabled(TransactionContext tx) {
    SystemConfigurationProperty configProperty = configPropertyDAO
        .getByNameNotNull(tx, SystemConfigurationProperty.AUTOMATIC_APPLICATION_CREATION_ENABLED);
    return Boolean.parseBoolean(configProperty.getValue());
  }

  public void setEnabled(boolean enabled) {
    try (TransactionContext tx = configPropertyDAO.createTransactionContext()) {
      tx.begin();
      setEnabled(tx, enabled);
      tx.commit();
    }
  }

  public void setEnabled(TransactionContext tx, boolean enabled) {
    configPropertyDAO.update(tx, new SystemConfigurationProperty(
        SystemConfigurationProperty.AUTOMATIC_APPLICATION_CREATION_ENABLED, Boolean.toString(enabled)));
  }

  public String getOrganizationId() {
    try (TransactionContext tx = configPropertyDAO.createTransactionContext()) {
      return getOrganizationId(tx);
    }
  }

  public String getOrganizationId(TransactionContext tx) {
    return configPropertyDAO
        .getByNameNotNull(tx, SystemConfigurationProperty.AUTOMATIC_APPLICATION_CREATION_ORGANIZATION_ID)
        .getValue();
  }

  public void setOrganizationId(String orgId) {
    try (TransactionContext tx = configPropertyDAO.createTransactionContext()) {
      tx.begin();
      setOrganizationId(tx, orgId);
      tx.commit();
    }
  }

  public void setOrganizationId(TransactionContext tx, String orgId) {
    configPropertyDAO.update(tx, new SystemConfigurationProperty(
        SystemConfigurationProperty.AUTOMATIC_APPLICATION_CREATION_ORGANIZATION_ID, orgId));
  }
}
