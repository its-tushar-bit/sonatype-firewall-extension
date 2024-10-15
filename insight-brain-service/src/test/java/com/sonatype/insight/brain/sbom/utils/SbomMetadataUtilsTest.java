/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.utils;

import java.io.File;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiSbomResource;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigService;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.sbom.ingestion.SbomRequestIdElements;
import com.sonatype.insight.brain.scan.ScanResult;
import com.sonatype.insight.brain.scan.Scanner;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.thirdparty.SbomScanType;
import com.sonatype.insight.error.exception.BadRequestException;
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

  @Inject
  private ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  @Mock
  private ProductLicense productLicense;

  @Mock
  private ProprietaryConfigService proprietaryConfigService;

  @Inject
  private Scanner scanner;

  private SbomMetadataUtils sbomMetadataUtils;

  @Before
  public void before() {
    sbomMetadataUtils =
        new SbomMetadataUtils(mockThirdPartySbomMetadataDAO, productLicense, proprietaryConfigService, scanner);
  }

  @Test
  public void testHasMaxSbomLimitBeenReached_Less() {
    when(mockThirdPartySbomMetadataDAO.getSbomCount()).thenReturn(1L);
    when(productLicense.getMaxSboms()).thenReturn(2);

    assertThat(sbomMetadataUtils.hasMaxSbomLimitBeenReached()).isFalse();
  }

  @Test
  public void testHasMaxSbomLimitBeenReached_Equal() {
    when(mockThirdPartySbomMetadataDAO.getSbomCount()).thenReturn(2L);
    when(productLicense.getMaxSboms()).thenReturn(2);

    assertThat(sbomMetadataUtils.hasMaxSbomLimitBeenReached()).isTrue();
  }

  @Test
  public void testHasMaxSbomLimitBeenReached_More() {
    when(mockThirdPartySbomMetadataDAO.getSbomCount()).thenReturn(3L);
    when(productLicense.getMaxSboms()).thenReturn(2);

    assertThat(sbomMetadataUtils.hasMaxSbomLimitBeenReached()).isTrue();
  }

  @Test
  public void testHasMaxSbomLimitBeenReached_NullSbomLimit() {
    when(mockThirdPartySbomMetadataDAO.getSbomCount()).thenReturn(2L);
    when(productLicense.getMaxSboms()).thenReturn(null);

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
    File scanDir = tempDir.newFolder();
    ScanResult scanResult =
        sbomMetadataUtils.scanSbomFile(app, getSbomFile("valid-cyclonedx-bom.xml"), scanDir, SbomFormat.XML,
            ItemContentType.SBOM,
            ScannerDriver.SBOM_API);
    assertThat(scanResult.getClientScanType()).isEqualTo(ClientScanType.SONATYPE_THIRD_PARTY);
    assertThat(scanResult.getScanFile()).isNotNull();
  }

  @Test
  public void testScanSbomContent_Invalid() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    File scanDir = tempDir.newFolder();
    assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> {
      sbomMetadataUtils.scanSbomFile(app, new File(""), scanDir, SbomFormat.XML, ItemContentType.SBOM,
          ScannerDriver.SBOM_API);
    }).withMessage("unable to read supplied sbom");
  }

  private File getSbomFile(final String fileName) throws URISyntaxException {
    return new File(getClass().getResource("/SbomMetadataUtilsTest/" + fileName).toURI());
  }

  @Test
  public void testInsertThirdPartySbomMetadataWithRetry() {
    SbomMetadataUtils sbomMetadataUtils =
        new SbomMetadataUtils(thirdPartySbomMetadataDAO, productLicense, proprietaryConfigService, scanner);
    Organization organization = tempEntity.newOrganization("Testing Organization");
    Application application = tempEntity.newApplication("Testing Application", "TESTING", organization.getId());
    final ThirdPartySbomMetadata thirdPartySbomMetadata =
        tempEntity.newThirdPartySbomMetadata(application.getId(), "PENDING", "test-file.xml");
    final ThirdPartySbomMetadata duplicateThirdPartySbomMetadata = new ThirdPartySbomMetadata();
    duplicateThirdPartySbomMetadata.setApplicationId(thirdPartySbomMetadata.getApplicationId());
    duplicateThirdPartySbomMetadata.setSbomVersion(thirdPartySbomMetadata.getSbomVersion());
    duplicateThirdPartySbomMetadata.setThirdPartyFileId(thirdPartySbomMetadata.getThirdPartyFileId());
    duplicateThirdPartySbomMetadata.setMetadataJson(thirdPartySbomMetadata.getMetadataJson());
    duplicateThirdPartySbomMetadata.setCreatedAt(thirdPartySbomMetadata.getCreatedAt());
    duplicateThirdPartySbomMetadata.setSerialNumber(thirdPartySbomMetadata.getSerialNumber());
    duplicateThirdPartySbomMetadata.setSpec(thirdPartySbomMetadata.getSpec());
    duplicateThirdPartySbomMetadata.setSpecFormat(thirdPartySbomMetadata.getSpecFormat());
    duplicateThirdPartySbomMetadata.setSpecVersion(thirdPartySbomMetadata.getSpecVersion());
    duplicateThirdPartySbomMetadata.setFilename(thirdPartySbomMetadata.getFilename());
    duplicateThirdPartySbomMetadata.setStatus(thirdPartySbomMetadata.getStatus());
    duplicateThirdPartySbomMetadata.setScanType(thirdPartySbomMetadata.getScanType());
    sbomMetadataUtils.insertThirdPartySbomMetadataWithRetry(duplicateThirdPartySbomMetadata);

    List<ThirdPartySbomMetadata> thirdPartySbomMetadataList =
        thirdPartySbomMetadataDAO.getByApplicationId(application.getId());
    List<String> sbomVersions =
        thirdPartySbomMetadataList.stream().map(ThirdPartySbomMetadata::getSbomVersion).toList();

    assertThat(thirdPartySbomMetadataList).hasSize(2);
    assertThat(sbomVersions).containsExactlyInAnyOrder(thirdPartySbomMetadata.getSbomVersion(),
        duplicateThirdPartySbomMetadata.getSbomVersion());
  }

  @Test
  public void testDecodeRequestId_emptyRequestId() {
    assertThat(sbomMetadataUtils.decodeRequestId("")).isNull();
  }

  @Test
  public void testDecodeRequestId_invalidRequestId() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      sbomMetadataUtils.decodeRequestId("invalid");
    }).withMessage("The provided requestId invalid is not valid.");
  }

  @Test
  public void testDecodeRequestId_validRequestId_SBOM_CDX() {
    String filenameUUID = UUID.randomUUID().toString().replace("-", "");
    String originalFilename = "test-bom.json";

    String requestId = Base64.getEncoder().encodeToString(
        String.format("%s-%s-%s-%s-%s", SbomScanType.SBOM.name(), "application/json", SbomSpecification.CYCLONEDX,
            filenameUUID, originalFilename).getBytes(StandardCharsets.UTF_8));
    SbomRequestIdElements requestIdElements = sbomMetadataUtils.decodeRequestId(requestId);

    assertThat(requestIdElements).isNotNull();
    assertThat(requestIdElements.getScanType()).isEqualTo(SbomScanType.SBOM);
    assertThat(requestIdElements.getSbomFormat()).isEqualTo(SbomFormat.JSON);
    assertThat(requestIdElements.getFilename()).isEqualTo(String.format("%s-%s", filenameUUID, originalFilename));
    assertThat(requestIdElements.getContentType()).isEqualTo(ItemContentType.SBOM);
  }

  @Test
  public void testDecodeRequestId_validRequestId_SBOM_SPDX() {
    String filenameUUID = UUID.randomUUID().toString().replace("-", "");
    String originalFilename = "test-bom.json";

    String requestId = Base64.getEncoder().encodeToString(
        String.format("%s-%s-%s-%s-%s", SbomScanType.SBOM.name(), "application/xml", SbomSpecification.SPDX,
            filenameUUID, originalFilename).getBytes(StandardCharsets.UTF_8));
    SbomRequestIdElements requestIdElements = sbomMetadataUtils.decodeRequestId(requestId);

    assertThat(requestIdElements).isNotNull();
    assertThat(requestIdElements.getScanType()).isEqualTo(SbomScanType.SBOM);
    assertThat(requestIdElements.getSbomFormat()).isEqualTo(SbomFormat.XML);
    assertThat(requestIdElements.getFilename()).isEqualTo(String.format("%s-%s", filenameUUID, originalFilename));
    assertThat(requestIdElements.getContentType()).isEqualTo(ItemContentType.SPDX);
  }

  @Test
  public void testDecodeRequestId_validRequestId_BINARY() {
    String filenameUUID = UUID.randomUUID().toString().replace("-", "");
    String originalFilename = "test.jar";

    String requestId =
        Base64.getEncoder()
            .encodeToString(String.format("%s-%s-%s", SbomScanType.BINARY.name(), filenameUUID, originalFilename)
                .getBytes(StandardCharsets.UTF_8));
    SbomRequestIdElements requestIdElements = sbomMetadataUtils.decodeRequestId(requestId);

    assertThat(requestIdElements).isNotNull();
    assertThat(requestIdElements.getScanType()).isEqualTo(SbomScanType.BINARY);
    assertThat(requestIdElements.getSbomFormat()).isNull();
    assertThat(requestIdElements.getFilename()).isEqualTo(String.format("%s-%s", filenameUUID, originalFilename));
    assertThat(requestIdElements.getContentType()).isNull();
  }

  @Test
  public void testDecodeRequestId_invalidSbomScanType() {
    String filenameUUID = UUID.randomUUID().toString().replace("-", "");
    String originalFilename = "test.jar";
    String requestId = Base64.getEncoder()
        .encodeToString(String.format("%s-%s-%s", "INVALID_REQUEST_TYPE", filenameUUID, originalFilename)
            .getBytes(StandardCharsets.UTF_8));
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      sbomMetadataUtils.decodeRequestId(requestId);
    }).withMessage("The provided requestId " + requestId + " is not valid.");
  }
}
