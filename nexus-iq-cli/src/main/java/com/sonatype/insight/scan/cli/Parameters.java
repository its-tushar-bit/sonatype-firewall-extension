/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.File;
import java.util.List;

import com.beust.jcommander.Parameter;

public class Parameters
    extends AbstractParameters
{
  @Parameter(names = {"-w", "--fail-on-policy-warnings"}, description = "Fail on policy evaluation warnings")
  private boolean failOnPolicyWarning;

  @Parameter(names = {"-r", "--result-file"}, description = "Path to a JSON file where the results "
      + "of the policy evaluation will be stored in a machine-readable format")
  private File resultFile;

  @Parameter(names = {"-a", "--authentication"},
      description = "Authentication credentials to use for the IQ Server, format <username:password> ")
  private String serverUser;

  /**
   * @since 1.25
   */
  @Parameter(names = {"--pki-authentication"}, description = "Delegate to the JVM for PKI authentication")
  private boolean pkiAuthentication;

  @Parameter(description = "Archives or directories to scan", required = true)
  private List<String> scanTargets;

  /**
   * @since 1.34
   */
  @Parameter(names = { "-xc", "--expanded-coverage" }, description = "Enable Expanded Coverage analysis.")
  private boolean expandedCoverageMode;

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

  public Parameters() {
  }

  public Parameters(String... args) {
    parse(args);
  }

  @Override
  public List<String> getScanTargets() {
    return scanTargets;
  }

  public boolean isExpandedCoverageMode() {
    return expandedCoverageMode;
  }
}
