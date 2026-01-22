/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.saml;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.api.v2.HasFeature;
import com.sonatype.insight.brain.dataaccess.configuration.saml.SamlConfigurationAdapter;
import com.sonatype.insight.brain.dataaccess.configuration.saml.SamlConfigurationInternal;
import com.sonatype.insight.brain.dataaccess.configuration.saml.SamlConfigurationInternalDAO;
import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.error.exception.BadRequestException;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature.SAML_ENABLED;

/**
 * @since 1.72
 */
@Named
@Singleton
@HasFeature(SAML_ENABLED)
public class SamlConfigurationService
{
  private final SamlConfigurationInternalDAO samlConfigurationInternalDAO;

  private final SamlConfigurationAdapter samlConfigurationAdapter;

  @Inject
  public SamlConfigurationService(
      final SamlConfigurationInternalDAO samlConfigurationInternalDAO,
      final SamlConfigurationAdapter samlConfigurationAdapter)
  {
    this.samlConfigurationInternalDAO = samlConfigurationInternalDAO;
    this.samlConfigurationAdapter = samlConfigurationAdapter;
  }

  public SamlConfiguration getById(String id) {
    return samlConfigurationAdapter.toSamlConfiguration(samlConfigurationInternalDAO.getById(id));
  }

  /**
   * Returns the one and only SAML configuration or null if SAML is not configured.
   */
  public SamlConfiguration get() {
    return samlConfigurationAdapter.toSamlConfiguration(samlConfigurationInternalDAO.get());
  }

  public void insert(SamlConfiguration samlConfiguration) {
    if (get() != null) {
      throw new BadRequestException("A SAML configuration already exists.");
    }

    SamlConfigurationInternal samlConfigurationInternal =
        samlConfigurationAdapter.fromSamlConfiguration(samlConfiguration);
    samlConfigurationInternalDAO.insert(samlConfigurationInternal);

    samlConfiguration.setId(samlConfigurationInternal.getId());
    samlConfigurationAdapter.loadKeyStoreData(samlConfigurationInternal, samlConfiguration);
  }

  public void update(SamlConfiguration samlConfiguration) {
    SamlConfigurationInternal samlConfigurationInternal =
        samlConfigurationAdapter.fromSamlConfiguration(samlConfiguration);
    samlConfigurationInternalDAO.update(samlConfigurationInternal);

    samlConfigurationAdapter.loadKeyStoreData(samlConfigurationInternal, samlConfiguration);
  }

  // A parameterless delete method may be needed if the SAML configuration is corrupt see CLM-14027
  public void delete() {
    SamlConfigurationInternal samlConfigurationInternal = samlConfigurationInternalDAO.get();
    if (samlConfigurationInternal != null) {
      samlConfigurationInternalDAO.delete(samlConfigurationInternal);
    }
  }
}
