/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

public class ApiVirtualRepositoryManagerDTO
{
  public String id;

  public String name;

  public long childRepositoryCount;

  public ApiVirtualRepositoryManagerDTO() {
  }

  public ApiVirtualRepositoryManagerDTO(final String id, final String name, final long childRepositoryCount) {
    this.id = id;
    this.name = name;
    this.childRepositoryCount = childRepositoryCount;
  }
}
