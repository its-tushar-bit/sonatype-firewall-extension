/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Response DTO for cascade re-evaluation request containing the status URL for tracking progress.
 *
 * @since 1.196
 */
@JsonInclude(Include.NON_NULL)
public class CascadeReevaluateTicketDTO
{
  /**
   * URL endpoint to check the status of the cascade re-evaluation request.
   */
  public String statusUrl;
}
