/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.test.LogOutput;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public class CompactCommandTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(CompactCommand.class);

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @After
  public void after() {
    DataSourceFactory.clear_ForTestsOnly();
  }

  @Test
  public void testRun_Compact() throws Exception {
    DataSourceFactory.clear_ForTestsOnly();
    final Path databasePath = setupDatabaseFile();
    final long originalSize = Files.size(databasePath);
    final InsightConfig insightConfig = new InsightConfig();
    insightConfig.setSonatypeWork(databasePath.getParent().getParent().toString());

    new CompactCommand().run(null, null, insightConfig);

    final long newSize = Files.size(databasePath);
    assertThat(newSize).isLessThan(originalSize);
    final BigDecimal percentChange = new BigDecimal(100 - newSize * 100.0d / originalSize)
        .setScale(2, BigDecimal.ROUND_HALF_EVEN);
    assertThat(logOutput).atInfoLevel().contains("Compacting " + databasePath.toAbsolutePath().toString())
        .contains("This might take a while, please be patient.")
        .contains("Successfully compacted " + databasePath.toAbsolutePath().toString() + " from " + originalSize
            + " bytes to " + newSize + " bytes " + "(reduced by " + percentChange + "%) in");
  }

  private Path setupDatabaseFile() throws Exception {
    final File databaseFolder = temporaryFolder.newFolder("data");
    FileUtils.copyFileToDirectory(Paths.get("src", "test", "resources", "CompactCommandTest", "ods.h2.db").toFile(),
        databaseFolder);
    return Paths.get(databaseFolder.getPath(), "ods.h2.db");
  }
}
