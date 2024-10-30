/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

import com.sonatype.insight.brain.db.rule.DatabaseRule;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;
import com.sonatype.insight.db.DatabaseConfig;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;

public abstract class AbstractDatabaseTest
{
  @Rule(order = 1)
  public DatabaseRule databaseRule = DatabaseRule.getInstance(AbstractDatabaseTest.class);

  protected File getDatabasePath() {
    Map<String, Object> metadata = databaseRule.getDatabaseMetadata();
    return new File((String) metadata.get(H2DiskTest.DATABASE_PATH));
  }

  protected DatabaseConfig getDatabaseConfig(String databaseName) {
    return databaseRule.getDatabaseConfig(databaseName);
  }

  protected void copyDatabase(File databaseDir, String resourceDir) {
    try {
      FileUtils.copyDirectory(new File("target/test-classes", resourceDir), databaseDir);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  protected File getDatabaseVersionFile(File databaseDir, String databaseName) {
    return new File(databaseDir, databaseName + ".ver");
  }

  protected String readDatabaseVersion(File versionFile) {
    try {
      return new String(Files.readAllBytes(versionFile.toPath()), StandardCharsets.UTF_8);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
