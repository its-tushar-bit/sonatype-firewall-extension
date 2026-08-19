/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * @since 1.13.0
 */
public class ApiComponentDTOV2
{
  public String packageUrl;

  public String hash;

  @JsonInclude(Include.NON_NULL)
  public String sha256;

  public ApiComponentIdentifierDTOV2 componentIdentifier;

  @JsonInclude(Include.NON_NULL)
  public String displayName;

  @JsonInclude(Include.NON_NULL)
  public Boolean proprietary = false;

  @JsonInclude(Include.NON_NULL)
  public Boolean thirdParty;

  @JsonIgnore
  public Integer breakingChangesCount;

  @JsonInclude(Include.NON_NULL)
  public String originalPurl;
}
