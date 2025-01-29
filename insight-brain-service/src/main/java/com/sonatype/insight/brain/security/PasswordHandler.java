/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.plexus.components.cipher.PlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

@Named
public class PasswordHandler
{
  private final PlexusCipher cipher;

  private final EncryptionKeyStore encryptionKeyStore;

  @Inject
  public PasswordHandler(PlexusCipher cipher, EncryptionKeyStore encryptionKeyStore) {
    this.cipher = cipher;
    this.encryptionKeyStore = encryptionKeyStore;
  }

  public char[] decryptPassword(char[] encryptedPassword) {
    if (encryptedPassword == null) {
      return null;
    }
    return decryptPassword(String.valueOf(encryptedPassword)).toCharArray();
  }

  public String decryptPassword(String encryptedPassword) {
    return decryptPassword(encryptedPassword, encryptionKeyStore.getKey());
  }

  protected String decryptPassword(final String encryptedPassword, final String encryptionKey) {
    if (encryptedPassword == null) {
      return null;
    }

    // PlexusCipher encrypts empty strings (i.e. "") to "{}".
    // Unfortunately it fails to decrypt "{}" back to an empty string.
    // We handle this special case here.
    if ("{}".equals(encryptedPassword)) {
      return "";
    }

    try {
      synchronized (cipher) {
        return cipher.decryptDecorated(encryptedPassword, encryptionKey);
      }
    }
    catch (PlexusCipherException e) {
      throw new IllegalStateException(e);
    }
  }

  public char[] encryptPassword(final char[] password) {
    if (password == null) {
      return null;
    }
    return encryptPassword(String.valueOf(password)).toCharArray();
  }

  public String encryptPassword(final String password) {
    return encryptPassword(password, encryptionKeyStore.getKey());
  }

  protected String encryptPassword(final String password, final String encryptionKey) {
    if (password == null) {
      return null;
    }
    try {
      synchronized (cipher) {
        return cipher.encryptAndDecorate(password, encryptionKey);
      }
    }
    catch (PlexusCipherException e) {
      throw new IllegalStateException(e);
    }
  }
}
