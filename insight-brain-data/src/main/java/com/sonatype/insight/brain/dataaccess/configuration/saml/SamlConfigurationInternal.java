/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.saml;

import com.sonatype.insight.model.HasStringId;

import jakarta.persistence.Column;
import jakarta.persistence.Table;

/**
 * @since 1.72
 */
@Table(name = "saml_configuration")
public class SamlConfigurationInternal
    implements HasStringId
{
  static final String KEYSTORE_ALIAS = "SAML";

  @Column(name = "saml_configuration_id")
  private String id;

  private String configurationJson;

  private byte[] keystore;

  private char[] keystorePasswordObfuscated;

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
    this.keystorePasswordObfuscated = keyStorePassword;
  }

  char[] getKeystorePasswordObfuscated() {
    return keystorePasswordObfuscated;
  }

  void setKeystorePasswordObfuscated(char[] keystorePasswordObfuscated) {
    this.keystorePasswordObfuscated = keystorePasswordObfuscated;
  }

  byte[] getKeystore() {
    return keystore;
  }

  void setKeystore(byte[] keystore) {
    this.keystore = keystore;
  }
}
