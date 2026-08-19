/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import com.sonatype.insight.brain.model.security.MemberType;

/**
 * @since 1.11.0
 */
public class ApiMemberDTO
{
  public String ownerId;

  public String ownerType;

  public MemberType type;

  public String userOrGroupName;

  public ApiMemberDTO() {
  }

  public ApiMemberDTO(String ownerId, String ownerType, final String userOrGroupName, final MemberType type) {
    this.ownerId = ownerId;
    this.ownerType = ownerType;
    this.userOrGroupName = userOrGroupName;
    this.type = type;
  }
}
