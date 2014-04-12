/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.dto;

import java.util.List;

/**
 * @since 1.11.0
 */
public class ApiRoleMemberMappingListDTO
{
  private List<ApiRoleMemberMappingDTO> memberMappings;

  public ApiRoleMemberMappingListDTO() {
  }

  public List<ApiRoleMemberMappingDTO> getMemberMappings() {
    return memberMappings;
  }

  public void setMemberMappings(final List<ApiRoleMemberMappingDTO> memberMappings) {
    this.memberMappings = memberMappings;
  }
}
