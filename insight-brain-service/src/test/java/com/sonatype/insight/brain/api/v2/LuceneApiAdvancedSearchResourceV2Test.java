/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import com.sonatype.insight.brain.service.InsightWork;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;

@Category(SlowTest.class)
public class LuceneApiAdvancedSearchResourceV2Test
    extends AbstractApiAdvancedSearchResourceV2Test
{
  @Before
  @After
  public void cleanSearchIndexDir() throws Exception {
    InsightWork insightWork = getCLMServer().getInstance(InsightWork.class);
    FileUtils.deleteDirectory(insightWork.getSearchIndexDir());
  }
}
