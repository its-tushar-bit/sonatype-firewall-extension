/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.development.prioritization;

import com.sonatype.insight.brain.api.v2.dto.PrioritizedComponent;
import com.sonatype.insight.brain.api.v2.dto.ApiPageResult;

public record DevelopmentPrioritizationResults(
    String scanIdFromLatestBuildStageEvaluation,
    boolean hasAutoWaiversConfigured,
    ApiPageResult<PrioritizedComponent> priorities)
{
}
