/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.saml;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.72
 */
@Entity
@Table(name = "saml_configuration")
public class SamlConfigurationInternal
    implements HasStringId
{
  static final String KEYSTORE_ALIAS = "SAML";

  @Id
  @Column(name = "saml_configuration_id")
  private String id;

  @Column(name = "configuration_json")
  private String configurationJson;

  @Column(name = "keystore")
  private byte[] keyStoreBytes;

  @Column(name = "keystore_password_obfuscated")
  private char[] keyStorePasswordObfuscated;

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

  void setKeyStorePassword(char[] keyStorePassword) {
    this.keyStorePasswordObfuscated = keyStorePassword;
  }

  char[] getKeyStorePasswordObfuscated() {
    return keyStorePasswordObfuscated;
  }

  void setKeyStorePasswordObfuscated(char[] keyStorePasswordObfuscated) {
    this.keyStorePasswordObfuscated = keyStorePasswordObfuscated;
  }

  byte[] getKeyStoreBytes() {
    return keyStoreBytes;
  }

  void setKeyStoreBytes(byte[] keyStoreBytes) {
    this.keyStoreBytes = keyStoreBytes;
  }
}
