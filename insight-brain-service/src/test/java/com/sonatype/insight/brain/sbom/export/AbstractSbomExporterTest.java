/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.export;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.report.FileLifecycleReportPersistenceService;
import com.sonatype.insight.brain.sbom.datastore.FileSbomPersistenceService;
import com.sonatype.insight.brain.sbom.export.SbomExportParams.ExportOption;
import com.sonatype.insight.brain.sbom.export.SbomExportParams.ExportSpecification;
import com.sonatype.insight.brain.sbom.license.ThirdPartyComponentLicenseResolutionService;
import com.sonatype.insight.brain.scan.datastore.FileScanPersistenceService;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.thirdparty.ThirdPartyPersistenceService;
import com.sonatype.insight.scan.file.SbomFormat;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import static org.mockito.Mockito.when;

abstract class AbstractSbomExporterTest
    extends AbstractComponentTest
{
  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  @Inject
  protected ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  @Inject
  protected MigrationTrackerDAO migrationTrackerDAO;

  @Inject
  protected ThirdPartyComponentLicenseResolutionService licenseResolutionService;

  @Inject
  protected TestProductLicense productLicense;

  @Inject
  protected ThirdPartyFileDAO thirdPartyFileDAO;

  @Inject
  protected ThirdPartyScanDAO thirdPartyScanDAO;

  @Inject
  private FileCleaner fileCleaner;

  @Inject
  private FileLifecycleReportPersistenceService lifecycleReportPersistenceService;

  @Inject
  private FileScanPersistenceService scanPersistenceService;

  @Mock
  protected InsightWork mockSbomPersistenceInsightWork;

  protected String appId = "APP_ID";

  protected File getGZippedSbom(String fileName) throws Exception {
    URL resource = getClass().getResource("/" + getClass().getSimpleName() + "/" + fileName);
    File tmpGzippedFile = tempDir.newFile(UUID.randomUUID() + "-bom.gz");
    try (GZIPOutputStream gzipStream = new GZIPOutputStream(Files.newOutputStream(tmpGzippedFile.toPath()))) {
      FileUtils.copyFile(new File(resource.toURI()), gzipStream);
    }
    return tmpGzippedFile;
  }

  protected String readFileToString(String fileName) throws Exception {
    URL resource = getClass().getResource("/" + getClass().getSimpleName() + "/" + fileName);
    return FileUtils.readFileToString(new File(resource.toURI()), StandardCharsets.UTF_8);
  }

  protected File mockSbomFileForApp(String applicationId, File sbomFile) {
    when(mockSbomPersistenceInsightWork.getSbomDir(applicationId)).thenReturn(sbomFile.getParentFile());
    return sbomFile;
  }

  protected SbomExportParams withExportParams(
      ThirdPartySbomMetadata sbomMetadata,
      ExportSpecification specification,
      SbomFormat targetFormat)
  {
    return SbomExportParams.newSbomExporterParams(sbomMetadata)
        .withExportOptions(ExportOption.NO_VULNERABILITIES)
        .withExportSpecification(specification)
        .withTargetFormat(targetFormat);
  }

  protected ThirdPartySbomMetadata insertTestData(
      String appId,
      String sbomVersion,
      String testBomFile,
      ThirdPartyFile thirdPartyFile)
  {
    ThirdPartySbomMetadata dbRecord = tempEntity.createSbomMetadata(appId, sbomVersion,
        thirdPartyFile, ACTIVE);
    dbRecord.setFilename(testBomFile);
    thirdPartySbomMetadataDAO.update(dbRecord);
    return dbRecord;
  }

  protected ThirdPartyPersistenceService buildThirdPartyPersistenceService() {
    return new ThirdPartyPersistenceService(
        thirdPartySbomMetadataDAO,
        thirdPartyFileDAO,
        thirdPartyScanDAO,
        new FileSbomPersistenceService(mockSbomPersistenceInsightWork, fileCleaner),
        lifecycleReportPersistenceService,
        scanPersistenceService);
  }
}
