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
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.brain.security.keypair.KeyPairFactory;
import com.sonatype.insight.brain.security.keystore.KeyStoreFactory;
import com.sonatype.insight.json.store.JsonUtils;

import static com.sonatype.insight.brain.dataaccess.configuration.saml.SamlConfigurationInternal.KEYSTORE_ALIAS;

@Named
@Singleton
public class SamlConfigurationAdapter
{
  private final SamlPasswordFactory samlPasswordFactory;

  @Inject
  public SamlConfigurationAdapter(final SamlPasswordFactory samlPasswordFactory) {
    this.samlPasswordFactory = samlPasswordFactory;
  }

  public SamlConfiguration toSamlConfiguration(SamlConfigurationInternal samlConfigurationInternal) {
    if (samlConfigurationInternal == null) {
      return null;
    }

    SamlConfiguration samlConfiguration;
    try {
      samlConfiguration = JsonUtils.parse(samlConfigurationInternal.getConfigurationJson(), SamlConfiguration.class);
    }
    catch (IOException e) {
      throw new UncheckedIOException("Failed to parse SAML configuration: " + e.getMessage(), e);
    }
    samlConfiguration.setId(samlConfigurationInternal.getId());

    loadKeyStoreData(samlConfigurationInternal, samlConfiguration);

    return samlConfiguration;
  }

  public SamlConfigurationInternal fromSamlConfiguration(SamlConfiguration samlConfiguration) {
    if (samlConfiguration == null) {
      return null;
    }

    if (samlConfiguration.getIdentityProviderName()
        .length() > SamlConfiguration.IDENTITY_PROVIDER_NAME_MAXIMUM_LENGTH)
    {
      throw new InvalidNameException(
          "Identity provider name must be " + SamlConfiguration.IDENTITY_PROVIDER_NAME_MAXIMUM_LENGTH +
              " characters or less.");
    }

    SamlConfigurationInternal result = new SamlConfigurationInternal();
    result.setId(samlConfiguration.getId());
    result.setConfigurationJson(JsonUtils.format(samlConfiguration));

    return result;
  }

  public void loadKeyStoreData(
      final SamlConfigurationInternal samlConfigurationInternal,
      final SamlConfiguration samlConfiguration)
  {
    try (ByteArrayInputStream keystoreInputStream = new ByteArrayInputStream(
        samlConfigurationInternal.getKeystore()))
    {
      KeyStore keyStore = KeyStoreFactory.createKeyStore();
      char[] keyStorePassword =
          samlPasswordFactory.decryptPassword(samlConfigurationInternal.getKeystorePasswordObfuscated());
      keyStore.load(keystoreInputStream, keyStorePassword);
      Certificate certificate = keyStore.getCertificate(KEYSTORE_ALIAS);
      samlConfiguration.setCertificate(certificate);
      PrivateKey privateKey = (PrivateKey) keyStore.getKey(KEYSTORE_ALIAS, keyStorePassword);
      samlConfiguration.setDecryptionKey(privateKey);

      KeyPair keyPair = KeyPairFactory.createKeyPair(certificate.getPublicKey(), privateKey);
      samlConfiguration.setSigningKeyPair(keyPair);
    }
    catch (Exception e) {
      throw new IllegalStateException("Could not load SAML keystore: " + e.getMessage(), e);
    }
  }
}
