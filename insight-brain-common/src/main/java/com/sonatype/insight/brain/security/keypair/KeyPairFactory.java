/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security.keypair;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import com.sonatype.insight.brain.security.FIPSModeDetector;

import static com.sonatype.insight.brain.security.FIPSConfig.getFipsKeyPairGeneratorAlgorithmOrDefault;
import static com.sonatype.insight.brain.security.FIPSConfig.getFipsKeyPairGeneratorKeySizeOrDefault;
import static com.sonatype.insight.brain.security.FIPSConfig.getFipsKeyPairGeneratorProviderOrDefault;
import static com.sonatype.insight.brain.security.FIPSConfig.getFipsKeyPairSecureAlgorithmOrDefault;
import static com.sonatype.insight.brain.security.FIPSConfig.getFipsKeyPairSecureProviderOrDefault;

/**
 * Factory class for creating {@link KeyPair} instances.
 */
public class KeyPairFactory
{
  public static final String RSA_ALGORITHM = "RSA";

  public static final int KEY_SIZE = 2048;

  private KeyPairFactory() {
    // prevent instantiation
  }

  /**
   * Creates a {@link KeyPair} instance, using the provided public and private keys, based on the environment
   * configuration. One such configuration is the checking of FIPS mode. If FIPS mode is enabled, the key pair is
   * created using the FIPS permitted values.
   *
   * @param publicKey - the public key
   * @param privateKey - the private key
   * @return a new {@link KeyPair} instance
   * @throws NoSuchAlgorithmException - if the algorithm is not found
   * @throws InvalidKeySpecException - if the key specification is invalid
   * @throws NoSuchProviderException - if the provider is not found
   */
  public static KeyPair createKeyPair(
      final PublicKey publicKey,
      final PrivateKey privateKey) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException
  {
    if (FIPSModeDetector.isEnabled()) {
      return createFipsKeyPair(publicKey, privateKey);
    }
    return new KeyPair(publicKey, privateKey);
  }

  /**
   * Creates a {@link KeyPair} instance, using the provided public and private keys, based on the FIPS configuration.
   *
   * @param publicKey - the public key
   * @param privateKey - the private key
   * @return a new {@link KeyPair} instance
   * @throws NoSuchAlgorithmException - if the algorithm is not found
   * @throws InvalidKeySpecException - if the key specification is invalid
   * @throws NoSuchProviderException - if the provider is not found
   */
  public static KeyPair createFipsKeyPair(
      final PublicKey publicKey,
      final PrivateKey privateKey) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException
  {
    KeyFactory keyFactory = KeyFactory.getInstance(
        getFipsKeyPairGeneratorAlgorithmOrDefault(),
        getFipsKeyPairGeneratorProviderOrDefault());

    return new KeyPair(
        keyFactory.generatePublic(new X509EncodedKeySpec(publicKey.getEncoded())),
        keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKey.getEncoded())));
  }

  /**
   * Creates a {@link KeyPair} instance based on the environment configuration. One such configuration is the checking
   * of FIPS mode. If FIPS mode is enabled, the key pair is generated using the FIPS permitted values.
   *
   * @return a new {@link KeyPair} instance
   * @throws NoSuchAlgorithmException - if the algorithm is not found
   * @throws NoSuchProviderException - if the provider is not found
   */
  public static KeyPair generateKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException {
    if (FIPSModeDetector.isEnabled()) {
      return generatFipsKeyPair();
    }

    return generateRSAKeyPair();
  }

  /**
   * Creates a {@link KeyPair} instance based on the FIPS configuration.
   *
   * @return a new {@link KeyPair} instance
   * @throws NoSuchAlgorithmException - if the algorithm is not found
   * @throws NoSuchProviderException - if the provider is not found
   */
  public static KeyPair generatFipsKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException {
    SecureRandom secureRandom = createFipsSecureRandom();
    KeyPairGenerator keyPairGenerator = createFipsKeyPairGenerator();
    keyPairGenerator.initialize(getFipsKeyPairGeneratorKeySizeOrDefault(), secureRandom);
    return keyPairGenerator.generateKeyPair();
  }

  /**
   * Creates a {@link KeyPair} instance based on the RSA algorithm.
   *
   * @return a new {@link KeyPair} instance
   * @throws NoSuchAlgorithmException - if the algorithm is not found
   */
  public static KeyPair generateRSAKeyPair() throws NoSuchAlgorithmException {
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
    keyPairGenerator.initialize(KEY_SIZE, new SecureRandom());
    return keyPairGenerator.generateKeyPair();
  }

  /**
   * Creates a {@link SecureRandom} instance based on the FIPS configuration.
   *
   * @return a new {@link SecureRandom} instance
   * @throws NoSuchAlgorithmException - if the algorithm is not found
   * @throws NoSuchProviderException - if the provider is not found
   */
  public static SecureRandom createFipsSecureRandom() throws NoSuchAlgorithmException, NoSuchProviderException {
    return SecureRandom.getInstance(getFipsKeyPairSecureAlgorithmOrDefault(), getFipsKeyPairSecureProviderOrDefault());
  }

  /**
   * Creates a {@link KeyPairGenerator} instance based on the FIPS configuration.
   *
   * @return a new {@link KeyPairGenerator} instance
   * @throws NoSuchAlgorithmException - if the algorithm is not found
   * @throws NoSuchProviderException - if the provider is not found
   */
  public static KeyPairGenerator createFipsKeyPairGenerator() throws NoSuchAlgorithmException, NoSuchProviderException {
    return KeyPairGenerator.getInstance(
        getFipsKeyPairGeneratorAlgorithmOrDefault(),
        getFipsKeyPairGeneratorProviderOrDefault());
  }
}
