/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.scan.model.ItemContentType;

@Named
public class ThirdPartyResultHandlerFactory
{
  protected final ThirdPartyFileDAO thirdPartyFileDAO;

  protected final DuplicateAwareThirdPartyFileCoordinatePersister fileCoordinatePersister;

  protected final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO;

  protected final ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO;

  protected final MultiLicenseDAO multiLicenseDAO;

  protected final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  protected final ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVexDAO;

  protected final TelemetryUtils telemetryUtils;

  protected final TelemetrySender telemetrySender;

  protected final ProductLicense productLicense;

  @Inject
  public ThirdPartyResultHandlerFactory(
      final ThirdPartyFileDAO thirdPartyFileDAO,
      final DuplicateAwareThirdPartyFileCoordinatePersister fileCoordinatePersister,
      final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO,
      final ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO,
      final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      final MultiLicenseDAO multiLicenseDAO,
      final ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVexDAO,
      final TelemetryUtils telemetryUtils,
      final TelemetrySender telemetrySender,
      final ProductLicense productLicense)
  {
    this.thirdPartyFileDAO = thirdPartyFileDAO;
    this.fileCoordinatePersister = fileCoordinatePersister;
    this.thirdPartyCoordinateSecurityDAO = thirdPartyCoordinateSecurityDAO;
    this.thirdPartyCoordinateLicenseDAO = thirdPartyCoordinateLicenseDAO;
    this.thirdPartySbomMetadataDAO = thirdPartySbomMetadataDAO;
    this.multiLicenseDAO = multiLicenseDAO;
    this.thirdPartyVexDAO = thirdPartyVexDAO;
    this.telemetryUtils = telemetryUtils;
    this.telemetrySender = telemetrySender;
    this.productLicense = productLicense;
  }

  public ThirdPartyScanResultHandler newHandler(
      ItemContentType itemContentType,
      ThirdPartyScanContext thirdPartyScanContext)
  {
    if (ItemContentType.CLAIR_SCANNER.equals(itemContentType)) {
      return new ClairScannerResultHandler(thirdPartyFileDAO, fileCoordinatePersister,
          thirdPartyCoordinateSecurityDAO);
    }
    else if (ItemContentType.SBOM.equals(itemContentType)) {
      return new SbomResultHandler(thirdPartyFileDAO, fileCoordinatePersister, thirdPartyCoordinateSecurityDAO,
          thirdPartyCoordinateLicenseDAO, thirdPartySbomMetadataDAO, multiLicenseDAO, thirdPartyVexDAO, telemetryUtils,
          telemetrySender, thirdPartyScanContext);
    }
    else if (ItemContentType.SPDX.equals(itemContentType)) {
      return new SpdxResultHandler(thirdPartyFileDAO, fileCoordinatePersister, thirdPartyCoordinateSecurityDAO,
          thirdPartyCoordinateLicenseDAO, thirdPartySbomMetadataDAO, multiLicenseDAO, thirdPartyVexDAO, telemetryUtils,
          telemetrySender, thirdPartyScanContext);
    }
    else if (ItemContentType.CONTAINER_URI.equals(itemContentType)
        || ItemContentType.CONTAINER_URI_SONATYPE.equals(itemContentType))
    {

      if (ItemContentType.CONTAINER_URI_SONATYPE.equals(itemContentType)
          && SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.isEnabled()
          && !productLicense.hasFeature(LicensedFeature.CONTAINER_IMAGES_EVALUATION))
      {
        throw new InvalidLicenseException(
            "Your IQ Server license does not include the Container Images Evaluation feature "
                + "required for Sonatype container scans.");
      }

      thirdPartyScanContext.setContainerItemContentType(itemContentType);

      return new ContainerResultHandler(thirdPartyFileDAO, fileCoordinatePersister, thirdPartyCoordinateSecurityDAO,
          thirdPartyCoordinateLicenseDAO, thirdPartySbomMetadataDAO, multiLicenseDAO, thirdPartyVexDAO, telemetryUtils,
          telemetrySender, thirdPartyScanContext, productLicense);
    }
    throw new IllegalArgumentException("unsupported third party content type " + itemContentType);
  }
}
