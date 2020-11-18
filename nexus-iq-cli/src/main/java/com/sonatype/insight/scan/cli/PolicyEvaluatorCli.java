/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import ch.qos.logback.classic.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyEvaluatorCli
{
  static final String PROP_OUTPUT_DIRECTORY = "com.sonatype.insight.scan.outDir";

  static final String PROP_START_TIME = "com.sonatype.insight.scan.startTime";

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

  public static void main(String[] args) {
    Parameters params = new Parameters(args);

    Class<? extends PolicyEvaluator> policyEvaluatorClass =
        params.isExpandedCoverageMode() ? ExpandedCoveragePolicyEvaluator.class : DefaultPolicyEvaluator.class;

    new PolicyEvaluatorCli().run(policyEvaluatorClass, params);
  }

  protected void run(
      Class<? extends PolicyEvaluator> policyEvaluatorClass,
      Parameters params)
  {
    try {
      PolicyEvaluator main = boot(policyEvaluatorClass, params);
      main.run(params);
    }
    catch (ExitException e) {
      System.exit(e.getExitCode());
    }
  }

  private <T extends PolicyEvaluator> T boot(Class<T> type, AbstractParameters params) throws ExitException {
    initLogging(params);

    // NOTE: Acquire logger after initLogging()
    Logger log = LoggerFactory.getLogger(type);

    if (params.isVersion()) {
      log.info(getVersion());
      throw new ExitException(0);
    }

    if (params.isHelp()) {
      log.info(params.createUsageHelp());
      throw new ExitException(0);
    }

    if (params.getError() != null) {
      log.error(params.createUsageHelp());
      log.error(params.getError().getMessage());

      throw new ExitException(1);
    }

    return instantiate(type, params);
  }

  protected String getVersion() throws ExitException {
    try {
      final Properties properties = new Properties();
      properties.load(getClass().getResourceAsStream("/com/sonatype/insight/scan/scanner.properties"));
      return properties.getProperty("version");
    }
    catch (Exception e) {
      throw new ExitException(1, "Unable to determine version");
    }
  }

  /**
   * @throws ExitException Subclasses can throw this exception for errors with specific exit codes
   */
  protected <T extends PolicyEvaluator> T instantiate(final Class<T> type, final AbstractParameters params)
      throws ExitException
  {
    return org.eclipse.sisu.launch.Main.boot(type, params.getArgs());
  }
}
