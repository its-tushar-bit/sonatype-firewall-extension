/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.search.indexquery.IndexQueryService;

/**
 * Stub {@link IndexQueryService} for {@link ResultsService} tests that never request facets. Any facet
 * call throws, so a test that unexpectedly reaches the facet path fails loudly instead of silently
 * returning no facets.
 */
final class UnusedIndexQueryServices
{
  private UnusedIndexQueryServices() {
  }

  static IndexQueryService throwOnUse() {
    IndexQueryService stub = mock(IndexQueryService.class);
    when(stub.facetsForResults(any(), any(), any()))
        .thenThrow(new AssertionError("facetsForResults must not be called by this test"));
    return stub;
  }
}
