/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;

import com.google.common.base.Joiner;

/**
 * Carries the data backing the Newest Risk view.
 *
 * @since 1.11.0
 */
public class NewestRiskDTO implements CsvWritable
{
  private static final Joiner joiner = Joiner.on(",");

  public String applicationPublicId;

  public String applicationName;

  public int threatLevel;

  public long time;

  public String policyId;

  public String policyName;

  public String hash;

  public ComponentDisplayName displayName;

  public List<String> pathnames;

  public List<StageDetailDTO> stageDetails = new ArrayList<>();

  public static String getCsvHeader() {
    return "Threat Level,Policy Name,Application Name,Component Name,Date First Seen,Timestamp First Seen";
  }

  @Override
  public String toCsvLine() {
    String componentName = displayName != null
        ? displayName.toString()
        : ComponentDisplayNameUtil.fromPathnames(pathnames, hash).toString();
    if (componentName.contains(",")) {
      componentName = "\"" + componentName + "\"";
    }
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    return joiner.join(threatLevel, policyName, applicationName, componentName, formatter.format(time), time);
  }
}
