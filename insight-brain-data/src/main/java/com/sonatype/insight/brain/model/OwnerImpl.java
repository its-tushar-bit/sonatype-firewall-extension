/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.model;

// Owner is an abstract interface that can be implemented by entities such as Application and Organization
// This is a pure implementation for use when returning entries straight from the database as owners
public class OwnerImpl
    implements Owner
{
  private String id;

  private final String name;

  private final String publicId;

  private final String parentOwnerId;

  private final boolean haveChildren;

  private final OwnerType type;

  public OwnerImpl(
      final String publicId,
      final String name,
      final String parentOwnerId,
      final boolean haveChildren,
      final OwnerType type,
      final String id)
  {
    this.publicId = publicId;
    this.name = name;
    this.parentOwnerId = parentOwnerId;
    this.haveChildren = haveChildren;
    this.type = type;
    this.id = id;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getPublicId() {
    return publicId;
  }

  @Override
  public String getParentOwnerId() {
    return parentOwnerId;
  }

  @Override
  public boolean canHaveChildren() {
    return haveChildren;
  }

  @Override
  public OwnerType getType() {
    return type;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }
}
