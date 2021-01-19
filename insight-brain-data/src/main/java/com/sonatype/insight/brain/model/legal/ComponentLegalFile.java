/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.legal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.HasComponentId;
import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.105
 */
@Entity
@Table(name = "component_legal_file")
public class ComponentLegalFile
    extends HasComponentId
    implements HasStringId
{
  @Id
  @Column(name = "component_legal_file_id")
  private String id;

  @Column(name = "owner_id")
  private String ownerId;

  @Column(name = "legal_content_hash")
  private String legalContentHash;

  public ComponentLegalFile() {
  }

  public ComponentLegalFile(ComponentIdentifier componentIdentifier, String ownerId, String legalContentHash) {
    setComponentIdentifier(componentIdentifier);
    this.ownerId = ownerId;
    this.legalContentHash = legalContentHash;
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

  public String getLegalContentHash() {
    return legalContentHash;
  }
}
