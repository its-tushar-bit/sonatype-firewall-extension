/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import ch.qos.logback.classic.Level;

public class PolicyEvaluatorCli
{

  static final String PROP_OUTPUT_DIRECTORY = "com.sonatype.insight.scan.outDir";

  static final String PROP_START_TIME = "com.sonatype.insight.scan.startTime";

  public static void main(String[] args) {
    Parameters params = new Parameters(args);

    PolicyEvaluator main = boot(PolicyEvaluator.class, params);

    try {
      main.run(params);
    }
    catch (ExitException e) {
      System.exit(e.getExitCode());
    }
  }

  private static <T> T boot(Class<T> type, Parameters params) {
    initLogging(params);

    if (params.getError() != null) {
      params.printUsage();

      // NOTE: Acquire logger after initLogging()
      Logger log = LoggerFactory.getLogger(PolicyEvaluatorCli.class);
      log.error(params.getError().getMessage());
      log.error(confidential(), "Actual arguments were: {}", Arrays.asList(params.getArgs()));

      System.exit(1);
    }

    if (params.isHelp()) {
      params.printUsage();
      System.exit(0);
    }

    return org.sonatype.guice.bean.containers.Main.boot(type, params.getArgs());
  }

  private static void initLogging(Parameters params) {
    System.setProperty(PROP_OUTPUT_DIRECTORY, params.getOutputDirectory().getAbsolutePath());
    System.setProperty(PROP_START_TIME, new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()));

    if (params.isDebug()) {
      setLogLevel(Level.DEBUG);
    }
    else if (params.isQuiet()) {
      setLogLevel(Level.ERROR);
    }
  }

  private static void setLogLevel(Level level) {
    setLogLevel(level, org.slf4j.Logger.ROOT_LOGGER_NAME, "com.sonatype.insight.scan");
  }

  private static void setLogLevel(Level level, String... loggers) {
    for (String logger : loggers) {
      ch.qos.logback.classic.Logger log = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(logger);
      log.setLevel(level);
    }
  }

  private static Marker confidential() {
    return MarkerFactory.getDetachedMarker("CONFIDENTIAL");
  }

}
