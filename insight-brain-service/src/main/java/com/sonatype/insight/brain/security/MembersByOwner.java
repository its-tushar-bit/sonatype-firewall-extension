/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.ArrayList;
import java.util.List;

public class MembersByOwner
{
  public String ownerId;

  public String ownerName;

  public String ownerType;

  public List<Member> members = new ArrayList<Member>();

  public MembersByOwner() {
  }

  public MembersByOwner(String ownerId, String ownerName, String ownerType) {
    this.ownerId = ownerId;
    this.ownerName = ownerName;
    this.ownerType = ownerType;
  }
}
