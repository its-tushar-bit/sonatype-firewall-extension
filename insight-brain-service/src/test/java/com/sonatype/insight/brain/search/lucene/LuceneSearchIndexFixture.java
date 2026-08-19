/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import com.sonatype.insight.brain.search.SearchConfig;
import com.sonatype.insight.brain.search.SearchIndexFixture;
import com.sonatype.insight.brain.search.SearchIndexRuleAnnotations.LuceneTest;

public class LuceneSearchIndexFixture
    implements SearchIndexFixture
{
  @SuppressWarnings("unused")
  public LuceneSearchIndexFixture(final LuceneTest luceneTest) {
  }

  @Override
  public SearchConfig getSearchConfig() {
    // With legacy code a null SearchConfig means Lucene (which has no config)
    return null;
  }

  @Override
  public boolean isFixtureReusable() {
    return true;
  }

  @Override
  public void close() throws Exception {
  }
}
