/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

// JPA representation of the owner_ancestor VIEW. This allows the view to be joined on in JPQL queries
@Entity
@Table(name = "owner_ancestor")
public class OwnerAncestor
    extends AncestorView
{
  @Column(name = "owner_id")
  private String id;

  @Column(name = "owner_type")
  @Enumerated(EnumType.STRING)
  private OwnerType ownerType;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public OwnerType getOwnerType() {
    return ownerType;
  }

  public void setAncestorId(OwnerType ownerType) {
    this.ownerType = ownerType;
  }
}
