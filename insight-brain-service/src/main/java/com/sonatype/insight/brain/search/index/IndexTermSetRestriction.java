/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Budget-exempt restriction of an index read to a known set of values on one field (Lucene
 * {@code TermInSetQuery} / OpenSearch {@code terms}). Used for page hydration, component-hash
 * filters, and organization/application scope (CLM-44783).
 *
 * @param field indexed field name (e.g. {@code vulnerabilityId}, {@code organizationId})
 * @param ids values to match; blanks are dropped and remaining values lower-cased by the client
 */
public record IndexTermSetRestriction(String field, Collection<String> ids)
    implements IndexFilterRestriction
{
  public IndexTermSetRestriction {
    Objects.requireNonNull(field, "field");
    ids = ids == null ? List.of() : List.copyOf(ids);
  }

  public static IndexTermSetRestriction of(final String field, final Collection<String> ids) {
    return new IndexTermSetRestriction(field, ids);
  }

  public static List<IndexFilterRestriction> singleton(final String field, final Collection<String> ids) {
    return List.of(of(field, ids));
  }
}
