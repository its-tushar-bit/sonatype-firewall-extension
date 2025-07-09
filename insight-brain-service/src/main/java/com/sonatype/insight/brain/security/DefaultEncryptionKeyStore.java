/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import javax.inject.Named;
import javax.inject.Singleton;

import static com.sonatype.insight.brain.security.keystore.KeyStoreFactory.getDefaultEncryptionKeyStoreKey;

@Named
@Singleton
public class DefaultEncryptionKeyStore
    implements EncryptionKeyStore
{
  @Override
  public String getKey() {
    return getDefaultEncryptionKeyStoreKey();
  }
}
