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

public class RepositoryContainer
    implements HasStringId, Owner
{
  public static final String REPOSITORY_CONTAINER_ID = "REPOSITORY_CONTAINER_ID";

  private static final String REPOSITORY_CONTAINER_NAME = "Repository Managers";

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
}
