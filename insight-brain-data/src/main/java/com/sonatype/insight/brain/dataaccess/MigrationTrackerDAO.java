/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import com.sonatype.insight.brain.model.MigrationTracker;

public class MigrationTrackerDAO
    extends AbstractOperationalSqlDAO<MigrationTracker>
{
  @Override
  public MigrationTracker getById(final String id) {
    return get("SELECT mt FROM MigrationTracker mt WHERE mt.id=?1", id);
  }
}
