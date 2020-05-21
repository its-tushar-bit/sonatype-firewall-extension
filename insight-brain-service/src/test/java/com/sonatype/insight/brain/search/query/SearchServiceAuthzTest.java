/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.query;

import javax.inject.Inject;

import com.sonatype.insight.brain.search.index.IndexService;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.junit.Test;

public class SearchServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private IndexService indexService;

  @Inject
  private SearchService searchService;

  @Test
  public void testSearchIndex_Unauthenticated() throws Exception {
    createIndex();
    searchService.searchIndex("query", 1, 1);
  }

  private void createIndex() throws Exception {
    grantConfigureSystemPermission();
    indexService.createSearchIndex();
    subject.logout();
  }
}
