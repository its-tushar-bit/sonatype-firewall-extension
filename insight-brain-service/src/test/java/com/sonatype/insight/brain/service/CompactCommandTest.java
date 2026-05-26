/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;
import com.sonatype.insight.brain.db.AbstractDatabaseTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.test.LogOutput;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CompactCommandTest
    extends AbstractDatabaseTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(CompactCommand.class);

  @Test
  @H2DiskTest(suppressMigrations = true, copyExistingDatabase = "CompactCommandTest")
  @Category(SlowTest.class)
  public void testRun_Compact_H2Database() throws Exception {
    Path databaseFile = Paths.get(getDatabasePath().getAbsolutePath(), "ods.h2.db");
    final long originalSize = Files.size(databaseFile);
    final InsightConfig insightConfig = new InsightConfig();
    insightConfig.setSonatypeWork(databaseFile.getParent().getParent().toString());

    new CompactCommand(insightConfig).run(insightConfig);

    final long newSize = Files.size(databaseFile);
    assertThat(newSize).isLessThan(originalSize);
    final BigDecimal percentChange = new BigDecimal(100 - newSize * 100.0d / originalSize)
        .setScale(2, RoundingMode.HALF_EVEN);
    assertThat(logOutput).atInfoLevel()
        .contains("Compacting " + databaseFile.toAbsolutePath())
        .contains("This might take a while, please be patient.")
        .contains("Successfully compacted " + databaseFile.toAbsolutePath() + " from " + originalSize
            + " bytes to " + newSize + " bytes " + "(reduced by " + percentChange + "%) in");
  }

  @Test
  public void testRun_Compact_NotH2Database() {
    InsightConfig insightConfig = new InsightConfig();
    insightConfig.setDatabase(new com.sonatype.insight.brain.service.DatabaseConfig());

    assertThatThrownBy(() -> new CompactCommand(insightConfig).run(insightConfig)).isInstanceOf(
        BadRequestException.class).hasMessage("The compact-db command is supported only for h2 databases.");
  }
}
