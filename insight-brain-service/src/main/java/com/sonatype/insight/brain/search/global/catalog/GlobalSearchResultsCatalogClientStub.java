/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global.catalog;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sonatype.insight.brain.search.global.ResultsRequest;
import com.sonatype.insight.brain.search.global.SectionResult;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Disabled fallback implementation of {@link GlobalSearchResultsCatalogClient}. Reports
 * {@link #isEnabled()} as {@code false} and returns {@link Optional#empty()} from
 * {@link #searchResults(ResultsRequest)} so the {@code /results} dispatcher degrades the catalog section
 * whenever the live {@link GlobalSearchResultsCatalogClientImpl} is not present.
 *
 * <p>
 * <b>Operational signal.</b> The first call to {@link #searchResults} emits a one-shot WARN log so an
 * operator who enables Global Search without a live catalog client sees the degrade was intentional.
 */
@Named
@Singleton
public class GlobalSearchResultsCatalogClientStub
    implements GlobalSearchResultsCatalogClient
{
  private static final Logger log = LoggerFactory.getLogger(GlobalSearchResultsCatalogClientStub.class);

  private final AtomicBoolean warned = new AtomicBoolean(false);

  @Override
  public Optional<SectionResult> searchResults(final ResultsRequest request) {
    if (warned.compareAndSet(false, true)) {
      log.warn("Results endpoint wired against the disabled catalog-client stub; "
          + "configure a live catalog client before enabling Global Search catalog results.");
    }
    return Optional.empty();
  }

  @Override
  public boolean isEnabled() {
    return false;
  }
}
