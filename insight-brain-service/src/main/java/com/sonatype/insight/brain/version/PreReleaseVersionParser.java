/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.version;

import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

public class PreReleaseVersionParser
{
  // This regex attempts to match pre-release qualifiers in a version string.
  // If no match is found, the version can be considered stable.
  private static final String PRE_RELEASE_VERSION_REGEX =
      "^[\\d.\\-]+[mba]\\d+$|[.\\-][mba]\\d+$|((rc|cr)[+\\-.\\d]|(rc|cr)\\b]|(rc|cr)$|milestone|alpha|beta|pre[\\-.]" +
          "?|preview|prerel|pre[_\\-]release|snapshot|eap|gamma|feature|dev|ea[\\-.\\d]*$|nightly|canary" +
          "|^[\\d.\\-_]*fc[\\-.\\d]*$|demo)[\\d$]*";

  private PreReleaseVersionParser() {
    // Utility class
  }

  public static boolean isPreReleaseVersion(final String version) {
    if (StringUtils.isEmpty(version)) {
      throw new IllegalArgumentException("Version cannot be null or empty");
    }
    final Pattern pattern = Pattern.compile(PRE_RELEASE_VERSION_REGEX, Pattern.CASE_INSENSITIVE);
    return pattern.matcher(version).find();
  }

  public static boolean isStable(final String version) {
    return !isPreReleaseVersion(version);
  }
}
