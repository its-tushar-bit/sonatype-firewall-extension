/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.export;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
import com.sonatype.insight.brain.model.thirdpartyscans.ResolvedLicenseDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchange;
import com.sonatype.insight.brain.report.pdf.PdfData;
import com.sonatype.insight.brain.sbom.license.ThirdPartyComponentLicenseResolutionService;
import com.sonatype.insight.brain.sbom.spdx.Spdx3VersionHandler;
import com.sonatype.insight.brain.sbom.spdx.SpdxGenerationContext;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.thirdparty.ThirdPartyPersistenceService;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.brain.version.VersionService;

import org.spdx.core.InvalidSPDXAnalysisException;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

@Named
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SpdxToSpdx3Exporter
    extends AbstractSbomExporter
{
  private final Spdx3VersionHandler spdx3VersionHandler;

  @Inject
  protected SpdxToSpdx3Exporter(
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
        versionService, thirdPartyLicenseResolver, thirdPartyPersistenceService);
    this.spdx3VersionHandler = spdx3VersionHandler;
  }

  @Override
  public String export() {
    try {
      String appId = exportParams.sbomMetadata.getApplicationId();
      String sbomVersion = exportParams.sbomMetadata.getSbomVersion();
      String thirdPartyFileId = exportParams.sbomMetadata.getThirdPartyFileId();

      List<ThirdPartyFileCoordinate> components =
          thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFileId);

      List<String> coordinateIds =
          components.stream().map(ThirdPartyFileCoordinate::getId).toList();
      List<ThirdPartyCoordinateSecurity> vulnerabilities =
          thirdPartyCoordinateSecurityDAO.getByFileCoordinateIds(coordinateIds);

      List<ThirdPartyVulnerabilityExploitabilityExchange> vexAnnotations = List.of();
      if (!vulnerabilities.isEmpty()) {
        Collection<String> securityIds =
            vulnerabilities.stream().map(ThirdPartyCoordinateSecurity::getId).toList();
        vexAnnotations =
            thirdPartyVulnerabilityExploitabilityExchangeDAO.getListByCoordinateSecurityIds(securityIds);
      }

      String extendedProfiles = exportParams.sbomMetadata.getExtendedProfileElements();
      String rootComponentRef = exportParams.sbomMetadata.getRootComponentRef();

      Map<String, Set<ResolvedLicenseDTO>> licensesByCoordinateId = new HashMap<>();
      for (ThirdPartyFileCoordinate comp : components) {
        Set<ResolvedLicenseDTO> resolved =
            thirdPartyLicenseResolver.resolveLicenseOverridesOrThirdPartyLicenses(appId, comp);
        if (!resolved.isEmpty()) {
          licensesByCoordinateId.put(comp.getId(), resolved);
        }
      }

      SpdxGenerationContext context = new SpdxGenerationContext(
          components,
          vulnerabilities,
          licensesByCoordinateId,
          vexAnnotations,
          List.of(),
          idUtils.getPublicOwnerId(OwnerType.APPLICATION, appId),
          sbomVersion,
          Spdx3VersionHandler.SPEC_VERSION,
          extendedProfiles,
          getBillOfMaterialsPath(),
          null,
          rootComponentRef);

      byte[] jsonLd = spdx3VersionHandler.generate(context);
      return new String(jsonLd, StandardCharsets.UTF_8);
    }
    catch (InvalidSPDXAnalysisException e) {
      throw new SbomExportException(
          String.format("Internal error generating SPDX 3.0 export for application %s, version %s",
              exportParams.sbomMetadata.getApplicationId(), exportParams.sbomMetadata.getSbomVersion()),
          e);
    }
  }

  @Override
  public PdfData exportPdf() {
    throw new UnsupportedOperationException("PDF export not supported for SPDX 3.0 exporter");
  }
}
