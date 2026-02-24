/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import org.apache.commons.lang3.StringUtils;

/**
 * Utility class for parsing GitHub App RSA private keys.
 *
 * @since 1.201
 */
public final class GitHubAppKeyUtils
{
  private GitHubAppKeyUtils() {
  }

  /**
   * Parse an RSA private key from Base64-encoded PKCS8 format.
   *
   * This format matches what is stored in the database after encryption:
   * - ApiGitHubAppService.processAndEncryptPrivateKey() converts PEM → PrivateKey → PKCS8 bytes → Base64 → Encrypt
   * - This method reverses: Decrypt → Base64 → PKCS8 bytes → PrivateKey
   *
   * @param base64Pkcs8 The Base64-encoded PKCS8 private key string
   * @return The parsed PrivateKey object
   * @throws IllegalArgumentException if base64Pkcs8 is null/empty or has invalid format
   */
  public static PrivateKey parsePrivateKeyFromBase64Pkcs8(final String base64Pkcs8) {
    if (StringUtils.isBlank(base64Pkcs8)) {
      throw new IllegalArgumentException("Private key content cannot be null or empty");
    }

    try {
      byte[] pkcs8Bytes = Base64.getDecoder().decode(base64Pkcs8);
      PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8Bytes);
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      return keyFactory.generatePrivate(keySpec);
    }
    catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Failed to parse RSA private key from Base64 PKCS8 format: " + e.getMessage(), e);
    }
    catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new IllegalArgumentException("Failed to parse RSA private key from Base64 PKCS8 format", e);
    }
  }
}
