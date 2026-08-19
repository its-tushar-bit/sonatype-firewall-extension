/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import com.sonatype.insight.model.HasStringId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "organization_ancestor")
public class OrganizationAncestor
    extends Ancestor
    implements HasStringId
{
  @Id
  @Column(name = "organization_ancestor_id")
  private String id;

  @Column(name = "organization_id")
  private String organizationId;

  public OrganizationAncestor() {
  }

  public OrganizationAncestor(String organizationId, String ancestorId, int ancestorDistance) {
    super(ancestorId, ancestorDistance);
    this.organizationId = organizationId;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getOrganizationId() {
    return organizationId;
  }

  public void setOrganizationId(String organizationId) {
    this.organizationId = organizationId;
  }
}
