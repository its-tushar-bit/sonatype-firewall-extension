/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @since 1.75
 */
public class ApiThirdPartyScanTicketDTO
{
  public String statusUrl;

  @JsonIgnore
  public String requestId;
}
