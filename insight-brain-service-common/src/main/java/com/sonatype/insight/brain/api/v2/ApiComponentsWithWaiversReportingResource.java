/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.api.v2.dto.ApiComponentWaiversDTO;

/**
 * Resource for API Components with Waivers Report
 */
public interface ApiComponentsWithWaiversReportingResource
{
  ApiComponentWaiversDTO getComponentsWithWaivers(String format);
}
