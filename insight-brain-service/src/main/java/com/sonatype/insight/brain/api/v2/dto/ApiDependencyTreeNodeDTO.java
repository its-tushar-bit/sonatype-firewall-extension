/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.List;

import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiDependencyTreeNodeDTO 
{
  private PackageUrlIdentifier packageUrl;

  private ApiComponentIdentifierDTOV2 componentIdentifier;

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private List<ApiDependencyTreeNodeDTO> children;

  public PackageUrlIdentifier getPackageUrl() {
    return packageUrl;
  }

  public void setPackageUrl(PackageUrlIdentifier packageUrl) {
    this.packageUrl = packageUrl;
  }

  public ApiComponentIdentifierDTOV2 getComponentIdentifier() {
    return componentIdentifier;
  }

  public void setComponentIdentifier(ApiComponentIdentifierDTOV2 componentIdentifier) {
    this.componentIdentifier = componentIdentifier;
  }

  public List<ApiDependencyTreeNodeDTO> getChildren() {
    return children;
  }

  public void setChildren(List<ApiDependencyTreeNodeDTO> children) {
    this.children = children;
  }
}
