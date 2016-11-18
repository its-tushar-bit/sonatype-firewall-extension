/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Named;

import org.codehaus.plexus.util.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.24
 */
@Named
public class TwistlockScanner
{
  private static final Logger log = LoggerFactory.getLogger(TwistlockPolicyEvaluator.class);

  /** Calls the Twistlock scanner and returns its stdout. */
  String scan(String twistlockScannerExecutable,
              String imageId,
              String twistlockConsoleUrl,
              String twistlockConsoleUsername,
              String twistlockConsolePassword)
  {
    long start = System.currentTimeMillis();

    log.info("Using Twistlock scanner '{}' to scan image with ID '{}'.", twistlockScannerExecutable, imageId);
    log.info("Connecting to Twistlock console at '{}' using user name '{}'.", twistlockConsoleUrl,
        twistlockConsoleUsername);

    ProcessBuilder processBuilder = new ProcessBuilder(twistlockScannerExecutable, //
        "-c", twistlockConsoleUrl, "-u", twistlockConsoleUsername, "-p", twistlockConsolePassword, //
        "-i", imageId, //
        "--include-files", "--include-package-files", //
        "--hash-method", "sha1");
    processBuilder.redirectErrorStream(true);
    Process twistlockScannerProcess;
    try {
      twistlockScannerProcess = processBuilder.start();
      int exitCode = twistlockScannerProcess.waitFor();

      String scannerOutput = getStdOutContent(twistlockScannerProcess);
      log.debug("Twistlock scanner output:\n{}", scannerOutput);

      if (exitCode != 0) {
        throw new RuntimeException(
            "The Twistlock scanner returned exit code = " + exitCode + ". Output: " + scannerOutput);
      }

      log.info("Scanned image with ID '{}' in {} ms.", imageId, System.currentTimeMillis() - start);

      return scannerOutput;
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
    catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private String getStdOutContent(Process process) throws IOException {
    InputStream stdOut = process.getInputStream();
    try {
      return IOUtil.toString(stdOut, "UTF-8");
    }
    finally {
      stdOut.close();
    }
  }
}
