/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security.keystore;

import java.io.File;
import java.security.KeyStore;
import java.security.NoSuchProviderException;

import java.nio.file.Files;
import java.nio.file.Path;

import com.sonatype.insight.brain.security.TestEnvironmentVariables;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

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

@ExtendWith(MockitoExtension.class)
public class KeyStoreFactoryTest
{
  private final TestEnvironmentVariables environmentVariables = new TestEnvironmentVariables();

  @TempDir
  public Path temporaryFolder;

  @BeforeEach
  public void setUp() {
    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "false");
  }

  @AfterEach
  public void restoreEnvironmentVariables() {
    environmentVariables.restore();
  }

  @AfterEach
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
  public void testGetDefaultEncryptionKeyStoreKey() throws Exception {
    File tempDir = Files.createDirectories(temporaryFolder.resolve("sonatype-work")).toFile();

    // Test non-FIPS mode
    assertThat(KeyStoreFactory.getDefaultEncryptionKeyStoreKey(tempDir)).isEqualTo("CMMDwoV");

    // Test FIPS mode
    insertBouncyCastleFipsProvider();
    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");
    assertThat(KeyStoreFactory.getDefaultEncryptionKeyStoreKey(tempDir)).hasSize(32)
        .matches(k -> k.chars().allMatch(c -> c >= 33 && c <= 126));
  }

  @Test
  public void testGetDefaultEncryptionKeyStoreKey_WithValidDirectory_GeneratesRealFipsKey() throws Exception {
    File tempDir = Files.createDirectories(temporaryFolder.resolve("sonatype-work-fips")).toFile();
    insertBouncyCastleFipsProvider();
    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");

    String fipsKey = KeyStoreFactory.getDefaultEncryptionKeyStoreKey(tempDir);
    assertThat(fipsKey)
        .isNotEqualTo("CMMDwoV")
        .isNotEqualTo("shesoldseashells")
        .hasSize(32)
        .matches(k -> k.chars().allMatch(c -> c >= 33 && c <= 126));
  }

  @Test
  public void testGetDefaultEncryptionKeyStoreKey_FipsModeInvalidDirectory_ThrowsException() {
    File invalidDir = new File("/invalid/nonexistent/path");
    insertBouncyCastleFipsProvider();
    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");

    assertThatThrownBy(() -> KeyStoreFactory.getDefaultEncryptionKeyStoreKey(invalidDir))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Failed to create keystore directory: /invalid/nonexistent/path/data/fips");
  }
}
