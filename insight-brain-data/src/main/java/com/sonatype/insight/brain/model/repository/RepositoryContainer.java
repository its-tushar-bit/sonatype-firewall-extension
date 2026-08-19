/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.repository;

import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.model.HasStringId;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "repository_container")
public class RepositoryContainer
    implements HasStringId, Owner
{
  public static final String REPOSITORY_CONTAINER_ID = "REPOSITORY_CONTAINER_ID";

  private static final String REPOSITORY_CONTAINER_NAME = "Repository Managers";

  @Id
  @Column(name = "repository_container_id")
  private String id;

  @Column(name = "related_organization_id")
  private String relatedOrganizationId;

  public static final RepositoryContainer SINGLETON = new RepositoryContainer();

  private RepositoryContainer() {
  }

  @Override
  public String getName() {
    return REPOSITORY_CONTAINER_NAME;
  }

  @Override
  public String getPublicId() {
    return getId();
  }

  @Override
  public String getParentOwnerId() {
    return Organization.ROOT_ORGANIZATION_ID;
  }

  @Override
  @JsonIgnore
  public boolean canHaveChildren() {
    return true;
  }

  @Override
  @JsonIgnore
  public OwnerType getType() {
    return OwnerType.REPOSITORY_CONTAINER;
  }

  @Override
  public String getId() {
    return REPOSITORY_CONTAINER_ID;
  }

  @Override
  public void setId(String id) {
    throw new UnsupportedOperationException("Cannot set the ID of the Repository Container");
  }

  public String getRelatedOrganizationId() {
    return relatedOrganizationId;
  }

  public void setRelatedOrganizationId(final String relatedOrganizationId) {
    this.relatedOrganizationId = relatedOrganizationId;
  }
}
