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
  private MemberType type;

  private String userOrGroupName;

  public ApiMemberDTO() {
  }

  public ApiMemberDTO(final String userOrGroupName, final MemberType type) {
    this.userOrGroupName = userOrGroupName;
    this.type = type;
  }

  public MemberType getType() {
    return type;
  }

  public void setType(final MemberType type) {
    this.type = type;
  }

  public String getUserOrGroupName() {
    return userOrGroupName;
  }

  public void setUserOrGroupName(final String userOrGroupName) {
    this.userOrGroupName = userOrGroupName;
  }

}
