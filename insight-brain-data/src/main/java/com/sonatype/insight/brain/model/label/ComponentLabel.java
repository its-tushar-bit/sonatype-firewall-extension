/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.label;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

@Entity
@Table(name = "component_label")
public class ComponentLabel
    implements HasStringId
{
  @Id
  @Column(name = "component_label_id")
  private String id;

  /**
   * @since 1.6
   */
  @Column(name = "owner_id")
  private String ownerId;

  @Column(name = "label_id")
  private String labelId;

  @Column(name = "hash")
  private String hash;

  public ComponentLabel() {
  }

  public ComponentLabel(String ownerId, String labelId, String hash) {
    this.ownerId = ownerId;
    this.labelId = labelId;
    this.hash = hash;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  /**
   * @since 1.6
   */
  public String getOwnerId() {
    return ownerId;
  }

  /**
   * @since 1.6
   */
  public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public String getLabelId() {
    return labelId;
  }

  public void setLabelId(String labelId) {
    this.labelId = labelId;
  }
}
