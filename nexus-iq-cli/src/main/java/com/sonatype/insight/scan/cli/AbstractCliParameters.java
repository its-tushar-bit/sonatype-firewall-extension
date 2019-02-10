/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.File;

import com.beust.jcommander.Parameter;

abstract class AbstractCliParameters
    extends AbstractParameters
{
  @Parameter(names = { "-w", "--fail-on-policy-warnings" }, description = "Fail on policy evaluation warnings")
  private boolean failOnPolicyWarning;

  @Parameter(names = { "-r", "--result-file" }, description = "Path to a JSON file where the results "
      + "of the policy evaluation will be stored in a machine-readable format")
  private File resultFile;

  @Parameter(names = {"-a", "--authentication"},
             description = "Authentication credentials to use for the IQ Server, format <username:password> ")
  private String serverUser;

  /**
   * @since 1.25
   */
  @Parameter(names = { "--pki-authentication" }, description = "Delegate to the JVM for PKI authentication")
  private boolean pkiAuthentication;

  AbstractCliParameters() {
  }

  AbstractCliParameters(String... args) {
    parse(args);
  }

  @Override
  protected String getProgramName() {
    return "java -jar nexus-iq-cli.jar";
  }

  public File getResultFile() {
    return resultFile;
  }

  public boolean isFailOnPolicyWarning() {
    return failOnPolicyWarning;
  }

  @Override
  public String getServerUser() {
    return serverUser;
  }

  public boolean isPkiAuthentication() {
    return pkiAuthentication;
  }
}
