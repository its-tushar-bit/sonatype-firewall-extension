/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.owner;

import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;

public class OwnerDTO
{
  private String id;

  private String publicId;

  private String name;

  private OwnerType type;

  private String parentOwnerId;

  public OwnerDTO() {
    // For Jackson
  }

  public OwnerDTO(Owner owner) {
    id = owner.getId();
    publicId = owner.getPublicId();
    name = owner.getName();
    type = owner.getType();
    parentOwnerId = owner.getParentOwnerId();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getPublicId() {
    return publicId;
  }

  public void setPublicId(String publicId) {
    this.publicId = publicId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public OwnerType getType() {
    return type;
  }

  public void setType(OwnerType type) {
    this.type = type;
  }

  public String getParentOwnerId() {
    return parentOwnerId;
  }

  public void setParentOwnerId(String parentOwnerId) {
    this.parentOwnerId = parentOwnerId;
  }
}
