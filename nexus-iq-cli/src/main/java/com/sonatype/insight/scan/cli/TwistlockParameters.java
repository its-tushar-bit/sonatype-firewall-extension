/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.util.List;

import com.beust.jcommander.Parameter;

/**
 * @since 1.24
 */
public class TwistlockParameters
    extends AbstractCliParameters
{
  @Parameter(description = "The image ID (full or prefix) or the image full name [REPOSITORY[:TAG]] (required)."
      + " Example: 63a92d0c131d", required = true)
  private List<String> scanTargets;

  @Parameter(names = {"--twistlock-scanner-executable"},
             description = "Executable for the Twistlock scanner/CLI. Example: twistlock-2-2-100/twistcli",
             required = true)
  private String twistlockScannerExecutable;

  @Parameter(names = {"--twistlock-console-url"},
             description = "URL for the Twistlock console. Example: https://localhost:8083", required = true)
  private String twistlockConsoleUrl;

  @Parameter(names = {
      "--twistlock-console-username" }, description = "User name for the Twistlock console", required = true)
  private String twistlockConsoleUsername;

  @Parameter(names = {
      "--twistlock-console-password" }, description = "Password for the Twistlock console", required = true)
  private String twistlockConsolePassword;

  public TwistlockParameters() {
  }

  public TwistlockParameters(String... args) {
    super(args);
  }

  @Override
  protected String getProgramName() {
    return "java -cp nexus-iq-cli.jar com.sonatype.insight.scan.cli.TwistlockPolicyEvaluatorCli";
  }

  public String getTwistlockScannerExecutable() {
    return twistlockScannerExecutable;
  }

  public String getTwistlockConsoleUrl() {
    return twistlockConsoleUrl;
  }

  public String getTwistlockConsoleUsername() {
    return twistlockConsoleUsername;
  }

  public String getTwistlockConsolePassword() {
    return twistlockConsolePassword;
  }

  @Override
  public List<String> getScanTargets() {
    return scanTargets;
  }
}
