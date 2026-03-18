/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.service.InsightWork;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;
import static com.sonatype.insight.brain.report.ApplicationReportPersistenceServiceTestHelper.APPLICATION_ID;
import static com.sonatype.insight.brain.report.ApplicationReportPersistenceServiceTestHelper.SCAN_ID;

public class FileApplicationReportPersistenceServiceTest
    extends AbstractApplicationReportPersistenceServiceTest
{
  @Inject
  private InsightWork insightWork;

  @Before
  public void setCluster() throws Exception {
    // set a distinct cluster dir so that we test that reports are saved there and not in the non-clustered
    // sonatype-work. Without this setting, those are the same location so the distinction doesn't get tested.
    insightConfig.setClusterDirectory(tempDir.newFolder().getAbsolutePath());
  }

  @Before
  public void setup() {
    var helper = new FileApplicationReportPersistenceServiceTestHelper(tempDir, insightConfig, insightWork);
    setup(helper);
  }

  @Test
  @Override
  public void testCorrectImplClass() {
    assertThat(service).isInstanceOf(FileApplicationReportPersistenceService.class);
  }

  @Test
  @Override
  @Category(SlowTest.class)
  public void testGetReportLocation() {
    // The output of this API will differ by OS depending on the path separator character
    String suffix =
        FileSystems.getDefault().getSeparator().equals("\\") ? "\\report\\app1\\scan1" : "/report/app1/scan1";

    assertThat(service.getReportLocation(APPLICATION_ID, SCAN_ID)).isEqualTo(
        insightConfig.getClusterDirectory().toString() + suffix);
  }

  @Override
  protected ApplicationReportPersistenceService mockForSaveOriginalReport_cleansUpOnFailure() throws IOException {
    Path zipPath = Path.of(insightWork.getReportDir(APPLICATION_ID, SCAN_ID) + "/report.zip");
    Files.createDirectories(zipPath.getParent());
    Files.createFile(zipPath);
    Files.write(zipPath, new byte[0]);
    return service;
  }
}
