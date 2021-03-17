/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
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
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalObligationDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentObligationAttributionDTO;
import com.sonatype.insight.brain.model.legal.ComponentCopyright;
import com.sonatype.insight.brain.model.legal.ComponentLegalFile;
import com.sonatype.insight.brain.model.legal.ComponentLegalPartStatus;
import com.sonatype.insight.brain.model.legal.ComponentObligation;
import com.sonatype.insight.brain.model.legal.ComponentObligationAttribution;
import com.sonatype.insight.brain.model.legal.CopyrightOverride;
import com.sonatype.insight.brain.model.legal.LegalFileOverride;
import com.sonatype.insight.brain.model.legal.LegalFileType;
import com.sonatype.insight.brain.model.legal.ObligationStatus;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.license.dto.model.ComponentLegalCommentDTO;
import com.sonatype.insight.license.dto.model.ComponentLegalFileDTO;
import com.sonatype.insight.license.dto.model.LegalFileDTO;
import com.sonatype.insight.license.dto.model.LicenseMetadataDTO;
import com.sonatype.insight.license.dto.model.LicenseObligationDTO;

import static com.sonatype.insight.brain.api.experimental.legal.LegalComponentIdentifierUtil.removeClassifierAndExtension;

@Named
public class LegalReportBuilder
{
  ApiLicenseLegalApplicationReportDTO getLicenseLegalApplicationReport(
      ApiReportRawDataDTOV2 rawReport,
      Map<ComponentIdentifier, Set<ComponentLegalCommentDTO>> componentLegalCommentsByComponentIdentifier,
      Map<ComponentIdentifier, List<CopyrightOverride>> copyrightOverridesByComponentIdentifier,
      Map<ComponentIdentifier, ComponentCopyright> componentCopyrightsByComponentIdentifier,
      Map<ComponentIdentifier, ComponentLegalFile> componentNoticesByComponentIdentifier,
      Map<ComponentIdentifier, ComponentLegalFile> componentLicensesByComponentIdentifier,
      Map<ComponentIdentifier, Set<ComponentLegalFileDTO>> componentLegalFilesByComponentIdentifier,
      Map<ComponentIdentifier, List<LegalFileOverride>> licenseOverridesByComponentIdentifier,
      Map<ComponentIdentifier, List<LegalFileOverride>> noticeOverridesByComponentIdentifier,
      Map<ComponentIdentifier, List<ComponentObligation>> obligationByComponentIdentifier,
      Map<ComponentIdentifier, List<ComponentObligationAttribution>> attributionByComponentIdentifier,
      Set<ApiLicenseDTO> multiLicenses,
      Set<License> licenses,
      Map<String, LicenseMetadataDTO> licenseMetadataById)
  {
    Set<ApiLicenseLegalMetadataDTO> licenseLegalMetadata =
        getLicenseLegalMetadata(multiLicenses, licenses, licenseMetadataById);
    List<ApiLicenseLegalComponentDTO> components =
        getLicenseLegalComponents(rawReport,
            licenseLegalMetadata,
            componentLegalCommentsByComponentIdentifier,
            copyrightOverridesByComponentIdentifier,
            componentCopyrightsByComponentIdentifier,
            componentNoticesByComponentIdentifier,
            componentLicensesByComponentIdentifier,
            componentLegalFilesByComponentIdentifier,
            licenseOverridesByComponentIdentifier,
            noticeOverridesByComponentIdentifier,
            obligationByComponentIdentifier,
            attributionByComponentIdentifier);
    return new ApiLicenseLegalApplicationReportDTO(components, licenseLegalMetadata);
  }

  private List<ApiLicenseLegalComponentDTO> getLicenseLegalComponents(
      ApiReportRawDataDTOV2 rawReport,
      Set<ApiLicenseLegalMetadataDTO> licenseLegalMetadata,
      Map<ComponentIdentifier, Set<ComponentLegalCommentDTO>> componentLegalCommentsByComponentIdentifier,
      Map<ComponentIdentifier, List<CopyrightOverride>> copyrightOverridesByComponentIdentifier,
      Map<ComponentIdentifier, ComponentCopyright> componentCopyrightsByComponentIdentifier,
      Map<ComponentIdentifier, ComponentLegalFile> componentNoticesByComponentIdentifier,
      Map<ComponentIdentifier, ComponentLegalFile> componentLicensesByComponentIdentifier,
      Map<ComponentIdentifier, Set<ComponentLegalFileDTO>> componentLegalFilesByComponentIdentifier,
      Map<ComponentIdentifier, List<LegalFileOverride>> licenseOverridesByComponentIdentifier,
      Map<ComponentIdentifier, List<LegalFileOverride>> noticeOverridesByComponentIdentifier,
      Map<ComponentIdentifier, List<ComponentObligation>> obligationByComponentIdentifier,
      Map<ComponentIdentifier, List<ComponentObligationAttribution>> attributionByComponentIdentifier)
  {
    return rawReport.components.stream()
        .filter(component -> component.componentIdentifier != null)
        .map(component -> {
          ComponentIdentifier key = removeClassifierAndExtension(component.componentIdentifier.toComponentIdentifier());
          return new ApiLicenseLegalComponentDTO(component, getLicenseLegalData(component.licenseData,
              licenseLegalMetadata,
              componentLegalCommentsByComponentIdentifier.getOrDefault(key, new LinkedHashSet<>()),
              copyrightOverridesByComponentIdentifier.getOrDefault(key, Collections.emptyList()),
              componentCopyrightsByComponentIdentifier.getOrDefault(key, null),
              componentLicensesByComponentIdentifier.getOrDefault(key, null),
              componentNoticesByComponentIdentifier.getOrDefault(key, null),
              componentLegalFilesByComponentIdentifier.getOrDefault(key, new LinkedHashSet<>()),
              licenseOverridesByComponentIdentifier.getOrDefault(key, Collections.emptyList()),
              noticeOverridesByComponentIdentifier.getOrDefault(key, Collections.emptyList()),
              obligationByComponentIdentifier.getOrDefault(key, Collections.emptyList()),
              attributionByComponentIdentifier.getOrDefault(key, Collections.emptyList())));
        })
        .collect(Collectors.toList());
  }

  ApiLicenseLegalDataDTO getLicenseLegalData(
      ApiLicenseDataDTOV2 sourceData,
      Set<ApiLicenseLegalMetadataDTO> licenseLegalMetadata,
      Set<ComponentLegalCommentDTO> componentLegalComments,
      List<CopyrightOverride> copyrightOverrides,
      ComponentCopyright componentCopyright,
      ComponentLegalFile componentLicense,
      ComponentLegalFile componentNotice,
      Set<ComponentLegalFileDTO> componentLegalFiles,
      List<LegalFileOverride> licenseOverrides,
      List<LegalFileOverride> noticeOverrides,
      List<ComponentObligation> obligations,
      List<ComponentObligationAttribution> attributions)
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
        getObligations(obligations, sourceData.effectiveLicenses, licenseLegalMetadata),
        getAttributions(attributions),
        componentCopyright == null ? null : componentCopyright.getId(),
        componentCopyright == null ? null : componentCopyright.getOwnerId(),
        componentLicense == null ? null : componentLicense.getId(),
        componentLicense == null ? null : componentLicense.getOwnerId(),
        componentNotice == null ? null : componentNotice.getId(),
        componentNotice == null ? null : componentNotice.getOwnerId());
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
        .sorted(LegalReportBuilder::sortCopyrightOverrides)
        .map(copyrightOverride -> new ApiLicenseLegalCopyrightDTO(
            copyrightOverride.getId(),
            copyrightOverride.getContent(),
            copyrightOverride.getOriginalContentHash(),
            copyrightOverride.getStatus()))
        .collect(Collectors.toList());
  }

  public static int sortCopyrightOverrides(CopyrightOverride c1, CopyrightOverride c2) {
    if (c1.getOriginalContentHash() != null && c2.getOriginalContentHash() == null) {
      return -1;
    }
    if (c1.getOriginalContentHash() == null && c2.getOriginalContentHash() != null) {
      return 1;
    }
    return c1.getContent().compareTo(c2.getContent());
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
          .sorted(Comparator.comparing(lc -> lc.content))
          .collect(Collectors.toList());
    }

    return legalFileOverrides.stream()
        .sorted(LegalReportBuilder::sortLegalFileOverrides)
        .filter(legalFileOverride -> legalFileOverride.getType() == legalFileType)
        .map(legalFileOverride -> {
          //Find the relPath by matching the contentHash.
          String relPath = componentLegalFiles.stream()
              .flatMap(c -> c.getLegalFiles().stream())
              .filter(l -> l.getType().equalsIgnoreCase(legalFileOverride.getType().toString()) &&
                  l.getContentHash().equals(legalFileOverride.getOriginalContentHash()))
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

  private List<ApiLicenseLegalObligationDTO> getObligations(
      final List<ComponentObligation> obligations,
      final List<ApiLicenseDTO> effectiveLicenses,
      final Set<ApiLicenseLegalMetadataDTO> licenseLegalMetadata)
  {
    List<ApiLicenseLegalObligationDTO> licenseLegalObligation = new ArrayList<>();
    Set<String> obligationNames = new HashSet<>();
    for (ApiLicenseDTO licenseDTO : effectiveLicenses) {
      obligationNames.addAll(getObligationNamesForLicense(licenseDTO.licenseId, licenseLegalMetadata));
    }

    Map<String, ComponentObligation> nameToObligation = obligations.stream()
        .collect(Collectors.toMap(ComponentObligation::getObligationName, Function.identity()));

    for (String obligationName : obligationNames) {
      if (nameToObligation.containsKey(obligationName)) {
        licenseLegalObligation.add(new ApiLicenseLegalObligationDTO(nameToObligation.get(obligationName)));
      }
      else {
        ApiLicenseLegalObligationDTO apiLicenseLegalObligationDTO = new ApiLicenseLegalObligationDTO();
        apiLicenseLegalObligationDTO.setName(obligationName);
        apiLicenseLegalObligationDTO.setStatus(ObligationStatus.OPEN);
        licenseLegalObligation.add(apiLicenseLegalObligationDTO);
      }
    }
    return licenseLegalObligation;
  }

  private List<ComponentObligationAttributionDTO> getAttributions(
      final List<ComponentObligationAttribution> attributions)
  {
    return attributions.stream()
        .map(ComponentObligationAttributionDTO::new)
        .collect(Collectors.toList());
  }

  private Set<String> getObligationNamesForLicense(
      String licenseId, Set<ApiLicenseLegalMetadataDTO> licenseLegalMetadataSet)
  {
    for (ApiLicenseLegalMetadataDTO llm : licenseLegalMetadataSet) {
      if (llm.licenseId.equals(licenseId)) {
        return llm.obligations.stream()
            .map(LicenseObligationDTO::getName)
            .collect(Collectors.toSet());
      }
    }
    return Collections.emptySet();
  }

  public static int sortLegalFileOverrides(LegalFileOverride l1, LegalFileOverride l2) {
    if (l1.getOriginalContentHash() != null && l2.getOriginalContentHash() == null) {
      return -1;
    }
    if (l1.getOriginalContentHash() == null && l2.getOriginalContentHash() != null) {
      return 1;
    }
    return l1.getContent().compareTo(l2.getContent());
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
