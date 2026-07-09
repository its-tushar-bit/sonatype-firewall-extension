/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.slo;

/**
 * Opaque continuation cursor for the SLO violation feed. Its field names deliberately match the request query
 * parameters a caller passes back to fetch the next slice: put {@code updatedSince} on the {@code updatedSince} param
 * and {@code afterViolationId} on the {@code afterViolationId} param.
 * <p>
 * {@code updatedSince} is the last returned row's sort key (epoch millis of the greatest of its open/waive/fix/legacy
 * times); paired with {@code afterViolationId} it forms the keyset position the next page continues strictly after.
 *
 * @param updatedSince epoch millis to pass back as the {@code updatedSince} request parameter
 * @param afterViolationId id to pass back as the {@code afterViolationId} request parameter
 */
public record SloViolationFeedCursor(
    long updatedSince,
    String afterViolationId)
{
}
