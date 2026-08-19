/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.export;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.api.v2.service.ApiReportDataServiceV2;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
import com.sonatype.insight.brain.report.pdf.PdfData;
import com.sonatype.insight.brain.sbom.license.ThirdPartyComponentLicenseResolutionService;
import com.sonatype.insight.brain.sbom.spdx.ParsedSpdxResult;
import com.sonatype.insight.brain.sbom.spdx.Spdx3VersionHandler;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.thirdparty.ThirdPartyPersistenceService;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.scan.file.SbomFormat;
import com.sonatype.insight.scan.file.SbomProcessingException;

import org.cyclonedx.model.Bom;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

@Named
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class Spdx3ToPdfExporter
    extends Spdx3ToCycloneDxExporter
{
  @Inject
  public Spdx3ToPdfExporter(
      final MultiLicenseDAO multiLicenseDAO,
      final ThirdPartyFileDAO thirdPartyFileDAO,
      final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO,
      final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO,
      final ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO,
      final ThirdPartyScanDAO thirdPartyScanDAO,
      final ApplicationDAO applicationDAO,
      final ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVulnerabilityExploitabilityExchangeDAO,
      final MigrationTrackerDAO migrationTrackerDAO,
      final BaseUrl baseUrl,
      final IdUtils idUtils,
      final VersionService versionService,
      final ApiReportDataServiceV2 apiReportDataServiceV2,
      final ThirdPartyComponentLicenseResolutionService licenseResolutionService,
      final ThirdPartyPersistenceService thirdPartyPersistenceService,
      final Spdx3VersionHandler spdx3VersionHandler)
  {
    super(
        multiLicenseDAO,
        thirdPartyFileDAO,
        thirdPartyFileCoordinateDAO,
        thirdPartyCoordinateSecurityDAO,
        thirdPartyCoordinateLicenseDAO,
        thirdPartyScanDAO,
        applicationDAO,
        thirdPartyVulnerabilityExploitabilityExchangeDAO,
        migrationTrackerDAO,
        baseUrl,
        idUtils,
        versionService,
        apiReportDataServiceV2,
        licenseResolutionService,
        thirdPartyPersistenceService,
        spdx3VersionHandler);
  }

  @Override
  public String export() {
    throw new UnsupportedOperationException("SBOM export not supported for Pdf exporter");
  }

  @Override
  public PdfData exportPdf() {
    try (InputStream gis = getOriginalSbomContent()) {
      String content = new String(gis.readAllBytes(), StandardCharsets.UTF_8);
      ParsedSpdxResult parsed = spdx3VersionHandler.parse(content, SbomFormat.JSON);

      Bom bom = buildCycloneDxBom(parsed);
      return convertToPdfData(mergeCurrentDatabaseState(bom));
    }
    catch (IOException | SbomProcessingException e) {
      throw new SbomExportException(
          String.format("Internal error exporting SPDX 3.0 to PDF for application %s, version %s",
              exportParams.sbomMetadata.getApplicationId(), exportParams.sbomMetadata.getSbomVersion()),
          e);
    }
  }
}
