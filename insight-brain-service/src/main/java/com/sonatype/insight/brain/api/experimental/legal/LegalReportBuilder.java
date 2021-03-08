/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationReportDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalCopyrightDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalDataDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalFileDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalMetadataDTO;
import com.sonatype.insight.brain.model.legal.ComponentCopyright;
import com.sonatype.insight.brain.model.legal.ComponentLegalFile;
import com.sonatype.insight.brain.model.legal.ComponentLegalPartStatus;
import com.sonatype.insight.brain.model.legal.CopyrightOverride;
import com.sonatype.insight.brain.model.legal.LegalFileOverride;
import com.sonatype.insight.brain.model.legal.LegalFileType;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.license.dto.model.ComponentLegalCommentDTO;
import com.sonatype.insight.license.dto.model.ComponentLegalFileDTO;
import com.sonatype.insight.license.dto.model.LegalFileDTO;
import com.sonatype.insight.license.dto.model.LicenseMetadataDTO;

import static com.sonatype.insight.brain.api.experimental.legal.LegalComponentIdentifierUtil.removeClassifierAndExtension;

@Named
public class LegalReportBuilder
{
  ApiLicenseLegalApplicationReportDTO getLicenseLegalApplicationReport(
      ApiReportRawDataDTOV2 rawReport,
      Map<ComponentIdentifier, Set<ComponentLegalCommentDTO>> componentLegalCommentsByComponentIdentifier,
      Map<ComponentIdentifier, List<CopyrightOverride>> copyrightOverridesByComponentIdentifier,
      Map<ComponentIdentifier, ComponentCopyright> componentCopyrightsByComponentIdentifier,
      Map<ComponentIdentifier, ComponentLegalFile> componentLegalFileByComponentIdentifier,
      Map<ComponentIdentifier, Set<ComponentLegalFileDTO>> componentLegalFilesByComponentIdentifier,
      Map<ComponentIdentifier, List<LegalFileOverride>> licenseOverridesByComponentIdentifier,
      Map<ComponentIdentifier, List<LegalFileOverride>> noticeOverridesByComponentIdentifier,
      Set<ApiLicenseDTO> multiLicenses,
      Set<License> licenses,
      Map<String, LicenseMetadataDTO> licenseMetadataById)
  {
    List<ApiLicenseLegalComponentDTO> components =
        getLicenseLegalComponents(rawReport,
            componentLegalCommentsByComponentIdentifier,
            copyrightOverridesByComponentIdentifier,
            componentCopyrightsByComponentIdentifier,
            componentLegalFileByComponentIdentifier,
            componentLegalFilesByComponentIdentifier,
            licenseOverridesByComponentIdentifier, noticeOverridesByComponentIdentifier);
    Set<ApiLicenseLegalMetadataDTO> licenseLegalMetadata =
        getLicenseLegalMetadata(multiLicenses, licenses, licenseMetadataById);
    return new ApiLicenseLegalApplicationReportDTO(components, licenseLegalMetadata);
  }

  private List<ApiLicenseLegalComponentDTO> getLicenseLegalComponents(
      ApiReportRawDataDTOV2 rawReport,
      Map<ComponentIdentifier, Set<ComponentLegalCommentDTO>> componentLegalCommentsByComponentIdentifier,
      Map<ComponentIdentifier, List<CopyrightOverride>> copyrightOverridesByComponentIdentifier,
      Map<ComponentIdentifier, ComponentCopyright> componentCopyrightsByComponentIdentifier,
      Map<ComponentIdentifier, ComponentLegalFile> componentLegalFileByComponentIdentifier,
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
              componentCopyrightsByComponentIdentifier.getOrDefault(key, null),
              componentLegalFileByComponentIdentifier.getOrDefault(key, null),
              componentLegalFilesByComponentIdentifier.getOrDefault(key, new LinkedHashSet<>()),
              licenseOverridesByComponentIdentifier.getOrDefault(key, Collections.emptyList()),
              noticeOverridesByComponentIdentifier.getOrDefault(key, Collections.emptyList())));
        })
        .collect(Collectors.toList());
  }

  ApiLicenseLegalDataDTO getLicenseLegalData(
      ApiLicenseDataDTOV2 sourceData,
      Set<ComponentLegalCommentDTO> componentLegalComments,
      List<CopyrightOverride> copyrightOverrides,
      ComponentCopyright componentCopyright,
      ComponentLegalFile componentLegalFile,
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
        getLegalFiles(LegalFileType.NOTICE, componentLegalFiles, noticeOverrides),
        componentCopyright == null ? null : componentCopyright.getId(),
        componentCopyright == null ? null : componentCopyright.getOwnerId(),
        componentLegalFile == null ? null : componentLegalFile.getId(),
        componentLegalFile == null ? null : componentLegalFile.getOwnerId());
  }

  private List<String> toLicenseIds(List<ApiLicenseDTO> licenses) {
    return licenses.stream().map(license -> license.licenseId).collect(Collectors.toList());
  }

  private List<ApiLicenseLegalCopyrightDTO> getCopyrights(
      Set<ComponentLegalCommentDTO> componentLegalComments,
      List<CopyrightOverride> copyrightOverrides)
  {
    if (copyrightOverrides.isEmpty()) {
      return componentLegalComments.stream()
          .flatMap(c -> c.getUniqueCopyrights().stream())
          .distinct()
          .map(legalCopyrightDTO -> new ApiLicenseLegalCopyrightDTO(
              null,
              legalCopyrightDTO.getContent(),
              legalCopyrightDTO.getContentHash(),
              ComponentLegalPartStatus.ENABLED))
          .sorted(Comparator.comparing(lc -> lc.content))
          .collect(Collectors.toList());
    }
    return copyrightOverrides.stream()
        .map(copyrightOverride -> new ApiLicenseLegalCopyrightDTO(
            copyrightOverride.getId(),
            copyrightOverride.getContent(),
            copyrightOverride.getOriginalContentHash(),
            copyrightOverride.getStatus()))
        .collect(Collectors.toList());
  }

  private List<ApiLicenseLegalFileDTO> getLegalFiles(
      LegalFileType legalFileType,
      Set<ComponentLegalFileDTO> componentLegalFiles,
      List<LegalFileOverride> legalFileOverrides)
  {
    if (legalFileOverrides.isEmpty()) {
      return componentLegalFiles.stream()
          .flatMap(c -> c.getLegalFiles().stream())
          .filter(c -> c.getType().equalsIgnoreCase(legalFileType.toString()))
          .map(legalFileDTO -> new ApiLicenseLegalFileDTO(
              null,
              legalFileDTO.getRelPath(),
              legalFileDTO.getContent(),
              legalFileDTO.getContentHash(),
              ComponentLegalPartStatus.ENABLED))
          .collect(Collectors.toList());
    }

    return legalFileOverrides.stream()
        .filter(legalFileOverride -> legalFileOverride.getType() == legalFileType)
        .map(legalFileOverride -> {
          //Find the relPath by matching the contentHash.
          String relPath = componentLegalFiles.stream()
              .flatMap(c -> c.getLegalFiles().stream())
              .filter(l -> l.getContentHash().equals(legalFileOverride.getOriginalContentHash()))
              .findFirst()
              .map(LegalFileDTO::getRelPath)
              .orElse(null);
          return new ApiLicenseLegalFileDTO(
              legalFileOverride.getId(),
              relPath,
              legalFileOverride.getContent(),
              legalFileOverride.getOriginalContentHash(),
              legalFileOverride.getStatus());
        })
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
