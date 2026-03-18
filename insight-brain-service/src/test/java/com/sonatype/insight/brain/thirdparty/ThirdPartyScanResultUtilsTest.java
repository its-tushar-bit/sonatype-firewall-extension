/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import static com.sonatype.insight.brain.model.component.SecurityVulnerabilitySource.NATIONAL_VULNERABILITY_DATABASE;
import static com.sonatype.insight.vulnerability.model.SecurityVulnerabilityResearchType.PUBLIC_RESEARCH;
import static com.sonatype.insight.vulnerability.model.SecurityVulnerabilityResearchType.VENDOR_RESEARCH;
import static org.assertj.core.api.Assertions.assertThat;

public class ThirdPartyScanResultUtilsTest
{
  @Test
  public void testGetVulnerabilitySourceFromReference() {
    assertThat(ThirdPartyScanResultUtils.getVulnerabilitySourceFromReference("CVE-1234-567")).isEqualTo("CVE");
    assertThat(ThirdPartyScanResultUtils.getVulnerabilitySourceFromReference("CVE22-1234")).isEqualTo("CVE");
    assertThat(ThirdPartyScanResultUtils.getVulnerabilitySourceFromReference("CVE")).isEqualTo("CVE");

    assertThat(ThirdPartyScanResultUtils.getVulnerabilitySourceFromReference("123")).isEqualTo("123");
    assertThat(ThirdPartyScanResultUtils.getVulnerabilitySourceFromReference("0123456789ABC")).isEqualTo("0123456789");
    assertThat(ThirdPartyScanResultUtils.getVulnerabilitySourceFromReference("")).isNull();
    assertThat(ThirdPartyScanResultUtils.getVulnerabilitySourceFromReference(null)).isNull();
  }

  @Test
  public void testHash() {
    assertThat(ThirdPartyScanResultUtils.hash("pypi:django:1.11.1")).isEqualTo("41d44bac96b8c0e4f78c");
    assertThat(ThirdPartyScanResultUtils.hash(null)).isEqualTo("da39a3ee5e6b4b0d3255");
  }

  @Test
  public void testGetValidFormat() {
    assertThat(ThirdPartyScanResultUtils.getValidFormat("abcd")).isEqualTo("abcd");
    assertThat(ThirdPartyScanResultUtils.getValidFormat("long_format_third_party_scans_truncation_request_test"))
        .isEqualTo("long_format_third_party_scans_truncation_request_t");
    assertThat(ThirdPartyScanResultUtils.getValidFormat("abc:d")).isEqualTo("abc-d");
    assertThat(ThirdPartyScanResultUtils.getValidFormat("long:format_third_party_scans_truncation_request_test"))
        .isEqualTo("long-format_third_party_scans_truncation_request_t");
  }

  @Test
  public void testGetTruncatedName() {
    assertThat(ThirdPartyScanResultUtils
        .getTruncatedName(StringUtils.repeat("*", ThirdPartyScanResultUtils.NAME_MAX_LENGTH + 10)))
            .hasSize(ThirdPartyScanResultUtils.NAME_MAX_LENGTH);
  }

  @Test
  public void testGetTruncatedVersion() {
    assertThat(ThirdPartyScanResultUtils
        .getTruncatedVersion(StringUtils.repeat("*", ThirdPartyScanResultUtils.VERSION_MAX_LENGTH + 10)))
            .hasSize(ThirdPartyScanResultUtils.VERSION_MAX_LENGTH);
  }

  @Test
  public void testGetTruncatedLink() {
    assertThat(ThirdPartyScanResultUtils
        .getTruncatedLink(StringUtils.repeat("*", ThirdPartyScanResultUtils.LINK_MAX_LENGTH + 10)))
            .hasSize(ThirdPartyScanResultUtils.LINK_MAX_LENGTH);
  }

  @Test
  public void testGetTruncatedFixedBy() {
    assertThat(ThirdPartyScanResultUtils
        .getTruncatedFixedBy(StringUtils.repeat("*", ThirdPartyScanResultUtils.FIXED_BY_MAX_LENGTH + 10)))
            .hasSize(ThirdPartyScanResultUtils.FIXED_BY_MAX_LENGTH);
  }

  @Test
  public void testGetTruncatedVulnerabilitySource() {
    assertThat(ThirdPartyScanResultUtils
        .getTruncatedVulnerabilitySource(
            StringUtils.repeat("*", ThirdPartyScanResultUtils.VULNERABILITY_SOURCE_MAX_LENGTH + 10)))
                .hasSize(ThirdPartyScanResultUtils.VULNERABILITY_SOURCE_MAX_LENGTH);
  }

  @Test
  public void testGetTruncatedSeverityDescription() {
    assertThat(ThirdPartyScanResultUtils
        .getTruncatedSeverityDescription(
            StringUtils.repeat("*", ThirdPartyScanResultUtils.SEVERITY_DESCRIPTION_MAX_LENGTH + 10)))
                .hasSize(ThirdPartyScanResultUtils.SEVERITY_DESCRIPTION_MAX_LENGTH);
  }

  @Test
  public void testGetTruncatedAttackVector() {
    assertThat(ThirdPartyScanResultUtils
        .getTruncatedAttackVector(StringUtils.repeat("*", ThirdPartyScanResultUtils.ATTACK_VECTOR_MAX_LENGTH + 10)))
            .hasSize(ThirdPartyScanResultUtils.ATTACK_VECTOR_MAX_LENGTH);
  }

  @Test
  public void testGetTruncatedRatingMethod() {
    assertThat(ThirdPartyScanResultUtils
        .getTruncatedRatingMethod(StringUtils.repeat("*", ThirdPartyScanResultUtils.RATING_METHOD_MAX_LENGTH + 10)))
            .hasSize(ThirdPartyScanResultUtils.RATING_METHOD_MAX_LENGTH);
  }

  @Test
  public void testGetTruncatedRefId() {
    assertThat(ThirdPartyScanResultUtils
        .getTruncatedRefId(StringUtils.repeat("*", ThirdPartyScanResultUtils.REFID_MAX_LENGTH + 10)))
            .hasSize(ThirdPartyScanResultUtils.REFID_MAX_LENGTH);
  }

  @Test
  public void testGetTruncatedPurl() {
    assertThat(ThirdPartyScanResultUtils
        .getTruncatedPurl(StringUtils.repeat("*", ThirdPartyScanResultUtils.PURL_MAX_LENGTH + 10)))
            .hasSize(ThirdPartyScanResultUtils.PURL_MAX_LENGTH);
  }

  @Test
  public void testGetResearchTypeForThirdPartyVulnerability() {
    // Null inputs
    assertThat(ThirdPartyScanResultUtils.getResearchTypeForThirdPartyVulnerability(null, null)).isNull();

    // Vulnerability source "NVD"
    assertThat(ThirdPartyScanResultUtils.getResearchTypeForThirdPartyVulnerability("NVD", null))
        .isEqualTo(PUBLIC_RESEARCH.name());

    // Vulnerability source "National Vulnerability Database"
    assertThat(
        ThirdPartyScanResultUtils.getResearchTypeForThirdPartyVulnerability(NATIONAL_VULNERABILITY_DATABASE.getName(),
            null)).isEqualTo(PUBLIC_RESEARCH.name());

    // Vulnerability source not "NVD"
    assertThat(ThirdPartyScanResultUtils.getResearchTypeForThirdPartyVulnerability("SomeSource", null))
        .isEqualTo(VENDOR_RESEARCH.name());

    // RefId contains "cve"
    assertThat(ThirdPartyScanResultUtils.getResearchTypeForThirdPartyVulnerability(null,
        NATIONAL_VULNERABILITY_DATABASE.getId())).isEqualTo(PUBLIC_RESEARCH.name());

    // RefId does not contain "cve"
    assertThat(ThirdPartyScanResultUtils.getResearchTypeForThirdPartyVulnerability(null, "SomeRefId"))
        .isEqualTo(VENDOR_RESEARCH.name());

    // Vulnerability source not "NVD" and RefId does not contain "cve"
    assertThat(
        ThirdPartyScanResultUtils.getResearchTypeForThirdPartyVulnerability("SomeSource", "SomeRefId"))
            .isEqualTo(VENDOR_RESEARCH.name());

    // Both vulnerability source and refId are provided
    assertThat(ThirdPartyScanResultUtils.getResearchTypeForThirdPartyVulnerability("NVD",
        NATIONAL_VULNERABILITY_DATABASE.getId())).isEqualTo(PUBLIC_RESEARCH.name());
  }
}
