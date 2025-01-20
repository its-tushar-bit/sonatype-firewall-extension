/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

// JPA representation of the repository_manager_ancestor VIEW. This allows the view to be joined on in JPQL queries
@Entity
@Table(name = "repository_manager_ancestor")
public class RepositoryManagerAncestor
    extends AncestorView
{
  @Column(name = "repository_manager_id")
  private String id;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }
}
