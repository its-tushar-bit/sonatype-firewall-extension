/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.TreeSet;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.TestLicenseFingerprinter;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.DatabaseConfig;
import com.sonatype.insight.brain.service.HdsMockServerRule;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.SignedProductLicenseDetailsDTO;
import com.sonatype.insight.test.productlicense.ProductLicenseConfig;
import com.sonatype.insight.test.productlicense.ProductLicenseSigner;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ProductLicenseServiceTest
    extends AbstractComponentTest
{
  @ClassRule
  public static HdsMockServerRule hdsMockServer = new HdsMockServerRule();

  @Inject
  private ProductLicenseService productLicenseService;

  @Inject
  private TestLicenseFingerprinter licenseFingerprinter;

  @Inject
  private ProductLicenseSigner productLicenseSigner;

  @Inject
  private InsightConfig config;

  @Before
  public void before() throws Exception {
    Files.copy(getClass().getResourceAsStream("/productlicense/licensing-keystore-hds.p12"),
        new File(tempDir.getRoot(), "hds.p12").toPath());
    hdsMockServer.reset();
    setHdsUrl(hdsMockServer.getHttpUrl());
  }

  @Override
  public void configure(Binder binder) {
    ProductLicenseConfig productLicenseConfig = new ProductLicenseConfig();
    productLicenseConfig.setKeyStorePath(new File(tempDir.getRoot(), "hds.p12").getAbsolutePath());
    productLicenseConfig.setKeyStoreAliasGroup("licensing-key-test");
    binder.bind(ProductLicenseConfig.class).toInstance(productLicenseConfig);
    super.configure(binder);
  }

  @Test
  public void testInstallLicense_ExternalDatabaseNotAllowed() {
    SignedProductLicenseDetailsDTO licenseDetails = new SignedProductLicenseDetailsDTO();
    licenseDetails.version = 1;
    licenseDetails.features = new TreeSet<>();
    licenseDetails.stageIds = new TreeSet<>();
    productLicenseSigner.sign(licenseDetails, licenseFingerprinter.calculate());
    hdsMockServer.respondWith(licenseDetails).atUri("/rest/productLicense/v1").withoutLicense();
    config.setDatabase(new DatabaseConfig());

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> productLicenseService.installLicense(new ByteArrayInputStream(new byte[1]), "auditor.lic"))
        .withMessageContaining("license does not support use of an external database");
  }
}
