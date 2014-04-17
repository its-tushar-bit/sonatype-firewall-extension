/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.dto;

import com.sonatype.insight.brain.model.security.MemberType;

/**
 * @since 1.11.0
 */
public class ApiMemberDTO
{
  public MemberType type;

  public String userOrGroupName;

  public ApiMemberDTO() {
  }

  public ApiMemberDTO(final String userOrGroupName, final MemberType type) {
    this.userOrGroupName = userOrGroupName;
    this.type = type;
  }
}
