/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.dbutil;

import java.io.File;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

public class DbUtilParameters
{
  private String[] args = {};

  private Exception error;

  @Parameter(names = {"--postgres"}, description = "Flag whose existence determines if the database engine is Postgres")
  private boolean postgres;

  // required
  @Parameter(names = { "-db", "--database" },
      description = "Path to h2 db file: e.g. ~/test/ods.h2.db, or name of the Postgres database.", required = true)
  private String database;

  @Parameter(names = {"-user", "--db-user"}, description = "db username")
  private String dbUser = "sa";

  @Parameter(names = {"-pass", "--db-password"}, description = "db password")
  private String dbPassword = "";

  @Parameter(names = {"-h", "--hostname"}, description = "Hostname of Postgres server")
  private String hostname;

  @Parameter(names = {"-p", "--port"}, description = "Port of Postgres server")
  private int port;

  // strongly suggested
  @Parameter(names = { "-u", "--urls" }, description = "Path to file containing target URL templates.")
  private File urls;

  // special
  @Parameter(names = { "-gt", "--generate-template" }, description = "Output the default template and exit.")
  private boolean generateTemplate;

  // limits
  @Parameter(names = { "-max-org", "--max-organizations" }, description = "Maximum number of organization values.")
  private Integer maxOrganizations = 20;

  @Parameter(names = { "-max-app", "--max-applications" }, description = "Maximum number of application values.")
  private Integer maxApplications = 10;

  @Parameter(names = { "-max-eval", "--max-evaluations" }, //
      description = "Maximum number of evaluations values per application.")
  private Integer maxEvaluations = 1;

  @Parameter(names = { "-max-pol", "--max-policies" }, description = "Maximum number of policy values.")
  private Integer maxPolicies = 15;

  @Parameter(names = { "-max-comp", "--max-components" }, description = "Maximum number of component values.")
  private Integer maxComponents = 10;

  // stage filters
  @Parameter(names = { "-xproxy", "--exclude-proxy" }, description = "Exclude evaluations for stage type 'proxy'.")
  private boolean excludeProxy;

  @Parameter(names = { "-xdevelop",
      "--exclude-develop" }, description = "Exclude evaluations for stage type 'develop'.")
  private boolean excludeDevelop;

  @Parameter(names = { "-xbuild", "--exclude-build" }, description = "Exclude evaluations for stage type 'build'.")
  private boolean excludeBuild;

  @Parameter(names = { "-xstage", "--exclude-stage" }, description = "Exclude evaluations for stage type 'stage'.")
  private boolean excludeStage;

  @Parameter(names = { "-xrelease",
      "--exclude-release" }, description = "Exclude evaluations for stage type 'release'.")
  private boolean excludeRelease;

  @Parameter(names = { "-xoperate",
      "--exclude-operate" }, description = "Exclude evaluations for stage type 'operate'.")
  private boolean excludeOperate;

  // evaluation filters

  // violation filters
  @Parameter(names = { "-xopen", "--exclude-open" }, description = "Exclude open violations.")
  private boolean excludeOpen;

  @Parameter(names = { "-xwaived", "--exclude-waived" }, description = "Exclude waived violations.")
  private boolean excludeWaived;

  @Parameter(names = { "-xfixed", "--exclude-fixed" }, description = "Exclude fixed violations.")
  private boolean excludeFixed;

  // end params

  public DbUtilParameters() {
  }

  public DbUtilParameters(String... args) {
    parse(args);
  }

  public void printUsage() {
    JCommander jc;
    // NOTE: Be sure to use a fresh params instance to not have current state spoil default values
    jc = new JCommander(new DbUtilParameters());

    jc.setProgramName(getProgramName());
    jc.usage();
  }

  private String getProgramName() {
    return "java -jar nexus-iq-tools.jar dbutil";
  }

  private void parse(String... args) {
    try {
      this.args = args.clone();
      error = null;
      JCommander jc = new JCommander(this);
      jc.parse(args);
    }
    catch (RuntimeException e) {
      error = e;
    }
  }

  public String[] getArgs() {
    return args;
  }

  public Exception getError() {
    return error;
  }

  public boolean isGenerateTemplate() {
    return generateTemplate;
  }

  public boolean isPostgres() {
    return postgres;
  }

  public String getDatabase() {
    return database;
  }

  public File getUrls() {
    return urls;
  }

  public String getDbUser() {
    return dbUser;
  }

  public String getDbPassword() {
    return dbPassword;
  }

  public String getHostname() {
    return hostname;
  }

  public int getPort() {
    return port;
  }

  // limits

  public Integer getMaxOrganizations() {
    return maxOrganizations;
  }

  public Integer getMaxApplications() {
    return maxApplications;
  }

  public Integer getMaxEvaluations() {
    return maxEvaluations;
  }

  public Integer getMaxPolicies() {
    return maxPolicies;
  }

  public Integer getMaxComponents() {
    return maxComponents;
  }

  // application filters

  // stage type filters - exclude the flagged stage
  public boolean excludeProxy() {
    return excludeProxy;
  }

  public boolean excludeDevelop() {
    return excludeDevelop;
  }

  public boolean excludeBuild() {
    return excludeBuild;
  }

  public boolean excludeStage() {
    return excludeStage;
  }

  public boolean excludeRelease() {
    return excludeRelease;
  }

  public boolean excludeOperate() {
    return excludeOperate;
  }
  // evaluation filters

  // violation filters
  public boolean excludeOpen() {
    return excludeOpen;
  }

  public boolean excludeFixed() {
    return excludeFixed;
  }

  public boolean excludeWaived() {
    return excludeWaived;
  }
}
