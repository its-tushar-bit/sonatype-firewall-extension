/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.Date;

/**
 * Analyze response — reads CURRENT health + estate (+ optional generation pointers) in a constant
 * number of round trips.
 */
public class ApiSearchIndexAnalyzeDTO
{
  public String healthStatus;

  public String recommendedOp;

  public long queueLagSeconds;

  public long pendingChangeCount;

  public long failedChangeCount;

  public String nouxUnlockState;

  public String activeJobId;

  public String servingGenerationId;

  public String buildingGenerationId;

  public Date lastSuccessfulCutoverAt;

  public Date lastCleanupAt;

  public long applicationCount;

  public long violationCount;

  public Long componentCount;

  public Integer etaLowMinutes;

  public Integer etaHighMinutes;

  /**
   * Whether a built index is live. Tracks the estate snapshot column of the same name, which
   * follows the serving generation rather than the Advanced Search setting. For that setting, read
   * {@code GET /rest/search/advanced/status}.
   */
  public boolean advancedSearchEnabled;

  public Date estateCapturedAt;
}
