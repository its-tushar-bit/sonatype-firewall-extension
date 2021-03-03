/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.legal;

import java.util.List;
import java.util.Objects;

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

  public List<ApiLicenseThreatDTOV2> effectiveLicenseThreats;

  public List<ApiLicenseLegalCopyrightDTO> copyrights;

  public List<ApiLicenseLegalFileDTO> licenseFiles;

  public List<ApiLicenseLegalFileDTO> noticeFiles;

  /**
   * Persisted {@link ComponentCopyright} identifier associated with this component, if any.
   */
  public String componentCopyrightId;

  /**
   * Internal owner ID of the {@link ComponentCopyright} scope.
   */
  public String componentCopyrightScopeOwnerId;

  /**
   * Persisted {@link ComponentLegalFile} identifier associated with this component, if any.
   */
  public String componentLegalFileId;

  /**
   * Internal owner ID of the {@link ComponentLegalFile} scope.
   */
  public String componentLegalFileScopeOwnerId;

  public ApiLicenseLegalDataDTO() {
    // for jackson
  }

  public ApiLicenseLegalDataDTO(
      final List<String> declaredLicenses,
      final List<String> observedLicenses,
      final List<String> effectiveLicenses,
      final List<ApiLicenseThreatDTOV2> effectiveLicenseThreats,
      final List<ApiLicenseLegalCopyrightDTO> copyrights,
      final List<ApiLicenseLegalFileDTO> licenseFiles,
      final List<ApiLicenseLegalFileDTO> noticeFiles,
      final String componentCopyrightId,
      final String componentCopyrightScopeOwnerId,
      final String componentLegalFileId,
      final String componentLegalFileScopeOwnerId)
  {
    this.declaredLicenses = declaredLicenses;
    this.observedLicenses = observedLicenses;
    this.effectiveLicenses = effectiveLicenses;
    this.effectiveLicenseThreats = effectiveLicenseThreats;
    this.copyrights = copyrights;
    this.licenseFiles = licenseFiles;
    this.noticeFiles = noticeFiles;
    this.componentCopyrightId = componentCopyrightId;
    this.componentCopyrightScopeOwnerId = componentCopyrightScopeOwnerId;
    this.componentLegalFileId = componentLegalFileId;
    this.componentLegalFileScopeOwnerId = componentLegalFileScopeOwnerId;
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
        Objects.equals(effectiveLicenseThreats, that.effectiveLicenseThreats) &&
        Objects.equals(copyrights, that.copyrights) &&
        Objects.equals(licenseFiles, that.licenseFiles) &&
        Objects.equals(noticeFiles, that.noticeFiles) &&
        Objects.equals(componentCopyrightId, that.componentCopyrightId) &&
        Objects.equals(componentCopyrightScopeOwnerId, that.componentCopyrightScopeOwnerId) &&
        Objects.equals(componentLegalFileId, that.componentLegalFileId) &&
        Objects.equals(componentLegalFileScopeOwnerId, that.componentLegalFileScopeOwnerId);
  }

  @Override
  public int hashCode() {
    return Objects
        .hash(declaredLicenses, observedLicenses, effectiveLicenses, effectiveLicenseThreats, copyrights, licenseFiles,
            noticeFiles, componentCopyrightId, componentCopyrightScopeOwnerId, componentLegalFileId,
            componentLegalFileScopeOwnerId);
  }
}
