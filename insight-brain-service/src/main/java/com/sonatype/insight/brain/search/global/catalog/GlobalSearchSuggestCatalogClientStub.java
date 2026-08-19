/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global.catalog;

import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Disabled fallback implementation of {@link GlobalSearchSuggestCatalogClient}. Reports
 * {@link #isEnabled()} as {@code false} and returns {@link CatalogSuggestResult#unavailable()} from
 * {@link #suggest(CatalogSuggestRequest)} so the suggest service reports {@code catalogAvailable:
 * false} whenever the live {@link GlobalSearchSuggestCatalogClientImpl} is not present.
 *
 * <p>
 * <b>Operational signal.</b> The first call to {@link #suggest} emits a one-shot WARN so an operator
 * who enables Global Search without a live catalog client sees the degrade was intentional.
 */
@Named
@Singleton
public class GlobalSearchSuggestCatalogClientStub
    implements GlobalSearchSuggestCatalogClient
{
  private static final Logger log = LoggerFactory.getLogger(GlobalSearchSuggestCatalogClientStub.class);

  private final AtomicBoolean warned = new AtomicBoolean(false);

  @Override
  public CatalogSuggestResult suggest(final CatalogSuggestRequest request) {
    if (warned.compareAndSet(false, true)) {
      log.warn("Suggest endpoint is wired against the disabled catalog-client stub; "
          + "configure a live catalog client before enabling Global Search catalog suggestions.");
    }
    return CatalogSuggestResult.unavailable();
  }

  @Override
  public boolean isEnabled() {
    return false;
  }
}
