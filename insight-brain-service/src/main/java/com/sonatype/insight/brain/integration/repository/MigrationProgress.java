/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import com.sonatype.clm.dto.model.repository.migration.MigrationState;

/**
 * @since 1.31
 */
class MigrationProgress
{
  private volatile MigrationState state = MigrationState.RUNNING;

  MigrationState getState() {
    return state;
  }

  void success() {
    state = MigrationState.COMPLETED;
  }

  void failure() {
    state = MigrationState.FAILED;
  }
}
