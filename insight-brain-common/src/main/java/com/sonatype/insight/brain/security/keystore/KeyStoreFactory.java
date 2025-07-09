/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security.keystore;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchProviderException;

import com.sonatype.insight.brain.security.FIPSModeDetector;

import static com.sonatype.insight.brain.security.FIPSConfig.getFipsEncryptionKeyStoreKeyOrDefault;
import static com.sonatype.insight.brain.security.FIPSConfig.getFipsKeyStoreProviderOrDefault;
import static com.sonatype.insight.brain.security.FIPSConfig.getFipsKeyStoreTypeOrDefault;

/**
 * Factory class for creating {@link KeyStore} instances.
 */
public class KeyStoreFactory
{
  public static final String PKCS12_KEY_STORE_TYPE = "pkcs12";

  public static final String ENC = "CMMDwoV";

  private KeyStoreFactory() {
    // no-op
  }

  /**
   * Creates a new instance of {@link KeyStore} based on the environment configuration. One such configuration is the
   * checking of FIPS mode. If FIPS mode is enabled, the key store type and provider are set to the FIPS permitted
   * values.
   *
   * @return a new instance of {@link KeyStore}
   * @throws KeyStoreException       - if the key store type is not available
   * @throws NoSuchProviderException - if the key store provider is not available
   */
  public static KeyStore createKeyStore() throws KeyStoreException, NoSuchProviderException {
    if (FIPSModeDetector.isEnabled()) {
      return createBcFipsKeyStore();
    }
    return createPkcs12KeyStore();
  }

  /**
   * Creates a new instance of {@link KeyStore} with the PKCS12 type.
   *
   * @return a new instance of {@link KeyStore}
   * @throws KeyStoreException - if the key store type is not available
   */
  public static KeyStore createPkcs12KeyStore() throws KeyStoreException {
    return KeyStore.getInstance(PKCS12_KEY_STORE_TYPE);
  }

  /**
   * Creates a new instance of {@link KeyStore} with the BCFIPS type.
   *
   * @return a new instance of {@link KeyStore}
   * @throws KeyStoreException       - if the key store type is not available
   * @throws NoSuchProviderException - if the key store provider is not available
   */
  public static KeyStore createBcFipsKeyStore() throws KeyStoreException, NoSuchProviderException {
    return KeyStore.getInstance(getFipsKeyStoreTypeOrDefault(), getFipsKeyStoreProviderOrDefault());
  }

  /**
   * Returns the default encryption key store key based on the FIPS mode.
   *
   * @return the default encryption key store key
   */
  public static String getDefaultEncryptionKeyStoreKey() {
    if (FIPSModeDetector.isEnabled()) {
      return getFipsEncryptionKeyStoreKeyOrDefault();
    }

    return getNonFipsEncryptionKeyStoreKey();
  }

  /**
   * Returns the encryption key store key for the non-FIPS mode.
   *
   * @return the encryption key store key, currently coded as "CMMDwoV"
   */
  public static String getNonFipsEncryptionKeyStoreKey() {
    return ENC;
  }
}
