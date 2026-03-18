/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class FileApplicationReportPersistenceServiceMultiTenantTest
    extends AbstractApplicationReportPersistenceServiceMultiTenantTest
{
  @Before
  public void setup() throws Exception {
    var configurator = new MtiqDatabaseConfigurator()
    {
      @Override
      public boolean isReusable() {
        // do not reuse the server as we assume a clean work directory for each test
        return false;
      }
    };

    setup(configurator, () -> {
      var insightConfig = lookup(InsightConfig.class);
      var insightWork = lookup(InsightWork.class);
      return new FileApplicationReportPersistenceServiceTestHelper(tempDir, insightConfig, insightWork);
    });
  }

  @Test
  @Override
  public void testCorrectImplClass() {
    assertThat(service).isInstanceOf(FileApplicationReportPersistenceService.class);
  }
}
