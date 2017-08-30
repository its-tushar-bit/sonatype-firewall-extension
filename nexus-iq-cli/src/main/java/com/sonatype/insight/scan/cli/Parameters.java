/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.util.List;

import com.beust.jcommander.Parameter;

public class Parameters
    extends AbstractCliParameters
{
  @Parameter(description = "Archives or directories to scan", required = true)
  private List<String> scanTargets;

  /**
   * @since 1.34
   */
  @Parameter(names = { "-xc", "--expanded-coverage" }, description = "Enable Expanded Coverage analysis.")
  private boolean expandedCoverageMode;

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
