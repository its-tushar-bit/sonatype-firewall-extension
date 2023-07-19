/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.saml;

import java.io.ByteArrayOutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.UUID;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.dataaccess.TransactionContext;

import sun.security.tools.keytool.CertAndKeyGen;
import sun.security.x509.X500Name;

/**
 * @since 1.72
 */
@SuppressWarnings("restriction")
class SamlConfigurationInternalDAO
    extends AbstractOperationalSqlDAO<SamlConfigurationInternal>
{
  // Visible for tests
  public static final long TEN_YEARS_IN_SECONDS = Duration.ofDays(10 * 365).getSeconds();

  /**
   * Returns the one and only SAML configuration or null if SAML is not configured.
   */
  SamlConfigurationInternal get() {
    String sQuery = "SELECT entity FROM SamlConfigurationInternal entity";
    return createQuery(sQuery).forceSingleResult().get();
  }
  
  @Override
  public void insert(TransactionContext tx, SamlConfigurationInternal entity) {
    generateKeyStore(entity);

    super.insert(tx, entity);
  }

  @Override
  public void update(TransactionContext tx, SamlConfigurationInternal entity) {
    // The keystore info cannot be updated and has to be retrieved from the existing SAML configuration.
    SamlConfigurationInternal currentEntity = get();
    entity.setKeyStorePasswordObfuscated(currentEntity.getKeyStorePasswordObfuscated());
    entity.setKeyStoreBytes(currentEntity.getKeyStoreBytes());

    super.update(tx, entity);
  }

  private void generateKeyStore(SamlConfigurationInternal samlConfigurationInternal) {
    try {
      // Create an empty keystore.
      KeyStore keyStore = KeyStore.getInstance("pkcs12");
      char[] keyStorePassword = generatePassword();
      keyStore.load(null, keyStorePassword);

      // Generate a private key and a self-signed certificate.
      CertAndKeyGen certAndKeyGen = new CertAndKeyGen("RSA", "SHA256WithRSA", null);
      certAndKeyGen.generate(2048);
      X509Certificate[] certificateChain = new X509Certificate[1];
      certificateChain[0] =
          certAndKeyGen.getSelfCertificate(new X500Name("CN=SAML KeyStore"), TEN_YEARS_IN_SECONDS);
      PrivateKey privateKey = certAndKeyGen.getPrivateKey();

      // Save the key and certificate in the keystore.
      keyStore.setKeyEntry(SamlConfigurationInternal.KEYSTORE_ALIAS, privateKey, keyStorePassword, certificateChain);

      samlConfigurationInternal.setKeyStorePassword(keyStorePassword);
      try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
        keyStore.store(byteArrayOutputStream, keyStorePassword);
        samlConfigurationInternal.setKeyStoreBytes(byteArrayOutputStream.toByteArray());
      }
    }
    catch (Exception e) {
      throw new IllegalStateException("Could not generate SAML keystore.", e);
    }
  }

  private char[] generatePassword() {
    return UUID.randomUUID().toString().replaceAll("-", "").toCharArray();
  }
}
