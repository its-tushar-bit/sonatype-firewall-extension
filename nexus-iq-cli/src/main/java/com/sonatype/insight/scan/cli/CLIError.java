/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

public class CLIError
{
  private final String errorMessage;

  private final boolean systemError;

  private final boolean scanningError;

  private CLIError(String errorMessage, boolean systemError, boolean scanningError) {
    this.errorMessage = errorMessage;
    this.systemError = systemError;
    this.scanningError = scanningError;
  }

  public static CLIError forSystemError(String errorMessage) {
    return new CLIError(errorMessage, true, false);
  }

  public static CLIError forScanningError(String errorMessage) {
    return new CLIError(errorMessage, false, true);
  }

  public static CLIError forConfigurationError(String errorMessage) {
    return new CLIError(errorMessage, false, false);
  }

  public String getErrorMessage() {
    return this.errorMessage;
  }

  public boolean isSystemError() {
    return this.systemError;
  }

  public boolean isScanningError() {
    return this.scanningError;
  }
}
