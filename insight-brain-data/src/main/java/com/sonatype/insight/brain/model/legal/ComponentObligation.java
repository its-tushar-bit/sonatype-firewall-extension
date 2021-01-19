/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.legal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.HasComponentId;
import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.105
 */
@Entity
@Table(name = "component_obligation")
public class ComponentObligation
    extends HasComponentId
    implements HasStringId
{
  @Id
  @Column(name = "component_obligation_id")
  private String id;

  @Column(name = "owner_id")
  private String ownerId;

  @Column(name = "obligation_name")
  private String obligationName;

  @Column(name = "comment")
  private String comment;

  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private ObligationStatus status;

  @Column(name = "legal_content_hash")
  private String legalContentHash;

  public ComponentObligation() {
  }

  public ComponentObligation(
      ComponentIdentifier componentIdentifier,
      String ownerId,
      String obligationName,
      String comment,
      ObligationStatus status,
      String legalContentHash)
  {
    setComponentIdentifier(componentIdentifier);
    this.ownerId = ownerId;
    this.obligationName = obligationName;
    this.comment = comment;
    this.status = status;
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

  public String getObligationName() {
    return obligationName;
  }

  public void setObligationName(String obligationName) {
    this.obligationName = obligationName;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public ObligationStatus getStatus() {
    return status;
  }

  public void setStatus(ObligationStatus status) {
    this.status = status;
  }

  public String getLegalContentHash() {
    return legalContentHash;
  }
}
