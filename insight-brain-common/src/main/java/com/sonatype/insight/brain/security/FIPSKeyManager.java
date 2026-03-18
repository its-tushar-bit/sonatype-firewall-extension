/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.security.FIPSConfig.getFipsCryptoProvider;
import static com.sonatype.insight.brain.security.FIPSConfig.getFipsEncryptionAlgorithm;
import static com.sonatype.insight.brain.security.FIPSConfig.getFipsKeyStoreProviderOrDefault;
import static com.sonatype.insight.brain.security.FIPSConfig.getFipsKeyStoreTypeOrDefault;
import static com.sonatype.insight.brain.security.keypair.KeyPairFactory.createFipsSecureRandom;

import com.sonatype.insight.brain.security.keystore.KeyStoreFactory;

/**
 * Manages encryption key generation and storage using a BCFKS keystore for single tenant IQ in FIPS mode.
 */
public class FIPSKeyManager
{
  private static final Logger log = LoggerFactory.getLogger(FIPSKeyManager.class);

  private static final String KEYSTORE_FILENAME = "fips-encryption.keystore";

  private static final String KEY_ALIAS = "fips-encryption-key";

  private static final int AES_KEY_SIZE = 256;

  private final File keystoreFile;

  private final File sonatypeWorkDirectory;

  public FIPSKeyManager(final File keystoreDirectory) {
    if (keystoreDirectory == null) {
      throw new IllegalArgumentException("Keystore directory cannot be null");
    }

    this.keystoreFile = new File(keystoreDirectory, KEYSTORE_FILENAME);

    // Derive sonatype work directory from keystoreDirectory path (fips -> data -> sonatype-work)
    this.sonatypeWorkDirectory = keystoreDirectory.getParentFile().getParentFile();

    try {
      Files.createDirectories(keystoreDirectory.toPath());
    }
    catch (IOException e) {
      throw new IllegalStateException("Failed to create keystore directory: " + keystoreDirectory, e);
    }
  }

  /**
   * Gets the encryption key, generating and storing it if it doesn't exist.
   *
   * @return the 32-character plaintext encryption key
   * @throws FIPSKeyException if key generation or retrieval fails
   */
  public String getOrGenerateKey() throws FIPSKeyException {
    if (keystoreExists()) {
      return retrieveKey();
    }
    else {
      return generateAndStoreKey();
    }
  }

  private String generateAndStoreKey() throws FIPSKeyException {
    log.info("Generating new FIPS encryption key");

    try {
      SecretKey secretKey = generateSecretKey();
      storeKey(secretKey);

      String encodedKey = encodeKey(secretKey);
      log.info("Successfully generated and stored FIPS encryption key");
      return encodedKey;
    }
    catch (Exception e) {
      throw new FIPSKeyException("Failed to generate and store FIPS encryption key", e);
    }
  }

  private String retrieveKey() throws FIPSKeyException {
    log.debug("Retrieving FIPS encryption key from keystore");

    try {
      KeyStore keyStore = loadKeyStore();
      SecretKey secretKey = (SecretKey) keyStore.getKey(KEY_ALIAS, getKeystorePassword().toCharArray());

      if (secretKey == null) {
        throw new FIPSKeyException("FIPS encryption key not found in keystore");
      }

      return encodeKey(secretKey);
    }
    catch (IOException | GeneralSecurityException e) {
      throw new FIPSKeyException("Failed to retrieve FIPS encryption key from keystore", e);
    }
  }

  private boolean keystoreExists() {
    return keystoreFile.exists() && keystoreFile.isFile();
  }

  private String getKeystorePassword() throws FIPSKeyException {
    try {
      return KeyStoreFactory.getFipsKeystorePassword(sonatypeWorkDirectory);
    }
    catch (GeneralSecurityException e) {
      throw new FIPSKeyException("Failed to generate deterministic keystore password", e);
    }
  }

  private SecretKey generateSecretKey() throws NoSuchAlgorithmException, NoSuchProviderException {
    String cryptoProvider = getFipsCryptoProvider();
    KeyGenerator keyGenerator = KeyGenerator.getInstance(getFipsEncryptionAlgorithm(), cryptoProvider);
    keyGenerator.init(AES_KEY_SIZE, createFipsSecureRandom());
    return keyGenerator.generateKey();
  }

  private void storeKey(final SecretKey secretKey) throws GeneralSecurityException, IOException, FIPSKeyException {
    KeyStore keyStore = createNewKeyStore();

    String keystorePassword = getKeystorePassword();
    KeyStore.SecretKeyEntry secretKeyEntry = new KeyStore.SecretKeyEntry(secretKey);
    KeyStore.PasswordProtection passwordProtection =
        new KeyStore.PasswordProtection(keystorePassword.toCharArray());

    keyStore.setEntry(KEY_ALIAS, secretKeyEntry, passwordProtection);

    try (FileOutputStream fos = new FileOutputStream(keystoreFile)) {
      keyStore.store(fos, keystorePassword.toCharArray());
    }

    setSecureFilePermissions();
  }

  private KeyStore loadKeyStore() throws GeneralSecurityException, IOException, FIPSKeyException {
    KeyStore keyStore = KeyStore.getInstance(getFipsKeyStoreTypeOrDefault(), getFipsKeyStoreProviderOrDefault());

    try (FileInputStream fis = new FileInputStream(keystoreFile)) {
      keyStore.load(fis, getKeystorePassword().toCharArray());
    }

    return keyStore;
  }

  private KeyStore createNewKeyStore() throws GeneralSecurityException, IOException, FIPSKeyException {
    KeyStore keyStore = KeyStore.getInstance(getFipsKeyStoreTypeOrDefault(), getFipsKeyStoreProviderOrDefault());
    keyStore.load(null, getKeystorePassword().toCharArray());
    return keyStore;
  }

  private String encodeKey(final SecretKey secretKey) {
    byte[] keyBytes = secretKey.getEncoded();
    // Convert 32-byte (256 bit) AES key to 32-character string using printable ASCII range 33-126
    return IntStream.range(0, 32)
        .map(i -> Byte.toUnsignedInt(keyBytes[i]) % 94 + 33)
        .mapToObj(value -> String.valueOf((char) value))
        .collect(Collectors.joining());
  }

  private void setSecureFilePermissions() {
    if (!keystoreFile.exists()) {
      log.warn("Keystore file does not exist, cannot set permissions");
      return;
    }

    try {
      Set<PosixFilePermission> ownerOnly = EnumSet.of(
          PosixFilePermission.OWNER_READ,
          PosixFilePermission.OWNER_WRITE);
      Files.setPosixFilePermissions(keystoreFile.toPath(), ownerOnly);
      log.debug("Set secure file permissions (600) on keystore file");
    }
    catch (UnsupportedOperationException e) {
      setWindowsFilePermissions();
    }
    catch (IOException e) {
      log.warn("Failed to set secure file permissions on keystore file", e);
    }
  }

  private void setWindowsFilePermissions() {
    try {
      AclFileAttributeView aclView = Files.getFileAttributeView(keystoreFile.toPath(), AclFileAttributeView.class);
      if (aclView == null) {
        log.warn("Unable to set Windows ACL permissions - ACL not supported");
        return;
      }

      UserPrincipal owner = Files.getOwner(keystoreFile.toPath());

      AclEntry ownerEntry = AclEntry.newBuilder()
          .setType(AclEntryType.ALLOW)
          .setPrincipal(owner)
          .setPermissions(
              AclEntryPermission.READ_DATA,
              AclEntryPermission.WRITE_DATA,
              AclEntryPermission.READ_ATTRIBUTES,
              AclEntryPermission.WRITE_ATTRIBUTES,
              AclEntryPermission.READ_ACL,
              AclEntryPermission.SYNCHRONIZE)
          .build();

      aclView.setAcl(Collections.singletonList(ownerEntry));
      log.debug("Set secure Windows ACL permissions on keystore file");
    }
    catch (IOException e) {
      log.warn("Failed to set Windows ACL permissions on keystore file", e);
    }
  }

  public static class FIPSKeyException
      extends Exception
  {
    public FIPSKeyException(final String message) {
      super(message);
    }

    public FIPSKeyException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }
}
