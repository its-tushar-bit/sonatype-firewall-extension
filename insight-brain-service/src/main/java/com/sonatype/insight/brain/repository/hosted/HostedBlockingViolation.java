/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

/**
 * Single policy violation summary surfaced in a blocked-deployment response.
 * <p>
 * Intentionally a narrow projection of the internal violation model: NXRM only needs enough
 * detail to render an actionable error message to the developer and echo it into logs/audit.
 * Full violation structure is not leaked over the API to keep the surface stable.
 * <p>
 * <b>Null contract:</b> all four String fields are nullable. The mapper that produces this
 * record (PR-2's {@code HostedEvaluationResultMapper}) populates them best-effort from the
 * internal violation model and may emit {@code null} for any field that is not available in
 * the underlying {@code PolicyAlert} / {@code ComponentFact} / {@code ConstraintFact} (e.g.
 * {@code componentIdentifier} when the scanner could not produce a PURL, or {@code reason}
 * when a constraint has no human-readable message configured). NXRM-side rendering must
 * tolerate any of these being {@code null}; the JSON serialiser emits them as JSON {@code null}.
 *
 * @param policyName Human-readable policy name (e.g. "Critical Security Policy"); nullable.
 * @param constraintName Constraint within the policy that matched (e.g. "Critical CVSS"); nullable.
 * @param reason Human-readable reason (e.g. "Component contains a critical security vulnerability."); nullable.
 * @param componentIdentifier PURL-style identifier of the violating component; nullable.
 */
public record HostedBlockingViolation(
    String policyName,
    String constraintName,
    String reason,
    String componentIdentifier)
{
}
