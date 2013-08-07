/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.CoordinatesValueType;

public class CoordinatesConditionType
    extends AbstractConditionType<String>
{
  public static final String ID = "Coordinates";

  private static List<String> supportedOperators;

  static {
    supportedOperators = new ArrayList<String>();
    supportedOperators.add("match");
    supportedOperators.add("do not match");
    supportedOperators = Collections.unmodifiableList(supportedOperators);
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return "Coordinates (GAV)";
  }

  @Override
  public List<String> getSupportedOperators() {
    return supportedOperators;
  }

  @Override
  public String generateDroolsConditionValue(String value) {
    return asDroolsString(value);
  }

  @Override
  public String explainMatch(final Condition condition, final Component component) {
    return "Coordinates were " + component.getGAV();
  }

  @Override
  public String getValueTypeId() {
    return CoordinatesValueType.ID;
  }

  @Override
  protected boolean internalEvaluateCondition(Component component, String operator, String value) {
    if (component.getGroupId() == null) {
      return false;
    }

    String groupId = "";
    String artifactId = "";
    String version = "";
    if (value != null) {
      String[] coordinates = value.split(":");
      if (coordinates.length >= 1) {
        groupId = coordinates[0].trim();
      }
      if (coordinates.length >= 2) {
        artifactId = coordinates[1].trim();
      }
      if (coordinates.length >= 3) {
        version = coordinates[2].trim();
      }
    }

    boolean match = new ArtifactCoordinate(groupId, artifactId, version).matches(component.getGroupId(),
        component.getArtifactId(), component.getVersion());
    return "match".equals(operator) ? match : !match;
  }
}
