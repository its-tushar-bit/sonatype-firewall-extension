/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.urlrunner;

import java.io.File;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.commons.lang3.StringUtils;

class Parameters
{
  private String[] args = {};

  private Exception error;

  @Parameter(names = { "-f", "--input-file" }, description = "path of the input file", required = true)
  private File inputFile = new File("");

  @Parameter(names = { "-s", "--server" }, description = "IQ Server URL in the form of http://server:port",
      required = true)
  private String server;

  @Parameter(names = { "-u", "--username" }, description = "IQ Server username")
  private String username = "admin";

  @Parameter(names = { "-p", "--password" }, description = "IQ Server password")
  private String password = "admin123";

  @Parameter(names = { "-a", "--adminUrl" }, description = "IQ Server Admin URL in the form of http://server:port")
  private String adminServer;

  @Parameter(names = { "-pr", "--proxy" }, description = "Proxy Server URL in the form of http://server:port")
  private String proxy;

  @Parameter(names = { "-h", "--help" }, description = "Show this help screen")
  private boolean help;

  public Parameters() {
  }

  public Parameters(String... args) {
    parse(args);
  }

  public void printUsage() {
    JCommander jc = new JCommander(new Parameters());
    jc.setProgramName(getProgramName());
    jc.usage();
  }

  private String getProgramName() {
    return "java -jar nexus-iq-tools.jar urlrunner";
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

  public boolean isHelp() {
    return help;
  }

  public File getInputFile() {
    return inputFile;
  }

  public String getServer() {
    return getServerUrl(this.server);
  }

  public String getAdminServer() {
    return getServerUrl(this.adminServer);
  }

  private String getServerUrl(String serverString) {
    String serverUrl = serverString;
    if (StringUtils.isNotEmpty(serverString) && !serverString.contains("//")) {
      serverUrl = "http://" + serverString;
    }
    return serverUrl;
  }

  public String getProxy() {
    return proxy;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }
}
