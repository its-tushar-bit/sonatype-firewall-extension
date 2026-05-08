/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.migrations;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;

import org.codehaus.plexus.util.StringUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class DatabaseMigrationScriptImmutabilityTest
{
  private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationScriptImmutabilityTest.class);

  private static final String[] DATA_STORE_DIRS = {
    AggregationDataStore.ID,
    DataMartDataStore.ID,
    OperationalDataStore.ID,
    ThirdPartyScansDataStore.ID
  };

  private static final String DB_SCRIPT_PATH_FROM_ROOT = "insight-brain-db/src/main/resources/db";

  private static final Duration PROCESS_TIMEOUT_SECONDS = Duration.ofSeconds(10);

  /**
   * This test verifies that existing migration scripts committed to origin/main have not been modified. Migration
   * scripts should be immutable once committed to prevent breaking existing deployments. If you need to make a change,
   * create a new migration script instead.
   */
  @Test
  public void testDatabaseMigrationScripts_ExistingScriptsAreImmutable() {
    assertThat(runCommand("git", "--version").isSuccess())
        .withFailMessage("Git is not available")
        .isTrue();
    assertThat(runCommand("git", "rev-parse", "--git-dir").isSuccess())
        .withFailMessage("Not in a git repository")
        .isTrue();

    log.info("Checking migration scripts for modifications against origin/main...");

    List<String> modifiedScripts = getModifiedMigrationScriptsVsOriginMainViaGitDiff();
    assertThat(modifiedScripts).withFailMessage(
        "The following migration scripts have been modified but already exist in origin/main:\n" +
            String.join("\n", modifiedScripts) +
            "\n\n" +
            "Migration scripts must be immutable once committed to origin/main.\n" +
            "If you need to make a change, please create a NEW migration script instead.\n" +
            "This ensures that existing deployments are not broken by schema changes.")
        .isEmpty();

    log.info("All migration scripts are unchanged - validation passed");
  }

  private List<String> getModifiedMigrationScriptsVsOriginMainViaGitDiff() {
    List<String> modifiedScripts = new ArrayList<>();
    CommandResult commandResult =
        runCommand("git", "diff", "--diff-filter=M", "--name-only", "origin/main...", "--",
            getDbScriptPath().toString());
    assertThat(commandResult.isSuccess()).withFailMessage(commandResult.command + " failed").isTrue();
    for (String line : commandResult.stdout.split("\\R")) {
      line = line.trim();
      if (line.contains("/schema_incremental_") && line.endsWith(".sql")) {
        for (String dataStoreDir : DATA_STORE_DIRS) {
          if (line.contains("/" + dataStoreDir + "/")) {
            modifiedScripts.add(line);
            log.debug("Found modified migration script: {}", line);
            break;
          }
        }
      }
    }
    return modifiedScripts;
  }

  private Path getDbScriptPath() {
    CommandResult gitRootLevel = runCommand("git", "rev-parse", "--show-toplevel");
    assertThat(gitRootLevel.isSuccess()).withFailMessage(gitRootLevel.command + " failed").isTrue();
    Path path = Path.of(gitRootLevel.stdout, DB_SCRIPT_PATH_FROM_ROOT);
    assertThat(path).exists().isDirectory();
    return path;
  }

  private CommandResult runCommand(final String... command) {
    String c = String.join(" ", command);
    Process process = null;
    Boolean finished = null;
    String stdout = null;
    String stderr = null;
    try {
      log.debug("Running command {}", c);
      process = new ProcessBuilder(command).start();
      finished = process.waitFor(PROCESS_TIMEOUT_SECONDS.getSeconds(), TimeUnit.SECONDS);
      stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
      if (StringUtils.isNotBlank(stdout)) {
        log.debug("Command stdout: {}", stdout);
      }
      stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8).trim();
      if (StringUtils.isNotBlank(stderr)) {
        log.error("Command stderr: {}", stderr);
      }
      return new CommandResult(c, process.exitValue(), stdout, stderr);
    }
    catch (Exception e) {
      log.error("{} failed", c, e);
      return new CommandResult(c, -1, stdout, stderr);
    }
    finally {
      if (process != null && Boolean.FALSE.equals(finished)) {
        process.destroyForcibly();
      }
    }
  }

  private record CommandResult(
      String command,
      int exitCode,
      String stdout,
      String stderr)
  {
    boolean isSuccess() {
      return exitCode == 0;
    }
  }
}
