/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.zip.GZIPOutputStream;

import javax.sql.DataSource;

import com.sonatype.insight.brain.db.AggregationDataStoreProvider;
import com.sonatype.insight.brain.db.DatabaseName;
import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.db.ThirdPartyScansProvider;
import com.sonatype.insight.error.exception.BadRequestException;

import io.dropwizard.cli.Cli;
import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.67
 */
public class ExportEmbeddedDatabaseCommand
    extends ConfiguredCommand<InsightConfig>
{
  private static final Logger log = LoggerFactory.getLogger(ExportEmbeddedDatabaseCommand.class);

  private static final String NULL_VALUE = "NULL";

  private static final String TIMESTAMP_PREFIX = "TIMESTAMP ";

  private static final String STRINGDECODE_PREFIX = "STRINGDECODE(";

  ExportEmbeddedDatabaseCommand() {
    super("export-embedded-db", "Exports the embedded database to a SQL file for import into an external database.");
  }

  @Override
  public void configure(Subparser subparser) {
    super.configure(subparser);
    subparser.addArgument("-d", "--dump-file").help("path to the dump file to which the database is exported");
  }

  @Override
  public void onError(Cli cli, Namespace namespace, Throwable t) {
    // throw up to let our main() method do the desired error logging/handling
    throw new IllegalStateException("Error trying to export database: " + t.getMessage(), t);
  }

  @Override
  protected void run(final Bootstrap<InsightConfig> bootstrap, final Namespace namespace, final InsightConfig config)
      throws Exception
  {
    long start = System.currentTimeMillis();

    if (!config.isDatabaseEmbedded()) {
      throw new BadRequestException("The " + getName()
          + " command can only be used when no external database is specified in the server's config.yml file.");
    }

    DatabaseConfigProvider databaseConfigProvider = new DatabaseConfigProvider(config);
    OperationalDataStoreProvider.initWithoutMigration(databaseConfigProvider.getDatabaseConfig(DatabaseName.ods));
    if (!DatabaseUtil.schemaVersionTableExists(OperationalDataStoreProvider.getDataSource(),
        OperationalDataStoreProvider.ID)) {
      throw new BadRequestException("The server needs to have been started normally once before"
          + " in order to complete the required upgrade steps.");
    }
    if (DatabaseUtil.getDatabaseSchemaVersion(OperationalDataStoreProvider.getDataSource(),
        OperationalDataStoreProvider.ID) <= 0) {
      throw new BadRequestException("The database from the work directory " + config.getSonatypeWork().getAbsolutePath()
          + " is empty. Please verify you specified the correct config.yml file.");
    }
    AggregationDataStoreProvider
        .initWithoutMigration(databaseConfigProvider.getDatabaseConfig(DatabaseName.aggregation));
    ThirdPartyScansProvider
        .initWithoutMigration(databaseConfigProvider.getDatabaseConfig(DatabaseName.third_party_scans));

    String path = namespace.getString("dump_file");
    File dumpFile = path != null ? new File(path) : new File(config.getSonatypeWork(), "data/db-dump.sql.gz");
    dumpFile = dumpFile.getAbsoluteFile();

    log.info("Exporting database to {}", dumpFile);
    try (BufferedWriter writer =
        new BufferedWriter(new OutputStreamWriter(newOutputStream(dumpFile), StandardCharsets.UTF_8))) {
      export(writer, OperationalDataStoreProvider.getDataSource());
      export(writer, AggregationDataStoreProvider.getDataSource());
      export(writer, ThirdPartyScansProvider.getDataSource());
    }
    log.info("Completed export to '{}' in {} ms.", dumpFile, System.currentTimeMillis() - start);
  }

  private OutputStream newOutputStream(File dumpFile) throws Exception {
    OutputStream out = new FileOutputStream(dumpFile);
    if (dumpFile.getName().endsWith(".gz")) {
      out = new GZIPOutputStream(out);
    }
    return out;
  }

  /**
   * Delegates to H2's SCRIPT command for the heavy lifting in generating the SQL dump and post-processes its output to
   * be both compatible and efficient for use with PostgreSQL, specifically its psql client.
   * 
   * @see https://www.h2database.com/html/commands.html#script
   * @see https://www.postgresql.org/docs/10/app-psql.html
   */
  private void export(BufferedWriter writer, DataSource dataSource) throws Exception {
    log.info("Reading tables, please be patient");
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        ResultSet results = statement.executeQuery("SCRIPT SIMPLE NOSETTINGS BLOCKSIZE " + Integer.MAX_VALUE)) {
      String currentTable = null;
      while (results.next()) {
        String sql = results.getString(1);
        try {
          if (sql.startsWith("INSERT INTO ")) {
            String tableName = sql.substring("INSERT INTO ".length(), sql.indexOf('(')).trim();
            int valuesBegin = sql.indexOf(" VALUES");
            if (!tableName.equals(currentTable)) {
              writer.write(sql.substring(0, valuesBegin).replace("INSERT INTO ", "COPY "));
              writer.write(" FROM stdin;");
              writer.newLine();
              currentTable = tableName;
            }
            sql = transformInsertValues(sql.substring(sql.indexOf('(', valuesBegin) + 1, sql.lastIndexOf(");")));
          }
          else if (sql.startsWith("--")) {
            continue;
          }
          else {
            if (currentTable != null) {
              currentTable = null;
              writer.write("\\.");
              writer.newLine();
            }
            if (sql.startsWith("CREATE USER ")) {
              continue;
            }
            else if (sql.startsWith("CREATE SCHEMA ")) {
              sql = sql.replace(" AUTHORIZATION SA", "");
              sql = sql.replace(" IF NOT EXISTS", "");
              String schemaName = sql.substring("CREATE SCHEMA ".length(), sql.lastIndexOf(';')).trim();
              writer.write("DROP SCHEMA IF EXISTS ");
              writer.write(schemaName);
              writer.write(" CASCADE;");
              writer.newLine();
            }
            else if (sql.startsWith("CREATE CACHED TABLE ")) {
              String tableName = sql.substring("CREATE CACHED TABLE".length(), sql.indexOf('(')).trim();
              log.info("Exporting table {}", tableName);
              sql = sql.replace(" CACHED TABLE", " TABLE");
              sql = sql.replaceAll(" SELECTIVITY [0-9]+", "");
              sql = sql.replace(" DATETIME", " TIMESTAMP");
              sql = sql.replace(" CLOB", " TEXT");
            }
            else if (sql.startsWith("ALTER TABLE ")) {
              sql = sql.replaceAll("(?<= ADD CONSTRAINT )\"[^\"]+\"\\.", "");
              sql = sql.replace(" NOCHECK", "");
            }
            else if (sql.startsWith("CREATE INDEX ")) {
              sql = sql.replaceFirst("(?<=CREATE INDEX )\"[^\"]+\"\\.", "");
            }
            else if (sql.startsWith("CREATE UNIQUE INDEX ")) {
              // unique constraints should be used instead, which will result in the creation of a unique index;
              // having this CREATE UNIQUE INDEX statement in the sql could be an indication of a problem in H2
              // where the auto-generated unique indexes can become abandoned (foreign key constraint created
              // AFTER a unique constraint that uses that FK column and the unique constraint subsequently dropped)
              log.debug("Database dump contains a CREATE UNIQUE INDEX statement which will be ignored.");
              continue;
            }
          }
        }
        catch (Exception e) {
          throw new IllegalStateException("Failed to transform SQL command:\n" + sql, e);
        }
        writer.write(sql);
        writer.newLine();
      }
      writer.newLine();
    }
  }

  /**
   * Transforms the comma-separated values of a traditional INSERT statement into the tab-separated text format for
   * PostgreSQL's COPY command.
   * 
   * @see https://www.postgresql.org/docs/10/sql-copy.html#id-1.9.3.52.9.2
   */
  static String transformInsertValues(String values) {
    int length = values.length();
    StringBuilder builder = new StringBuilder(length);
    for (int i = 0; i < length;) {
      char c = values.charAt(i);
      if (c == ',') {
        builder.append('\t');
        i = skipOptionalWhitespace(values, i + 1);
      }
      else if (c == '\'') {
        i = transformSingleQuotedString(builder, values, i, true);
      }
      else if (c == 'N' && values.regionMatches(i, NULL_VALUE, 0, NULL_VALUE.length())) {
        i += NULL_VALUE.length();
        builder.append("\\N");
      }
      else if (c == 'T' && values.regionMatches(i, TIMESTAMP_PREFIX, 0, TIMESTAMP_PREFIX.length())) {
        i = skipOptionalWhitespace(values, i + TIMESTAMP_PREFIX.length());
        if (values.charAt(i) != '\'') {
          throw new IllegalStateException("Malformed TIMESTAMP: " + values.substring(i));
        }
        i = transformSingleQuotedString(builder, values, i, true);
      }
      else if (c == 'S' && values.regionMatches(i, STRINGDECODE_PREFIX, 0, STRINGDECODE_PREFIX.length())) {
        i = skipOptionalWhitespace(values, i + STRINGDECODE_PREFIX.length());
        if (values.charAt(i) != '\'') {
          throw new IllegalStateException("Malformed STRINGDECODE argument: " + values.substring(i));
        }
        i = transformSingleQuotedString(builder, values, i, false);
        i = skipOptionalWhitespace(values, i);
        if (values.charAt(i) != ')') {
          throw new IllegalStateException("Malformed STRINGDECODE argument list: " + values.substring(i));
        }
        i++;
      }
      else if (c == 'X' && i + 1 < length && values.charAt(i + 1) == '\'') {
        builder.append("\\\\x");
        i++;
      }
      else {
        builder.append(c);
        i++;
      }
    }
    return builder.toString();
  }

  private static int skipOptionalWhitespace(String values, int start) {
    for (int i = start;; i++) {
      if (values.charAt(i) != ' ') {
        return i;
      }
    }
  }

  private static int transformSingleQuotedString(
      StringBuilder builder,
      String values,
      int singleQuote,
      boolean stillNeedsEscaping)
  {
    boolean nextCharIsEscaped = false;
    for (int i = singleQuote + 1;; i++) {
      char c = values.charAt(i);
      if (c == '\'') {
        if (i + 1 < values.length() && values.charAt(i + 1) == c) {
          i++;
        }
        else {
          return i + 1;
        }
      }
      else if (c == '\\') {
        if (stillNeedsEscaping) {
          builder.append('\\');
        }
        else if (!nextCharIsEscaped && i + 5 < values.length() && values.charAt(i + 1) == 'u') {
          c = (char) Integer.parseInt(values.substring(i + 2, i + 6), 16);
          i += 5;
        }
      }
      builder.append(c);
      nextCharIsEscaped = !nextCharIsEscaped && c == '\\';
    }
  }
}
