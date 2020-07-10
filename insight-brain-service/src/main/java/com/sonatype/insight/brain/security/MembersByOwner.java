/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.model.OwnerType;

public class MembersByOwner
{
  public String ownerId;

  public String ownerName;

  public OwnerType ownerType;

  public List<Member> members = new ArrayList<>();

  public MembersByOwner() {
  }

  public MembersByOwner(String ownerId, String ownerName, OwnerType ownerType) {
    this.ownerId = ownerId;
    this.ownerName = ownerName;
    this.ownerType = ownerType;
  }

  @Override
  public String toString() {
    return "ownerName=" + ownerName + "(id=" + ownerId + ", type=" + ownerType + "), members=" + members;
  }
}
