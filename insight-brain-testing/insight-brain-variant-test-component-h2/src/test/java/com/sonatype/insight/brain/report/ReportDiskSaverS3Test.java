/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.config.StorageConfig;
import com.sonatype.insight.brain.service.config.StorageConfig.DataStoreType;
import com.sonatype.insight.brain.service.config.StorageConfig.S3DataStoreConfig;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@ComponentH2Test
public class ReportDiskSaverS3Test
    extends AbstractComponentH2Test
{
  @Mock
  private InsightConfig mockInsightConfig;

  @Inject
  private ReportDiskSaver reportDiskSaver;

  @Test
  public void testExecute_Unsupported_S3DataStoreConfig() {
    StorageConfig storageConfig = new StorageConfig();
    storageConfig.setS3Config(new S3DataStoreConfig());
    storageConfig.setType(DataStoreType.S3);
    when(mockInsightConfig.getStorage()).thenReturn(storageConfig);
    mockInsightConfig.setStorage(storageConfig);

    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> reportDiskSaver.execute())
        .withMessageContaining("Report zip minification is only needed for legacy reports using local file storage.");
  }
}
