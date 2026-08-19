/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for bulk policy waiver requests
 *
 * @param violationIds List of policy violation IDs to create waivers for (max 1000, deduplicated automatically)
 * @param apiWaiverOptionsDTO Waiver options to apply to all violations
 */
@Schema(description = "Request to create bulk policy waivers for repository policy violations")
public record ApiBulkWaiversDTO(
    @Schema(
        description = "List of repository policy violation IDs to waive. Maximum 1000 violations per request. " +
            "Duplicate IDs are automatically deduplicated. Supports both quarantine (FAIL) and non-quarantine (WARN) violations. "
            +
            "Already-waived violations are skipped without error.",
        required = true,
        example = "[\"violation-id-1\", \"violation-id-2\", \"violation-id-3\"]",
        minLength = 1,
        maxLength = 1000) List<String> violationIds,

    @Schema(
        description = "Waiver options to apply to all violations in this bulk request",
        required = true) ApiWaiverOptionsDTO apiWaiverOptionsDTO)
{
}
