/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.dbutil;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sonatype.insight.brain.tools.common.PerfTestConfig;
import com.sonatype.insight.brain.tools.common.PerfTestConfig.TestUrl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
Generates a list of target URLs based on an input template, provided data source and config params.
General layout & function:
 - Load target URLs from template
 - Build Replaces from list of ReplacementSources
 - Generate new URL targets by passing template URLs through each Replacer
 - Output final URL list to the command line
 */
public class DbUtil
{
  private static final Logger log = LoggerFactory.getLogger(DbUtil.class);

  private static final String defaultUrls = "/dbutil/DEFAULT_TARGET_URLS.json";

  private final DbUtilParameters parameters;

  protected DbUtil(DbUtilParameters params) {
    parameters = params;
  }

  private void run() throws Exception {
    if (parameters.isGenerateTemplate()) {
      IOUtils.copy(getClass().getResourceAsStream(defaultUrls), System.out);
      return;
    }
    if (parameters.getError() != null) {
      parameters.printUsage();
      log.error("arguments passed: {}", Arrays.asList(parameters.getArgs()));
      System.exit(1);
    }
    runCore();
  }

  private void runCore() throws Exception {

    PerfTestConfig config = parameters.getUrls() == null
        ? new ObjectMapper().readValue(getClass().getResource(defaultUrls), PerfTestConfig.class)
        : new ObjectMapper().readValue(parameters.getUrls(), PerfTestConfig.class);

    List<Replacer> replacers = loadReplacers();

    List<TestUrl> targetUrls = new ArrayList<>();

    for (TestUrl url : config.getUrls()) {
      replacers.forEach(replacer -> targetUrls.addAll(replacer.generateUrls(url)));
    }

    PerfTestConfig perfTestConfig = new PerfTestConfig();
    perfTestConfig.setUrls(targetUrls);

    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

    log.info(mapper.writeValueAsString(perfTestConfig));
    log.debug("Count: {}", perfTestConfig.getUrls().size());
  }

  private Connection getConnection() throws Exception {
    String dbUrl;
    if (parameters.isPostgres()) {
      dbUrl = "jdbc:postgresql://" + parameters.getHostname() + ":" + parameters.getPort() + "/"
          + parameters.getDatabase() + "?currentSchema=insight_brain_ods";
    }
    else {
      dbUrl = "jdbc:h2:" + new File(parameters.getDatabase()).getAbsolutePath()
          + ";DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;SCHEMA=insight_brain_ods;MV_STORE=FALSE";
    }
    String dbUsername = parameters.getDbUser();
    String dbPassword = parameters.getDbPassword();
    return DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
  }

  private List<ReplacementSource> getReplacementSources() {
    List<ReplacementSource> replaceSources = new ArrayList<>();
    replaceSources.add(new PolicySelector());
    replaceSources.add(new OrganizationSelector());
    replaceSources.add(new ApplicationEvalSelector());
    replaceSources.add(new StageComponentSelector());
    replaceSources.add(new CompoundSelector(new ApplicationListSelector(), new OrganizationListSelector()));
    return replaceSources;
  }

  private List<Replacer> loadReplacers() throws Exception {
    List<Replacer> replacers = new ArrayList<>();
    try (Connection conn = getConnection()) {
      for (ReplacementSource rs : getReplacementSources()) {
        replacers.add(rs.buildReplacer(conn, parameters));
      }
    }
    replacers.add(Replacer.DIRECT_URLS);
    return replacers;
  }

  public static void main(String[] args) {
    DbUtilParameters params = new DbUtilParameters(args);

    try {
      new DbUtil(params).run();
    }
    catch (Exception e) {
      log.error(e.getMessage(), e);
      System.exit(1);
    }
  }
}
