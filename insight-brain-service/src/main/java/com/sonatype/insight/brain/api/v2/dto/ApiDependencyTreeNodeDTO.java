/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiDependencyTreeNodeDTO
{
  private String packageUrl;

  private ApiComponentIdentifierDTOV2 componentIdentifier;

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private List<ApiDependencyTreeNodeDTO> children;

  private boolean direct;

  public String getPackageUrl() {
    return packageUrl;
  }

  public void setPackageUrl(String packageUrl) {
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

  public boolean isDirect() {
    return direct;
  }

  public void setDirect(boolean direct) {
    this.direct = direct;
  }
}
