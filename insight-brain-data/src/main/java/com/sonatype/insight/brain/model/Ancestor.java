/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

/**
 * Superclass of OrganizationAncestor (a table) and the various *Ancestor VIEWs, which all have these fields in
 * common
 */
@MappedSuperclass
public abstract class Ancestor
{
  public Ancestor() {
  }

  public Ancestor(String ancestorId, int ancestorDistance) {
    this.ancestorId = ancestorId;
    this.ancestorDistance = ancestorDistance;
  }

  @Column(name = "ancestor_id")
  private String ancestorId;

  @Column(name = "ancestor_distance")
  private int ancestorDistance;

  public String getAncestorId() {
    return ancestorId;
  }

  public void setAncestorId(String ancestorId) {
    this.ancestorId = ancestorId;
  }

  public int getAncestorDistance() {
    return ancestorDistance;
  }

  public void setAncestorDistance(int ancestorDistance) {
    this.ancestorDistance = ancestorDistance;
  }
}
