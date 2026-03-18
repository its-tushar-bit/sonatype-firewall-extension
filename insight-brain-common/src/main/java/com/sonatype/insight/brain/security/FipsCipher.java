/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.sonatype.plexus.components.cipher.PlexusCipher;

import static com.sonatype.insight.brain.security.FIPSConfig.getFipsCryptoProvider;
import static com.sonatype.insight.brain.security.FIPSConfig.getFipsEncryptionAlgorithm;
import static com.sonatype.insight.brain.security.FIPSConfig.getFipsEncryptionMode;
import static com.sonatype.insight.brain.security.FIPSConfig.getFipsIVLength;

public class FipsCipher
    implements PlexusCipher
{
  private static final Pattern ENCRYPTED_STRING_PATTERN = Pattern.compile(".*?[^\\\\]?\\{(.*?[^\\\\])}.*");

  private final Cipher cipher;

  public FipsCipher() {
    try {
      this.cipher = Cipher.getInstance(getFipsEncryptionMode(), getFipsCryptoProvider());
    }
    catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  public FipsCipher(final Cipher cipher) {
    this.cipher = cipher;
  }

  public Cipher getCipher() {
    return cipher;
  }

  @Override
  public String encryptAndDecorate(
      final String plaintext,
      final String encryptionKey)
  {
    if (StringUtils.isNotEmpty(plaintext)) {
      return decorate(encrypt(plaintext, encryptionKey));
    }
    return plaintext;
  }

  @Override
  public String decryptDecorated(final String str, final String encryptionKey) {
    // Check for a decorated empty string
    if ("{}".equals(str)) {
      return "";
    }
    if (StringUtils.isNotEmpty(str)) {
      return isEncryptedString(str) ? decrypt(unDecorate(str), encryptionKey) : decrypt(str, encryptionKey);
    }
    return str;
  }

  @Override
  public String encrypt(
      final String plaintext,
      final String encryptionKey)
  {
    try {
      byte[] ivBytes = generateIVBytes();
      cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(encryptionKey, getFipsEncryptionAlgorithm()),
          new IvParameterSpec(ivBytes));
      byte[] encrypted = cipher.doFinal(plaintext.getBytes());
      byte[] ivAndEncrypted = new byte[ivBytes.length + encrypted.length];
      // Prepend the IV to the encrypted data so that it can be fetched and used during decryption
      System.arraycopy(ivBytes, 0, ivAndEncrypted, 0, ivBytes.length);
      System.arraycopy(encrypted, 0, ivAndEncrypted, ivBytes.length, encrypted.length);
      return new String(Base64.getEncoder().encode(ivAndEncrypted), StandardCharsets.UTF_8);
    }
    catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public String decrypt(final String encrypted, final String encryptionKey) {
    try {
      int ivLength = getFipsIVLength();
      byte[] decodedIVAndEncrypted = Base64.getDecoder().decode(encrypted);
      byte[] iv = Arrays.copyOfRange(decodedIVAndEncrypted, 0, ivLength);
      byte[] encryptedText = Arrays.copyOfRange(decodedIVAndEncrypted, ivLength, decodedIVAndEncrypted.length);
      cipher.init(Cipher.DECRYPT_MODE, getSecretKey(encryptionKey, getFipsEncryptionAlgorithm()),
          new IvParameterSpec(iv));
      return new String(cipher.doFinal(encryptedText));
    }
    catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public String decorate(final String str) {
    return '{' + (str == null ? "" : str) + '}';
  }

  @Override
  public String unDecorate(final String str) {
    Matcher matcher = ENCRYPTED_STRING_PATTERN.matcher(str);
    if (!matcher.matches() && !matcher.find()) {
      throw new IllegalArgumentException("Unexpected format for encrypted string: " + str);
    }
    return matcher.group(1);
  }

  @Override
  public boolean isEncryptedString(final String str) {
    if (StringUtils.isNotEmpty(str)) {
      Matcher matcher = ENCRYPTED_STRING_PATTERN.matcher(str);
      return matcher.matches() || matcher.find();
    }
    return false;
  }

  private SecretKey getSecretKey(final String decryptedKey, final String algorithm) {
    return new SecretKeySpec(decryptedKey.getBytes(StandardCharsets.UTF_8), algorithm);
  }

  private byte[] generateIVBytes() {
    byte[] ivBytes = new byte[getFipsIVLength()];
    new SecureRandom().nextBytes(ivBytes);
    return ivBytes;
  }
}
