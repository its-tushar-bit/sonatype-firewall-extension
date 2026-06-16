/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.sbom.export.SbomExportParams.ExportSpecification;
import com.sonatype.insight.brain.sbom.utils.SbomDetectionResult;
import com.sonatype.insight.brain.sbom.utils.SbomSummary;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.utils.ExistingFilesHelper;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;

/**
 * Tests for the from-scan paths of {@link ThirdPartyPersistenceService}: {@code saveSbomManagerSbomFromScan} and
 * {@code saveSbomManagerBinaryFromScan}. Tests for the user-upload path ({@code saveSbomManagerSbomOrBinary}) live in
 * {@code PendingSbomMetadataCleanerTest} and {@code SbomImportServiceTest}.
 */
@Category(SlowTest.class)
@ContextConfiguration(classes = ThirdPartyPersistenceServiceTest.ExistingFilesHelperTestConfig.class)
public class ThirdPartyPersistenceServiceTest
    extends AbstractComponentTest
{
  @TestConfiguration
  static class ExistingFilesHelperTestConfig
  {
    @Bean
    ExistingFilesHelper existingFilesHelper() {
      return new ExistingFilesHelper();
    }
  }

  @Inject
  private ThirdPartyPersistenceService thirdPartyPersistenceService;

  // A minimal CycloneDX 1.1 SBOM – enough for SbomFileDetector to parse it
  private static final String MINIMAL_CDX_SBOM =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
          "<bom xmlns=\"http://cyclonedx.org/schema/bom/1.1\" version=\"1\">\n" +
          "  <components/>\n" +
          "</bom>";

  // A non-SBOM binary payload
  private static final String BINARY_CONTENT = "binary content that is not an sbom";

  // ---------------------------------------------------------------------------
  // saveSbomManagerSbomFromScan – SBOM path
  // ---------------------------------------------------------------------------

  @Test
  public void saveSbomManagerSbomFromScan_usesUserPreferredVersion_whenProvided() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    SbomDetectionResult detection = sbomDetectionResult(true);

    ImmutablePair<ThirdPartySbomMetadata, ?> result = thirdPartyPersistenceService.saveSbomManagerSbomFromScan(
        MINIMAL_CDX_SBOM,
        "scan.xml",
        application.getId(),
        "user.requested.1.2.3",
        detection);

    assertThat(result.getLeft().getSbomVersion()).isEqualTo("user.requested.1.2.3");
  }

  @Test
  public void saveSbomManagerSbomFromScan_appendsTimestampSuffix_onUniqueCollision() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    SbomDetectionResult detection = sbomDetectionResult(true);

    // First save succeeds with user-preferred version
    thirdPartyPersistenceService.saveSbomManagerSbomFromScan(
        MINIMAL_CDX_SBOM,
        "scan.xml",
        application.getId(),
        "user.requested.1.2.3",
        detection);

    // Second save with the same version must produce a different (collision-resolved) version
    ImmutablePair<ThirdPartySbomMetadata, ?> second = thirdPartyPersistenceService.saveSbomManagerSbomFromScan(
        MINIMAL_CDX_SBOM,
        "scan2.xml",
        application.getId(),
        "user.requested.1.2.3",
        detection);

    // trySaveInLoop calls getNewHopefullyUniqueVersion which is preferredVersion + "_" + yyyyMMddHHmmssSSS
    assertThat(second.getLeft().getSbomVersion())
        .matches("user\\.requested\\.1\\.2\\.3_\\d{17}");
  }

  @Test
  public void saveSbomManagerSbomFromScan_acceptsNull_fallsBackToDetectionThenTimestamp() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    // detectionResult has no applicationVersion in summary AND userPreferredVersion is null
    // → must fall back to timestamp
    SbomDetectionResult detection = sbomDetectionResult(true);
    detection.summary.applicationVersion = null;

    ImmutablePair<ThirdPartySbomMetadata, ?> result = thirdPartyPersistenceService.saveSbomManagerSbomFromScan(
        MINIMAL_CDX_SBOM,
        "scan.xml",
        application.getId(),
        null,
        detection);

    // getTimestampForVersion produces exactly 17 decimal digits: yyyyMMddHHmmssSSS
    assertThat(result.getLeft().getSbomVersion()).matches("\\d{17}");
  }

  // ---------------------------------------------------------------------------
  // saveSbomManagerBinaryFromScan – binary path
  // ---------------------------------------------------------------------------

  @Test
  public void saveSbomManagerBinaryFromScan_propagatesUserPreferredVersion() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    SbomDetectionResult detection = sbomDetectionResult(false);

    ImmutablePair<ThirdPartySbomMetadata, ?> result = thirdPartyPersistenceService.saveSbomManagerBinaryFromScan(
        BINARY_CONTENT,
        "/some/path/binary.zip",
        application.getId(),
        "user.requested.1.2.3",
        detection);

    assertThat(result.getLeft().getSbomVersion()).isEqualTo("user.requested.1.2.3");
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  /**
   * Build a minimal {@link SbomDetectionResult} with {@code isSbom} set as requested and a non-blank
   * {@code summary.applicationVersion} so that tests wishing to override with a user-preferred version can
   * confirm the override wins over detection.
   */
  private static SbomDetectionResult sbomDetectionResult(boolean isSbom) throws IOException {
    SbomDetectionResult detection = new SbomDetectionResult();
    detection.isSbom = isSbom;
    detection.isValid = true;
    detection.mimeType = MediaType.APPLICATION_XML;
    SbomSummary summary = new SbomSummary();
    summary.applicationName = "test-app";
    summary.applicationVersion = "detected-version-1.0";
    summary.specification = SbomSpecification.CYCLONEDX.toString();
    summary.version = ExportSpecification.CYCLONEDX_16.getVersion();
    summary.format = detection.mimeType;
    detection.summary = summary;
    return detection;
  }
}
