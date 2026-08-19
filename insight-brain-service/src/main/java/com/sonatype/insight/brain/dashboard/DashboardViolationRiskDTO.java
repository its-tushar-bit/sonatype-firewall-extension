/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.utils.CsvWritable;

import com.google.common.base.Joiner;

/**
 * Carries the data backing the Dashboard Violations tab.
 *
 * @since 1.11.0
 */
public class DashboardViolationRiskDTO
    implements CsvWritable
{
  private static final Joiner joiner = Joiner.on(",");

  public String applicationName;

  public String organizationName;

  public int threatLevel;

  public long firstOccurrenceTime;

  public String policyName;

  public String policyId;

  public String policyViolationId;

  public String hash;

  public ComponentDisplayName displayName;

  public String derivedComponentName;

  public String filename;

  public String referenceId;

  public static String getCsvHeader() {
    // this is the dto involved in the export
    return "Threat Level,Policy Name,Organization Name,Application Name,Component Name,Date First Seen"
        + ",Timestamp First Seen, Reference, Policy Violation Id";
  }

  @Override
  public String toCsvLine() {
    String componentName = displayName != null
        ? displayName.toString()
        : ComponentDisplayNameUtil.fromFilename(filename, hash).toString();
    if (componentName.contains(",")) {
      componentName = "\"" + componentName + "\"";
    }
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    String referenceIdOrDefault = referenceId == null ? "" : referenceId;
    return joiner.join(threatLevel, policyName, organizationName, applicationName, componentName,
        formatter.format(firstOccurrenceTime), firstOccurrenceTime, referenceIdOrDefault, policyViolationId);
  }

  public String getApplicationName() {
    return applicationName;
  }

  public long getFirstOccurrenceTime() {
    return firstOccurrenceTime;
  }
}
