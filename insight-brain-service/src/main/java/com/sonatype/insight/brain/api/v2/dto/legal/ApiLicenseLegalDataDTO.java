/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.legal;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.sonatype.insight.brain.api.v2.dto.ApiLicenseThreatDTOV2;
import com.sonatype.insight.brain.model.legal.ComponentCopyright;
import com.sonatype.insight.brain.model.legal.ComponentLegalFile;

/**
 * @since 1.101
 */
public class ApiLicenseLegalDataDTO
{
  public List<String> declaredLicenses;

  public List<String> observedLicenses;

  public List<String> effectiveLicenses;

  public ApiLicenseThreatDTOV2 highestEffectiveLicenseThreatGroup;

  public List<ApiLicenseLegalCopyrightDTO> copyrights;

  public List<ApiLicenseLegalFileDTO> licenseFiles;

  public List<ApiLicenseLegalFileDTO> noticeFiles;

  public List<ApiLicenseLegalObligationDTO> obligations;

  public List<ComponentObligationAttributionDTO> attributions;

  public Set<LegalSourceLinkDTO> sourceLinks;

  public String effectiveLicenseStatus;

  /**
   * Persisted {@link ComponentCopyright} identifier associated with this component, if any.
   */
  public String componentCopyrightId;

  /**
   * Internal owner ID of the {@link ComponentCopyright} scope.
   */
  public String componentCopyrightScopeOwnerId;

  public String componentCopyrightLastUpdatedByUsername;

  public Date componentCopyrightLastUpdatedAt;

  /**
   * Persisted {@link ComponentLegalFile} identifier associated with the licenses of this component, if any.
   */
  public String componentLicensesId;

  /**
   * Internal owner ID of the licenses {@link ComponentLegalFile} scope.
   */
  public String componentLicensesScopeOwnerId;

  public String componentLicensesLastUpdatedByUsername;

  public Date componentLicensesLastUpdatedAt;

  /**
   * Persisted {@link ComponentLegalFile} identifier associated with the notices of this component, if any.
   */
  public String componentNoticesId;

  /**
   * Internal owner ID of the notices {@link ComponentLegalFile} scope.
   */
  public String componentNoticesScopeOwnerId;

  public String componentNoticesLastUpdatedByUsername;

  public Date componentNoticesLastUpdatedAt;

  public ApiLicenseLegalDataDTO() {
    // for jackson
  }

  public ApiLicenseLegalDataDTO(
      final List<String> declaredLicenses,
      final List<String> observedLicenses,
      final List<String> effectiveLicenses,
      final ApiLicenseThreatDTOV2 highestEffectiveLicenseThreatGroup,
      final List<ApiLicenseLegalCopyrightDTO> copyrights,
      final List<ApiLicenseLegalFileDTO> licenseFiles,
      final List<ApiLicenseLegalFileDTO> noticeFiles,
      final List<ApiLicenseLegalObligationDTO> obligations,
      final List<ComponentObligationAttributionDTO> attributions,
      final Set<LegalSourceLinkDTO> sourceLinks,
      final String effectiveLicenseStatus,
      final String componentCopyrightId,
      final String componentCopyrightScopeOwnerId,
      final String componentCopyrightLastUpdatedByUsername,
      final Date componentCopyrightLastUpdatedAt,
      final String componentLicensesId,
      final String componentLicensesScopeOwnerId,
      final String componentLicensesLastUpdatedByUsername,
      final Date componentLicensesLastUpdatedAt,
      final String componentNoticesId,
      final String componentNoticesScopeOwnerId,
      final String componentNoticesLastUpdatedByUsername,
      final Date componentNoticesLastUpdatedAt)
  {
    this.declaredLicenses = declaredLicenses;
    this.observedLicenses = observedLicenses;
    this.effectiveLicenses = effectiveLicenses;
    this.highestEffectiveLicenseThreatGroup = highestEffectiveLicenseThreatGroup;
    this.copyrights = copyrights;
    this.licenseFiles = licenseFiles;
    this.noticeFiles = noticeFiles;
    this.obligations = obligations;
    this.attributions = attributions;
    this.sourceLinks = sourceLinks;
    this.effectiveLicenseStatus = effectiveLicenseStatus;
    this.componentCopyrightId = componentCopyrightId;
    this.componentCopyrightScopeOwnerId = componentCopyrightScopeOwnerId;
    this.componentCopyrightLastUpdatedByUsername = componentCopyrightLastUpdatedByUsername;
    this.componentCopyrightLastUpdatedAt = componentCopyrightLastUpdatedAt;
    this.componentLicensesId = componentLicensesId;
    this.componentLicensesScopeOwnerId = componentLicensesScopeOwnerId;
    this.componentLicensesLastUpdatedByUsername = componentLicensesLastUpdatedByUsername;
    this.componentLicensesLastUpdatedAt = componentLicensesLastUpdatedAt;
    this.componentNoticesId = componentNoticesId;
    this.componentNoticesScopeOwnerId = componentNoticesScopeOwnerId;
    this.componentNoticesLastUpdatedByUsername = componentNoticesLastUpdatedByUsername;
    this.componentNoticesLastUpdatedAt = componentNoticesLastUpdatedAt;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiLicenseLegalDataDTO that = (ApiLicenseLegalDataDTO) o;
    return Objects.equals(declaredLicenses, that.declaredLicenses) &&
        Objects.equals(observedLicenses, that.observedLicenses) &&
        Objects.equals(effectiveLicenses, that.effectiveLicenses) &&
        Objects.equals(highestEffectiveLicenseThreatGroup, that.highestEffectiveLicenseThreatGroup) &&
        Objects.equals(copyrights, that.copyrights) &&
        Objects.equals(licenseFiles, that.licenseFiles) &&
        Objects.equals(noticeFiles, that.noticeFiles) &&
        Objects.equals(obligations, that.obligations) &&
        Objects.equals(attributions, that.attributions) &&
        Objects.equals(sourceLinks, that.sourceLinks) &&
        Objects.equals(effectiveLicenseStatus, that.effectiveLicenseStatus) &&
        Objects.equals(componentCopyrightId, that.componentCopyrightId) &&
        Objects.equals(componentCopyrightScopeOwnerId, that.componentCopyrightScopeOwnerId) &&
        Objects.equals(componentCopyrightLastUpdatedByUsername, that.componentCopyrightLastUpdatedByUsername) &&
        Objects.equals(componentCopyrightLastUpdatedAt, that.componentCopyrightLastUpdatedAt) &&
        Objects.equals(componentLicensesId, that.componentLicensesId) &&
        Objects.equals(componentLicensesScopeOwnerId, that.componentLicensesScopeOwnerId) &&
        Objects.equals(componentLicensesLastUpdatedByUsername, that.componentLicensesLastUpdatedByUsername) &&
        Objects.equals(componentLicensesLastUpdatedAt, that.componentLicensesLastUpdatedAt) &&
        Objects.equals(componentNoticesId, that.componentNoticesId) &&
        Objects.equals(componentNoticesScopeOwnerId, that.componentNoticesScopeOwnerId) &&
        Objects.equals(componentNoticesLastUpdatedByUsername, that.componentNoticesLastUpdatedByUsername) &&
        Objects.equals(componentNoticesLastUpdatedAt, that.componentNoticesLastUpdatedAt);
  }

  @Override
  public int hashCode() {
    return Objects
        .hash(declaredLicenses, observedLicenses, effectiveLicenses, highestEffectiveLicenseThreatGroup, copyrights,
            licenseFiles, noticeFiles, obligations, attributions, sourceLinks, effectiveLicenseStatus,
            componentCopyrightId, componentCopyrightScopeOwnerId, componentCopyrightLastUpdatedByUsername,
            componentCopyrightLastUpdatedAt, componentLicensesId, componentLicensesScopeOwnerId,
            componentLicensesLastUpdatedByUsername, componentLicensesLastUpdatedAt, componentNoticesId,
            componentNoticesScopeOwnerId, componentNoticesLastUpdatedByUsername, componentNoticesLastUpdatedAt);
  }
}
