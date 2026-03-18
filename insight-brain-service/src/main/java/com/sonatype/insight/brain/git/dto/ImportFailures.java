/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.dto;

import java.util.List;

public class ImportFailures
{
  public ImportFailures() {
    // no-op
  }

  public ImportFailures(List<ImportFailure> failures) {
    this.failures = failures;
  }

  public List<ImportFailure> failures;
}
