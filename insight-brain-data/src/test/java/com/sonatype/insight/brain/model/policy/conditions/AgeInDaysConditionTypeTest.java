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
}
