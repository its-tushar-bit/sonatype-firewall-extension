/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

public class TestMultiTenantEncryptionKeyStore
    implements EncryptionKeyStore
{
  private static final String NON_FIPS_ENC = "TESTKEY";

  private static final String FIPS_ENC = "TESTKEY123456789"; // 16 bytes for AES-128

  public TestMultiTenantEncryptionKeyStore() {
  }

  @Override
  public String getKey() {
    // If FIPS mode is enabled, return a key suitable for FIPS cipher (16 bytes)
    if (FIPSModeDetector.isEnabled()) {
      return FIPS_ENC;
    }
    return NON_FIPS_ENC;
  }
}
