/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

/**
 * Thrown by {@link GlobalSearchResultsIqLocalClientStub} when no real IQ-local client bean is wired.
 * A dedicated type lets {@link GlobalSearchResultsIqLocalClientStubMapper} map exactly this failure to
 * HTTP 503 without inspecting message strings or rethrowing unrelated exceptions.
 */
class GlobalSearchNotConfiguredException
    extends RuntimeException
{
  GlobalSearchNotConfiguredException(final String message) {
    super(message);
  }
}
