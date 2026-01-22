/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractSqlDAO;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.FileCoordinateDisplayNameGenerator;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.dataaccess.TransactionContext;

import com.networknt.schema.utils.StringUtils;

/**
 * Migrator that will populate the file_coordinate display_name column with a value calculated based on the pURL or
 * (format, name, version)
 */
@Named
@Singleton
public class DisplayNameForFileCoordinateAsyncDbMigration
    extends AsyncDbMigration<ThirdPartyFileCoordinate>
{
  @Inject
  public DisplayNameForFileCoordinateAsyncDbMigration(
      final ThirdPartyFileCoordinateDAO dao,
      final MigrationTrackerDAO migrationTrackerDAO,
      final InsightConfig config)
  {
    super(dao, migrationTrackerDAO, "display name for file coordinates", config);
  }

  @Override
  protected void migrate(
      final AbstractSqlDAO<ThirdPartyFileCoordinate> dao,
      final ThirdPartyFileCoordinate entity,
      final TransactionContext tx)
  {
    if (StringUtils.isBlank(entity.getDisplayName())) {
      entity.setDisplayName(
          FileCoordinateDisplayNameGenerator.generateDisplayName(entity.getPackageUrl(), entity.getFormat(),
              entity.getName(),
              entity.getVersion()));
    }
  }
}
