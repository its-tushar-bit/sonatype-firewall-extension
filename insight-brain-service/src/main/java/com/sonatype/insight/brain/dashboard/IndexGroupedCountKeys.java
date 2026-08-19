/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Locale;

/**
 * Lookup keys for {@code countDistinctGroupedBy} result maps.
 * <p>
 * Both Lucene and OpenSearch key that map by the lowercased group value (keyword fields carry a
 * lowercase normalizer). Reading with a verbatim id silently reports zero for any group whose id
 * is not already lowercase.
 */
public final class IndexGroupedCountKeys
{
  private IndexGroupedCountKeys() {
  }

  public static String lookupKey(final String groupValue) {
    return groupValue.toLowerCase(Locale.ROOT);
  }
}
