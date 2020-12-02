/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiPullRequestResults;
import com.sonatype.insight.brain.model.OwnerType;

/**
 * Resource for API Source Control Metrics
 */
public interface ApiSourceControlMetricsResource
{
  ApiPullRequestResults getSourceControl(OwnerType ownerType, String internalOwnerId);
}
