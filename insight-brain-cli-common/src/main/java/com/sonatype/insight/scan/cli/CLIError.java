/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

public class CLIError
{
  private String errorMessage;

  private boolean systemError;

  private CLIError(String errorMessage, boolean systemError) {
    this.errorMessage = errorMessage;
    this.systemError = systemError;
  }

  public static CLIError forSystemError(String errorMessage) {
    return new CLIError(errorMessage, true);
  }

  public static CLIError forConfigurationError(String errorMessage) {
    return new CLIError(errorMessage, false);
  }

  public String getErrorMessage() {
    return this.errorMessage;
  }

  public boolean isSystemError() {
    return this.systemError;
  }
}
