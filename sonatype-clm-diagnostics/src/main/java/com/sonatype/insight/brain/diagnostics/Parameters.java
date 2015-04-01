/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.diagnostics;

import java.io.File;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

public class Parameters
{
  private String[] args = {};

  private Exception error;

  @Parameter(names = { "-w", "--sonatype-work" }, description = "Path to work directory of CLM server, cf. sonatypeWork setting from server's config.yml")
  private File workDirectory = new File("sonatype-work/clm-server");

  @Parameter(names = { "-c", "--compact-database" }, description = "Compact the database by reclaiming empty space")
  private boolean compact;

  @Parameter(names = { "-h", "--help" }, description = "Show this help screen")
  private boolean help;

  public Parameters() {
  }

  public Parameters(String... args) {
    parse(args);
  }

  public void printUsage() {
    JCommander jc;
    try {
      // NOTE: Be sure to use a fresh params instance to not have current state spoil default values
      jc = new JCommander(getClass().newInstance());
    }
    catch (Exception e) {
      throw new IllegalStateException(e);
    }
    jc.setProgramName(getProgramName());
    jc.usage();
  }

  private String getProgramName() {
    return "java -jar sonatype-clm-diagnostics.jar";
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

  public File getWorkDirectory() {
    return workDirectory;
  }

  public boolean isCompact() {
    return compact;
  }
}
