/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.applications;

import java.util.List;

import com.sonatype.insight.brain.search.index.IndexFilterRestriction;

/**
 * Applications list index read: string query plus budget-exempt scope term-set restrictions
 * (CLM-44783).
 */
record ApplicationsIndexQuery(String query, List<IndexFilterRestriction> termSets)
{
  ApplicationsIndexQuery {
    termSets = termSets == null ? List.of() : List.copyOf(termSets);
  }
}
