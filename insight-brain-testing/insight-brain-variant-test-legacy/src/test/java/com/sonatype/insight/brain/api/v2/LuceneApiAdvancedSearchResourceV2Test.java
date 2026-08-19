/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.service.InsightWork;

import org.apache.commons.io.FileUtils;
import com.sonatype.insight.brain.variant.LegacyServerTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

// Requires an isolated per-class TestCLMServer: the search-index rebuild/cancel lifecycle deletes and
// recreates the on-disk Lucene index, which conflicts with a reused server's open index reader, so it
// cannot run in a reused-server variant cohort (CLM-45580 re-audit).
@LegacyServerTest
public class LuceneApiAdvancedSearchResourceV2Test
    extends AbstractApiAdvancedSearchResourceV2Test
{
  @BeforeEach
  @AfterEach
  public void cleanSearchIndexDir() throws Exception {
    InsightWork insightWork = getCLMServer().getInstance(InsightWork.class);
    FileUtils.deleteDirectory(insightWork.getSearchIndexDir());
  }
}
