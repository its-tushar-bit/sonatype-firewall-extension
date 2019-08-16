/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.saml;

import java.io.IOException;
import java.io.UncheckedIOException;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.72
 */
@Entity
@Table(name = "saml_configuration")
class SamlConfigurationInternal
    implements HasStringId
{
  @Id
  @Column(name = "saml_configuration_id")
  private String id;

  @Column(name = "configuration_json")
  private String configurationJson;

  public SamlConfigurationInternal() {
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getConfigurationJson() {
    return configurationJson;
  }

  public void setConfigurationJson(String configurationJson) {
    this.configurationJson = configurationJson;
  }

  SamlConfiguration toSamlConfiguration() {
    SamlConfiguration result;
    try {
      result = JsonUtils.parse(configurationJson, SamlConfiguration.class);
    }
    catch (IOException e) {
      throw new UncheckedIOException("Failed to parse SAML configuration: " + e.getMessage(), e);
    }
    result.setId(id);

    return result;
  }

  static SamlConfiguration toSamlConfiguration(SamlConfigurationInternal samlConfigurationInternal) {
    if (samlConfigurationInternal == null) {
      return null;
    }
    return samlConfigurationInternal.toSamlConfiguration();
  }

  public static SamlConfigurationInternal fromSamlConfiguration(SamlConfiguration samlConfiguration) {
    if (samlConfiguration == null) {
      return null;
    }

    SamlConfigurationInternal result = new SamlConfigurationInternal();
    result.setId(samlConfiguration.getId());
    result.setConfigurationJson(JsonUtils.format(samlConfiguration));

    return result;
  }
}
