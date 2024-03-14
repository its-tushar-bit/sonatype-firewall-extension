/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.List;

public class RepositoryAllVersionsResponse
{
  private final List<RepositoryComponentResult> components;

  public List<RepositoryComponentResult> getComponents() {
    return components;
  }

  public RepositoryAllVersionsResponse(List<RepositoryComponentResult> components) {
    this.components = components;
  }
}
