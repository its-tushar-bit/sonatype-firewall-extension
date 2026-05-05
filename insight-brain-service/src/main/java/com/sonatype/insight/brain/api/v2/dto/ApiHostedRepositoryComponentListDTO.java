/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.List;

public class ApiHostedRepositoryComponentListDTO
{
  public List<ApiHostedRepositoryComponentDTO> components;

  public int totalCount;

  public int page;

  public int pageSize;

  public boolean hasNextPage;

  public String repositoryPublicId;
}
