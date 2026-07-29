/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Optional;

import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.sbom.license.ThirdPartyComponentLicenseResolutionService;
import com.sonatype.insight.brain.sbom.utils.SbomCycloneDxUtils;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.thirdparty.ThirdPartyPersistenceService;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.scan.file.SbomFormat;

import org.cyclonedx.Version;
import org.cyclonedx.exception.GeneratorException;
import org.cyclonedx.generators.BomGeneratorFactory;
import org.cyclonedx.model.Bom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdx.jacksonstore.MultiFormatStore;
import org.spdx.jacksonstore.MultiFormatStore.Format;
import org.spdx.jacksonstore.MultiFormatStore.Verbose;
import org.spdx.library.model.v2.SpdxDocument;

public abstract class AbstractSbomExporter
    implements SbomExporter
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  protected final ThirdPartyFileDAO thirdPartyFileDAO;

  protected final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO;

  protected final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO;

  protected final ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO;

  protected final BaseUrl baseUrl;

  protected final IdUtils idUtils;

  protected final VersionService versionService;

  protected final ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVulnerabilityExploitabilityExchangeDAO;

  protected final ThirdPartyComponentLicenseResolutionService thirdPartyLicenseResolver;

  protected final ThirdPartyPersistenceService thirdPartyPersistenceService;

  protected SbomExportParams exportParams;

  protected static final DateTimeFormatterBuilder DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
      .appendPattern("yyyy-MM-dd")
      .appendLiteral('T')
      .appendPattern("HH:mm:ss")
      .appendLiteral('Z');

  protected AbstractSbomExporter(
      final ThirdPartyFileDAO thirdPartyFileDAO,
      final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO,
      final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO,
      final ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO,
      final ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVulnerabilityExploitabilityExchangeDAO,
      final BaseUrl baseUrl,
      final IdUtils idUtils,
      final VersionService versionService,
      final ThirdPartyComponentLicenseResolutionService thirdPartyLicenseResolver,
      final ThirdPartyPersistenceService thirdPartyPersistenceService)
  {
    this.thirdPartyFileDAO = thirdPartyFileDAO;
    this.thirdPartyFileCoordinateDAO = thirdPartyFileCoordinateDAO;
    this.thirdPartyCoordinateSecurityDAO = thirdPartyCoordinateSecurityDAO;
    this.thirdPartyCoordinateLicenseDAO = thirdPartyCoordinateLicenseDAO;
    this.thirdPartyVulnerabilityExploitabilityExchangeDAO = thirdPartyVulnerabilityExploitabilityExchangeDAO;
    this.baseUrl = baseUrl;
    this.idUtils = idUtils;
    this.versionService = versionService;
    this.thirdPartyLicenseResolver = thirdPartyLicenseResolver;
    this.thirdPartyPersistenceService = thirdPartyPersistenceService;
  }

  protected InputStream getOriginalSbomContent() throws IOException {
    return thirdPartyPersistenceService.getSbomContentsInputStream(exportParams.sbomMetadata);
  }

  void setExportParams(final SbomExportParams exportParams) {
    this.exportParams = exportParams;
  }

  protected String generateTargetSbomString(Bom bom) throws GeneratorException {
    String versionStr = exportParams.exportSpecification.getVersion();
    Optional<Version> cycloneDxEnumVersion = SbomCycloneDxUtils.getVersionFromString(versionStr);
    if (cycloneDxEnumVersion.isPresent()) {
      if (exportParams.targetFormat.equals(SbomFormat.XML)) {
        // Use a schema-ordered generator to work around a cyclonedx-core-java bug where the
        // License @JsonPropertyOrder emits <licensing> before <url>/<text>, violating the XSD.
        return new CycloneDxSchemaOrderedXmlGenerator(bom, cycloneDxEnumVersion.get()).toXmlString();
      }
      else if (exportParams.targetFormat.equals(SbomFormat.JSON)) {
        return BomGeneratorFactory.createJson(cycloneDxEnumVersion.get(), bom).toJsonString();
      }
      else {
        throw new SbomExportException("Unsupported target format: " + exportParams.targetFormat);
      }
    }
    else {
      throw new SbomExportException("Unsupported target version: " + versionStr);
    }
  }

  protected String generateTargetSbomString(SpdxDocument document) {
    Format spdxFormat = SbomFormat.JSON.equals(exportParams.targetFormat) ? Format.JSON_PRETTY : Format.XML;
    try (MultiFormatStore multiFormatStore =
        new MultiFormatStore(document.getModelStore(), spdxFormat, Verbose.STANDARD);
        ByteArrayOutputStream out = new ByteArrayOutputStream())
    {
      multiFormatStore.serialize(out);
      return out.toString(StandardCharsets.UTF_8);
    }
    catch (Exception e) {
      throw new SbomExportException("Internal error generating the target SBOM", e);
    }
  }

  protected String getBaseUrl() {
    String iqBaseUrl = null;
    try {
      iqBaseUrl = this.baseUrl.get();
    }
    catch (IllegalStateException e) {
      log.warn("SBOM Manager base URL is not configured", e);
    }
    return iqBaseUrl;
  }

  protected String getBillOfMaterialsPath() {
    return getBaseUrl() + UserInterfaceLinksHelper.getSBOMBillOfMaterialPath(
        idUtils.getPublicOwnerId(OwnerType.APPLICATION, exportParams.sbomMetadata.getApplicationId()),
        exportParams.sbomMetadata.getSbomVersion());
  }
}
