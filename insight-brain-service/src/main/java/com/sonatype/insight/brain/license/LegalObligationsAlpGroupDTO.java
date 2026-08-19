/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

/**
 * One row in the ALP variant of the Legal Obligations dashboard tile (CLM-39604 / P1.5-D-2): a single
 * license-threat-group with its unreviewed-component count and its 30-day-over-prior-30-day license-category
 * violation trend percentage.
 *
 * <p>
 * {@code trendPct} is expressed as a signed percentage (e.g. {@code 25.0} means "+25% versus the prior 30-day
 * window", {@code -10.0} means "-10%"). It is {@code 0.0} when the prior window had zero violations (i.e. there
 * is no baseline to compare against) so that the UI does not display infinity / NaN.
 *
 * @since 1.205
 */
public record LegalObligationsAlpGroupDTO(String id, String name, long reviewCount, double trendPct)
{
}
