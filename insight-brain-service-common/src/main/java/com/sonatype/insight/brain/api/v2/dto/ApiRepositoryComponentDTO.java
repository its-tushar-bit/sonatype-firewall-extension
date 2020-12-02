/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.Date;

import com.sonatype.insight.json.store.ApiDateFormat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * @since 1.77
 */
@JsonIgnoreProperties("proprietary")
public class ApiRepositoryComponentDTO
    extends ApiComponentDTOV2
{
  @JsonInclude(Include.NON_EMPTY)
  public String quarantineId;

  @ApiDateFormat
  public Date quarantineTime;

  @JsonInclude(Include.NON_EMPTY)
  @ApiDateFormat
  public Date quarantineReleaseTime;
}
