/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security.keypair;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_KEY_PAIR_GENERATOR_ALGORITHM_ENV;
import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_KEY_PAIR_GENERATOR_KEY_SIZE_ENV;
import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_KEY_PAIR_GENERATOR_PROVIDER_ENV;
import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_KEY_PAIR_SECURE_ALGORITHM_ENV;
import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_KEY_PAIR_SECURE_PROVIDER_ENV;
import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_MODE_ENABLED_ENV;
import static com.sonatype.insight.brain.security.FipsTestUtil.insertBouncyCastleFipsProvider;
import static com.sonatype.insight.brain.security.FipsTestUtil.removeBouncyCastleFipsProvider;
import static com.sonatype.insight.brain.security.keypair.KeyPairFactory.RSA_ALGORITHM;
import static com.sonatype.insight.brain.security.keypair.KeyPairFactory.createFipsKeyPairGenerator;
import static com.sonatype.insight.brain.security.keypair.KeyPairFactory.createFipsSecureRandom;
import static com.sonatype.insight.brain.security.keypair.KeyPairFactory.createKeyPair;
import static com.sonatype.insight.brain.security.keypair.KeyPairFactory.generateKeyPair;
import static com.sonatype.insight.brain.security.keypair.KeyPairFactory.generateRSAKeyPair;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(MockitoJUnitRunner.class)
public class KeyPairFactoryTest
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
  public void testCreateKeyPair() throws Exception {
    KeyPair original = generateKeyPair();
    KeyPair keyPair = createKeyPair(original.getPublic(), original.getPrivate());

    assertThat(keyPair.getPublic()).isEqualTo(original.getPublic());
    assertThat(keyPair.getPrivate()).isEqualTo(original.getPrivate());
  }

  @Test
  public void testCreateKeyPair_WithFipsEnabled() throws Exception {
    insertBouncyCastleFipsProvider();
    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");

    KeyPair original = generateKeyPair();
    KeyPair keyPair = createKeyPair(original.getPublic(), original.getPrivate());
    assertThat(keyPair.getPublic()).isEqualTo(original.getPublic());
    assertThat(keyPair.getPrivate()).isEqualTo(original.getPrivate());
  }

  @Test
  public void testCreateKeyPair_WithFipsEnabledAndDisabled_AreNotTheSame() throws Exception {
    insertBouncyCastleFipsProvider();
    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");

    KeyPair originalWithFips = generateKeyPair();
    KeyPair keyPairWithFips = createKeyPair(originalWithFips.getPublic(), originalWithFips.getPrivate());
    assertThat(keyPairWithFips.getPublic()).isEqualTo(originalWithFips.getPublic());
    assertThat(keyPairWithFips.getPrivate()).isEqualTo(originalWithFips.getPrivate());

    // proof that creating the same key pair with FIPS enabled results in the same outcome
    keyPairWithFips = createKeyPair(originalWithFips.getPublic(), originalWithFips.getPrivate());
    assertThat(keyPairWithFips.getPublic()).isEqualTo(originalWithFips.getPublic());
    assertThat(keyPairWithFips.getPrivate()).isEqualTo(originalWithFips.getPrivate());

    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "false");

    // proof that creating the same key pair with FIPS disabled results in a different keypair
    KeyPair originalWithOutFips = generateKeyPair();
    KeyPair keyPairWithOutFips = createKeyPair(originalWithOutFips.getPublic(), originalWithOutFips.getPrivate());
    assertThat(keyPairWithOutFips.getPublic()).isEqualTo(originalWithOutFips.getPublic());
    assertThat(keyPairWithOutFips.getPrivate()).isEqualTo(originalWithOutFips.getPrivate());
    assertThat(keyPairWithOutFips.getPublic()).isNotEqualTo(originalWithFips.getPublic());
    assertThat(keyPairWithOutFips.getPrivate()).isNotEqualTo(originalWithFips.getPrivate());
  }

  @Test
  public void testCreateKeyPair_WithFipsEnabled_AndRSAKeyPair_Throws_NoSuchProviderException() throws NoSuchAlgorithmException {
    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");

    KeyPair keyPair = generateRSAKeyPair();
    assertThatThrownBy(() -> createKeyPair(keyPair.getPublic(), keyPair.getPrivate()))
        .isInstanceOf(NoSuchProviderException.class)
        .hasMessageContaining("no such provider: BCFIPS");
  }

  @Test
  public void testGenerateKeyPair() throws Exception {
    KeyPair keyPair = generateKeyPair();
    assertThat(keyPair.getPublic().getAlgorithm()).isEqualTo(RSA_ALGORITHM);
    assertThat(keyPair.getPrivate().getAlgorithm()).isEqualTo(RSA_ALGORITHM);
    assertThat(keyPair.getPublic().getFormat()).isEqualTo("X.509");
    assertThat(keyPair.getPrivate().getAlgorithm()).isEqualTo(RSA_ALGORITHM);
    assertThat(keyPair.getPrivate().getFormat()).isEqualTo("PKCS#8");
  }

  @Test
  public void testGenerateKeyPair_WithFipsEnabled() throws Exception {
    insertBouncyCastleFipsProvider();

    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");

    KeyPair keyPair = generateKeyPair();
    assertThat(keyPair.getPublic().getAlgorithm()).isEqualTo("RSA");
    assertThat(keyPair.getPublic().getFormat()).isEqualTo("X.509");
    assertThat(keyPair.getPrivate().getAlgorithm()).isEqualTo("RSA");
    assertThat(keyPair.getPrivate().getFormat()).isEqualTo("PKCS#8");
  }

  @Test
  public void testGenerateKeyPair_WithFipsEnabled_AndDefaultsOverriddenByEnvironmentVariables() throws Exception {
    insertBouncyCastleFipsProvider();

    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");

    environmentVariables.set(FIPS_KEY_PAIR_SECURE_ALGORITHM_ENV, "DEFAULT");
    environmentVariables.set(FIPS_KEY_PAIR_SECURE_PROVIDER_ENV, "BCFIPS");
    environmentVariables.set(FIPS_KEY_PAIR_GENERATOR_PROVIDER_ENV, "BCFIPS");
    environmentVariables.set(FIPS_KEY_PAIR_GENERATOR_ALGORITHM_ENV, "RSA");
    environmentVariables.set(FIPS_KEY_PAIR_GENERATOR_KEY_SIZE_ENV, "1024");

    KeyPair keyPair = generateKeyPair();
    assertThat(keyPair.getPublic().getAlgorithm()).isEqualTo("RSA");
    assertThat(keyPair.getPublic().getFormat()).isEqualTo("X.509");
    assertThat(keyPair.getPrivate().getAlgorithm()).isEqualTo("RSA");
    assertThat(keyPair.getPrivate().getFormat()).isEqualTo("PKCS#8");
  }

  @Test
  public void testGenerateKeyPair_WithFipsEnabledAndDisabled_AreNotTheSame() throws Exception {
    insertBouncyCastleFipsProvider();

    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");

    KeyPair originalWithFips = generateKeyPair();

    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "false");

    KeyPair originalWithOutFips = generateKeyPair();
    assertThat(originalWithFips.getPublic()).isNotEqualTo(originalWithOutFips.getPublic());
    assertThat(originalWithFips.getPrivate()).isNotEqualTo(originalWithOutFips.getPrivate());
  }

  @Test
  public void testGenerateKeyPair_WithFipsEnabled_Throws_NoSuchProviderException() {
    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");

    assertThatThrownBy(KeyPairFactory::generateKeyPair)
        .isInstanceOf(NoSuchProviderException.class)
        .hasMessageContaining("no such provider: BCFIPS");
  }

  @Test
  public void testCreateFipsSecureRandom() throws Exception {
    insertBouncyCastleFipsProvider();

    SecureRandom secureRandom = createFipsSecureRandom();
    assertThat(secureRandom.getProvider().getName()).isEqualTo("BCFIPS");
    assertThat(secureRandom.getAlgorithm()).isEqualTo("DEFAULT");
  }

  @Test
  public void testCreateFipsSecureRandom_Throws_NoSuchProviderException() {
    assertThatThrownBy(KeyPairFactory::createFipsSecureRandom)
        .isInstanceOf(NoSuchProviderException.class)
        .hasMessageContaining("no such provider: BCFIPS");
  }

  @Test
  public void testCreateFipsKeyPairGenerator() throws Exception {
    insertBouncyCastleFipsProvider();

    KeyPairGenerator keyPairGenerator = createFipsKeyPairGenerator();
    assertThat(keyPairGenerator.getProvider().getName()).isEqualTo("BCFIPS");
    assertThat(keyPairGenerator.getAlgorithm()).isEqualTo("RSA");
  }

  @Test
  public void testCreateFipsKeyPairGenerator_Throws_NoSuchProviderException() {
    assertThatThrownBy(KeyPairFactory::createFipsKeyPairGenerator)
        .isInstanceOf(NoSuchProviderException.class)
        .hasMessageContaining("no such provider: BCFIPS");
  }
}
