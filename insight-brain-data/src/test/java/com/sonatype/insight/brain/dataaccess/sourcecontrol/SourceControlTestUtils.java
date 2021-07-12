/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import com.sonatype.nexus.scm.SourceControlProvider;

public class SourceControlTestUtils
{
  public static String getTestUrlForProvider(final SourceControlProvider sourceControlProvider) {
    switch (sourceControlProvider) {
      case AZURE:
        return "https://example.com/scm/organization/_git/project";
      case BITBUCKET:
        return "https://example.com/scm/organization/project";
      default:
        return "https://example.com/organization/project";
    }
  }
}
