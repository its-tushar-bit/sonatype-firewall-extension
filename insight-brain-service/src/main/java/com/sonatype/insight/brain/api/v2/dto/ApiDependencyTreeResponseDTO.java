/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

public class ApiDependencyTreeResponseDTO
{
  private ApiDependencyTreeNodeDTO dependencyTree;

  public ApiDependencyTreeResponseDTO(ApiDependencyTreeNodeDTO dependencyTree) {
    this.dependencyTree = dependencyTree;
  }

  public ApiDependencyTreeResponseDTO() {
    this.dependencyTree = new ApiDependencyTreeNodeDTO();
  }

  public ApiDependencyTreeNodeDTO getDependencyTree() {
    return dependencyTree;
  }

  public void setDependencyTree(ApiDependencyTreeNodeDTO dependencyTree) {
    this.dependencyTree = dependencyTree;
  }
}
