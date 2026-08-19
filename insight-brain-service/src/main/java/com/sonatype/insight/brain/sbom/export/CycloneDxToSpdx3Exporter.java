/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.export;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
import com.sonatype.insight.brain.sbom.license.ThirdPartyComponentLicenseResolutionService;
import com.sonatype.insight.brain.sbom.spdx.Spdx3VersionHandler;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.thirdparty.ThirdPartyPersistenceService;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.brain.version.VersionService;

@Named
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CycloneDxToSpdx3Exporter
    extends SpdxToSpdx3Exporter
{
  @Inject
  protected CycloneDxToSpdx3Exporter(
      final ThirdPartyFileDAO thirdPartyFileDAO,
      final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO,
      final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO,
      final ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO,
      final ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVulnerabilityExploitabilityExchangeDAO,
      final BaseUrl baseUrl,
      final IdUtils idUtils,
      final VersionService versionService,
      final ThirdPartyComponentLicenseResolutionService thirdPartyLicenseResolver,
      final ThirdPartyPersistenceService thirdPartyPersistenceService,
      final Spdx3VersionHandler spdx3VersionHandler)
  {
    super(thirdPartyFileDAO, thirdPartyFileCoordinateDAO, thirdPartyCoordinateSecurityDAO,
        thirdPartyCoordinateLicenseDAO, thirdPartyVulnerabilityExploitabilityExchangeDAO, baseUrl, idUtils,
        versionService, thirdPartyLicenseResolver, thirdPartyPersistenceService, spdx3VersionHandler);
  }
}
