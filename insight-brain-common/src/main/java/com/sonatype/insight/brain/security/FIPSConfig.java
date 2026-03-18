/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Configuration class for FIPS settings.
 */
public class FIPSConfig
{
  public static final String FIPS_MODE_ENABLED_ENV = "FIPS_MODE_ENABLED";

  public static final String FIPS_KEY_STORE_TYPE_ENV = "FIPS_KEY_STORE_TYPE";

  public static final String FIPS_KEY_STORE_PROVIDER_ENV = "FIPS_KEY_STORE_PROVIDER";

  public static final String FIPS_KEY_PAIR_SECURE_ALGORITHM_ENV = "FIPS_KEY_PAIR_SECURE_ALGORITHM";

  public static final String FIPS_KEY_PAIR_SECURE_PROVIDER_ENV = "FIPS_KEY_PAIR_SECURE_PROVIDER";

  public static final String FIPS_KEY_PAIR_GENERATOR_ALGORITHM_ENV = "FIPS_KEY_PAIR_GENERATOR_ALGORITHM";

  public static final String FIPS_KEY_PAIR_GENERATOR_PROVIDER_ENV = "FIPS_KEY_PAIR_GENERATOR_PROVIDER";

  public static final String FIPS_KEY_PAIR_GENERATOR_KEY_SIZE_ENV = "FIPS_KEY_PAIR_GENERATOR_KEY_SIZE";

  public static final String FIPS_CERTIFICATE_SIGNER_ALGORITHM_ENV = "FIPS_CERTIFICATE_SIGNATURE_ALGORITHM";

  public static final String FIPS_CERTIFICATE_SIGNER_PROVIDER_ENV = "FIPS_CERTIFICATE_SIGNER_PROVIDER";

  public static final String FIPS_CERTIFICATE_SIGNATURE_PROVIDER_ENV = "FIPS_CERTIFICATE_SIGNATURE_PROVIDER";

  public static final String FIPS_CERTIFICATE_SIGNATURE_VALIDITY_YEARS_ENV =
      "FIPS_CERTIFICATE_SIGNATURE_VALIDITY_YEARS";

  public static final String FIPS_CERTIFICATE_CN_X500_NAME_ENV = "FIPS_CERTIFICATE_CN_X500_NAME";

  public static final String FIPS_HASH_ALGORITHM_ENV = "FIPS_HASH_ALGORITHM";

  public static final String HASH_ITERATIONS_ENV = "HASH_ITERATIONS";

  public static final String FIPS_ENCRYPTION_ALGORITHM_ENV = "FIPS_ENCRYPTION_ALGORITHM";

  public static final String FIPS_ENCRYPTION_MODE_ENV = "FIPS_ENCRYPTION_MODE";

  public static final String FIPS_CRYPTO_PROVIDER_ENV = "FIPS_CRYPTO_PROVIDER";

  public static final String FIPS_IV_LENGTH_ENV = "FIPS_IV_LENGTH";

  public static final String FIPS_WEBHOOK_SECRET_PASSPHRASE_ENV = "FIPS_WEBHOOK_SECRET_PASSPHRASE_ENV";

  public static final String FIPS_HMAC_ALGORITHM_ENV = "FIPS_HMAC_ALGORITHM";

  public static final String FIPS_KEYSTORE_PBKDF2_ALGORITHM_ENV = "FIPS_KEYSTORE_PBKDF2_ALGORITHM";

  public static final String FIPS_KEYSTORE_PBKDF2_ITERATIONS_ENV = "FIPS_KEYSTORE_PBKDF2_ITERATIONS";

  public static final String FIPS_KEYSTORE_KEY_LENGTH_BITS_ENV = "FIPS_KEYSTORE_KEY_LENGTH_BITS";

  public static final String FIPS_KEYSTORE_SALT_ENV = "FIPS_KEYSTORE_SALT";

  public static final String FIPS_DEFAULT_KEY_STORE_TYPE = "BCFKS";

  public static final String FIPS_DEFAULT_KEY_STORE_PROVIDER = "BCFIPS";

  public static final String FIPS_HASH_ALGORITHM = "SHA-256";

  public static final int HASH_ITERATIONS = 400000;

  public static final String FIPS_DEFAULT_KEY_PAIR_SECURE_ALGORITHM = "DEFAULT";

  public static final String FIPS_DEFAULT_KEY_PAIR_SECURE_PROVIDER = "BCFIPS";

  public static final String FIPS_DEFAULT_KEY_PAIR_GENERATOR_ALGORITHM = "RSA";

  public static final String FIPS_DEFAULT_KEY_PAIR_GENERATOR_PROVIDER = "BCFIPS";

  public static final int FIPS_DEFAULT_KEY_PAIR_GENERATOR_KEY_SIZE = 2048;

  // to note in fips mode the signer algorithm is capitalized to SHA256WITHRSA and not SHA256withRSA
  public static final String FIPS_DEFAULT_CERTIFICATE_SIGNER_ALGORITHM = "SHA256WITHRSA";

  public static final String FIPS_DEFAULT_CERTIFICATE_SIGNER_PROVIDER = "BCFIPS";

  public static final String FIPS_DEFAULT_CERTIFICATE_SIGNATURE_PROVIDER = "BCFIPS";

  public static final int FIPS_DEFAULT_CERTIFICATE_SIGNATURE_VALIDITY_YEARS = 10;

  public static final String FIPS_DEFAULT_CERTIFICATE_CN_X500_NAME = "CN=SAML KeyStore";

  public static final String FIPS_DEFAULT_ENCRYPTION_ALGORITHM = "AES";

  public static final String FIPS_DEFAULT_ENCRYPTION_MODE = "AES/GCM/NoPadding";

  public static final String FIPS_DEFAULT_CRYPTO_PROVIDER = "BCFIPS";

  public static final int FIPS_DEFAULT_IV_LENGTH = 12;

  public static final String FIPS_DEFAULT_HMAC_ALGORITHM = "HmacSHA256";

  public static final String FIPS_DEFAULT_KEYSTORE_PASSWORD = "fips-keystore-internal";

  public static final String FIPS_DEFAULT_WEBHOOK_SECRET_PASSPHRASE = "^d1swM!FF&qQ$%0/";

  public static final String FIPS_DEFAULT_KEYSTORE_PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";

  public static final int FIPS_DEFAULT_KEYSTORE_PBKDF2_ITERATIONS = 100000;

  public static final int FIPS_DEFAULT_KEYSTORE_KEY_LENGTH_BITS = 256;

  public static final String FIPS_DEFAULT_KEYSTORE_SALT =
      "7f2a7b1c8e5d3f3a4b9c2e7f1a8d5c3b6f4a2a9c7b1f8d3e6a2c5f9b4d7a1d8c6";

  private FIPSConfig() {
    // prevent instantiation
  }

  public static String getFipsModeEnabled() {
    return System.getenv(FIPS_MODE_ENABLED_ENV);
  }

  /**
   * Check if FIPS mode variable is set in the environment. Visible for testing.
   *
   * @return true if FIPS_MODE_ENABLED environment variable is set to true or false, false otherwise.
   */
  public static boolean isFipsModeEnabledVariableSet() {
    String value = FIPSConfig.getFipsModeEnabled();
    return TRUE.toString().equalsIgnoreCase(value) || FALSE.toString().equalsIgnoreCase(value);
  }

  /**
   * Check if FIPS mode is forced by environment variable.
   *
   * @return true if FIPS mode is forced by environment variable, false otherwise.
   */
  public static boolean isFipsEnabledByEnvironment() {
    return TRUE.toString().equalsIgnoreCase(FIPSConfig.getFipsModeEnabled());
  }

  /**
   * Get the FIPS hash algorithm from the environment variable {@link #FIPS_HASH_ALGORITHM_ENV} or return the default
   * value.
   *
   * @return the FIPS hash algorithm or "SHA-256" if not set.
   */
  public static String getFipsHashAlgorithmOrDefault() {
    return getFipsHashAlgorithmOrDefault(FIPS_HASH_ALGORITHM);
  }

  /**
   * Get the FIPS hash algorithm from the environment variable {@link #FIPS_HASH_ALGORITHM_ENV} or return the
   * provided default value.
   *
   * @return the FIPS hash algorithm or the provided default if not set.
   */
  public static String getFipsHashAlgorithmOrDefault(final String defaultValue) {
    String fipsHashAlgorithm = System.getenv(FIPS_HASH_ALGORITHM_ENV);
    return isNotBlank(fipsHashAlgorithm) ? fipsHashAlgorithm : defaultValue;
  }

  /**
   * Get the number of FIPS hash iterations from the environment variable {@link #HASH_ITERATIONS_ENV} or return the
   * default value.
   *
   * @return the number of hash iterations or 400,000 if not set.
   */
  public static int getNumHashIterationsOrDefault() {
    return getNumHashIterationsOrDefault(HASH_ITERATIONS);
  }

  /**
   * Get the number of FIPS hash iterations from the environment variable {@link #HASH_ITERATIONS_ENV} or return the
   * provided default value.
   *
   * @return the number of hash iterations or the provided value if not set.
   */
  public static int getNumHashIterationsOrDefault(final int defaultValue) {
    String numHashIterations = System.getenv(HASH_ITERATIONS_ENV);
    return isNotBlank(numHashIterations) ? Integer.parseInt(numHashIterations) : defaultValue;
  }

  /**
   * Get the FIPS key store type from the environment variable {@link #FIPS_KEY_STORE_TYPE_ENV} or return the default
   * value.
   *
   * @return the FIPS key store type or "BCFKS" if not set.
   */
  public static String getFipsKeyStoreTypeOrDefault() {
    return getFipsKeyStoreTypeOrDefault(FIPS_DEFAULT_KEY_STORE_TYPE);
  }

  /**
   * Get the FIPS key store type from the environment variable {@link #FIPS_KEY_STORE_TYPE_ENV} or return the default
   * value.
   *
   * @param defaultValue - the default value to return if the environment variable is not set
   * @return the FIPS key store type or the default value if not set.
   */
  public static String getFipsKeyStoreTypeOrDefault(final String defaultValue) {
    String fipsKeyStoreType = System.getenv(FIPS_KEY_STORE_TYPE_ENV);
    return isNotBlank(fipsKeyStoreType) ? fipsKeyStoreType : defaultValue;
  }

  /**
   * Get the FIPS key store provider from the environment variable {@link #FIPS_KEY_STORE_PROVIDER_ENV} or return the
   * default value.
   *
   * @return the FIPS key store provider or "BCFIPS" if not set.
   */
  public static String getFipsKeyStoreProviderOrDefault() {
    return getFipsKeyStoreProviderOrDefault(FIPS_DEFAULT_KEY_STORE_PROVIDER);
  }

  /**
   * Get the FIPS key store provider from the environment variable FIPS_KEY_STORE_PROVIDER or return the default value.
   *
   * @return the FIPS key store provider or "BCFIPS" if not set.
   */
  public static String getFipsKeyStoreProviderOrDefault(final String defaultValue) {
    String fipsKeyStoreProvider = System.getenv(FIPS_KEY_STORE_PROVIDER_ENV);
    return isNotBlank(fipsKeyStoreProvider) ? fipsKeyStoreProvider : defaultValue;
  }

  /**
   * Get the FIPS key pair secure algorithm from the environment variable
   * {@link #FIPS_DEFAULT_KEY_PAIR_SECURE_ALGORITHM} or return the default value.
   *
   * @return the FIPS key pair secure algorithm or "DEFAULT" if not set.
   */
  public static String getFipsKeyPairSecureAlgorithmOrDefault() {
    return getFipsKeyPairSecureAlgorithmOrDefault(FIPS_DEFAULT_KEY_PAIR_SECURE_ALGORITHM);
  }

  /**
   * Get the FIPS key pair secure algorithm from the environment variable
   * {@link #FIPS_DEFAULT_KEY_PAIR_SECURE_ALGORITHM} or return the default value.
   *
   * @param defaultValue - the default value to return if the environment variable is not set
   * @return the FIPS key pair secure algorithm or the default value if not set.
   */
  public static String getFipsKeyPairSecureAlgorithmOrDefault(final String defaultValue) {
    String fipsKeyPairSecureAlgorithm = System.getenv(FIPS_KEY_PAIR_SECURE_ALGORITHM_ENV);
    return isNotBlank(fipsKeyPairSecureAlgorithm) ? fipsKeyPairSecureAlgorithm : defaultValue;
  }

  /**
   * Get the FIPS key pair secure provider from the environment variable {@link #FIPS_DEFAULT_KEY_PAIR_SECURE_PROVIDER}
   * or return the default value.
   *
   * @return the FIPS key pair secure provider or "BCFIPS" if not set.
   */
  public static String getFipsKeyPairSecureProviderOrDefault() {
    return getFipsKeyPairSecureProviderOrDefault(FIPS_DEFAULT_KEY_PAIR_SECURE_PROVIDER);
  }

  /**
   * Get the FIPS key pair secure provider from the environment variable {@link #FIPS_DEFAULT_KEY_PAIR_SECURE_PROVIDER}
   * or return the default value.
   *
   * @param defaultValue - the default value to return if the environment variable is not set
   * @return the FIPS key pair secure provider or the default value if not set.
   */
  public static String getFipsKeyPairSecureProviderOrDefault(final String defaultValue) {
    String fipsKeyPairSecureProvider = System.getenv(FIPS_KEY_PAIR_SECURE_PROVIDER_ENV);
    return isNotBlank(fipsKeyPairSecureProvider) ? fipsKeyPairSecureProvider : defaultValue;
  }

  /**
   * Get the FIPS key pair generator algorithm from the environment variable
   * {@link #FIPS_DEFAULT_KEY_PAIR_GENERATOR_ALGORITHM} or return the default value.
   *
   * @return the FIPS key pair generator algorithm or "RSA" if not set.
   */
  public static String getFipsKeyPairGeneratorAlgorithmOrDefault() {
    return getFipsKeyPairGeneratorAlgorithmOrDefault(FIPS_DEFAULT_KEY_PAIR_GENERATOR_ALGORITHM);
  }

  /**
   * Get the FIPS key pair generator algorithm from the environment variable
   * {@link #FIPS_DEFAULT_KEY_PAIR_GENERATOR_ALGORITHM} or return the default value.
   *
   * @param defaultValue - the default value to return if the environment variable is not set
   * @return the FIPS key pair generator algorithm or the default value if not set.
   */
  public static String getFipsKeyPairGeneratorAlgorithmOrDefault(final String defaultValue) {
    String fipsKeyPairGeneratorAlgorithm = System.getenv(FIPS_KEY_PAIR_GENERATOR_ALGORITHM_ENV);
    return isNotBlank(fipsKeyPairGeneratorAlgorithm) ? fipsKeyPairGeneratorAlgorithm : defaultValue;
  }

  /**
   * Get the FIPS key pair generator provider from the environment variable
   * {@link #FIPS_DEFAULT_KEY_PAIR_GENERATOR_PROVIDER} or return the default value.
   *
   * @return the FIPS key pair generator provider or "BCFIPS" if not set.
   */
  public static String getFipsKeyPairGeneratorProviderOrDefault() {
    return getFipsKeyPairGeneratorProviderOrDefault(FIPS_DEFAULT_KEY_PAIR_GENERATOR_PROVIDER);
  }

  /**
   * Get the FIPS key pair generator provider from the environment variable
   * {@link #FIPS_DEFAULT_KEY_PAIR_GENERATOR_PROVIDER} or return the default value.
   *
   * @param defaultValue - the default value to return if the environment variable is not set
   * @return the FIPS key pair generator provider or the default value if not set.
   */
  public static String getFipsKeyPairGeneratorProviderOrDefault(final String defaultValue) {
    String fipsKeyPairGeneratorProvider = System.getenv(FIPS_KEY_PAIR_GENERATOR_PROVIDER_ENV);
    return isNotBlank(fipsKeyPairGeneratorProvider) ? fipsKeyPairGeneratorProvider : defaultValue;
  }

  /**
   * Get the FIPS key pair generator key size from the environment variable
   * {@link #FIPS_DEFAULT_KEY_PAIR_GENERATOR_KEY_SIZE} or return the default value.
   *
   * @return the FIPS key pair generator key size or 2048 if not set.
   */
  public static int getFipsKeyPairGeneratorKeySizeOrDefault() {
    return getFipsKeyPairGeneratorKeySizeOrDefault(FIPS_DEFAULT_KEY_PAIR_GENERATOR_KEY_SIZE);
  }

  /**
   * Get the FIPS key pair generator key size from the environment variable
   * {@link #FIPS_DEFAULT_KEY_PAIR_GENERATOR_KEY_SIZE} or return the default value.
   *
   * @param defaultValue - the default value to return if the environment variable is not set
   * @return the FIPS key pair generator key size or the default value if not set.
   */
  public static int getFipsKeyPairGeneratorKeySizeOrDefault(final int defaultValue) {
    String fipsKeyPairGeneratorKeySize = System.getenv(FIPS_KEY_PAIR_GENERATOR_KEY_SIZE_ENV);
    return isNotBlank(fipsKeyPairGeneratorKeySize) ? Integer.parseInt(fipsKeyPairGeneratorKeySize) : defaultValue;
  }

  /**
   * Get the FIPS certificate signer algorithm from the environment variable
   * {@link #FIPS_DEFAULT_CERTIFICATE_SIGNER_ALGORITHM} or return the default value.
   *
   * @return the FIPS certificate signer algorithm or "SHA256withRSA" if not set.
   */
  public static String getFipsCertificateSignerAlgorithmOrDefault() {
    return getFipsCertificateSignerAlgorithmOrDefault(FIPS_DEFAULT_CERTIFICATE_SIGNER_ALGORITHM);
  }

  /**
   * Get the FIPS certificate signer algorithm from the environment variable
   * {@link #FIPS_DEFAULT_CERTIFICATE_SIGNER_ALGORITHM} or return the default value.
   *
   * @param defaultValue - the default value to return if the environment variable is not set
   * @return the FIPS certificate signer algorithm or the default value if not set.
   */
  public static String getFipsCertificateSignerAlgorithmOrDefault(final String defaultValue) {
    String fipsCertificateSignerAlgorithm = System.getenv(FIPS_CERTIFICATE_SIGNER_ALGORITHM_ENV);
    return isNotBlank(fipsCertificateSignerAlgorithm) ? fipsCertificateSignerAlgorithm : defaultValue;
  }

  /**
   * Get the FIPS certificate signer provider from the environment variable
   * {@link #FIPS_DEFAULT_CERTIFICATE_SIGNER_PROVIDER} or return the default value.
   *
   * @return the FIPS certificate signer provider or "BCFIPS" if not set.
   */
  public static String getFipsCertificateSignerProviderOrDefault() {
    return getFipsCertificateSignerProviderOrDefault(FIPS_DEFAULT_CERTIFICATE_SIGNER_PROVIDER);
  }

  /**
   * Get the FIPS certificate signer provider from the environment variable
   * {@link #FIPS_DEFAULT_CERTIFICATE_SIGNER_PROVIDER} or return the default value.
   *
   * @param defaultValue - the default value to return if the environment variable is not set
   * @return the FIPS certificate signer provider or the default value if not set.
   */
  public static String getFipsCertificateSignerProviderOrDefault(final String defaultValue) {
    String fipsCertificateSignerProvider = System.getenv(FIPS_CERTIFICATE_SIGNER_PROVIDER_ENV);
    return isNotBlank(fipsCertificateSignerProvider) ? fipsCertificateSignerProvider : defaultValue;
  }

  /**
   * Get the FIPS certificate signature provider from the environment variable
   * {@link #FIPS_DEFAULT_CERTIFICATE_SIGNATURE_PROVIDER} or return the default value.
   *
   * @return the FIPS certificate signature provider or "BCFIPS" if not set.
   */
  public static String getFipsCertificateSignatureProviderOrDefault() {
    return getFipsCertificateSignatureProviderOrDefault(FIPS_DEFAULT_CERTIFICATE_SIGNATURE_PROVIDER);
  }

  /**
   * Get the FIPS certificate signature provider from the environment variable
   * {@link #FIPS_DEFAULT_CERTIFICATE_SIGNATURE_PROVIDER} or return the default value.
   *
   * @param defaultValue - the default value to return if the environment variable is not set
   * @return the FIPS certificate signature provider or the default value if not set.
   */
  public static String getFipsCertificateSignatureProviderOrDefault(final String defaultValue) {
    String fipsCertificateSignatureProvider = System.getenv(FIPS_CERTIFICATE_SIGNATURE_PROVIDER_ENV);
    return isNotBlank(fipsCertificateSignatureProvider) ? fipsCertificateSignatureProvider : defaultValue;
  }

  /**
   * Get the FIPS certificate signature validity years from the environment variable
   * {@link #FIPS_DEFAULT_CERTIFICATE_SIGNATURE_VALIDITY_YEARS} or return the default value.
   *
   * @return the FIPS certificate signature validity years or 10 if not set.
   */
  public static int getFipsCertificateSignatureValidityYearsOrDefault() {
    return getFipsCertificateSignatureValidityYearsOrDefault(FIPS_DEFAULT_CERTIFICATE_SIGNATURE_VALIDITY_YEARS);
  }

  /**
   * Get the FIPS certificate signature validity years from the environment variable
   * {@link #FIPS_DEFAULT_CERTIFICATE_SIGNATURE_VALIDITY_YEARS} or return the default value.
   *
   * @param defaultValue - the default value to return if the environment variable is not set
   * @return the FIPS certificate signature validity years or the default value if not set.
   */
  public static int getFipsCertificateSignatureValidityYearsOrDefault(final int defaultValue) {
    String fipsCertificateSignatureValidityYears = System.getenv(FIPS_CERTIFICATE_SIGNATURE_VALIDITY_YEARS_ENV);
    return isNotBlank(fipsCertificateSignatureValidityYears)
        ? Integer.parseInt(
            fipsCertificateSignatureValidityYears)
        : defaultValue;
  }

  /**
   * Get the FIPS certificate CN X500 name from the environment variable {@link #FIPS_DEFAULT_CERTIFICATE_CN_X500_NAME}
   * or return the default value.
   *
   * @return the FIPS certificate CN X500 name or "CN=SAML KeyStore" if not set.
   */
  public static String getFipsCertificateCNX500NameOrDefault() {
    return getFipsCertificateCNX500NameOrDefault(FIPS_DEFAULT_CERTIFICATE_CN_X500_NAME);
  }

  /**
   * Get the FIPS certificate CN X500 name from the environment variable {@link #FIPS_DEFAULT_CERTIFICATE_CN_X500_NAME}
   * or return the default value.
   *
   * @param defaultValue - the default value to return if the environment variable is not set
   * @return the FIPS certificate CN X500 name or the default value if not set.
   */
  public static String getFipsCertificateCNX500NameOrDefault(final String defaultValue) {
    String fipsCertificateCNX500Name = System.getenv(FIPS_CERTIFICATE_CN_X500_NAME_ENV);
    return isNotBlank(fipsCertificateCNX500Name) ? fipsCertificateCNX500Name : defaultValue;
  }

  /**
   * Get the FIPS encryption algorithm from the environment variable {@link #FIPS_DEFAULT_ENCRYPTION_ALGORITHM} or
   * return the default value.
   *
   * @return the FIPS encryption algorithm or "AES" if not set.
   */
  public static String getFipsEncryptionAlgorithm() {
    return getFipsEncryptionAlgorithmOrDefault(FIPS_DEFAULT_ENCRYPTION_ALGORITHM);
  }

  /**
   * Get the FIPS encryption algorithm from the environment variable {@link #FIPS_DEFAULT_ENCRYPTION_ALGORITHM} or
   * return the provided default value.
   *
   * @return the FIPS encryption algorithm or the provided default if not set.
   */
  public static String getFipsEncryptionAlgorithmOrDefault(final String defaultValue) {
    String fipsEncryptionAlgorithm = System.getenv(FIPS_ENCRYPTION_ALGORITHM_ENV);
    return isNotBlank(fipsEncryptionAlgorithm) ? fipsEncryptionAlgorithm : defaultValue;
  }

  /**
   * Get the FIPS encryption mode from the environment variable {@link #FIPS_DEFAULT_ENCRYPTION_MODE} or
   * return the default value.
   *
   * @return the FIPS encryption algorithm or "AE/GCM/NoPadding" if not set.
   */
  public static String getFipsEncryptionMode() {
    return getFipsEncryptionModeOrDefault(FIPS_DEFAULT_ENCRYPTION_MODE);
  }

  /**
   * Get the FIPS encryption mode from the environment variable {@link #FIPS_DEFAULT_ENCRYPTION_MODE} or
   * return the provided default value.
   *
   * @return the FIPS encryption mode or the provided default if not set.
   */
  public static String getFipsEncryptionModeOrDefault(final String defaultValue) {
    String fipsEncryptionMode = System.getenv(FIPS_ENCRYPTION_MODE_ENV);
    return isNotBlank(fipsEncryptionMode) ? fipsEncryptionMode : defaultValue;
  }

  /**
   * Get the FIPS crypto provider from the environment variable {@link #FIPS_DEFAULT_CRYPTO_PROVIDER} or
   * return the default value.
   *
   * @return the FIPS crypto provider or "BCFIPS" if not set.
   */
  public static String getFipsCryptoProvider() {
    return getFipsCryptoProviderOrDefault(FIPS_DEFAULT_CRYPTO_PROVIDER);
  }

  /**
   * Get the FIPS crypto provider from the environment variable {@link #FIPS_DEFAULT_CRYPTO_PROVIDER} or
   * return the provided default value.
   *
   * @return the FIPS crypto provider or the provided default if not set.
   */
  public static String getFipsCryptoProviderOrDefault(final String defaultValue) {
    String fipsCryptoProvider = System.getenv(FIPS_CRYPTO_PROVIDER_ENV);
    return isNotBlank(fipsCryptoProvider) ? fipsCryptoProvider : defaultValue;
  }

  /**
   * Get the FIPS IV length from the environment variable {@link #FIPS_DEFAULT_IV_LENGTH} or
   * return the default value.
   *
   * @return the FIPS IV length or "12" if not set.
   */
  public static int getFipsIVLength() {
    return getFipsIVLengthOrDefault(FIPS_DEFAULT_IV_LENGTH);
  }

  /**
   * Get the FIPS IV length from the environment variable {@link #FIPS_DEFAULT_IV_LENGTH} or
   * return the provided default value.
   *
   * @return the FIPS IV length or the provided default if not set.
   */
  public static int getFipsIVLengthOrDefault(final int defaultValue) {
    if (defaultValue <= 0) {
      throw new IllegalArgumentException("Provided IV length must be greater than 0");
    }

    String fipsIVLength = System.getenv(FIPS_IV_LENGTH_ENV);
    if (isNotBlank(fipsIVLength)) {
      int ivLength = Integer.parseInt(fipsIVLength);
      if (ivLength > 0) {
        return ivLength;
      }
    }
    return defaultValue;
  }

  /**
   * Get the FIPS webhook secret passphrase from the environment variable
   * {@link #FIPS_WEBHOOK_SECRET_PASSPHRASE_ENV} or return the default value.
   *
   * @return the FIPS webhook secret passphrase or default value if not set.
   */
  public static String getFipsWebhookSecretPassphraseOrDefault() {
    String webhookSecretPassphrase = System.getenv(FIPS_WEBHOOK_SECRET_PASSPHRASE_ENV);
    return isNotBlank(webhookSecretPassphrase) ? webhookSecretPassphrase : FIPS_DEFAULT_WEBHOOK_SECRET_PASSPHRASE;
  }

  /**
   * Get the FIPS HMAC Algorithm from the environment variable {@link #FIPS_DEFAULT_HMAC_ALGORITHM} or
   * return the default value.
   *
   * @return the FIPS HMAC Algorithm or "HmacSHA1" if not set.
   */
  public static String getFipsHmacAlgorithm() {
    return getFipsHmacAlgorithmOrDefault(FIPS_DEFAULT_HMAC_ALGORITHM);
  }

  /**
   * Get the FIPS HMAC Algorithm from the environment variable {@link #FIPS_DEFAULT_HMAC_ALGORITHM} or
   * return the provided default value.
   *
   * @return the FIPS HMAC Algorithm or the provided default if not set.
   */
  public static String getFipsHmacAlgorithmOrDefault(final String defaultValue) {
    String fipsHmacAlgo = System.getenv(FIPS_HMAC_ALGORITHM_ENV);
    return isNotBlank(fipsHmacAlgo) ? fipsHmacAlgo : defaultValue;
  }

  /**
   * Get the FIPS keystore PBKDF2 algorithm from the environment variable
   * {@link #FIPS_KEYSTORE_PBKDF2_ALGORITHM_ENV} or return the default value.
   *
   * @return the FIPS keystore PBKDF2 algorithm or "PBKDF2WithHmacSHA256" if not set.
   */
  public static String getFipsKeystorePbkdf2AlgorithmOrDefault() {
    return getFipsKeystorePbkdf2AlgorithmOrDefault(FIPS_DEFAULT_KEYSTORE_PBKDF2_ALGORITHM);
  }

  /**
   * Get the FIPS keystore PBKDF2 algorithm from the environment variable
   * {@link #FIPS_KEYSTORE_PBKDF2_ALGORITHM_ENV} or return the provided default value.
   *
   * @param defaultValue - the default value to return if the environment variable is not set
   * @return the FIPS keystore PBKDF2 algorithm or the provided default if not set.
   */
  public static String getFipsKeystorePbkdf2AlgorithmOrDefault(final String defaultValue) {
    String keystorePbkdf2Algorithm = System.getenv(FIPS_KEYSTORE_PBKDF2_ALGORITHM_ENV);
    return isNotBlank(keystorePbkdf2Algorithm) ? keystorePbkdf2Algorithm : defaultValue;
  }

  /**
   * Get the FIPS keystore PBKDF2 iterations from the environment variable
   * {@link #FIPS_KEYSTORE_PBKDF2_ITERATIONS_ENV} or return the default value.
   *
   * @return the FIPS keystore PBKDF2 iterations or 100000 if not set.
   */
  public static int getFipsKeystorePbkdf2IterationsOrDefault() {
    return getFipsKeystorePbkdf2IterationsOrDefault(FIPS_DEFAULT_KEYSTORE_PBKDF2_ITERATIONS);
  }

  /**
   * Get the FIPS keystore PBKDF2 iterations from the environment variable
   * {@link #FIPS_KEYSTORE_PBKDF2_ITERATIONS_ENV} or return the provided default value.
   *
   * @param defaultValue - the default value to return if the environment variable is not set
   * @return the FIPS keystore PBKDF2 iterations or the provided default if not set.
   */
  public static int getFipsKeystorePbkdf2IterationsOrDefault(final int defaultValue) {
    String keystorePbkdf2Iterations = System.getenv(FIPS_KEYSTORE_PBKDF2_ITERATIONS_ENV);
    return isNotBlank(keystorePbkdf2Iterations) ? Integer.parseInt(keystorePbkdf2Iterations) : defaultValue;
  }

  /**
   * Get the FIPS keystore key length bits from the environment variable
   * {@link #FIPS_KEYSTORE_KEY_LENGTH_BITS_ENV} or return the default value.
   *
   * @return the FIPS keystore key length bits or 256 if not set.
   */
  public static int getFipsKeystoreKeyLengthBitsOrDefault() {
    return getFipsKeystoreKeyLengthBitsOrDefault(FIPS_DEFAULT_KEYSTORE_KEY_LENGTH_BITS);
  }

  /**
   * Get the FIPS keystore key length bits from the environment variable
   * {@link #FIPS_KEYSTORE_KEY_LENGTH_BITS_ENV} or return the provided default value.
   *
   * @param defaultValue - the default value to return if the environment variable is not set
   * @return the FIPS keystore key length bits or the provided default if not set.
   */
  public static int getFipsKeystoreKeyLengthBitsOrDefault(final int defaultValue) {
    String keystoreKeyLengthBits = System.getenv(FIPS_KEYSTORE_KEY_LENGTH_BITS_ENV);
    return isNotBlank(keystoreKeyLengthBits) ? Integer.parseInt(keystoreKeyLengthBits) : defaultValue;
  }

  /**
   * Get the FIPS keystore salt from the environment variable
   * {@link #FIPS_KEYSTORE_SALT_ENV} or return the default value.
   *
   * @return the FIPS keystore salt or default hex string if not set.
   */
  public static String getFipsKeystoreSaltOrDefault() {
    return getFipsKeystoreSaltOrDefault(FIPS_DEFAULT_KEYSTORE_SALT);
  }

  /**
   * Get the FIPS keystore salt from the environment variable
   * {@link #FIPS_KEYSTORE_SALT_ENV} or return the provided default value.
   *
   * @param defaultValue - the default value to return if the environment variable is not set
   * @return the FIPS keystore salt or the provided default if not set.
   */
  public static String getFipsKeystoreSaltOrDefault(final String defaultValue) {
    String keystoreSalt = System.getenv(FIPS_KEYSTORE_SALT_ENV);
    return isNotBlank(keystoreSalt) ? keystoreSalt : defaultValue;
  }
}
