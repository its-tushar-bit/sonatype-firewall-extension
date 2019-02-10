/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    List<String> twistlockScannerCommand = buildTwistlockScannerCommand(twistlockScannerExecutable, imageId,
        twistlockConsoleUrl, twistlockConsoleUsername, twistlockConsolePassword);

    String scannerOutput = runTwistlockScannerCommand(twistlockScannerCommand);

    log.info("Scanned image with ID '{}' in {} ms.", imageId, System.currentTimeMillis() - start);
    return scannerOutput;
  }

  String runTwistlockScannerCommand(List<String> twistlockScannerCommand) {
    ProcessBuilder processBuilder = new ProcessBuilder(twistlockScannerCommand);
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

      return scannerOutput;
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

  private List<String> buildTwistlockScannerCommand(String twistlockScannerExecutable,
                                                    String imageId,
                                                    String twistlockConsoleUrl,
                                                    String twistlockConsoleUsername,
                                                    String twistlockConsolePassword)
  {
    List<String> twistlockScannerCommand = new ArrayList<>();
    twistlockScannerCommand.addAll(Arrays.asList(twistlockScannerExecutable, //
        "images", "scan", //
        "--address", twistlockConsoleUrl, "--user", twistlockConsoleUsername, //
        "--include-files", "--include-package-files", //
        "--hash", "sha1", //
        "--upload", //
        imageId));

    logTwistlockScannerCommand(twistlockScannerCommand);

    // Add the password parameter only after we logged the command.
    // The image ID must be the last parameter, so first remove it and add it back after the password is added.
    twistlockScannerCommand.remove(twistlockScannerCommand.size() - 1);
    twistlockScannerCommand.add("--password");
    twistlockScannerCommand.add(twistlockConsolePassword);
    twistlockScannerCommand.add(imageId);

    return twistlockScannerCommand;
  }

  private void logTwistlockScannerCommand(List<String> twistlockScannerCommand) {
    StringBuffer command = new StringBuffer();
    for (String s : twistlockScannerCommand) {
      command.append(s).append(" ");
    }
    log.info("Twistlock scanner command (password excluded): {}", command);
  }

  private String getStdOutContent(Process process) throws IOException {
    try (InputStream stdOut = process.getInputStream()) {
      return IOUtil.toString(stdOut, "UTF-8");
    }
  }
}
