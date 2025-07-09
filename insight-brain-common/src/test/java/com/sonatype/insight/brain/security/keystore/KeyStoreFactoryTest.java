/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security.keystore;

import java.security.KeyStore;
import java.security.NoSuchProviderException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_DEFAULT_ENCRYPTION_KEY_STORE_KEY_ENV;
import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_KEY_STORE_PROVIDER_ENV;
import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_KEY_STORE_TYPE_ENV;
import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_MODE_ENABLED_ENV;
import static com.sonatype.insight.brain.security.FipsTestUtil.insertBouncyCastleFipsProvider;
import static com.sonatype.insight.brain.security.FipsTestUtil.removeBouncyCastleFipsProvider;
import static com.sonatype.insight.brain.security.keystore.KeyStoreFactory.PKCS12_KEY_STORE_TYPE;
import static com.sonatype.insight.brain.security.keystore.KeyStoreFactory.createBcFipsKeyStore;
import static com.sonatype.insight.brain.security.keystore.KeyStoreFactory.createKeyStore;
import static com.sonatype.insight.brain.security.keystore.KeyStoreFactory.createPkcs12KeyStore;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(MockitoJUnitRunner.class)
public class KeyStoreFactoryTest
{
  @Rule
  public EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Before
  public void setUp() {
    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "false");
  }

  @After
  public void tearDown() {
    removeBouncyCastleFipsProvider();
  }

  @Test
  public void testCreateKeyStore() throws Exception {
    assertThat(createKeyStore().getType()).isEqualTo(PKCS12_KEY_STORE_TYPE);
  }

  @Test
  public void testCreateKeyStore_WithFipsEnabled() throws Exception {
    insertBouncyCastleFipsProvider();

    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");

    KeyStore keyStore = createKeyStore();
    assertThat(keyStore.getType()).isEqualTo("BCFKS");
    assertThat(keyStore.getProvider().getName()).isEqualTo("BCFIPS");
  }

  @Test
  public void testCreateKeyStore_WithFipsEnabled_Throws_NoSuchProviderException() {
    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");

    assertThatThrownBy(KeyStoreFactory::createKeyStore)
        .isInstanceOf(NoSuchProviderException.class)
        .hasMessage("no such provider: BCFIPS");
  }

  @Test
  public void testCreateKeyStore_WithFipsEnabledAndProvider() throws Exception {
    insertBouncyCastleFipsProvider();

    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");
    environmentVariables.set(FIPS_KEY_STORE_PROVIDER_ENV, "BCFIPS");

    KeyStore keyStore = createKeyStore();
    assertThat(keyStore.getType()).isEqualTo("BCFKS");
    assertThat(keyStore.getProvider().getName()).isEqualTo("BCFIPS");
  }

  @Test
  public void testCreateKeyStore_WithFipsEnabledAndProviderNotSet() throws Exception {
    insertBouncyCastleFipsProvider();

    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");
    environmentVariables.set(FIPS_KEY_STORE_PROVIDER_ENV, null);

    KeyStore keyStore = createKeyStore();
    assertThat(keyStore.getType()).isEqualTo("BCFKS");
    assertThat(keyStore.getProvider().getName()).isEqualTo("BCFIPS");
  }

  @Test
  public void testCreateKeyStore_WithFipsEnabledAndTypeNotSet() throws Exception {
    insertBouncyCastleFipsProvider();

    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");
    environmentVariables.set(FIPS_KEY_STORE_TYPE_ENV, null);

    KeyStore keyStore = createKeyStore();
    assertThat(keyStore.getType()).isEqualTo("BCFKS");
    assertThat(keyStore.getProvider().getName()).isEqualTo("BCFIPS");
  }

  @Test
  public void testCreateKeyStore_WithFipsEnabledAndTypeSet() throws Exception {
    insertBouncyCastleFipsProvider();

    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");
    environmentVariables.set(FIPS_KEY_STORE_TYPE_ENV, "PKCS12");

    KeyStore keyStore = createKeyStore();
    assertThat(keyStore.getType()).isEqualTo("PKCS12");
    assertThat(keyStore.getProvider().getName()).isEqualTo("BCFIPS");
  }

  @Test
  public void testCreatePkcs12KeyStore() throws Exception {
    KeyStore keyStore = createPkcs12KeyStore();
    assertThat(keyStore.getType()).isEqualTo(PKCS12_KEY_STORE_TYPE);
    assertThat(keyStore.getProvider().getName()).isEqualTo("SUN");

    insertBouncyCastleFipsProvider();

    keyStore = createPkcs12KeyStore();
    assertThat(keyStore.getType()).isEqualTo(PKCS12_KEY_STORE_TYPE);
    assertThat(keyStore.getProvider().getName()).isEqualTo("SUN");
  }

  @Test
  public void testCreateBcFipsKeyStore() throws Exception {
    insertBouncyCastleFipsProvider();

    KeyStore keyStore = createBcFipsKeyStore();
    assertThat(keyStore.getType()).isEqualTo("BCFKS");
    assertThat(keyStore.getProvider().getName()).isEqualTo("BCFIPS");
  }

  @Test
  public void testCreateBcFipsKeyStore_Throws_NoSuchProviderException() {
    assertThatThrownBy(KeyStoreFactory::createBcFipsKeyStore)
        .isInstanceOf(NoSuchProviderException.class)
        .hasMessage("no such provider: BCFIPS");
  }

  @Test
  public void testGetDefaultEncryptionKeyStoreKey() {
    assertThat(KeyStoreFactory.getDefaultEncryptionKeyStoreKey()).isEqualTo("CMMDwoV");

    insertBouncyCastleFipsProvider();
    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");

    assertThat(KeyStoreFactory.getDefaultEncryptionKeyStoreKey()).isEqualTo("shesoldseashells");
  }

  @Test
  public void testGetDefaultEncryptionKeyStoreKey_WithFipsEnabledAndProvider() {
    insertBouncyCastleFipsProvider();
    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");
    environmentVariables.set(FIPS_DEFAULT_ENCRYPTION_KEY_STORE_KEY_ENV, "AAAA");

    assertThat(KeyStoreFactory.getDefaultEncryptionKeyStoreKey()).isEqualTo("AAAA");
  }
}
