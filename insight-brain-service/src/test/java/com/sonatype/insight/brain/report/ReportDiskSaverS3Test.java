/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.config.StorageConfig;
import com.sonatype.insight.brain.service.config.StorageConfig.DataStoreType;
import com.sonatype.insight.brain.service.config.StorageConfig.S3DataStoreConfig;

import com.google.inject.Binder;
import com.google.inject.Inject;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@Category(SlowTest.class)
public class ReportDiskSaverS3Test
    extends AbstractComponentTest
{
  @Mock
  private InsightConfig mockInsightConfig;

  @Inject
  private ReportDiskSaver reportDiskSaver;

  @Override
  public void configure(Binder binder) {
    binder.bind(InsightConfig.class).toInstance(mockInsightConfig);
    super.configure(binder);
  }

  @Test
  public void testExecute_Unsupported_S3DataStoreConfig() {
    StorageConfig storageConfig = new StorageConfig();
    storageConfig.setS3Config(new S3DataStoreConfig());
    storageConfig.setType(DataStoreType.S3);
    when(mockInsightConfig.getStorage()).thenReturn(storageConfig);
    mockInsightConfig.setStorage(storageConfig);

    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> reportDiskSaver.execute(null, null))
        .withMessageContaining("Report zip minification is only needed for legacy reports using local file storage.");
  }
}
