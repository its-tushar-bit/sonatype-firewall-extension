/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import java.io.IOException;

/**
 * Thrown when the near-real-time {@link LuceneSearcherManagerHolder} cannot serve acquire requests
 * because it is paused or closed (for example during index rebuild or shutdown).
 */
public class SearcherManagerUnavailableException
    extends IOException
{
  public SearcherManagerUnavailableException(final String message) {
    super(message);
  }

  public SearcherManagerUnavailableException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
