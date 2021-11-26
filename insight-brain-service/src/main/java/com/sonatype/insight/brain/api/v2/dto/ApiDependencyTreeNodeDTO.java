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
  public ApiComponentIdentifierDTOV2 componentIdentifier;

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public List<ApiDependencyTreeNodeDTO> children;
}
