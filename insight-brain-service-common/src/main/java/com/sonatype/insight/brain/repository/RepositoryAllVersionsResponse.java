/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;

public class RepositoryAllVersionsResponse
{
  private final List<ComponentIdentifier> versions;

  public List<ComponentIdentifier> getVersions() {
    return versions;
  }

  public RepositoryAllVersionsResponse(List<ComponentIdentifier> versions) {
    this.versions = versions;
  }
}
