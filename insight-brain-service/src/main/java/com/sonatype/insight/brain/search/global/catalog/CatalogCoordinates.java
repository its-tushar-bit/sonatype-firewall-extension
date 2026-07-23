/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global.catalog;

import com.sonatype.insight.brain.guide.api.dto.GuideComponentDocument;

import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURLBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared coordinate rendering for the Global Search catalog legs. Both the results
 * ({@code GlobalSearchResultsCatalogClientImpl}) and suggest ({@code GlobalSearchSuggestCatalogClientImpl})
 * clients render a catalog component's identifier through this one helper so their id strings cannot
 * diverge — divergent coordinates would break case-sensitive BEST MATCH parity between {@code /results}
 * and {@code /suggest}.
 */
final class CatalogCoordinates
{
  private static final Logger log = LoggerFactory.getLogger(CatalogCoordinates.class);

  private CatalogCoordinates() {
  }

  /**
   * Renders a canonical Package URL ({@code pkg:<format>/<namespace>/<name>@<version>}) for a catalog
   * component hit via the shared {@link PackageURLBuilder}. Returns {@code null} when the format or name
   * is missing, or when the coordinate is malformed — such rows are dropped rather than serialized with
   * a bad coordinate.
   */
  static String coordinateOf(final GuideComponentDocument c) {
    if (c.format() == null || c.format().isBlank() || c.name() == null || c.name().isBlank()) {
      return null;
    }
    PackageURLBuilder builder = PackageURLBuilder.aPackageURL()
        .withType(c.format())
        .withName(c.name());
    if (c.namespace() != null && !c.namespace().isBlank()) {
      builder.withNamespace(c.namespace());
    }
    if (c.version() != null && !c.version().isBlank()) {
      builder.withVersion(c.version());
    }
    try {
      return builder.build().toString();
    }
    catch (MalformedPackageURLException e) {
      log.debug("Catalog global search produced a malformed purl for format {} name {}", c.format(), c.name());
      return null;
    }
  }
}
