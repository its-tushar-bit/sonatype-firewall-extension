/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.List;

/**
 * DTO for bulk policy waiver requests
 *
 * @param violationIds List of policy violation IDs to create waivers for
 * @param apiWaiverOptionsDTO Waiver options to apply to all violations
 */
public record ApiBulkWaiversDTO(
    List<String> violationIds,
    ApiWaiverOptionsDTO apiWaiverOptionsDTO)
{
}
