/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Body of a request to start an index job. Both fields are validated by the service rather than here,
 * so that a value outside the known set reads as a 400 with the offending value named.
 */
public class ApiSearchIndexJobRequestDTO
{
  @Schema(description = "Job to run: FULL_REBUILD or FIRST_TIME_INDEX.", example = "FULL_REBUILD")
  public String jobType;

  @Schema(description = "What asked for the job: UNLOCK_WIZARD, HEALTH_UI, SUPPORT or SYSTEM. "
      + "Defaults to HEALTH_UI.", example = "HEALTH_UI")
  public String trigger;
}
