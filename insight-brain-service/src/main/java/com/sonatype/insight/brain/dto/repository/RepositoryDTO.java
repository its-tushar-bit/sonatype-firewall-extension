/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dto.repository;

import com.sonatype.insight.brain.model.repository.Repository;

public class RepositoryDTO
{
  public Long oldestEvalTimestamp;

  public String managerInstanceId;

  public String managerName;

  public String proxyUrl;

  public Repository repository;
}
