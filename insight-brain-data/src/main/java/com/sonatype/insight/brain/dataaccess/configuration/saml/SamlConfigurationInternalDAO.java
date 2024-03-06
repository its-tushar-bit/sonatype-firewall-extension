/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.saml;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v1CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 * @since 1.72
 */
@Named
@Singleton
@SuppressWarnings("restriction")
public class SamlConfigurationInternalDAO
    extends AbstractOperationalSqlDAO<SamlConfigurationInternal>
{
  @Inject
  public SamlConfigurationInternalDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

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

      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(2048, new SecureRandom());

      KeyPair keyPair = keyPairGenerator.generateKeyPair();
      X500Name issuerAndSubject = new X500Name("CN=SAML KeyStore");
      SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
      X509v1CertificateBuilder builder = new X509v1CertificateBuilder(
          issuerAndSubject,
          BigInteger.valueOf(System.currentTimeMillis()),
          new Date(),
          dateTenYearsFromNow(),
          issuerAndSubject,
          subjectPublicKeyInfo);

      ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());
      X509CertificateHolder holder = builder.build(contentSigner);
      X509Certificate[] cert = {new JcaX509CertificateConverter().getCertificate(holder)};

      // Save the key and certificate in the keystore.
      keyStore.setKeyEntry(SamlConfigurationInternal.KEYSTORE_ALIAS, keyPair.getPrivate(), keyStorePassword, cert);

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

  private static Date dateTenYearsFromNow() {
    Calendar c = Calendar.getInstance();
    c.add(Calendar.YEAR, 10);
    return c.getTime();
  }

  private char[] generatePassword() {
    return UUID.randomUUID().toString().replaceAll("-", "").toCharArray();
  }
}
