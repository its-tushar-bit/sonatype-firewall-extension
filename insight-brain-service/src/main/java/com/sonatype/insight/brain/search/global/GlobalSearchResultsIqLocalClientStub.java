/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import java.util.Optional;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

/**
 * Fail-fast default implementation of {@link GlobalSearchResultsIqLocalClient}. Throwing at request time (rather than
 * returning empty) prevents an operator from silently serving empty Global Search responses when a real
 * IQ-local implementation has not been wired.
 *
 * <p>
 * The {@link GlobalSearchNotConfiguredException} thrown here is mapped to HTTP 503 by
 * {@link GlobalSearchResultsIqLocalClientStubMapper} so callers see a clear operational error instead of a hollow
 * results page.
 */
@Named
@Singleton
public class GlobalSearchResultsIqLocalClientStub
    implements GlobalSearchResultsIqLocalClient
{
  static final String MESSAGE = "Global Search requires a real GlobalSearchResultsIqLocalClient bean";

  @Override
  public Optional<SectionResult> searchNative(final ResultsRequest request) {
    throw new GlobalSearchNotConfiguredException(MESSAGE);
  }
}
