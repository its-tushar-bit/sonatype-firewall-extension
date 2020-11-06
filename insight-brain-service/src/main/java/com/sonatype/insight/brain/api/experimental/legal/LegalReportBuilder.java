/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

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
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationReportDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalDataDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalMetadataDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalObligationDTO;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.license.dto.model.ComponentLegalCommentDTO;
import com.sonatype.insight.license.dto.model.ComponentLegalFileDTO;
import com.sonatype.insight.license.dto.model.LegalCopyrightDTO;
import com.sonatype.insight.license.dto.model.LegalFileDTO;
import com.sonatype.insight.license.dto.model.LicenseMetadataDTO;

@Named
public class LegalReportBuilder
{
  ApiLicenseLegalApplicationReportDTO getLicenseLegalApplicationReport(
      ApiReportRawDataDTOV2 rawReport,
      Map<ComponentIdentifier, Set<ComponentLegalCommentDTO>> componentLegalCommentsByComponentIdentifier,
      Map<ComponentIdentifier, Set<ComponentLegalFileDTO>> componentLegalFilesByComponentIdentifier,
      Set<License> licenses,
      Map<String, LicenseMetadataDTO> licenseMetadataById)
  {
    List<ApiLicenseLegalComponentDTO> components = getLicenseLegalComponents(rawReport,
        componentLegalCommentsByComponentIdentifier, componentLegalFilesByComponentIdentifier);
    Set<ApiLicenseLegalMetadataDTO> licenseLegalMetadata = getLicenseLegalMetadata(licenses, licenseMetadataById);
    return new ApiLicenseLegalApplicationReportDTO(components, licenseLegalMetadata);
  }

  private List<ApiLicenseLegalComponentDTO> getLicenseLegalComponents(
      ApiReportRawDataDTOV2 rawReport,
      Map<ComponentIdentifier, Set<ComponentLegalCommentDTO>> componentLegalCommentsByComponentIdentifier,
      Map<ComponentIdentifier, Set<ComponentLegalFileDTO>> componentLegalFilesByComponentIdentifier)
  {
    return rawReport.components.stream()
        .filter(component -> component.componentIdentifier != null)
        .map(component -> {
          ComponentIdentifier key = removeClassifierAndExtension(component.componentIdentifier.toComponentIdentifier());
          return new ApiLicenseLegalComponentDTO(component, getLicenseLegalData(component.licenseData,
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

  ApiLicenseLegalDataDTO getLicenseLegalData(
      ApiLicenseDataDTOV2 sourceData,
      Set<ComponentLegalCommentDTO> componentLegalComments,
      Set<ComponentLegalFileDTO> componentLegalFiles)
  {
    if (sourceData == null) {
      return null;
    }
    return new ApiLicenseLegalDataDTO(
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

  Set<ApiLicenseLegalMetadataDTO> getLicenseLegalMetadata(
      Set<License> licenses,
      Map<String, LicenseMetadataDTO> licenseMetadataById)
  {
    return licenses.stream()
        .filter(license -> licenseMetadataById.containsKey(license.getId()))
        .map(license ->
            new ApiLicenseLegalMetadataDTO(license.getId(),
                license.getShortDisplayName(),
                licenseMetadataById.get(license.getId()).getLicenseText(),
                licenseMetadataById.get(license.getId()).getLicenseObligations().stream()
                    .map(licenseObligationDTO -> new ApiLicenseLegalObligationDTO(licenseObligationDTO, 0))
                    .collect(Collectors.toSet())))
        .collect(Collectors.toSet());
  }
}
