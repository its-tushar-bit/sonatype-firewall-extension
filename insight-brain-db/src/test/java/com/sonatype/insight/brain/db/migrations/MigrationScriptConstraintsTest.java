/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.migrations;

import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.util.List;

import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.db.DatabaseEngine;
import com.sonatype.insight.db.H2DatabaseEngine;
import com.sonatype.insight.db.PostgresDatabaseEngine;

import org.junit.Test;

import static com.sonatype.insight.brain.db.migrations.LegacyDataStoreMigrator.determineDesiredVersion;
import static com.sonatype.insight.brain.db.migrations.LegacyDataStoreMigrator.getDatabaseSpecificIncrementalFileName;
import static com.sonatype.insight.brain.db.migrations.LegacyDataStoreMigrator.getIncrementalFileName;
import static com.sonatype.insight.brain.db.migrations.LegacyDataStoreMigrator.loadIncrementalScriptResource;
import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class MigrationScriptConstraintsTest
{
  @Test
  public void testValidate_MultipleIncrementalScripts_DoNotExist() {
    List<String> dataStoreIds = List.of(
        AggregationDataStore.ID,
        DataMartDataStore.ID,
        OperationalDataStore.ID,
        ThirdPartyScansDataStore.ID);

    for (String dataStoreId : dataStoreIds) {
      int firstVersion = determineMinVersion(dataStoreId);
      int maxVersion = determineDesiredVersion(dataStoreId);

      for (int version = firstVersion; version <= maxVersion; version++) {

        boolean scriptExists = scriptExists(dataStoreId, version);
        boolean h2ScriptExists = dbSpecificScriptExists(dataStoreId, version, H2DatabaseEngine.INSTANCE);
        boolean pgScriptExists = dbSpecificScriptExists(dataStoreId, version, PostgresDatabaseEngine.INSTANCE);

        String message = """
            Only two states are valid for incremental migration scripts. Either:

            1. A single script ending in .sql exists that applies to both H2 and Postgres.
            2. OR two scripts exist, one for H2 and one for Postgres, with the same version number.

            Both cannot be true. For version %d of data store %s, the following is the detected state:
                - Generic incremental script exists: %s
                - H2 script exists: %s
                - PG script exists: %s
            """;

        if (scriptExists) {
          assertThat(h2ScriptExists)
              .as(message, version, dataStoreId, scriptExists, h2ScriptExists, pgScriptExists)
              .isFalse();
          assertThat(pgScriptExists)
              .as(message, version, dataStoreId, scriptExists, h2ScriptExists, pgScriptExists)
              .isFalse();
        }
        else {
          assertThat(h2ScriptExists)
              .as(message, version, dataStoreId, scriptExists, h2ScriptExists, pgScriptExists)
              .isTrue();
          assertThat(pgScriptExists)
              .as(message, version, dataStoreId, scriptExists, h2ScriptExists, pgScriptExists)
              .isTrue();
        }
      }
    }
  }

  private int determineMinVersion(String dataStoreId) {
    for (int version = 1; version < determineDesiredVersion(dataStoreId); version++) {
      boolean scriptExists = scriptExists(dataStoreId, version);
      boolean h2ScriptExists = dbSpecificScriptExists(dataStoreId, version, H2DatabaseEngine.INSTANCE);
      boolean pgScriptExists = dbSpecificScriptExists(dataStoreId, version, PostgresDatabaseEngine.INSTANCE);

      if (scriptExists || h2ScriptExists || pgScriptExists) {
        return version;
      }
    }

    throw new RuntimeException("No scripts found for " + dataStoreId);
  }

  private boolean scriptExists(String dataStoreId, int version) {
    return loadIncrementalScriptResource(getIncrementalFileName(dataStoreId, "sql", version)).exists();
  }

  private boolean dbSpecificScriptExists(String dataStoreId, int version, DatabaseEngine engine) {
    return loadIncrementalScriptResource(getDatabaseSpecificIncrementalFileName(version, dataStoreId, engine)).exists();
  }
}
