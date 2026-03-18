/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sonatype.insight.brain.model.component.SecurityVulnerabilitySource;
import com.sonatype.insight.scan.util.HashUtils;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityResearchType;

import org.apache.commons.lang3.StringUtils;

public class ThirdPartyScanResultUtils
{
  public static final int FORMAT_MAX_LENGTH = 50;

  public static final int NAME_MAX_LENGTH = 300;

  public static final int VERSION_MAX_LENGTH = 200;

  public static final int LINK_MAX_LENGTH = 200;

  public static final int FIXED_BY_MAX_LENGTH = 200;

  public static final int VULNERABILITY_SOURCE_MAX_LENGTH = 50;

  public static final int SEVERITY_DESCRIPTION_MAX_LENGTH = 15;

  public static final int ATTACK_VECTOR_MAX_LENGTH = 255;

  public static final int RATING_METHOD_MAX_LENGTH = 10;

  public static final int REFID_MAX_LENGTH = 255;

  public static final int THIRD_PARTY_IDENTIFICATION_SOURCE_MAX_LENGTH = 20;

  public static final int PURL_MAX_LENGTH = 1000;

  private static final Pattern VULNERABILITY_REF_SOURCE_PATTERN = Pattern.compile("^([a-zA-Z]*)-?(.*)$");

  /**
   * Returns the vulnerability source based on the vulnerability reference.
   * <p>
   * Usually, the alphanumeric prefix of a vulnerability reference is considered the source
   * (e.g. CVE for CVE-2014-1113) so will pick that if available.
   * Otherwise returns the first few (up to 10) characters
   * </p>
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

  public static String getValidFormat(String format) {
    String newFormat = StringUtils.truncate(format, FORMAT_MAX_LENGTH);
    return newFormat.replace(':', '-');
  }

  public static String getTruncatedName(String name) {
    return StringUtils.truncate(name, NAME_MAX_LENGTH);
  }

  public static String getTruncatedVersion(String version) {
    return StringUtils.truncate(version, VERSION_MAX_LENGTH);
  }

  public static String getTruncatedLink(String link) {
    return StringUtils.truncate(link, LINK_MAX_LENGTH);
  }

  public static String getTruncatedFixedBy(String fixedBy) {
    return StringUtils.truncate(fixedBy, FIXED_BY_MAX_LENGTH);
  }

  public static String getTruncatedVulnerabilitySource(String vulnerabilitySource) {
    return StringUtils.truncate(vulnerabilitySource, VULNERABILITY_SOURCE_MAX_LENGTH);
  }

  public static String getTruncatedSeverityDescription(String severityDescription) {
    return StringUtils.truncate(severityDescription, SEVERITY_DESCRIPTION_MAX_LENGTH);
  }

  public static String getTruncatedAttackVector(String attackVector) {
    return StringUtils.truncate(attackVector, ATTACK_VECTOR_MAX_LENGTH);
  }

  public static String getTruncatedRatingMethod(String ratingMethod) {
    return StringUtils.truncate(ratingMethod, RATING_METHOD_MAX_LENGTH);
  }

  public static String getTruncatedRefId(String refId) {
    return StringUtils.truncate(refId, REFID_MAX_LENGTH);
  }

  public static String getTruncatedThirdPartyIdentificationSource(String source) {
    return StringUtils.truncate(source, THIRD_PARTY_IDENTIFICATION_SOURCE_MAX_LENGTH);
  }

  public static String getTruncatedPurl(String purl) {
    return StringUtils.truncate(purl, PURL_MAX_LENGTH);
  }

  public static String getResearchTypeForThirdPartyVulnerability(String vulnerabilitySource, String refId) {
    if (vulnerabilitySource != null) {
      if (vulnerabilitySource.equalsIgnoreCase("NVD") ||
          vulnerabilitySource.equalsIgnoreCase(SecurityVulnerabilitySource.NATIONAL_VULNERABILITY_DATABASE.getName()))
      {
        return SecurityVulnerabilityResearchType.PUBLIC_RESEARCH.name();
      }
      return SecurityVulnerabilityResearchType.VENDOR_RESEARCH.name();
    }
    if (StringUtils.isNotBlank(refId)) {
      if (StringUtils.containsIgnoreCase(refId, SecurityVulnerabilitySource.NATIONAL_VULNERABILITY_DATABASE.getId())) {
        return SecurityVulnerabilityResearchType.PUBLIC_RESEARCH.name();
      }
      return SecurityVulnerabilityResearchType.VENDOR_RESEARCH.name();
    }
    return null;
  }
}
