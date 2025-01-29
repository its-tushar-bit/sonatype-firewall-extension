/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.JobExecutionException;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ReloadTenantEncryptionKeyJobTest
{
  @Mock
  EncryptionKeyStore encryptionKeyStore;

  @Test
  public void testExecute_shouldCallInitializeKey() throws JobExecutionException {
    ReloadTenantEncryptionKeyJob underTest = new ReloadTenantEncryptionKeyJob(encryptionKeyStore);

    underTest.execute(null);

    verify(encryptionKeyStore).initializeKey();
  }
}
