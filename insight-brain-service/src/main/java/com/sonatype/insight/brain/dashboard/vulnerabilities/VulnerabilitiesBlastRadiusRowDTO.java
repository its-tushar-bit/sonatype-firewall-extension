/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.vulnerabilities;

import java.util.Locale;
import java.util.Map;

import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.utils.CsvWritable;

import org.apache.commons.lang3.StringUtils;

/**
 * One My Scan Data blast-radius CSV row: vulnerability × application scan target (CLM-42216).
 */
public class VulnerabilitiesBlastRadiusRowDTO
    implements CsvWritable
{
  /** Scan-target type until hosted-repo index fields land. */
  static final String SCAN_TARGET_TYPE_APPLICATION = "Application";

  public String vulnerabilityId;

  public String title;

  public Float cvssScore;

  public String severity;

  public String vulnerabilityStatus;

  public String organizationName;

  public String applicationName;

  public String stage;

  public String componentName;

  public String componentVersion;

  public String ecosystem;

  /** Always {@link #SCAN_TARGET_TYPE_APPLICATION} until hosted-repo index fields land. */
  public String scanTargetType = SCAN_TARGET_TYPE_APPLICATION;

  public static String getCsvHeader() {
    return "Vulnerability ID,Title,CVSS Score,Severity,Vulnerability Status,Organization,Application,Stage,"
        + "Component,Component Version,Ecosystem,Scan Target Type";
  }

  static VulnerabilitiesBlastRadiusRowDTO fromIndexItem(final SearchResultItemDTO item) {
    VulnerabilitiesBlastRadiusRowDTO row = new VulnerabilitiesBlastRadiusRowDTO();
    row.vulnerabilityId = item.vulnerabilityId;
    row.title = item.vulnerabilityDescription;
    row.cvssScore = item.vulnerabilitySeverity;
    row.severity = VulnerabilitiesListRequestValidator.severityBand(item.vulnerabilitySeverity);
    row.vulnerabilityStatus = item.vulnerabilityStatus;
    row.organizationName = item.organizationName;
    row.applicationName = item.applicationName;
    row.stage = item.policyEvaluationStage;
    row.componentName = item.componentName;
    if (item.componentIdentifier != null) {
      row.ecosystem = item.componentIdentifier.getFormat();
      Map<String, String> coords = item.componentIdentifier.getCoordinates();
      if (coords != null) {
        row.componentVersion = firstNonBlank(coords.get("version"), coords.get("Version"));
      }
    }
    return row;
  }

  @Override
  public String toCsvLine() {
    return CsvWritable.joiner.join(
        csv(vulnerabilityId),
        csv(title),
        cvssScore == null ? "" : String.format(Locale.ROOT, "%.1f", cvssScore),
        csv(severity),
        csv(vulnerabilityStatus),
        csv(organizationName),
        csv(applicationName),
        csv(stage),
        csv(componentName),
        csv(componentVersion),
        csv(ecosystem),
        csv(scanTargetType));
  }

  private static String firstNonBlank(final String a, final String b) {
    if (StringUtils.isNotBlank(a)) {
      return a;
    }
    return StringUtils.isNotBlank(b) ? b : null;
  }

  private static String csv(final String value) {
    if (value == null || value.isEmpty()) {
      return "";
    }
    return CsvWritable.quoteFieldWhenSpecialCsvCharactersPresent(CsvWritable.escapeDoubleQuotes(value));
  }
}
