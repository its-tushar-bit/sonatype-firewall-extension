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

    // PlexusCipher encrypts empty strings (i.e. "") to "{}".
    // Unfortunately it fails to decrypt "{}" back to an empty string.
    // We handle this special case here.
    if ("{}".equals(String.valueOf(encryptedPassword))) {
      return "".toCharArray();
    }

    try {
      synchronized (cipher) {
        return cipher.decryptDecorated(String.valueOf(encryptedPassword), encryptionKeyStore.getKey()).toCharArray();
      }
    }
    catch (PlexusCipherException e) {
      throw new IllegalStateException(e);
    }
  }

  public char[] encryptPassword(char[] password) {
    if (password == null) {
      return null;
    }

    try {
      synchronized (cipher) {
        return cipher.encryptAndDecorate(String.valueOf(password), encryptionKeyStore.getKey()).toCharArray();
      }
    }
    catch (PlexusCipherException e) {
      throw new IllegalStateException(e);
    }
  }
}
