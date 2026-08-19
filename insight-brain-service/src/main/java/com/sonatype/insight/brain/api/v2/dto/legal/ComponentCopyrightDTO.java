/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.legal;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.model.legal.ComponentCopyright;
import com.sonatype.insight.purl.PackageUrlIdentifier;

/**
 * @since 1.105
 */
public class ComponentCopyrightDTO
{
  private String id;

  private ApiComponentIdentifierDTOV2 componentIdentifier;

  private String packageUrl;

  private List<CopyrightOverrideDTO> copyrightOverrides = new ArrayList<>();

  private Date lastUpdatedAt;

  private String lastUpdatedByUsername;

  public ComponentCopyrightDTO() {
  }

  public ComponentCopyrightDTO(
      final String id,
      final ApiComponentIdentifierDTOV2 componentIdentifier,
      final List<CopyrightOverrideDTO> copyrightOverrides,
      final String lastUpdatedByUsername,
      final Date lastUpdatedAt)
  {
    this.id = id;
    this.componentIdentifier = componentIdentifier;
    if (componentIdentifier.getFormat() != null && componentIdentifier.getCoordinates() != null) {
      packageUrl = PackageUrlIdentifier.toPackageUrl(componentIdentifier.toComponentIdentifier());
    }
    this.copyrightOverrides = copyrightOverrides;
    this.lastUpdatedByUsername = lastUpdatedByUsername;
    this.lastUpdatedAt = lastUpdatedAt;
  }

  public static ComponentCopyrightDTO fromComponentCopyright(
      final ComponentCopyright componentCopyright,
      final List<CopyrightOverrideDTO> copyrightOverrideDTOS)
  {
    return new ComponentCopyrightDTO(
        componentCopyright.getId(),
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentCopyright.getComponentIdentifier()),
        copyrightOverrideDTOS,
        componentCopyright.getLastUpdatedByUsername(),
        componentCopyright.getLastUpdatedAt());
  }

  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public ApiComponentIdentifierDTOV2 getComponentIdentifier() {
    return componentIdentifier;
  }

  public void setComponentIdentifier(final ApiComponentIdentifierDTOV2 componentIdentifier) {
    this.componentIdentifier = componentIdentifier;
  }

  public String getPackageUrl() {
    return packageUrl;
  }

  public void setPackageUrl(final String packageUrl) {
    this.packageUrl = packageUrl;
  }

  public List<CopyrightOverrideDTO> getCopyrightOverrides() {
    return copyrightOverrides;
  }

  public void setCopyrightOverrides(final List<CopyrightOverrideDTO> copyrightOverrides) {
    this.copyrightOverrides = copyrightOverrides;
  }

  public Date getLastUpdatedAt() {
    return lastUpdatedAt;
  }

  public void setLastUpdatedAt(final Date lastUpdatedAt) {
    this.lastUpdatedAt = lastUpdatedAt;
  }

  public String getLastUpdatedByUsername() {
    return lastUpdatedByUsername;
  }

  public void setLastUpdatedByUsername(final String lastUpdatedByUsername) {
    this.lastUpdatedByUsername = lastUpdatedByUsername;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ComponentCopyrightDTO that = (ComponentCopyrightDTO) o;
    return Objects.equals(getId(), that.getId()) &&
        Objects.equals(getComponentIdentifier(), that.getComponentIdentifier()) &&
        Objects.equals(getPackageUrl(), that.getPackageUrl()) &&
        Objects.equals(getCopyrightOverrides(), that.getCopyrightOverrides()) &&
        Objects.equals(getLastUpdatedAt(), that.getLastUpdatedAt()) &&
        Objects.equals(getLastUpdatedByUsername(), that.getLastUpdatedByUsername());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), getComponentIdentifier(), getPackageUrl(), getCopyrightOverrides(),
        getLastUpdatedAt(), getLastUpdatedByUsername());
  }
}
