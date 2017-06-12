/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.CoordinatesValueType;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.apache.commons.lang.StringUtils;

public class CoordinatesConditionType
    extends AbstractComponentConditionType<String>
{
  public static final String ID = "Coordinates";

  private static List<String> supportedOperators;

  static {
    supportedOperators = new ArrayList<>();
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
    return "Coordinates";
  }

  @Override
  public List<String> getSupportedOperators() {
    return supportedOperators;
  }

  @Override
  public String generateDroolsConditionValue(TransactionContext tx, String value) {
    return asDroolsString(value);
  }

  @Override
  public String explainMatch(final Condition condition, final Component component) {
    return "Coordinates were " + component.getDisplayName();
  }

  @Override
  public String getValueTypeId() {
    return CoordinatesValueType.ID;
  }

  @Override
  protected boolean internalEvaluateCondition(Component component, String operator, String value) {
    String[] coordinates = value.split(":");
    String format = coordinates[0].trim();

    String c1 = "";
    String c2 = "";
    String c3 = "";
    if (coordinates.length >= 2) {
      c1 = coordinates[1].trim();
    }
    if (coordinates.length >= 3) {
      c2 = coordinates[2].trim();
    }
    if (coordinates.length >= 4) {
      c3 = coordinates[3].trim();
    }

    ComponentIdentifier componentIdentifier;
    switch (format) {
      case ComponentIdentifier.FORMAT_MAVEN:
        componentIdentifier = ComponentIdentifier.createMavenCoordinates(c1, c2, c3);
        break;
      case ComponentIdentifier.FORMAT_ANAME:
        componentIdentifier = ComponentIdentifier.createAnameCoordinates(c1, c2, c3);
        break;
      default:
        throw new IllegalArgumentException("Unsupported component identifier format:" + format);
    }

    boolean match = new ArtifactCoordinate(componentIdentifier).matches(component.getComponentIdentifier());
    return "match".equals(operator) ? match : !match;
  }

  @Override
  public void validateCondition(TransactionContext tx, Condition condition, String ownerId)
      throws InvalidConditionException
  {
    String value = condition.getValue();
    if (StringUtils.isBlank(value)) {
      throw new InvalidConditionException(condition, "Missing coordinates");
    }

    String[] coordinates = value.split(":");
    String format = coordinates[0].trim();
    switch (format) {
      case ComponentIdentifier.FORMAT_MAVEN:
      case ComponentIdentifier.FORMAT_ANAME:
        break;
      default:
        throw new InvalidConditionException(condition,
            "Unsupported component identifier format for coordinates policy condition: '" + format + "'");
    }

    super.validateCondition(tx, condition, ownerId);
  }
}
