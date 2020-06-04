/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.dbmodifier;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.h2.tools.RunScript;
import org.h2.tools.Script;
import org.h2.util.ScriptReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.tools.dbmodifier.ScrubberInsertMods.scrubInputLine;

public class DbScrubber
{
  static final String SQL_FILENAME_PREFIX = "dbmod_tmp-scrub-backup";

  static final String SCRUBBED_SQL_FILENAME_SUFFIX = ".scrubbed.sql";

  private static final Logger log = LoggerFactory.getLogger(DbScrubber.class);

  static void scrubDb(
      String dbConnectionString,
      String username,
      String password,
      boolean rebuild,
      boolean keepFiles,
      File workDir)
  {
    long start = System.currentTimeMillis();

    List<File> workingFiles = new ArrayList<>();
    try {
      log.info("Start: db scrub");
      String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ENGLISH).format(new Date());
      File sqlDumpFile = new File(workDir, SQL_FILENAME_PREFIX + "." + timestamp + ".sql");
      workingFiles.add(sqlDumpFile);
      dumpDbToSql(sqlDumpFile, dbConnectionString, username, password);

      File scrubbedSqlFile = new File(makeScrubFileName(sqlDumpFile));
      workingFiles.add(scrubbedSqlFile);
      scrubSqlBackup(sqlDumpFile, scrubbedSqlFile);

      if (rebuild) {
        String rebuildDb = "./ods_scrubbed_" + timestamp;
        String rebuildDbUrl =
            "jdbc:h2:" + rebuildDb + ";DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;MV_STORE=FALSE";
        String[] rebuildParams = new String[]{//
            "-url", rebuildDbUrl, //
            "-user", username, "-password", password, //
            "-script", scrubbedSqlFile.getAbsolutePath()};
        long startRebuild = System.currentTimeMillis();
        log.info("Starting rebuild to '{}'", rebuildDb);
        RunScript.main(rebuildParams);
        log.info("Rebuilt to '{}' in {} ms", rebuildDb, System.currentTimeMillis() - startRebuild);
      }
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
    finally {
      if (!keepFiles) {
        cleanup(workingFiles);
      }
      log.info("Complete: db scrub in {} ms", System.currentTimeMillis() - start);
    }
  }

  private static void cleanup(List<File> files) {
    files.stream().map(File::toPath).filter(Files::exists).forEach(path -> {
      try {
        Files.delete(path);
      }
      catch (IOException ioex) {
        log.error("Failed to delete file '{}'", path, ioex);
      }
    });
  }

  private static void dumpDbToSql(
      File outFile,
      String dbConnectionString,
      String username,
      String password)
      throws SQLException
  {
    long start = System.currentTimeMillis();
    log.info("Exporting db to '{}'", outFile.getAbsolutePath());

    String[] params = new String[]{//
        "-url", dbConnectionString, //
        "-user", username, "-password", password, //
        "-script", outFile.getAbsolutePath(), //
        "-options", "SIMPLE"};
    Script.main(params);

    log.info("Exported db to '{}' in {} ms", outFile.getAbsolutePath(), System.currentTimeMillis() - start);
  }

  private static String filterInserts(String sql) {
    if (sql == null) {
      return null;
    }
    String trimmed = sql.trim();
    if (trimmed.toUpperCase(Locale.ENGLISH).startsWith("INSERT")) {
      return trimmed;
    }
    return null;
  }

  private static String[] splitComments(String sql) {
    // this is required due to the script reader joining comment lines added in the backup sql to the following line
    if (sql == null) {
      return new String[]{};
    }
    String trimmed = sql.trim();
    if (!trimmed.toUpperCase(Locale.ENGLISH).startsWith("--") || !trimmed.contains("\n")) {
      return new String[]{trimmed};
    }
    int commentEnd = trimmed.indexOf('\n');
    String comment = trimmed.substring(0, commentEnd).trim();
    String nextLine = trimmed.substring(commentEnd).trim();
    return new String[]{comment, nextLine};
  }

  private static String makeScrubFileName(File sqlFile) {
    String sqlFileName = sqlFile.getAbsolutePath();
    return sqlFileName.toLowerCase(Locale.ENGLISH).endsWith(".sql")
        ? sqlFileName.substring(0, sqlFileName.length() - 4) + SCRUBBED_SQL_FILENAME_SUFFIX
        : sqlFileName + SCRUBBED_SQL_FILENAME_SUFFIX;
  }

  private static void scrubSqlBackup(File sqlFile, File scrubbedFile) throws IOException {
    long start = System.currentTimeMillis();
    log.info("Scrubbing sql backup file '{}'", sqlFile.getAbsolutePath());

    log.info("Writing scrubbed sql to '{}'", scrubbedFile.getAbsolutePath());

    int processedCount = 0;
    int insertCount = 0;
    int scrubErrors = 0;
    try (ScriptReader scriptReader = new ScriptReader(new FileReader(sqlFile));
        PrintWriter scrubbedOut = new PrintWriter(new BufferedWriter(new FileWriter(scrubbedFile)))) {
      String sql;
      while ((sql = scriptReader.readStatement()) != null) {
        String[] lines = splitComments(sql);
        for (String line : lines) {
          try {
            processedCount++;
            String insertSqlString = filterInserts(line);
            if (insertSqlString != null) {
              SQLLine sqlLine = InputParser.parseInput(insertSqlString);
              if (sqlLine != null) {
                scrubInputLine(sqlLine).forEach(scrubbedOut::println);
                insertCount++;
              }
              else {
                scrubErrors++;
              }
            }
            else {
              scrubbedOut.println(line.trim() + ";");
            }
            if (processedCount % 1000000 == 0) {
              log.info("   Lines processed: {}", processedCount);
            }
          }
          catch (Exception e) {
            log.error("Failed while processing line:\n{}", line);
            throw e;
          }
        }
      }
    }
    log.info("Lines processed: {}", processedCount);
    log.info("Insert lines: {}", insertCount);
    log.info("Errors: {}", scrubErrors);
    log.info("Scrubbed in {} ms", System.currentTimeMillis() - start);
  }
}
