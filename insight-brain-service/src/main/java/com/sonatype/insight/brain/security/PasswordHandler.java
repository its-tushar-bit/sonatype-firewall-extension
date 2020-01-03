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
  private static final String ENC = "CMMDwoV";

  private final PlexusCipher cipher;

  @Inject
  public PasswordHandler(PlexusCipher cipher) {
    this.cipher = cipher;
  }

  public char[] decryptPassword(char[] encryptedPassword) {
    if (encryptedPassword == null) {
      return null;
    }

    try {
      synchronized (cipher) {
        return cipher.decryptDecorated(String.valueOf(encryptedPassword), ENC).toCharArray();
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
        return cipher.encryptAndDecorate(String.valueOf(password), ENC).toCharArray();
      }
    }
    catch (PlexusCipherException e) {
      throw new IllegalStateException(e);
    }
  }
}
