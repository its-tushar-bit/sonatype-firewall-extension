/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.saml;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.error.exception.BadRequestException;

/**
 * @since 1.72
 */
@Named
@Singleton
public class SamlConfigurationDAO
{
  private final SamlConfigurationInternalDAO samlConfigurationInternalDAO;

  private final SamlConfigurationService samlConfigurationService;

  @Inject
  public SamlConfigurationDAO(
      final SamlConfigurationInternalDAO samlConfigurationInternalDAO,
      final SamlConfigurationService samlConfigurationService)
  {
    this.samlConfigurationInternalDAO = samlConfigurationInternalDAO;
    this.samlConfigurationService = samlConfigurationService;
  }

  public SamlConfiguration getById(String id) {
    return samlConfigurationService.toSamlConfiguration(samlConfigurationInternalDAO.getById(id));
  }

  /**
   * Returns the one and only SAML configuration or null if SAML is not configured.
   */
  public SamlConfiguration get() {
    return samlConfigurationService.toSamlConfiguration(samlConfigurationInternalDAO.get());
  }

  public void insert(SamlConfiguration samlConfiguration) {
    if (get() != null) {
      throw new BadRequestException("A SAML configuration already exists.");
    }

    SamlConfigurationInternal samlConfigurationInternal =
        samlConfigurationService.fromSamlConfiguration(samlConfiguration);
    samlConfigurationInternalDAO.insert(samlConfigurationInternal);

    samlConfiguration.setId(samlConfigurationInternal.getId());
    samlConfigurationService.loadKeyStoreData(samlConfigurationInternal, samlConfiguration);
  }

  public void update(SamlConfiguration samlConfiguration) {
    SamlConfigurationInternal samlConfigurationInternal =
        samlConfigurationService.fromSamlConfiguration(samlConfiguration);
    samlConfigurationInternalDAO.update(samlConfigurationInternal);

    samlConfigurationService.loadKeyStoreData(samlConfigurationInternal, samlConfiguration);
  }

  // A parameterless delete method may be needed if the SAML configuration is corrupt see CLM-14027
  public void delete() {
    SamlConfigurationInternal samlConfigurationInternal = samlConfigurationInternalDAO.get();
    if (samlConfigurationInternal != null) {
      samlConfigurationInternalDAO.delete(samlConfigurationInternal);
    }
  }
}
