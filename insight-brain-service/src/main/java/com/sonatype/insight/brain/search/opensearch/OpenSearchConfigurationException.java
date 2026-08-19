/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.opensearch;

import com.sonatype.insight.brain.search.SearchConfigurationException;

/**
 * Exception thrown when OpenSearch configuration is invalid or incomplete.
 */
public class OpenSearchConfigurationException
    extends SearchConfigurationException
{
  public OpenSearchConfigurationException(final String message) {
    super(message);
  }
}
