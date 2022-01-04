/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.legal;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.model.legal.ComponentLegalFile;
import com.sonatype.insight.brain.model.legal.LegalFileOverride;
import com.sonatype.insight.brain.model.legal.LegalFileType;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * @since 1.107
 */
@JsonInclude(Include.NON_NULL)
public class ComponentLegalFileDTO
{
  private String id;

  private ApiComponentIdentifierDTOV2 componentIdentifier;

  private String packageUrl;

  private String ownerId;

  private LegalFileType legalFileType;

  private List<LegalFileOverrideDTO> legalFileOverrides = new ArrayList<>();

  private Date lastUpdatedAt;

  private String lastUpdatedByUsername;

  public ComponentLegalFileDTO() {
    // for jackson
  }

  public ComponentLegalFileDTO(ComponentLegalFile componentLegalFile, List<LegalFileOverride> legalFileOverrides) {
    id = componentLegalFile.getId();
    componentIdentifier =
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentLegalFile.getComponentIdentifier());
    if (componentIdentifier.getFormat() != null && componentIdentifier.getCoordinates() != null) {
      packageUrl = PackageUrlIdentifier.toPackageUrl(componentIdentifier.toComponentIdentifier());
    }
    ownerId = componentLegalFile.getOwnerId();
    legalFileType = componentLegalFile.getType();
    this.legalFileOverrides = legalFileOverrides.stream().map(LegalFileOverrideDTO::new).collect(Collectors.toList());
    lastUpdatedAt = componentLegalFile.getLastUpdatedAt();
    lastUpdatedByUsername = componentLegalFile.getLastUpdatedByUsername();
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

  public LegalFileType getLegalFileType() {
    return legalFileType;
  }

  public void setLegalFileType(LegalFileType legalFileType) {
    this.legalFileType = legalFileType;
  }

  public List<LegalFileOverrideDTO> getLegalFileOverrides() {
    return legalFileOverrides;
  }

  public void setLegalFileOverrides(List<LegalFileOverrideDTO> legalFileOverrides) {
    this.legalFileOverrides = legalFileOverrides;
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
}
