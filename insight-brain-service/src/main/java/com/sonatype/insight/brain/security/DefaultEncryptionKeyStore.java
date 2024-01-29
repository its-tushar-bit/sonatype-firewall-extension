/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import javax.inject.Named;
import javax.inject.Singleton;

@Named
@Singleton
public class DefaultEncryptionKeyStore
    implements EncryptionKeyStore
{
  private static final String ENC = "CMMDwoV";

  @Override
  public String getKey() {
    return ENC;
  }
}
