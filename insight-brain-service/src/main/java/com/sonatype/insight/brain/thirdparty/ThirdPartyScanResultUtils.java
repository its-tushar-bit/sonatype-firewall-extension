/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sonatype.insight.scan.util.HashUtils;

import org.apache.commons.lang.StringUtils;

public class ThirdPartyScanResultUtils
{
  private static final Pattern VULNERABILITY_REF_SOURCE_PATTERN = Pattern.compile("^([a-zA-Z]*)-?(.*)$");

  /**
   * Returns the vulnerability source based on the vulnerability reference.
   * <p>Usually, the alphanumeric prefix of a vulnerability reference is considered the source
   * (e.g. CVE for CVE-2014-1113) so will pick that if available.
   * Otherwise returns the first few (up to 10) characters </p>
   *
   * @return the <b>source</b> based on this reference or <b>null</b> if could not be determined
   */
  public static String getVulnerabilitySourceFromReference(String reference) {
    if (StringUtils.isNotBlank(reference)) {
      Matcher sourceMatcher = VULNERABILITY_REF_SOURCE_PATTERN.matcher(reference);
      if (sourceMatcher.matches() && StringUtils.isNotBlank(sourceMatcher.group(1))) {
        return sourceMatcher.group(1);
      }
      else {
        return reference.substring(0, Math.min(reference.length(), 10));
      }
    }

    return null;
  }

  public static String hash(String plainText) {
    final String sha1 = HashUtils.hash(plainText, HashUtils.SHA1);
    return sha1.substring(0, Math.min(sha1.length(), 20));
  }
}
