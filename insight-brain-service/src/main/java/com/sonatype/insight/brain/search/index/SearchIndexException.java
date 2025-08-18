/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

public class SearchIndexException
    extends RuntimeException
{
  public SearchIndexException(final Exception e) {
    super(e);
  }

  public SearchIndexException(final String message, final Exception e) {
    super(message, e);
  }
}
