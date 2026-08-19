/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Objects;

import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.utils.CsvWritable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.common.base.Joiner;

/**
 * Carries the data backing the "Highest Risk Component View", i.e. roll-up of violations by component.
 * <p>
 * Score fields are nullable so index-only stub rows (no SQL enrich) omit them on the wire — primitive
 * {@code int} would serialize as {@code 0} and make NOUX cards show empty {@code 0/0/0/0} badges.
 * Include is field-scoped so Classic {@code componentRisks} keeps serializing other nulls as before.
 */
public class ComponentRiskDTO
    implements CsvWritable
{
  private static final Joiner joiner = Joiner.on(",");

  public String hash;

  @JsonInclude(Include.NON_NULL)
  public Integer score;

  @JsonInclude(Include.NON_NULL)
  public Integer scoreCritical;

  @JsonInclude(Include.NON_NULL)
  public Integer scoreSevere;

  @JsonInclude(Include.NON_NULL)
  public Integer scoreModerate;

  @JsonInclude(Include.NON_NULL)
  public Integer scoreLow;

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
    return joiner.join(
        componentName,
        affectedApplications,
        Objects.requireNonNullElse(score, 0),
        Objects.requireNonNullElse(scoreCritical, 0),
        Objects.requireNonNullElse(scoreSevere, 0),
        Objects.requireNonNullElse(scoreModerate, 0),
        Objects.requireNonNullElse(scoreLow, 0));
  }
}
