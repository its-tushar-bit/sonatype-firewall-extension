/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.utils;

import java.io.InputStream;
import java.io.UncheckedIOException;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiSbomResource;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigService;
import com.sonatype.insight.brain.scan.ScanResult;
import com.sonatype.insight.brain.scan.Scanner;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.scan.application.ScannerDriver;
import com.sonatype.insight.scan.file.SbomFormat;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.scan.model.ItemContentType;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

public class SbomMetadataUtilsTest
    extends AbstractComponentTest
{
  @Mock
  private ThirdPartySbomMetadataDAO mockThirdPartySbomMetadataDAO;

  @Mock
  private ProductLicense mockProductLicense;

  @Mock
  private ProprietaryConfigService mockProprietaryConfigService;

  @Inject
  private Scanner scanner;

  private SbomMetadataUtils sbomMetadataUtils;

  @Before
  public void before() {
    sbomMetadataUtils =
        new SbomMetadataUtils(mockThirdPartySbomMetadataDAO, mockProductLicense, mockProprietaryConfigService, scanner);
  }

  @Test
  public void testHasMaxSbomLimitBeenReached_Less() {
    when(mockThirdPartySbomMetadataDAO.getSbomCount()).thenReturn(1L);
    when(mockProductLicense.getMaxSboms()).thenReturn(2);

    assertThat(sbomMetadataUtils.hasMaxSbomLimitBeenReached()).isFalse();
  }

  @Test
  public void testHasMaxSbomLimitBeenReached_Equal() {
    when(mockThirdPartySbomMetadataDAO.getSbomCount()).thenReturn(2L);
    when(mockProductLicense.getMaxSboms()).thenReturn(2);

    assertThat(sbomMetadataUtils.hasMaxSbomLimitBeenReached()).isTrue();
  }

  @Test
  public void testHasMaxSbomLimitBeenReached_More() {
    when(mockThirdPartySbomMetadataDAO.getSbomCount()).thenReturn(3L);
    when(mockProductLicense.getMaxSboms()).thenReturn(2);

    assertThat(sbomMetadataUtils.hasMaxSbomLimitBeenReached()).isTrue();
  }

  @Test
  public void testHasMaxSbomLimitBeenReached_NullSbomLimit() {
    when(mockThirdPartySbomMetadataDAO.getSbomCount()).thenReturn(2L);
    when(mockProductLicense.getMaxSboms()).thenReturn(null);

    assertThat(sbomMetadataUtils.hasMaxSbomLimitBeenReached()).isTrue();
  }

  @Test
  public void testCreateSbomImportTicket() {
    assertThat(sbomMetadataUtils.createSbomImportTicket("appId").statusUrl).startsWith(
        String.format("%s%s/%s/status", PublicApiPaths.SBOM_RESOURCE_PATH, ApiSbomResource.SBOMS_APPLICATIONS_PATH,
            "appId"));
  }

  @Test
  public void testDetermineItemContentType_SPDX() {
    assertThat(sbomMetadataUtils.determineItemContentType("SPDX")).isEqualTo(ItemContentType.SPDX);
  }

  @Test
  public void testDetermineItemContentType_CycloneDx() {
    assertThat(sbomMetadataUtils.determineItemContentType("CycloneDx")).isEqualTo(ItemContentType.SBOM);
  }

  @Test
  public void testDetermineItemContentType_Other() {
    assertThat(sbomMetadataUtils.determineItemContentType("blah")).isNull();
  }

  @Test
  public void testDetermineItemContentType_Null() {
    assertThat(sbomMetadataUtils.determineItemContentType(null)).isNull();
  }

  @Test
  public void testHasSbomMetadata_True() {
    when(mockThirdPartySbomMetadataDAO.hasSbomMetadata("scanId")).thenReturn(true);

    assertThat(sbomMetadataUtils.hasSbomMetadata("scanId")).isTrue();
  }

  @Test
  public void testHasSbomMetadata_False() {
    when(mockThirdPartySbomMetadataDAO.hasSbomMetadata("scanId")).thenReturn(false);

    assertThat(sbomMetadataUtils.hasSbomMetadata("scanId")).isFalse();
  }

  @Test
  public void testScanSbomContent_Valid() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    try (var sbomStream = getSbomStream("valid-cyclonedx-bom.xml")) {
      ScanResult scanResult =
          sbomMetadataUtils.scanSbomInputStream(app, sbomStream, SbomFormat.XML,
              ItemContentType.SBOM,
              ScannerDriver.SBOM_API);

      assertThat(scanResult.getClientScanType()).isEqualTo(ClientScanType.SONATYPE_THIRD_PARTY);
      assertThat(scanResult.getScanEntity()).isNotNull();
    }
  }

  @Test
  public void testScanSbomContent_Invalid() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    // Create a stream that will cause an IOException when sbomMetadataUtils tries to read it
    var sbomStream = getSbomStream("valid-cyclonedx-bom.xml");
    sbomStream.close();

    assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> {
      sbomMetadataUtils.scanSbomInputStream(app, sbomStream, SbomFormat.XML, ItemContentType.SBOM,
          ScannerDriver.SBOM_API);
    }).withMessage("unable to read supplied sbom");
  }

  private InputStream getSbomStream(final String fileName) {
    return getClass().getResourceAsStream("/SbomMetadataUtilsTest/" + fileName);
  }
}
