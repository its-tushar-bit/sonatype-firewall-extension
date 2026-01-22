/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.export;

import java.io.IOException;
import java.io.InputStream;
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
import com.sonatype.insight.brain.sbom.utils.SbomCycloneDxUtils;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.thirdparty.ThirdPartyPersistenceService;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.brain.version.VersionService;

import org.cyclonedx.exception.GeneratorException;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;

@Named
public class CycloneDxToCycloneDxExporter
    extends AbstractCycloneDxExporter
{
  @Inject
  public CycloneDxToCycloneDxExporter(
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
      final ThirdPartyPersistenceService thirdPartyPersistenceService)
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
        thirdPartyPersistenceService
    );
  }

  @Override
  public String export() {
    try (InputStream gis = getOriginalSbomContent()) {
      Bom bom = SbomCycloneDxUtils.parseContentStreamNoValidation(gis);
      cleanupLegacyVulnerabilitiesFromBomComponents(bom);
      return generateTargetSbomString(mergeCurrentDatabaseState(bom));
    }
    catch (IOException | ParseException | GeneratorException e) {
      throw new SbomExportException(
          String.format("Internal error reading from the original SBOM file for application %s, version %s",
              exportParams.sbomMetadata.getApplicationId(), exportParams.sbomMetadata.getSbomVersion()), e);
    }
  }

  @Override
  public PdfData exportPdf() {
    throw new UnsupportedOperationException("PDF export not supported for SBOM exporter");
  }
}
