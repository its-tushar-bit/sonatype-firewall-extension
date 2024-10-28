/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.CoordinatesValueType;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;
import com.sonatype.insight.dataaccess.TransactionContext;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.StringUtils;

@Singleton
@Named
public class CoordinatesConditionType
    extends AbstractComponentConditionType<String>
{
  private static final Map<String, Set<Integer>> FORMAT_TO_OPTIONAL_COORDINATE_INDEXES = new HashMap<>();

  static {
    FORMAT_TO_OPTIONAL_COORDINATE_INDEXES.put(ComponentIdentifier.FORMAT_MAVEN, Collections.singleton(5));
    FORMAT_TO_OPTIONAL_COORDINATE_INDEXES.put(ComponentIdentifier.FORMAT_ANAME, Collections.singleton(2));
    FORMAT_TO_OPTIONAL_COORDINATE_INDEXES.put(ComponentIdentifier.FORMAT_PYPI, ImmutableSet.of(3, 4));
    FORMAT_TO_OPTIONAL_COORDINATE_INDEXES.put(ComponentIdentifier.FORMAT_NPM, Collections.emptySet());
  }

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
  public String explainMatch(final Condition condition, final MatchFact matchFact) {
    String conditionComponent =
        ComponentDisplayNameUtil.fromIdentifier(getComponentIdentifier(condition.getValue())).toString();
    return "Coordinates were " + matchFact.getComponent().getDisplayNameFromIdentifier() + " (" +
        condition.getOperator() + " " + conditionComponent + ")";
  }

  @Override
  public String convertIfNeeded(final String value) {
    return convertToWildcardWhereNeeded(value);
  }

  @Override
  public boolean isAutoUnquarantineSupported() {
    return false;
  }

  private String convertToWildcardWhereNeeded(final String value) {
    if (StringUtils.isBlank(value)) {
      return value;
    }
    final String[] splitValue = value.split(":", -1);
    final String format = splitValue[0].trim();
    final Set<String> allCoordinateNames = ComponentIdentifier.getAllCoordinateNames(format);
    if (allCoordinateNames.isEmpty()) {
      return value;
    }
    final String[] coordinates = new String[allCoordinateNames.size() + 1];
    // first part of coordinate value is format, all other parts are wildcards by default
    coordinates[0] = format;
    Arrays.fill(coordinates, 1, coordinates.length, ArtifactCoordinate.PLACEHOLDER);
    final Set<Integer> optionalCoordinateIndexes = getOptionalCoordinateIndexes(format);
    for (int i = 1; i < coordinates.length && i < splitValue.length; i++) {
      splitValue[i] = splitValue[i].trim();
      // if trimmed coordinate value part is not empty, or is empty and optional, then use it
      if (!splitValue[i].isEmpty() || optionalCoordinateIndexes.contains(i)) {
        coordinates[i] = splitValue[i];
      }
    }
    return StringUtils.join(coordinates, ":");
  }

  private Set<Integer> getOptionalCoordinateIndexes(final String format) {
    if (!FORMAT_TO_OPTIONAL_COORDINATE_INDEXES.containsKey(format)) {
      return Collections.emptySet();
    }
    return FORMAT_TO_OPTIONAL_COORDINATE_INDEXES.get(format);
  }

  @Override
  public String getValueTypeId() {
    return CoordinatesValueType.ID;
  }

  @Override
  protected boolean internalEvaluateCondition(Component component, String operator, String value) {
    final ComponentIdentifier componentIdentifier = getComponentIdentifier(value);

    boolean match = new ArtifactCoordinate(componentIdentifier).matches(component.getComponentIdentifier());
    return "match".equals(operator) ? match : !match;
  }

  private ComponentIdentifier getComponentIdentifier(final String value) {
    final String[] coordinates = value.split(":", -1);
    final String format = coordinates[0];

    final ComponentIdentifier componentIdentifier;
    switch (format) {
      case ComponentIdentifier.FORMAT_MAVEN:
        // this method takes maven coordinates in the order GAVCE, but we have them as GAVEC, so swap the last two
        componentIdentifier = ComponentIdentifier
            .createMavenCoordinates(coordinates[1], coordinates[2], coordinates[3], coordinates[5], coordinates[4]);
        break;
      case ComponentIdentifier.FORMAT_ANAME:
        componentIdentifier = ComponentIdentifier
            .createAnameCoordinates(coordinates[1], coordinates[2], coordinates[3]);
        break;
      case ComponentIdentifier.FORMAT_PYPI:
        componentIdentifier = ComponentIdentifier
            .createPypiCoordinates(coordinates[1], coordinates[2], coordinates[3], coordinates[4]);
        break;
      case ComponentIdentifier.FORMAT_NPM:
        componentIdentifier = ComponentIdentifier
            .createNpmCoordinates(coordinates[1], coordinates[2]);
        break;
      default:
        throw new IllegalArgumentException("Unsupported component identifier format:" + format);
    }
    return componentIdentifier;
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
      case ComponentIdentifier.FORMAT_PYPI:
      case ComponentIdentifier.FORMAT_NPM:
        break;
      default:
        throw new InvalidConditionException(condition,
            "Unsupported component identifier format for coordinates policy condition: '" + format + "'");
    }

    super.validateCondition(tx, condition, ownerId);
  }
}
