/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sonatype.insight.brain.model.security.UserPrincipal;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Empty-result fallback implementation of {@link GlobalSearchSuggestIqLocalClient}. Returns no rows
 * for every call so the {@code /rest/search/suggest} endpoint still produces a valid
 * {@link SuggestResponse} (with empty IQ-local groups) when the live
 * {@link GlobalSearchSuggestIqLocalClientImpl} is not present.
 *
 * <p>
 * <b>Operational signal.</b> The first invocation emits a one-shot WARN so an operator who enables
 * Global Search without wiring a real IQ-local implementation sees the empty results were intentional.
 */
@Named
@Singleton
public class GlobalSearchSuggestIqLocalClientStub
    implements GlobalSearchSuggestIqLocalClient
{
  private static final Logger log = LoggerFactory.getLogger(GlobalSearchSuggestIqLocalClientStub.class);

  private final AtomicBoolean warned = new AtomicBoolean(false);

  @Override
  public List<SuggestRow> suggest(
      final String query,
      final List<SuggestItemType> types,
      final int perTypeLimit,
      final UserPrincipal principal)
  {
    if (warned.compareAndSet(false, true)) {
      log.warn("Suggest endpoint is wired against the empty-result IQ-local stub; "
          + "configure a real GlobalSearchSuggestIqLocalClient before enabling Global Search.");
    }
    // The stub honours the null-principal contract vacuously by always returning empty.
    return List.of();
  }
}
