/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.legal;

import java.util.Date;
import java.util.Objects;

import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.model.legal.ComponentObligationAttribution;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * @since 1.106
 */
@JsonInclude(Include.NON_NULL)
public class ComponentObligationAttributionDTO
{
  private String id;

  private ApiComponentIdentifierDTOV2 componentIdentifier;

  private String packageUrl;

  private String ownerId;

  @JsonInclude()
  private String obligationName;

  private String content;

  private Date lastUpdatedAt;

  private String lastUpdatedByUsername;

  public ComponentObligationAttributionDTO() {
    // for jackson
  }

  public ComponentObligationAttributionDTO(String id, String ownerId, String obligationName, String content) {
    this.id = id;
    this.ownerId = ownerId;
    this.content = content;
    this.obligationName = obligationName;
  }

  public ComponentObligationAttributionDTO(ComponentObligationAttribution componentObligationAttribution) {
    id = componentObligationAttribution.getId();
    ownerId = componentObligationAttribution.getOwnerId();
    componentIdentifier =
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentObligationAttribution.getComponentIdentifier());
    if (componentIdentifier.getFormat() != null && componentIdentifier.getCoordinates() != null) {
      packageUrl = PackageUrlIdentifier.toPackageUrl(componentIdentifier.toComponentIdentifier());
    }
    obligationName = componentObligationAttribution.getObligationName();
    content = componentObligationAttribution.getContent();
    lastUpdatedAt = componentObligationAttribution.getLastUpdatedAt();
    lastUpdatedByUsername = componentObligationAttribution.getLastUpdatedByUsername();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public ApiComponentIdentifierDTOV2 getComponentIdentifier() {
    return componentIdentifier;
  }

  public void setComponentIdentifier(ApiComponentIdentifierDTOV2 componentIdentifier) {
    this.componentIdentifier = componentIdentifier;
  }

  public String getPackageUrl() {
    return packageUrl;
  }

  public void setPackageUrl(final String packageUrl) {
    this.packageUrl = packageUrl;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
  }

  public String getObligationName() {
    return obligationName;
  }

  public void setObligationName(String obligationName) {
    this.obligationName = obligationName;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public Date getLastUpdatedAt() {
    return lastUpdatedAt;
  }

  public void setLastUpdatedAt(Date lastUpdatedAt) {
    this.lastUpdatedAt = lastUpdatedAt;
  }

  public String getLastUpdatedByUsername() {
    return lastUpdatedByUsername;
  }

  public void setLastUpdatedByUsername(String lastUpdatedByUsername) {
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
    ComponentObligationAttributionDTO that = (ComponentObligationAttributionDTO) o;
    return Objects.equals(getId(), that.getId()) &&
        Objects.equals(getComponentIdentifier() == null ? null : getComponentIdentifier().toComponentIdentifier(),
            that.getComponentIdentifier() == null ? null : getComponentIdentifier().toComponentIdentifier())
        &&
        Objects.equals(getPackageUrl(), that.getPackageUrl()) &&
        Objects.equals(getOwnerId(), that.getOwnerId()) &&
        Objects.equals(getObligationName(), that.getObligationName()) &&
        Objects.equals(getContent(), that.getContent()) &&
        Objects.equals(getLastUpdatedAt(), that.getLastUpdatedAt()) &&
        Objects.equals(getLastUpdatedByUsername(), that.getLastUpdatedByUsername());
  }

  @Override
  public int hashCode() {
    return Objects
        .hash(getId(), getComponentIdentifier(), getPackageUrl(), getOwnerId(), getObligationName(), getContent(),
            getLastUpdatedAt(), getLastUpdatedByUsername());
  }

  @Override
  public String toString() {
    return "ComponentObligationAttributionDTO{" +
        "id='" + id + '\'' +
        ", componentIdentifier=" + componentIdentifier +
        ", packageUrl=" + packageUrl +
        ", ownerId='" + ownerId + '\'' +
        ", obligationName='" + obligationName + '\'' +
        ", content='" + content + '\'' +
        ", lastUpdatedAt=" + lastUpdatedAt +
        ", lastUpdatedByUsername='" + lastUpdatedByUsername + '\'' +
        '}';
  }
}
