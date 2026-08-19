/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.license;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.brain.model.Nameable;
import com.sonatype.insight.model.HasStringId;

@Entity
@Table(name = "license_threat_group")
public class LicenseThreatGroup
    extends Nameable
    implements HasStringId
{
  @Id
  @Column(name = "license_threat_group_id")
  private String id;

  @Column(name = "owner_id")
  private String ownerId;

  @Column(name = "threat_level")
  private int threatLevel;

  public LicenseThreatGroup() {
  }

  public LicenseThreatGroup(String ownerId, String name, int threatLevel) {
    this.ownerId = ownerId;
    setName(name);
    this.threatLevel = threatLevel;
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

  public int getThreatLevel() {
    return threatLevel;
  }

  public void setThreatLevel(int threatLevel) {
    this.threatLevel = threatLevel;
  }
}
