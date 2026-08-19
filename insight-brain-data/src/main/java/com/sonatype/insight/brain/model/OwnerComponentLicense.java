/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import com.sonatype.insight.model.HasStringId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Association between components in applications scans and licenses.
 *
 * @since 1.104
 */
@Entity
@Table(name = "owner_component_license")
public class OwnerComponentLicense
    implements HasStringId
{
  @Id
  @Column(name = "owner_component_license_id")
  private String id;

  @Column(name = "owner_component_id")
  private String ownerComponentId;

  @Column(name = "effective_license_id")
  private String effectiveLicenseId;

  public OwnerComponentLicense() {
  }

  public OwnerComponentLicense(String ownerComponentId, String effectiveLicenseId) {
    this.ownerComponentId = ownerComponentId;
    this.effectiveLicenseId = effectiveLicenseId;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getOwnerComponentId() {
    return ownerComponentId;
  }

  public void setOwnerComponentId(String ownerComponentId) {
    this.ownerComponentId = ownerComponentId;
  }

  public String getEffectiveLicenseId() {
    return effectiveLicenseId;
  }

  public void setEffectiveLicenseId(String effectiveLicenseId) {
    this.effectiveLicenseId = effectiveLicenseId;
  }

  @Override
  public String toString() {
    return "OwnerComponentLicense [ownerComponentId=" + ownerComponentId + ", effectiveLicenseId="
        + effectiveLicenseId + "]";
  }
}
