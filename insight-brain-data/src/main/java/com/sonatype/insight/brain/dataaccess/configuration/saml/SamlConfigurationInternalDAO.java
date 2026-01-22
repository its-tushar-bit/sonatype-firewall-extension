/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.saml;

import java.io.ByteArrayOutputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.security.certificate.CertificateFactory;
import com.sonatype.insight.brain.security.keypair.KeyPairFactory;
import com.sonatype.insight.brain.security.keystore.KeyStoreFactory;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.72
 */
@Named
@Singleton
public class SamlConfigurationInternalDAO
    extends AbstractOperationalSqlDAO<SamlConfigurationInternal>
{
  private final SamlPasswordFactory samlPasswordFactory;

  @Inject
  public SamlConfigurationInternalDAO(
      final OperationalDataStore operationalDataStore,
      final SamlPasswordFactory samlPasswordFactory)
  {
    super(operationalDataStore);
    this.samlPasswordFactory = samlPasswordFactory;
  }

  /**
   * Returns the one and only SAML configuration or null if SAML is not configured.
   */
  public SamlConfigurationInternal get() {
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
      KeyStore keyStore = KeyStoreFactory.createKeyStore();
      char[] keyStorePassword = generatePassword();
      keyStore.load(null, keyStorePassword);

      KeyPair keyPair = KeyPairFactory.generateKeyPair();
      X509Certificate[] certificate = CertificateFactory.createCertificate(keyPair);

      // Save the key and certificate in the keystore.
      keyStore.setKeyEntry(SamlConfigurationInternal.KEYSTORE_ALIAS, keyPair.getPrivate(), keyStorePassword,
          certificate);

      samlConfigurationInternal.setKeyStorePassword(samlPasswordFactory.encryptPassword(keyStorePassword));
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
