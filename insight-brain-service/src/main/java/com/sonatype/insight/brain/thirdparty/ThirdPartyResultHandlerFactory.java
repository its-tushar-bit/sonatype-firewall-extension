/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.scan.model.ItemContentType;

@Named
public class ThirdPartyResultHandlerFactory
{
  protected final ThirdPartyFileDAO thirdPartyFileDAO;

  protected final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO;

  protected final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO;

  protected final ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO;

  protected final MultiLicenseDAO multiLicenseDAO;

  protected final ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVexDAO;

  protected final TelemetryUtils telemetryUtils;

  protected final TelemetrySender telemetrySender;

  @Inject
  public ThirdPartyResultHandlerFactory(
      final ThirdPartyFileDAO thirdPartyFileDAO,
      final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO,
      final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO,
      final ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO,
      final MultiLicenseDAO multiLicenseDAO,
      final ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVexDAO,
      final TelemetryUtils telemetryUtils,
      final TelemetrySender telemetrySender)
  {
    this.thirdPartyFileDAO = thirdPartyFileDAO;
    this.thirdPartyFileCoordinateDAO = thirdPartyFileCoordinateDAO;
    this.thirdPartyCoordinateSecurityDAO = thirdPartyCoordinateSecurityDAO;
    this.thirdPartyCoordinateLicenseDAO = thirdPartyCoordinateLicenseDAO;
    this.multiLicenseDAO = multiLicenseDAO;
    this.thirdPartyVexDAO = thirdPartyVexDAO;
    this.telemetryUtils = telemetryUtils;
    this.telemetrySender = telemetrySender;
  }

  public ThirdPartyScanResultHandler newHandler(
      ItemContentType itemContentType,
      ThirdPartyScanContext thirdPartyScanContext)
  {
    if (ItemContentType.CLAIR_SCANNER.equals(itemContentType)) {
      return new ClairScannerResultHandler(thirdPartyFileDAO, thirdPartyFileCoordinateDAO,
          thirdPartyCoordinateSecurityDAO);
    }
    else if (ItemContentType.SBOM.equals(itemContentType)) {
      return new SbomResultHandler(thirdPartyFileDAO, thirdPartyFileCoordinateDAO, thirdPartyCoordinateSecurityDAO,
          thirdPartyCoordinateLicenseDAO, multiLicenseDAO, thirdPartyVexDAO, telemetryUtils, telemetrySender,
          thirdPartyScanContext);
    }
    else if (ItemContentType.SPDX.equals(itemContentType)) {
      return new SpdxResultHandler(thirdPartyFileDAO, thirdPartyFileCoordinateDAO, thirdPartyCoordinateSecurityDAO,
          thirdPartyCoordinateLicenseDAO, multiLicenseDAO, thirdPartyVexDAO, telemetryUtils, telemetrySender,
          thirdPartyScanContext);
    }
    else if (ItemContentType.CONTAINER_URI.equals(itemContentType)) {
      return new ContainerResultHandler(thirdPartyFileDAO, thirdPartyFileCoordinateDAO, thirdPartyCoordinateSecurityDAO,
          thirdPartyCoordinateLicenseDAO, multiLicenseDAO, thirdPartyVexDAO, telemetryUtils, telemetrySender,
          thirdPartyScanContext);
    }
    throw new IllegalArgumentException("unsupported third party content type " + itemContentType);
  }
}
