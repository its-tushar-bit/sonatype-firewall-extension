/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;

/**
 * @since 1.10
 */
public class Parameters
    extends AbstractParameters
{
  @Parameter(description = "<Archives or directories to scan>", required = true)
  private List<String> scanTargets = new ArrayList<>();

  @Parameter(names = {"-b", "--bundle-file"},
             description = "Path to file where the report bundle ZIP file will be downloaded")
  private File reportBundleFile = new File("report.zip");

  @Parameter(names = {"-a", "--authentication"},
             description = "Authentication credentials to use for the IQ Server, format <username:password> ",
             required = true)
  private String serverUser;

  public Parameters() {
  }

  public Parameters(String... args) {
    parse(args);
  }

  @Override
  protected String getProgramName() {
    return "java -jar nexus-iq-integrator-scanner.jar";
  }

  public File getReportBundleFile() {
    return reportBundleFile;
  }

  @Override
  public String getServerUser() {
    return serverUser;
  }

  @Override
  public List<String> getScanTargets() {
    return scanTargets;
  }
}
