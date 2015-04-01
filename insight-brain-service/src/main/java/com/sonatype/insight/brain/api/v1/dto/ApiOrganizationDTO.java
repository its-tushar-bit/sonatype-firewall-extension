/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v1.dto;

import java.util.List;

/**
 * @since 1.11.0
 */
public class ApiOrganizationDTO
{
  public String id;

  public String name;

  public List<ApiTagDTO> tags;
}
