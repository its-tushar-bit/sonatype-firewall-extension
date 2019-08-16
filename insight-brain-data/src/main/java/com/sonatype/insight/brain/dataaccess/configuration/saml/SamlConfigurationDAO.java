/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.saml;

import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.error.exception.BadRequestException;

/**
 * @since 1.72
 */
public class SamlConfigurationDAO
{
  private static SamlConfigurationInternalDAO samlConfigurationInternalDAO = new SamlConfigurationInternalDAO();

  public SamlConfiguration getById(String id) {
    return SamlConfigurationInternal.toSamlConfiguration(samlConfigurationInternalDAO.getById(id));
  }

  /**
   * Returns the one and only SAML configuration or null if SAML is not configured.
   */
  public SamlConfiguration get() {
    return SamlConfigurationInternal.toSamlConfiguration(samlConfigurationInternalDAO.get());
  }

  public void insert(SamlConfiguration samlConfiguration) {
    if (get() != null) {
      throw new BadRequestException("A SAML configuration already exists.");
    }

    SamlConfigurationInternal samlConfigurationInternal =
        SamlConfigurationInternal.fromSamlConfiguration(samlConfiguration);
    samlConfigurationInternalDAO.insert(samlConfigurationInternal);

    samlConfiguration.setId(samlConfigurationInternal.getId());
  }

  public void update(SamlConfiguration samlConfiguration) {
    SamlConfigurationInternal samlConfigurationInternal =
        SamlConfigurationInternal.fromSamlConfiguration(samlConfiguration);
    samlConfigurationInternalDAO.update(samlConfigurationInternal);
  }

  public void delete(SamlConfiguration samlConfiguration) {
    SamlConfigurationInternal samlConfigurationInternal =
        SamlConfigurationInternal.fromSamlConfiguration(samlConfiguration);
    samlConfigurationInternalDAO.delete(samlConfigurationInternal);
  }
}
