/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.apache.shiro.lang.codec.Hex;

/**
 * Hashing service for user token passcodes using plain SHA-256.
 *
 * <p>
 * User tokens are server-generated, high-entropy random values (44 alphanumeric characters, ~260 bits of entropy).
 * Unlike user-chosen passwords, they do not need expensive memory-hard KDFs like Argon2id to resist brute-force
 * attacks. A single SHA-256 pass is cryptographically sufficient and dramatically more efficient.
 * </p>
 *
 * <p>
 * This service uses {@link java.security.MessageDigest} which leverages JVM intrinsics for hardware-accelerated
 * SHA-256 (SHA-NI on x86_64, ARM Crypto Extensions on AArch64/Graviton).
 * </p>
 *
 * <p>
 * Stored hash format: {@code $sha256$<64-char-hex-digest>}
 * </p>
 *
 * <p>
 * No per-token salt is used. With 260 bits of entropy in the token value, rainbow tables and
 * multi-target attacks are computationally infeasible regardless of salting.
 * </p>
 */
@Named
@Singleton
public class UserTokenHashService
{
  static final String SHA256_PREFIX = "$sha256$";

  private static final String SHA_256_ALGORITHM = "SHA-256";

  /**
   * Hashes a plaintext passcode using SHA-256.
   *
   * @param plainPassCode the plaintext passcode as a char array (not converted to String to avoid lingering on heap)
   * @return the hash in the format {@code $sha256$<hex>}
   */
  public String hashPassCode(char[] plainPassCode) {
    byte[] digest = sha256(plainPassCode);
    return SHA256_PREFIX + Hex.encodeToString(digest);
  }

  /**
   * Verifies a plaintext passcode against a stored SHA-256 hash.
   * The stored hash must be in the {@code $sha256$<hex>} format (check with {@link #supports} first).
   *
   * @param plainPassCode the plaintext passcode presented for authentication
   * @param storedHash the stored hash from the database
   * @return true if the passcode matches
   */
  public boolean verifyPassCode(char[] plainPassCode, String storedHash) {
    byte[] expectedDigest = Hex.decode(storedHash.substring(SHA256_PREFIX.length()));
    byte[] actualDigest = sha256(plainPassCode);
    boolean matches = MessageDigest.isEqual(expectedDigest, actualDigest);
    Arrays.fill(actualDigest, (byte) 0);
    return matches;
  }

  /**
   * Returns true if the stored hash is in the SHA-256 format produced by this service.
   */
  public boolean supports(String storedHash) {
    return storedHash != null && storedHash.startsWith(SHA256_PREFIX);
  }

  private byte[] sha256(char[] input) {
    MessageDigest md;
    try {
      md = MessageDigest.getInstance(SHA_256_ALGORITHM);
    }
    catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm not available", e);
    }

    // Convert char[] to byte[] without creating an intermediate String
    // Note: ByteBuffer typically will be longer than necessary as encode() estimates the size before determining the
    // variable lengths of each codepoint.
    ByteBuffer inputByteBuffer = StandardCharsets.UTF_8.encode(CharBuffer.wrap(input));

    byte[] inputBytes;
    int byteLength = inputByteBuffer.remaining();
    if (inputByteBuffer.hasArray()) {
      inputBytes = inputByteBuffer.array();
    }
    else {
      inputBytes = new byte[inputByteBuffer.remaining()];
      inputByteBuffer.get(inputBytes);

      // Clear sensitive data from memory
      inputByteBuffer.clear();
      while (inputByteBuffer.hasRemaining()) {
        inputByteBuffer.put((byte) 0);
      }
    }

    int offset = inputByteBuffer.hasArray() ? inputByteBuffer.arrayOffset() : 0;
    md.update(inputBytes, offset, byteLength);
    byte[] outputBytes = md.digest();

    // Clear sensitive data from memory
    Arrays.fill(inputBytes, (byte) 0);

    return outputBytes;
  }
}
