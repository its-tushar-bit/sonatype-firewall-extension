/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.dto.policy;

/**
 * Tri-state policy compliance level backing the Guide compliance badge (green check / amber check /
 * red cross):
 * <ul>
 * <li>{@link #PASS} — no actionable violations (clean, or only notify-level actions);
 * <li>{@link #WARN} — compliant but worth attention: an active warn-action violation, or
 * violations that are entirely waived;
 * <li>{@link #FAIL} — non-compliant: an active fail-action violation.
 * </ul>
 * The companion {@code GuidePolicyCompliance.compliant} boolean is derived as
 * {@code complianceLevel != FAIL} — i.e. {@code true} for {@link #PASS} and {@link #WARN} (the
 * "check" states) and {@code false} for {@link #FAIL} (the "cross").
 */
public enum GuidePolicyComplianceLevel
{
  PASS,
  WARN,
  FAIL
}
