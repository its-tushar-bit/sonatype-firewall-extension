/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.export;

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
      final ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVulnerabilityExploitabilityExchangeDAO,
      final BaseUrl baseUrl,
      final IdUtils idUtils,
      final VersionService versionService)
  {
    super(insightWork, multiLicenseDAO, thirdPartyFileCoordinateDAO, thirdPartyCoordinateSecurityDAO,
        thirdPartyCoordinateLicenseDAO, thirdPartyVulnerabilityExploitabilityExchangeDAO, baseUrl, idUtils,
        versionService);
  }

  @Override
  public String export() {
    try (InputStream gis = new GZIPInputStream(Files.newInputStream(getOriginalSbomFile().toPath()))) {
      Bom bom = SbomCycloneDxUtils.parseContentStreamNoValidation(gis);
      return generateTargetSbomString(mergeCurrentDatabaseState(bom));
    }
    catch (IOException | ParseException | GeneratorException e) {
      throw new SbomExportException(
          String.format("Internal error reading from the original SBOM file for application %s, version %s",
              exportParams.sbomMetadata.getApplicationId(), exportParams.sbomMetadata.getSbomVersion()), e);
    }
  }
}
