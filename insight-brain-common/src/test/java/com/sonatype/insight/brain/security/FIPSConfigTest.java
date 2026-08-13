/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_CRYPTO_PROVIDER_ENV;
import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_DEFAULT_KEY_PAIR_GENERATOR_ALGORITHM;
import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_DEFAULT_KEY_PAIR_GENERATOR_KEY_SIZE;
import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_DEFAULT_KEY_PAIR_GENERATOR_PROVIDER;
import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_DEFAULT_KEY_PAIR_SECURE_ALGORITHM;
import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_DEFAULT_KEY_PAIR_SECURE_PROVIDER;
import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_DEFAULT_KEY_STORE_PROVIDER;
import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_DEFAULT_KEY_STORE_TYPE;
import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_ENCRYPTION_ALGORITHM_ENV;
import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_ENCRYPTION_MODE_ENV;
import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_HASH_ALGORITHM;
import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_HASH_ALGORITHM_ENV;
import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_HMAC_ALGORITHM_ENV;
import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_IV_LENGTH_ENV;
import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_KEY_PAIR_GENERATOR_ALGORITHM_ENV;
import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_KEY_PAIR_GENERATOR_KEY_SIZE_ENV;
import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_KEY_PAIR_GENERATOR_PROVIDER_ENV;
import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_KEY_PAIR_SECURE_ALGORITHM_ENV;
import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_KEY_PAIR_SECURE_PROVIDER_ENV;
import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_KEY_STORE_PROVIDER_ENV;
import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_KEY_STORE_TYPE_ENV;
import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_MODE_ENABLED_ENV;
import static com.sonatype.insight.brain.security.FIPSConfig.getFipsCryptoProvider;
import static com.sonatype.insight.brain.security.FIPSConfig.getFipsCryptoProviderOrDefault;
import static com.sonatype.insight.brain.security.FIPSConfig.getFipsEncryptionAlgorithm;
import static com.sonatype.insight.brain.security.FIPSConfig.getFipsEncryptionAlgorithmOrDefault;
import static com.sonatype.insight.brain.security.FIPSConfig.getFipsEncryptionMode;
import static com.sonatype.insight.brain.security.FIPSConfig.getFipsHashAlgorithmOrDefault;
import static com.sonatype.insight.brain.security.FIPSConfig.getFipsHmacAlgorithm;
import static com.sonatype.insight.brain.security.FIPSConfig.getFipsHmacAlgorithmOrDefault;
import static com.sonatype.insight.brain.security.FIPSConfig.getFipsIVLength;
import static com.sonatype.insight.brain.security.FIPSConfig.getFipsIVLengthOrDefault;
import static com.sonatype.insight.brain.security.FIPSConfig.getFipsKeyPairGeneratorAlgorithmOrDefault;
import static com.sonatype.insight.brain.security.FIPSConfig.getFipsKeyPairGeneratorKeySizeOrDefault;
import static com.sonatype.insight.brain.security.FIPSConfig.getFipsKeyPairGeneratorProviderOrDefault;
import static com.sonatype.insight.brain.security.FIPSConfig.getFipsKeyPairSecureAlgorithmOrDefault;
import static com.sonatype.insight.brain.security.FIPSConfig.getFipsKeyPairSecureProviderOrDefault;
import static com.sonatype.insight.brain.security.FIPSConfig.getFipsKeyStoreProviderOrDefault;
import static com.sonatype.insight.brain.security.FIPSConfig.getFipsKeyStoreTypeOrDefault;
import static com.sonatype.insight.brain.security.FIPSConfig.getFipsModeEnabled;
import static org.assertj.core.api.Assertions.assertThat;

public class FIPSConfigTest
{
  private final TestEnvironmentVariables environmentVariables = new TestEnvironmentVariables();

  @AfterEach
  public void restoreEnvironmentVariables() {
    environmentVariables.restore();
  }

  @Test
  public void testGetFipsModeEnabled() {
    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");
    assertThat(getFipsModeEnabled()).isEqualTo("true");

    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "false");
    assertThat(getFipsModeEnabled()).isEqualTo("false");

    environmentVariables.clear(FIPS_MODE_ENABLED_ENV);
    assertThat(getFipsModeEnabled()).isNull();
  }

  @Test
  public void testGetFipsKeyStoreTypeOrDefault() {
    environmentVariables.set(FIPS_KEY_STORE_TYPE_ENV, "AAA");
    assertThat(getFipsKeyStoreTypeOrDefault()).isEqualTo("AAA");

    environmentVariables.clear(FIPS_KEY_STORE_TYPE_ENV);
    assertThat(getFipsKeyStoreTypeOrDefault()).isEqualTo(FIPS_DEFAULT_KEY_STORE_TYPE);
  }

  @Test
  public void testGetFipsKeyStoreTypeOrDefault_WithDefaultValue() {
    environmentVariables.set(FIPS_KEY_STORE_TYPE_ENV, "AAA");
    assertThat(getFipsKeyStoreTypeOrDefault(FIPS_DEFAULT_KEY_STORE_TYPE)).isEqualTo("AAA");

    environmentVariables.clear(FIPS_KEY_STORE_TYPE_ENV);
    assertThat(getFipsKeyStoreTypeOrDefault(FIPS_DEFAULT_KEY_STORE_TYPE)).isEqualTo(FIPS_DEFAULT_KEY_STORE_TYPE);
  }

  @Test
  public void testGetFipsKeyStoreProviderOrDefault() {
    environmentVariables.set(FIPS_KEY_STORE_PROVIDER_ENV, "AAA");
    assertThat(getFipsKeyStoreProviderOrDefault()).isEqualTo("AAA");

    environmentVariables.clear(FIPS_KEY_STORE_PROVIDER_ENV);
    assertThat(getFipsKeyStoreProviderOrDefault()).isEqualTo(FIPS_DEFAULT_KEY_STORE_PROVIDER);
  }

  @Test
  public void testGetFipsKeyStoreProviderOrDefault_WithDefaultValue() {
    environmentVariables.set(FIPS_KEY_STORE_PROVIDER_ENV, "AAA");
    assertThat(getFipsKeyStoreProviderOrDefault(FIPS_DEFAULT_KEY_STORE_PROVIDER)).isEqualTo("AAA");

    environmentVariables.clear(FIPS_KEY_STORE_PROVIDER_ENV);
    assertThat(getFipsKeyStoreProviderOrDefault(FIPS_DEFAULT_KEY_STORE_PROVIDER)).isEqualTo(
        FIPS_DEFAULT_KEY_STORE_PROVIDER);
  }

  @Test
  public void testGetFipsHashAlgorithmOrDefault() {
    environmentVariables.set(FIPS_HASH_ALGORITHM_ENV, "ALGO");
    assertThat(getFipsHashAlgorithmOrDefault()).isEqualTo("ALGO");

    environmentVariables.clear(FIPS_HASH_ALGORITHM_ENV);
    assertThat(getFipsHashAlgorithmOrDefault()).isEqualTo(FIPS_HASH_ALGORITHM);
  }

  @Test
  public void testGetFipsHashAlgorithmOrDefault_WithDefaultValue() {
    environmentVariables.set(FIPS_HASH_ALGORITHM_ENV, "ALGO");
    assertThat(getFipsHashAlgorithmOrDefault("OTHER_ALGO")).isEqualTo("ALGO");

    environmentVariables.clear(FIPS_HASH_ALGORITHM_ENV);
    assertThat(getFipsHashAlgorithmOrDefault("OTHER_ALGO")).isEqualTo("OTHER_ALGO");
  }

  @Test
  public void testGetFipsKeyPairGeneratorAlgorithmOrDefault() {
    environmentVariables.set(FIPS_KEY_PAIR_GENERATOR_ALGORITHM_ENV, "AAA");
    assertThat(getFipsKeyPairGeneratorAlgorithmOrDefault()).isEqualTo("AAA");

    environmentVariables.clear(FIPS_KEY_PAIR_GENERATOR_ALGORITHM_ENV);
    assertThat(getFipsKeyPairGeneratorAlgorithmOrDefault()).isEqualTo(FIPS_DEFAULT_KEY_PAIR_GENERATOR_ALGORITHM);
  }

  @Test
  public void testGetFipsKeyPairGeneratorAlgorithmOrDefault_WithDefaultValue() {
    environmentVariables.set(FIPS_KEY_PAIR_GENERATOR_ALGORITHM_ENV, "AAA");
    assertThat(getFipsKeyPairGeneratorAlgorithmOrDefault(FIPS_DEFAULT_KEY_PAIR_GENERATOR_ALGORITHM)).isEqualTo("AAA");

    environmentVariables.clear(FIPS_KEY_PAIR_GENERATOR_ALGORITHM_ENV);
    assertThat(getFipsKeyPairGeneratorAlgorithmOrDefault(FIPS_DEFAULT_KEY_PAIR_GENERATOR_ALGORITHM))
        .isEqualTo(FIPS_DEFAULT_KEY_PAIR_GENERATOR_ALGORITHM);
  }

  @Test
  public void testGetFipsKeyPairSecureAlgorithmOrDefault() {
    environmentVariables.set(FIPS_KEY_PAIR_SECURE_ALGORITHM_ENV, "AAA");
    assertThat(getFipsKeyPairSecureAlgorithmOrDefault()).isEqualTo("AAA");

    environmentVariables.clear(FIPS_KEY_PAIR_SECURE_ALGORITHM_ENV);
    assertThat(getFipsKeyPairSecureAlgorithmOrDefault()).isEqualTo(FIPS_DEFAULT_KEY_PAIR_SECURE_ALGORITHM);
  }

  @Test
  public void testGetFipsKeyPairSecureAlgorithmOrDefault_WithDefaultValue() {
    environmentVariables.set(FIPS_KEY_PAIR_SECURE_ALGORITHM_ENV, "AAA");
    assertThat(getFipsKeyPairSecureAlgorithmOrDefault(FIPS_DEFAULT_KEY_PAIR_SECURE_ALGORITHM)).isEqualTo("AAA");

    environmentVariables.clear(FIPS_KEY_PAIR_SECURE_ALGORITHM_ENV);
    assertThat(getFipsKeyPairSecureAlgorithmOrDefault(FIPS_DEFAULT_KEY_PAIR_SECURE_ALGORITHM))
        .isEqualTo(FIPS_DEFAULT_KEY_PAIR_SECURE_ALGORITHM);
  }

  @Test
  public void testGetFipsKeyPairSecureProviderOrDefault() {
    environmentVariables.set(FIPS_KEY_PAIR_SECURE_PROVIDER_ENV, "AAA");
    assertThat(getFipsKeyPairSecureProviderOrDefault()).isEqualTo("AAA");

    environmentVariables.clear(FIPS_KEY_PAIR_SECURE_PROVIDER_ENV);
    assertThat(getFipsKeyPairSecureProviderOrDefault()).isEqualTo(FIPS_DEFAULT_KEY_PAIR_SECURE_PROVIDER);
  }

  @Test
  public void testGetFipsKeyPairSecureProviderOrDefault_WithDefaultValue() {
    environmentVariables.set(FIPS_KEY_PAIR_SECURE_PROVIDER_ENV, "AAA");
    assertThat(getFipsKeyPairSecureProviderOrDefault(FIPS_DEFAULT_KEY_PAIR_SECURE_PROVIDER)).isEqualTo("AAA");

    environmentVariables.clear(FIPS_KEY_PAIR_SECURE_PROVIDER_ENV);
    assertThat(getFipsKeyPairSecureProviderOrDefault(FIPS_DEFAULT_KEY_PAIR_SECURE_PROVIDER))
        .isEqualTo(FIPS_DEFAULT_KEY_PAIR_SECURE_PROVIDER);
  }

  @Test
  public void testGetFipsKeyPairGeneratorProviderOrDefault() {
    environmentVariables.set(FIPS_KEY_PAIR_GENERATOR_PROVIDER_ENV, "AAA");
    assertThat(getFipsKeyPairGeneratorProviderOrDefault()).isEqualTo("AAA");

    environmentVariables.clear(FIPS_KEY_PAIR_GENERATOR_PROVIDER_ENV);
    assertThat(getFipsKeyPairGeneratorProviderOrDefault()).isEqualTo(FIPS_DEFAULT_KEY_PAIR_GENERATOR_PROVIDER);
  }

  @Test
  public void testGetFipsKeyPairGeneratorProviderOrDefault_WithDefaultValue() {
    environmentVariables.set(FIPS_KEY_PAIR_GENERATOR_PROVIDER_ENV, "AAA");
    assertThat(getFipsKeyPairGeneratorProviderOrDefault(FIPS_DEFAULT_KEY_PAIR_GENERATOR_PROVIDER)).isEqualTo("AAA");

    environmentVariables.clear(FIPS_KEY_PAIR_GENERATOR_PROVIDER_ENV);
    assertThat(getFipsKeyPairGeneratorProviderOrDefault(FIPS_DEFAULT_KEY_PAIR_GENERATOR_PROVIDER))
        .isEqualTo(FIPS_DEFAULT_KEY_PAIR_GENERATOR_PROVIDER);
  }

  @Test
  public void testGetFipsKeyPairGeneratorKeySizeOrDefault() {
    environmentVariables.set(FIPS_KEY_PAIR_GENERATOR_KEY_SIZE_ENV, "4096");
    assertThat(getFipsKeyPairGeneratorKeySizeOrDefault()).isEqualTo(4096);

    environmentVariables.clear(FIPS_KEY_PAIR_GENERATOR_KEY_SIZE_ENV);
    assertThat(getFipsKeyPairGeneratorKeySizeOrDefault()).isEqualTo(FIPS_DEFAULT_KEY_PAIR_GENERATOR_KEY_SIZE);
  }

  @Test
  public void testGetFipsKeyPairGeneratorKeySizeOrDefault_WithDefaultValue() {
    environmentVariables.set(FIPS_KEY_PAIR_GENERATOR_KEY_SIZE_ENV, "8192");
    assertThat(getFipsKeyPairGeneratorKeySizeOrDefault(FIPS_DEFAULT_KEY_PAIR_GENERATOR_KEY_SIZE)).isEqualTo(8192);

    environmentVariables.clear(FIPS_KEY_PAIR_GENERATOR_KEY_SIZE_ENV);
    assertThat(getFipsKeyPairGeneratorKeySizeOrDefault(FIPS_DEFAULT_KEY_PAIR_GENERATOR_KEY_SIZE))
        .isEqualTo(FIPS_DEFAULT_KEY_PAIR_GENERATOR_KEY_SIZE);
  }

  @Test
  public void testGetFipsEncryptionAlgorithmOrDefault() {
    environmentVariables.set(FIPS_ENCRYPTION_ALGORITHM_ENV, "DES");
    assertThat(getFipsEncryptionAlgorithm()).isEqualTo("DES");

    environmentVariables.clear(FIPS_ENCRYPTION_ALGORITHM_ENV);
    assertThat(getFipsEncryptionAlgorithm()).isEqualTo("AES");
  }

  @Test
  public void testGetFipsEncryptionAlgorithmOrDefault_WithDefaultValue() {
    environmentVariables.set(FIPS_ENCRYPTION_ALGORITHM_ENV, "DES");
    assertThat(getFipsEncryptionAlgorithmOrDefault("AES")).isEqualTo("DES");

    environmentVariables.clear(FIPS_ENCRYPTION_ALGORITHM_ENV);
    assertThat(getFipsEncryptionAlgorithmOrDefault("DES")).isEqualTo("DES");
  }

  @Test
  public void testGetFipsEncryptionModeOrDefault() {
    environmentVariables.set(FIPS_ENCRYPTION_MODE_ENV, "AES/CBC/PKCS5Padding");
    assertThat(getFipsEncryptionMode()).isEqualTo("AES/CBC/PKCS5Padding");

    environmentVariables.clear(FIPS_ENCRYPTION_MODE_ENV);
    assertThat(getFipsEncryptionMode()).isEqualTo("AES/GCM/NoPadding");
  }

  @Test
  public void testGetFipsEncryptionModeOrDefault_WithDefaultValue() {
    environmentVariables.set(FIPS_ENCRYPTION_ALGORITHM_ENV, "AES/CBC/PKCS5Padding");
    assertThat(getFipsEncryptionAlgorithmOrDefault("RSA/ECB/OAEPWithSHA-256AndMGF1Padding"))
        .isEqualTo("AES/CBC/PKCS5Padding");

    environmentVariables.clear(FIPS_ENCRYPTION_MODE_ENV);
    assertThat(getFipsEncryptionAlgorithmOrDefault("AES/CBC/PKCS5Padding"))
        .isEqualTo("AES/CBC/PKCS5Padding");
  }

  @Test
  public void testGetFipsCryptoProviderOrDefault() {
    environmentVariables.set(FIPS_CRYPTO_PROVIDER_ENV, "BC");
    assertThat(getFipsCryptoProvider()).isEqualTo("BC");

    environmentVariables.clear(FIPS_CRYPTO_PROVIDER_ENV);
    assertThat(getFipsCryptoProvider()).isEqualTo("BCFIPS");
  }

  @Test
  public void testGetFipsCryptoProviderOrDefault_WithDefaultValue() {
    environmentVariables.set(FIPS_CRYPTO_PROVIDER_ENV, "BCFIPS");
    assertThat(getFipsCryptoProviderOrDefault("BC")).isEqualTo("BCFIPS");

    environmentVariables.clear(FIPS_CRYPTO_PROVIDER_ENV);
    assertThat(getFipsCryptoProviderOrDefault("BC")).isEqualTo("BC");
  }

  @Test
  public void testGetFipsIVLengthOrDefault() {
    environmentVariables.set(FIPS_IV_LENGTH_ENV, "16");
    assertThat(getFipsIVLength()).isEqualTo(16);

    environmentVariables.clear(FIPS_IV_LENGTH_ENV);
    assertThat(getFipsIVLength()).isEqualTo(12);
  }

  @Test
  public void testGetFipsIVLengthOrDefault_WithDefaultValue() {
    environmentVariables.set(FIPS_IV_LENGTH_ENV, "32");
    assertThat(getFipsIVLengthOrDefault(16)).isEqualTo(32);

    environmentVariables.clear(FIPS_IV_LENGTH_ENV);
    assertThat(getFipsIVLengthOrDefault(32)).isEqualTo(32);
  }

  @Test
  public void testGetFipsHmacAlgorithmOrDefault() {
    environmentVariables.set(FIPS_HMAC_ALGORITHM_ENV, "HmacSHA512");
    assertThat(getFipsHmacAlgorithm()).isEqualTo("HmacSHA512");

    environmentVariables.clear(FIPS_HMAC_ALGORITHM_ENV);
    assertThat(getFipsHmacAlgorithm()).isEqualTo("HmacSHA256");
  }

  @Test
  public void testGetFipsHmacAlgorithmOrDefault_WithDefaultValue() {
    environmentVariables.set(FIPS_HMAC_ALGORITHM_ENV, "HmacSHA512");
    assertThat(getFipsHmacAlgorithmOrDefault("HmacSHA256")).isEqualTo("HmacSHA512");

    environmentVariables.clear(FIPS_HMAC_ALGORITHM_ENV);
    assertThat(getFipsHmacAlgorithmOrDefault("HmacSHA256")).isEqualTo("HmacSHA256");
  }
}
