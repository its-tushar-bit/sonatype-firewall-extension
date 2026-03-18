/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import static com.sonatype.insight.brain.security.FIPSConfig.getFipsCryptoProvider;
import static com.sonatype.insight.brain.security.FIPSConfig.getFipsKeystoreKeyLengthBitsOrDefault;
import static com.sonatype.insight.brain.security.FIPSConfig.getFipsKeystorePbkdf2AlgorithmOrDefault;
import static com.sonatype.insight.brain.security.FIPSConfig.getFipsKeystorePbkdf2IterationsOrDefault;
import static com.sonatype.insight.brain.security.FIPSConfig.getFipsKeystoreSaltOrDefault;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Generates the same password for the same system by combining stable system identifiers
 * with FIPS-approved key derivation algorithms.
 */
public class FIPSKeystorePasswordGenerator
{
  private FIPSKeystorePasswordGenerator() {
    // prevent instantiation
  }

  /**
   * Generate a deterministic FIPS-compliant keystore password.
   * The password is derived from stable system identifiers and will be the same
   * for the same system across restarts.
   *
   * @param sonatypeWorkDirectory the sonatype-work directory for additional entropy
   * @return deterministically-generated keystore password
   * @throws GeneralSecurityException if key derivation fails
   */
  public static String generateDeterministicPassword(final File sonatypeWorkDirectory) throws GeneralSecurityException {
    String systemIdentifiers = collectSystemIdentifiers(sonatypeWorkDirectory);
    return derivePassword(systemIdentifiers);
  }

  private static String collectSystemIdentifiers(final File sonatypeWorkDirectory) {
    StringBuilder identifiers = new StringBuilder();

    // Installation path - provides uniqueness per installation
    identifiers.append(getInstallationPath(sonatypeWorkDirectory));

    // Stable system hardware identifiers
    String hardwareId = getStableHardwareId();
    if (isNotBlank(hardwareId)) {
      identifiers.append("|").append(hardwareId);
    }

    return identifiers.toString();
  }

  private static String getInstallationPath(final File sonatypeWorkDirectory) {
    return sonatypeWorkDirectory.getAbsolutePath();
  }

  private static String getStableHardwareId() {
    return System.getProperty("os.name", "") +
        "|" + System.getProperty("os.arch", "") +
        "|" + System.getProperty("file.separator", "") +
        "|" + System.getProperty("path.separator", "");
  }

  private static String derivePassword(final String input) throws GeneralSecurityException {
    SecretKeyFactory factory = SecretKeyFactory.getInstance(getFipsKeystorePbkdf2AlgorithmOrDefault(),
        getFipsCryptoProvider());
    KeySpec spec = new PBEKeySpec(
        input.toCharArray(),
        getFipsKeystoreSaltOrDefault().getBytes(StandardCharsets.UTF_8),
        getFipsKeystorePbkdf2IterationsOrDefault(),
        getFipsKeystoreKeyLengthBitsOrDefault());
    byte[] derivedKey = factory.generateSecret(spec).getEncoded();
    return Base64.getEncoder().encodeToString(derivedKey);
  }
}
