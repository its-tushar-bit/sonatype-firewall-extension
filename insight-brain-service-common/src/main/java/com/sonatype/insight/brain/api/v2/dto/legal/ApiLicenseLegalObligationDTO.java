/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.legal;

import java.util.Date;

import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.model.legal.ComponentObligation;
import com.sonatype.insight.brain.model.legal.ObligationStatus;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class ApiLicenseLegalObligationDTO
{
  private String id;

  private String name;

  private ObligationStatus status;

  private String comment;

  private ApiComponentIdentifierDTOV2 componentIdentifier;

  private String packageUrl;

  private String ownerId;

  private Date lastUpdatedAt;

  private String lastUpdatedByUsername;

  public ApiLicenseLegalObligationDTO() {
    // for jackson
  }

  public ApiLicenseLegalObligationDTO(ComponentObligation componentObligation) {
    id = componentObligation.getId();
    name = componentObligation.getObligationName();
    status = componentObligation.getStatus();
    comment = componentObligation.getComment();
    componentIdentifier =
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentObligation.getComponentIdentifier());
    if (componentIdentifier.getFormat() != null && componentIdentifier.getCoordinates() != null) {
      packageUrl = PackageUrlIdentifier.toPackageUrl(componentIdentifier.toComponentIdentifier());
    }
    ownerId = componentObligation.getOwnerId();
    lastUpdatedAt = componentObligation.getLastUpdatedAt();
    lastUpdatedByUsername = componentObligation.getLastUpdatedByUsername();
  }

  public ApiLicenseLegalObligationDTO(
      String name,
      ObligationStatus status,
      String comment)
  {
    this.name = name;
    this.status = status;
    this.comment = comment;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ObligationStatus getStatus() {
    return status;
  }

  public void setStatus(ObligationStatus status) {
    this.status = status;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
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

  public void setPackageUrl(String packageUrl) {
    this.packageUrl = packageUrl;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
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
