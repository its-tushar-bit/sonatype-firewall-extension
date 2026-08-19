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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestInfo;

public abstract class AbstractDatabaseTest
{
  @Rule(order = 1)
  public DatabaseRule databaseRule = DatabaseRule.getInstance(AbstractDatabaseTest.class);

  // JUnit 5 (Jupiter): the @Rule(order=1) does not fire under Jupiter, so provision the (reused) database
  // fixture from a @BeforeEach. Inert under the Vintage engine, which drives the @Rule instead; this base is
  // shared with still-JUnit 4 tests in insight-brain-data / insight-brain-service, so it is intentionally NOT
  // switched to @ExtendWith (which would make those Vintage tests Jupiter-discoverable).
  @BeforeEach
  public void jupiterInitDatabaseRule(final TestInfo testInfo) {
    databaseRule.beforeFromJupiter(testInfo.getTestClass().orElse(null), testInfo.getTestMethod().orElse(null));
  }

  // JUnit 5 (Jupiter) teardown: run DatabaseRule.after() so its per-test reset/dirty bookkeeping stays correct
  // (the JUnit 4 @Rule runs after() per test; without this the reused fixture is never reset and data leaks).
  // Runs last on teardown (superclass @AfterEach after subclass), matching the order=1 outer rule.
  @AfterEach
  public void jupiterCleanupDatabaseRule() {
    databaseRule.afterFromJupiter();
  }

  protected File getDatabasePath() {
    Map<String, Object> metadata = databaseRule.getMetadata();
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
