/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response body returned by the synchronous hosted-repository policy-enforcement path.
 * <p>
 * When NXRM POSTs a scan.xml upload with {@code evaluationMode=SYNCHRONOUS}, IQ evaluates the
 * component against the configured hosted-stage policy and returns this envelope in the
 * 200 OK response body. NXRM consults {@link #blocked()} to decide whether to commit the
 * artifact or reject the deployment with HTTP 403.
 * <p>
 * {@link #evaluationUrl()} is an absolute URL pointing to the Lifecycle UI view for the
 * evaluation (interim target is the hosted-repo components page; a per-evaluation report
 * page may replace it in a later release). It is populated on every sync response so
 * developers, AppSec, and audit teams share a stable source of truth, regardless of
 * whether the deployment was blocked.
 *
 * @param blocked {@code true} if any policy action is {@code FAIL}; NXRM should reject the upload.
 * @param policyAction Highest-severity action observed ({@code FAIL}, {@code WARN}, or {@code null} for none).
 * @param highestThreatLevel Highest policy threat level observed (0-10).
 * @param evaluationUrl Absolute URL to the Lifecycle UI for this evaluation, always populated.
 * @param blockingViolations Ordered list of the policy violations that caused the block; empty when not blocked.
 *          Wire-name on the JSON envelope is {@code policyViolations} for parity with NXRM's
 *          {@code IQEvaluationResponse} parser; the Java identifier {@code blockingViolations}
 *          reads more clearly internally and is rebound via {@link JsonProperty}.
 * @param correlationId The correlation ID NXRM supplied, echoed back for cross-system tracing.
 * @param componentId The IQ componentId for the uploaded component (for parity with the async response).
 */
public record HostedEvaluationResult(
    boolean blocked,
    String policyAction,
    int highestThreatLevel,
    String evaluationUrl,
    @JsonProperty("policyViolations") List<HostedBlockingViolation> blockingViolations,
    String correlationId,
    String componentId)
{
  public HostedEvaluationResult {
    // Defensive copy + null-safety. Filter out any null elements rather than letting
    // List.copyOf NPE: a partially-hydrated violation in the source list should not abort
    // the entire response and leave NXRM hanging (matches the CLM-37961 hardening pattern).
    blockingViolations = blockingViolations == null
        ? List.of()
        : blockingViolations.stream().filter(Objects::nonNull).collect(Collectors.toUnmodifiableList());
  }
}
