/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import javax.inject.Inject;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.search.SearchIndexRuleAnnotations.LuceneTest;
import com.sonatype.insight.brain.search.SearchIndexRuleAnnotations.OpenSearchHttpTest;
import com.sonatype.insight.brain.search.index.HybridSearchIndexClient;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.lucene.LuceneSearchIndexClient;
import com.sonatype.insight.brain.testing.BrainInjectedTest;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class SearchModuleTest
    extends BrainInjectedTest
{
  @Inject
  private SearchIndexClient searchIndexClient;

  @Test
  public void luceneDefaultTest() {
    assertThat(searchIndexClient).isInstanceOf(LuceneSearchIndexClient.class);
  }

  @Test
  @LuceneTest
  public void luceneSpecificTest() {
    assertThat(searchIndexClient).isInstanceOf(LuceneSearchIndexClient.class);
  }

  @Test
  @OpenSearchHttpTest
  public void openSearchHttpTest() {
    assertThat(searchIndexClient).isInstanceOf(HybridSearchIndexClient.class);
  }
}
