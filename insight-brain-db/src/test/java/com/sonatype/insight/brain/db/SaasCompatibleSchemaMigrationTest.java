/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.google.common.io.Files;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SaasCompatibleSchemaMigrationTest
{
  private static final String SAAS_COMPATIBLE = "-- SaaS Compatible";

  @Test
  public void testSaasCompatibleSchemaMigrationsForAggregation() throws IOException {
    testSaasCompatibleSchemaMigration("insight_brain_aggregation", 13);
  }

  @Test
  public void testSaasCompatibleSchemaMigrationsForDM() throws IOException {
    testSaasCompatibleSchemaMigration("insight_brain_dm", 12);
  }

  @Test
  public void testSaasCompatibleSchemaMigrationsForODS() throws IOException {
    testSaasCompatibleSchemaMigration("insight_brain_ods", 301);
  }

  @Test
  public void testSaasCompatibleSchemaMigrationsForThirdPartyScans() throws IOException {
    testSaasCompatibleSchemaMigration("insight_brain_third_party_scans", 13);
  }

  private void testSaasCompatibleSchemaMigration(String schema, int schemaCutoff) throws IOException {
    File schemaDir = new File("src/main/resources/db/" + schema);
    for (File file : schemaDir.listFiles()) {
      String filename = file.getName();
      if (filename.startsWith("schema_incremental_") && filename.endsWith(".sql")) {
        if (getVersion(filename) >= schemaCutoff) {
          assertThat(isSaasCompatible(file))
              .as("Schema file %s/%s is annotated as SaaS Compatible.  " +
                  "See insight_brain_ods/schema_incremental_0301.sql for more info.",
                  schema, filename)
              .isTrue();
        }
        else {
          assertThat(isSaasCompatible(file)).isFalse();
        }
      }
    }
  }

  private int getVersion(String schemaIncrementalFilename) {
    int offset = "schema_incremental_".length();
    String version = schemaIncrementalFilename.substring(offset, offset + 4);
    return Integer.parseInt(version);
  }

  private boolean isSaasCompatible(File file) throws IOException {
    boolean compatible = false;

    for (String line : Files.readLines(file, StandardCharsets.UTF_8)) {
      if (SAAS_COMPATIBLE.equalsIgnoreCase(line)) {
        compatible = true;
        break;
      }
    }

    return compatible;
  }
}
