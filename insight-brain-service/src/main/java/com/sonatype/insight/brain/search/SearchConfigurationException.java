/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

/**
 * Exception thrown when search configuration is invalid or incomplete.
 */
public class SearchConfigurationException
    extends RuntimeException
{
  public SearchConfigurationException(final String message) {
    super(message);
  }
}
