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
