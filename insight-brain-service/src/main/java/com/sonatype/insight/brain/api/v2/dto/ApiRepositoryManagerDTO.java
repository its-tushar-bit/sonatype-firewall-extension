/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import com.sonatype.insight.brain.model.repository.ManagerType;

public class ApiRepositoryManagerDTO
{
  public String id;

  public String name;

  public String instanceId;

  public String productName;

  public String productVersion;

  public ManagerType managerType;
}
