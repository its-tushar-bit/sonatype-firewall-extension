/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.legal;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.legal.dto.ApplicationReportRawDataDTO;
import com.sonatype.insight.brain.legal.dto.LegalApplicationDataDTO;
import com.sonatype.insight.brain.legal.dto.LegalLicenseDataDTO;
import com.sonatype.insight.brain.legal.dto.LegalLicenseMetadataDTO;
import com.sonatype.insight.brain.legal.dto.LegalOrganizationReportDataDTO;
import com.sonatype.insight.brain.legal.dto.LegalReportComponentDTO;
import com.sonatype.insight.brain.legal.dto.LegalReportDataDTO;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.license.dto.model.ComponentLegalCommentDTO;
import com.sonatype.insight.license.dto.model.ComponentLegalFileDTO;
import com.sonatype.insight.license.dto.model.LegalCopyrightDTO;
import com.sonatype.insight.license.dto.model.LegalFileDTO;
import com.sonatype.insight.license.dto.model.LicenseMetadataDTO;

@Named
public class LegalReportBuilder
{
  LegalReportDataDTO buildLicenseMetadataReport(
      ApiReportRawDataDTOV2 rawReport,
      Map<ComponentIdentifier, Set<ComponentLegalCommentDTO>> componentLegalCommentsByComponentIdentifier,
      Map<ComponentIdentifier, Set<ComponentLegalFileDTO>> componentLegalFilesByComponentIdentifier,
      Set<License> licenses,
      Map<String, LicenseMetadataDTO> licenseMetadataById)
  {
    List<LegalReportComponentDTO> components = augmentReportComponents(rawReport,
        componentLegalCommentsByComponentIdentifier, componentLegalFilesByComponentIdentifier);
    Set<LegalLicenseMetadataDTO> legalLicenseMetadata = getLegalLicenseMetadata(licenses, licenseMetadataById);
    return new LegalReportDataDTO(components, legalLicenseMetadata);
  }

  LegalOrganizationReportDataDTO getLegalOrganizationReportData(
      Set<ApplicationReportRawDataDTO> reportsForOrg,
      Map<ComponentIdentifier, Set<ComponentLegalCommentDTO>> componentLegalCommentsByComponentIdentifier,
      Map<ComponentIdentifier, Set<ComponentLegalFileDTO>> componentLegalFilesByComponentIdentifier,
      Set<License> licenses,
      Map<String, LicenseMetadataDTO> licenseMetadataById)
  {
    Set<LegalApplicationDataDTO> legalAppData = reportsForOrg.stream()
        .map(report -> new LegalApplicationDataDTO(report.applicationPublicId, augmentReportComponents(
            report.apiReportRawDataDTOV2,
            componentLegalCommentsByComponentIdentifier,
            componentLegalFilesByComponentIdentifier)))
        .collect(Collectors.toSet());
    Set<LegalLicenseMetadataDTO> legalLicenseMetadata = getLegalLicenseMetadata(licenses, licenseMetadataById);
    return new LegalOrganizationReportDataDTO(legalAppData, legalLicenseMetadata);
  }

  private List<LegalReportComponentDTO> augmentReportComponents(
      ApiReportRawDataDTOV2 rawReport,
      Map<ComponentIdentifier, Set<ComponentLegalCommentDTO>> componentLegalCommentsByComponentIdentifier,
      Map<ComponentIdentifier, Set<ComponentLegalFileDTO>> componentLegalFilesByComponentIdentifier)
  {
    return rawReport.components.stream()
        .filter(component -> component.componentIdentifier != null)
        .map(component -> {
          ComponentIdentifier key = removeClassifierAndExtension(component.componentIdentifier.toComponentIdentifier());
          return new LegalReportComponentDTO(component, augmentLicenseData(component.licenseData,
              componentLegalCommentsByComponentIdentifier.getOrDefault(key, new HashSet<>()),
              componentLegalFilesByComponentIdentifier.getOrDefault(key, new HashSet<>())));
        })
        .collect(Collectors.toList());
  }

  static ComponentIdentifier removeClassifierAndExtension(ComponentIdentifier componentIdentifier) {
    TreeMap<String, String> coordinates = new TreeMap<>(componentIdentifier.getCoordinates());
    coordinates.remove(ComponentIdentifier.MAVEN_CLASSIFIER);
    coordinates.remove(ComponentIdentifier.MAVEN_EXTENSION);
    return new ComponentIdentifier(componentIdentifier.getFormat(), coordinates);
  }

  private LegalLicenseDataDTO augmentLicenseData(
      ApiLicenseDataDTOV2 sourceData,
      Set<ComponentLegalCommentDTO> componentLegalComments,
      Set<ComponentLegalFileDTO> componentLegalFiles)
  {
    if (sourceData == null) {
      return null;
    }
    return new LegalLicenseDataDTO(
        toLicenseIds(sourceData.declaredLicenses),
        toLicenseIds(sourceData.observedLicenses),
        toLicenseIds(sourceData.effectiveLicenses),
        sourceData.effectiveLicenseThreats,
        componentLegalComments.stream()
            .flatMap(c -> c.getUniqueCopyrights().stream())
            .map(LegalCopyrightDTO::getContent).sorted()
            .collect(Collectors.toList()),
        componentLegalFiles.stream()
            .flatMap(c -> c.getLegalFiles().stream())
            .filter(c -> c.getType().equals("LICENSE"))
            .map(LegalFileDTO::getContent)
            .collect(Collectors.toList()),
        componentLegalFiles.stream()
            .flatMap(c -> c.getLegalFiles().stream())
            .filter(c -> c.getType().equals("NOTICE"))
            .map(LegalFileDTO::getContent)
            .collect(Collectors.toList()));
  }

  private List<String> toLicenseIds(List<ApiLicenseDTO> licenses) {
    return licenses.stream().map(license -> license.licenseId).collect(Collectors.toList());
  }

  private Set<LegalLicenseMetadataDTO> getLegalLicenseMetadata(
      Set<License> licenses,
      Map<String, LicenseMetadataDTO> licenseMetadataById)
  {
    return licenses.stream()
        .filter(license -> licenseMetadataById.containsKey(license.getId()))
        .map(license ->
            new LegalLicenseMetadataDTO(license.getId(),
                license.getShortDisplayName(),
                licenseMetadataById.get(license.getId()).getLicenseText(),
                licenseMetadataById.get(license.getId()).getLicenseObligations()))
        .collect(Collectors.toSet());
  }
}
