/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

/**
 * Cache key for integration version lookups.
 * Combines integration name with the configured supported version count.
 */
public record IntegrationVersionCacheKey(String name, int supportedVersionCount)
{
}
