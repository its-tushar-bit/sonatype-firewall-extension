/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.sonatype.insight.brain.TestLicenseFingerprinter;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.service.HdsMockServerRule;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.SignedProductLicenseDetailsDTO;
import com.sonatype.insight.test.productlicense.ProductLicenseSigner;
import jakarta.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.TreeSet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@ComponentH2Test
public class ProductLicenseServiceTest
    extends AbstractComponentH2Test
{
  private static final HdsMockServerRule hdsMockServer = new HdsMockServerRule();

  @BeforeAll
  public static void startHdsMockServer() throws Exception {
    hdsMockServer.start();
  }

  @AfterAll
  public static void stopHdsMockServer() {
    hdsMockServer.stop();
  }

  @Inject
  private ProductLicenseService productLicenseService;

  @Inject
  private TestLicenseFingerprinter licenseFingerprinter;

  @Inject
  private ProductLicenseSigner productLicenseSigner;

  @Inject
  private InsightConfig config;

  @BeforeEach
  public void before() throws Exception {
    Files.copy(getClass().getResourceAsStream("/productlicense/licensing-keystore-hds.p12"),
        new File(tempDir.getRoot(), "hds.p12").toPath());
    hdsMockServer.reset();
    setHdsUrl(hdsMockServer.getHttpUrl());
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
