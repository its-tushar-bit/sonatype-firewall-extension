/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * One typeahead result row in the Global Search suggest response. {@code subtitle} may be empty but is
 * coerced from {@code null} to an empty string so the wire shape stays consistent.
 *
 * <p>
 * {@code href} is nullable and intentionally so: suggest rows stay within Lifecycle and emit no
 * catalog-outbound link. A row is never dropped for lacking an href. Because it is always null, it is
 * omitted from the wire shape ({@link JsonInclude}) rather than serialized as {@code "href": null}.
 */
@JsonInclude(Include.NON_NULL)
public record SuggestRow(
    String id,
    SuggestItemType type,
    SearchSource source,
    String title,
    String subtitle,
    String href)
{
  public SuggestRow {
    if (id == null || type == null || source == null || title == null) {
      throw new IllegalArgumentException("id, type, source, title must all be non-null");
    }
    subtitle = subtitle == null ? "" : subtitle;
  }
}
