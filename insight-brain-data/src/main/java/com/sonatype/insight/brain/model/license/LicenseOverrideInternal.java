/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.license;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.HasComponentId;
import com.sonatype.insight.model.HasStringId;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The license of a component (identified by GAV) can be overridden at application or organization (i.e. owner) level.
 * If it is overridden at both application and organization levels, the application one wins.
 *
 * @since 1.13.0
 */
@Cacheable
@Entity
@Table(name = "license_override")
public class LicenseOverrideInternal
    extends HasComponentId
    implements HasStringId
{
  @Id
  @Column(name = "license_override_id")
  private String id;

  @Column(name = "owner_id")
  private String ownerId;

  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private LicenseOverrideStatus status;

  @Column(name = "comment")
  private String comment;

  public LicenseOverrideInternal() {
  }

  public LicenseOverrideInternal(
      String ownerId,
      ComponentIdentifier componentIdentifier,
      LicenseOverrideStatus status,
      String comment)
  {
    this.ownerId = ownerId;
    this.status = status;
    this.comment = comment;
    setComponentIdentifier(componentIdentifier);
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public LicenseOverrideStatus getStatus() {
    return status;
  }

  public void setStatus(LicenseOverrideStatus status) {
    this.status = status;
  }

  @Override
  public String toString() {
    return "LicenseOverride{" + "id='" + id + '\'' + ", ownerId='" + ownerId + '\'' + ", status=" + status
        + ", comment='" + comment + '\'' + ", componentIdentifier='" + getComponentIdentifier() + '\'' + '}';
  }
}
