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
import java.nio.file.Paths;
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
  private static final Logger log = LoggerFactory.getLogger(DbScrubber.class);

  static void scrubDb(String dbConnectionString, boolean rebuild, boolean keepFiles) {
    List<String> workingFiles = new ArrayList<>();
    try {
      log.info("Start: db scrub");
      String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ENGLISH).format(new Date());
      String outFile = "dbmod_tmp-scrub-backup." + timestamp + ".sql";
      workingFiles.add(outFile);
      dumpDbToSql(outFile, dbConnectionString);

      String scrubbedFile = makeScrubFileName(outFile);
      workingFiles.add(scrubbedFile);
      scrubSqlBackup(outFile, scrubbedFile);

      if (rebuild) {
        String rebuildDb = "./ods_scrubbed_" + timestamp;
        String rebuildDbUrl =
            "jdbc:h2:" + rebuildDb + ";DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;MV_STORE=FALSE";
        String[] rebuildParams = new String[]{"-url", rebuildDbUrl, "-script", scrubbedFile};
        long start = System.currentTimeMillis();
        log.info("Starting rebuild to: " + rebuildDb);
        RunScript.main(rebuildParams);
        log.info("Rebuild complete.");
        log.info("Elapsed: " + (System.currentTimeMillis() - start) + "ms");
      }
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
    finally {
      if (!keepFiles) {
        cleanup(workingFiles);
      }
      log.info("Complete: db scrub");
    }
  }

  private static void cleanup(List<String> fileNames) {
    fileNames.stream().map(Paths::get).filter(Files::exists).forEach(path -> {
      try {
        Files.delete(path);
      }
      catch (IOException ioex) {
        log.error("Failed to delete file: " + path, ioex);
      }
    });
  }

  private static void dumpDbToSql(String outFile, String dbConnectionString) throws SQLException {
    String[] params = new String[]{"-url", dbConnectionString, "-script", outFile, "-options", "SIMPLE"};
    Script.main(params);
    log.info("Exported to: " + outFile);
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

  private static String makeScrubFileName(String sqlFile) {
    return sqlFile.toLowerCase(Locale.ENGLISH).endsWith(".sql") ?
        sqlFile.substring(0, sqlFile.length() - 4) + ".scrubbed.sql"
        : sqlFile + ".scrubbed.sql";
  }

  private static void scrubSqlBackup(String sqlFile, String scrubbedFile) throws IOException {
    long start = System.currentTimeMillis();
    log.info("Scrubbing sql backup file at: " + new File(sqlFile).getAbsolutePath());

    log.info("Scrubbed sql ouput to: " + new File(scrubbedFile).getAbsolutePath());

    ScriptReader scriptReader = new ScriptReader(new FileReader(sqlFile));
    int processedCount = 0;
    int insertCount = 0;
    int scrubErrors = 0;
    try (PrintWriter scrubbedOut = new PrintWriter(new BufferedWriter(new FileWriter(scrubbedFile)))) {
      String sql;
      while ((sql = scriptReader.readStatement()) != null) {
        String[] lines = splitComments(sql);
        for (String line : lines) {
          processedCount++;
          String insert = filterInserts(line);

          if (insert != null) {
            SQLLine sqlLine = InputParser.parseInput(insert);
            if (!"ERROR".equals(sqlLine.table)) {
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
        }
      }
    }
    finally {
      scriptReader.close();
    }
    log.info("Lines processed: " + processedCount);
    log.info("Insert lines: " + insertCount);
    log.info("Errors: " + scrubErrors);
    log.info("Elapsed: " + (System.currentTimeMillis() - start) + "ms");
  }
}
