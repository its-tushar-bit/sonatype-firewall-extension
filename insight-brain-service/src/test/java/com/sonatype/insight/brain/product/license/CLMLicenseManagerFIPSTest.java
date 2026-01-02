/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import com.sonatype.insight.brain.security.FIPSConfig;

import org.junit.Before;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.experimental.categories.Category;

import static com.sonatype.insight.brain.security.FipsTestUtil.insertBouncyCastleFipsProvider;

public class CLMLicenseManagerFIPSTest
    extends CLMLicenseManagerTest
{
  @Rule
  public EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Before
  @Override
  public void before() throws Exception {
    // Ensure that the Bouncy Castle FIPS provider is inserted before the tests.
    insertBouncyCastleFipsProvider();

    // Set the environment variable to enable FIPS mode.
    environmentVariables.set(FIPSConfig.FIPS_MODE_ENABLED_ENV, "true");

    // Initialize the parent class.
    super.before();

    // TODO - Uncomment after the HDS code is updated to use the new keystore format. See HDS-3252
    //try (InputStream in = getClass().getResourceAsStream("/productlicense/licensing-keystore-hds.bcfks")) {
    //  assert in != null;
    //  Files.copy(in, new File(tempDir.getRoot(), "hds.bcfks").toPath());
    //}
    //hdsMockServer.reset();
    //setHdsUrl(hdsMockServer.getHttpUrl());
  }


  /*
  TODO - Uncomment after the HDS code is updated to use the new keystore format. See HDS-3252

  @Override
  public void configure(Binder binder) {
    ProductLicenseConfig productLicenseConfig = new ProductLicenseConfig();
    productLicenseConfig.setKeyStorePath(new File(tempDir.getRoot(), "hds.bcfks").getAbsolutePath());
    productLicenseConfig.setKeyStoreAliasGroup("licensing-key-test");
    binder.bind(ProductLicenseConfig.class).toInstance(productLicenseConfig);
    binder.bind(TaskScheduler.class).toInstance(taskSchedulerMock);
    super.configure(binder);
  }*/
}
