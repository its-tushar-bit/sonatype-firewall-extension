/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

/**
 * One group in a ranked result.
 *
 * @param groupValue the group's value, lower-cased to match the keyword normalizer both backends apply
 * @param metricValue the maximum metric across the group's documents, or {@code null} when no
 *          document in the group carried the metric
 */
public record RankedGroup(String groupValue, Float metricValue)
{
}
