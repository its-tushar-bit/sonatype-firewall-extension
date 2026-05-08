/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.migrations;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

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

public class MigrationScriptConstraintsTest
{
  private static final List<String> DATA_STORE_IDS = List.of(
      AggregationDataStore.ID,
      DataMartDataStore.ID,
      OperationalDataStore.ID,
      ThirdPartyScansDataStore.ID);

  @Test
  public void testValidate_MultipleIncrementalScripts_DoNotExist() {
    for (String dataStoreId : DATA_STORE_IDS) {
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

  /**
   * CONCURRENTLY deadlocks with the advisory lock held by ClusterLockManager.createForSchemaMigration.
   * See doc/devdocs/concurrent-index-creation.md
   */
  @Test
  public void testMigrationScripts_noConcurrentlyKeyword() throws IOException {
    Pattern concurrently = Pattern.compile("(?i)\\bCONCURRENTLY\\b");
    List<String> violations = new ArrayList<>();

    for (String dataStoreId : DATA_STORE_IDS) {
      int maxVersion = determineDesiredVersion(dataStoreId);
      for (int version = 1; version <= maxVersion; version++) {
        checkScriptForConcurrently(concurrently, violations, getIncrementalFileName(dataStoreId, "sql", version));
        checkScriptForConcurrently(concurrently, violations,
            getDatabaseSpecificIncrementalFileName(version, dataStoreId, H2DatabaseEngine.INSTANCE));
        checkScriptForConcurrently(concurrently, violations,
            getDatabaseSpecificIncrementalFileName(version, dataStoreId, PostgresDatabaseEngine.INSTANCE));
      }

      for (String script : getSchemaScripts(dataStoreId)) {
        checkScriptForConcurrently(concurrently, violations, "/db/" + dataStoreId + "/" + script);
      }
    }

    assertThat(violations).withFailMessage(
        "The following migration scripts use CONCURRENTLY, which deadlocks with ClusterLock:\n" +
            String.join("\n", violations) +
            "\n\nUse AsyncDbMigration instead. See doc/devdocs/concurrent-index-creation.md")
        .isEmpty();
  }

  private void checkScriptForConcurrently(
      Pattern concurrently,
      List<String> violations,
      String scriptName) throws IOException
  {
    var resource = loadIncrementalScriptResource(scriptName);
    if (!resource.exists()) {
      return;
    }
    String content = resource.getContentAsString(StandardCharsets.UTF_8);
    boolean found = content.lines()
        .filter(line -> !line.trim().startsWith("--"))
        .anyMatch(line -> concurrently.matcher(line).find());
    if (found) {
      violations.add(scriptName);
    }
  }

  private List<String> getSchemaScripts(String dataStoreId) {
    List<String> scripts = new ArrayList<>();
    for (String manifest : List.of("scripts.txt", "scripts_h2.txt", "scripts_postgresql.txt")) {
      String path = "/db/" + dataStoreId + "/" + manifest;
      try (InputStream is = getClass().getResourceAsStream(path)) {
        if (is == null) {
          continue;
        }
        try (LineNumberReader reader = new LineNumberReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
          String line;
          while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (!line.isEmpty() && !line.contains("schema_incremental_")) {
              scripts.add(line);
            }
          }
        }
      }
      catch (IOException e) {
        throw new RuntimeException("Failed to read " + path, e);
      }
    }
    return scripts;
  }

  private boolean scriptExists(String dataStoreId, int version) {
    return loadIncrementalScriptResource(getIncrementalFileName(dataStoreId, "sql", version)).exists();
  }

  private boolean dbSpecificScriptExists(String dataStoreId, int version, DatabaseEngine engine) {
    return loadIncrementalScriptResource(getDatabaseSpecificIncrementalFileName(version, dataStoreId, engine)).exists();
  }
}
