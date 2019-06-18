/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * @since 1.13.0
 */
public class ApiComponentDTOV2
{
  @JsonInclude(Include.NON_NULL)
  public String packageUrl;

  public String hash;

  public ApiComponentIdentifierDTOV2 componentIdentifier;

  @JsonInclude(Include.NON_NULL)
  public Boolean proprietary = false;
}
