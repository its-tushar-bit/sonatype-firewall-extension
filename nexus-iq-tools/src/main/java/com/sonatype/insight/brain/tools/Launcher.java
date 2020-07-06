/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;

import com.sonatype.insight.brain.tools.dbutil.DbUtil;
import com.sonatype.insight.brain.tools.reportgenerator.ReportGenerator;
import com.sonatype.insight.brain.tools.resultdiff.ResultDiff;
import com.sonatype.insight.brain.tools.dbmodifier.DbModifierCli;
import com.sonatype.insight.brain.tools.scanscrubber.ScanScrubber;
import com.sonatype.insight.brain.tools.urlrunner.UrlRunnerCli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Launcher
{
  private static final Logger log = LoggerFactory.getLogger(Launcher.class);

  private String[] args = {};

  @Parameter(names = { "dbutil", "-dbutil" }, description = "Run DbUtil.")
  boolean runDbUtil;

  @Parameter(names = { "reportgenerator", "-reportgenerator" },
          description = "Run ReportGenerator.")
  boolean reportGenerator;

  @Parameter(names = { "urlrunner", "-urlrunner" }, description = "Run UrlRunner.")
  boolean runUrlRunner;

  @Parameter(names = { "resultdiff", "-resultdiff" }, description = "Run ResultDiff.")
  boolean runResultDiff;

  @Parameter(names = { "dbmod", "-dbmod" }, description = "Run DbModifier.")
  boolean dbMod;

  @Parameter(names = {"scanscrubber", "-scanscrubber"}, description = "Run ScanScrubber.")
  boolean scanScrubber;

  private Launcher() {
  }

  // visible for testing
  Launcher(String... args) {
    parse(args);
  }

  private void launch() {
    String[] launchParams = Arrays.copyOfRange(args, 1, args.length);
    Optional<Consumer<String[]>> target = Optional.empty();
    target = runDbUtil ? Optional.of(DbUtil::main) : target;
    target = reportGenerator ? Optional.of(ReportGenerator::main) : target;
    target = runUrlRunner ? Optional.of(UrlRunnerCli::main) : target;
    target = runResultDiff ? Optional.of(ResultDiff::main) : target;
    target = dbMod ? Optional.of(DbModifierCli::main) : target;
    target = scanScrubber ? Optional.of(ScanScrubber::main) : target;

    target.ifPresent(util -> util.accept(launchParams));
  }

  public static void printUsage() {
    JCommander jc;
    // NOTE: Be sure to use a fresh params instance to not have current state spoil default values
    jc = new JCommander(new Launcher());

    jc.setProgramName(getProgramName());
    jc.usage();
  }

  private static String getProgramName() {
    return "java -jar nexus-iq-tools.jar";
  }

  private void parse(String... args) {
    try {
      this.args = args.clone();
      JCommander jc = new JCommander(this);
      jc.parse(args[0]);
    }
    catch (RuntimeException e) {
      printUsage();
      System.exit(1);
    }
  }

  public static void main(String[] args) {
    if (args.length == 0) {
      printUsage();
      System.exit(1);
    }
    try {
      new Launcher(args).launch();
    }
    catch (Exception e) {
      log.error(e.getMessage(), e);
      System.exit(1);
    }
  }
}
