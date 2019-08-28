/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.saml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.stream.Stream;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.model.HasStringId;

import de.schlichtherle.util.ObfuscatedString;

/**
 * @since 1.72
 */
@Entity
@Table(name = "saml_configuration")
class SamlConfigurationInternal
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

  SamlConfiguration toSamlConfiguration() {
    SamlConfiguration samlConfiguration;
    try {
      samlConfiguration = JsonUtils.parse(configurationJson, SamlConfiguration.class);
    }
    catch (IOException e) {
      throw new UncheckedIOException("Failed to parse SAML configuration: " + e.getMessage(), e);
    }
    samlConfiguration.setId(id);

    loadKeyStoreData(samlConfiguration);

    return samlConfiguration;
  }

  public void loadKeyStoreData(SamlConfiguration samlConfiguration) {
    try (ByteArrayInputStream keystoreInputStream = new ByteArrayInputStream(keyStoreBytes)) {
      KeyStore keyStore = KeyStore.getInstance("pkcs12");
      char[] keyStorePassword = getKeyStorePassword();
      keyStore.load(keystoreInputStream, keyStorePassword);
      Certificate certificate = keyStore.getCertificate(KEYSTORE_ALIAS);
      samlConfiguration.setCertificate(certificate);
      PrivateKey privateKey = (PrivateKey) keyStore.getKey(KEYSTORE_ALIAS, keyStorePassword);
      samlConfiguration.setDecryptionKey(privateKey);
      samlConfiguration.setSigningKeyPair(new KeyPair(certificate.getPublicKey(), privateKey));
    }
    catch (Exception e) {
      throw new IllegalStateException("Could not load SAML keystore.", e);
    }
  }

  private char[] getKeyStorePassword() {
    // The obfuscated password is stored as a comma-separated list of long values, for ex:
    // B262BEF4066834E2, 1E31D4FF44C663F0, 2AF7E801C69AC83C
    long[] obfuscated = Stream.of(String.valueOf(keyStorePasswordObfuscated).split(","))
        .mapToLong(s -> Long.parseUnsignedLong(s, 16)).toArray();
    return new ObfuscatedString(obfuscated).toString().toCharArray();
  }

  void setKeyStorePassword(char[] keyStorePassword) {
    // ObfuscatedString.obfuscate returns a string that can be pasted directly into Java code, for ex:
    // new ObfuscatedString(new long[] {0xB262BEF4066834E2L, 0x1E31D4FF44C663F0L, 0x2AF7E801C69AC83CL}).toString() /* =>
    // "qwedqwdeq" */
    // We only need the long values in between curly braces.
    String obfuscated = ObfuscatedString.obfuscate(String.valueOf(keyStorePassword));
    keyStorePasswordObfuscated = obfuscated.substring(obfuscated.indexOf('{') + 1, obfuscated.indexOf('}')) //
        .replace("0x", "") //
        .replace("L", "") //
        .replace(" ", "") //
        .toCharArray();
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
