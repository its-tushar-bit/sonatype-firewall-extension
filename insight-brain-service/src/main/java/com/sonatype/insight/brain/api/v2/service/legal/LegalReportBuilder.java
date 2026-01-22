/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service.legal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseThreatDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationReportDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentReportDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalCopyrightDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalDataDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalFileDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalMetadataDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalObligationDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentObligationAttributionDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.LegalSourceLinkDTO;
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections4.CollectionUtils;

import static com.sonatype.insight.brain.api.experimental.legal.LegalComponentIdentifierUtil.removeClassifierAndExtension;

/**
 * Helper class for building out the application and component legal reports.
 */
@Named
public class LegalReportBuilder
{
  /**
   * Build an {@link ApiLicenseLegalApplicationReportDTO}
   *
   * @param rawReport                                   the HDS {@link ApiReportRawDataDTOV2} for this application
   * @param componentReportLegalMap                     a map where the key is the {@link ApiReportComponentDTOV2} and
   *                                                    value is the {@link ComponentIdentifierLegalData}. All
   *                                                    components in the report raw data should be accounted for in
   *                                                    this map.
   * @param componentLegalCommentsByComponentIdentifier a map where the key is the {@link ComponentIdentifier} and the
   *                                                    value is the IQ stored {@link ComponentLegalCommentDTO}.
   *                                                    Components with not persisted LegalComment can be excluded from
   *                                                    the map or have an empty list.
   * @param componentLegalFilesByComponentIdentifier    a map where the key is the {@link ComponentIdentifier} and the
   *                                                    value is the IQ stored {@link ComponentLegalFileDTO}. Components
   *                                                    with not persisted LegalFile can be excluded from the map or
   *                                                    have an empty list.
   * @param multiLicenseToSingleLicense                 a map where the key is a multi-license and the value is a set of
   *                                                    single licenses which makeup the multi license.
   * @param licenseMetadataById                         a map where the key is the single license id and the value is
   *                                                    the {@link LicenseMetadataDTO} for this license.
   * @return a populate {@link ApiLicenseLegalApplicationReportDTO}.
   */
  public ApiLicenseLegalApplicationReportDTO getLicenseLegalApplicationReport(
      ApiReportRawDataDTOV2 rawReport,
      Map<ApiReportComponentDTOV2, ComponentIdentifierLegalData> componentReportLegalMap,
      Map<ComponentIdentifier, Set<ComponentLegalCommentDTO>> componentLegalCommentsByComponentIdentifier,
      Map<ComponentIdentifier, Set<ComponentLegalFileDTO>> componentLegalFilesByComponentIdentifier,
      Map<ApiLicenseDTO, Set<License>> multiLicenseToSingleLicense,
      Map<String, LicenseMetadataDTO> licenseMetadataById,
      Map<ComponentIdentifier, Set<LegalSourceLinkDTO>> componentSourceLinksMap)
  {
    Set<ApiLicenseLegalMetadataDTO> licenseLegalMetadata =
        getLicenseLegalMetadata(multiLicenseToSingleLicense, licenseMetadataById);

    List<ApiLicenseLegalComponentDTO> components =
        getLicenseLegalComponents(
            rawReport.components,
            licenseLegalMetadata,
            componentLegalCommentsByComponentIdentifier,
            componentLegalFilesByComponentIdentifier,
            componentReportLegalMap,
            componentSourceLinksMap,
            multiLicenseToSingleLicense
        );
    return new ApiLicenseLegalApplicationReportDTO(components, licenseLegalMetadata);
  }

  /**
   * Given a component, build out an {@link ApiLicenseLegalComponentReportDTO}.
   *
   * @param apiReportComponentDTOV2      the {@link ApiReportComponentDTOV2} in question.
   * @param componentIdentifierLegalData the component's {@link ComponentIdentifierLegalData}
   * @param componentLegalComments       the component's list of persisted IQ {@link ComponentLegalCommentDTO}, can be
   *                                     empty.
   * @param componentLegalFiles          the component's list of persisted IQ {@link ComponentLegalFileDTO}, can be
   *                                     empty
   * @param multiLicenseToSingleLicense  a map where the key is a multi-license and the value is a set of single
   *                                     licenses which makeup the multi license.
   * @param licenseMetadataById          a map where the key is the single license id and the value is the {@link
   *                                     LicenseMetadataDTO} for this license.
   * @return a populated {@link ApiLicenseLegalComponentReportDTO}
   */
  public ApiLicenseLegalComponentReportDTO getLicenseLegalComponentReport(
      ApiReportComponentDTOV2 apiReportComponentDTOV2,
      ComponentIdentifierLegalData componentIdentifierLegalData,
      Set<ComponentLegalCommentDTO> componentLegalComments,
      Set<ComponentLegalFileDTO> componentLegalFiles,
      Map<ApiLicenseDTO, Set<License>> multiLicenseToSingleLicense,
      Map<String, LicenseMetadataDTO> licenseMetadataById,
      Set<LegalSourceLinkDTO> sourceLinks
  )
  {
    Set<ApiLicenseLegalMetadataDTO> licenseLegalMetadata = getLicenseLegalMetadata(
        multiLicenseToSingleLicense, licenseMetadataById);

    ApiLicenseLegalComponentDTO componentDTO = getLicenseLegalComponents(
        Collections.singleton(apiReportComponentDTOV2),
        licenseLegalMetadata,
        ImmutableMap.of(componentIdentifierLegalData.getComponentIdentifier(), componentLegalComments),
        ImmutableMap.of(componentIdentifierLegalData.getComponentIdentifier(), componentLegalFiles),
        ImmutableMap.of(apiReportComponentDTOV2, componentIdentifierLegalData),
        ImmutableMap.of(componentIdentifierLegalData.getComponentIdentifier(), sourceLinks),
        multiLicenseToSingleLicense
    ).get(0);

    return new ApiLicenseLegalComponentReportDTO(componentDTO, licenseLegalMetadata);
  }

  private List<ApiLicenseLegalComponentDTO> getLicenseLegalComponents(
      Collection<ApiReportComponentDTOV2> apiComponentDTOV2s,
      Set<ApiLicenseLegalMetadataDTO> licenseLegalMetadata,
      Map<ComponentIdentifier, Set<ComponentLegalCommentDTO>> componentLegalCommentsByComponentIdentifier,
      Map<ComponentIdentifier, Set<ComponentLegalFileDTO>> componentLegalFilesByComponentIdentifier,
      Map<ApiReportComponentDTOV2, ComponentIdentifierLegalData> apiReportComponentDTOV2ComponentIdentifierLegalDataMap,
      Map<ComponentIdentifier, Set<LegalSourceLinkDTO>> sourceLinks,
      Map<ApiLicenseDTO, Set<License>> multiLicenseToSingleLicense
  )
  {
    return apiComponentDTOV2s.stream()
        .filter(apiReportComponentDTOV2 -> apiReportComponentDTOV2.componentIdentifier != null)
        .map(apiReportComponentDTOV2 -> {
          ComponentIdentifier key =
              removeClassifierAndExtension(apiReportComponentDTOV2.componentIdentifier.toComponentIdentifier());
          ComponentIdentifierLegalData componentIdentifierLegalData =
              apiReportComponentDTOV2ComponentIdentifierLegalDataMap.get(apiReportComponentDTOV2);
          return new ApiLicenseLegalComponentDTO(apiReportComponentDTOV2,
              getLicenseLegalData(
                  apiReportComponentDTOV2.licenseData,
                  componentIdentifierLegalData.getHighestEffectiveLicenseThreatGroup(),
                  licenseLegalMetadata,
                  componentLegalCommentsByComponentIdentifier.getOrDefault(key, new LinkedHashSet<>()),
                  componentIdentifierLegalData.getCopyrightOverrides(),
                  componentIdentifierLegalData.getComponentCopyrights(),
                  componentIdentifierLegalData.getComponentLicense(),
                  componentIdentifierLegalData.getComponentNotice(),
                  componentLegalFilesByComponentIdentifier.getOrDefault(key, new LinkedHashSet<>()),
                  componentIdentifierLegalData.getLicenseOverrides(),
                  componentIdentifierLegalData.getNoticeOverrides(),
                  componentIdentifierLegalData.getObligations(),
                  componentIdentifierLegalData.getAttributions(),
                  sourceLinks.getOrDefault(key, new LinkedHashSet<>()),
                  multiLicenseToSingleLicense
              ),
              componentIdentifierLegalData.getStageScans());
        })
        .collect(Collectors.toList());
  }

  private ApiLicenseLegalDataDTO getLicenseLegalData(
      ApiLicenseDataDTOV2 apiLicenseDataDTOV2,
      ApiLicenseThreatDTOV2 highestEffectiveLicenseThreatGroup,
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
      List<ComponentObligationAttribution> attributions,
      Set<LegalSourceLinkDTO> sourceLinks,
      Map<ApiLicenseDTO, Set<License>> multiLicenseToSingleLicense
  )
  {
    if (apiLicenseDataDTOV2 == null) {
      return null;
    }

    return new ApiLicenseLegalDataDTO(
        toLicenseIds(apiLicenseDataDTOV2.declaredLicenses),
        toLicenseIds(apiLicenseDataDTOV2.observedLicenses),
        toLicenseIds(apiLicenseDataDTOV2.effectiveLicenses),
        highestEffectiveLicenseThreatGroup,
        getCopyrights(componentLegalComments, copyrightOverrides),
        getLegalFiles(LegalFileType.LICENSE, componentLegalFiles, licenseOverrides),
        getLegalFiles(LegalFileType.NOTICE, componentLegalFiles, noticeOverrides),
        getObligations(obligations, apiLicenseDataDTOV2.effectiveLicenses, licenseLegalMetadata,
            multiLicenseToSingleLicense),
        getAttributions(attributions),
        sourceLinks,
        apiLicenseDataDTOV2.status,
        componentCopyright == null ? null : componentCopyright.getId(),
        componentCopyright == null ? null : componentCopyright.getOwnerId(),
        componentCopyright == null ? null : componentCopyright.getLastUpdatedByUsername(),
        componentCopyright == null ? null : componentCopyright.getLastUpdatedAt(),
        componentLicense == null ? null : componentLicense.getId(),
        componentLicense == null ? null : componentLicense.getOwnerId(),
        componentLicense == null ? null : componentLicense.getLastUpdatedByUsername(),
        componentLicense == null ? null : componentLicense.getLastUpdatedAt(),
        componentNotice == null ? null : componentNotice.getId(),
        componentNotice == null ? null : componentNotice.getOwnerId(),
        componentNotice == null ? null : componentNotice.getLastUpdatedByUsername(),
        componentNotice == null ? null : componentNotice.getLastUpdatedAt());
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
          .distinct()
          .collect(Collectors.toList());
    }

    return legalFileOverrides.stream()
        .sorted(LegalReportBuilder::sortLegalFileOverrides)
        .map(legalFileOverride -> {
          //Find the relPath by matching the contentHash.
          String relPath = componentLegalFiles.stream()
              .flatMap(c -> c.getLegalFiles().stream())
              .filter(l -> l.getType().equalsIgnoreCase(legalFileType.toString()) &&
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
        .distinct()
        .collect(Collectors.toList());
  }

  private List<ApiLicenseLegalObligationDTO> getObligations(
      final List<ComponentObligation> obligations,
      final List<ApiLicenseDTO> effectiveLicenses,
      final Set<ApiLicenseLegalMetadataDTO> licenseLegalMetadata,
      final Map<ApiLicenseDTO, Set<License>> multiLicenseToSingleLicense
  )
  {
    List<ApiLicenseLegalObligationDTO> licenseLegalObligation = new ArrayList<>();
    Set<String> obligationNames = new HashSet<>();
    for (ApiLicenseDTO licenseDTO : effectiveLicenses) {
      Set<License> singleLicenses = multiLicenseToSingleLicense.get(licenseDTO);
      if (CollectionUtils.isNotEmpty(singleLicenses)) {
        obligationNames.addAll(singleLicenses.stream()
            .flatMap(license -> getObligationNamesForLicense(license.getId(), licenseLegalMetadata).stream())
            .collect(Collectors.toSet()));
      }
      else {
        obligationNames.addAll(getObligationNamesForLicense(licenseDTO.licenseId, licenseLegalMetadata));
      }
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
    licenseLegalObligation.sort(Comparator.comparing(ApiLicenseLegalObligationDTO::getName));
    return licenseLegalObligation;
  }

  private List<ComponentObligationAttributionDTO> getAttributions(
      final List<ComponentObligationAttribution> attributions)
  {
    return attributions.stream()
        .map(ComponentObligationAttributionDTO::new)
        .sorted(LegalReportBuilder::sortAttributions)
        .collect(Collectors.toList());
  }

  private static int sortAttributions(ComponentObligationAttributionDTO a1, ComponentObligationAttributionDTO a2) {
    if (a1.getObligationName() != null && a2.getObligationName() == null) {
      return -1;
    }
    if (a1.getObligationName() == null && a2.getObligationName() != null) {
      return 1;
    }
    return Objects.compare(a1.getObligationName(), a2.getObligationName(), String::compareTo);
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

  /**
   * A convenience method for building out the {@link ApiLicenseLegalMetadataDTO}.
   * <p>
   * Given a Set of effective, observed, and declared multi-licenses and with
   *
   * @param multiLicenseToSingleLicense - a Map who's key is a MultiLicense and who's value is a set of Single Licenses
   *                                    associated with this multi license
   * @param licenseMetadataById         - Map who's key in the single license and value is the associated {@link
   *                                    LicenseMetadataDTO}
   * @return the Set of {@link ApiLicenseLegalMetadataDTO} for the given licenses.
   */
  @VisibleForTesting
  Set<ApiLicenseLegalMetadataDTO> getLicenseLegalMetadata(
      Map<ApiLicenseDTO, Set<License>> multiLicenseToSingleLicense,
      Map<String, LicenseMetadataDTO> licenseMetadataById)
  {
    Set<ApiLicenseLegalMetadataDTO> allLicenseLegalMetadata = new LinkedHashSet<>();
    Set<String> licenseIds = new HashSet<>();

    for (License license : multiLicenseToSingleLicense.values().stream()
        .flatMap(Collection::stream)
        .collect(Collectors.toSet())) {
      ApiLicenseLegalMetadataDTO licenseLegalMetadata = new ApiLicenseLegalMetadataDTO();
      licenseLegalMetadata.licenseId = license.getId();
      licenseLegalMetadata.licenseName = license.getShortDisplayName();
      if (licenseMetadataById.containsKey(license.getId())) {
        LicenseMetadataDTO licenseMetadataDTO = licenseMetadataById.get(license.getId());
        licenseLegalMetadata.licenseText = licenseMetadataDTO.getLicenseText();
        licenseLegalMetadata.obligations = licenseMetadataDTO.getLicenseObligations();
        licenseLegalMetadata.threatGroup = licenseMetadataDTO.getLicenseThreatGroup();
      }
      allLicenseLegalMetadata.add(licenseLegalMetadata);
      licenseIds.add(license.getId());
    }

    for (Entry<ApiLicenseDTO, Set<License>> e : multiLicenseToSingleLicense.entrySet()) {
      if (licenseIds.add(e.getKey().licenseId)) {
        ApiLicenseLegalMetadataDTO licenseLegalMetadata = new ApiLicenseLegalMetadataDTO();
        licenseLegalMetadata.licenseId = e.getKey().licenseId;
        licenseLegalMetadata.licenseName = e.getKey().licenseName;
        licenseLegalMetadata.isMulti = true;
        licenseLegalMetadata.singleLicenseIds = e.getValue().stream().map(License::getId).collect(Collectors.toSet());
        allLicenseLegalMetadata.add(licenseLegalMetadata);
      }
    }
    return allLicenseLegalMetadata;
  }
}
