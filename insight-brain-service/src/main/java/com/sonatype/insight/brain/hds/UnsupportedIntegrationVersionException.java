/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.util.List;

import com.sonatype.insight.error.HttpStatusCode;

@HttpStatusCode(400)
public class UnsupportedIntegrationVersionException
    extends RuntimeException
{
  private static final String MSG_FORMAT = "The integration version %s of %s is not supported. " +
      "Minimum supported version is %s.";

  public UnsupportedIntegrationVersionException(
      final String version,
      final String integrationName,
      final List<String> supportedVersions)
  {
    super(String.format(MSG_FORMAT, version, integrationName, getMinimumSupportedVersion(supportedVersions)));
  }

  // HDS implicitly sends versions sorted newest to oldest, so we can expect the last item in the list to be the
  // minimum supported version
  private static String getMinimumSupportedVersion(final List<String> supportedVersions) {
    return supportedVersions.get(supportedVersions.size() - 1);
  }
}
