/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import com.sonatype.insight.brain.model.component.Component;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AgeInDaysConditionTypeTest
{
  private final AgeInDaysConditionType conditionType = new AgeInDaysConditionType();

  @Test
  public void internalEvaluateCondition_returnsFalse_whenCatalogDateIsNull() {
    Component component = new Component();
    component.setCatalogDate(null);

    assertThat(conditionType.internalEvaluateCondition(component, "younger than", 7)).isFalse();
    assertThat(conditionType.internalEvaluateCondition(component, "older than", 7)).isFalse();
  }

  @Test
  public void internalEvaluateCondition_returnsFalse_whenCatalogDateIsZero() {
    Component component = new Component();
    component.setCatalogDate(0L);

    assertThat(conditionType.internalEvaluateCondition(component, "younger than", 7)).isFalse();
    assertThat(conditionType.internalEvaluateCondition(component, "older than", 7)).isFalse();
  }

  @Test
  public void internalEvaluateCondition_returnsFalse_whenCatalogDateIsNegative() {
    Component component = new Component();
    component.setCatalogDate(-1L);

    assertThat(conditionType.internalEvaluateCondition(component, "younger than", 7)).isFalse();
    assertThat(conditionType.internalEvaluateCondition(component, "older than", 7)).isFalse();
  }

  @Test
  public void internalEvaluateCondition_youngerThan_firesForRecentCatalogDate() {
    Component component = new Component();
    long oneDayAgoMs = System.currentTimeMillis() - (24L * 3600L * 1000L);
    component.setCatalogDate(oneDayAgoMs);

    assertThat(conditionType.internalEvaluateCondition(component, "younger than", 7)).isTrue();
    assertThat(conditionType.internalEvaluateCondition(component, "older than", 7)).isFalse();
  }

  @Test
  public void internalEvaluateCondition_olderThan_firesForOldCatalogDate() {
    Component component = new Component();
    long oneYearAgoMs = System.currentTimeMillis() - (365L * 24L * 3600L * 1000L);
    component.setCatalogDate(oneYearAgoMs);

    assertThat(conditionType.internalEvaluateCondition(component, "older than", 7)).isTrue();
    assertThat(conditionType.internalEvaluateCondition(component, "younger than", 7)).isFalse();
  }

  @Test
  public void internalEvaluateCondition_atBoundary_neitherOperatorFires_strictlyInsideEachFires() {
    Component component = new Component();
    // Offset by 3.5 days so ageInDays computes to 3 even with GC pauses or CI
    // scheduler jitter between this line and System.currentTimeMillis() inside
    // the production code. A smaller buffer (e.g. 1s) flips age to 4 under load
    // and silently changes which boundary the assertions exercise.
    long threeDaysAgoMs = System.currentTimeMillis()
        - (3L * AgeInDaysConditionType.DAY_IN_MILLISECONDS)
        - (AgeInDaysConditionType.DAY_IN_MILLISECONDS / 2L);
    component.setCatalogDate(threeDaysAgoMs);

    assertThat(conditionType.internalEvaluateCondition(component, "younger than", 3)).isFalse();
    assertThat(conditionType.internalEvaluateCondition(component, "older than", 3)).isFalse();
    assertThat(conditionType.internalEvaluateCondition(component, "younger than", 4)).isTrue();
    assertThat(conditionType.internalEvaluateCondition(component, "older than", 2)).isTrue();
  }
}
