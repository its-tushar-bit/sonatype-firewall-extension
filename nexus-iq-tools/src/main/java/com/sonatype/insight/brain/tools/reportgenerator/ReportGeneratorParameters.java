/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.tools.reportgenerator;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

public class ReportGeneratorParameters
{
  @Parameter(names = "--template", description = "Path to the template JSON file",
          required = true)
  private String template;

  @Parameter(names = "--queries", description = "Path to the queries JSON file", required = true)
  private String queries;

  @Parameter(names = "--postgres", description = "Flag for PostgreSQL or H2")
  private boolean postgres;

  @Parameter(names = "--database",
          description = "For PostgreSQL is the database name. For H2 is the path to the database",
          required = true)
  private String database;

  @Parameter(names = "--user", description = "Database user")
  private String user = "sa";

  @Parameter(names = "--password", description = "Database password")
  private String password;

  @Parameter(names = "--hostname", description = "PostgreSQL hostname")
  private String hostname = "localhost";

  @Parameter(names = "--port", description = "PostgreSQL port")
  private int port = 5432;

  @Parameter(names = "--report-cache-zip",
          description = "Path to the zip file with the report files (report-zip and report-cache)",
          required = true)
  private String reportAndCacheZip;

  @Parameter(names = "--sonatype-work", description = "Path to the Sonatype work folder",
          required = true)
  private String sonatypeWork;

  public void printUsage() {
    JCommander jc = new JCommander(this);
    jc.setProgramName("java -jar nexus-iq-tools.jar reportgenerator");
    jc.usage();
  }

  public String getTemplate() {
    return template;
  }

  public void setTemplate(String template) {
    this.template = template;
  }

  public String getQueries() {
    return queries;
  }

  public void setQueries(String queries) {
    this.queries = queries;
  }

  public boolean isPostgres() {
    return postgres;
  }

  public void setPostgres(boolean postgres) {
    this.postgres = postgres;
  }

  public String getDatabase() {
    return database;
  }

  public void setDatabase(String database) {
    this.database = database;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getReportAndCacheZip() {
    return reportAndCacheZip;
  }

  public void setReportAndCacheZip(String reportAndCacheZip) {
    this.reportAndCacheZip = reportAndCacheZip;
  }

  public String getSonatypeWork() {
    return sonatypeWork;
  }

  public void setSonatypeWork(String sonatypeWork) {
    this.sonatypeWork = sonatypeWork;
  }

  @Override
  public String toString() {
    return "ReportGeneratorParameters{" +
            "template='" + template + '\'' +
            ", queries='" + queries + '\'' +
            ", database='" + database + '\'' +
            ", reportAndCacheZip='" + reportAndCacheZip + '\'' +
            ", sonatypeWork='" + sonatypeWork + '\'' +
            '}';
  }
}
