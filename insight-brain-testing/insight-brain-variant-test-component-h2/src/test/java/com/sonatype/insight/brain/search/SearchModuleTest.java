/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.search.SearchIndexRuleAnnotations.LuceneTest;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.lucene.LuceneSearchIndexClient;
import com.sonatype.insight.brain.variant.AbstractBrainInjectedH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the default (Lucene) {@link SearchIndexClient} wiring. Runs in the shared reused H2 component context:
 * the client type is fixed by the module's default search config, so the previous per-method {@code @DirtiesContext}
 * rebuild is unnecessary.
 */
@ComponentH2Test
public class SearchModuleTest
    extends AbstractBrainInjectedH2Test
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
}
