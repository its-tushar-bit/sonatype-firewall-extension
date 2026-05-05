/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.search.SearchIndexRuleAnnotations.LuceneTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Category(SlowTest.class)
public class SearchIndexRuleAndFixturesTest
{
  @Rule
  public SearchIndexRule searchIndexRule = SearchIndexRule.getInstance(SearchIndexRuleAndFixturesTest.class);

  @Test
  @LuceneTest
  public void testSearch_DefaultIsLucene() {
    SearchConfig searchConfig = searchIndexRule.getSearchConfig();
    assertThat(searchConfig).isNull();
    // nothing else to assert here as the main class does all the work and there technically is no real fixture
  }
}
