/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

public class TestMultiTenantEncryptionKeyStore
    implements EncryptionKeyStore
{
  private static final String ENC = "TESTKEY";

  public TestMultiTenantEncryptionKeyStore() {
  }

  @Override
  public String getKey() {
    return ENC;
  }
}
