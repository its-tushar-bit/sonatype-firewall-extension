/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

/**
 * Specifies the source of metadata for report entity operations.
 */
public enum MetadataSource
{
  /**
   * Fetch fresh metadata from the underlying storage system. This always performs a storage operation to get the most
   * current metadata.
   */
  FETCH,

  /**
   * Use the last cached metadata if available. For storage systems like S3, this avoids redundant metadata calls by
   * reusing previously fetched metadata. Falls back to FETCH behavior if no cached metadata exists.
   */
  CACHED
}
