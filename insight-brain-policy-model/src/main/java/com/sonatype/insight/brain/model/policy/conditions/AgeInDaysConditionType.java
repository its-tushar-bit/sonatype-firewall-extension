/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.AgeInDaysValueType;

import org.joda.time.Interval;
import org.joda.time.PeriodType;
import org.joda.time.format.PeriodFormat;
import org.joda.time.format.PeriodFormatter;

public class AgeInDaysConditionType
    extends AbstractConditionType<Integer>
{
  public static final String ID = "AgeInDays";

  public static final long DAY_IN_MILLISECONDS = 24L * 3600L * 1000L;

  private static final PeriodFormatter AGE_FORMATTER = PeriodFormat.wordBased(Locale.ENGLISH);

  private static List<String> supportedOperators = new ArrayList<String>();

  static {
    supportedOperators.add("older than");
    supportedOperators.add("younger than");
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return "Age";
  }

  @Override
  public List<String> getSupportedOperators() {
    return supportedOperators;
  }

  @Override
  public String generateDroolsConditionValue(String value) {
    return asDroolsInteger(value) + asDroolsComment("days");
  }

  @Override
  public String explainMatch(final Condition condition, final Component component) {
    String age = "unknown";
    final Long catalogDate = component.getCatalogDate();
    if (catalogDate != null) {
      final Interval interval = new Interval(catalogDate, System.currentTimeMillis());
      age = interval.toPeriod(PeriodType.yearMonthDay()).toString(AGE_FORMATTER);
    }
    return "Age was " + age;
  }

  @Override
  public String getValueTypeId() {
    return AgeInDaysValueType.ID;
  }

  @Override
  public void validateCondition(Condition condition, String ownerId) throws InvalidConditionException {
    super.validateCondition(condition, ownerId);

    try {
      Integer.parseInt(condition.getValue());
    }
    catch (NumberFormatException e) {
      throw new InvalidConditionException(condition, "Invalid age (in days): " + condition.getValue());
    }
  }

  @Override
  public String getValueHint() {
    return "Enter term";
  }

  @Override
  public boolean internalEvaluateCondition(Component component, String operator, Integer value) {
    if (component.getCatalogDate() == null) {
      return false;
    }
    int ageInDays = (int) ((System.currentTimeMillis() - component.getCatalogDate()) / DAY_IN_MILLISECONDS);
    if ("older than".equals(operator)) {
      return ageInDays > value;
    }
    else {
      return ageInDays <= value;
    }
  }
}
