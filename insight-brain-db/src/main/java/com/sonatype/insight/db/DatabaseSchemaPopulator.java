/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Vendored/copied from hosted-data-services/insight-db-common
package com.sonatype.insight.db;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

public class DatabaseSchemaPopulator
    extends AbstractDatabaseSchemaPopulator
{
  private static final Logger log = LoggerFactory.getLogger(DatabaseSchemaPopulator.class);

  private final ResourceLoader resourceLoader = new DefaultResourceLoader();

  private final ResourceDatabasePopulator resourceDatabasePopulator = new ResourceDatabasePopulator();

  public DatabaseSchemaPopulator(
      DataSource dataSource,
      DatabaseEngine databaseEngine,
      String dataStoreId,
      String schemaName)
  {
    super(dataSource, databaseEngine, dataStoreId, schemaName);
    String scriptsPath = "/db/" + dataStoreId + "/";
    String scriptsFilename = getScriptsResource(databaseEngine, scriptsPath);
    log.debug("Loading list of database scripts from {}", scriptsFilename);
    List<String> scripts = getScripts(scriptsFilename);
    for (String script : scripts) {
      log.debug("  Found script {}", script);
      addScript(scriptsPath + script);
    }
  }

  void addScript(String script) {
    resourceDatabasePopulator.addScript(resourceLoader.getResource(script));
  }

  @Override
  void doPopulate(Connection connection) {
    resourceDatabasePopulator.populate(connection);
  }

  String getScriptsResource(DatabaseEngine databaseEngine, String scriptsPath) {
    String scriptsResource = scriptsPath + "scripts_" + databaseEngine.getId() + ".txt";
    if (getClass().getResource(scriptsResource) == null) {
      scriptsResource = scriptsPath + "scripts.txt";
    }
    return scriptsResource;
  }

  private List<String> getScripts(String scriptsFilename) {
    List<String> scripts = new ArrayList<>();
    try (InputStream is = getClass().getResourceAsStream(scriptsFilename);
        LineNumberReader reader = new LineNumberReader(new InputStreamReader(is, StandardCharsets.UTF_8)))
    {
      String line = reader.readLine();
      while (line != null) {
        if (!line.trim().isEmpty()) {
          scripts.add(line);
        }
        line = reader.readLine();
      }
      return scripts;
    }
    catch (IOException e) {
      throw new DatabaseException(e);
    }
  }
}
