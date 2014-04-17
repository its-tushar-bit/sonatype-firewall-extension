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
public class ApiOrganizationDTO
{
  private String id;

  private String name;

  private List<ApiTagDTO> tags;

  public ApiOrganizationDTO() {
  }

  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public List<ApiTagDTO> getTags() {
    return tags;
  }

  public void setTags(final List<ApiTagDTO> tags) {
    this.tags = tags;
  }
}
