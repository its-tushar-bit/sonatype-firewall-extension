/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.banning.rest.api.v2;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.security.EncryptionKeyStore;
import com.sonatype.insight.brain.security.FIPSConfig;
import com.sonatype.insight.brain.security.TestFipsEncryptionKeyStore;

import org.junit.After;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import static com.sonatype.insight.brain.security.FipsTestUtil.insertBouncyCastleFipsProvider;
import static com.sonatype.insight.brain.security.FipsTestUtil.removeBouncyCastleFipsProvider;

public class MultiTenantSamlConfigurationResourceFIPSTest
    extends MultiTenantSamlConfigurationResourceTest
{
  private EnvironmentVariables environmentVariables;

  @Override
  protected TemporaryEntity createTenantTemporaryEntity() {
    // Ensure that the Bouncy Castle FIPS provider is inserted before the TemporaryEntity is created.
    insertBouncyCastleFipsProvider();

    // Initialize the EnvironmentVariables here instead of as a class variable as this gets run as part of a JUnit rule
    environmentVariables = new EnvironmentVariables();
    environmentVariables.set(FIPSConfig.FIPS_MODE_ENABLED_ENV, "true");

    return super.createTenantTemporaryEntity();
  }

  @After
  @Override
  public void cleanupTest() throws Exception {
    super.cleanupTest();

    // Ensure that the Bouncy Castle FIPS provider is removed after the tests as
    // some providers are accessed in the afterTest parent method.
    removeBouncyCastleFipsProvider();
  }

  @Override
  protected Class<? extends EncryptionKeyStore> getEncryptionKeyStoreClass() {
    // Return the FIPS compliant encryption key store class.
    return TestFipsEncryptionKeyStore.class;
  }
}
