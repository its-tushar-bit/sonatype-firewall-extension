/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.export;

import com.sonatype.insight.brain.api.v2.service.ApiReportDataServiceV2;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.zip.GZIPInputStream;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.report.pdf.PdfData;
import com.sonatype.insight.brain.sbom.utils.SbomCycloneDxUtils;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightWork;
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
      final InsightWork insightWork,
      final MultiLicenseDAO multiLicenseDAO,
      final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO,
      final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO,
      final ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO,
      final ThirdPartyScanDAO thirdPartyScanDAO,
      final ApplicationDAO applicationDAO,
      final ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVulnerabilityExploitabilityExchangeDAO,
      final BaseUrl baseUrl,
      final IdUtils idUtils,
      final VersionService versionService,
      final ApiReportDataServiceV2 apiReportDataServiceV2)
  {

    super(insightWork, multiLicenseDAO, thirdPartyFileCoordinateDAO, thirdPartyCoordinateSecurityDAO,
        thirdPartyCoordinateLicenseDAO, thirdPartyScanDAO, applicationDAO,
        thirdPartyVulnerabilityExploitabilityExchangeDAO, baseUrl, idUtils,
        versionService, apiReportDataServiceV2);
  }

  @Override
  public String export() {
    try (InputStream gis = new GZIPInputStream(Files.newInputStream(getOriginalSbomFile().toPath()))) {
      Bom bom = SbomCycloneDxUtils.parseContentStreamNoValidation(gis);
      // This is a version 1.1 vulnerabilities extensions
      // (https://github.com/CycloneDX/specification/blob/1.6/schema/ext/vulnerability-1.0.xsd)
      // related fix that is not properly handled by the cyclonedx-java library when converting to newer versions.
      bom.getComponents().stream()
          .filter(c -> c.getExtensions() != null && c.getExtensions().containsKey("vulnerabilities")).forEach(c -> {
            c.getExtensions().remove("vulnerabilities");
          });
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
