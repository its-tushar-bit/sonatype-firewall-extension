/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration;

import com.sonatype.insight.model.HasStringId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @since 1.190
 */
@Entity
@Table(name = "cpe_matching_configuration")
public class CpeMatchingConfiguration
    implements HasStringId
{
  @Id
  @Column(name = "cpe_matching_configuration_id")
  private String id;

  @Column(name = "owner_id")
  private String ownerId;

  @Column(name = "cpe_enabled")
  private boolean cpeEnabled;

  @Column(name = "allow_override")
  private boolean allowOverride;

  public CpeMatchingConfiguration(final String ownerId) {
    this.ownerId = ownerId;
  }

  public CpeMatchingConfiguration(final String ownerId, final boolean cpeEnabled) {
    this.ownerId = ownerId;
    this.cpeEnabled = cpeEnabled;
  }

  public CpeMatchingConfiguration(final String ownerId, final boolean cpeEnabled, final boolean allowOverride) {
    this.ownerId = ownerId;
    this.cpeEnabled = cpeEnabled;
    this.allowOverride = allowOverride;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(final String ownerId) {
    this.ownerId = ownerId;
  }

  public boolean isCpeEnabled() {
    return cpeEnabled;
  }

  public void setCpeEnabled(final boolean cpeEnabled) {
    this.cpeEnabled = cpeEnabled;
  }

  public boolean isAllowOverride() {
    return allowOverride;
  }

  public void setAllowOverride(final boolean allowOverride) {
    this.allowOverride = allowOverride;
  }
}
