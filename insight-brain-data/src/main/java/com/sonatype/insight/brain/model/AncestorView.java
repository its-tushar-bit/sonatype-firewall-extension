/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;

/**
 * Superclass of entities for the *_ancestor views, which all have ancestor_id, ancestor_type, and ancestor_distance
 * fields
 */
@MappedSuperclass
public abstract class AncestorView
    extends Ancestor
{
  @Column(name = "ancestor_type")
  @Enumerated(EnumType.STRING)
  private OwnerType ancestorType;

  public OwnerType getAncestorType() {
    return ancestorType;
  }

  public void setAncestorType(OwnerType ancestorType) {
    this.ancestorType = ancestorType;
  }
}
