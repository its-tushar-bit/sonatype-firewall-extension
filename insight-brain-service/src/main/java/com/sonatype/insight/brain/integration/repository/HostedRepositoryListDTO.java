/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.util.List;

public class HostedRepositoryListDTO
{
  public ManagerInfo manager;

  public List<HostedRepositoryDTO> repositories;

  public int totalCount;

  public static class ManagerInfo
  {
    public String name;

    public String instanceId;

    public String baseUrl;
  }
}
