/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.dbmodifier;

import java.io.File;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DbModifierCli
{
  private static final Logger log = LoggerFactory.getLogger(DbModifierCli.class);

  private static final String H2_DATABASE_SUFFIX = ".h2.db";

  @Parameter(names = {"--postgres"}, description = "Flag to enable Postgres")
  private boolean isPostgres;

  @Parameter(names = {"-db", "--database"},
      description = "Path to h2 db file: e.g. ~/test/ods.h2.db, or name of the Postgres database", required = true)
  private String database;

  @Parameter(names = {"-user", "--db-user"}, description = "db username")
  private String dbUser = "sa";

  @Parameter(names = {"-pass", "--db-password"}, description = "db password")
  private String dbPassword = "";

  @Parameter(names = {"-schema", "--db-schema"}, description = "db schema")
  private String dbSchema = "insight_brain_ods";

  @Parameter(names = {"-max", "-maxDate", "--max-date"}, description = "new max date in the db. ex: 2018-09-15")
  private Date maxDate;

  @Parameter(names = {"-shift", "-shiftDays", "--shift-days"}, description = "number of days to shift dates. ex: 42")
  private Integer shiftDays;

  @Parameter(names = {"-info", "-showDateInfo",
      "--show-date-info"}, description = "Shows info on current max/min dates for db.")
  private boolean dateInfo;

  @Parameter(names = {"-q", "-quiet"}, description = "Suppress all output.")
  private boolean quiet;

  @Parameter(names = {"-v", "-verbose"}, description = "Provide extended output.")
  private boolean verbose;

  @Parameter(names = {"-c", "-compact"}, description = "Compact db, can be run alone or with date change")
  private boolean compact;

  @Parameter(names = {"-s", "-scrub"}, description = "Extract sql, scrub, rebuild & compact db")
  private boolean scrub;

  @Parameter(names = {"-sn", "-scrub-no-build"},
             description = "Extract and scrub db sql.  Do not rebuild. (implies -scrub-keep)")
  private boolean scrubNoBuild;

  @Parameter(names = {"-sk", "-scrub-keep"}, description = "Keep files used during scrub operations.")
  private boolean scrubKeep;

  @Parameter(names = {"-dbv", "-db-version"}, description = "Print db version info and exit.")
  private boolean dbVersion;

  @Parameter(names = {"-h", "--hostname"}, description = "Hostname of Postgres server")
  private String hostname;

  @Parameter(names = {"-p", "--port"}, description = "Port of Postgres server")
  private int port;

  private static void noArgsCheck(int argCount) {
    if (argCount == 0) {
      printUsage();
      System.exit(1);
    }
  }

  private void onSuccess(DbModifier dbmod) {
    if (!quiet) {
      if (verbose) {
        printTableInfo(dbmod.getDateInfo());
      }
    }
    if (compact) {
      if (!quiet) {
        log.info("Starting DB Compaction");
      }
      dbmod.compact();
    }
    log.info("SUCCESS");
  }

  private static void onError(DbModifierCli cli, Exception ex) {
    if (ex instanceof ParameterException) {
      log.info("\nERROR: {}\n", ex.getMessage());
      printUsage();
    }
    else {
      if (!cli.quiet) {
        log.info("\nERROR: see nexus-iq-tools.log\n{}\n", ex.getMessage());
        if (cli.verbose) {
          log.info(ex.getMessage(), ex);
        }
      }
    }
    log.error(ex.getMessage(), ex);
    System.exit(1);
  }

  public static void main(String[] args) {
    noArgsCheck(args.length);
    DbModifierCli dbmcli = new DbModifierCli();
    try {
      JCommander.newBuilder() //
          .addObject(dbmcli) //
          .build() //
          .parse(args);
      if (!dbmcli.isPostgres) {
        dbmcli.validateH2Db();
      }
      dbmcli.run();
    }
    catch (Exception e) {
      onError(dbmcli, e);
    }
  }

  private static void printUsage() {
    JCommander.newBuilder() //
        .addObject(new DbModifierCli()) //
        .programName("java -jar nexus-iq-tools.jar dbmod") //
        .build() //
        .usage();
  }

  private void validateH2Db() {
    String dbPath = new File(database).getAbsolutePath();
    String fullDbPath = dbPath.endsWith(H2_DATABASE_SUFFIX) ? dbPath : dbPath + H2_DATABASE_SUFFIX;

    if (!new File(fullDbPath).exists()) {
      throw new ParameterException("Invalid Db File: " + fullDbPath);
    }

    this.database = fullDbPath.substring(0, fullDbPath.length() - H2_DATABASE_SUFFIX.length());
  }

  private LocalDate mapDate(Date date) {
    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
  }

  private void printTableInfo(List<DbModifier.TableDateMinMax> tablesMinMax) {
    log.info("--------------------------------------------------------------------------------");
    if (isPostgres) {
      log.info("DB: {}", database);
    }
    else {
      log.info("DB: {}", new File(database).getAbsolutePath() + H2_DATABASE_SUFFIX);
    }
    log.info("schema: {}", dbSchema);
    log.info("--------------------------------------------------------------------------------");
    log.info("{} {} {}", StringUtils.center(":table:", 30), StringUtils.center(":min date:", 25),
        StringUtils.center(":max date:", 25));
    log.info("--------------------------------------------------------------------------------");
    tablesMinMax.forEach(tmm -> log.info(String.format("%1$-30s %2$-25s %3$-25s", tmm.table, tmm.min, tmm.max)));
    log.info("--------------------------------------------------------------------------------");
  }

  private void run() {
    DbModifier dbmod;
    if ( isPostgres ) {
      dbmod = new PostgresDbModifier(dbUser, dbPassword, hostname, port, database, dbSchema);
    }
    else {
      dbmod = new H2DbModifier(dbUser, dbPassword, new File(database), dbSchema);
    }
    if (dateInfo) {
      printTableInfo(dbmod.getDateInfo());
    }
    else if (shiftDays != null) {
      log.info("Shifting timestamps by {} days", shiftDays);
      dbmod.shiftDays(shiftDays);
      onSuccess(dbmod);
    }
    else if (maxDate != null) {
      log.info("Shifting timestamps to {}", maxDate);
      dbmod.shiftToDate(mapDate(maxDate));
      onSuccess(dbmod);
    }
    else if (compact) {
      dbmod.compact();
    }
    else if (scrub || scrubNoBuild) {
      dbmod.scrub(!scrubNoBuild, scrubNoBuild || scrubKeep);
    }
    else if (dbVersion) {
      log.info(dbmod.dbVersion());
    }
    else {
      printUsage();
    }
  }
}
