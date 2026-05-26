/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_MODE_ENABLED_ENV;
import static com.sonatype.insight.brain.security.FipsTestUtil.insertBouncyCastleFipsProvider;
import static com.sonatype.insight.brain.security.FipsTestUtil.removeBouncyCastleFipsProvider;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import jakarta.inject.Inject;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class CLMLicenseManagerFIPSTest
    extends CLMLicenseManagerTest
{
  @Rule
  public EnvironmentVariables environmentVariables;

  @Inject
  private TestProductLicense testProductLicense;

  @Inject
  private TestProductLicenseDetailsCache testProductLicenseDetailsCache;

  @After
  public void afterFipsTest() {
    removeBouncyCastleFipsProvider();
  }

  @Before
  @Override
  public void before() throws Exception {
    testProductLicense.reset();
    testProductLicenseDetailsCache.resetToDefaults();

    try (InputStream in = getClass().getResourceAsStream("/productlicense/licensing-keystore-hds.bcfks")) {
      assert in != null;
      Files.copy(in, new File(tempDir.getRoot(), "hds.bcfks").toPath());
    }
    hdsMockServer.reset();
    setHdsUrl(hdsMockServer.getHttpUrl());
  }

  @Override
  public TemporaryEntity createTemporaryEntity() {
    // Ensure that the Bouncy Castle FIPS provider is inserted before the TemporaryEntity is created.
    insertBouncyCastleFipsProvider();

    // Initialize the environment rule here so the whole Spring test harness observes FIPS mode.
    environmentVariables = new EnvironmentVariables();
    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");

    return super.createTemporaryEntity();
  }
}
