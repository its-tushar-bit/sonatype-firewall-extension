/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.telemetry;

/**
 * One captured Guide usage event held in the buffer until the daily drain. Carries only the
 * already-hashed user id (the raw username is never buffered) and, for by-identifier lookups, the
 * component PURL or vulnerability ref id. Search operations carry a null identifier (count-only).
 *
 * <p>
 * {@code operationType} is the value emitted as the {@code operation_type} telemetry attribute &mdash;
 * for MCP this is the channel-derived {@code mcp_lookup}. {@code restOperationType} is the original
 * annotated operation kind (component / vulnerability / search), preserved so the identifier can be
 * keyed correctly ({@code purl} vs {@code vulnerability_id}) regardless of channel.
 */
public record GuideUsageEventRecord(
    long timestampMillis,
    String operationType, // GuideOperationType.value()
    GuideOperationType restOperationType,
    GuideChannel channel,
    String hashedUserId,
    String identifier)
{
}
