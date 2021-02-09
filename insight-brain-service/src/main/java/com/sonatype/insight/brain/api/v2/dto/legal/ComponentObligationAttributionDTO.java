/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.legal;

import java.util.Date;

import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.model.legal.ComponentObligationAttribution;

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

  private String ownerId;

  private String obligationName;

  private String content;

  private Date lastUpdatedAt;

  private String lastUpdatedByUsername;

  public ComponentObligationAttributionDTO() {
    // for jackson
  }

  public ComponentObligationAttributionDTO(String id, String ownerId, String content) {
    this.id = id;
    this.ownerId = ownerId;
    this.content = content;
  }

  public ComponentObligationAttributionDTO(ComponentObligationAttribution componentObligationAttribution) {
    id = componentObligationAttribution.getId();
    ownerId = componentObligationAttribution.getOwnerId();
    componentIdentifier =
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentObligationAttribution.getComponentIdentifier());
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
}
