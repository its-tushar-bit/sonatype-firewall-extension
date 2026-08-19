/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.components;

import java.util.List;

import com.sonatype.insight.brain.search.index.IndexFilterRestriction;

/**
 * Components list index read: string query plus budget-exempt term-set restrictions (e.g.
 * {@code componentHash} filters, org/app scope).
 */
record ComponentsIndexQuery(String query, List<IndexFilterRestriction> termSets)
{
  ComponentsIndexQuery {
    termSets = termSets == null ? List.of() : List.copyOf(termSets);
  }
}
