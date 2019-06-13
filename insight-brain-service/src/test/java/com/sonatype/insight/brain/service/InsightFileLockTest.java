/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class InsightFileLockTest
{
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void testLockRelease_SameInstance() {
    InsightConfig insightConfig = new InsightConfig();
    insightConfig.setSonatypeWork(temporaryFolder.getRoot().getAbsolutePath());
    InsightFileLock insightFileLock = new InsightFileLock(insightConfig);

    insightFileLock.lock();
    insightFileLock.release();

    insightFileLock.lock();
    insightFileLock.lock();
    insightFileLock.release();
    insightFileLock.release();
  }

  @Test
  public void testLockRelease_DifferentInstances() {
    InsightConfig insightConfig = new InsightConfig();
    insightConfig.setSonatypeWork(temporaryFolder.getRoot().getAbsolutePath());
    String expectedMessage =
        "Work directory " + insightConfig.getSonatypeWork().getAbsolutePath() + " is already in use.";
    InsightFileLock insightFileLockOne = new InsightFileLock(insightConfig);
    InsightFileLock insightFileLockTwo = new InsightFileLock(insightConfig);

    insightFileLockOne.lock();
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(insightFileLockTwo::lock)
        .withMessage(expectedMessage);
    insightFileLockOne.release();

    insightFileLockTwo.lock();
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(insightFileLockOne::lock)
        .withMessage(expectedMessage);
    insightFileLockTwo.release();
  }
}
