/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

/**
 * Resolves the ids of all relevant authorization contexts from a given entity or id.
 *
 * @since 1.7
 */
interface ContextIdResolver<T>
{
  Iterable<String> resolveContextIds(T object);
}
