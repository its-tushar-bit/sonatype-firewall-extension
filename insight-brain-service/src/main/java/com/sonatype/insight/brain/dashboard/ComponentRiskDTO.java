/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.utils.CsvWritable;

import com.google.common.base.Joiner;

/**
 * Carries the data backing the "Highest Risk Component View", i.e. roll-up of violations by component.
 */
public class ComponentRiskDTO
    implements CsvWritable
{
  private static final Joiner joiner = Joiner.on(",");

  public String hash;

  public int score;

  public int scoreCritical;

  public int scoreSevere;

  public int scoreModerate;

  public int scoreLow;

  public int affectedApplications;

  public ComponentDisplayName displayName;

  public String derivedComponentName;

  public String filename;

  public static String getCsvHeader() {
    return "Component Name,Affected Apps,Total Risk,Critical,Severe,Moderate,Low";
  }

  @Override
  public String toCsvLine() {
    String componentName = displayName != null
        ? displayName.toString()
        : ComponentDisplayNameUtil.fromFilename(filename, hash).toString();
    if (componentName.contains(",")) {
      componentName = "\"" + componentName + "\"";
    }
    return joiner.join(componentName, affectedApplications, score, scoreCritical, scoreSevere, scoreModerate, scoreLow);
  }
}
