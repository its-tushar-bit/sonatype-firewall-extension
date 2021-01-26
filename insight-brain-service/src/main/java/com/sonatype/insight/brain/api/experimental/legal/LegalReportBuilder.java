/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalFileDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalMetadataDTO;
import com.sonatype.insight.brain.model.legal.CopyrightOverride;
import com.sonatype.insight.brain.model.legal.LegalFileOverride;
import com.sonatype.insight.brain.model.legal.LegalFileType;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.license.dto.model.ComponentLegalCommentDTO;
import com.sonatype.insight.license.dto.model.ComponentLegalFileDTO;
import com.sonatype.insight.license.dto.model.LegalCopyrightDTO;
import com.sonatype.insight.license.dto.model.LicenseMetadataDTO;

@Named
public class LegalReportBuilder
{
  ApiLicenseLegalApplicationReportDTO getLicenseLegalApplicationReport(
      ApiReportRawDataDTOV2 rawReport,
      Map<ComponentIdentifier, Set<ComponentLegalCommentDTO>> componentLegalCommentsByComponentIdentifier,
      Map<ComponentIdentifier, List<CopyrightOverride>> copyrightOverridesByComponentIdentifier,
      Map<ComponentIdentifier, Set<ComponentLegalFileDTO>> componentLegalFilesByComponentIdentifier,
      Map<ComponentIdentifier, List<LegalFileOverride>> licenseOverridesByComponentIdentifier,
      Map<ComponentIdentifier, List<LegalFileOverride>> noticeOverridesByComponentIdentifier,
      Set<ApiLicenseDTO> multiLicenses,
      Set<License> licenses,
      Map<String, LicenseMetadataDTO> licenseMetadataById)
  {
    List<ApiLicenseLegalComponentDTO> components =
        getLicenseLegalComponents(rawReport, componentLegalCommentsByComponentIdentifier,
            copyrightOverridesByComponentIdentifier, componentLegalFilesByComponentIdentifier,
            licenseOverridesByComponentIdentifier, noticeOverridesByComponentIdentifier);
    Set<ApiLicenseLegalMetadataDTO> licenseLegalMetadata =
        getLicenseLegalMetadata(multiLicenses, licenses, licenseMetadataById);
    return new ApiLicenseLegalApplicationReportDTO(components, licenseLegalMetadata);
  }

  private List<ApiLicenseLegalComponentDTO> getLicenseLegalComponents(
      ApiReportRawDataDTOV2 rawReport,
      Map<ComponentIdentifier, Set<ComponentLegalCommentDTO>> componentLegalCommentsByComponentIdentifier,
      Map<ComponentIdentifier, List<CopyrightOverride>> copyrightOverridesByComponentIdentifier,
      Map<ComponentIdentifier, Set<ComponentLegalFileDTO>> componentLegalFilesByComponentIdentifier,
      Map<ComponentIdentifier, List<LegalFileOverride>> licenseOverridesByComponentIdentifier,
      Map<ComponentIdentifier, List<LegalFileOverride>> noticeOverridesByComponentIdentifier)
  {
    return rawReport.components.stream()
        .filter(component -> component.componentIdentifier != null)
        .map(component -> {
          ComponentIdentifier key = removeClassifierAndExtension(component.componentIdentifier.toComponentIdentifier());
          return new ApiLicenseLegalComponentDTO(component, getLicenseLegalData(component.licenseData,
              componentLegalCommentsByComponentIdentifier.getOrDefault(key, new LinkedHashSet<>()),
              copyrightOverridesByComponentIdentifier.getOrDefault(key, Collections.emptyList()),
              componentLegalFilesByComponentIdentifier.getOrDefault(key, new LinkedHashSet<>()),
              licenseOverridesByComponentIdentifier.getOrDefault(key, Collections.emptyList()),
              noticeOverridesByComponentIdentifier.getOrDefault(key, Collections.emptyList())));
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
      List<CopyrightOverride> copyrightOverrides,
      Set<ComponentLegalFileDTO> componentLegalFiles,
      List<LegalFileOverride> licenseOverrides,
      List<LegalFileOverride> noticeOverrides)
  {
    if (sourceData == null) {
      return null;
    }
    return new ApiLicenseLegalDataDTO(
        toLicenseIds(sourceData.declaredLicenses),
        toLicenseIds(sourceData.observedLicenses),
        toLicenseIds(sourceData.effectiveLicenses),
        sourceData.effectiveLicenseThreats,
        getCopyrights(componentLegalComments, copyrightOverrides),
        getLegalFiles(LegalFileType.LICENSE, componentLegalFiles, licenseOverrides),
        getLegalFiles(LegalFileType.NOTICE, componentLegalFiles, noticeOverrides));
  }

  private List<String> toLicenseIds(List<ApiLicenseDTO> licenses) {
    return licenses.stream().map(license -> license.licenseId).collect(Collectors.toList());
  }

  private List<String> getCopyrights(
      Set<ComponentLegalCommentDTO> componentLegalComments,
      List<CopyrightOverride> copyrightOverrides)
  {
    if (copyrightOverrides.isEmpty()) {
      return componentLegalComments.stream()
          .flatMap(c -> c.getUniqueCopyrights().stream())
          .map(LegalCopyrightDTO::getContent).sorted()
          .collect(Collectors.toList());
    }
    return copyrightOverrides.stream().map(CopyrightOverride::getContent).collect(Collectors.toList());
  }

  private List<ApiLicenseLegalFileDTO> getLegalFiles(
      LegalFileType legalFileType,
      Set<ComponentLegalFileDTO> componentLegalFiles,
      List<LegalFileOverride> legalFileOverrides)
  {
    if (legalFileOverrides.isEmpty()) {
      return componentLegalFiles.stream()
          .flatMap(c -> c.getLegalFiles().stream())
          .filter(c -> c.getType().equals(legalFileType.toString()))
          .map(legalFileDTO -> new ApiLicenseLegalFileDTO(legalFileDTO.getRelPath(), legalFileDTO.getContent()))
          .collect(Collectors.toList());
    }
    return legalFileOverrides.stream()
        .filter(legalFileOverride -> legalFileOverride.getType() == legalFileType)
        // TODO: Determine relPath by finding the corresponding legalFileDTO by LegalFileDTO.originalContentHash
        .map(legalFileOverride -> new ApiLicenseLegalFileDTO(null, legalFileOverride.getContent()))
        .collect(Collectors.toList());
  }

  Set<ApiLicenseLegalMetadataDTO> getLicenseLegalMetadata(
      Collection<ApiLicenseDTO> multiLicenses,
      Set<License> licenses,
      Map<String, LicenseMetadataDTO> licenseMetadataById)
  {
    Set<ApiLicenseLegalMetadataDTO> allLicenseLegalMetadata = new LinkedHashSet<>();
    Set<String> licenseIds = new HashSet<>();
    for (License license : licenses) {
      ApiLicenseLegalMetadataDTO licenseLegalMetadata = new ApiLicenseLegalMetadataDTO();
      licenseLegalMetadata.licenseId = license.getId();
      licenseLegalMetadata.licenseName = license.getShortDisplayName();
      if (licenseMetadataById.containsKey(license.getId())) {
        LicenseMetadataDTO licenseMetadataDTO = licenseMetadataById.get(license.getId());
        licenseLegalMetadata.licenseText = licenseMetadataDTO.getLicenseText();
        licenseLegalMetadata.obligations = licenseMetadataDTO.getLicenseObligations();
      }
      allLicenseLegalMetadata.add(licenseLegalMetadata);
      licenseIds.add(license.getId());
    }
    for (ApiLicenseDTO multiLicense : multiLicenses) {
      if (licenseIds.add(multiLicense.licenseId)) {
        ApiLicenseLegalMetadataDTO licenseLegalMetadata = new ApiLicenseLegalMetadataDTO();
        licenseLegalMetadata.licenseId = multiLicense.licenseId;
        licenseLegalMetadata.licenseName = multiLicense.licenseName;
        allLicenseLegalMetadata.add(licenseLegalMetadata);
      }
    }
    return allLicenseLegalMetadata;
  }
}
