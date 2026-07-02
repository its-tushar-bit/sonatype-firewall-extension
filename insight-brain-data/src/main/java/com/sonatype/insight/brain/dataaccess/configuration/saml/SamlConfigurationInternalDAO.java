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

import org.jooq.Record;
import org.jooq.Table;
import org.jooq.UpdatableRecord;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.SamlConfiguration.SAML_CONFIGURATION;

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

  @Override
  protected UpdatableRecord<?> fromEntity(
      final UpdatableRecord<?> record,
      final SamlConfigurationInternal entity)
  {
    record.set(SAML_CONFIGURATION.CONFIGURATION_JSON, entity.getConfigurationJson());
    record.set(SAML_CONFIGURATION.KEYSTORE, entity.getKeystore());
    record.set(SAML_CONFIGURATION.KEYSTORE_PASSWORD_OBFUSCATED, entity.getKeystorePasswordObfuscated());
    return record;
  }

  @Override
  protected SamlConfigurationInternal toEntity(final Record record) {
    if (record == null) {
      return null;
    }
    SamlConfigurationInternal entity = new SamlConfigurationInternal();
    entity.setId(record.get(SAML_CONFIGURATION.SAML_CONFIGURATION_ID));
    entity.setConfigurationJson(record.get(SAML_CONFIGURATION.CONFIGURATION_JSON));
    entity.setKeystore(record.get(SAML_CONFIGURATION.KEYSTORE));
    entity.setKeystorePasswordObfuscated(record.get(SAML_CONFIGURATION.KEYSTORE_PASSWORD_OBFUSCATED));
    return entity;
  }

  public SamlConfigurationInternal get() {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(SAML_CONFIGURATION)
          .limit(1)
          .fetchOne());
    }
  }

  @Override
  public int insert(final TransactionContext tx, final SamlConfigurationInternal entity) {
    generateKeyStore(entity);
    return super.insert(tx, entity);
  }

  @Override
  public void update(final TransactionContext tx, final SamlConfigurationInternal entity) {
    // The keystore info cannot be updated and has to be retrieved from the existing SAML configuration.
    SamlConfigurationInternal currentEntity = get();
    entity.setKeystorePasswordObfuscated(currentEntity.getKeystorePasswordObfuscated());
    entity.setKeystore(currentEntity.getKeystore());
    super.update(tx, entity);
  }

  private void generateKeyStore(final SamlConfigurationInternal samlConfigurationInternal) {
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
        samlConfigurationInternal.setKeystore(byteArrayOutputStream.toByteArray());
      }
    }
    catch (Exception e) {
      throw new IllegalStateException("Could not generate SAML keystore.", e);
    }
  }

  private char[] generatePassword() {
    return UUID.randomUUID().toString().replaceAll("-", "").toCharArray();
  }

  @Override
  public Table<?> getJooqTable() {
    return SAML_CONFIGURATION;
  }

  @Override
  public Class<SamlConfigurationInternal> getEntityClass() {
    return SamlConfigurationInternal.class;
  }
}
