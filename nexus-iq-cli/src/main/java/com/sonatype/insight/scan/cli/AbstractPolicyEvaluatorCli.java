/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.text.SimpleDateFormat;
import java.util.Date;

import ch.qos.logback.classic.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractPolicyEvaluatorCli
{

  static final String PROP_OUTPUT_DIRECTORY = "com.sonatype.insight.scan.outDir";

  static final String PROP_START_TIME = "com.sonatype.insight.scan.startTime";

  protected void run(Class<? extends PolicyEvaluator> policyEvaluatorClass, AbstractCliParameters params) {
    PolicyEvaluator<AbstractCliParameters> main = boot(policyEvaluatorClass, params);

    try {
      main.run(params);
    }
    catch (ExitException e) {
      System.exit(e.getExitCode());
    }
  }

  private <T extends PolicyEvaluator<?>> T boot(Class<T> type, AbstractParameters params) {
    initLogging(params);

    if (params.getError() != null) {
      params.printUsage();

      // NOTE: Acquire logger after initLogging()
      Logger log = LoggerFactory.getLogger(type);
      log.error(params.getError().getMessage());

      System.exit(1);
    }

    if (params.isHelp()) {
      params.printUsage();
      System.exit(0);
    }

    return org.eclipse.sisu.launch.Main.boot(type, params.getArgs());
  }

  private static void initLogging(AbstractParameters params) {
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

}
