/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultEncryptionKeyStoreTest
    extends AbstractComponentTest
{
  @Inject
  private DefaultEncryptionKeyStore encryptionKeyStore;

  @Test
  public void testGetKey() {
    assertThat(encryptionKeyStore.getKey()).isEqualTo("CMMDwoV");
  }
}
