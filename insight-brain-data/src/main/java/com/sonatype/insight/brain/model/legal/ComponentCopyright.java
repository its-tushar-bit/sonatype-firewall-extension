/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.legal;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.HasComponentId;
import com.sonatype.insight.brain.model.HasOwnerId;
import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.105
 */
@Entity
@Table(name = "component_copyright")
public class ComponentCopyright
    extends HasComponentId
    implements HasStringId, HasOwnerId
{
  @Id
  @Column(name = "component_copyright_id")
  private String id;

  @Column(name = "owner_id")
  private String ownerId;

  @Column(name = "legal_content_hash")
  private String legalContentHash;

  // Note: this is not the ideal/model solution for auditing (i.e. retrieving audit data), rather an interim solution
  @Column(name = "last_updated_by_username")
  private String lastUpdatedByUsername;

  @Column(name = "last_updated_at")
  private Date lastUpdatedAt;

  public ComponentCopyright() {
  }

  public ComponentCopyright(
      ComponentIdentifier componentIdentifier,
      String ownerId,
      String legalContentHash,
      String lastUpdatedByUsername)
  {
    setComponentIdentifier(componentIdentifier);
    this.ownerId = ownerId;
    this.legalContentHash = legalContentHash;
    this.lastUpdatedByUsername = lastUpdatedByUsername;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  @Override
  public String getOwnerId() {
    return ownerId;
  }

  @Override
  public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
  }

  public String getLegalContentHash() {
    return legalContentHash;
  }

  public String getLastUpdatedByUsername() {
    return lastUpdatedByUsername;
  }

  public void setLastUpdatedByUsername(String lastUpdatedByUsername) {
    this.lastUpdatedByUsername = lastUpdatedByUsername;
  }

  public Date getLastUpdatedAt() {
    return lastUpdatedAt;
  }

  public void setLastUpdatedAt(Date lastUpdatedAt) {
    this.lastUpdatedAt = lastUpdatedAt;
  }
}
