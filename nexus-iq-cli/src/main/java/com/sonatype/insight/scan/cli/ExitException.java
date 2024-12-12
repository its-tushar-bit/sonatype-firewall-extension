/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

public class ExitException
    extends Exception
{
  private static final long serialVersionUID = 1860065432528002161L;

  private final int exitCode;

  public ExitException(int exitCode) {
    this.exitCode = exitCode;
  }

  public ExitException(int exitCode, String message) {
    super(message);
    this.exitCode = exitCode;
  }

  public ExitException(boolean ignorable, Throwable cause) {
    super(cause);
    this.exitCode = ignorable ? 0 : 1;
  }

  public ExitException(boolean ignorable, String message) {
    super(message);
    this.exitCode = ignorable ? 0 : 1;
  }

  public int getExitCode() {
    return exitCode;
  }
}
