/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.sbom.export;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.brain.version.VersionService;

import org.spdx.library.InvalidSPDXAnalysisException;
import org.spdx.library.SpdxConstants;
import org.spdx.library.model.SpdxCreatorInformation;
import org.spdx.library.model.SpdxDocument;
import org.spdx.library.model.license.SpdxListedLicense;

public abstract class AbstractSpdxExporter extends AbstractCycloneDxExporter
{
  protected static final String INVALID_REF_REGEX = "[^0-9a-zA-Z\\.\\-\\+]";

  protected AbstractSpdxExporter(
      final InsightWork insightWork,
      final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO,
      final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO,
      final ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO,
      final ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVulnerabilityExploitabilityExchangeDAO,
      final BaseUrl baseUrl,
      final IdUtils idUtils,
      final VersionService versionService)
  {
    super(insightWork, thirdPartyFileCoordinateDAO, thirdPartyCoordinateSecurityDAO, thirdPartyCoordinateLicenseDAO,
        thirdPartyVulnerabilityExploitabilityExchangeDAO, baseUrl, idUtils, versionService);
  }

  protected void setMetadata(SpdxDocument newDocument) throws InvalidSPDXAnalysisException {
    newDocument.setSpecVersion("SPDX-" + exportParams.exportSpecification.getVersion());
    newDocument.setName(idUtils.getPublicOwnerId(OwnerType.APPLICATION, exportParams.sbomMetadata.getApplicationId()));
    newDocument.setDataLicense(new SpdxListedLicense(SpdxConstants.SPDX_DATA_LICENSE_ID));

    SpdxCreatorInformation creatorInfo = new SpdxCreatorInformation();
    String date = LocalDateTime.now(ZoneOffset.UTC).format(DATE_TIME_FORMATTER.toFormatter());

    creatorInfo.setCreated(date);
    creatorInfo.getCreators().add("Tool: Sonatype SBOM Manager - " + versionService.getFullVersion());
    //not adding data date (for NDE customers) until is needed.
    newDocument.setCreationInfo(creatorInfo);
  }
}
