/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import com.sonatype.clm.dto.model.repository.migration.MigrationState;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class MigrationProgressTest
{
  @Test
  public void testSuccess() throws Exception {
    MigrationProgress progress = new MigrationProgress();

    progress.success();

    assertThat(progress.getState(), is(MigrationState.COMPLETED));
  }

  @Test
  public void testFailure() throws Exception {
    MigrationProgress progress = new MigrationProgress();

    progress.failure();

    assertThat(progress.getState(), is(MigrationState.FAILED));
  }
}
